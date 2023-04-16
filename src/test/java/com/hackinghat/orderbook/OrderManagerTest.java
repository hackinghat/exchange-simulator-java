package com.hackinghat.orderbook;

import com.hackinghat.agent.Agent;
import com.hackinghat.agent.AgentImplTest;
import com.hackinghat.agent.NullAgent;
import com.hackinghat.instrument.ConstantTickSizeToLevelConverter;
import com.hackinghat.model.Currency;
import com.hackinghat.model.Trade;
import com.hackinghat.order.*;
import com.hackinghat.model.Instrument;
import com.hackinghat.model.Level;
import com.hackinghat.orderbook.auction.*;
import com.hackinghat.statistic.Statistic;
import com.hackinghat.util.*;
import org.junit.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static com.hackinghat.util.component.AbstractComponentTest.checkNumberOfMBeans;

public class OrderManagerTest
{
    private Instrument VOD;
    private OrderManager manager;
    private StatisticsAppenderTestHelper tape;
    private TimeMachine timeMachine;
    private AgentImplTest buyer;
    private AgentImplTest seller;
    private NullAgent nullAgent;
    private MarketManager marketManager;
    private long cId;
    private SyncEventDispatcher dispatcher;
    private Level referencePrice;

    private int nMBeans;



    @Before
    public void setUp()
    {
        checkNumberOfMBeans(0, "OrderManagerTest.setup");
        VOD = new Instrument("VOD.L", new Currency("GBP"), new ConstantTickSizeToLevelConverter(1, 100, 2));
        tape = new StatisticsAppenderTestHelper();
        timeMachine = new TimeMachine();
        dispatcher = new SyncEventDispatcher(timeMachine);
        referencePrice = VOD.getLevel(100.0);
        marketManager = new MarketManager(referencePrice, 0.1, Duration.of(5L, ChronoUnit.MINUTES), timeMachine, dispatcher, new AuctionSchedule());
        manager = new OrderManager(marketManager, timeMachine, null, MarketState.CONTINUOUS, VOD, dispatcher, tape, null, Duration.ZERO);
        cId = 0;
        buyer = new AgentImplTest(VOD, 1L);
        seller = new AgentImplTest(VOD, 2L);
        nullAgent = new NullAgent(0L, VOD, new NotSoRandomSource(), timeMachine, "AGENT-0", null);
    }

    @After
    public void teardown() {
        marketManager.shutdown();
        manager.shutdown();
        Assert.assertTrue(manager.isClosed());
        Assert.assertTrue(manager.getQueue(OrderSide.BUY).isClosed());
        buyer.shutdown();
        seller.shutdown();
        nullAgent.shutdown();
        checkNumberOfMBeans(0, "OrderManagerTest.teardown");
    }

    private void checkTouch(double expectedBid, int expectedBidCount, long expectedBidVolume, double expectedOffer, int expectedOfferCount, long expectedOfferVolume, MarketState expectedState)
    {
        final Level1 level1 = manager.getLevel1();
        OrderInterestTest.checkInterest(OrderSide.BUY, level1.getBid(), VOD.roundToTick(expectedBid), expectedBidCount, expectedBidVolume);
        OrderInterestTest.checkInterest(OrderSide.SELL, level1.getOffer(), VOD.roundToTick(expectedOffer), expectedOfferCount, expectedOfferVolume);
        Assert.assertEquals(expectedState, manager.getLevel1().getTouchState());
    }

    @SuppressWarnings("SameParameterValue")
    private void checkTouch(BigDecimal expectedBid, int expectedBidCount, long expectedBidVolume, BigDecimal expectedOffer, int expectedOfferCount, long expectedOfferVolume, MarketState expectedState)
    {
        Level1 level1 = manager.getLevel1();
        OrderInterestTest.checkInterest(OrderSide.BUY, level1.getBid(), expectedBid, expectedBidCount, expectedBidVolume);
        OrderInterestTest.checkInterest(OrderSide.SELL, level1.getOffer(), expectedOffer, expectedOfferCount, expectedOfferVolume);
        Assert.assertEquals(expectedState, manager.getLevel1().getTouchState());
    }


