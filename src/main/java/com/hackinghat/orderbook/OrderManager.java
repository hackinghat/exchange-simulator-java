package com.hackinghat.orderbook;

import com.hackinghat.kafka.KafkaConfigBuilder;
import com.hackinghat.kafka.KafkaJsonSerializer;
import com.hackinghat.kafka.KafkaTradeSerializer;
import com.hackinghat.model.Instrument;
import com.hackinghat.model.Trade;
import com.hackinghat.model.serialize.EventPublisher;
import com.hackinghat.model.serialize.EventPublisherComponent;
import com.hackinghat.order.*;
import com.hackinghat.model.Level;
import com.hackinghat.orderbook.auction.AuctionException;
import com.hackinghat.orderbook.auction.AuctionState;
import com.hackinghat.orderbook.auction.AuctionTriggerEvent;
import com.hackinghat.orderbook.auction.MarketManager;
import com.hackinghat.util.*;
import com.hackinghat.util.component.AbstractComponent;
import com.hackinghat.util.mbean.MBeanAttribute;
import com.hackinghat.util.mbean.MBeanType;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static com.hackinghat.util.Formatters.PRICE_FORMAT;
import static com.hackinghat.util.Formatters.QUANTITY_FORMAT;

@MBeanType(description = "Order manager")
public class OrderManager extends AbstractComponent implements Runnable, Listener
{
    private static final Logger LOG = LogManager.getLogger(OrderManager.class);
    private static final AtomicLong ID = new AtomicLong();

    private final Object                          sync        = new Object();
    private final AtomicLong                      counter     = new AtomicLong();
    private final AtomicBoolean                   terminate   = new AtomicBoolean(true);

    private final Map<Long, Order>                orderLatest;
    private final PriorityBlockingQueue<Event>    eventQueue;
    private final Instrument                      instrument;
    private final CachedValue<FullDepth>          fullDepth;
    private final OrderBook                       bidBook;
    private final OrderBook                       offerBook;
    private final EventDispatcher                 eventDispatcher;
    private final AbstractStatisticsAppender      tape;
    private final AbstractStatisticsAppender      orderAppender;
    private final TimeMachine                     timeMachine;
    private final OrderManagerState               orderManagerState;
    private final Duration                        marketDataDelay;
    private final MarketManager                   marketManager;
    private final SimulatorObjectMapper           mapper;
    private final EventPublisherComponent<String, Trade> tapePublisher;
    private       CachedValue<Level1>             level1;
    private       Level                           referencePrice;
    public OrderManager(final MarketManager marketManager, TimeMachine timeMachine, final Level referencePrice, final MarketState initialState, final Instrument instrument, final EventDispatcher eventDispatcher, final AbstractStatisticsAppender tape, final AbstractStatisticsAppender orderAppender, final Duration marketDataDelay)
    {
        super("OrderManager-" + instrument.getTicker());
        Objects.requireNonNull(marketManager);
        Objects.requireNonNull(timeMachine);
        Objects.requireNonNull(instrument);
        Objects.requireNonNull(eventDispatcher);
        Objects.requireNonNull(marketDataDelay);

        this.marketManager = marketManager;
        this.instrument = instrument;
        this.timeMachine = timeMachine;
        this.eventDispatcher = require(eventDispatcher);
        this.bidBook = require(new OrderBook(OrderSide.BUY, instrument));
        this.offerBook = require(new OrderBook(OrderSide.SELL, instrument));
        this.orderLatest = new ConcurrentHashMap<>();
        this.tape = tape;
        this.orderAppender = orderAppender;
        this.eventQueue = new PriorityBlockingQueue<>();
        this.eventDispatcher.addListener(AuctionTriggerEvent.class, this);
        this.orderManagerState = new OrderManagerState(initialState);
        this.level1 = new CachedValue<>(Level1.class, timeMachine, marketDataDelay, this::calculateLevel1, eventDispatcher, true);
        this.fullDepth = new CachedValue<>(FullDepth.class, timeMachine, marketDataDelay, this::calculateFullDepth, eventDispatcher, true);
        this.marketDataDelay = marketDataDelay;
        this.referencePrice = referencePrice;
        this.mapper = new SimulatorObjectMapper(SimulatorObjectMapperAudience.PUBLIC, timeMachine);
        // If we're not appending orders to a log then let's not publish anything  either (because we're in test)
        if (orderAppender != null) {
            this.tapePublisher = require(new EventPublisherComponent<>("EP-Trade", mapper, new StringSerializer(), new KafkaTradeSerializer(mapper), (t) -> "VOD.TRADE"));
        } else {
            this.tapePublisher = null;
        }
    }

