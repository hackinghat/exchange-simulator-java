package com.hackinghat.orderbook;


import com.hackinghat.agent.NullAgent;
import com.hackinghat.instrument.ConstantTickSizeToLevelConverter;
import com.hackinghat.model.Currency;
import com.hackinghat.model.Instrument;
import com.hackinghat.model.Level;
import com.hackinghat.order.LevelTest;
import com.hackinghat.order.Order;
import com.hackinghat.order.OrderSide;
import com.hackinghat.util.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.*;

import static com.hackinghat.order.OrderSide.BUY;
import static com.hackinghat.order.OrderSide.SELL;
import static com.hackinghat.orderbook.OrderTest.limitOrder;
import static com.hackinghat.orderbook.OrderTest.marketOrder;

public class OrderBookTest
{
    private Instrument VOD;
    private OrderBook bidQueue;
    private OrderBook offerQueue;
    private TimeMachine timeMachine;
    private BigDecimal limitPrice;
    private Level levelForLimitPrice;
    private EventDispatcher eventDispatcher;
    private NullAgent nullAgent;
    private long cId;

    private String nextId() { return "C" + ++cId; }

    @Before
    public void setUp() throws Exception {
        VOD = new Instrument( "VOD.L", new Currency("GBP"), new ConstantTickSizeToLevelConverter(1, 100, 3));
        bidQueue = new OrderBook(BUY, VOD);
        offerQueue = new OrderBook(SELL, VOD);
        limitPrice = VOD.roundToTick(150.0);
        timeMachine = new TimeMachine();
        levelForLimitPrice = VOD.getLevel(limitPrice);
        eventDispatcher = new SyncEventDispatcher(timeMachine);
        nullAgent = new NullAgent(0L, VOD, new NotSoRandomSource(), timeMachine, "AGENT-0", eventDispatcher);
        cId = 0;
    }

    @After
    public void teardown() {
        bidQueue.shutdown();
        offerQueue.shutdown();
        nullAgent.shutdown();
    }

    private OrderBook queueForSide(OrderSide side)
    {
        return side == BUY ? bidQueue : offerQueue;
    }

    public void testAddAndCancelLimit()
    {
        for (OrderSide side: EnumSet.allOf(OrderSide.class))
        {
            OrderBook queue = queueForSide(side);
            Order order1 = new Order(nextId(), side, VOD, limitPrice, 1000, nullAgent, timeMachine);
            order1.setId(1L);
            order1.resetState(timeMachine.toSimulationTime());
            Assert.assertTrue(queue.newOrder(order1));
            OrderInterestTest.checkInterest(side, queue.getInterest(levelForLimitPrice), limitPrice, 1, 1000);
            // Can't add the same order twice
            Assert.assertFalse(queue.newOrder(order1));
            OrderInterestTest.checkInterest(side, queue.getInterest(levelForLimitPrice), limitPrice, 1, 1000);
            Assert.assertTrue(queue.cancelOrder(order1));
            OrderInterestTest.checkInterest(side, queue.getInterest(levelForLimitPrice), limitPrice, 0, 0);
        }
    }

    @Test
    public void testBest()
    {
        int nLevelsToTest = 10;
        final Level bestPossible = VOD.getLevel(limitPrice);
        for (int i = 0; i < nLevelsToTest; ++i)
        {
            int index = nLevelsToTest - i;
            for (OrderSide side: EnumSet.allOf(OrderSide.class))
            {
                OrderBook queue = queueForSide(side);
                BigDecimal orderPrice = VOD.worsenOnBook(bestPossible, side, index).getPrice();
                Order order1 = new Order(nextId(), side, VOD, orderPrice, 1000, nullAgent, timeMachine);
                order1.setId((long) i);
                order1.resetState(timeMachine.toSimulationTime());

                Assert.assertTrue(queue.newOrder(order1));
                Assert.assertEquals(0, orderPrice.compareTo(queue.getBestLimitQueue().getLevel().getPrice()));
            }
        }
    }

    /**
     * Test whether given the market and limit order that two relative levels (will buy & won't buy) will
     * @param queue the queue to test
     * @param marketOrder a sample market order to add to the queue
     * @param limitOrder a limit order to add to the queue
     * @param wontTradeTick the number of ticks away from the best for buyer/seller
     * @param willTradeTick the number of ticks away (negative is toward) the best for buyer/seller
     */
    private void testLimit(OrderBook queue, Order marketOrder, Order limitOrder, int wontTradeTick, int willTradeTick)
    {
        Assert.assertTrue(marketOrder.isMarket());
        Assert.assertFalse(limitOrder.isMarket());

        queue.newOrder(marketOrder);
        queue.newOrder(limitOrder);
        Assert.assertTrue(queue.otherLevelAllowsExecution(levelForLimitPrice));
        // Won't trade a buy above (or a sell below) the limit - note better on book is worse for originator
        final Level wontBuyLevel = VOD.betterOnBook(levelForLimitPrice, queue.getQueueSide(), wontTradeTick);
        Assert.assertFalse(queue.otherLevelAllowsExecution(wontBuyLevel));
        // Will trade a buy below (or a sell above) the limit - note worse on book is better for the originator
        final Level willBuyLevel = VOD.worsenOnBook(levelForLimitPrice, queue.getQueueSide(), willTradeTick);
        Assert.assertTrue(queue.otherLevelAllowsExecution(willBuyLevel));
        // Market order must execute against a limit
        Assert.assertTrue(queue.otherLevelAllowsExecution(VOD.getMarket()));
        // Now cancel the limit order leaving the market order, limits will cross (but not markets)
        queue.cancelOrder(limitOrder);
        Assert.assertTrue(queue.otherLevelAllowsExecution(willBuyLevel));
        Assert.assertTrue(queue.otherLevelAllowsExecution(wontBuyLevel));
        Assert.assertFalse(queue.otherLevelAllowsExecution(VOD.getMarket()));
    }