    @SuppressWarnings("SameParameterValue")
    private void checkChanges(Agent buyer, double buyerCash, int buyerShares, Agent seller, double sellerCash, int sellerShares)
    {
        assertEquals("Buyer cash", buyerCash, buyer.cashChange(), 1E-2);
        assertEquals("Seller cash", sellerCash, seller.cashChange(), 1E-2);
        assertEquals("Buyer shares", buyerShares, buyer.sharesChange());
        assertEquals("Seler shares", sellerShares, seller.sharesChange());
    }

    private void checkNet(Agent buyer, Agent seller)
    {
        assertEquals(0, buyer.sharesChange() + seller.sharesChange());
        assertEquals(0.0, buyer.cashChange() + seller.cashChange(), 1E-6);
    }

    @SuppressWarnings("SameParameterValue")
    private Pair<Order, Order> setupTop(double bidPrice, double offerPrice, int quantity)
    {
        final Order bidOrder = submitLimit(buyer, OrderSide.BUY, bidPrice, manager, quantity);
        final Order offerOrder = submitLimit(seller, OrderSide.SELL, offerPrice, manager, quantity);
        manager.process();
        checkNet(buyer, seller);
        return Pair.instanceOf(bidOrder, offerOrder);
    }


    private String nextId() { return "C" + ++cId; }

    private Order submitMarket(final Agent sender, final OrderSide side, final OrderManager orderManager, int... quantities)
    {
        Order last = null;
        for (int quantity : quantities)
        {
            last = orderManager.add(new Order(nextId(), side, VOD, Level.MARKET, quantity, sender, timeMachine))[0];
        }
        orderManager.process();
        return last;
    }

    private Order submitLimit(Agent sender, OrderSide side, double price, final OrderManager orderManager, int... quantities)
    {
        return submitLimit(sender, side, VOD.getLevel(price), orderManager, quantities);
    }

    private Order submitLimit(Agent sender, OrderSide side, Level price, final OrderManager orderManager, int... quantities)
    {
        Order last = null;
        for (final int quantity : quantities)
        {
            last = orderManager.add(new Order(nextId(), side, VOD, price, quantity, sender, timeMachine))[0];
        }
        orderManager.process();
        return last;
    }

    @SuppressWarnings("SameParameterValue")
    /** Makes a book where side1 is used to build a book with orders at a range of fixed interval prices equal to :
     *      startLimit +- (nLevels * tickSize).  The arguments are defined to
     * @param side1 the side which will have positive adjustment applied to the price (usually the bid side for a book
     *              with no crossable quantity)
     * @param side2 the side which will have a negative adjustment applied to the price (usually the offer side for a book
     *              with no crossable quantity)*/
    private void makeBook(int nLevels, OrderSide side1, OrderSide side2, final Level startLimit, int quantity)
    {
        for (int i = 0; i < nLevels; ++i)
        {
            submitLimit(side1 == OrderSide.BUY ? buyer : seller, side1, VOD.worsenOnBook(startLimit, side1, i+1).getPrice().doubleValue(), manager, quantity);
            submitLimit(side2 == OrderSide.BUY ? buyer : seller, side2, VOD.worsenOnBook(startLimit, side2, i+1).getPrice().doubleValue(), manager, quantity);
        }
        manager.process();
    }

    @Test
    public void testPendingOrders()
    {
        Assert.assertEquals(0, manager.sizePending());
        manager.add(new Order(nextId(), OrderSide.BUY, VOD, 100.0, 1000, buyer, timeMachine));
        manager.add(new Order(nextId(), OrderSide.BUY, VOD, 100.0, 1000, buyer, timeMachine));
        Assert.assertEquals(2, manager.sizePending());
    }

    @Test
    public void testCancelInProcess()
    {
        Order b = new Order(nextId(), OrderSide.BUY, VOD, 100.0, 500, nullAgent, timeMachine);
        manager.add(b);
        manager.process();
        Order s = new Order(nextId(), OrderSide.SELL, VOD, Level.MARKET, 1000, nullAgent, timeMachine);
        manager.add(s);
        manager.process();
        assertEquals(1, nullAgent.getOutstandingOrderCount());
        Assert.assertEquals(1, tape.size());
        s.cancel(timeMachine.toSimulationTime());
        manager.add(s);
        manager.process();
        assertEquals(0, nullAgent.getOutstandingOrderCount());
    }

