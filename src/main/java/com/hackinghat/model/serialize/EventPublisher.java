package com.hackinghat.model.serialize;

import com.hackinghat.util.TimeMachine;

public interface EventPublisher<E> {
    void configure();

    void publish(final TimeMachine time, final E event);
}
