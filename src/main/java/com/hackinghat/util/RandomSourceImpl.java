package com.hackinghat.util;

import java.util.Random;

public class RandomSourceImpl implements RandomSource
{
    private final Random random;

    public RandomSourceImpl(long seed)
    {
        random = new Random(seed);
    }

    public int nextInt(int bound) { return random.nextInt(bound); }

    public long nextLong() { return random.nextLong(); }

    public double nextDouble() { return random.nextDouble(); }

    public boolean nextUniform(double threshold) { return random.nextDouble() <= threshold; }

    public int nextPoisson(double lambda) {
        double L = Math.exp(-lambda);
        double p = 1.0;
        int k = 0;

        do {
            k++;
            p *= nextDouble();
        } while (p > L);

        return k - 1;
    }

    public double nextExponential(double lambda)
    {
        return Math.log(1-random.nextDouble())/(-lambda);
    }

    public int nextBinomial(int n, double p) {
        int x = 0;
        for(int i = 0; i < n; i++) {
            if(nextDouble() < p)
                x++;
        }
        return x;
    }

    public double nextLogNormal(final double mean, final double stdev)
    {
        return Math.exp(random.nextGaussian()*stdev + mean);
    }

    public int nextPower(int xmin, double alpha)
    {
        // From: https://arxiv.org/pdf/0706.1062.pdf - an approximation to a power law distribution (good enough for now)
        return (int)Math.floor(((double)xmin - 0.5)*Math.pow(1.0-nextDouble(), -1./(alpha-1))+0.5);
    }

    public int getBernoulli(double p)
    {
        return nextBinomial(1, p);
    }

}
