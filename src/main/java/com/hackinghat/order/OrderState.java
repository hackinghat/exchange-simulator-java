package com.hackinghat.order;

import java.util.Arrays;
import java.util.List;

public enum OrderState {
    PENDING_NEW,
    PENDING_CANCEL,
    PENDING_REPLACE,
    NEW,
    CANCELLED,
    FILLED,
    PARTIALLY_FILLED;

    public final static List<OrderState> TERMINAL_STATES = Arrays.asList(CANCELLED, FILLED);
    public final static List<OrderState> ALL_PENDING_STATES = Arrays.asList(PENDING_NEW, PENDING_REPLACE, PENDING_CANCEL);
    public final static List<OrderState> PENDING_AMENDABLE_STATES = Arrays.asList(PENDING_REPLACE, PENDING_CANCEL);

    public static boolean isTerminal(final OrderState state) {
        return TERMINAL_STATES.contains(state);
    }

    public static boolean isPending(final OrderState state) {
        return ALL_PENDING_STATES.contains(state);
    }

    public static boolean isAmendPending(final OrderState state) {
        return PENDING_AMENDABLE_STATES.contains(state);
    }

    public static OrderState accept(final OrderState state) {
        switch (state) {
            case PENDING_NEW:
                return NEW;
            case PENDING_CANCEL:
                return CANCELLED;
        }
        return null;
    }
}