    @MBeanAttribute(description = "Current state of the market")
    public String getMarketState() { return orderManagerState.getCurrent().toString(); }

    private Level1 calculateLevel1()
    {
        return orderManagerState.isState(MarketState.AUCTION) ? getAuctionState() : getTouch();
    }

    private FullDepth calculateFullDepth()
    {
        return new FullDepth(getQueue(OrderSide.BUY), getQueue(OrderSide.SELL), timeMachine);
    }

    /**
     * Observers and agents will react based on the current touch price whilst technically the touch
     * is simply the top of the order book less informed agents will base most decisions upon it
     * TODO: Whilst the order books are individually locked the books are not locked together allowing
     * a potentially inconsistent view of what the touch is
     * @see com.hackinghat.agent.ZeroIntelligenceAgent
     * @return the current touch (which could be zero)
     */
    private Level1 getTouch()
    {
        return new Touch(timeMachine.toSimulationTime(), orderManagerState.getCurrent(), bidBook.getBestInterest(), offerBook.getBestInterest());
    }

    private AuctionState getAuctionState()
    {
        return new AuctionState(timeMachine.toSimulationTime(), referencePrice, instrument, bidBook, offerBook);
    }

    /**
     * This is the weaker guarantee of being no staler than 100ms, but prevents every agent having to build
     * their own touch record.  This mimics how information is disseminated in the real world, i.e. prices
     * appear instantaneous but always take some time to be delivered.  Should not really be used
     * @return
     */
    public Level1 getLevel1()
    {
        return level1.get();
    }

    @MBeanAttribute(description = "The string description of the Level1")
    public String getLevel1Interest() {
        return getLevel1().getBid().formatInterest() + " - " + getLevel1().getOffer().formatInterest();
    }

    @MBeanAttribute(description = "Show the wall-clock in the simulator")
    public String getSimulationTime() {
        return timeMachine.formatTime();
    }

    public FullDepth getFullDepth()
    {
        return fullDepth.get();
    }

    public OrderBook getQueue(OrderSide side)
    {
        return side == OrderSide.BUY ? bidBook : offerBook;
    }

    private OrderBook getOtherQueue(OrderSide side)
    {
        return side == OrderSide.BUY ? offerBook : bidBook;
    }

    public void terminate()
    {
        terminate.set(true);
    }

    boolean isTerminate()
    {
        return terminate.get();
    }

    public Level getReferencePrice() { return referencePrice; }

    public Order[] add(Order... orders)
    {
        for(Order order : orders)
        {
            if (!OrderState.isPending(order.getState()))
                throw new IllegalArgumentException("Must be in a pending state");

            if (order.getId() == null)
            {
                order.setId(counter.getAndIncrement());
            }
            // We don't want to put someone else's order into the manager, all orders should be
            // cloned with the current simulation time (to preserve the ordering between event types)
            eventQueue.add(order.copy(timeMachine.toSimulationTime()));
        }
        return orders;
    }

    public int sizePending()
    {
        return eventQueue.size();
    }

    private Level getExecutionPrice(final OrderBook queue, final Level localLevel, final Level opposingLevel)
    {
        return chooseLevelForPrice(queue.getQueueSide(), localLevel, opposingLevel);
    }

    /**
     * When determining the executing price the assumption is that the order that presents itself to the
     * manager to be considered for clearing must either 1) be a market order; 2) be a limit order outside the
     * opposing spread.  In both cases the oldest order at the top of the book will be executed.
     * @param side the side for the 'clearing' order
     * @param levelLocalToClearingOrder the level of the clearing order (could be 'Market')
     * @param levelOpposingClearingOrder the level of the opposing orders (could be 'Market')
     * @return the appropriate level that they should execute at
     */
    Level chooseLevelForPrice(final OrderSide side, Level levelLocalToClearingOrder, Level levelOpposingClearingOrder) throws IllegalArgumentException
    {
        if (levelLocalToClearingOrder.isMarket() && levelOpposingClearingOrder.isMarket())
            throw new IllegalArgumentException("Can't chose price for two market orders");
        if (levelLocalToClearingOrder.isMarket())
            return levelOpposingClearingOrder;
        if (levelOpposingClearingOrder.isMarket())
            return levelLocalToClearingOrder;

        // Levels are both not-best levels
        switch (side)
        {
            case BUY:
                if (levelLocalToClearingOrder.getLevel() < levelOpposingClearingOrder.getLevel())
                    throw new IllegalArgumentException("Can't chose price because clearing order can't clear, local: " + levelLocalToClearingOrder + ", opposing: " + levelOpposingClearingOrder);
                break;
            case SELL:
                if (levelLocalToClearingOrder.getLevel() > levelOpposingClearingOrder.getLevel())
                    throw new IllegalArgumentException("Can't chose price because clearing order can't clear, local: " + levelLocalToClearingOrder + ", opposing: " + levelOpposingClearingOrder);
                break;
        }
        return levelOpposingClearingOrder;
    }

