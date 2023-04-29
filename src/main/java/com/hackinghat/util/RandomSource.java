package com.hackinghat.util;


public interface RandomSource {
    int nextInt(int bound);

    double nextDouble();

    boolean nextUniform(double threshold);

    int nextPoisson(double lambda);

    int nextBinomial(int n, double p);

    double nextLogNormal(final double mean, final double stdev);

    int nextPower(int xmin, double alpha);

    int getBernoulli(double p);

    double nextExponential(double lambda);
}
