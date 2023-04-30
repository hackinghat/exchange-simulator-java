package com.hackinghat.orderbook;

import com.hackinghat.order.OrderSide;
import com.hackinghat.util.Copyable;
import com.hackinghat.util.TimeMachine;
import com.hackinghat.util.Timestampable;

import javax.annotation.Nonnull;
import java.time.LocalDateTime;
import java.util.Collection;

public class FullDepth implements Timestampable, Copyable<FullDepth> {
    @Nonnull
    final Collection<OrderInterest> bidDepth;
    @Nonnull
    final Collection<OrderInterest> offerDepth;
    @Nonnull
    private LocalDateTime simulationTime;

    public FullDepth(final OrderBook bid, final OrderBook offer, final TimeMachine timeMachine) {
        // Executable levels clones the underlying order interests
        bidDepth = bid.getExecutableLevels();
        offerDepth = offer.getExecutableLevels();
        simulationTime = timeMachine.toSimulationTime();
    }

    @Nonnull
    public Collection<OrderInterest> getBidDepth() {
        return bidDepth;
    }

    @Nonnull
    public Collection<OrderInterest> getOfferDepth() {
        return offerDepth;
    }

    public Collection<OrderInterest> getDepth(final OrderSide side) {
        switch (side) {
            case BUY:
                return getBidDepth();
            case SELL:
                return getOfferDepth();
            default:
                throw new IllegalArgumentException("Unexpected side: " + side);
        }
    }

    @Override
    public LocalDateTime getTimestamp() {
        return simulationTime;
    }

    @Override
    public void setTimestamp(LocalDateTime timestamp) {
        this.simulationTime = timestamp;
    }

    @Override
    public FullDepth cloneEx() throws CloneNotSupportedException {
        return (FullDepth) clone();
    }
}
