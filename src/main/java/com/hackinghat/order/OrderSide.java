package com.hackinghat.order;

public enum OrderSide {
    BUY,
    SELL;

    public static OrderSide getOther(OrderSide side) {
        switch (side) {
            case BUY:
                return SELL;
            case SELL:
                return BUY;
            default:
                throw new IllegalArgumentException("Unknown side");
        }
    }
}
