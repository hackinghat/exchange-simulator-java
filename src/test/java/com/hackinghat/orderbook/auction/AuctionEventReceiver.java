package com.hackinghat.orderbook.auction;

import com.hackinghat.util.Event;
import com.hackinghat.util.EventDispatcher;
import com.hackinghat.util.Listener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class AuctionEventReceiver implements Listener {
    List<Event> events = new ArrayList<>();

    public AuctionEventReceiver(final EventDispatcher dispatcher) {
        dispatcher.addListener(AuctionTriggerEvent.class, this);
    }

    @Override
    public void notify(final Event event) {
        events.add(event);
    }

    public Collection<Event> getEvents() {
        return Collections.unmodifiableCollection(events);
    }
}

