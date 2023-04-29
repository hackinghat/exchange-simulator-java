package com.hackinghat.order;

import com.hackinghat.model.Level;
import com.hackinghat.orderbook.OrderInterest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Objects;

/**
 * Encapsulates the collection of orders at the same price point.  For the purposes of this simulator
 * a market order is at a special level of {@link }.
 */
public class OrderLimitQueue {
    private final static boolean DEBUG = false;
    private final static Logger LOG = LogManager.getLogger(OrderLimitQueue.class);

    @Nonnull
    private final OrderInterest interest;
    @Nonnull
    private final ArrayDeque<Order> orders;
    @Nonnull
    private final Level level;
    @Nonnull
    private final OrderSide side;

    public OrderLimitQueue(final Level level, OrderSide queueSide) {
        Objects.requireNonNull(level);
        Objects.requireNonNull(queueSide);
        this.interest = new OrderInterest(queueSide, level, 0L);
        this.orders = new ArrayDeque<>();
        this.level = level;
        this.side = queueSide;
    }

    @Nonnull
    public OrderInterest getInterest() {
        return interest;
    }

    @Nonnull
    public Level getLevel() {
        return level;
    }

    @Nonnull
    public ArrayDeque<Order> getOrders() {
        return orders;
    }

    public OrderSide getSide() {
        return side;
    }

    private void throwIfInvalid(final Order order) {
        if (DEBUG) {
            if (order.isMarket() != level.isMarket())
                throw new IllegalArgumentException("Internal error: Order and limit types don't match!");
            if (!order.isMarket() && !order.getLevel().equals(interest.getLevel()))
                throw new IllegalArgumentException("Internal error: Order and limit price mismatch");
        }
    }

    public void add(Order order) {
        throwIfInvalid(order);
        interest.add(order.getRemainingQuantity());
        orders.addLast(order);
        verifyInterest();
    }

    public void remove(Order order, final int quantity) {
        throwIfInvalid(order);
        if (orders.remove(order))
            interest.remove(quantity);
        else
            throw new IllegalArgumentException("Unknown order: " + order);
        verifyInterest();
    }

    void verifyInterest() {
        if (DEBUG) {
            int count = 0;
            long quantity = 0;
            for (final Order order : orders) {
                quantity += order.getRemainingQuantity();
                count += 1;
            }
            final OrderInterest interest = getInterest();
            assert (quantity == interest.getQuantity() && count == interest.getCount());
        }
    }

    /***
     * Execute the required quantity from our order (this could mutate the order)
     * @param order the order we wish to execute some quantity of
     * @param quantity the quantity to execute
     * @param price the price to execute at
     * @param simulationTime time of change
     * @return null if there's no remaining quantity otherwise the supplied order is returned
     */
    public Order execute(final Order order, final int quantity, final Level price, final LocalDateTime simulationTime) {
        throwIfInvalid(order);
        for (final Order ourOrder : orders) {
            if (ourOrder.equals(order)) {
                ourOrder.fillQuantity(quantity, price, simulationTime);
                if (OrderState.isTerminal(ourOrder.getState())) {
                    remove(ourOrder, quantity);
                    verifyInterest();
                    return null;
                } else {
                    interest.reduce(quantity);
                    verifyInterest();
                    return order;
                }
            }
        }
        throw new IllegalArgumentException("Internal error: unknown order: " + order);
    }

    @Override
    public String toString() {
        return "OrderLimitQueue{" +
                "interest=" + interest +
                ", norders=" + interest.getCount() +
                '}';
    }
}
