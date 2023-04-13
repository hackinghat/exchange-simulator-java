package com.hackinghat.orderbook;

import com.hackinghat.order.OrderSide;
import org.junit.Test;

import java.math.BigDecimal;

import static junit.framework.TestCase.assertEquals;

public class OrderInterestTest {

    public static void checkInterest(OrderSide side, OrderInterest interest, BigDecimal expectedPrice, int expectedCount, long expectedVolume)
    {
        checkInterest(side, interest, expectedPrice.doubleValue(), expectedCount, expectedVolume);
    }
    
    public static void checkInterest(OrderSide side, OrderInterest interest, double expectedPrice, int expectedCount, long expectedVolume)
    {
        assertEquals("price break on " + side, expectedPrice, interest.getLevel().getPrice().doubleValue(), 1E-6);
        assertEquals("count break on " + side, expectedCount, interest.getCount());
        assertEquals("volume break on " + side, expectedVolume, interest.getQuantity());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoAddEmpty()
    {
        OrderInterest.EMPTY.add(10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoReduceEmpty()
    {
        OrderInterest.EMPTY.reduce(10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoRemoveEmpty()
    {
        OrderInterest.EMPTY.remove(10);
    }
}
