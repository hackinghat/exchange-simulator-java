package com.hackinghat.model;

import com.hackinghat.order.OrderSide;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Objects;

import static com.hackinghat.util.Formatters.PRICE_FORMAT;

/**
 * The level ties the price to a position in the order book.  A price therefore is the combination of
 * the desired price and the allowed tick size at the level.
 *
 * TODO: The level needs to incorporate a sliding scale of tick sizes so that larger prices have
 * correspondingly larger tick sizes.  In the real world this is dependent on the average daily
 * turnover of the stock but for our purposes we are assuming this is controlled outside of our order
 * book (i.e. we're given a different tick regime at order book construction time).
 *
 * Note, there can be no doubt when it comes to the price about floating point rounding.  The price is
 * what is defined by the level
 */
public class Level
{
    /**
     * The 'market' level allows orders to be collected in the same structure as the limit order
     */
    public  static final Level MARKET = new Level();

    public static final Comparator<Level> BUY_COMPARATOR = Level.makeComparator(OrderSide.BUY);
    public static final Comparator<Level> SELL_COMPARATOR = Level.makeComparator(OrderSide.SELL);

    private int         level;
    private boolean     market;
    private BigDecimal  price;

    Level(final BigDecimal price, final int level)
    {
        Objects.requireNonNull(price);
        if (level <= 0)
            throw new IllegalArgumentException("Level must be greater than zero");
        if (BigDecimal.ZERO.compareTo(price) >= 0)
            throw new IllegalArgumentException("Price must be greater than zero");
        this.price = price;
        this.level = level;
        this.market = false;
    }

    Level()
    {
        this.price = BigDecimal.ZERO;
        this.level = 0;
        this.market = true;
    }

    public boolean isMarket()
    {
        return market;
    }
    public void setMarket(final boolean market) { this.market = market; }

    public BigDecimal getPrice()
    {
        return price;
    }
    public void setPrice(final BigDecimal price) { this.price = price;}

    public int getLevel() { return level; }
    public void setLevel(final int level) { this.level = level; }

    @Override
    public int hashCode()
    {
        return market ? -1 : level;
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof Level))
            return false;
        final Level otherLevel = Level.class.cast(other);
        return hashCode() == otherLevel.hashCode();
    }

    /**
     * Used to navigate the levels by queue type, therefore the next level away from the best on the BID side is the level below,
     * conversely the next level away from the touch on the SELL side is the level above
     * @return a number that can be multiplied by the level to adjust it
     */
    public static int sideToDirection(@Nonnull final OrderSide queueSide)
    {
        return queueSide == OrderSide.BUY ? -1 : 1;
    }

    /**
     * Whether this level is better than the other level
     * @param otherLevel the level to compare it to
     * @param queueSide the interpretation of 'worse' varies with the side
     * @return true if this level is worse than the otherLevel
     */
    public boolean betterThan(final Level otherLevel, final OrderSide queueSide)
    {
        return ticksBetweenSameSide(otherLevel, queueSide) < 0;
    }

    /**
     * Whether this level is better than or equal to the other level
     * @param otherLevel the level to compare it to
     * @param queueSide the interpretation of 'better' varies with the side
     * @return true if this level is better than or equal to the otherLevel
     */
    public boolean betterThanOrEqual(final Level otherLevel, final OrderSide queueSide)
    {
        return ticksBetweenSameSide(otherLevel, queueSide) <= 0;
    }

    /**
     * Whether this level is strictly worse than the other level.  As a buyer a level is worse than this if it is more expensive (i.e. level index is higher)
     * @param otherLevel the level to compare it to
     * @param queueSide the interpretation of 'worse' varies with the side
     * @return true if this level is worse than the otherLevel
     */
    public boolean worseThan(final Level otherLevel, final OrderSide queueSide)
    {
        return ticksBetweenSameSide(otherLevel, queueSide) > 0;
    }

    /**
     * Whether this level is worse than or equal to.  As a buyer a level is worse than this if it is more expensive (i.e. level index is higher)
     * @param otherLevel the level to compare it to
     * @param queueSide the interpretation of 'worse' varies with the side
     * @return true if this level is worse than the otherLevel
     */
    public boolean worseThanOrEqual(final Level otherLevel, final OrderSide queueSide)
    {
        return ticksBetweenSameSide(otherLevel, queueSide) >= 0;
    }

    public int ticksBetweenSameSide(final Level otherLevel, final OrderSide queueSide)
    {
        return (level - otherLevel.getLevel()) * sideToDirection(queueSide);
    }

    public static int ticksBetweenBidAndOffer(final Level bid, final Level offer)
    {
        return offer.getLevel() - bid.getLevel();
    }

    public int absoluteticksBetween(final Level otherLevel)
    {
        Objects.requireNonNull(otherLevel);
        return Math.abs(getLevel() - otherLevel.getLevel());
    }

    private static Comparator<Level> makeComparator(final OrderSide orderSide)
    {
        return (o1, o2) -> o1.levelCompare(orderSide, o2);
    }

    public static Comparator<Level> comparator(final OrderSide side)
    {
        switch (side)
        {
            case BUY:
                return BUY_COMPARATOR;
            case SELL:
                return SELL_COMPARATOR;
            default:
                throw new IllegalArgumentException("Unexpected side: " + side);
        }
    }

    private int sign(int i) { return Integer.compare(i, 0); }

    /**
     * The ordering of levels plays a major role in auction pricing where we need to ensure that we
     * have the orders in the sort-order that places them as 'market order' -> 'best limit' ... 'worst limit'
     * by side.
     * @param queueSide the side for which the queue relates
     * @param o the side to compare against
     * @return -1 if this level is 'better', 1 if the other is better and 0 if the same.  This appears counter-intuitive
     * but it ensures that the best level is at the top (i.e. before all others) of the natural ordering
     */
    public int levelCompare(final OrderSide queueSide, final Level o)
    {
        // Both levels are markets
        if (isMarket() && o.isMarket())
            return 0;
        // Our level is market (other is not)
        if (isMarket())
            return -1;
        // Our level is not market (other is)
        if (o.isMarket())
            return 1;
        // Calculate which is greater
        return sign(sideToDirection(queueSide) * (getLevel() - o.getLevel()));
    }

    @Override
    public String toString()
    {
        return "Level(" + (market ? "MARKET" : PRICE_FORMAT.get().format(price)) + ")";
    }
}


