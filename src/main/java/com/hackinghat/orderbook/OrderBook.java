package com.hackinghat.orderbook;

import com.hackinghat.model.Instrument;
import com.hackinghat.model.Level;
import com.hackinghat.order.*;
import com.hackinghat.util.component.AbstractComponent;
import com.hackinghat.util.mbean.MBeanAttribute;
import com.hackinghat.util.mbean.MBeanType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents a collection of queues that represent the orders at the various limit/market prices on this side of the book
 * There are some concurrency considerations to consider with this type.
 * <p>
 * Firstly the orders that it works on are 'copies' of the orders maintained by the agents.  This allows
 * the agents to have a different version of the order to that known by the exchange.  This is what you'd expect
 * to see in the real market.
 * <p>
 * Agents should know how to process responses from the exchange that indicate their view of the order is not
 * consistent with that of the exchange.  For example, an order becomes fully filled while the agent has submitted
 * a cancel.
 * <p>
 * TODO: We need a way to reliably lock a book when both book sides are being modified/accessed
 */
@MBeanType(description = "Order Book")
public class OrderBook extends AbstractComponent {
    private static final Logger LOG = LogManager.getLogger(OrderBook.class);
    private final OrderInterest marketInterest;
    private final OrderSide queueSide;
    private final Instrument instrument;
    private final NavigableMap<Level, OrderLimitQueue> limitQueue;

    // ReadWriteLock's are only strictly 'fair'!
    private final ReadWriteLock lock = new ReentrantReadWriteLock(true);
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    public OrderBook(final OrderSide queueSide, final Instrument instrument) {
        super("OrderBook-" + instrument.getTicker() + "-" + queueSide);
        this.queueSide = queueSide;
        this.instrument = instrument;
        this.limitQueue = new TreeMap<>(Level.comparator(queueSide));
        this.marketInterest = new OrderInterest(queueSide, instrument.getMarket(), 0, 0);
    }

    private static Order getOurOrder(Collection<Order> orders, Order order) {
        for (Order search : orders) {
            if (search.equals(order))
                return search;
        }
        return null;
    }

    @MBeanAttribute(description = "Level depth")
    public int getLevelDepth() {
        return getExecutableLevels().size();
    }

    public OrderSide getQueueSide() {
        return queueSide;
    }

    OrderInterest getInterest(final Level level) {
        readLock.lock();
        try {
            final OrderLimitQueue orderLimitQueue = limitQueue.get(level);
            return orderLimitQueue == null ? null : orderLimitQueue.getInterest();
        } finally {
            readLock.unlock();
        }
    }

    boolean otherLevelAllowsExecution(Level otherSideLevel) {
        final Level bestLevel = getBestLimitQueue().getLevel();
        if (bestLevel.isMarket() && otherSideLevel.isMarket())
            return false;
        if (bestLevel.isMarket() || otherSideLevel.isMarket())
            return true;
        return (queueSide == OrderSide.BUY ? bestLevel.getLevel() >= otherSideLevel.getLevel() : bestLevel.getLevel() <= otherSideLevel.getLevel());
    }

    private OrderLimitQueue getOrAddLimitQueue(final Level level) {
        OrderLimitQueue limit = limitQueue.get(level);
        if (limit == null) {
            limit = new OrderLimitQueue(level, queueSide);
            limitQueue.put(level, limit);
        }
        return limit;
    }

    public boolean newOrder(Order newOrder) {
        if (newOrder == null || newOrder.getId() == null)
            throw new IllegalArgumentException("Can't accept order: id is null");
        if (newOrder.getSide() != queueSide)
            throw new IllegalArgumentException("Can't accept order: Wrong side");
        if (OrderState.isPending(newOrder.getState()))
            throw new IllegalArgumentException("Can't accept order: Order is pending");

        writeLock.lock();
        try {
            final Level level = newOrder.getLevel();
            final OrderLimitQueue limit = getOrAddLimitQueue(level);
            if (limit.getOrders().contains(newOrder))
                return false;
            limit.add(newOrder);
            return true;

        } finally {
            writeLock.unlock();
        }
    }

    boolean replaceOrder(final Order oldOrder, final Order newOrder) {
        return cancelOrder(oldOrder) || newOrder(newOrder);
    }

