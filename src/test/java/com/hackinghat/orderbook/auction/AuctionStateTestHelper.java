package com.hackinghat.orderbook.auction;

import com.hackinghat.agent.Agent;
import com.hackinghat.agent.NullAgent;
import com.hackinghat.model.Instrument;
import com.hackinghat.model.Level;
import com.hackinghat.order.Order;
import com.hackinghat.order.OrderSide;
import com.hackinghat.orderbook.OrderBook;
import com.hackinghat.orderbook.OrderManager;
import com.hackinghat.util.NotSoRandomSource;
import com.hackinghat.util.SyncEventDispatcher;
import com.hackinghat.util.TimeMachine;
import com.hackinghat.util.component.AbstractComponent;
import com.hackinghat.util.mbean.MBeanType;

import static com.hackinghat.orderbook.OrderTest.limitOrder;
import static com.hackinghat.orderbook.OrderTest.marketOrder;

/**
 * The auction state calculations are based upon an LSE technical document I found on the web:
 * "Release 3.1, worked examples London Stock Exchange market enhancements, March 2000"
 * <p>
 * {@link "https://www.londonstockexchange.com/products-and-services/technical-library/technical-guidance-notes/technicalguidancenotesarchive/release.pdf"}
 * <p>
 * Although the document is fairly old it does detail specific scenarios and how they should be
 * treated.  It's not our intention to have an exact LSE simulator, rather something that has features
 * approaching a real market, so this should be sufficient.
 */
@MBeanType(description = "Auction state helper")
public class AuctionStateTestHelper extends AbstractComponent {
    private static long orderId = 0L;
    private final TimeMachine timeMachine;
    private final OrderBook bidBook;
    private final OrderBook offerBook;
    private final Level referenceLevel;
    private final Instrument instrument;
    private final Agent nullAgent;
    private final OrderManager orderManager;

    public AuctionStateTestHelper(final TimeMachine timeMachine, final Instrument instrument, final Level referenceLevel, final OrderBook bidBook, final OrderBook offerBook) {
        this(timeMachine, instrument, referenceLevel, null, bidBook, offerBook);
    }

    public AuctionStateTestHelper(final TimeMachine timeMachine, final Instrument instrument, final Level referenceLevel, final OrderManager orderManager) {
        this(timeMachine, instrument, referenceLevel, orderManager, orderManager.getQueue(OrderSide.BUY), orderManager.getQueue(OrderSide.SELL));
    }

    public AuctionStateTestHelper(final TimeMachine timeMachine, final Instrument instrument, final Level referenceLevel, final OrderManager orderManager, final OrderBook bidBook, final OrderBook offerBook) {
        super("AuctionStateTestHelper");
        this.timeMachine = timeMachine;
        this.referenceLevel = referenceLevel;
        this.instrument = instrument;
        this.orderManager = require(orderManager);
        this.bidBook = require(bidBook);
        this.offerBook = require(offerBook);
        this.nullAgent = require(new NullAgent(0L, instrument, new NotSoRandomSource(), timeMachine, "AGENT-" + instrument.getTicker(), new SyncEventDispatcher(timeMachine)));
    }

    private void addOrder(final Order order) {
        if (hasOrderManager()) {
            orderManager.add(order);
            orderManager.process();
        } else {
            sideToBook(order.getSide()).newOrder(order);
        }
    }

    private boolean hasOrderManager() {
        return orderManager != null;
    }

    OrderBook sideToBook(final OrderSide side) {
        assert (!hasOrderManager());
        switch (side) {
            case BUY:
                return bidBook;
            case SELL:
                return offerBook;
            default:
                throw new IllegalArgumentException("Unknown side: " + side);
        }
    }

    long addLimit(final OrderSide side, final int quantity, final float limit) {
        final long id = orderId++;
        addOrder(limitOrder(id, side, instrument, limit, quantity, nullAgent, timeMachine, !hasOrderManager()));
        return id;
    }

    long addMarket(final OrderSide side, final int quantity) {
        final long id = orderId++;
        addOrder(marketOrder(id, side, instrument, quantity, nullAgent, timeMachine, !hasOrderManager()));
        return id;
    }

    /**
     * See: pp 12, Release 3.1 market enhancements
     *
     * @return This market state has a single best price (104.5), with a volume of 10,400
     */
    public AuctionState makeState1() {
        addLimit(OrderSide.BUY, 10000, 105.5f);
        addLimit(OrderSide.BUY, 5600, 104.5f);
        addLimit(OrderSide.BUY, 200, 104.0f);
        addMarket(OrderSide.SELL, 2500);
        addLimit(OrderSide.SELL, 6900, 103.0f);
        addLimit(OrderSide.SELL, 1000, 104.5f);
        addLimit(OrderSide.SELL, 200, 106.0f);
        return new AuctionState(timeMachine.toSimulationTime(), referenceLevel, instrument, bidBook, offerBook);
    }

    /**
     * See: pp 14, Release 3.1 market enhancements
     *
     * @return This market state has two prices, but one satisfies the minimum getSurplus (104.5)
     */
    public AuctionState makeState2() {
        addLimit(OrderSide.BUY, 10000, 105.5f);
        addLimit(OrderSide.BUY, 5600, 104.5f);
        addLimit(OrderSide.BUY, 200, 104.0f);
        addMarket(OrderSide.SELL, 2500);
        addLimit(OrderSide.SELL, 6900, 103.0f);
        addLimit(OrderSide.SELL, 1000, 104.0f);
        addLimit(OrderSide.SELL, 200, 106.0f);
        return new AuctionState(timeMachine.toSimulationTime(), referenceLevel, instrument, bidBook, offerBook);
    }

    /**
     * See: pp 15, Release 3.1 market enhancements
     *
     * @return This market state has three levels that maximise volume (104, 104.5  & 105), two
     * of these minimise the auction surplus to 5,200 shares (104.5, 105.0).  Because the surpluses
     * are on the same side of the book (BUY) we select the best bid (105.0)
     */
    public AuctionState makeState3() {
        addLimit(OrderSide.BUY, 10000, 105.5f);
        addLimit(OrderSide.BUY, 5600, 105.0f);
        addLimit(OrderSide.BUY, 1000, 104.0f);
        addMarket(OrderSide.SELL, 2500);
        addLimit(OrderSide.SELL, 6900, 103.0f);
        addLimit(OrderSide.SELL, 1000, 104.0f);
        addLimit(OrderSide.SELL, 200, 106.0f);
        return new AuctionState(timeMachine.toSimulationTime(), referenceLevel, instrument, bidBook, offerBook);
    }

    /**
     * See: pp 16, Release 3.1 market enhancements
     *
     * @return This market state has two levels which maximise the volume (104, 104.5), the auction
     * surplus are on opposite sides of the book, meaning there is equal presurre exerted
     * after the auction is closed.  Therefore the level that is closest to the reference
     * level is selected.
     */
    public AuctionState makeState4(final Level referenceLevel) {
        addLimit(OrderSide.BUY, 6000, 105.0f);
        addLimit(OrderSide.BUY, 1000, 104.5f);
        addLimit(OrderSide.BUY, 1000, 104.0f);
        addLimit(OrderSide.BUY, 6000, 103.5f);

        addLimit(OrderSide.SELL, 6000, 103.0f);
        addLimit(OrderSide.SELL, 1000, 104.0f);
        addLimit(OrderSide.SELL, 1000, 104.5f);
        addLimit(OrderSide.SELL, 6000, 105.5f);
        return new AuctionState(timeMachine.toSimulationTime(), referenceLevel, instrument, bidBook, offerBook);
    }
}