    private void clear(Order ourOrder)
    {
        if (!orderManagerState.getCurrent().isClearingState())
            return;

        final OrderBook ourQueue = getQueue(ourOrder.getSide());
        final OrderBook otherQueue = getOtherQueue(ourOrder.getSide());
        // We will only continue while our level is above that of the opposing side (note that the best level can change)
        Level ourLevel = ourOrder.getLevel();
        PriorityOrders opposingOrders = otherQueue.getPriorityOrders();
        int opposingIndex = 0;
        while (otherQueue.otherLevelAllowsExecution(ourLevel) && opposingOrders.size() > 0 && ourOrder.getRemainingQuantity() > 0)
        {
            Order opposing = opposingOrders.get(opposingIndex);
            int executable = Math.min(ourOrder.getRemainingQuantity(), opposing.getRemainingQuantity());
            final LocalDateTime executionTime = timeMachine.toSimulationTime();
            Level executionLevel = getExecutionPrice(ourQueue, ourLevel, opposingOrders.getLevel());
            ourQueue.execute(ourOrder, executable, executionLevel, executionTime);
            if (otherQueue.execute(opposing, executable, executionLevel, executionTime) == null)
            {
                ++opposingIndex;
                // Did we eat through all the available orders
                if (opposingIndex >= opposingOrders.size())
                {
                    opposingOrders = otherQueue.getPriorityOrders();
                    opposingIndex = 0;
                }
            }
            print(false, executionTime, ourOrder, opposing, executionLevel, executable);
        }
    }

    private void print(final boolean inAuction, final LocalDateTime executionTime,  Order ourOrder, final Order opposing, final Level executionLevel, final int executable)
    {
        final String tradeFlag =  inAuction ? "A" : null;
        final String tradeId = "T" + ID.getAndIncrement();
        final Trade last = new Trade(this, tradeId, instrument, executionTime, tradeFlag, ourOrder.getClientId(), opposing.getClientId(), executionLevel, executable);
        if (!inAuction)
        {
            final AuctionTriggerEvent trigger =  marketManager.priceMonitor(last);
            if (trigger != null)
            {
                processAuctionEvent(trigger);
                // Inform the other listeners.  We told the market manager who we are so the trigger event it
                // returned should have our 'this' as the sender, and therefore we won't trigger the auction twice.
                eventDispatcher.dispatch(trigger);
            }
        }
        eventDispatcher.dispatch(last);
        // TODO: If there's no order appender we're testing (ideally should be mocked)
        tape.append(timeMachine, last);
        if (orderAppender != null) {
            tapePublisher.publish(timeMachine, last);
        }
        referencePrice = executionLevel;
    }

    /**
     * The agent is not the best judge of the state of the order, there certain amendments to the order
     * are not acceptable (since they apply a previous version of the order).  This requires us to consider
     * three versions of the order.
     * @param ourOrder the last version that the manager processed
     * @param newOrder the version received by the agent
     * @return a new instruction which combines our order and the new order
     */
    private Order mergeOrders(final Order ourOrder, final Order newOrder)
    {
        // If the order is already in the system you can only suggest a change, the internal state may not allow it
        if (ourOrder != null)
        {
            // We could have filled this order in the batch of 'orders' of new/replace traes and so this cancellation
            // may be too late
            if (newOrder.getState() == OrderState.PENDING_CANCEL && OrderState.isTerminal(ourOrder.getState())) {
                ourOrder.tooLate();
                return null;
            }

            if (!OrderState.isAmendPending(newOrder.getState()))
                return null;

            if (!ourOrder.getSide().equals(newOrder.getSide()))
                return null;

            final Order newInstruction = (Order)ourOrder.copy();
            newInstruction.setState(newOrder.getState());
            if (newOrder.getState() == OrderState.PENDING_REPLACE)
            {
                newInstruction.setQuantity(newOrder.getQuantity());
                newInstruction.setLevel(newOrder.getLevel());
            }
            return newInstruction;
        }
        return newOrder;
    }

    void registerOrder(final Order order)
    {
        orderLatest.put(order.getId(), order);
        if (orderAppender != null)
            orderAppender.append(timeMachine, order);
    }