    @Test
    public void testClear()
    {
        try (final AgentImplTest buyAgent = new AgentImplTest(VOD, NotSoRandomSource.makeZeroRandom(), 3L);
            final AgentImplTest sellAgent = new AgentImplTest(VOD, NotSoRandomSource.makeZeroRandom(), 4L)) {
            buyAgent.setBalances(1000.0, 100);
            sellAgent.setBalances(1000.0, 100);

            Order sell1 = new Order(nextId(), OrderSide.SELL, VOD, 100, 1000, sellAgent, timeMachine);
            manager.add(sell1);
            manager.process();
            assertEquals(100.0, manager.getLevel1().getOffer().getLevel().getPrice().doubleValue(), 1E-6);
            assertEquals(1000, (int) sell1.getRemainingQuantity());
            Order buy1 = new Order(nextId(), OrderSide.BUY, VOD, 100, 500, buyAgent, timeMachine);
            manager.add(buy1);
            manager.process();
            Assert.assertEquals(100.0, manager.getLevel1().getOffer().getLevel().getPrice().doubleValue(), 1E-16);

            Order agentSellOrder = sellAgent.getOrder(sell1.getClientId());
            Assert.assertEquals(500, (int) agentSellOrder.getRemainingQuantity());
            Assert.assertEquals(OrderState.PARTIALLY_FILLED, agentSellOrder.getState());

            Order agentBuyOrder = buyAgent.getOrder(buy1.getClientId());
            Assert.assertNull(agentBuyOrder);

            Assert.assertEquals(-49000.0, buyAgent.getCash(), 1E-6);
            Assert.assertEquals(600, buyAgent.getShares());

            Assert.assertEquals(51000.0, sellAgent.getCash(), 1E-6);
            Assert.assertEquals(-400, sellAgent.getShares());
        }
    }

    @Test
    public void testMarket()
    {
        // Price monitoring for this test starts an auction
        marketManager.setPriceMonitoring(false);

        // Submit a number of market orders both side
        submitMarket(buyer, OrderSide.BUY, manager, 100, 200, 300, 400);
        submitMarket(buyer, OrderSide.SELL, manager, 100, 200, 300, 400);

        // Nothing can clear so we should end up with 1000 shares (4 trades) on both sides with the market in choice
        checkTouch(BigDecimal.ZERO, 4, 1000, BigDecimal.ZERO, 4, 1000, MarketState.CHOICE);
        submitLimit(buyer, OrderSide.BUY, 150.0, manager, 500);
        // The 150 got executed so now we've got ...
        checkTouch(0.0, 4, 1000, 0.0, 2, 500, MarketState.CHOICE);
        submitLimit(buyer, OrderSide.SELL, 150.0, manager, 100);
        //TODO: The addition of the SELL 500@150.0, makes a market that allow the market orders to cross, is this correct?
        checkTouch(0.0, 3, 900, 0.0, 2, 500, MarketState.CHOICE);
    }

    @Test
    public void testBackwardated()
    {
        // This test would trigger an auction
        marketManager.setPriceMonitoring(false);
        // Make a book where everything crosses
        Level midPrice = VOD.getLevel(150.0);
        submitLimit(buyer, OrderSide.BUY, VOD.betterOnBook(midPrice, OrderSide.BUY, 1), manager, 100 );
        submitLimit(buyer, OrderSide.SELL, VOD.betterOnBook(midPrice, OrderSide.SELL, 1), manager, 100);
        assertEquals(1, tape.size());
        manager.process();
        checkTouch(0.0, 0, 0, 0.0, 0, 0, MarketState.CHOICE);
    }

