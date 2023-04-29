package com.hackinghat.util;

import com.hackinghat.statistic.Statistic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;

public class KafkaEventsAppender<T extends Statistic, V> extends AbstractStatisticsAppender {
    private final static DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmssSSSSSS");
    private final String topicName;
    private final Properties producerProps;
    private final Function<T, String> keyGenerator;
    private final Function<T, Map<String, String>> valueGenerator;
    private Producer<String, String> producer;

    public KafkaEventsAppender(final String topicName, final Properties producerProps, final Function<T, String> keyGenerator, final Function<T, Map<String, String>> valueGenerator) {
        super();
        Objects.requireNonNull(topicName);
        Objects.requireNonNull(producerProps);
        Objects.requireNonNull(keyGenerator);
        Objects.requireNonNull(valueGenerator);
        this.topicName = topicName;
        this.producerProps = producerProps;
        this.keyGenerator = keyGenerator;
        this.valueGenerator = valueGenerator;

        if (topicName.length() == 0)
            throw new IllegalArgumentException("No topic specified for statistics file");
    }

    @Override
    void configure() {
        producer = new KafkaProducer<>(producerProps);
    }

    @Override
    public void close() {
        super.close();
        try {
            producer.close();
        } catch (final Exception ioex) {
            LOG.error("Couldn't close kafka producer for topic: " + topicName + ", reason: ", ioex);
        }
    }

    @Override
    protected void process(final Collection<String> lines) {
        try {
            for (final String line : lines) {
                producer.send(new ProducerRecord<>(topicName, line));
            }
        } catch (final Throwable t) {
            LOG.error("Couldn't log statistics, reason: ", t);
        }

    }
}
