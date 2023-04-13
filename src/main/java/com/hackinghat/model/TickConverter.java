package com.hackinghat.model;

import com.hackinghat.model.Level;

import java.math.BigDecimal;

/**
 * Expressing prices (which in most markets can be decimals) as levels provides an exchange where floating
 * point arithmetic is not really required. The {@see TickConverter} is used to provide a mechanism to switch between
 * prices and their integer equivalent levels.
 */
public interface TickConverter {
    Integer calculateLevelIndex(final BigDecimal price);
    BigDecimal calculatePrice(final Integer levelIndex);
    BigDecimal roundToTick(Number price);

    default Integer calculateLevelIndex(final Number price) {
        return calculateLevelIndex(roundToTick(price));
    }

    default Level calculateLevel(final Number price) {
        final BigDecimal roundPrice = roundToTick(price);
        return new Level(roundPrice, calculateLevelIndex(roundPrice));
    }
}
