package com.hackinghat.util;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

public class MemoryCache<K, V extends CopyableAndIdentifiable<K>> {
    private final Object    sync = new Object();

    private final HashMap<K, V>   cache;
    private final Class<V>        valueClazz;

    public MemoryCache(final Class<V> valueClazz) {
        this.cache = new HashMap<>();
        this.valueClazz = valueClazz;
    }

    public Class<V> getValueClass() { return valueClazz; }

    @SuppressWarnings("unchecked")
    public void upsert(final V... values) {
        synchronized (sync) {
            for (final V value : values) {
                Objects.requireNonNull(value);
                Objects.requireNonNull(value.getId());
                final V val = (V) value.copy();
                final K key = val.getId();
                cache.put(key, val);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void insert(final V value) {
        synchronized (sync) {
            Objects.requireNonNull(value);
            Objects.requireNonNull(value.getId());
            final V val = (V)value.copy();
            final K key = val.getId();
            if (cache.containsKey(key)) {
                throw new IllegalArgumentException("Key '" + key + "' already present in map");
            }
            cache.put(key, val);
        }
    }

    public int size() {
        synchronized (sync) {
            return cache.size();
        }
    }

    @SuppressWarnings("unchecked")
    public void update(final V value) {
        synchronized (sync) {
            Objects.requireNonNull(value);
            Objects.requireNonNull(value.getId());
            final V val = (V)value.copy();
            final K key = val.getId();
            if (!cache.containsKey(key)) {
                throw new IllegalArgumentException("Key '" + key + "' not present in map");
            }
            cache.put(key, val);
        }
    }

    public Optional<V> get(final K key) {
        synchronized (sync) {
            return Optional.of(cache.get(key));
        }
    }

    /**
     * Used for iterating over the memory cache, note that the keys are a copy of the keys taken at the time of
     * the call to getKeys.
     * @return an array of keys that may or may not refer to elements in the cache
     */
    @SuppressWarnings("unchecked")
    public K[] getKeys() {
        synchronized (sync) {
            return (K[])cache.keySet().toArray();
        }
    }

    public boolean remove(final K key) {
        synchronized (sync) {
            return cache.remove(key) != null;
        }
    }
}
