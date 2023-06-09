package com.hackinghat.util;

import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.Assert.assertTrue;

public class MemoryCacheTest {
    @Test
    public void testInsert() {
        final MemoryCache<String, RandomObject> instrumentCache = new MemoryCache<>(RandomObject.class);
        final RandomObject ro1 = new RandomObject("A", 1L);
        instrumentCache.insert(ro1);
        Assert.assertEquals(1, instrumentCache.size());
        final Optional<RandomObject> opt = instrumentCache.get(ro1.getId());
        assertTrue(opt.isPresent());
        Assert.assertEquals(ro1, opt.get());
        Assert.assertNotSame(ro1, opt.get());
        Assert.assertThrows(NullPointerException.class, () -> instrumentCache.insert(new RandomObject(null, 1L)));
        Assert.assertThrows(NullPointerException.class, () -> instrumentCache.insert( null));
        Assert.assertThrows(IllegalArgumentException.class, () -> instrumentCache.insert(new RandomObject("A", 1L)));
    }

    @Test
    public void testUpdate() {
        final MemoryCache<String, RandomObject> instrumentCache = new MemoryCache<>(RandomObject.class);
        final RandomObject ro1 = new RandomObject("A", 1L);
        Assert.assertThrows(IllegalArgumentException.class, () -> instrumentCache.update(ro1));
        instrumentCache.insert(ro1);
        ro1.setTimestamp(LocalDateTime.now());
        Optional<RandomObject> opt = instrumentCache.get(ro1.getId());
        assertTrue(opt.isPresent());
        Assert.assertNotEquals(opt.get(), ro1);
        instrumentCache.update(ro1);
        opt = instrumentCache.get(ro1.getId());
        assertTrue(opt.isPresent());
        Assert.assertEquals(opt.get(), ro1);
        final Optional<RandomObject> optRo1 = instrumentCache.get(ro1.getId());
        assertTrue(optRo1.isPresent());
        Assert.assertNotSame(ro1, optRo1.get());
        Assert.assertThrows(NullPointerException.class, () -> instrumentCache.update(new RandomObject(null, 1L)));
        Assert.assertThrows(NullPointerException.class, () -> instrumentCache.update(null));
    }

    @Test
    public void testRemove() {
        final MemoryCache<String, RandomObject> instrumentCache = new MemoryCache<>(RandomObject.class);
        final RandomObject ro1 = new RandomObject("A", 1L);
        Assert.assertFalse(instrumentCache.remove(ro1.getId()));
        instrumentCache.insert(ro1);
        Assert.assertEquals(1, instrumentCache.size());
        assertTrue(instrumentCache.remove(ro1.getId()));
        Assert.assertEquals(0, instrumentCache.size());
    }
}
