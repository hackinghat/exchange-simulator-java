package com.hackinghat.agent;

import com.hackinghat.model.Instrument;
import com.hackinghat.model.Level;
import com.hackinghat.order.Order;
import com.hackinghat.order.OrderSide;
import com.hackinghat.order.OrderState;
import com.hackinghat.orderbook.Level1;
import com.hackinghat.orderbook.OrderInterest;
import com.hackinghat.simulator.OrderBookSimulatorImpl;
import com.hackinghat.util.RandomSource;
import com.hackinghat.util.TimeMachine;
import com.hackinghat.util.mbean.MBeanType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.hackinghat.order.OrderSide.BUY;
import static com.hackinghat.order.OrderSide.SELL;

/**
 * Maintains two orders around the mid that are {@link ZeroIntelligenceMarketMaker#nSpreadLevels} apart.  It re-evaluates its
 * orders every {@link ZeroIntelligenceMarketMaker#evaluateOrders} duration elapses and if the orders are filled will replenish
 * them.   The market-maker assumes it can go short (both cash & stock), which is in-line with most market expectations.
 *
 * The market maker will not attempt to enter two-way prices during an auction.
 */
@MBeanType(description = "Zero intelligence market maker")
public class ZeroIntelligenceMarketMaker extends Agent
{
    private static final Logger LOG = LogManager.getLogger(ZeroIntelligenceMarketMaker.class);

    private final OrderBookSimulatorImpl simulator;
    private final   Duration            evaluateOrders;
    private final   int                 bidOrderSize;
    private final   int                 offerOrderSize;
    private final   int                 spreadTolerance;
    private final   boolean             cancelIfTop;

    private         int                 nSpreadLevels;
    private         Order               bid;
    private         Order               offer;
    private         long                clientId;

    Order getBid() { return bid; }
    Order getOffer() { return offer; }

    /**
     * Zero intelligence market maker.
     * @param id of the market maker
     * @param instrument the instrument they are market making on
     * @param randomSource for generating behaviour
     * @param timeMachine to convert wall-clock time to simulation time
     * @param evaluateOrders duration
     * @param name a name for this agent that we can use to trace its activity
     * @param simulator a referene to the order simulator
     * @param spreadLevels the number of ticks between the market maker's bid & offer
     * @param spreadTolerance the amount of ticks a market making order can be out by before it will adjust its order
     * @param cancelIfTop should the market maker get to the top of either bid or offer book, then cancel the order
     */
    public ZeroIntelligenceMarketMaker(final Long id, final Instrument instrument, final RandomSource randomSource, final TimeMachine timeMachine, final Duration evaluateOrders, final String name, final OrderBookSimulatorImpl simulator, final int spreadLevels, final int spreadTolerance, final int bidOrderSize, final int offerOrderSize, final boolean cancelIfTop) {
        super(id, instrument, randomSource, timeMachine, name, simulator.getEventDispatcher(), false);
        if (nSpreadLevels % 2 == 1)
            throw new IllegalArgumentException("Spread levels should be even for a 'zero' intelligence market-maker!");
        this.nSpreadLevels = spreadLevels / 2;
        this.evaluateOrders = evaluateOrders;
        this.simulator = simulator;
        this.clientId = 0;
        this.bidOrderSize = bidOrderSize;
        this.offerOrderSize = offerOrderSize;
        this.spreadTolerance = spreadTolerance;
        this.cancelIfTop = cancelIfTop;
    }

    @Override
    public Duration wakeUp() {
        return evaluateOrders;
    }

    private Order replaceOrder(final OrderSide side, final Order newOrder)
    {
        final Order nullIfTerminated = (newOrder != null && OrderState.isTerminal(newOrder.getState())) ? null : newOrder;
        switch (side)
        {
            case BUY:
                bid = nullIfTerminated;
                break;
            case SELL:
                offer = nullIfTerminated;
                break;
            default:
                throw new IllegalArgumentException("Unexpected side: " + side + " when trying to replace order");
        }
        return nullIfTerminated;
    }

    private Level getLevelForSide(final OrderSide side, final Level1 level1)
    {
        return instrument.worsenOnBook(level1.getMid(instrument), side, nSpreadLevels);
    }

    private Order generateOrder(final OrderSide side, final Level1 level1, final int currentQuantity)
    {
        final Level requiredLevel = getLevelForSide(side, level1);
        final String id = "M-" + getName() + "-" + clientId++;
        return  new Order(id, side, instrument, requiredLevel, currentQuantity, this, timeMachine, false);
    }

    boolean orderNeedsAmendmentDueToBookPosition(final OrderPosition position)
    {
        return position == OrderPosition.OutsideTolerance || position == OrderPosition.BelowAllowed || position == OrderPosition.SharedTop || position == OrderPosition.NoOrder;
    }

