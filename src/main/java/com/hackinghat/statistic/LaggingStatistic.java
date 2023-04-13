package com.hackinghat.statistic;

import java.text.DecimalFormat;

public class LaggingStatistic
{
    final String      name;
    final double[]    buffer;
    final double      initialValue;
    final int         length;
    final int         lag;

    int         index;
    double      value;

    public LaggingStatistic(final String name, final double initialValue, final int averageLength, int lag)
    {
        this.buffer = new double[averageLength];
        this.name = name;
        this.initialValue = initialValue;
        this.length = averageLength;
        for (int i = 0; i < averageLength; ++i)
            this.buffer[i] = initialValue;
        this.value = initialValue;
        this.index = 0;
        this.lag = lag;
    }

    public void update(final double value)
    {
        int posn = index % length;
        _update(value);
        buffer[posn] = value;
        index = (index + 1) % length;
    }

    protected void _update(double next) {
        this.value = buffer[(index + length - lag) % length];
    }

    public double getValue() {
        return value;
    }

    public int getLength() {
        return length;
    }

    public String format(final ThreadLocal<DecimalFormat> format) {
        return format.get().format(getValue());
    }
}

