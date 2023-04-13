package com.hackinghat.model;

import com.hackinghat.instrument.ConstantTickSizeToLevelConverter;
import com.hackinghat.model.Instrument;
import com.hackinghat.model.Level;
import com.hackinghat.order.OrderSide;
import junit.framework.TestCase;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class InstrumentTest extends TestCase
{

    private Instrument VOD;
    private BigDecimal limitPrice;
    private Level levelForLimitPrice;

    @Override
    protected void setUp() {
        VOD = new Instrument( "VOD.L", new Currency("GBP"), new ConstantTickSizeToLevelConverter(1, 100, 3));
        limitPrice = VOD.roundToTick(150.0);
        levelForLimitPrice = VOD.getLevel(limitPrice);
    }

    @Test
    public void testAdjustBestBy()
    {
        assertEquals(1, VOD.adjustBy(levelForLimitPrice, OrderSide.BUY, levelForLimitPrice.getLevel() - 1).getLevel());
        assertEquals(1, VOD.adjustBy(levelForLimitPrice, OrderSide.SELL, -levelForLimitPrice.getLevel() + 1).getLevel());
        assertEquals(limitPrice, VOD.adjustBy(levelForLimitPrice, OrderSide.BUY, 0).getPrice());
        assertEquals(limitPrice, VOD.adjustBy(levelForLimitPrice, OrderSide.SELL, 0).getPrice());

        assertEquals(levelForLimitPrice.getLevel()-1, VOD.adjustBy(levelForLimitPrice, OrderSide.BUY, 1).getLevel());
        assertEquals(levelForLimitPrice.getLevel()+1, VOD.adjustBy(levelForLimitPrice, OrderSide.BUY, -1).getLevel());
        assertEquals(levelForLimitPrice.getLevel()+1, VOD.adjustBy(levelForLimitPrice, OrderSide.SELL, 1).getLevel());
        assertEquals(levelForLimitPrice.getLevel()-1, VOD.adjustBy(levelForLimitPrice, OrderSide.SELL, -1).getLevel());
    }


}