    void processOrder(final Order newOrder)
    {
        try
        {
            //orderHistory.append(newOrder.toString()).append(System.lineSeparator());
            Order oldOrder = orderLatest.get(newOrder.getId());
            final Order newInstruction = mergeOrders(oldOrder, newOrder);
            if (newInstruction == null)
            {
                LOG.info("Order rejected because it can't be amended: " + newOrder);
                return;
            }
            if (orderManagerState.getCurrent().isClosed())
            {
                newOrder.rejected("Market closed");
                return;
            }
            assert(OrderState.isPending(newInstruction.getState()));
            OrderBook queue = getQueue(newInstruction.getSide());
            final OrderState preProcessState = newInstruction.getState();
            newInstruction.resetState(timeMachine.toSimulationTime());
            registerOrder(newInstruction);
            switch (preProcessState) {
                case PENDING_NEW:
                    queue.newOrder(newInstruction);
                    clear(newInstruction);
                    break;
                case PENDING_CANCEL:
                    queue.cancelOrder(newInstruction);
                    break;
                case PENDING_REPLACE:
                    queue.replaceOrder(oldOrder, newInstruction);
                    clear(newInstruction);
                    break;
                default:
                    LOG.error("Found a non pending order in the submit queue: " + preProcessState);
            }
        }
        catch (IllegalArgumentException illex)
        {
            LOG.error("Internal error: unable to apply order, because it would cause inconsistencies: " + newOrder, illex);
            throw illex;
        }
    }

    <T extends Event> void processEvents(final Collection<T> events)
    {
        synchronized (sync)
        {
            boolean enteredAuction = false;
            for (Event event : events)
            {
                if (event instanceof Order)
                {
                    if (enteredAuction)
                    {
                        if (LOG.isTraceEnabled())
                            LOG.trace("Skipping order because an auction was started: " + event);
                    }
                    else
                    {
                        processOrder((Order) event);
                    }
                }
                else if (event instanceof AuctionTriggerEvent)
                {
                    enteredAuction |= processAuctionEvent((AuctionTriggerEvent) event);
                }
                else
                {
                    throw new IllegalStateException("Unexpected event type: " + event);
                }
            }
        }
    }

    /**
     * This method is called by the manager prior to starting an auction or ending an auction prior to close
     */
    private void cancelAllOrders()
    {
        final LocalDateTime auctionStartTime = timeMachine.toSimulationTime();
        LOG.info("Cancel all orders @ " + timeMachine.formatTime(auctionStartTime));
        for (final Map.Entry<Long, Order> item : orderLatest.entrySet())
        {
            final Order order = item.getValue();
            final OrderBook queue = getQueue(order.getSide());
            // Change the state of the order to CANCELLED (will also notify agent)
            order.cancel(auctionStartTime, false);
            // Actually cancel
            queue.cancelOrder(order);
        }
    }

    /** Post-auction processing requires getting a final view on the outstanding orders and executing them
     * at the auction price.  If there is no price then the auction is post-ponsed
     * @return the uncrossing price reached
     */
    Level uncross()
    {
        try
        {
            final AuctionState auctionState = new AuctionState(timeMachine.toSimulationTime(), referencePrice, instrument, bidBook, offerBook);
            final Pair<Level, Long> interest = auctionState.getUncrossingInterest();
            if (interest.getSecond() == 0L)
                throw new AuctionException("No executable volume resulted from auction");

            final Level auctionPrice = interest.getFirst();
            final PriorityOrders bidOrders = bidBook.getAuctionPriorityOrders(interest.getFirst());
            final PriorityOrders offerOrders = offerBook.getAuctionPriorityOrders(interest.getFirst());
            long remainingVolume = interest.getSecond();
            LOG.debug("Uncrossing " + instrument.getTicker() + " " + QUANTITY_FORMAT.get().format(remainingVolume) + "@" +
                            PRICE_FORMAT.get().format(auctionPrice.getPrice()));
            while (remainingVolume > 0)
            {
                final Order bidOrder = bidOrders.take();
                final Order offerOrder = offerOrders.take();
                final int executableQuantity = Math.min(bidOrder.getRemainingQuantity(), offerOrder.getRemainingQuantity());
                bidBook.execute(bidOrder, executableQuantity, auctionPrice, auctionState.getTimestamp());
                offerBook.execute(offerOrder, executableQuantity, auctionPrice, auctionState.getTimestamp());
                print(true, auctionState.getTimestamp(), bidOrder, offerOrder, auctionPrice, executableQuantity);
                remainingVolume -= executableQuantity;
            }
            return auctionPrice;
        }
        catch (final AuctionException auxEx)
        {
            //TODO: If there is no auction state then the auction needs to be extended, this isn't implemented yet!
            throw new UnsupportedOperationException();
        }
    }

