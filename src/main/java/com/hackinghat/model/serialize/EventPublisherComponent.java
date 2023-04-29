package com.hackinghat.model.serialize;

import com.hackinghat.kafka.KafkaConfigBuilder;
import com.hackinghat.util.Identifiable;
import com.hackinghat.util.SimulatorObjectMapper;
import com.hackinghat.util.TimeMachine;
import com.hackinghat.util.component.AbstractComponent;
import com.hackinghat.util.mbean.MBeanAttribute;
import com.hackinghat.util.mbean.MBeanType;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serializer;

import java.time.Duration;
import java.util.function.Function;

@MBeanType(description = "A published of events")
public class EventPublisherComponent<K, V extends Identifiable<K>> extends AbstractComponent implements EventPublisher<V> {

    private final Function<V, String> topicDeriveFn;
    private final SimulatorObjectMapper mapper;
    private Producer<K, V> producer;
    private int messagesSent;

    public EventPublisherComponent(final String name, final SimulatorObjectMapper mapper, final Serializer<K> keySerializer, final Serializer<V> valueSerializer, final Function<V, String> topicDeriveFn) {
        super(name);
        this.producer = null;
        this.topicDeriveFn = topicDeriveFn;
        this.mapper = mapper;
        this.producer = KafkaConfigBuilder.makeProducer(keySerializer, valueSerializer);
    }

    @MBeanAttribute(description = "Sent messages")
    public int getSentMessages() {
        return messagesSent;
    }

    @Override
    public void configure() {

    }

    @Override
    public void start() {
        messagesSent = 0;
    }

    @Override
    public void stop() {
        producer.close(Duration.ofMillis(0));
        producer = null;
    }

    @Override
    public void publish(final TimeMachine time, final V event) {
        producer.send(new ProducerRecord<>(topicDeriveFn.apply(event), event.getId(), event));
    }
}
