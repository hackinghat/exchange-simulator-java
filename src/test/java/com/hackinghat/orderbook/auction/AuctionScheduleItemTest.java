package com.hackinghat.orderbook.auction;

import com.hackinghat.util.TimeMachine;
import org.junit.Test;

import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AuctionScheduleItemTest
{
    private final Duration TEN_SECONDS = Duration.of(10L, ChronoUnit.SECONDS);
    private final Duration ONE_SECOND = Duration.of(1L, ChronoUnit.SECONDS);

    @Test
    public void testCompare()
    {
        final AuctionScheduleItem i1 = new AuctionScheduleItem("A1",null, TimeMachine.toLocalDateTime(8, 0, 0), TEN_SECONDS, null);
        final AuctionScheduleItem i2 = new AuctionScheduleItem("A2",null, TimeMachine.toLocalDateTime(8, 0, 10), ONE_SECOND, null);
        assertEquals(-1, i1.compareTo(i2));
        assertEquals(1, i2.compareTo(i1));
        assertEquals(0, i1.compareTo(i1));
    }

    @Test
    public void testDoesntOverlap()
    {
        final AuctionScheduleItem i1 = new AuctionScheduleItem("A1", null, TimeMachine.toLocalDateTime(8, 0, 0), TEN_SECONDS, null);
        final AuctionScheduleItem i2 = new AuctionScheduleItem("A2", null, TimeMachine.toLocalDateTime(8, 0, 10), ONE_SECOND, null);
        assertFalse(i2.doesOverlap(i1));
        assertFalse(i1.doesOverlap(i2));
    }

    @Test
    public void testDoesOverlap()
    {
        final AuctionScheduleItem i1 = new AuctionScheduleItem("A1", null, TimeMachine.toLocalDateTime(8, 0, 0), TEN_SECONDS.plusNanos(1L), null);
        final AuctionScheduleItem i2 = new AuctionScheduleItem("A2", null, TimeMachine.toLocalDateTime(8, 0, 10), Duration.of(1L, ChronoUnit.SECONDS), null);
        assertTrue(i2.doesOverlap(i1));
        assertTrue(i1.doesOverlap(i2));
    }
}
