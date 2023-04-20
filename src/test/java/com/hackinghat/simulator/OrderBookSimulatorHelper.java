package com.hackinghat.simulator;

import com.hackinghat.agent.Agent;
import com.hackinghat.agent.NullAgent;
import com.hackinghat.instrument.ConstantTickSizeToLevelConverter;
import com.hackinghat.model.Currency;
import com.hackinghat.model.Instrument;
import com.hackinghat.model.Level;
import com.hackinghat.order.MarketState;
import com.hackinghat.order.Order;
import com.hackinghat.order.OrderSide;
import com.hackinghat.orderbook.auction.AuctionEventReceiver;
import com.hackinghat.orderbook.auction.AuctionSchedule;
import com.hackinghat.orderbook.auction.AuctionScheduleItem;
import com.hackinghat.orderbook.auction.MarketManager;
import com.hackinghat.util.NotSoRandomSource;
import com.hackinghat.util.SyncEventDispatcher;
import com.hackinghat.util.TimeMachine;
import com.hackinghat.util.component.AbstractComponent;
import com.hackinghat.util.component.Component;
import com.hackinghat.util.mbean.MBeanType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;

/**
 * This class is for testing purposes and sets up a working test-simulator that uses a synchronous dispatcher that we can
 * move between market states at will (this is useful to test agent behaviour in response to market changes).
 *
 * This class then delegates the necessary calls to the underlying objects.
 */
@MBeanType(description = "Order book simulator helper")
public class OrderBookSimulatorHelper extends AbstractComponent
{
    public static final Logger LOG = LogManager.getLogger(OrderBookSimulatorHelper.class);

    public static Duration ONE_SECOND = Duration.of(1L, ChronoUnit.SECONDS);

    private final Instrument              inst;
    private final TimeMachine             timeMachine;
    private final NotSoRandomSource       randomSource;
    private final OrderBookSimulatorImpl  simulator;
    private final Agent                   testAgent;
    private final Level                   referenceLevel;
    private final AuctionSchedule         auctionSchedule;
    private final SyncEventDispatcher     eventDispatcher;
    private final AuctionEventReceiver    auctionEventReceiver;

    private static int              clientId = 0;

    public Instrument getInst() {
        return inst;
    }

    public TimeMachine getTimeMachine() {
        return timeMachine;
    }

    public NotSoRandomSource getRandomSource() {
        return randomSource;
    }

    public OrderBookSimulatorImpl getSimulator() {
        return simulator;
    }

    public OrderBookSimulatorHelper()
    {
        super("OrderBookSimulatorHelper");
        inst = new Instrument( "VOD", new Currency("GBP"), new ConstantTickSizeToLevelConverter(1, 100, 3));
        randomSource = new NotSoRandomSource();
        // We need a delta of 1.0 otherwise time won't advance
        timeMachine = new TimeMachine(LocalTime.of(0, 0, 0), 1.0);
        timeMachine.start();
        referenceLevel = inst.getLevel(1.5);
        eventDispatcher = require(new SyncEventDispatcher(timeMachine));
        auctionSchedule = AuctionSchedule.makeLSESchedule(LocalDate.now());
        auctionEventReceiver = new AuctionEventReceiver(eventDispatcher);
        MarketManager marketManager = require(new MarketManager(referenceLevel, 0.1, Duration.of(5L, ChronoUnit.MINUTES), timeMachine, eventDispatcher, auctionSchedule));
        marketManager.start();
        simulator = require(new OrderBookSimulatorImpl(inst, marketManager, eventDispatcher, randomSource, timeMachine, Duration.ZERO, null, false));
        testAgent = require(new NullAgent(0L, inst, randomSource, timeMachine, "A1", eventDispatcher));
    }

    @Override
    public void close() {
        super.close();
        // Close all the dependencies (because once we're done nothing should run)
        Arrays.stream(getRequirementsOf()).forEach(Component::close);
    }

