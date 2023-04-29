package com.hackinghat.order;

import java.util.EnumSet;

public enum MarketState {
    /* The state prior to the beginning and at the end of the trading day */
    CLOSED,
    /* In this state the order manager is clearing the order book preparing for an auction, orders
     * received are rejected */
    AUCTION,
    /* In this state the final uncrossing price is chosen and the book cleared and trades entered. During
       this phase new orders are rejected.
     */
    CONTINUOUS,
    /* In this state the best offer is less than the best bid (or the best bid is greater than the best offer)
       this non-normal market condition indicates the presence of an oder at the top of the book that is constrained
       in some-way (such as immediate or cancel or iceberg etc)
     */
    BACK,
    /* In this state the best offer is equal to the best bid.  This non-normal market condition indicates the
       presence of an oder at the top of the book that is constrained in some-way (such as immediate or cancel or iceberg etc)
     */
    CHOICE;

    private static final EnumSet<MarketState> CLEARING_STATES = EnumSet.of(MarketState.CONTINUOUS, MarketState.BACK, MarketState.CHOICE);

    /**
     * Only a continuous market state will have a valid spread.   The others will either have a spread
     * of zero (CHOICE / AUCTION) or the spread will be negative (BACK)
     *
     * @return true if the market state can be interpreted as having a spread
     */
    public boolean hasSpread() {
        return this == MarketState.CONTINUOUS;
    }

    public boolean isClosed() {
        return this == MarketState.CLOSED;
    }

    public boolean isClearingState() {
        return CLEARING_STATES.contains(this);
    }
}