    boolean cancelOrder(final Order oldOrder) {
        if (oldOrder == null || oldOrder.getId() == null || OrderState.isPending(oldOrder.getState()))
            return false;

        writeLock.lock();
        try {
            final Collection<Order> orderSet = getOrders(oldOrder.getLevel());
            Order ourOrder = getOurOrder(orderSet, oldOrder);
            if (ourOrder == null) {
                oldOrder.tooLate();
                return false;
            } else {
                final OrderLimitQueue limitQueue = getLimitQueue(oldOrder.getLevel());
                limitQueue.remove(oldOrder, oldOrder.getRemainingQuantity());
                return true;
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Should be called with the read or write lock held.
     *
     * @param price the price level to retrieve the orders for
     * @return the current orders with remaining quantity at the requested price
     */
    Collection<Order> getOrders(final Level price) {
        return getLimitQueue(price).getOrders();
    }

    private OrderLimitQueue getLimitQueue(final Level price) {
        return getOrAddLimitQueue(price);
    }

    /**
     * Can be called without the lock held.
     * This method is intended for calculating the aggregate volume at a price (i.e.
     * assumming the order quantities in array order will give the total executable volume
     * at that price or 'better'.  Because the limit queue is stored in 'level' order in
     * theory we could simply return the keys of the map, except for the fact that the map contains
     * empty levels (i.e. levels where no orders exist) and that we don't want to expose the inner
     * objects of the {@see OrderBook}.
     *
     * @return a collection of cloned interests for this side in the marketable order.
     */
    public Collection<OrderInterest> getExecutableLevels() {
        readLock.lock();
        try {
            final List<OrderInterest> executable = new ArrayList<>();
            for (final Map.Entry<Level, OrderLimitQueue> levelAndQ : limitQueue.entrySet()) {
                final OrderInterest interest = levelAndQ.getValue().getInterest();
                if (interest.getCount() > 0)
                    executable.add(interest.copy());
            }
            return executable;
        } finally {
            readLock.unlock();
        }
    }

    /***
     * Execute 'quantity' of the order (by first locating it in our structure and then executing it.   If the quantity exhausts
     * the remaining order quantity then we remove it from our structures and return null.
     * @param order the order we wish to execute some quantity of
     * @param quantity the quantity we want to execute
     * @param simulationTime the time that the execution was agreed
     * @return null if there is no more to execute, otherwise the partially filled order
     */
    Order execute(final Order order, final int quantity, final Level executionPrice, final LocalDateTime simulationTime) {
        Objects.requireNonNull(order);
        Objects.requireNonNull(executionPrice);
        assert (quantity > 0);

        writeLock.lock();
        try {
            OrderLimitQueue limitQueue = getLimitQueue(order.getLevel());
            return limitQueue.execute(order, quantity, executionPrice, simulationTime) == null ? null : order;
        } finally {
            writeLock.unlock();
        }
    }

    OrderLimitQueue getMarketQueue() {
        return getOrAddLimitQueue(instrument.getMarket());
    }

    /**
     * The best queue is the first non-market queue in 'side' order that has an active order
     *
     * @return the level at the top of the book or the market queue if there is none
     */
    OrderLimitQueue getBestLimitQueue() {
        readLock.lock();
        try {
            for (Map.Entry<Level, OrderLimitQueue> limit : limitQueue.entrySet()) {
                if (limit.getKey().isMarket())
                    continue;

                final OrderLimitQueue orderLimitQueue = limit.getValue();
                if (orderLimitQueue.getOrders().size() > 0)
                    return orderLimitQueue;
            }
            return getMarketQueue();
        } finally {
            readLock.unlock();
        }
    }

    OrderInterest getBestInterest() {
        readLock.lock();
        try {
            OrderInterest bestInterest = getBestLimitQueue().getInterest();
            return bestInterest.getTouchInterest(getMarketQueue().getInterest());
        } finally {
            readLock.unlock();
        }
    }

    PriorityOrders getPriorityOrders(final Level opposingLevel) {
        final OrderLimitQueue bestQueue = getBestLimitQueue();
        if (bestQueue.getLevel().isMarket())
            return new PriorityOrders(instrument, opposingLevel, Collections.singleton(bestQueue));
        else
            return new PriorityOrders(instrument, opposingLevel, Arrays.asList(getMarketQueue(), bestQueue));
    }

    PriorityOrders getAuctionPriorityOrders(final Level auctionLevel) {
        final List<OrderLimitQueue> available = new ArrayList<>();
        for (Map.Entry<Level, OrderLimitQueue> limit : limitQueue.entrySet()) {
            final Level level = limit.getKey();
            if (level.isMarket() || level.betterThanOrEqual(auctionLevel, getQueueSide()))
                available.add(limit.getValue());
        }
        return new PriorityOrders(auctionLevel, available);
    }

    /**
     * TODO: This method returns the order interest at the VWAP price.  This price must be rounded
     * to the nearest tick to be represented by an interest.  Which makes it no-longer a VWAP price!
     *
     * @return the VWAP represented as an {@see OrderInterest}
     */
    OrderInterest getVwapOfLimitOrders() {
        readLock.lock();
        try {
            long quantity = 0;
            double value = 0.d;
            int vwapCount = 0;
            for (Map.Entry<Level, OrderLimitQueue> item : limitQueue.entrySet()) {
                if (!item.getKey().isMarket()) {
                    OrderInterest interest = item.getValue().getInterest();
                    if (interest.getCount() > 0) {
                        quantity += interest.getQuantity();
                        value += interest.getValue(0.f);
                        vwapCount += interest.getCount();
                    }
                }
            }
            if (quantity == 0) {
                return marketInterest;
            } else {
                final Level vwapLevel = instrument.getLevel((float) (value / quantity));
                return new OrderInterest(queueSide, vwapLevel, quantity, vwapCount);
            }
        } finally {
            readLock.unlock();
        }
    }

    OrderInterest getMarketInterest() {
        readLock.lock();
        try {
            return getMarketQueue().getInterest();
        } finally {
            readLock.unlock();
        }
    }

    public int size() {
        return limitQueue.size();
    }

    @Override
    public String toString() {
        return "OrderBook{" +
                "queueSide=" + queueSide +
                ", best=" + getBestLimitQueue().getInterest() + '}';
    }
}
