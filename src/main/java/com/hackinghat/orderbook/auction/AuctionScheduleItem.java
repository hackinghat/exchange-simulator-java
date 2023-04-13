package com.hackinghat.orderbook.auction;

import com.hackinghat.order.MarketState;
import com.hackinghat.orderbook.OrderManager;
import com.hackinghat.util.EventDispatcher;
import com.hackinghat.util.Nameable;
import com.hackinghat.util.TimeMachine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;

/**
 * Represents the scheduling of a single part of the auction.  Each has an item
 */
public class AuctionScheduleItem implements Comparable<AuctionScheduleItem>, Nameable
{
    private static final Logger   LOG                        = LogManager.getLogger(OrderManager.class);
    public  static final Duration DEFAULT_EXTENSION_DURATION = Duration.of(5, ChronoUnit.MINUTES);

    private final String                  name;
    private final EnumSet<MarketState>    preConditionStates;
    private final LocalDateTime           simulationStartTime;
    private final MarketState             postConditionState;
    private final Duration                duration;

    private Instant                 wallStartTime;
    private Duration                extensionDuration;
    private ScheduledFuture<?>      startFuture;
    private ScheduledFuture<?>      endFuture;

    private TimeMachine             timeMachine;
    private EventDispatcher         eventDispatcher;

    public AuctionScheduleItem(final String name, final EnumSet<MarketState> preConditionStates, final LocalDateTime simulationStartTime, final Duration duration, final MarketState postConditionState)
    {
        this.name = name;
        this.preConditionStates = preConditionStates;
        this.simulationStartTime = simulationStartTime;
        this.duration = duration;
        this.postConditionState = postConditionState;
        this.startFuture = null;
        this.endFuture = null;
        this.wallStartTime = null;
    }

    @Override
    public String getName()
    {
        return name;
    }

    public Set<MarketState> getPreConditionStates() {
        return preConditionStates;
    }

    public LocalDateTime getSimulationStartTime() {
        return simulationStartTime;
    }
    public LocalDateTime getSimulationEndTime() { return getAdjustedStartTime(duration); }
    public LocalDateTime getAdjustedStartTime(final Duration adjustment) { return simulationStartTime.plus(adjustment); }
    public LocalDateTime getAdjustedEndTime(final Duration adjustment) { return getSimulationEndTime().plus(adjustment); }

    public Duration getDuration() {
        return duration;
    }

    public MarketState getPostConditionState() {
        return postConditionState;
    }

    /**
     * Two events overlap if the durations start or end of one is within the range of the other.  Note that events
     * may end & start at the same instant and not overlap.
     *
     * @param that the other schedule we want to check
     * @return true if the schedules overlap
     */
    public boolean doesOverlap(final AuctionScheduleItem that)
    {
        final LocalDateTime thisEventStart = this.simulationStartTime;
        final LocalDateTime thisEventEnd = this.simulationStartTime.plus(this.duration);
        final LocalDateTime thatEventStart = that.simulationStartTime;
        final LocalDateTime thatEventEnd = that.simulationStartTime.plus(that.duration);
        // If either the start or the end of that event fall inside the range of this event then they
        // must overlap
        return (thatEventStart.isAfter(thisEventStart) && thatEventStart.isBefore(thisEventEnd)) ||
                (thatEventEnd.isAfter(thisEventStart) && thatEventEnd.isBefore(thisEventEnd));
    }

    /**
     * The auction will not occur if the order manager can't satisfy the pre-condition states, the order
     * manager will place the market into the post condition state if successful.
     */
    private void triggerStart()
    {
        final LocalDateTime auctionStart = timeMachine.toSimulationTime();
        LOG.info("Triggered auction start at: " + timeMachine.formatTime(auctionStart));
        final AuctionTriggerEvent trigger = new AuctionTriggerEvent(this, auctionStart, preConditionStates, MarketState.AUCTION, null, DEFAULT_EXTENSION_DURATION);
        scheduleEnd();
        eventDispatcher.dispatch(trigger);
    }

    /**
     * The auction will not end if the order manager isn't in the auction state
     */
    private void triggerEnd()
    {
        final LocalDateTime auctionEnd = timeMachine.toSimulationTime();
        LOG.info("Triggered auction end at: " + timeMachine.formatTime(auctionEnd));
        final AuctionTriggerEvent trigger = new AuctionTriggerEvent(this, auctionEnd, EnumSet.of(MarketState.AUCTION), postConditionState, null, DEFAULT_EXTENSION_DURATION);
        eventDispatcher.dispatch(trigger);
    }

    Object forciblyStart() throws ExecutionException, InterruptedException
    {
        return startFuture.get();
    }

    Object forciblyEnd() throws ExecutionException, InterruptedException
    {
        return endFuture.get();
    }

    private boolean futureComplete(final ScheduledFuture<?> future)
    {
        return future != null && (future.isDone() || future.isCancelled());
    }

    public boolean isCompleted()
    {
        return futureComplete(startFuture) && futureComplete(endFuture);
    }

    public void cancel()
    {
        if (startFuture == null)
            return;

        synchronized (startFuture)
        {
            startFuture.cancel(true);
            startFuture = null;

            if (endFuture != null)
            {
                endFuture.cancel(true);
                endFuture = null;
            }
        }
    }

    public void reschedule(final TimeMachine timeMachine, final EventDispatcher eventDispatcher)
    {
        cancel();
        this.timeMachine = timeMachine;
        this.eventDispatcher = eventDispatcher;

        wallStartTime = timeMachine.fromSimulationTime(simulationStartTime);
        final Duration waitTime = Duration.between(Instant.now(), wallStartTime);
        final long nanosToWait = Math.max(0, waitTime.toNanos());
        startFuture = eventDispatcher.schedule(this::triggerStart, nanosToWait);
    }

    public void scheduleStart(final TimeMachine timeMachine, final EventDispatcher eventDispatcher)
    {
        if (isCompleted())
            throw new IllegalStateException("Auction schedule item has already completed, can no longer be re-scheduled");
        reschedule(timeMachine, eventDispatcher);
    }

    /**
     * The end of the auction is scheduled after the event for the auction has been sent
     */
    private void scheduleEnd()
    {
        final Instant wallEndTime = timeMachine.fromSimulationTime(simulationStartTime.plus(this.duration));
        final Duration waitDuration = Duration.between(Instant.now(), wallEndTime);
        final long nanosToWait = Math.max(0, waitDuration.toNanos());
        endFuture = eventDispatcher.schedule(this::triggerEnd, nanosToWait);
    }

    @Override
    public String toString()
    {
        return "AuctionScheduleItem{" +
                "name='" + name +
                "', preConditionStates=" + preConditionStates +
                ", simulationStartTime=" + simulationStartTime +
                ", duration=" + duration +
                ", postConditionState=" + postConditionState +
                '}';
    }

    @Override
    public int compareTo(final AuctionScheduleItem o)
    {
        Objects.requireNonNull(o);
        return simulationStartTime.compareTo(o.simulationStartTime);
    }

}
