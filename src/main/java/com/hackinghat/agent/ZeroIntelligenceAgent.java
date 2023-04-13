package com.hackinghat.agent;

import com.hackinghat.model.Instrument;
import com.hackinghat.order.*;
import com.hackinghat.orderbook.*;
import com.hackinghat.simulator.OrderBookSimulatorImpl;
import com.hackinghat.model.Level;
import com.hackinghat.util.Pair;
import com.hackinghat.util.RandomSource;
import com.hackinghat.util.TimeMachine;
import com.hackinghat.util.mbean.MBeanType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

@MBeanType(description = "Zero Intelligence Agent")
public class ZeroIntelligenceAgent extends Agent
{
    private static final Logger LOG = LogManager.getLogger(ZeroIntelligenceAgent.class);
    private enum ActionType { LIMIT, CANCEL, MARKET }

    private final double      pCancel;
    private final double      pBuy;
    private final double      pMarket;
    private final double      pInSpread;
    private final Duration    sleepT1;
    private final Duration    sleepT2;
    private final TimeMachine timeMachine;
    private final OrderBookSimulatorImpl simulator;

    private double      alpha;
    private double      sizeSigma;
    private double      sizeMean;
    private long        clientId;

    public ZeroIntelligenceAgent(final Long id, final Instrument instrument, final RandomSource randomSource, final TimeMachine timeMachine, final Duration sleepT1, final Duration sleepT2, final String name, final OrderBookSimulatorImpl simulator, final double pCancel, final double pMarket, final double pBuy, final double pInSpread)
    {
        super(id, instrument, randomSource, timeMachine, name, simulator.getEventDispatcher(), false);
        this.pCancel = pCancel;
        this.pBuy = pBuy;
        this.pMarket = pMarket;
        this.pInSpread = pInSpread;
        this.clientId = 0;
        this.sleepT1 = sleepT1;
        this.sleepT2 = sleepT2;
        this.timeMachine = timeMachine;
        this.simulator = simulator;
    }

    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    public void setSizeSigma(double sizeSigma) {
        this.sizeSigma = sizeSigma;
    }

    public void setSizeMean(double sizeMean) {
        this.sizeMean = sizeMean;
    }

    private ActionType nextActionType()
    {
        // Probability of cancel = pCancel
        // Probability of market = pMarket
        // Probability of limt = 1-pCancel-pMarket
        double sample = randomSource.nextDouble();
        if (sample <= pCancel) return ActionType.CANCEL;
        return (sample <= pCancel + pMarket) ? ActionType.MARKET : ActionType.LIMIT;
    }

    private OrderSide nextSide()
    {
        return randomSource.nextDouble() <= pBuy ? OrderSide.BUY : OrderSide.SELL;
    }

    private Pair<Level, String> nextOutOfSpreadPrice(final OrderSide side, final Level level)
    {
        // Pick a level outside the current spread
        int ticks = randomSource.nextPower(1, alpha);
        final String note = "OUT(" + ticks + ")";
        return Pair.instanceOf(instrument.worsenOnBook(level, side, ticks), note);
    }

    private Pair<Level, String> nextPrice(final OrderSide side, final Level1 level1Price)
    {
        double inSpread = randomSource.nextDouble();
        if (level1Price.getTouchState().hasSpread() && inSpread < pInSpread)
        {
            // Select a random 'in-spread' tick value in the interval [best bid, best offer], note that we shouldn't be able to select a price
            // that will cross because if there is one tick between bid and offer then the only possible return value of this calculation is the best same side price
            try
            {
                // We want negative ticks to move closer to the far side (positive ticks move away from the near side)
                int ticks = randomSource.nextInt(level1Price.ticksBetweenBidAndOffer());
                final Level best = level1Price.getPrice(side);
                final String note = "IN(" + ticks + ")";
                return Pair.instanceOf(instrument.betterOnBook(best, side, ticks), note);
            }
            catch (final InvalidMarketStateException invalidState)
            {
                LOG.debug("No market so can't calculate a limit order price");
                return null;
            }
        }
        else
        {
            return nextOutOfSpreadPrice(side, level1Price.getPrice(side));
        }
    }

    boolean canAfford(final Order order, final Level1 marketTouch, final Level last)
    {
        switch (order.getSide())
        {
            case BUY:
                BigDecimal estimate = estimatePrice(order.getSide(), marketTouch, last);
                if (estimate == null)
                    return false;
                return order.getConsideration(estimate) < getCash();
            case SELL:
                return order.getRemainingQuantity() < getShares();
            default:
                throw new IllegalArgumentException("Unrecognised order side: " + order.getSide());
        }
    }

