package com.hackinghat.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DynamicTickToLevelConverterTest {
    @Test
    public void testZero() {
        DynamicTickToLevelConverter defaultConverter = DynamicTickToLevelConverter.DEFAULT_DYNAMIC_TICK_TO_LEVEL_CONVERTER;
        assertEquals(0, defaultConverter.calculateLevelIndex(0.f));
        assertEquals(1, defaultConverter.calculateLevelIndex(defaultConverter.getTickSize(0.f)));
    }
}
