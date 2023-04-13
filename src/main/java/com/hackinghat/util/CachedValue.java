package com.hackinghat.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class CachedValue<CachedType extends Timestampable & Copyable>
{
    private static final Logger LOG = LogManager.getLogger(CachedValue.class);

    private AtomicReference<CachedType> cachedValue;
    private ScheduledFuture<?>          inProgress;

    private final Class<CachedType>     clazz;
    private final Supplier<CachedType>  cachedValueProvider;
    private final Duration              maxAge;
    private final TimeMachine           timeMachine;
    private final EventDispatcher       eventDispatcher;
    private final boolean               allowNull;
    private final boolean               immediate;

    /**
     * Create a cached value holder, note that when {@code allowNull=false}
     * @param clazz the class of the cached type
     * @param timeMachine the
     * @param maxAge the length of time a duration will persist before it's replaced with a new version
     * @param valueProvider a functional interface that can return an object of clazz
     * @param eventDispatcher the event dispatcher has access to a scheduler
     * @param allowNull whether null is an acceptable value for this
     * @throws NullPointerException if allowNull=false and the value provider returns null
     */
    public CachedValue(final Class<CachedType> clazz, final TimeMachine timeMachine, final Duration maxAge, final Supplier<CachedType> valueProvider, final EventDispatcher eventDispatcher, final boolean allowNull)
    {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(timeMachine);
        Objects.requireNonNull(eventDispatcher);
        Objects.requireNonNull(maxAge);
        Objects.requireNonNull(valueProvider);

        this.clazz                  = clazz;
        this.timeMachine            = timeMachine;
        this.cachedValue            = null;
        this.maxAge                 = maxAge;
        this.cachedValueProvider    = valueProvider;
        this.eventDispatcher        = eventDispatcher;
        this.allowNull              = allowNull;
        this.immediate              = maxAge == Duration.ZERO;
        // If the cached value can not have a null value then we'll try to populate it now
        if (!allowNull)
            _get();
    }

    CachedType _get()
    {
        try
        {
            final CachedType temp = cachedValueProvider.get();
            if (temp == null && !allowNull)
                throw new NullPointerException("Provider did not provide an initial value and allowNull is false");
            if (temp != null && temp.getTimestamp() == null)
                throw new NullPointerException("Timestamp should be set on a cached value");
            if (cachedValue == null)
                cachedValue = new AtomicReference<>(temp);
            else
                cachedValue.set(temp);
            return temp;
        }
        catch (final Throwable t)
        {
            LOG.error("Unexptected exception trying to get cached value for type: " +
                    clazz.getSimpleName() + ", value ignored", t);
        }
        return null;
    }

    boolean expired(final LocalDateTime now)
    {
        Objects.requireNonNull(now);
        if (cachedValue == null)
            return true;
        final CachedType latest = cachedValue.get();
        if (latest == null)
            return true;
        if (latest.getTimestamp() == null)
            throw new NullPointerException("Unexpected null timestamp, check provider of: " + clazz.getSimpleName());
        return latest.getTimestamp().plus(maxAge).isBefore(now);
    }

    /**
     * For testing
     * @return a future which will either be completed or will allow for the completion of the current get task
     */
    ScheduledFuture<?> getInProgress()
    {
        return inProgress;
    }

    @SuppressWarnings("unchecked")
    CachedType peek()
    {
        if (cachedValue == null)
            return null;
        CachedType returnValue = cachedValue.get();
        if (returnValue == null)
            return null;
        return (CachedType) returnValue.copy();
    }

    public CachedType get()
    {
        // An immediate cached value is a special type used in testing
        if (immediate)
            return _get();

        if (cachedValue == null || expired(timeMachine.toSimulationTime()))
        {
            try
            {
                inProgress = eventDispatcher.schedule(this::_get, 0L);
            }
            catch (final RejectedExecutionException rejected)
            {
                if (LOG.isTraceEnabled())
                    LOG.trace("Executor rejected cached value, get are we shutting down?", rejected);
            }
        }
        return peek();
    }
}
