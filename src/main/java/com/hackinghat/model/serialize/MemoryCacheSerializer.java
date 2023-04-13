package com.hackinghat.model.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.hackinghat.util.MemoryCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public class MemoryCacheSerializer extends StdSerializer<MemoryCache> {

    private static final Logger LOG = LogManager.getLogger(MemoryCacheSerializer.class);

    public MemoryCacheSerializer() {
        super(MemoryCache.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void serialize(final MemoryCache memoryCache, final JsonGenerator jsonGenerator, final SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartArray();
        Object[] keys =  memoryCache.getKeys();
        Arrays.sort(keys);
        for (final Object key : keys) {
            final Optional<?> opt = memoryCache.get(key);
            if (opt.isPresent())
                jsonGenerator.writeObject(opt.get());
            else
                LOG.debug("Key no longer present in cache, concurrent modification");
        }
        jsonGenerator.writeEndArray();
    }
}
