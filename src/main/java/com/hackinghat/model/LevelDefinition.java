package com.hackinghat.model;

import com.hackinghat.order.OrderSide;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.hackinghat.util.Formatters.PRICE_FORMAT;
import static com.hackinghat.model.Level.MARKET;

/** Defines how a level is defined, it contains a mapping between a price and a level index which
 * is arrived at by a TickConverter, there will be a level definition per stock.  This definition
 * could be shared with other stocks with the same tick level definition.
 */
public class LevelDefinition {

    private final Map<Integer, Level>     indexToLevelMap;
    //private final Map<BigDecimal,    Level>     priceToLevelMap;
    private final TickConverter tickConverter;

    LevelDefinition(final TickConverter tickConverter)
    {
         this.indexToLevelMap = new ConcurrentHashMap<>();
         //this.priceToLevelMap = new ConcurrentHashMap<>();
         this.tickConverter = tickConverter;
    }

    static Level makeMarket()
    {
        return MARKET;
    }

    public TickConverter getTickConverter()
    {
        return tickConverter;
    }

    Level makeLimit(final BigDecimal price)
    {
        final int i = tickConverter.calculateLevelIndex(price);

        if (tickConverter.calculateLevelIndex(tickConverter.calculatePrice(i)) != i)
            throw new IllegalArgumentException("Was expecting the tick converter to behave symmetrically for price: " + PRICE_FORMAT.get().format(price));

        final Level level = indexToLevelMap.get(i);
        if (level == null)
        {
            final Level newLevel = new Level(price, i);
            indexToLevelMap.put(i, newLevel);
            return newLevel;
        }
        return level;
    }

    /**
     * Given a side, return the current level for that side adjusted by the number of levels.
     * @param queueSide the queue side we we want to adjust this price by
     * @param nLevels the number of levels to adjust the price by, if level is the best price then if
     *                nLevels is +ve this betters the price (from the perspective of the order owner, so
     *                a BUY order price will go down a SELL order will go up),
     *                if nLevels is negative it worsens the price for the order owner.
     * @return the adjusted level
     */
    Level adjustBy(Level level, OrderSide queueSide, int nLevels)
    {
        final int directionInt = Level.sideToDirection(queueSide);
        final int newLevel = level.getLevel() + (nLevels * directionInt);
        return makeLimit(tickConverter.calculatePrice(newLevel));
    }
}
