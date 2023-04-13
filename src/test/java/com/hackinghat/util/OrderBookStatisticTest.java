package com.hackinghat.util;

import com.hackinghat.statistic.*;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OrderBookStatisticTest
{
    @Test
    public void testSumStatistic()
    {
        final SumStatistic ss = new SumStatistic("testSum", 0.0, 10);
        assertEquals(0.0d, ss.getValue(), 1E-12);
        for (int i = 0; i < ss.getLength()*2; ++i)
            ss.update(0.1*i);

        // Since our sum statistic holds the last 10 entries it will retain entries 10..19. The sum of i where
        // i is between 10 & 19 is (10+19)*5 = 145, multiplied by 0.1 = 14.5
        assertEquals(14.5d, ss.getValue(), 1E-12);
    }

    @Test
    public void testAveragingStatistic()
    {
        final ArithmeticMeanStatistic as = new ArithmeticMeanStatistic("testMean", 0.0, 10);
        assertEquals(0.0d, as.getValue(), 1E-12);
        for (int i = 0; i < as.getLength(); ++i)
            as.update(0.1*i);
        assertEquals(0.45d, as.getValue(), 1E-12);
    }

    @Test
    public void testLaggingStatistic()
    {
        final LaggingStatistic ls = new LaggingStatistic("testLag", Double.NaN, 10, 2);
        assertTrue(Double.isNaN(ls.getValue()));
        for (int i = 0; i < ls.getLength(); ++i)
        {
            ls.update(0.1 * i);
            if (i > 1)
                assertEquals(0.1 * (i-2), ls.getValue(), 1E-12);
        }
    }

    @Test
    public void testVarianceStatistic()
    {
        final VarianceStatistic vs = new VarianceStatistic("estVar", 0.0, 10);
        for (int i = 0; i < vs.getLength(); ++i)
            vs.update(0.1 * i);
        assertEquals(0.091666667, vs.getValue(), 1E-6);
    }

    @Test
    public void testNoLevel1()
    {
        final OrderBookStatistic orderBookStatistic = new OrderBookStatistic(0, 0);
        orderBookStatistic.update(null);
    }

}
