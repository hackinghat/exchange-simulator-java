package com.hackinghat.orderbook.auction;

import com.hackinghat.model.*;
import com.hackinghat.order.MarketState;
import com.hackinghat.util.Event;
import com.hackinghat.util.EventDispatcher;
import com.hackinghat.util.SyncEventDispatcher;
import com.hackinghat.util.TimeMachine;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.*;

public class MarketManagerTest {
    private TimeMachine timeMachine;
    private MarketManager marketManager;
    private AuctionEventReceiver auctionEventReceiver;
    private EventDispatcher eventDispatcher;
    private Instrument VOD;
    private Level referenceLevel;

    @Before
    public void setUp() {
        VOD = new Instrument("VOD.L", new Currency("GBP"), new ConstantTickSizeToLevelConverter(1, 100, 3));
        timeMachine = new TimeMachine(LocalTime.of(7, 49, 59), 0.0);
        timeMachine.start();
        eventDispatcher = new SyncEventDispatcher(timeMachine);
        referenceLevel = VOD.getLevel(100.0f);
        marketManager = new MarketManager(referenceLevel, 0.1, Duration.of(5L, ChronoUnit.MINUTES), timeMachine, eventDispatcher, AuctionSchedule.makeLSESchedule(LocalDate.now()));
        auctionEventReceiver = new AuctionEventReceiver(eventDispatcher);
    }

    @After
    public void teardown() {
        marketManager.shutdown();
    }

    @Test
    public void testLastPriceAuction() throws Exception {
        assertFalse(marketManager.isAuctionInProgress());
        assertEquals(referenceLevel, marketManager.getLastLevel());
        marketManager.priceMonitor(new Trade(this, "T1", VOD, null, null, "O1", "O2", referenceLevel, 100));
        assertEquals(referenceLevel, marketManager.getLastLevel());
        marketManager.priceMonitor(new Trade(this, "T2", VOD, null, null, "O3", "O4", VOD.getLevel(referenceLevel.getPrice() + 10.0f), 100));
        assertEquals(VOD.getLevel(110.0f), marketManager.getLastLevel());
        assertEquals(0, auctionEventReceiver.events.size());
        assertFalse(marketManager.isAuctionInProgress());
        marketManager.priceMonitor(new Trade(this, "T3", VOD, null, null, "O5", "O6", VOD.getLevel(referenceLevel.getPrice() - 2.0f), 100));
        // The first auction trigger event will be published by the market manager and so there will be no dispatch
        assertEquals(0, auctionEventReceiver.events.size());
        assertEquals(VOD.getLevel(98.0f), marketManager.getLastLevel());
        assertTrue(marketManager.isAuctionInProgress());
        // The end of the auction trigger event is scheduled, run it by hand now ...
        marketManager.inProgressAuction.get();
        assertEquals(1, auctionEventReceiver.events.size());
        assertFalse(marketManager.isAuctionInProgress());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnexpectedEvent() {
        marketManager.notify(new NonStandardEvent(this));
    }

    @Test(expected = IllegalStateException.class)
    public void testNotifyTradeDuringAuction() {
        float lastPx = 100.0f;
        // This one is fine
        marketManager.priceMonitor(new Trade(this, "T4", VOD, timeMachine.toSimulationTime(), null, "O1", "O2", VOD.getLevel(lastPx), 100));
        assertFalse(marketManager.isAuctionInProgress());
        // This one triggers an auction
        marketManager.priceMonitor(new Trade(this, "T5", VOD, timeMachine.toSimulationTime(), null, "O5", "O6", VOD.getLevel(lastPx - 20.0f), 100));
        assertTrue(marketManager.isAuctionInProgress());
        // This one is illegal
        marketManager.priceMonitor(new Trade(this, "T6", VOD, timeMachine.toSimulationTime(), null, "O5", "O6", VOD.getLevel(lastPx - 20.0f), 100));
    }

    private void checkEvents(final AuctionEventReceiver receiver, final MarketState... postConditionStates) {
        if (postConditionStates.length > receiver.events.size())
            throw new IllegalArgumentException("More market states than events!");

        for (int i = 0; i < postConditionStates.length; ++i) {
            final AuctionTriggerEvent triggerEvent = (AuctionTriggerEvent) receiver.events.get(i);
            assertEquals(postConditionStates[i], triggerEvent.getPostcondition());
        }
    }

    @Test
    public void testNormalMarketOperation() throws Exception {
        final AuctionSchedule schedule = AuctionSchedule.makeLSESchedule(LocalDate.now());
        final AuctionScheduleItem open = schedule.getAuctionScheduleItem("Opening");
        final AuctionScheduleItem close = schedule.getAuctionScheduleItem("Closing");

        assertEquals(0, auctionEventReceiver.events.size());
        schedule.schedule(timeMachine, eventDispatcher);
        assertFalse(open.isCompleted());
        // Forcibly trigger the auction start
        open.forciblyStart();
        checkEvents(auctionEventReceiver, MarketState.AUCTION);
        open.forciblyEnd();
        // We've now got the previous event plus the post-condition state
        assertTrue(open.isCompleted());
        checkEvents(auctionEventReceiver, MarketState.AUCTION, MarketState.CONTINUOUS);
        assertFalse(close.isCompleted());
        close.forciblyStart();
        close.forciblyEnd();
        checkEvents(auctionEventReceiver, MarketState.AUCTION, MarketState.CONTINUOUS, MarketState.AUCTION, MarketState.CLOSED);
        assertTrue(close.isCompleted());
    }

    private static class NonStandardEvent extends Event {
        NonStandardEvent(Object sender) {
            super(sender, null);
        }
    }
}
