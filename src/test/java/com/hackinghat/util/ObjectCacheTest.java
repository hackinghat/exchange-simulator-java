package com.hackinghat.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

public class ObjectCacheTest {
    @Test
    public void testInsert() {
        final ObjectCache oc = new ObjectCache();
        Assert.assertThrows(NullPointerException.class, () -> oc.insert(null));
        Assert.assertEquals(0, oc.getSizes().size());
        Assert.assertThrows(IllegalArgumentException.class, () -> oc.insert(new RandomObject("A", null)));
        oc.addCache(new MemoryCache<>(RandomObject.class));
        oc.insert(new RandomObject("A", null));
        Assert.assertEquals(1, oc.getSizes().size());
        Assert.assertEquals(1L, (long) oc.getSizes().get(RandomObject.class));
    }

    @Test
    public void testUpdate() {
        final ObjectCache oc = new ObjectCache();
        Assert.assertThrows(NullPointerException.class, () -> oc.update(null));
        Assert.assertEquals(0, oc.getSizes().size());
        Assert.assertThrows(IllegalArgumentException.class, () -> oc.update(new RandomObject("A", null)));
        oc.addCache(new MemoryCache<>(RandomObject.class));
        Assert.assertEquals(1, oc.getSizes().size());
        Assert.assertEquals(0L, (long) oc.getSizes().get(RandomObject.class));
        oc.insert(new RandomObject("A", 1L));
        Optional<RandomObject> opt = oc.get(RandomObject.class, "A");
        Assert.assertTrue(opt.isPresent());
        Assert.assertEquals(1L, opt.get().random);
        oc.update(new RandomObject("A", 2L));
        opt = oc.get(RandomObject.class, "A");
        Assert.assertEquals(2L, opt.get().random);
    }

    @Test
    public void testRemove() {
        final ObjectCache oc = new ObjectCache();
        Assert.assertThrows(NullPointerException.class, () -> oc.remove(null));
        Assert.assertEquals(0, oc.getSizes().size());
        Assert.assertThrows(IllegalArgumentException.class, () -> oc.remove(new RandomObject("A", null)));
        oc.addCache(new MemoryCache<>(RandomObject.class));
        Assert.assertFalse(oc.remove(RandomObject.class, "A"));
        oc.insert(new RandomObject("A", null));
        Assert.assertEquals(1, oc.getSizes().size());
        Assert.assertEquals(1L, (long) oc.getSizes().get(RandomObject.class));
        Assert.assertTrue(oc.remove(RandomObject.class, "A"));
    }
}