    private boolean processAuctionEvent(final AuctionTriggerEvent event)
    {
        Objects.requireNonNull(event);
        final MarketState currentState = orderManagerState.getCurrent();
        boolean enteredAuction = false;
        if (!event.getPreconditions().contains(currentState))
        {
            LOG.error("Request to trigger auction denied, because market is in: " + currentState + ", " + event);
        }
        else
        {
            switch (event.getPostcondition())
            {
                case AUCTION:
                    cancelAllOrders();
                    enteredAuction = true;
                    break;
                case CONTINUOUS:
                    uncross();
                    break;
                case CLOSED:
                    uncross();
                    cancelAllOrders();
                    // We now reset the time machine and re-schedule the auction to start the next day
                    timeMachine.start();
                    marketManager.restart();
                    level1 = new CachedValue<>(Level1.class, timeMachine, marketDataDelay, this::calculateLevel1, eventDispatcher, true);
                    break;
                default:
                    throw new IllegalStateException("Was expecting auction post condition to be AUCTION, CONTINUOUS or CLOSED but was: " + event.getPostcondition());
            }
            orderManagerState.accept(event.getPostcondition());
        }
        return enteredAuction;
    }

    <T extends Event> ArrayList<T> pendingItems(final BlockingQueue<T> queue, long waitMillis)
    {
        ArrayList<T> items = new ArrayList<>();
        try
        {
            final T item = queue.poll(waitMillis, TimeUnit.MILLISECONDS);
            if (item != null)
            {
                items.add(item);
                queue.drainTo(items);
            }
        }
        catch (InterruptedException ignored)
        {
        }
        return items;
    }

    private String printDepth() {
        StringBuilder depth = new StringBuilder();
        final Level1 currentLevel1 = level1.get();
        final OrderInterest bidVwap = bidBook.getVwapOfLimitOrders();
        final OrderInterest offerVwap = offerBook.getVwapOfLimitOrders();
        final OrderInterest marketBidInterest = bidBook.getMarketInterest();
        final OrderInterest marketOfferInterest = bidBook.getMarketInterest();
        depth.append(timeMachine.formatTime());
        depth.append(" - ");
        depth.append(orderManagerState.getCurrent().toString());
        if (currentLevel1 != null)
        {
            depth.append(" - ");
            depth.append("Touch(");
            depth.append(currentLevel1.getBid().formatInterest());
            depth.append(" - ");
            depth.append(currentLevel1.getOffer().formatInterest());
            depth.append(") / Vwap(");
            depth.append(bidVwap.formatInterest());
            depth.append(" - ");
            depth.append(offerVwap.formatInterest());
            depth.append(") / Market(");
            depth.append(marketBidInterest.getQuantity());
            depth.append(" - ");
            depth.append(marketOfferInterest.getQuantity());
            depth.append(")");
        }
        return depth.toString();
    }

    public void process()
    {
        processEvents(pendingItems(eventQueue, timeMachine.defaultWaitMillis()));
    }

    public void preProcess()
    {
        if (terminate.get())
            process();
        else
            throw new IllegalStateException("Can't preProcess after start is called.");
    }

    @Override
    public void run()
    {
        terminate.set(false);
        LocalDateTime last = timeMachine.toSimulationTime();
        final Duration touchDelay = Duration.of(1L, ChronoUnit.MINUTES);
        try
        {
            int startCount = 0;
            while (!terminate.get())
            {
                process();
                // Crude 1 minute (simulation time) sample for the logging console
                LocalDateTime now = timeMachine.toSimulationTime();
                int nowCount = timeMachine.getStartCount();
                if (nowCount != startCount)
                {
                    startCount = nowCount;
                    last = timeMachine.toSimulationTime();
                }
                else
                {
                    final LocalDateTime next = last.plus(touchDelay);
                    if (next.isBefore(now))
                    {
                        LOG.debug(printDepth());
                        last = next;
                    }
                }
            }
            eventDispatcher.removeListener(AuctionTriggerEvent.class, this);
        }
        catch (final Exception ex)
        {
            LOG.error("Encountered an irrecoverable order manager error: ", ex);
            //LOG.error("HISTORY: " + System.lineSeparator() + orderHistory);
        }
        finally
        {
            terminate.set(true);
        }
    }

    @Override
    public void notify(final Event event)
    {
        eventQueue.add(event);
    }
}