    @Test
    public void testNormalMarketNoCrossing()
    {
        // Make a book where nothing crosses
        makeBook(10, OrderSide.BUY, OrderSide.SELL, VOD.getLevel(150.0), 100);
        assertEquals(0, tape.size());
        manager.process();
        OrderInterest buyVwap = manager.getQueue(OrderSide.BUY).getVwapOfLimitOrders();
        OrderInterest sellVwap = manager.getQueue(OrderSide.SELL).getVwapOfLimitOrders();
        /* The real VWAP is 149.945 / 150.055 but because of the shortcomings of {@link OrderInterest}
         * it can't actually be represented properly */
        OrderInterestTest.checkInterest(OrderSide.BUY, buyVwap, 149.95, 10, 1000);
        OrderInterestTest.checkInterest(OrderSide.SELL, sellVwap, 150.06, 10, 1000);
        checkTouch(149.99, 1, 100, 150.01, 1, 100, MarketState.CONTINUOUS);
    }

    @Test
    public void testInitialise() throws InterruptedException
    {
        manager.preProcess();
        Thread manThread = new Thread(manager);
        manThread.start();
        // Spin until we are running
        //noinspection StatementWithEmptyBody
        while (manager.isTerminate());
        try
        {
            manager.preProcess();
            Assert.fail("Initialise should fail");
        }
        catch (IllegalStateException isex)
        {
            // Expected behaviour
        }
        manager.terminate();
        manThread.join();
        manager.preProcess();
    }

    /**
     * Market orders will always cross the spread if there is a price for them to cross at
     */
    @Test
    public void testMarketBuy()
    {
        setupTop(1.4, 1.6, 1000);
        submitMarket(buyer, OrderSide.BUY, manager, 500);
        checkNet(buyer, seller);
        checkChanges(buyer, -800.0, 500, seller, 800, -500);
    }

    @Test
    public void testMarketSell()
    {
        setupTop(1.4, 1.6, 1000);
        submitMarket(seller, OrderSide.SELL, manager, 500);
        checkNet(buyer, seller);
        checkChanges(buyer, -700.0, 500, seller, 700.0, -500);
    }

    @Test
    public void testUncrossedMarket()
    {
        // Two buy orders at the top of the book
        submitMarket(buyer, OrderSide.BUY, manager, 250);
        submitLimit(buyer, OrderSide.BUY, 1.4, manager, 250);
        checkTouch(1.4, 2, 500, 0.0, 0, 0, MarketState.CHOICE);

        // Now submit a market order we should execute 250 shares of order 1 and 50 of order 2
        submitMarket(seller, OrderSide.SELL, manager, 300);
        checkNet(buyer, seller);
    }

    /***
     * Limit orders don't result in any orders unless they cross the spread
     */
    @Test
    public void testLimitBuy()
    {
        setupTop(1.4, 1.6, 1000);
        submitLimit(buyer, OrderSide.BUY, 1.7, manager, 500);
        checkNet(buyer, seller);
        checkChanges(buyer, -800.0, 500, seller, 800.0, -500);
    }

    @Test
    public void testLimitSell()
    {
        setupTop(1.4, 1.6, 1000);
        Assert.assertEquals(0, tape.size());
        submitLimit(seller, OrderSide.SELL, 1.3, manager, 500);
        checkNet(buyer, seller);
        checkChanges(buyer, -700.0, 500, seller, 700.0, -500);
    }

    @Test
    public void testChoseLevel()
    {
        final Level l100 = LevelTest.makeLevel(100, VOD);
        final Level l110 = LevelTest.makeLevel(110, VOD);
        final Level market = VOD.getMarket();
        Assert.assertEquals(l100, manager.chooseLevelForPrice(OrderSide.BUY, l110, l100));
        Assert.assertEquals(l110, manager.chooseLevelForPrice(OrderSide.SELL, l100, l110));
        Assert.assertEquals(l100, manager.chooseLevelForPrice(OrderSide.SELL, l100, l100));
        Assert.assertEquals(l100, manager.chooseLevelForPrice(OrderSide.BUY, l100, l100));
        Assert.assertEquals(l100, manager.chooseLevelForPrice(OrderSide.BUY, l100, market));
        Assert.assertEquals(l100, manager.chooseLevelForPrice(OrderSide.SELL, market, l100));
    }

