package com.hackinghat.model;

import org.junit.Assert;
import org.junit.Test;

public class ConstantTickSizeToLevelConverterTest {
    @Test
    public void testSetup() {
        Assert.assertThrows(IllegalArgumentException.class, () -> new ConstantTickSizeToLevelConverter(0.f));
        Assert.assertThrows(IllegalArgumentException.class, () -> new ConstantTickSizeToLevelConverter(-1, 2, 2));
        Assert.assertThrows(IllegalArgumentException.class, () -> new ConstantTickSizeToLevelConverter(1, 3, 2));
        Assert.assertThrows(IllegalArgumentException.class, () -> new ConstantTickSizeToLevelConverter(1, 1000, 2));
    }

    @Test
    public void testLargeTickSize() {
        final ConstantTickSizeToLevelConverter largeTickSize = new ConstantTickSizeToLevelConverter(1234, 1, 2);
        Assert.assertEquals(1234.f, largeTickSize.getTickSize(), 1E-2);
        Assert.assertEquals(1234.f, largeTickSize.roundToTick(1234.f), largeTickSize.getTickSize());
    }

    @Test
    public void testSmallTickSize() {
        final ConstantTickSizeToLevelConverter tickSize = new ConstantTickSizeToLevelConverter(1, 10, 1);
        Assert.assertEquals(5.1f, tickSize.roundToTick(5.11f), tickSize.getTickSize());
        Assert.assertEquals(51, tickSize.calculateLevelIndex(5.11f));
        Assert.assertEquals(5.2f, tickSize.roundToTick(5.15f), tickSize.getTickSize());
        final ConstantTickSizeToLevelConverter tinyTickSize = new ConstantTickSizeToLevelConverter(1, 100000, 5);
        Assert.assertEquals(5.00001f, tinyTickSize.roundToTick(5.00001f), tinyTickSize.getTickSize());
        Assert.assertEquals(5.00001f, tinyTickSize.roundToTick(5.000012f), tinyTickSize.getTickSize());
        Assert.assertEquals(5.00002f, tinyTickSize.roundToTick(5.000015f), tinyTickSize.getTickSize());
        Assert.assertEquals(5.00002f, tinyTickSize.roundToTick(5.000015f), tinyTickSize.getTickSize());

    }

    @Test
    public void testUnityTickSize() {
        final ConstantTickSizeToLevelConverter tickSize = new ConstantTickSizeToLevelConverter(1.f);
        Assert.assertEquals(100, tickSize.calculateLevel(100.1f).getLevel());
        Assert.assertEquals(100.0f, tickSize.calculateLevel(100.1f).getPrice(), tickSize.getTickSize());
        Assert.assertEquals(100.f, tickSize.calculatePrice(100), tickSize.getTickSize());
    }
}
