package com.hackinghat.statistic;

/**
 * Calculates the variance statistic by using a constant number of floating point operations regardless
 * of the size of the sample.
 */
public class VarianceStatistic extends SumStatistic {
    ArithmeticMeanStatistic mean;
    SumStatistic sum;

    public VarianceStatistic(final String name, final double initialValue, final int averageLength) {
        super(name, initialValue, averageLength);
        mean = new ArithmeticMeanStatistic(name + " mean", initialValue, averageLength);
        sum = new SumStatistic(name + " sum", initialValue, averageLength);
    }

    @Override
    public void update(final double value) {
        super.update(value * value);
        mean.update(value);
        sum.update(value);
    }

    @Override
    public double getValue() {
        // E(x_i^2) - 2 mu E(x_i) + n mu^2
        return (value - 2.0 * mean.value * sum.value + (mean.value * mean.value * length)) / (length - 1);
    }
}
