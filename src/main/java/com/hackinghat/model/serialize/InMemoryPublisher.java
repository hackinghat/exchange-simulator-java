package com.hackinghat.model.serialize;

import com.hackinghat.util.CopyableAndIdentifiable;
import com.hackinghat.util.TimeMachine;
import com.hackinghat.util.component.AbstractComponent;
import com.hackinghat.util.mbean.MBeanAttribute;
import com.hackinghat.util.mbean.MBeanType;
import org.apache.mina.util.CircularQueue;

@MBeanType(description = "In Memory Publisher")
public class InMemoryPublisher<E extends CopyableAndIdentifiable<E>> extends AbstractComponent implements EventPublisher<E> {

    private final CircularQueue<E> queue;

    public InMemoryPublisher(int initialCapacity) {
        super("InMemoryPublisher");
        queue = new CircularQueue<>(initialCapacity);
    }

    @MBeanAttribute(description = "totalEvents")
    public int getTotalEvents() {
        return queue.size();
    }

    @Override
    public void stop() {
        queue.clear();
    }

    @Override
    public void start() {
        queue.clear();
    }

    @Override
    public void configure() {
    }

    @Override
    public void publish(TimeMachine time, E event) {
        queue.add(event);
    }
}
