package com.hackinghat.orderbook.auction;

import com.hackinghat.model.Level;
import com.hackinghat.order.MarketState;
import com.hackinghat.util.Event;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

public class AuctionTriggerEvent extends Event
{
    public final static Set<MarketState> DEFAULT_PRE_AUCTION_STATES = EnumSet.of(
            MarketState.CHOICE, MarketState.BACK, MarketState.CONTINUOUS, MarketState.AUCTION,
            MarketState.CLOSED);

    public final static Set<MarketState> DEFAULT_AUCTION_STATES = EnumSet.of(
            MarketState.AUCTION);

    private final Set<MarketState>  preconditions;
    private final MarketState       postcondition;
    private final Level             referenceLevel;
    private final Duration          extensionDuration;

    public AuctionTriggerEvent(final Object sender, final LocalDateTime timeOccurred, final Set<MarketState> preconditions, final MarketState postcondition, final Level referenceLevel, final Duration extensionDuration)
    {
        super(sender, timeOccurred);
        Objects.requireNonNull(preconditions);
        if (!DEFAULT_PRE_AUCTION_STATES.contains(postcondition))
            throw new IllegalArgumentException("Post condition state of: '" + postcondition + "' is not allowed");
        if (preconditions.size() == 0)
            throw new IllegalArgumentException("No auction event preconditions specified, event can never be triggered");
        this.preconditions = new HashSet<>(preconditions);
        this.postcondition = postcondition;
        this.referenceLevel = referenceLevel;
        this.extensionDuration = extensionDuration;
    }

    @Override
    public int hashCode()
    {
        return getTimestamp().hashCode();
    }

    public Level getReferenceLevel()
    {
        return referenceLevel;
    }

    public boolean hasReferenceLevel()
    {
        return referenceLevel != null;
    }

    public Set<MarketState> getPreconditions()
    {
        return preconditions;
    }

    public MarketState getPostcondition()
    {
        return postcondition;
    }

    public Duration getExtensionDuration()
    {
        return extensionDuration;
    }

    @Override
    public String toString() {
        return "AuctionTriggerEvent{" +
                "preconditions=" + preconditions +
                ", postcondition=" + postcondition +
                ", simulationTime=" + getTimestamp() +
                ", referenceLevel=" + referenceLevel +
                ", extensionDuration=" + extensionDuration +
                '}';
    }
}
