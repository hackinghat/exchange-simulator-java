package com.hackinghat.util.component;

public enum ComponentState {
    STOPPED(0), STARTING(1), RUNNING(2), FAILED(3), SUSPENDED(4), STOPPING(5);

    private final int value;

    ComponentState(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
