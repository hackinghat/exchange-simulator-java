package com.hackinghat.orderbook;

import com.hackinghat.model.Instrument;
import com.hackinghat.model.Level;
import com.hackinghat.order.MarketState;
import com.hackinghat.order.OrderSide;
import com.hackinghat.util.Copyable;
import com.hackinghat.util.Timestampable;


/**
 * The top level for each side, and the interest at the level
 */
public interface Level1 extends Timestampable, Copyable
{
    OrderInterest   getBid();
    OrderInterest   getOffer();
    MarketState     getTouchState();
    MarketState     getMarketState();

    default OrderInterest getInterest(final OrderSide side)
    {
        switch (side)
        {
            case BUY:
                return getBid();
            case SELL:
                return getOffer();
            default:
                throw new IllegalArgumentException("Unexpected side");
        }
    }

    default Level getPrice(final OrderSide side)
    {
        return getInterest(side).getLevel();
    }

    default Integer getSpread()
    {
        final Level bid = getPrice(OrderSide.BUY);
        final Level offer = getPrice(OrderSide.SELL);
        if (bid == null || bid.isMarket() || offer == null || offer.isMarket())
            return null;
        return offer.absoluteticksBetween(bid);
    }

    default Level getMid(final Instrument instrument)
    {
        final Level bid = getPrice(OrderSide.BUY);
        final Integer spread = getSpread();
        return spread == null ? null : instrument.betterOnBook(bid, OrderSide.BUY, getSpread() / 2);
    }

    default int ticksBetweenBidAndOffer() throws InvalidMarketStateException
    {
        if (!getTouchState().hasSpread())
            throw new InvalidMarketStateException("Market is not valid");
        final OrderInterest bid = getBid();
        final OrderInterest offer = getOffer();
        if (bid == null || bid.getLevel().isMarket() || offer == null || offer.getLevel().isMarket())
            throw new InvalidMarketStateException("Market is not valid");
        return Level.ticksBetweenBidAndOffer(bid.getLevel(), offer.getLevel());
    }

    default String format() {
        return String.format("%d %f - %f %d", getBid().getQuantity(), getBid().getLevel().getPrice().doubleValue(),
                getOffer().getLevel().getPrice().doubleValue(), getOffer().getQuantity());
    }
}
