package com.hackinghat.util;

public class NotSoRandomSource implements RandomSource
{
    private int[]       intSource;
    private int         intSourceIndex;

    private double[]    doubleSource;
    private int         doubleSourceIndex;

    public NotSoRandomSource()
    {
        intSourceIndex = 0;
        doubleSourceIndex = 0;
    }

    public static RandomSource makeZeroRandom()
    {
        NotSoRandomSource notSoRandomSource = new NotSoRandomSource();
        notSoRandomSource.setDoubleSource(new double[] { 0.0 });
        notSoRandomSource.setIntSource(new int[] { 0 });
        return notSoRandomSource;
    }

    public void setIntSource(int[] intSource) {
        this.intSource = intSource;
        this.intSourceIndex = -1;
    }

    public void setDoubleSource(double[] doubleSource) {
        this.doubleSource = doubleSource;
        this.doubleSourceIndex = -1;
    }

    public void reset() {
        intSourceIndex = -1;
        doubleSourceIndex = -1;
    }

    @Override
    public int nextInt(int bound) {
        if (intSource.length == 0)
            throw new IllegalArgumentException("No ints defined");
        return intSource[intSourceIndex = (intSourceIndex + 1)%intSource.length];
    }

    @Override
    public double nextDouble() {
        if (doubleSource.length == 0)
            throw new IllegalArgumentException("No doubles defined");
        return doubleSource[doubleSourceIndex = (doubleSourceIndex + 1)%doubleSource.length];
    }

    @Override
    public boolean nextUniform(double threshold) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int nextPoisson(double lambda) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int nextBinomial(int n, double p) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double nextLogNormal(double mean, double stdev) {
        return nextDouble();
    }

    @Override
    public int nextPower(int xmin, double alpha) {
        return nextInt(xmin);
    }

    @Override
    public int getBernoulli(double p) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double nextExponential(double lambda) { return nextDouble(); }
}
