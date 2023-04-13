package com.hackinghat.model.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.hackinghat.util.CopyableAndIdentifiable;
import com.hackinghat.util.ObjectCache;

import java.io.IOException;
import java.util.*;

public class ObjectCacheSerializer extends StdSerializer<ObjectCache> {

    public static String jsonNameForClass(final Class<?> clazz) { return clazz.getSimpleName().toLowerCase(Locale.ROOT); }

    public ObjectCacheSerializer() {
        super(ObjectCache.class);
    }

    /**
     * We need to make the ordering of the output predictable
     * @param objectCache
     * @return
     */
    private Collection<Class<CopyableAndIdentifiable<Object>>> getCacheNames(final ObjectCache objectCache) {
        final Map<Class<CopyableAndIdentifiable<Object>>, Long> caches = objectCache.getSizes();
        final ArrayList<Class<CopyableAndIdentifiable<Object>>> names = new ArrayList<>(caches.keySet());
        names.sort(Comparator.comparing(ObjectCacheSerializer::jsonNameForClass));
        return names;
    }

    @Override
    public void serialize(final ObjectCache objectCache, final JsonGenerator jsonGenerator, final SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        for (Class<CopyableAndIdentifiable<Object>> cache : getCacheNames(objectCache)) {
            jsonGenerator.writeObjectField(jsonNameForClass(cache), objectCache.getCache(cache));
        }
        jsonGenerator.writeEndObject();
    }
}
