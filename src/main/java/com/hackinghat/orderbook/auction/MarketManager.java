package com.hackinghat.orderbook.auction;

import com.hackinghat.model.Level;
import com.hackinghat.model.Trade;
import com.hackinghat.order.MarketState;
import com.hackinghat.order.MarketStateEvent;
import com.hackinghat.util.*;
import com.hackinghat.util.component.AbstractComponent;
import com.hackinghat.util.mbean.MBeanAttribute;
import com.hackinghat.util.mbean.MBeanType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;

import static com.hackinghat.orderbook.auction.AuctionScheduleItem.DEFAULT_EXTENSION_DURATION;
import static com.hackinghat.orderbook.auction.AuctionTriggerEvent.DEFAULT_AUCTION_STATES;
import static com.hackinghat.orderbook.auction.AuctionTriggerEvent.DEFAULT_PRE_AUCTION_STATES;

/**
 * The market manager observes conditions in the market and advises how trading should proceed.
 * It is synchronously called upon by the {@link com.hackinghat.orderbook.OrderManager} to see if an unscheduled
 * auction is required.  When an auction is required it will schedule the auction's end, this will cause
 * a new {@link AuctionTriggerEvent} to close the auction.
 * <p>
 * It also holds the auction schedule which may asynchronously trigger an auction event, it doesn't control
 * this directly but it has the control to end the auction schedule.
 * <p>
 * Things that trigger an auction are:
 * 1. the touch enters into 'BACK'
 * 2. a trade is placed more than {@see priceMoveThreshold}% away from the previous trade
 * 3. a start-of day or intra-day auction occurs
 * 4. trading begins or trading ends
 * 5. an unexpected state is detected, causing the manager to try to shut the market
 */
@MBeanType(description = "Market Manager")
public class MarketManager extends AbstractComponent implements Listener {
    private static final Logger LOG = LogManager.getLogger(MarketManager.class);
    private final EventDispatcher dispatcher;
    private final TimeMachine timeMachine;
    private final Object sync;

    // These could be potentially changed by the UI?
    private final double priceMoveThreshold;
    private final Duration intradayAuctionDuration;
    private final AuctionSchedule auctionSchedule;
    ScheduledFuture<?> inProgressAuction;
    private Level lastLevel;
    private boolean priceMonitoring;

    /**
     * Make a new market manager
     *
     * @param referenceLevel   the reference level, usually the uncrossing price from the prior day
     * @param auctionThreshold the ratio threshold that will trigger an auction
     * @param timeMachine      the time simulation
     * @param dispatcher       how the manager schedules and relays market status change information
     * @param auctionSchedule  the time schedule for the auctions this manager will preside over
     */
    public MarketManager(final Level referenceLevel, final double auctionThreshold, final Duration intradayAuctionDuration, final TimeMachine timeMachine, final EventDispatcher dispatcher, final AuctionSchedule auctionSchedule) {
        super("MarketManager");
        Objects.requireNonNull(dispatcher);
        Objects.requireNonNull(timeMachine);
        Objects.requireNonNull(auctionSchedule);

        this.priceMoveThreshold = auctionThreshold;
        this.timeMachine = timeMachine;
        this.lastLevel = referenceLevel;
        this.inProgressAuction = null;
        this.dispatcher = require(dispatcher);
        this.dispatcher.addListener(AuctionTriggerEvent.class, this);
        this.dispatcher.addListener(TimeSpeedChangeEvent.class, this);
        this.sync = new Object();
        this.auctionSchedule = auctionSchedule;
        this.intradayAuctionDuration = intradayAuctionDuration;
        this.priceMonitoring = true;
    }

    @MBeanAttribute(description = "Last")
    public float getLast() {
        return lastLevel.getPrice();
    }

    Level getLastLevel() {
        return lastLevel;
    }

    @MBeanAttribute(description = "Price monitoring")
    public boolean isPriceMonitoring() {
        return priceMonitoring;
    }

    @MBeanAttribute(description = "Price monitoring")
    public void setPriceMonitoring(final boolean priceMonitoring) {
        this.priceMonitoring = priceMonitoring;
    }

    private AuctionTriggerEvent startAuction(final Trade cause) {
        LOG.info("Auction priceMoveThreshold breached, auction will be triggered");
        // We dispatch a delayed event to finish the auction
        final long nanosToWait = timeMachine.simulationPeriodToWall(intradayAuctionDuration, ChronoUnit.NANOS);
        inProgressAuction = dispatcher.delayedDispatch(new AuctionTriggerEvent(this, timeMachine.toSimulationTime(), DEFAULT_AUCTION_STATES, MarketState.CONTINUOUS, lastLevel, DEFAULT_EXTENSION_DURATION), nanosToWait);
        // We don't dispatch the auction trigger we return it so that the caller can decide when to publish it
        return new AuctionTriggerEvent(cause.getSender(), timeMachine.toSimulationTime(), DEFAULT_PRE_AUCTION_STATES, MarketState.AUCTION, lastLevel, DEFAULT_EXTENSION_DURATION);
    }

    public AuctionTriggerEvent priceMonitor(final Trade observed) {
        if (!priceMonitoring)
            return null;

        if (isAuctionInProgress())
            throw new IllegalStateException("Received an observed trade with an auction still in progress: " + observed);

        final float thisPrice = observed.getLevel().getPrice();
        final float lastPrice = getLastLevel().getPrice();
        // Check if the price has moved by more the threshold, we try to keep it numerically stable by
        // looking for a %age difference in excess of threshold + 0.00001%
        // TODO: Needs to be checked to make sure the float sums only trigger an auction when the result must be bigger than the threshold
        final boolean auctionRequired = (Math.abs(1 - thisPrice / lastPrice) - priceMoveThreshold) > 1E-5;
        lastLevel = observed.getLevel();
        return auctionRequired ? startAuction(observed) : null;
    }

    private void notifyAuctionEvent(final AuctionTriggerEvent auctionTriggerEvent) {
        LOG.debug("Auction event " + auctionTriggerEvent);
    }

    private void notifyMarketStateEvent(final MarketStateEvent marketStateEvent) {
        if (marketStateEvent.getNewMarketState() == MarketState.BACK) {
            LOG.info("Detected a backward market");
//            dispatcher.dispatch(new AuctionTriggerEvent(this, timeMachine.toSimulationTime(), MarketState.AUCTION));
        }
    }

    public boolean isAuctionInProgress() {
        return inProgressAuction != null && !(inProgressAuction.isCancelled() || inProgressAuction.isDone());
    }

    /**
     * The time machine might have its run speed adjusted, that should require us to reschedule all the auction events
     *
     * @param timeSpeedChangeEvent the change in speed
     */
    private void notifyTimeEvent(final TimeSpeedChangeEvent timeSpeedChangeEvent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void notify(final Event event) {
        // We only want one notification to occur concurrently
        synchronized (sync) {
            if (event instanceof AuctionTriggerEvent)
                notifyAuctionEvent((AuctionTriggerEvent) event);
            else if (event instanceof TimeSpeedChangeEvent)
                notifyTimeEvent((TimeSpeedChangeEvent) event);
            else if (event instanceof MarketStateEvent)
                notifyMarketStateEvent((MarketStateEvent) event);
            else
                throw new IllegalArgumentException("Found unexpected event type: " + event);
        }
    }

    public void start() {
        auctionSchedule.schedule(timeMachine, dispatcher);
    }

    public void restart() {
        auctionSchedule.reschedule(timeMachine, dispatcher);
    }

    public void stop() {
        auctionSchedule.cancel();
    }
}
