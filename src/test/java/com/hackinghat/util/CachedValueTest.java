package com.hackinghat.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.*;

public class CachedValueTest {
    private TimeMachine timeMachine;
    private EventDispatcher eventDispatcher;
    private NotSoRandomSource randomSource;

    private VisForValue supplyOne() {
        return new VisForValue(randomSource.nextInt(10));
    }

    private VisForValue suppyNone() {
        return null;
    }

    @Before
    public void before() {
        timeMachine = new TimeMachine();
        eventDispatcher = new SyncEventDispatcher(timeMachine);
        randomSource = new NotSoRandomSource();
        randomSource.setIntSource(new int[]{1, 2, 3});
    }

    @After
    public void after() {
        eventDispatcher.shutdown();
    }

    @Test
    public void testEmptyCachedValue() throws Exception {
        final CachedValue<VisForValue> cachedValue = new CachedValue<>(VisForValue.class, timeMachine, Duration.of(1L, ChronoUnit.MINUTES), this::supplyOne, eventDispatcher, true);
        assertNull(cachedValue.peek());
        assertNull(cachedValue.getInProgress());
        // Trigger a scheduled request to populate the cached value
        assertNull(cachedValue.get());
        assertEquals(1, ((VisForValue) cachedValue.getInProgress().get()).getValue());
        assertEquals(1, cachedValue.peek().getValue());
        assertTrue(cachedValue.expired(timeMachine.toSimulationTime().plus(Duration.of(2L, ChronoUnit.MINUTES))));
    }

    @Test
    public void testSetNullValue() throws Exception {
        final CachedValue<VisForValue> cachedValue = new CachedValue<>(VisForValue.class, timeMachine, Duration.of(1L, ChronoUnit.MINUTES), this::suppyNone, eventDispatcher, true);
        assertNull(cachedValue.peek());
        assertNull(cachedValue.getInProgress());
        // Trigger a scheduled request to populate the cached value
        assertNull(cachedValue.get());
        assertNull(cachedValue.getInProgress().get());
        // The supplier function returned no value so we should still have nothing
        assertNull(cachedValue.peek());
    }

    private class VisForValue implements Copyable<VisForValue>, Timestampable {
        final Integer value;
        LocalDateTime timeStamp;

        public VisForValue(final Integer newValue) {
            this.value = newValue;
            this.timeStamp = timeMachine.toSimulationTime();
        }

        @Override
        public VisForValue copy() {
            return new VisForValue(value);
        }

        @Override
        public VisForValue copy(final LocalDateTime localTime) {
            return new VisForValue(value);
        }

        @Override
        public LocalDateTime getTimestamp() {
            return timeStamp;
        }

        @Override
        public void setTimestamp(final LocalDateTime timestamp) {
            this.timeStamp = timestamp;
        }

        int getValue() {
            return value;
        }

        @Override
        public VisForValue cloneEx() throws CloneNotSupportedException {
            return (VisForValue) this.clone();
        }
    }
}