    private Integer nextVolume()
    {
        return  Double.valueOf(randomSource.nextLogNormal(sizeMean, sizeSigma)).intValue();
    }

    private BigDecimal estimatePrice(final OrderSide side, final Level1 level1, final Level reference)
    {
        BigDecimal best = level1.getPrice(side).getPrice();
        return BigDecimal.ZERO.equals(best) ? (reference == null ? null : reference.getPrice()) : best;
    }

    public static String getHeaders() { return "Name,Inspread,Buy,Market,Cancel"; }

    @Override
    public String formatStatistic(final TimeMachine timeMachine)
    {
        final StringBuilder statistic = new StringBuilder();
        format(statistic, null, getName(), false);
        formatStatistic(statistic, pInSpread, false);
        formatStatistic(statistic, pBuy, false);
        formatStatistic(statistic, pMarket, false);
        formatStatistic(statistic, pCancel, true);
        return statistic.toString();
    }

    @Override
    public Duration wakeUp() {
        double chooseWakeup = randomSource.nextDouble();
        long lambda = ((chooseWakeup < 0.5d) ? sleepT1 : sleepT2).toMillis();
        return Duration.of(Double.valueOf(randomSource.nextExponential(1.0/lambda)).longValue(), ChronoUnit.MILLIS);
    }

    @Override
    protected void doActions()
    {
        if (LOG.isTraceEnabled())
            LOG.trace(getName() + ": Working (cash = " + decimalFormatThread.get().format(getCash()) + ", shares = " + getShares() + ")");

        Order action = null;
        // Cancel at most one
        ActionType actionType = nextActionType();
        final Level1 level1 = simulator.getLevel1();
        switch (actionType)
        {
            case CANCEL:
            {
                // The orders are in time-priority order so the oldest will be cancelled first
                for (final Order submitted : getOutstandingOrders())
                {
                    if (!OrderState.isPending(submitted.getState()))
                    {
                        action = submitted;
                        action.cancel(timeMachine.toSimulationTime());
                        break;
                    }
                }
                if (action == null) {
                    if (LOG.isTraceEnabled())
                        LOG.trace(getName() + ": nothing to cancel");
                }
                break;
            }
            case MARKET: {
                final OrderSide side = nextSide();
                action = new Order("A-" + getName() + "-" + clientId++, side, simulator.getInstrument(), Level.MARKET, nextMarketVolume(side, level1), this, timeMachine);
                break;
            }
            case LIMIT: {
                Integer quantity = nextVolume();
                OrderSide side = nextSide();
                BigDecimal price;
                String note = null;
                if (MarketState.CONTINUOUS.equals(level1.getTouchState()))
                {
                    Pair<Level, String> calculatedPrice = nextPrice(side, level1);
                    price = calculatedPrice.getFirst().getPrice();
                    note = calculatedPrice.getSecond();
                }
                else
                {
                    final boolean inAuction = MarketState.AUCTION.equals(level1.getMarketState());
                    final Level refPrice = simulator.getReferencePrice();
                    if (refPrice == null)
                        throw new IllegalStateException("There is no apparent reference price, this seems wrong!");
                    else
                    {
                        // Get a price, but don't use the existing side if we're in an auction (otherwise we'll never
                        // get an uncrossing price)
                        Pair<Level, String> pricePair = nextOutOfSpreadPrice(inAuction ? nextSide() : side, refPrice);
                        price = pricePair.getFirst().getPrice();
                        note = pricePair.getSecond();
                    }

                }
                action = new Order("C-" + getName() + "-" + clientId++, side, simulator.getInstrument(), price, quantity, this, timeMachine);
                if (note != null)
                    action.addNote(note);
                break;
            }
        }
        if (action != null)
        {
            if (actionType != ActionType.CANCEL && !canAfford(action, level1, simulator.getReferencePrice()))
            {
                if (LOG.isTraceEnabled())
                    LOG.trace("Order fails affordability checks so discarding: " + action);
                // The creation of the order added it to the outstanding order list, so now remove it again
                outstandingOrders.remove(action);
            } else {
                action.setReferencePrice(level1);
                simulator.add(action);
            }
            if (actionType == ActionType.CANCEL)
                cancelCount++;
            else
                newOrderCount++;
        }
        if (LOG.isTraceEnabled())
            LOG.trace(getName() + ": Done " + (action == null ? "no action" : actionType.toString()));
    }

    private Integer nextMarketVolume(final OrderSide side, final Level1 level1)
    {
        final long nextVol = nextVolume();
        return Math.toIntExact(Math.min(nextVol, level1.getInterest(OrderSide.getOther(side)).getQuantity()));
    }
}
