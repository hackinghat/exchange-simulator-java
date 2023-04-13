package com.hackinghat.model.serialize;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.hackinghat.model.Instrument;
import com.hackinghat.util.MemoryCache;
import com.hackinghat.util.ObjectCache;

import java.io.IOException;

public class InstrumentDeserializer extends StdDeserializer<Instrument> {

    private final ObjectCache objectCache;

    public InstrumentDeserializer(final ObjectCache cache) {
        super(Instrument.class);
        if (!cache.hasCache(Instrument.class))
            throw new IllegalArgumentException("Object cache does not cache instruments and should");
        this.objectCache = cache;
    }

    @Override
    public Instrument deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext) throws IOException, JacksonException {
        final MemoryCache<String, Instrument> cache = objectCache.getCache(Instrument.class);
        final ObjectCodec codec = jsonParser.getCodec();
        final JsonNode node = codec.readTree(jsonParser);
        return new Instrument(node.get("ticker").textValue(), node.get("description").textValue(), null, null);
    }
}
