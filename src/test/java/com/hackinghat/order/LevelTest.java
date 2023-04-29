package com.hackinghat.order;


import com.hackinghat.model.ConstantTickSizeToLevelConverter;
import com.hackinghat.model.Currency;
import com.hackinghat.model.Instrument;
import com.hackinghat.model.Level;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class LevelTest {
    private Instrument VOD;
    private float limitPrice;
    private Level levelForLimitPrice;

    public static Level makeLevel(final float price, final Instrument instrument) {
        return instrument.getLevel(price);
    }

    @Before
    public void setUp() {
        VOD = new Instrument("VOD.L", new Currency("GBP"), new ConstantTickSizeToLevelConverter(1, 100, 3));
        limitPrice = VOD.roundToTick(150.0f);
        levelForLimitPrice = VOD.getLevel(limitPrice);
    }

    @Test
    public void testLevelBetter() {
        final Level a = makeLevel(100.0f, VOD);
        final Level b = makeLevel(101.0f, VOD);
        Assert.assertTrue(a.worseThan(b, OrderSide.BUY));
        Assert.assertTrue(b.worseThan(a, OrderSide.SELL));
    }

    @Test
    public void testLevelCompare() {
        final Level L100 = makeLevel(100.0f, VOD);
        final Level L101 = makeLevel(101.0f, VOD);
        final Level market = VOD.getMarket();
        assertEquals(0, market.levelCompare(OrderSide.BUY, market));
        assertEquals(-1, L101.levelCompare(OrderSide.BUY, L100));
        assertEquals(0, L101.levelCompare(OrderSide.BUY, L101));
        assertEquals(1, L100.levelCompare(OrderSide.BUY, L101));

        assertEquals(1, L101.levelCompare(OrderSide.SELL, L100));
        assertEquals(0, L101.levelCompare(OrderSide.SELL, L101));
        assertEquals(-1, L100.levelCompare(OrderSide.SELL, L101));

        // Market orders are better than anything (other levels are < MARKET)
        assertEquals(-1, market.levelCompare(OrderSide.BUY, L100));
        assertEquals(-1, market.levelCompare(OrderSide.SELL, L100));

        // Limit orders are always 'worse' than markets (MARKET > LIMIT)
        assertEquals(1, L100.levelCompare(OrderSide.BUY, market));
        assertEquals(1, L100.levelCompare(OrderSide.SELL, market));
    }

    @Test
    public void testInvalidLevelBuy() {
        try {
            VOD.worsenOnBook(levelForLimitPrice, OrderSide.BUY, levelForLimitPrice.getLevel() + 1);
            Assert.fail();
        } catch (IllegalArgumentException illex) {
            // EXPECTED
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeLevel() {
        makeLevel(-1.0f, VOD);
    }

    @Test
    public void testInvalidLevelSell() {
        try {
            VOD.betterOnBook(levelForLimitPrice, OrderSide.SELL, levelForLimitPrice.getLevel() + 1);
            Assert.fail();
        } catch (IllegalArgumentException illex) {
            // EXPECTED
        }
    }

}
