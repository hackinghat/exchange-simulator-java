package com.hackinghat.model;

/**
 * Expressing prices (which in most markets can be decimals) as levels provides an exchange where floating
 * point arithmetic is not really required. The {@see TickConverter} is used to provide a mechanism to switch between
 * prices and their integer equivalent levels.
 */
public interface TickConverter {
    float calculatePrice(final Integer levelIndex);

    float roundToTick(final float price);

    float getTickSize(final float price);

    int calculateLevelIndex(final float price);

    default Level calculateLevel(final float price) {
        final float tickSize = getTickSize(price);
        final float roundPrice = roundToTick(price);
        return new Level(roundPrice, calculateLevelIndex(roundPrice), tickSize);
    }
}
