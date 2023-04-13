package com.hackinghat.statistic;

public interface SampledStatistic<Source> extends Statistic
{
    void update(final Source item);
}
