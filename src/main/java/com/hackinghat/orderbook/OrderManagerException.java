package com.hackinghat.orderbook;

public class OrderManagerException extends Exception {
    public OrderManagerException(final String message) {
        super(message);
    }

    public OrderManagerException(final String message, final Throwable t) {
        super(message, t);
    }
}