    private void testGetBestQueue(final OrderBook queue, final Order marketOrder, final Order limitOrder)
    {
        Assert.assertTrue(marketOrder.isMarket());
        Assert.assertFalse(limitOrder.isMarket());
        queue.newOrder(marketOrder);
        queue.newOrder(limitOrder);
        Assert.assertEquals(queue.getBestLimitQueue().getLevel(), limitOrder.getLevel());
    }

    @Test
    public void testWrongSide() throws IllegalArgumentException
    {
        try
        {
            bidQueue.newOrder(marketOrder(1L, OrderSide.SELL, VOD, 500, nullAgent, timeMachine, true));
            Assert.fail();
        }
        catch(final IllegalArgumentException illex) {}
    }

    @Test
    public void testCanExecuteLimitBuy()
    {
        testLimit(bidQueue, marketOrder(1L, OrderSide.BUY, VOD, 500, nullAgent, timeMachine, true),
                            limitOrder(2L, OrderSide.BUY, VOD, limitPrice, 500, nullAgent, timeMachine, true),1, 1);
    }

    @Test
    public void testCanExecuteLimitSell()
    {
        testLimit(offerQueue, marketOrder(1L, OrderSide.SELL, VOD, 500, nullAgent, timeMachine, true),
                              limitOrder(2L, OrderSide.SELL, VOD, limitPrice, 500, nullAgent, timeMachine, true),1, 1);
    }

    @Test
    public void testGetBestQueueOffer()
    {
        testGetBestQueue(offerQueue, marketOrder(1L, OrderSide.SELL, VOD, 500, nullAgent, timeMachine, true),
                                     limitOrder(2L, OrderSide.SELL, VOD, limitPrice, 500, nullAgent, timeMachine, true));
    }

    private void checkOrderInterests(final OrderBook book, final Level... interests )
    {
        final Iterator<Level> levelIterator = Arrays.asList(interests).iterator();
        final Iterator<OrderInterest> interestIterator = book.getExecutableLevels().iterator();
        while (levelIterator.hasNext() && interestIterator.hasNext())
        {
            final Level expected = levelIterator.next();
            final Level observed = interestIterator.next().getLevel();
            if (expected.isMarket())
                Assert.assertTrue(observed.isMarket());
            else
                Assert.assertEquals(expected.getLevel(), observed.getLevel());
        }
        Assert.assertFalse(levelIterator.hasNext() || interestIterator.hasNext());
    }

    @Test
    public void testGetExecutable()
    {
        // Limit price2 is 'better' than limit price
        final Level level1 = VOD.getLevel(limitPrice);
        final Level level2 = VOD.betterOnBook(level1, OrderSide.BUY, 1);

        final Order marketOrder = marketOrder(1L, OrderSide.BUY, VOD, 500, nullAgent, timeMachine, true);
        final Order limitOrder1 = limitOrder(2L, OrderSide.BUY, VOD, limitPrice, 500, nullAgent, timeMachine, true);
        final Order limitOrder2 = limitOrder(3L, OrderSide.BUY, VOD, level2.getPrice(), 500, nullAgent, timeMachine, true);

        // Insert the orders in any order they should come back in marketable order, empty levels should
        // be collapsed
        bidQueue.newOrder(limitOrder2);
        checkOrderInterests(bidQueue, level2);
        bidQueue.newOrder(marketOrder);
        checkOrderInterests(bidQueue, VOD.getMarket(), level2);
        bidQueue.newOrder(limitOrder1);
        checkOrderInterests(bidQueue, VOD.getMarket(), level2, level1);
        bidQueue.cancelOrder(marketOrder);
        checkOrderInterests(bidQueue, level2, level1);
        bidQueue.cancelOrder(limitOrder1);
        bidQueue.cancelOrder(limitOrder2);
        checkOrderInterests(bidQueue);
    }

    @Test
    public void testGetBestQueueBid()
    {
        testGetBestQueue(bidQueue, marketOrder(1L, OrderSide.BUY, VOD, 500, nullAgent, timeMachine, true),
                limitOrder(2L, OrderSide.BUY, VOD, limitPrice, 500, nullAgent, timeMachine, true));
    }

    @Test
    public void testVwap()
    {
        Level L100 = LevelTest.makeLevel(100.0, VOD);
        Level L116 = LevelTest.makeLevel(116.67, VOD);
        bidQueue.newOrder(marketOrder(1L, OrderSide.BUY, VOD, 500, nullAgent, timeMachine, true));
        Assert.assertTrue(bidQueue.getVwapOfLimitOrders().getLevel().isMarket());
        bidQueue.newOrder(limitOrder(2L, OrderSide.BUY, VOD, limitPrice, 500, nullAgent, timeMachine, true));
        Assert.assertEquals(0, limitPrice.compareTo(bidQueue.getVwapOfLimitOrders().getLevel().getPrice()));
        bidQueue.newOrder(limitOrder(3L, OrderSide.BUY, VOD, L100.getPrice(), 1000, nullAgent, timeMachine, true));
        Assert.assertEquals(0, L116.getPrice().compareTo(bidQueue.getVwapOfLimitOrders().getLevel().getPrice()));
    }

}
