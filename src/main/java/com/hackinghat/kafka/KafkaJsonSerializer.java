package com.hackinghat.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.hackinghat.model.serialize.TradeSerializer;
import com.hackinghat.util.SimulatorObjectMapper;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

public class KafkaJsonSerializer<T> implements Serializer<T> {

    private final SimulatorObjectMapper mapper;

    public KafkaJsonSerializer(final SimulatorObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        Serializer.super.configure(configs, isKey);
    }

    @Override
    public byte[] serialize(final String s, final T t) {
        try {
            return mapper.writeValueAsBytes(t);
        }
        catch (final JsonProcessingException jpex) {
            return null;
        }
    }

    @Override
    public byte[] serialize(String topic, Headers headers, T data) {
        return Serializer.super.serialize(topic, headers, data);
    }

    @Override
    public void close() {
        Serializer.super.close();
    }
}
