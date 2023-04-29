package com.hackinghat.order;

import com.hackinghat.model.Instrument;
import com.hackinghat.model.Level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class PriorityOrders {
    private final List<Order> orders;
    private Level level;

    /**
     * This constructor is intended for use by the auction uncrossing process where we know the level already
     * we just want all the orders arranged into level-time priority order
     *
     * @param auctionLevel     the level the auction will uncross from usually calculated by an {@linkplain com.hackinghat.orderbook.auction.AuctionState}
     * @param orderLimitQueues the queues that we will take orders from
     */
    public PriorityOrders(final Level auctionLevel, final Collection<OrderLimitQueue> orderLimitQueues) {
        orders = new ArrayList<>();
        level = auctionLevel;
        orderLimitQueues.forEach(q -> orders.addAll(q.getOrders()));
        orders.sort(Order::compareTo);
    }

    /**
     * Combine
     *
     * @param instrument
     * @param orderLimitQueues
     */
    public PriorityOrders(final Instrument instrument, final Level opposingLevel, final Collection<OrderLimitQueue> orderLimitQueues) {
        orders = new ArrayList<>();
        level = null;
        for (final OrderLimitQueue orderLimitQueue : orderLimitQueues) {
            Objects.requireNonNull(orderLimitQueue);
            if (orderLimitQueue.getOrders().size() > 0) {
                orders.addAll(orderLimitQueue.getOrders());
                if (opposingLevel.isMarket()) {
                    if (!orderLimitQueue.getLevel().isMarket()) {
                        level = orderLimitQueue.getLevel();
                    }
                } else {
                    if (level == null || level.betterThan(orderLimitQueue.getLevel(), orderLimitQueue.getSide())) {
                        level = orderLimitQueue.getLevel();
                    }
                }
            }
        }
        if (level == null) {
            level = instrument.getMarket();
        }
        orders.sort(Order::compareTo);
    }

    public List<Order> getOrders() {
        return orders;
    }

    public Level getLevel() {
        return level;
    }

    public Order get(int index) {
        return orders.get(index);
    }

    public long size() {
        return orders.size();
    }

    /**
     * Returns the first order from the priority order list which has some executable volume
     *
     * @return an order
     * @throws IllegalStateException if {@link #take()} is called with no remaining quantity
     */
    public Order take() {
        if (orders.size() == 0)
            throw new IllegalStateException("No remaining orders to draw from!");

        if (orders.get(0).getRemainingQuantity() == 0) {
            orders.remove(0);
            return take();
        }
        return orders.get(0);
    }

    public long getTotalVolume() {
        return orders.stream().mapToLong(Order::getQuantity).sum();
    }
}

