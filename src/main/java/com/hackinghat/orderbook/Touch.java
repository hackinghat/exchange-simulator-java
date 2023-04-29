package com.hackinghat.orderbook;

import com.hackinghat.model.Level;
import com.hackinghat.order.MarketState;

import javax.annotation.Nonnull;
import java.time.LocalDateTime;
import java.util.Objects;

public class Touch implements Level1 {
    @Nonnull
    private final OrderInterest bid;
    @Nonnull
    private final OrderInterest offer;
    @Nonnull
    private final MarketState touchState;
    @Nonnull
    private final MarketState marketState;
    @Nonnull
    private LocalDateTime simulationTime;


    public Touch(@Nonnull final LocalDateTime simulationTime, @Nonnull final MarketState marketState, @Nonnull final OrderInterest bid, @Nonnull final OrderInterest offer) {
        Objects.requireNonNull(bid);
        Objects.requireNonNull(offer);

        this.bid = bid;
        this.offer = offer;
        this.simulationTime = simulationTime;
        this.marketState = marketState;
        int overlap = bid.getLevel().isMarket() || offer.getLevel().isMarket() ? 0 : Level.ticksBetweenBidAndOffer(this.bid.getLevel(), this.offer.getLevel());
        if (overlap == 0)
            this.touchState = MarketState.CHOICE;
        else if (overlap < 0)
            this.touchState = MarketState.BACK;
        else
            this.touchState = MarketState.CONTINUOUS;
    }

    @Override
    public OrderInterest getBid() {
        return bid;
    }

    @Override
    public OrderInterest getOffer() {
        return offer;
    }

    @Override
    public MarketState getTouchState() {
        return touchState;
    }

    @Override
    public MarketState getMarketState() {
        return marketState;
    }

    @Override
    public LocalDateTime getTimestamp() {
        return simulationTime;
    }

    @Override
    public void setTimestamp(final LocalDateTime simulationTime) {
        this.simulationTime = simulationTime;
    }

    @Override
    public String toString() {
        return "Touch{" +
                "time=" + simulationTime +
                ", bid=" + bid +
                ", offer=" + offer +
                ", marketState=" + marketState +
                '}';
    }

    @Override
    public Touch cloneEx() throws CloneNotSupportedException {
        return Touch.class.cast(clone());
    }
}
