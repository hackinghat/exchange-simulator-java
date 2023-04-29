package com.hackinghat.statistic;

public class SumStatistic extends LaggingStatistic {
    public SumStatistic(final String name, final double initialValue, final int averageLength) {
        super(name, initialValue, averageLength, averageLength);
    }

    @Override
    protected void _update(double next) {
        this.value += (next - buffer[index]);
    }
}


