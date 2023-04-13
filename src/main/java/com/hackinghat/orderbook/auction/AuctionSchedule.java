package com.hackinghat.orderbook.auction;

import com.hackinghat.order.MarketState;
import com.hackinghat.util.EventDispatcher;
import com.hackinghat.util.TimeMachine;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class AuctionSchedule
{
    private final List<AuctionScheduleItem> auctionScheduleItems;

    public final static String OPENING     = "Opening";
    public final static String INTRADAY    = "Intraday";
    public final static String CLOSING     = "Closing";

    /** Although probably not exactly the same as the LSE auction schedule it's a good first approximation **/
    public static AuctionSchedule makeLSESchedule(final LocalDate date)
    {
        final AuctionSchedule schedule = new AuctionSchedule();
        schedule.addAuction(OPENING, EnumSet.of(MarketState.CLOSED), LocalDateTime.of(date.getYear(), date.getMonth(), date.getDayOfMonth(), 7,55,0), Duration.of(5L, ChronoUnit.MINUTES), MarketState.CONTINUOUS);
        schedule.addAuction(INTRADAY, EnumSet.of(MarketState.CONTINUOUS), LocalDateTime.of(date.getYear(), date.getMonth(), date.getDayOfMonth(),12, 0, 0), Duration.of(5L, ChronoUnit.MINUTES), MarketState.CONTINUOUS);
        schedule.addAuction(CLOSING, EnumSet.of(MarketState.CONTINUOUS), LocalDateTime.of(date.getYear(), date.getMonth(), date.getDayOfMonth(),16,30,0), Duration.of(5L, ChronoUnit.MINUTES), MarketState.CLOSED);
        return schedule;
    }

    public AuctionSchedule(AuctionScheduleItem... auctionScheduleItems)
    {
        Objects.requireNonNull(auctionScheduleItems);
        this.auctionScheduleItems = new ArrayList<>();
        Arrays.stream(auctionScheduleItems).forEach(this::addAuction);
    }

    List<AuctionScheduleItem> getAuctionSchedule()
    {
        return Collections.unmodifiableList(auctionScheduleItems);
    }


    public AuctionScheduleItem getAuctionScheduleItem(final String name)
    {
        for (final AuctionScheduleItem item : auctionScheduleItems)
            if (item.getName().equals(name))
                return item;
        throw new IllegalArgumentException("No such auction: " + name);
    }

    /**
     * Schedules an auction to begin at {@param startTime} and continue for {@param duration} milliseconds.  Note
     * that both times are in simulation time.  The {@link MarketManager} will convert the times into wall-clock
     * times using a {@linkplain TimeMachine}.
     *
     * If, when the auction starts, the market is not in the {@param preConditions} states the auction will not take-place.
     * When the auction completes the market will be placed in the {@param postCondition} state.
     * @param preConditions the allowable states prior to the auction (this will be CLOSED for start of day, for example)
     * @param startTime the start time (in simulation time)
     * @param duration the duration (in simulation time)
     * @param postCondition the state that will be assumed immediately following the auction (CLOSED for end-of day for example)
     */
    public AuctionScheduleItem addAuction(final String name, final EnumSet<MarketState> preConditions, final LocalDateTime startTime, final Duration duration, final MarketState postCondition)
    {
        return addAuction(new AuctionScheduleItem(name, preConditions, startTime, duration, postCondition));
    }

    private AuctionScheduleItem addAuction(final AuctionScheduleItem newItem)
    {
        verify(auctionScheduleItems, newItem);
        auctionScheduleItems.add(newItem);
        return newItem;
    }

    static void verify(final List<AuctionScheduleItem> existingEvents, final AuctionScheduleItem event)
    {
        final ArrayList<AuctionScheduleItem> items = new ArrayList<>(existingEvents);
        Collections.sort(items);
        for (final AuctionScheduleItem item : items)
        {
            if (item.doesOverlap(event))
                throw new IllegalStateException("New event: " + event + ", overlaps in the existing schedule, rejecting");
        }
    }

    public void schedule(final TimeMachine timeMachine, final EventDispatcher dispatcher)
    {
        auctionScheduleItems.forEach(ai -> ai.scheduleStart(timeMachine, dispatcher));
    }

    public void reschedule(final TimeMachine timeMachine, final EventDispatcher dispatcher)
    {
        auctionScheduleItems.forEach(ai -> ai.reschedule(timeMachine, dispatcher));
    }

    public void cancel()
    {
        auctionScheduleItems.forEach(AuctionScheduleItem::cancel);
    }
}
