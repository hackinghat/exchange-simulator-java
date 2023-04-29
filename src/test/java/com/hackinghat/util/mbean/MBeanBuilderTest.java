package com.hackinghat.util.mbean;

import org.junit.Assert;
import org.junit.Test;

public class MBeanBuilderTest {
    @Test(expected = IllegalArgumentException.class)
    public void testNullAttributeName() {
        MBeanBuilder.getAttributeName(null);
    }

    @Test()
    public void testGetAttributeName() {
        Assert.assertEquals("a", MBeanBuilder.getAttributeName("a"));
        Assert.assertEquals("Get A", MBeanBuilder.getAttributeName("GetA"));
        Assert.assertEquals("A", MBeanBuilder.getAttributeName("getA"));
        Assert.assertEquals("A", MBeanBuilder.getAttributeName("setA"));
        Assert.assertEquals("Test Int", MBeanBuilder.getAttributeName("getTestInt"));
        Assert.assertEquals("Test Int", MBeanBuilder.getAttributeName("setTestInt"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalMBeanAttributeSignature1() {
        MBeanBuilder.getAttributes(IllegalMBeanTestService1.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalMBeanAttributeSignature2() {
        MBeanBuilder.getAttributes(IllegalMBeanTestService2.class);
    }

//    @Test
//    public void TestMBeanBuilder() throws Exception {
//        final MBeanTestService test = new MBeanTestService("ABC", 1,  2.0f);
//        final Class<?> clazz = test.getClass();
//        Constructor<?> ctor = null;
//        try {
//            ctor = clazz.getConstructor();
//        } catch (NoSuchMethodException noSuchMethodException) {
//            // Do nothing
//        }
//        if (ctor == null)
//            throw new IllegalArgumentException("Class " + clazz.getSimpleName() + " does not have a no-arg constructor");
//
//        //Assert.assertEquals(1L, MBeanBuilder.getAttributes(clazz).length);
//        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
//        final MBeanHolder holder = new MBeanHolder(new MBeanTestService("ABC", 1, 2.0f), "xx");
//        ObjectInstance xx = server.registerMBean(holder, holder.getName());
//
//        // Keep program running.
//        while (true) {
//            System.out.println("Test - " + new Date() + " ");
//            Thread.sleep(1000);
//
//        }
//    }
}
