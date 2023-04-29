package com.hackinghat.util;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.concurrent.*;

/**
 * A future that is never scheduled but will be executed if someone tries to 'get' it
 * Used for testing code.
 */
public class SyncScheduledFuture<V> implements ScheduledFuture<V> {
    private Callable<V> callable;
    private Instant executionInstant;
    private long timeToWait;
    private V result;
    private boolean done;

    public SyncScheduledFuture(final Callable<V> callable, final long wallTimeToWait) {
        this.callable = callable;
        this.timeToWait = wallTimeToWait;
        this.executionInstant = Instant.now().plusNanos(wallTimeToWait);
        this.result = null;
        this.done = false;
    }

    /**
     * The callable result
     *
     * @param callableResult the value to report back
     * @param wallTimeToWait the amount of time to delay
     */
    public SyncScheduledFuture(final V callableResult, final long wallTimeToWait) {
        this.callable = null;
        this.result = callableResult;
        this.timeToWait = wallTimeToWait;
    }

    @Override
    public long getDelay(@Nonnull final TimeUnit unit) {
        throw new IllegalStateException("This future has no delay");
    }

    public Instant getExecutionInstant() {
        return executionInstant;
    }

    @Override
    public int compareTo(@Nonnull final Delayed o) {
        throw new IllegalStateException("This future should not be put inside a collection!");
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (result != null)
            return false;

        callable = null;
        return true;
    }

    @Override
    public boolean isCancelled() {
        return callable == null && result == null;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public V get() throws ExecutionException {
        if (isCancelled())
            throw new CancellationException("Already cancelled");
        try {
            if (result == null)
                result = callable.call();
        } catch (final Exception ex) {
            throw new ExecutionException(ex);
        }
        done = true;
        return result;
    }

    @Override
    public V get(long timeout, @Nonnull TimeUnit unit) throws InterruptedException, ExecutionException {
        return get();
    }
}
