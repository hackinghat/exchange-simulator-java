package com.hackinghat.statistic;

public class ArithmeticMeanStatistic extends LaggingStatistic {
    public ArithmeticMeanStatistic(final String name, final double initialValue, final int averageLength) {
        super(name, initialValue, averageLength, averageLength);
    }

    @Override
    protected void _update(double next) {
        value += (next / length - buffer[index] / length);
    }
}


