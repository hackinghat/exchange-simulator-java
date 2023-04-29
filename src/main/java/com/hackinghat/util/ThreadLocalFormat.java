package com.hackinghat.util;

import java.lang.reflect.Constructor;
import java.text.Format;

public class ThreadLocalFormat<T extends Format> extends ThreadLocal<T> {
    private final String pattern;
    private final Class<T> clazz;

    public ThreadLocalFormat(final Class<T> formatClass, final String pattern) {
        this.clazz = formatClass;
        this.pattern = pattern;
    }

    @Override
    protected T initialValue() {
        try {
            Constructor<T> constructor = clazz.getConstructor(String.class);
            return constructor.newInstance(pattern);
        } catch (final Exception ex) {
            // Force a null-pointer exception
            return null;
        }
    }
}
