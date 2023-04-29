package com.hackinghat.model;

class TickRange {
    private final float lowerBound;
    private final float upperBound;
    private final float tickSize;

    public TickRange(float lowerBound, float upperBound, float tickSize) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.tickSize = tickSize;
    }

    public float getLowerBound() {
        return lowerBound;
    }

    public float getUpperBound() {
        return upperBound;
    }

    public float getTickSize() {
        return tickSize;
    }

}
