package com.hackinghat.instrument;

import com.hackinghat.model.TickConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.hackinghat.util.Formatters.PRICE_FORMAT;

public class ConstantTickSizeToLevelConverter implements TickConverter
{
    private  final BigDecimal tickSize;

    /**
     * Creates a constant size tick converter where the tick size is a fraction of q/r.
     * @param numerator combined with the denominator to arrive the size of a tick
     * @param denominator combined with the numerator to arrive at the size of a tick
     */
    public ConstantTickSizeToLevelConverter(int numerator, int denominator, int scale) {
        this(atSameScale(numerator, denominator, scale));
    }

    private static BigDecimal atSameScale(int numerator, int denominator, int scale) {
        final BigDecimal atScale = new BigDecimal(numerator).divide(new BigDecimal(denominator), scale, RoundingMode.HALF_EVEN);
        if (atScale.compareTo(BigDecimal.ZERO) == 0)
            throw new IllegalArgumentException("Tick size will be zero if " + numerator + "/" + denominator + " is used at scale: " + scale);
        final BigDecimal nextScale = new BigDecimal(numerator).divide(new BigDecimal(denominator), scale+1, RoundingMode.HALF_EVEN);
        if (atScale.compareTo(nextScale) != 0) {
            throw new IllegalArgumentException("Tick size is irrational!");
        }
        return atScale;
    }

    public ConstantTickSizeToLevelConverter(final BigDecimal tickSize) {
        if (tickSize.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Illegal tick size");
        }
        this.tickSize = tickSize;
    }

    public BigDecimal getTickSize() { return tickSize; }

    @Override
    public Integer calculateLevelIndex(final BigDecimal price) {
        BigDecimal bdp = price.divide(tickSize, RoundingMode.HALF_EVEN);
        bdp = bdp.setScale(0, RoundingMode.HALF_EVEN);
        return bdp.intValue();
    }

    @Override
    public BigDecimal calculatePrice(final Integer levelIndex)
    {
        if (levelIndex < 0)
            throw new IllegalArgumentException("Attempt to adjust to a level below zero");
        if (levelIndex == 0)
            return BigDecimal.ZERO;
        return new BigDecimal(levelIndex).multiply(tickSize);
    }

    public BigDecimal roundToTick(Number price)
    {
        if (price == null)
            return null;
        BigDecimal bdp = BigDecimal.valueOf(price.doubleValue());
        bdp = bdp.divide(tickSize);
        bdp = bdp.setScale(0, RoundingMode.HALF_EVEN);
        return bdp.multiply(tickSize);
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName() +
                "(tickSize=" +
                PRICE_FORMAT.get().format(tickSize) +
                ")";
    }
}
