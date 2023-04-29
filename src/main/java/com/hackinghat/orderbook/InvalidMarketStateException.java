package com.hackinghat.orderbook;

public class InvalidMarketStateException extends Exception {
    public InvalidMarketStateException(final String message) {
        super(message);
    }

    public InvalidMarketStateException(final String message, final Exception cause) {
        super(message, cause);
    }
}
