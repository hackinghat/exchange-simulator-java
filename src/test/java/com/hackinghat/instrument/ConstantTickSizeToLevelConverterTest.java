package com.hackinghat.instrument;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

public class ConstantTickSizeToLevelConverterTest {
    @Test
    public void testSetup() {
        Assert.assertThrows(IllegalArgumentException.class, () -> new ConstantTickSizeToLevelConverter(BigDecimal.ZERO));
        Assert.assertThrows(IllegalArgumentException.class, () -> new ConstantTickSizeToLevelConverter(-1, 2, 2));
        Assert.assertThrows(IllegalArgumentException.class, () -> new ConstantTickSizeToLevelConverter(1, 3, 2));
        Assert.assertThrows(IllegalArgumentException.class, () -> new ConstantTickSizeToLevelConverter(1, 1000, 2));
    }

    @Test
    public void testLargeTickSize() {
        final ConstantTickSizeToLevelConverter largeTickSize = new ConstantTickSizeToLevelConverter(1234, 1, 2);
        Assert.assertEquals(1234.0d, largeTickSize.getTickSize().doubleValue(), 1E-2);
        Assert.assertEquals(1234.0d, largeTickSize.roundToTick(new BigDecimal("1234")).doubleValue(), 1E-9);
    }

    @Test
    public void testSmallTickSize() {
        final ConstantTickSizeToLevelConverter tickSize = new ConstantTickSizeToLevelConverter(1, 10, 1);
        Assert.assertEquals(5.1d, tickSize.roundToTick(5.11).doubleValue(), 1E-3);
        Assert.assertEquals(51, tickSize.calculateLevelIndex(5.11).intValue());
        Assert.assertEquals(5.2d, tickSize.roundToTick(5.15).doubleValue(), 1E-3);
        final ConstantTickSizeToLevelConverter tinyTickSize = new ConstantTickSizeToLevelConverter(1, 100000, 5);
        Assert.assertEquals(5.00001d, tinyTickSize.roundToTick(5.00001).doubleValue(), 1E-9);
        Assert.assertEquals(5.00001d, tinyTickSize.roundToTick(5.000012).doubleValue(), 1E-9);
        Assert.assertEquals(5.00002d, tinyTickSize.roundToTick(5.000015).doubleValue(), 1E-9);
    }

    @Test
    public void testUnityTickSize() {
        final ConstantTickSizeToLevelConverter tickSize = new ConstantTickSizeToLevelConverter(BigDecimal.ONE);
        Assert.assertEquals(100, tickSize.calculateLevel(100.1d).getLevel());
        Assert.assertEquals(100.0, tickSize.calculateLevel(100.1d).getPrice().doubleValue(), 1E-9);
        Assert.assertEquals(100d, tickSize.calculatePrice(100).doubleValue(), 1E-9);
    }
}
