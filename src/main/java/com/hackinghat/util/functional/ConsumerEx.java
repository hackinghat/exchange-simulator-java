package com.hackinghat.util.functional;

@FunctionalInterface
public interface ConsumerEx<T, E extends Exception> {
    void accept(T t) throws E;
}
