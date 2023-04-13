package com.hackinghat.model.serialize;

import com.hackinghat.util.CopyableAndIdentifiable;
import com.hackinghat.util.TimeMachine;
import com.hackinghat.util.component.Component;

public interface EventPublisher<E>  {
    void configure();
    void publish(final TimeMachine time, final E event);
}
