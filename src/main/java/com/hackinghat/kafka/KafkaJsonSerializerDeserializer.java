package com.hackinghat.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Map;

public class KafkaJsonSerializerDeserializer<Entity> implements Serializer<Entity>, Deserializer<Entity> {
    private static final Logger LOG = LogManager.getLogger(KafkaJsonSerializerDeserializer.class);
    private final Class<Entity> clazz;
    private final ObjectMapper mapper;

    public KafkaJsonSerializerDeserializer(final Class<Entity> clazz) {
        this.clazz = clazz;
        this.mapper = new ObjectMapper();
        this.mapper.findAndRegisterModules();
    }

    @Override
    public void configure(final Map configs, final boolean isKey) {
    }

    @Override
    public void close() {
    }

    @Override
    public byte[] serialize(final String s, final Entity t) {
        try {
            return mapper.writeValueAsBytes(t);
        } catch (final JsonProcessingException jpex) {
            LOG.error("Malformed trade object: " + jpex);
        }
        return new byte[0];
    }

    @Override
    public Entity deserialize(final String s, final byte[] bytes) {
        try {
            return mapper.readValue(bytes, clazz);
        } catch (final IOException desex) {
            LOG.error("Malformed trade object: " + desex);
        }
        return null;
    }
}
