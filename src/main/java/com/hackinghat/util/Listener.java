package com.hackinghat.util;

import java.util.Objects;

public interface Listener {
    /**
     * Usually a listener would not expect to receive its own events back.  However under some circimstances
     * it may be a convenient way of dispatching work back to the listener.
     *
     * @param event prior to notification the dispatcher will check that the listener would
     *              like to receive the event.
     * @return true if the event should be notified to the listener
     */
    default boolean shouldNotify(final Event event) {
        Objects.requireNonNull(event);
        return !event.isSender(this);
    }

    void notify(final Event event);
}
