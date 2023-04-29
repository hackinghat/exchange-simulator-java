package com.hackinghat.orderbook.auction;

import com.hackinghat.util.TimeMachine;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

public class AuctionScheduleTest {
    private TimeMachine timeMachine;

    @Before
    public void setup() {
        timeMachine = new TimeMachine(LocalTime.of(7, 45, 0), 1.0d);
        timeMachine.start();
    }

    @Test(expected = IllegalStateException.class)
    public void testVerify() {
        final AuctionScheduleItem i1 = new AuctionScheduleItem("A1", null, TimeMachine.toLocalDateTime(8, 0, 1), Duration.of(10L, ChronoUnit.SECONDS), null);
        final AuctionScheduleItem i2 = new AuctionScheduleItem("A2", null, TimeMachine.toLocalDateTime(8, 0, 10), Duration.of(1L, ChronoUnit.SECONDS), null);
        new AuctionSchedule(i1, i2);
    }
}
