package com.hackinghat.util;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class StatisticsTest {

    @Test
    public void testMean() {
        assertEquals(0.0, Statistics.mean(-1.0, 0.0, 1), 1E-6);
    }

    @Test
    public void testVariance() {
        assertEquals(2.5, Statistics.variance(-1.0, -2.0, 0.0, 1, 2), 1E-6);
        // Population variance will tend to unde-restimate
        assertEquals(2.0, Statistics.varianceP(-1.0, -2.0, 0.0, 1, 2), 1E-6);
    }

    @Test
    public void testKurtosis() {
        assertEquals(1.7, Statistics.kurtosis_(-1.0, -2.0, 0.0, 1, 2), 1E-6);
        // Calculate kurtosis with a bias adjustment (normal behaviour)
        assertEquals(1.8, Statistics.kurtosis(-1.0, -2.0, 0.0, 1, 2), 1E-6);
    }

    @Test
    public void testSkewness() {
        assertEquals(0.395870, Statistics.skewness_(-1.0, -2.0, 0.0, 1.0, 3.0), 1E-6);
        assertEquals(0.590128, Statistics.skewness(-1.0, -2.0, 0.0, 1.0, 3.0), 1E-6);

    }

}
