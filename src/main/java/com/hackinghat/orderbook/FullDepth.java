package com.hackinghat.orderbook;

import com.hackinghat.order.OrderSide;
import com.hackinghat.util.Copyable;
import com.hackinghat.util.TimeMachine;
import com.hackinghat.util.Timestampable;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import javax.annotation.Nonnull;

public class FullDepth implements Timestampable, Copyable
{
    @Nonnull
    private LocalDateTime simulationTime;
    @Nonnull
    final Collection<OrderInterest> bidDepth;
    @Nonnull
    final Collection<OrderInterest> offerDepth;

    public FullDepth(final OrderBook bid, final OrderBook offer, final TimeMachine timeMachine)
    {
        // Executable levels clones the underlying order interests
        bidDepth = bid.getExecutableLevels();
        offerDepth = offer.getExecutableLevels();
        simulationTime = timeMachine.toSimulationTime();
    }

    public Collection<OrderInterest> getBidDepth() { return bidDepth; }
    public Collection<OrderInterest> getOfferDepth() { return offerDepth; }

    public Collection<OrderInterest> getDepth(final OrderSide side)
    {
        switch (side)
        {
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
        return FullDepth.class.cast(clone());
    }
}
