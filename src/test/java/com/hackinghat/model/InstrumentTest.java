package com.hackinghat.model;

import com.hackinghat.order.OrderSide;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InstrumentTest  {

    private Instrument VOD;
    private float limitPrice;
    private Level levelForLimitPrice;

    protected void setUp() {
        VOD = new Instrument("VOD.L", new Currency("GBP"), new ConstantTickSizeToLevelConverter(1, 100, 3));
        limitPrice = VOD.roundToTick(150.0f);
        levelForLimitPrice = VOD.getLevel(limitPrice);
    }

    @Test
    public void testAdjustBestBy() {
        assertEquals(1, VOD.adjustBy(levelForLimitPrice, OrderSide.BUY, levelForLimitPrice.getLevel() - 1).getLevel());
        assertEquals(1, VOD.adjustBy(levelForLimitPrice, OrderSide.SELL, -levelForLimitPrice.getLevel() + 1).getLevel());
        assertEquals(limitPrice, VOD.adjustBy(levelForLimitPrice, OrderSide.BUY, 0).getPrice(), levelForLimitPrice.getTickSize());
        assertEquals(limitPrice, VOD.adjustBy(levelForLimitPrice, OrderSide.SELL, 0).getPrice(), levelForLimitPrice.getTickSize());

        assertEquals(levelForLimitPrice.getLevel() - 1, VOD.adjustBy(levelForLimitPrice, OrderSide.BUY, 1).getLevel());
        assertEquals(levelForLimitPrice.getLevel() + 1, VOD.adjustBy(levelForLimitPrice, OrderSide.BUY, -1).getLevel());
        assertEquals(levelForLimitPrice.getLevel() + 1, VOD.adjustBy(levelForLimitPrice, OrderSide.SELL, 1).getLevel());
        assertEquals(levelForLimitPrice.getLevel() - 1, VOD.adjustBy(levelForLimitPrice, OrderSide.SELL, -1).getLevel());
    }


}