    @Test
    public void testTooLateToCancel()
    {
        Pair<Order, Order> bidOffer = setupTop(1.4, 1.6, 1000);
        Order bid = (Order)bidOffer.getFirst().copy();
        Assert.assertTrue(bid.cancel(timeMachine.toSimulationTime()));
        // Now add a trade to match the top of the book, immediately followed by a trade to cancel
        // the filled order
        manager.add(new Order(nextId(), OrderSide.SELL, VOD, 1.4, 1000, seller, timeMachine));
        manager.add(bid);
        manager.process();
    }

    @Test
    public void testStaleCancel()
    {
        Pair<Order, Order> bidOffer = setupTop(1.4, 1.6, 1000);
        Order bid = (Order)bidOffer.getFirst().copy();
        Assert.assertTrue(bid.cancel(timeMachine.toSimulationTime()));
        // Now add a trade to match the top of the book, immediately followed by a trade to cancel
        // the filled order
        manager.add(new Order(nextId(), OrderSide.SELL, VOD, 1.4, 500, seller, timeMachine));
        manager.add(bid);
        manager.process();
    }

    @Test
    public void testStaleNew()
    {
        Pair<Order, Order> bidOffer = setupTop(1.4, 1.6, 1000);
        Order bid = (Order)bidOffer.getFirst().copy();
        // Now add a trade to match the top of the book, immediately followed by a trade to cancel
        // the filled order
        manager.add(new Order(nextId(), OrderSide.SELL, VOD, 1.4, 500, seller, timeMachine));
        manager.add(bid);
        manager.process();
    }

    @Test
    public void testRejectOnClosed()
    {
        final Instrument LLOY = new Instrument("LLOY.L", new Currency("GBP"), new ConstantTickSizeToLevelConverter(1, 100, 3));
        try (final OrderManager closed = new OrderManager(marketManager, timeMachine, null, MarketState.CLOSED, LLOY, new SyncEventDispatcher(timeMachine), tape, null, Duration.ZERO)) {
            Assert.assertEquals(0, closed.getQueue(OrderSide.BUY).size());
            final Order rejected = submitLimit(buyer, OrderSide.BUY, 1.4, closed, 1000);
            Assert.assertNull(buyer.getOrder(rejected.getClientId()));
            Assert.assertEquals(0, closed.getQueue(OrderSide.BUY).size());
        }
    }

    private Long getPrintedVolume()
    {
        return tape.getStatistics().stream().mapToLong(t -> ((Trade)t).getQuantity().longValue()).sum();
    }

    private void checkInterest(final OrderManager orderManager, final Level level, final long bidInterest, final long offerInterest)
    {
        assertEquals("Bid interest", bidInterest, orderManager.getQueue(OrderSide.BUY).getInterest(level).getQuantity());
        assertEquals("Offer interest", offerInterest, orderManager.getQueue(OrderSide.SELL).getInterest(level).getQuantity());
    }

    private void checkTape(final int... tradeQuantities)
    {
        assertEquals("There were more or fewer prints than expected", tradeQuantities.length, tape.getStatistics().size());
        int i = 0;
        for (final Statistic statistic : tape.getStatistics())
        {
            Trade t = Trade.class.cast(statistic);
            assertEquals("Execution# " + i, tradeQuantities[i], t.getQuantity().intValue());
            i++;
        }
    }

    @Test
    public void testAuctionUncross() throws AuctionException
    {
        final Instrument LLOY  = new Instrument("LLOY.L", new Currency("GBP"), new ConstantTickSizeToLevelConverter(1, 100, 3));
        final Level referenceLevel = LLOY.getLevel(100.0);
        try (final OrderManager auction = new OrderManager(marketManager, timeMachine, referenceLevel, MarketState.AUCTION, LLOY, new SyncEventDispatcher(timeMachine), tape, null, Duration.ZERO);
            final AuctionStateTestHelper testHelper = new AuctionStateTestHelper(timeMachine, LLOY, referenceLevel, auction)) {
            AuctionState state = testHelper.makeState1();
            assertEquals(0, tape.size());
            auction.uncross();
            // The executions should occur in priority order so the prints are predictable
            checkTape(2500, 6900, 600, 400);
            assertEquals(state.getUncrossingInterest().getSecond(), getPrintedVolume());
            // At the end of the auction we should be left with the following interest at the auction price
            checkInterest(auction, LLOY.getLevel(104.5), 5200, 0);
        }
    }

