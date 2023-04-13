package com.hackinghat.util;

import com.hackinghat.statistic.SampledStatistic;
import jdk.dynalink.CallSiteDescriptor;

import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Sampled statistics are meant to be placed in a scheduled executor with a 'scheduleWithFixedInterval', they
 * then place their results into the queue but are flushed immediately (rather than waiting to be batched by a
 * separate thread). Other appenders (such as {@link  FileStatisticsAppender}) are designed to occupy a thread and drain
 * the incoming queue of events as they are added.
 * @param <Source> the type being sampled
 * @param <Statistic> the statistic(s) being measured
 */
public class SamplingStatisticAppender<Source, Statistic extends SampledStatistic<Source>> extends FileStatisticsAppender<Statistic>{

    private final SampledStatistic<Source> sampledStatistic;
    private final Supplier<Source> sourceGetter;
    private final TimeMachine timeMachine;

    public SamplingStatisticAppender(final TimeMachine timeMachine, final Instant now, final Statistic statistic, final Supplier<Source> getter, final Supplier<String> headerFunction, final String fileName) {
        super(headerFunction, now, fileName);
        Objects.requireNonNull(statistic);
        Objects.requireNonNull(getter);
        this.timeMachine = timeMachine;
        this.sourceGetter = getter;
        this.sampledStatistic = statistic;
        configure();
    }

    @Override
    public void run() {
        try {
            sampledStatistic.update(sourceGetter.get());
            append(sampledStatistic.formatStatistic(timeMachine));
            writePending();
        }
        catch (final Exception ex) {
            LOG.error("Unable to format statistic: ", ex);
        }
    }
}