    /**
     * Given information for the side
     * @param side the side to assess
     * @param currentOrder for this market maker
     * @param currentQuantity of the quote
     * @return either a zero length order array, a single order ()
     */
    private Collection<Order> calculateOrderActions(final Level1 level1, final OrderSide side, final Order currentOrder, final int currentQuantity)
    {
        final boolean canClear = level1 != null && level1.getMarketState().isClearingState() && !level1.getPrice(side).isMarket();
        final List<Order> result = new ArrayList<>();
        final boolean needsReplenish = currentOrder != null && currentOrder.getRemainingQuantity() < currentQuantity;
        final boolean hasOrder = currentOrder != null;
        final OrderPosition position = calculateOrderPosition(level1, currentOrder);
        final boolean atTop = OrderPosition.SoleTop == position;
        final boolean needsAmendment = orderNeedsAmendmentDueToBookPosition(position);

        // The market is shut or the order needs replenishment or amendment
        if ((!canClear || needsReplenish || (atTop && cancelIfTop) || needsAmendment) && hasOrder)  {
            currentOrder.cancel(timeMachine.toSimulationTime());
            result.add(currentOrder);
        }

        // If the market is clearing and the order needs amendment then create a new order now
        if (canClear && (!hasOrder || needsReplenish || needsAmendment)) {
            final Order newOrder = generateOrder(side, level1, currentQuantity);
            newOrder.init(timeMachine);
            result.add(replaceOrder(side, newOrder));
        }
        return result;
    }

    enum OrderPosition {
        /* If we are the sole top of the book then we're leading the market price, when this occurs cancel our order and
           await a new opportunity */
        SoleTop,
        /* If we are shared top then there is another order at the top of the book to support the price we also have, so
           we simply need to move our order */
        SharedTop,
        /* Some markets will mandate that the market maker's spread can be no wider than a certain amount, this position
           indicates that we've fallen outside that tolerance  */
        BelowAllowed,
        /* Our order is not at the spread. but it's within tolerance, likely no action is required */
        InsideTolerance,
        /* Our order is not at the spread, and it's outside tolerance, likely no action is required */
        OutsideTolerance,
        /* Our order is correctly positions */
        AtSpread,
        /* There's no order currently */
        NoOrder }

    /**
     * Our actions as a market maker ultimately depend on how our order sits within the bid/offer books.   This calculation
     * returns the most relevant fact about our order.  So for instance, an order can be both {@link OrderPosition#SoleTop}
     * and {@link OrderPosition#AtSpread}, however only the former is relevant for order management.
     * @param level1 the current {@linkplain Level1} in the market
     * @param order the order we want to compare against the book
     * @return a {@link OrderPosition} within the book
     */
    OrderPosition calculateOrderPosition(final Level1 level1, final Order order)
    {
        if (order == null)
            return OrderPosition.NoOrder;
        final OrderInterest interest = level1.getInterest(order.getSide());
        if (interest.getLevel().levelCompare(order.getSide(), order.getLevel()) == 0)
            return interest.getQuantity() == order.getRemainingQuantity().longValue() ? OrderPosition.SoleTop : OrderPosition.SharedTop;

        final Level expected = getLevelForSide(order.getSide(), level1);
        final int orderRelativeToExpected = order.getLevel().levelCompare(order.getSide(), expected);
        final int distanceFromExpected = order.getLevel().ticksBetweenSameSide(expected, order.getSide());
        if (orderRelativeToExpected == 0)
            return OrderPosition.AtSpread;
        else
        {
            if (distanceFromExpected < 0)
                return OrderPosition.BelowAllowed;
            else if (distanceFromExpected - spreadTolerance < 0)
                return OrderPosition.InsideTolerance;
            else
                return OrderPosition.OutsideTolerance;
        }
    }

    @Override
    public void orderUpdate(final Order orderChanged)
    {
        synchronized (sync)
        {
            super.orderUpdate(orderChanged);
            if (orderChanged.equals(getBid()))
                replaceOrder(OrderSide.BUY, orderChanged);
            else if (orderChanged.equals(getOffer()))
                replaceOrder(SELL, orderChanged);
            else if (bid != null && offer != null)
                LOG.warn(getName() + " received update to an order we didn't recognise: " + orderChanged);
        }
    }

    @Override
    protected void doActions() {
        if (LOG.isTraceEnabled())
            LOG.trace(getName() + ", Bid = " + getBid() + ", Order = " + getOffer());
        final Level1 level1 = simulator.getLevel1();
        simulator.add(calculateOrderActions(level1, BUY, bid, bidOrderSize));
        simulator.add(calculateOrderActions(level1, SELL, offer, offerOrderSize));
    }


}
