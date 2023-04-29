package com.hackinghat.util;

import com.hackinghat.util.mbean.MBeanType;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.time.temporal.ChronoUnit.NANOS;

@MBeanType(description = "AsyncEventDispatcher")
public class AsyncEventDispatcher extends EventDispatcher {
    private final ScheduledExecutorService threadPoolExecutor;
    private final ConcurrentSkipListSet<ScheduledFuture<?>> scheduledFutures;
    private final Object sync = new Object();
    private final AtomicBoolean shuttingDown;
    private final TimeMachine timeMachine;

    public AsyncEventDispatcher(final ScheduledExecutorService threadPoolExecutor, final TimeMachine timeMachine) {
        super("AsyncEventDispatcher");
        this.threadPoolExecutor = threadPoolExecutor;
        this.scheduledFutures = new ConcurrentSkipListSet<>();
        this.shuttingDown = new AtomicBoolean(false);
        this.timeMachine = timeMachine;
    }

    private <T extends Event> ScheduledFuture<?> _dispatch(final T event, final long wallTimeToWait) {
        final Event publicCopy = event.copy();
        final ScheduledFuture<?> schedule = threadPoolExecutor.schedule(() -> dispatchSync(publicCopy), wallTimeToWait, TimeUnit.NANOSECONDS);
        scheduledFutures.add(schedule);
        eventDispatched();
        clearFutures();
        return schedule;
    }

    public <T extends Event> ScheduledFuture<?> delayedDispatch(final T event, final LocalDateTime simulationTime) {
        if (shuttingDown.get())
            throw new IllegalStateException("Can't dispatch event, publisher is shutting down " + event);

        final Instant wallTime = timeMachine.fromSimulationTime(simulationTime);
        final Instant now = Instant.now();
        final long timeToWait = Math.max(NANOS.between(now, wallTime), 0);
        return _dispatch(event, timeToWait);
    }

    @Override
    public <T extends Event> void dispatch(final T event) {
        _dispatch(event, 0L);
    }

    @Override
    public ScheduledFuture<?> schedule(final Callable<?> callable, final long nanosToWait) {
        Objects.requireNonNull(callable);
        return threadPoolExecutor.schedule(callable, nanosToWait, TimeUnit.NANOSECONDS);
    }

    private void clearFutures() {
        synchronized (sync) {
            final Set<ScheduledFuture<?>> done = new HashSet<>();
            for (final ScheduledFuture<?> future : scheduledFutures) {
                if (future.isDone() || future.isCancelled())
                    done.add(future);
            }
            done.forEach(scheduledFutures::remove);
        }
    }

    @Override
    public void close() {
        synchronized (sync) {
            scheduledFutures.forEach(f -> f.cancel(true));
        }
    }

}
