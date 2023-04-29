package com.hackinghat.util;

import com.hackinghat.order.MarketState;
import com.hackinghat.orderbook.auction.AuctionTriggerEvent;
import org.junit.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class EventDispatcherTest {
    /**
     * Listners that are also dispatchers of the same event shouldn't receive their own event back
     */
    @Test
    public void testNoEcho() {
        final TimeMachine timeMachine = new TimeMachine();
        try (final SyncEventDispatcher eventDispatcher = new SyncEventDispatcher(timeMachine)) {
            final AuctionListener l = new AuctionListener(eventDispatcher);
            final AuctionTriggerEvent myEvent = new AuctionTriggerEvent(this, timeMachine.toSimulationTime(), EnumSet.of(MarketState.CLOSED), MarketState.CLOSED, null, null);
            eventDispatcher.addListener(AuctionTriggerEvent.class, l);
            //  This one should be dispatched because it originated outside the listener
            eventDispatcher.dispatch(myEvent);
            assertEquals(1, l.receivedEvents.size());
            // We dispatch this one as being sent by the listener, it shouldn't be notified back
            final AuctionTriggerEvent someEvent = new AuctionTriggerEvent(l, timeMachine.toSimulationTime(), EnumSet.of(MarketState.CLOSED), MarketState.CLOSED, null, null);
            eventDispatcher.dispatch(someEvent);
            assertEquals(1, l.receivedEvents.size());
        }
    }

    @Test
    public void testBadlyBehaved() {
        final TimeMachine timeMachine = new TimeMachine();
        try (final SyncEventDispatcher eventDispatcher = new SyncEventDispatcher(timeMachine)) {
            final AuctionListener l1 = new AuctionListener(eventDispatcher);
            eventDispatcher.addListener(AuctionTriggerEvent.class, l1);
            final AuctionListener l2 = new AuctionListener(eventDispatcher);
            eventDispatcher.addListener(AuctionTriggerEvent.class, l2);
            final AuctionTriggerEvent someEvent = new AuctionTriggerEvent(this, timeMachine.toSimulationTime(), EnumSet.of(MarketState.CLOSED), MarketState.CLOSED, null, Duration.ZERO);
            eventDispatcher.dispatch(someEvent);
            assertEquals(0, l1.receivedEvents.size());
            assertEquals(0, l2.receivedEvents.size());
        }
    }

    private static class AuctionListener implements Listener {
        final List<AuctionTriggerEvent> receivedEvents = new ArrayList<>();
        final EventDispatcher dispatcher;

        public AuctionListener(final EventDispatcher dispatcher) {
            this.dispatcher = dispatcher;
        }

        @Override
        public void notify(final Event event) {
            if (event instanceof AuctionTriggerEvent) {
                final AuctionTriggerEvent triggerEvent = AuctionTriggerEvent.class.cast(event);
                if (triggerEvent.getExtensionDuration() == Duration.ZERO)
                    throw new IllegalArgumentException("event");
                receivedEvents.add(triggerEvent);
            }
        }
    }
}
