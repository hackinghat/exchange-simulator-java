package com.hackinghat.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static java.time.temporal.ChronoUnit.NANOS;

        /**
 * Handles the 'simulation time', allowing a consistent view of time from the point of view of statistics
 * and scheduled events (auctions etc.) but allowing it to change at a different rate to the actual time.
 *
 * This allows for the speed-up (or slowdown) of time within the simulation.
 *
 */
public class TimeMachine
{
    private static final Logger LOG = LogManager.getLogger(TimeMachine.class);

    private final static DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSSSSS");

    final AtomicInteger startCount;
    final ZonedDateTime startTime;
    final ZoneOffset    zoneOffset;
    final double        delta;
          Instant       creationInstant;
    /**
     * Create a time machine based on Java 8 style times
     * @param startTime the time of day that we want the time machine to begin the simulation from
     */
    public TimeMachine(final LocalTime startTime, final double delta)
    {
        this(startTime.atDate(LocalDate.now()), delta);
    }

    public TimeMachine(final LocalDateTime startTime, final double delta)
    {
        this(startTime.atZone(ZoneId.systemDefault()), delta);
    }

    public TimeMachine(final ZonedDateTime startTime, final double delta)
    {
        Objects.requireNonNull(startTime);
        this.startTime = startTime;
        this.delta = delta;
        this.startCount = new AtomicInteger(0);
        this.zoneOffset = startTime.getOffset();
    }

    /**
     * For use in tests, create a time machine set at local-midnight that is also started
     */
    public TimeMachine()
    {
        this(LocalTime.of(0, 0, 0), 0.0d);
        start();
    }

    public void start()
    {
        start(Instant.now());
    }

    /**
     * Reset the time machine to the current time.  This method can be called multiple times to move
     * the relative start point in time.  This can be useful to set the time machine at a point where
     * subsequent calls to {@linkplain #toSimulationTime()}  will return a specific time in the day.
     * @param creationTime the required start time
     */
    public void start(final Instant creationTime)
    {
        final boolean restarted = creationTime != null;
        this.creationInstant = creationTime;
        this.startCount.incrementAndGet();
        LOG.info("Time machine " + (restarted ? "re-" : "") + "started with simulation time of: " + formatTime(startTime.toLocalDateTime()) + " and delta of: " + delta);
    }

    public int getStartCount()
    {
        return startCount.intValue();
    }

    public Instant getCreationInstant() { return creationInstant; }

    public LocalTime getStartTime()
    {
        return startTime.toLocalTime();
    }

    public LocalDateTime toSimulationTime()
    {
        return toSimulationTime(Instant.now());
    }

    public static LocalDateTime toLocalDateTime(final LocalDate date, int hour, int minute, int second) {
        return LocalDateTime.of(date.getYear(), date.getMonth(), date.getDayOfMonth(), hour, minute, second);
    }

    public static LocalDateTime toLocalDateTime(int hour, int minute, int second) {
        final LocalDate now = LocalDate.now();
        return LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), hour, minute, second);
    }

    public LocalDateTime toSimulationTime(final Instant now)
    {
        if (creationInstant == null)
            throw new IllegalStateException("Time machine not started!");

        final long nanosBetween = NANOS.between(creationInstant, now);
        final long adjustedDifference = Double.valueOf(nanosBetween * delta).longValue();
        return startTime.plusNanos(adjustedDifference).toLocalDateTime();
    }

    public Instant fromSimulationTime(final LocalDateTime simulationTime)
    {
        final Instant now = startTime.with(simulationTime).toInstant();
        final long nanosBetween = NANOS.between(startTime.toInstant(), now);
        final long adjustedDifference = Double.valueOf(nanosBetween / delta).longValue();
        return creationInstant.plusNanos(adjustedDifference);
    }

    public String formatTime() {
        return formatTime(toSimulationTime());
    }

    public String formatTime(final LocalDateTime localtime)
    {
        Objects.requireNonNull(localtime);
        return startTime.with(localtime).format(TIME_FORMATTER);
    }

    /**
     * Return a string representation of the time in the UTC timezone, suitable for storing timestamps
     * @param localDateTime implicitly in the zone of the time machine
     * @return string formatted as ISO
     */
    public String formatTimeAsUTCISO(final LocalDateTime localDateTime) {
        Objects.requireNonNull(localDateTime);
        final ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, startTime.getZone());
        return LocalDateTime.ofInstant(zonedDateTime.toInstant(), ZoneOffset.UTC).toString();
    }

    /**
     * Used for persisting timestamps we store all our date/times as UTC but we restore them into the timezone of the time machine
     * @param stringTime the UTC timestamp in ISO format
     * @return a zoned {@link  LocalDateTime}
     */
    public LocalDateTime parseTimeFromUTCISO(final String stringTime) {
        Objects.requireNonNull(stringTime);
        ZonedDateTime utcDateTime = ZonedDateTime.of(LocalDateTime.parse(stringTime), ZoneOffset.UTC);
        return LocalDateTime.ofInstant(utcDateTime.toInstant(), startTime.getZone());
    }

    /**
     * The number of milliseconds that the period represents in the {@linkplain ChronoUnit}
     * @param duration the duration
     * @param expressedIn the units the output should be expressed in
     * @return the number of millis
     */
    public long simulationPeriodToWall(final Duration duration, final ChronoUnit expressedIn)
    {
        return Double.valueOf(duration.toNanos() / delta).longValue() / Duration.of(1L, expressedIn).toNanos();
    }

    public Instant simulationTimeToWallTime(final LocalTime simulationTime) {
        ZonedDateTime zonedSimulationTime = startTime.with(simulationTime);
        final long nanos = Double.valueOf(NANOS.between(startTime, zonedSimulationTime) * delta).longValue();
        return creationInstant.plusNanos(nanos);
    }
}
