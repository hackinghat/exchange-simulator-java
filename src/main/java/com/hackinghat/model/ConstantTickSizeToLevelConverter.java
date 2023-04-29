package com.hackinghat.model;

import com.hackinghat.util.FloatUtil;

import static com.hackinghat.util.Formatters.PRICE_FORMAT;

public class ConstantTickSizeToLevelConverter implements TickConverter {
    private final float tickSize;

    /**
     * Creates a constant size tick converter where the tick size is a fraction of q/r.
     *
     * @param numerator   combined with the denominator to arrive the size of a tick
     * @param denominator combined with the numerator to arrive at the size of a tick
     * @param dps         used to check the numerator/denominator will give a valid tick size
     */
    public ConstantTickSizeToLevelConverter(int numerator, int denominator, int dps) {
        this(atSameScale(numerator, denominator, dps));
    }

    public ConstantTickSizeToLevelConverter(final float tickSize) {
        if (tickSize <= 0) {
            throw new IllegalArgumentException("Illegal tick size");
        }
        this.tickSize = tickSize;
    }

    private static float atSameScale(int numerator, int denominator, int dps) {
        final float atScale = FloatUtil.round((float) numerator / denominator, dps);
        if (atScale == 0.f)
            throw new IllegalArgumentException("Tick size will be zero if " + numerator + "/" + denominator + " is used at scale: " + dps);
        final float nextScale = FloatUtil.round((float) numerator / denominator, dps + 1);
        if (atScale != nextScale) {
            throw new IllegalArgumentException("Tick size is irrational!");
        }
        return atScale;
    }

    public float getTickSize() {
        return tickSize;
    }

    @Override
    public int calculateLevelIndex(final float price) {
        return Math.round(price / tickSize);
    }


    @Override
    public float calculatePrice(final Integer levelIndex) {
        if (levelIndex < 0)
            throw new IllegalArgumentException("Attempt to adjust to a level below zero");
        if (levelIndex == 0)
            return 0.f;
        return tickSize * levelIndex;
    }

    @Override
    public float roundToTick(float price) {
        if (price == 0.f)
            return 0;
        return Math.round(price / tickSize) * tickSize;
    }

    @Override
    public float getTickSize(float price) {
        return tickSize;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                "(tickSize=" +
                PRICE_FORMAT.get().format(tickSize) +
                ")";
    }
}
