package com.hackinghat.util;

import com.hackinghat.statistic.Statistic;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public abstract class AbstractStatisticsAppender implements Runnable, Closeable {
    protected final Logger LOG;
    private final static long DEFAULT_POLL_TIME = 10000L;

    private final LinkedBlockingQueue<String> statsQueue;
    private AtomicBoolean terminate;

    public static String defaultObjectArrayFormatter(final Object[] item) {
        final String[] result = new String[item.length];
        for (int i = 0; i < item.length; ++i) {
            result[i] = "\"" +(item[i] == null ? "" : item[i].toString()) + "\"";
        }
        return String.join(",", result);
    }


    public AbstractStatisticsAppender() {
        this.statsQueue = new LinkedBlockingQueue<>();
        this.terminate = new AtomicBoolean(false);
        this.LOG =  LogManager.getLogger(getClass().getSimpleName());
    }

    public void append(final String item) {
        if (item == null)
            return;
        statsQueue.add(item);
    }

    public void append(final TimeMachine timeMachine, final Statistic... items) {
        if (items == null)
            return;
        for (final Statistic item : items) {
            if (item != null) {
                final String stringItem = item.formatStatistic(timeMachine);
                if (stringItem != null && stringItem.length() > 0)
                    statsQueue.add(stringItem);
            }
        }
    }

    public long size() { return statsQueue.size(); }

    public void terminate() {
        terminate.set(true);
    }

    protected abstract void process(final Collection<String> lines);

    @Override
    public void close() {
        terminate();
    }
    
    void writePending() {
        try {
            List<String> itemsToAppend = new ArrayList<>();
            final String item = statsQueue.poll(DEFAULT_POLL_TIME, TimeUnit.MICROSECONDS);
            if (item != null) {
                itemsToAppend.add(item);
                statsQueue.drainTo(itemsToAppend);
                process(itemsToAppend);
            }
        }
        catch (final InterruptedException iex) {
            if (LOG.isTraceEnabled())
                LOG.trace("Awoke prematurely from poll", iex);
        }
        catch (final Throwable t) {
            LOG.error("Error writing statistics: ", t);
        }
    }

    abstract void configure();

    public void run() {
        configure();
        while (!terminate.get()) {
            writePending();
        }
    }
}