    /**
     * Preheat the order book with a bid order and a sell order that would either cross or give a spread around the current reference level
     * @param ticks number of ticks away from the reference level
     * @param bidQuantity the bid quantity to put on the bid side
     * @param offerQuantity the offer quantity to put on the offer side
     * @param better if true the orders will cross, otherwise they will leave a spread
     * @return the two orders that were created
     */
    public List<Order> submitOrder(final int ticks, final int bidQuantity, final int offerQuantity, final boolean better) {
        return submitOrder(referenceLevel, ticks, bidQuantity, offerQuantity, better);
    }

    /**
     * Preheat the order book with a bid order and a sell order that would either cross or give a spread around the reference level
     * @param referenceLevel the reference level to use
     * @param ticks number of ticks away from the reference level
     * @param bidQuantity the quantity to put on the bid side, zero places no order
     * @param offerQuantity the offer to put on the offer side, zero places no order
     * @param better if true the orders will cross, otherwise they will leave a spread
     * @return the orders sent to the simulator
     */
    public List<Order> submitOrder(final Level referenceLevel, final int ticks, final int bidQuantity, final int offerQuantity, final boolean better) {
        Function<OrderSide, BigDecimal> preheatAdjuster = (final OrderSide s) ->
                (better ? inst.betterOnBook(referenceLevel, s, ticks) :
                          inst.worsenOnBook(referenceLevel, s, ticks)).getPrice();
        final List<Order> orders = new ArrayList<>();
        if (bidQuantity > 0)
                orders.add(new Order("PRE-B-" + clientId++, OrderSide.BUY, inst, preheatAdjuster.apply(OrderSide.BUY), bidQuantity, testAgent, timeMachine));
        if (offerQuantity > 0)
                orders.add(new Order("PRE-S-" + clientId++, OrderSide.SELL, inst, preheatAdjuster.apply(OrderSide.SELL), offerQuantity, testAgent, timeMachine));
        simulator.add(orders);
        simulator.process();
        return orders;
    }

    /**
     * Cancel the given orders so long as they are still known to the agent
     * @param orders the orders to cancel
     */
    public void cancelOrders(final Collection<Order> orders) {
        for (final Order order : orders) {
            if (testAgent.hasOutstandingOrder(order)) {
                order.cancel(timeMachine.toSimulationTime());
                simulator.add(order);
            } else {
                LOG.debug("Order is no longer outstanding with agent: " + testAgent.getName() + ", '" + order + "'");
            }
        }
        simulator.process();
    }

    public void setDoubleSource(final double[] doubleSource) {
        randomSource.setDoubleSource(doubleSource);
    }

    public void setIntSource(final int[] intSource)
    {
        randomSource.setIntSource(intSource);
    }

    public void resetRandomSource()
    {
        randomSource.reset();
    }

    public void transitionClosedToAuction() throws Exception
    {
        final int auctionEvents = auctionEventReceiver.getEvents().size();
        assertEquals(MarketState.CLOSED, simulator.getLevel1().getMarketState());
        final AuctionScheduleItem opening = auctionSchedule.getAuctionScheduleItem(AuctionSchedule.OPENING);
        eventDispatcher.executeLapsedTasks(opening.getAdjustedStartTime(Duration.of(1L, ChronoUnit.SECONDS)));
        // There should now be an auction event waiting to be processed that will move us into 'AUCTION'
        assertEquals(auctionEvents+1, auctionEventReceiver.getEvents().size());
        simulator.process();
        assertEquals(MarketState.AUCTION, simulator.getLevel1().getMarketState());
    }

    public void transitionAuctionToContinuous() throws Exception
    {
        final int auctionEvents = auctionEventReceiver.getEvents().size();
        assertEquals(MarketState.AUCTION, simulator.getLevel1().getMarketState());
        final AuctionScheduleItem opening = auctionSchedule.getAuctionScheduleItem(AuctionSchedule.OPENING);
        eventDispatcher.executeLapsedTasks(opening.getAdjustedEndTime(Duration.of(1L, ChronoUnit.SECONDS)));
        // There should now be an auction event waiting to be processed that will move us into 'CONTINUOUS'
        assertEquals(auctionEvents+1, auctionEventReceiver.getEvents().size());
        simulator.process();
        assertEquals(MarketState.CONTINUOUS, simulator.getLevel1().getMarketState());
    }
}
