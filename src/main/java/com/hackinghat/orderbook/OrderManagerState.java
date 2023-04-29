package com.hackinghat.orderbook;

import com.hackinghat.order.MarketState;

import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Objects;

public class OrderManagerState {
    final OrderManagerStateTransition[] ACCEPTABLE_TRANSITIONNS = new OrderManagerStateTransition[]
            {
                    new OrderManagerStateTransition(MarketState.CLOSED, EnumSet.of(MarketState.AUCTION)),
                    new OrderManagerStateTransition(MarketState.AUCTION, EnumSet.of(MarketState.CONTINUOUS, MarketState.CLOSED)),
                    new OrderManagerStateTransition(MarketState.CONTINUOUS, EnumSet.of(MarketState.CHOICE, MarketState.BACK, MarketState.AUCTION)),
                    new OrderManagerStateTransition(MarketState.CHOICE, EnumSet.of(MarketState.CHOICE, MarketState.BACK, MarketState.AUCTION)),
                    new OrderManagerStateTransition(MarketState.BACK, EnumSet.of(MarketState.CHOICE, MarketState.BACK, MarketState.AUCTION))
            };
    private final Object sync = new Object();
    private MarketState marketState;
    private LocalTime timestamp;
    public OrderManagerState(final MarketState initialState) {
        Objects.requireNonNull(initialState);
        this.marketState = initialState;
    }

    public MarketState getCurrent() {
        synchronized (sync) {
            return marketState;
        }
    }

    public boolean isState(final MarketState comparisonState) {
        synchronized (sync) {
            return this.marketState == comparisonState;
        }
    }

    public void accept(final MarketState nextState) {
        synchronized (sync) {
            // Accept a no change transition
            if (marketState == nextState)
                return;

            for (final OrderManagerStateTransition transition : ACCEPTABLE_TRANSITIONNS) {
                if (transition.canAccept(marketState)) {
                    if (transition.accept(nextState)) {
                        marketState = nextState;
                        break;
                    }
                    throw new IllegalStateException("Can't accept state: " + nextState + ", it's not in the current set of acceptable states: " + transition.acceptedTransitions);
                }
            }
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + marketState + ")";
    }

    private static class OrderManagerStateTransition {
        private final MarketState startState;
        private final EnumSet<MarketState> acceptedTransitions;

        private OrderManagerStateTransition(final MarketState startState, final EnumSet<MarketState> acceptableStates) {
            this.startState = startState;
            this.acceptedTransitions = acceptableStates;
        }

        private boolean canAccept(final MarketState currentState) {
            return startState == currentState;
        }

        private boolean accept(final MarketState nextState) {
            return acceptedTransitions.contains(nextState);
        }

    }
}
