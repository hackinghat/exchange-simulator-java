package com.hackinghat.util;

import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TimeMachineTest
{
    @Test
    public void testStandStill()
    {
        final Instant base = Instant.now();
        final TimeMachine tm = new TimeMachine(LocalTime.of(8, 0, 0), 0.0);
        tm.start(base);
        final LocalDateTime then = tm.toSimulationTime();
        assertEquals("08:00:00.000000", tm.formatTime(then));
        final LocalDateTime now = tm.toSimulationTime(base.plusMillis(100000L));
        assertEquals(then, now);
        assertEquals("08:00:00.000000", tm.formatTime(now));
    }

    @Test
    public void testHalfSpeed()
    {
        final Instant base = Instant.now();
        final TimeMachine tm = new TimeMachine(LocalTime.of( 8, 0, 0), 0.5);
        tm.start(base);
        final LocalDateTime then = tm.toSimulationTime(base);
        assertEquals("08:00:00.000000", tm.formatTime(then));
        // Now fast forward by 2 minutes in wall time and
        final LocalDateTime now = tm.toSimulationTime(base.plusMillis(120002L));
        assertEquals(then.plusNanos(60001000000L), now);
        assertEquals("08:01:00.001000", tm.formatTime(now));
    }

    @Test
    public void testDoubleSpeed()
    {
        final Instant base = Instant.now();
        final TimeMachine tm = new TimeMachine(LocalTime.of(8, 0, 0), 2.0);
        tm.start(base);
        final LocalDateTime then = tm.toSimulationTime(base);
        assertEquals("08:00:00.000000", tm.formatTime(then));
        // Now fast forward by 2 minutes in wall time and
        final LocalDateTime now = tm.toSimulationTime(base.plusMillis(120002L));
        assertEquals(then.plusNanos(240004000000L), now);
        assertEquals("08:04:00.004000", tm.formatTime(now));
    }

    @Test
    public void testConvertSimulationToWall()
    {
        final TimeMachine tm = new TimeMachine(LocalTime.of(8, 0, 0), 2.0);
        tm.start();
        assertEquals(500L, tm.simulationPeriodToWall(Duration.of(1L, ChronoUnit.SECONDS), ChronoUnit.MILLIS));
    }

    @Test
    public void testConvertSimulationTimeToWallMillis()
    {
        final Instant now = Instant.now();
        final TimeMachine doubleSpeed = new TimeMachine(LocalTime.of(8, 0, 0), 2.0);
        doubleSpeed.start(now);
        assertEquals(2000L, doubleSpeed.simulationTimeToWallTime(LocalTime.of(8, 0, 1)).toEpochMilli() - now.toEpochMilli());

        final TimeMachine halfSpeed = new TimeMachine(LocalTime.of(8, 0, 0), 0.5);
        halfSpeed.start(now);
        assertEquals(2000L, halfSpeed.simulationTimeToWallTime(LocalTime.of(8, 0, 4)).toEpochMilli() - now.toEpochMilli());
    }

    @Test
    public void testSimulationTimeSymmetry()
    {
        final Instant now = Instant.now();
        final Instant wallPlus1 = now.plusMillis(1000L);
        final TimeMachine doubleSpeed = new TimeMachine(LocalTime.of( 8, 0, 0), 2.0);
        doubleSpeed.start(now);
        final Instant doublePlus1 = doubleSpeed.fromSimulationTime(doubleSpeed.toSimulationTime(wallPlus1));
        assertEquals(wallPlus1, doublePlus1);

        final TimeMachine halfSpeed = new TimeMachine(LocalTime.of(8, 0, 0), 0.5);
        halfSpeed.start(now);
        final Instant halfPlus1 = halfSpeed.fromSimulationTime(halfSpeed.toSimulationTime(wallPlus1));
        assertEquals(wallPlus1, halfPlus1);

        // Although we don't tend to mix time-machines it's a good test to make sure we've got this component, right!
        // We advance at double speed of 1s (so 2s), but 2s forward at half-speed is 4s forward from the starting point
        assertEquals(now.plusMillis(4000L), halfSpeed.fromSimulationTime(doubleSpeed.toSimulationTime(wallPlus1)));
        // We advance at half speed of 1s (so 0.5s), but 0.5s forward at double-speed is 0.25s forward from the starting point
        assertEquals(now.plusMillis(250L), doubleSpeed.fromSimulationTime(halfSpeed.toSimulationTime(wallPlus1)));
    }

    @Test
    public void testFormatTimeAsUtcIso() {
        final TimeMachine doubleSpeed = new TimeMachine(LocalTime.of( 8, 0, 0), 2.0);
        final LocalDateTime random1 = LocalDateTime.of(2020, 3, 9, 9, 8, 7, 50001);
        Assert.assertEquals("2020-03-09T09:08:07.000050001", doubleSpeed.formatTimeAsUTCISO(random1));
        final LocalDateTime random2 = LocalDateTime.of(2020, 8, 9, 9, 8, 7, 50001);
        Assert.assertEquals("2020-08-09T08:08:07.000050001", doubleSpeed.formatTimeAsUTCISO(random2));
    }

    @Test
    public void testParseTimeFromUtcIso() {
        final TimeMachine doubleSpeed = new TimeMachine(LocalTime.of( 8, 0, 0), 2.0);
        final LocalDateTime random1 = LocalDateTime.of(2020, 3, 9, 9, 8, 7, 50001);
        Assert.assertEquals(random1, doubleSpeed.parseTimeFromUTCISO("2020-03-09T09:08:07.000050001"));
        final LocalDateTime random2 = LocalDateTime.of(2020, 8, 9, 9, 8, 7, 50001);
        Assert.assertEquals(random2, doubleSpeed.parseTimeFromUTCISO("2020-08-09T08:08:07.000050001"));
    }
}
