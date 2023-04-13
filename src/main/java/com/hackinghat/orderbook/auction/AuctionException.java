package com.hackinghat.orderbook.auction;

public class AuctionException extends Exception {
    public AuctionException(final String reason, final Throwable cause) {
        super(reason, cause);
    }

    public AuctionException(final String reason) {
        super(reason);
    }
}