    @Test
    public void testAuctionOrders() throws Exception
    {
        final Instrument LLOY = new Instrument("LLOY.L", new Currency("GBP"), new ConstantTickSizeToLevelConverter(1, 100, 3));
        final Level referenceLevel = LLOY.getLevel(100.0);
        try (final OrderManager auction = new OrderManager(marketManager, timeMachine, referenceLevel, MarketState.AUCTION, LLOY, dispatcher, tape, null, Duration.ZERO);
             final AuctionStateTestHelper testHelper = new AuctionStateTestHelper(timeMachine, LLOY, referenceLevel, auction)) {
            AuctionState state = testHelper.makeState1();
            final Level uncrossingLevel = state.getUncrossingInterest().getFirst();
            assertEquals(10450, uncrossingLevel.getLevel());
            final PriorityOrders buyPriorityOrders = auction.getQueue(OrderSide.BUY).getAuctionPriorityOrders(uncrossingLevel);
            assertEquals(2, buyPriorityOrders.size());
            assertEquals(10450, buyPriorityOrders.getLevel().getLevel());
            assertEquals(15600L, buyPriorityOrders.getTotalVolume());
            final PriorityOrders sellPriorityOrders = auction.getQueue(OrderSide.SELL).getAuctionPriorityOrders(uncrossingLevel);
            assertEquals(3, sellPriorityOrders.size());
            assertEquals(10450, sellPriorityOrders.getLevel().getLevel());
            assertEquals( state.getUncrossingInterest().getSecond().longValue(), sellPriorityOrders.getTotalVolume());
        }
    }

    @Test
    public void testAuctionUncrossWithPendingOrderEvents()
    {
        final Instrument LLOY  = new Instrument("LLOY.L", new Currency("GBP"), new ConstantTickSizeToLevelConverter(1, 100, 3));
        final Level referenceLevel = LLOY.getLevel(100.0);
        try (final OrderManager auction = new OrderManager(marketManager, timeMachine, referenceLevel, MarketState.CONTINUOUS, LLOY, new SyncEventDispatcher(timeMachine), null, null, Duration.ZERO)) {
            final Order inFlight = submitMarket(nullAgent, OrderSide.BUY, auction, 500);
            auction.notify(new AuctionTriggerEvent(this, timeMachine.toSimulationTime(), EnumSet.of(MarketState.CONTINUOUS), MarketState.AUCTION, referenceLevel, Duration.ZERO));
            inFlight.setQuantity(1000);
            assertNotEquals(MarketState.AUCTION, auction.getLevel1().getTouchState());
            // An order event now sits behind the auction trigger event
            auction.add(inFlight);
            auction.process();
        }
    }

    /** When an auction occurs as a result of price monitoring it should happen immediately, there should be
     * no possibility that another trade can cross before the auction occurs
     * @throws Exception
     */
    @Test
    public void testPriceMonitoringAuctionImmediateCancel() throws Exception
    {
        final Instrument LLOY  = new Instrument("LLOY.L", new Currency("GBP"), new ConstantTickSizeToLevelConverter(1, 100, 3));
        final Level referenceLevel = LLOY.getLevel(100.0);
        try (final OrderManager auction = new OrderManager(marketManager, timeMachine, referenceLevel, MarketState.CONTINUOUS, LLOY, dispatcher, tape, null, Duration.ZERO)) {
            submitMarket(nullAgent, OrderSide.BUY, auction, 500);
            submitLimit(nullAgent, OrderSide.SELL, LLOY.getLevel(200.0), auction, 200, 200);
            // The second order should be cancelled by the order manager
            checkTape(200);
            assertEquals(MarketState.AUCTION, auction.getLevel1().getTouchState());
        }
    }
}
