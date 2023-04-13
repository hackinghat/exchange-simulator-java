package com.hackinghat.util;

import com.hackinghat.statistic.Statistic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class StatisticsAppenderTestHelper extends AbstractStatisticsAppender
{
    private ArrayList<String>         lines;
    private ArrayList<Statistic>      sourceData;

    public StatisticsAppenderTestHelper() {
        lines = new ArrayList<>();
        sourceData = new ArrayList<>();
    }

    @Override
    public void configure() { }

    @Override
    protected void process(Collection<String> lines)
    {
        this.lines.addAll(lines);
    }

    @Override
    public void append(final TimeMachine timeMachine, final Statistic... statistics) {
        super.append(timeMachine, statistics);
        sourceData.addAll(Arrays.asList(statistics));
    }

    public Collection<Statistic> getStatistics()
    {
        return sourceData;
    }

    @Override
    public void close() {

    }
}
