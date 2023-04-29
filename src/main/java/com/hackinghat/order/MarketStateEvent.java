package com.hackinghat.order;

import com.hackinghat.util.Event;

import java.time.LocalDateTime;

public class MarketStateEvent extends Event {
    private final MarketState oldMarketState;
    private final MarketState newMarketState;

    public MarketStateEvent(Object sender, final LocalDateTime stateChangeTime, final MarketState oldMarketState, final MarketState newMarketState) {
        super(sender, stateChangeTime);
        if (oldMarketState == newMarketState)
            throw new IllegalArgumentException("Market state has not changed, so this state is not valid");
        this.oldMarketState = oldMarketState;
        this.newMarketState = newMarketState;
    }

    public MarketState getOldMarketState() {
        return oldMarketState;
    }

    public MarketState getNewMarketState() {
        return newMarketState;
    }
}
