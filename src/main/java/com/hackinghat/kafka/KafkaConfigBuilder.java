package com.hackinghat.kafka;

import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.lang.invoke.SerializedLambda;
import java.util.Properties;
import java.util.function.Supplier;

public class KafkaConfigBuilder {
    public final static String BOOTSTRAP_SERVERS = "localhost:9092";

    public static Properties makeConfig(final String bootstrapServer) {
        //Assign topicName to string variable
        // create instance for properties to access producer configs
        Properties props = new Properties();
        //Assign localhost id
        props.put("bootstrap.servers", bootstrapServer);
        //Set acknowledgements for producer requests.
        props.put("acks", "all");
        //If the request fails, the producer can automatically retry,
        props.put("retries", 0);
        //Specify buffer size in config
        props.put("batch.size", 16384);
        //Reduce the no of requests less than 0
        props.put("linger.ms", 1);
        //The buffer.memory controls the total amount of memory available to the producer for buffering.
        props.put("buffer.memory", 33554432);
        return props;
    }

    public static <K, V> Producer<K, V> makeProducer(final Serializer<K> keySerializer, final Serializer<V> valueSerializer) {
        return new KafkaProducer<>(makeConfig(BOOTSTRAP_SERVERS), keySerializer, valueSerializer);
    }
}
