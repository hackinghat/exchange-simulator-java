package com.hackinghat.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.temporal.ValueRange;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class ObjectCache {

    private final HashMap<Class<?>, MemoryCache<Object, CopyableAndIdentifiable<Object>>> memoryCache;
    private final Object sync = new Object();

    public ObjectCache() {
        memoryCache = new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    public <K, V extends CopyableAndIdentifiable<K>> void addCache(final MemoryCache<K, V> cache) {
        if (memoryCache.containsKey(cache.getValueClass()))
            throw new IllegalArgumentException("Class is already present in cache: " + cache.getValueClass().getSimpleName());
        memoryCache.put(cache.getValueClass(), (MemoryCache<Object, CopyableAndIdentifiable<Object>>) cache);
    }

    public <K, V extends CopyableAndIdentifiable<K>> boolean hasCache(final Class<V> valueClass) {
        return memoryCache.containsKey(valueClass);
    }

    @SuppressWarnings("unchecked")
    public <K, V extends CopyableAndIdentifiable<K>> MemoryCache<K, V> getCache(final Class<V> valueClass) {
        return (MemoryCache<K, V>) memoryCache.get(valueClass);
    }

    @SuppressWarnings("unchecked")
    public <K, V extends CopyableAndIdentifiable<K>> void insert(final V value) {
        Objects.requireNonNull(value);
        synchronized (sync) {
            final MemoryCache<K, V> cache = (MemoryCache<K, V>) memoryCache.get(value.getClass());
            if (cache == null)
                throw new IllegalArgumentException("Unknown object type: " + value.getClass().getSimpleName());
            cache.insert(value);
        }
    }

    @SuppressWarnings("unchecked")
    public <K, V extends CopyableAndIdentifiable<K>> void update(final V value) {
        Objects.requireNonNull(value);
        synchronized (sync) {
            final MemoryCache<K, V> cache = (MemoryCache<K, V>) memoryCache.get(value.getClass());
            if (cache == null)
                throw new IllegalArgumentException("Unknown object type: " + value.getClass().getSimpleName());
            cache.update(value);
        }
    }

    @SuppressWarnings("unchecked")
    public <K, V extends CopyableAndIdentifiable<K>> boolean remove(final Class<V> valueClass, final K key) {
        Objects.requireNonNull(valueClass);
        Objects.requireNonNull(key);
        synchronized (sync) {
            final MemoryCache<K, V> cache = (MemoryCache<K, V>) memoryCache.get(valueClass);
            if (cache == null)
                throw new IllegalArgumentException("Unknown object type: " + valueClass.getSimpleName());
            return cache.remove(key);
        }
    }

    @SuppressWarnings("unchecked")
    public <K, V extends CopyableAndIdentifiable<K>> boolean remove(final V value) {
        Objects.requireNonNull(value);
        synchronized (sync) {
            final MemoryCache<K, V> cache = (MemoryCache<K, V>) memoryCache.get(value.getClass());
            if (cache == null)
                throw new IllegalArgumentException("Unknown object type: " + value.getClass().getSimpleName());
            return cache.remove(value.getId());
        }
    }

    @SuppressWarnings("unchecked")
    public <K, V extends CopyableAndIdentifiable<K>> Optional<V> get(final V value) {
        Objects.requireNonNull(value);
        synchronized (sync) {
            final MemoryCache<K, V> cache = (MemoryCache<K, V>) memoryCache.get(value.getClass());
            if (cache == null)
                throw new IllegalArgumentException("Unknown object type: " + value.getClass().getSimpleName());
            return cache.get(value.getId());
        }
    }

    @SuppressWarnings("unchecked")
    public <K, V extends CopyableAndIdentifiable<K>> Optional<V> get(final Class<V> valueClass, final K key) {
        Objects.requireNonNull(valueClass);
        Objects.requireNonNull(key);
        synchronized (sync) {
            final MemoryCache<K, V> cache = (MemoryCache<K, V>) memoryCache.get(valueClass);
            if (cache == null)
                throw new IllegalArgumentException("Unknown object type: " + valueClass.getSimpleName());
            return cache.get(key);
        }
    }

    public Map<Class<CopyableAndIdentifiable<Object>>, Long> getSizes() {
        final Map<Class<CopyableAndIdentifiable<Object>>, Long> result = new HashMap<>();
        synchronized (sync) {
            for (final MemoryCache<Object, CopyableAndIdentifiable<Object>> cache : memoryCache.values()) {
                result.put(cache.getValueClass(), (long) cache.size());
            }
            return result;
        }
    }

    public void write(final String fileName) throws IOException {
        final BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
        for (final MemoryCache<?, ?> cache : memoryCache.values()) {
            cache.getValueClass().getSimpleName().toLowerCase(Locale.ROOT);
        }
        writer.newLine();
        writer.flush();

    }
}
