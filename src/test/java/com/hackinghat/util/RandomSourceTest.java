package com.hackinghat.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RandomSourceTest {
    @Test
    public void testPower() {
        RandomSource randomSource = new RandomSourceImpl(0L);
        int[] test = new int[10240];
        int N = 100000;
        for (int i = 0; i < N; ++i) {
            int idx = randomSource.nextPower(1, 2.3) - 1;
            if (idx >= test.length)
                System.out.println(idx);
            else
                test[idx]++;
        }
        for (int i = 0; i < 100; ++i) {
            System.out.println(i + "\t" + test[i]);
        }
    }

    @Test
    public void testExponential() {
        double LAMBDA = 5;
        double EXPECTED_MEAN = 1 / LAMBDA;
        double EXPECTED_VAR = 1 / (LAMBDA * LAMBDA);

        RandomSource randomSource = new RandomSourceImpl(0L);
        double meantotal = 0;
        double vartotal = 0;
        int n = 100000;
        // TODO: Use calculus to figure out how big 'n' has to be to guarantee the estimate is within a tolerance of 0.01
        for (int i = 0; i < n; ++i) {
            double next = randomSource.nextExponential(LAMBDA) / n;
            meantotal += randomSource.nextExponential(LAMBDA) / n;
            vartotal += Math.pow(EXPECTED_MEAN - next, 2) / n;
        }
        assertEquals(EXPECTED_MEAN, meantotal, 0.01d);
        assertEquals(EXPECTED_VAR, vartotal, 0.01d);
    }
}
