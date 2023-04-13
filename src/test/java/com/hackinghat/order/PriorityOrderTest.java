package com.hackinghat.order;


import com.hackinghat.agent.NullAgent;
import com.hackinghat.instrument.ConstantTickSizeToLevelConverter;
import com.hackinghat.model.Currency;
import com.hackinghat.model.Instrument;
import com.hackinghat.orderbook.OrderTest;
import com.hackinghat.util.NotSoRandomSource;
import com.hackinghat.util.SyncEventDispatcher;
import com.hackinghat.util.TimeMachine;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PriorityOrderTest
{
    private Instrument  VOD;
    private TimeMachine timeMachine;
    private NullAgent   nullAgent;

    @Before
    public void setUp()
    {
        VOD = new Instrument("VOD.L", new Currency("GBP"), new ConstantTickSizeToLevelConverter(1, 100, 3));
        timeMachine = new TimeMachine();
        nullAgent = new NullAgent(1L, VOD, new NotSoRandomSource(), timeMachine, "AGENT-0", new SyncEventDispatcher(timeMachine));
    }

    @After
    public void teardown() { nullAgent.shutdown(); }

    private void checkQuantitiesInPriorityOrder(final PriorityOrders orders, int... quantities)
    {
        assertEquals("Number of priority orders mismatch", quantities.length, orders.size());
        for (int i = 0; i < quantities.length; ++i)
        {
            Order o = orders.get(i);
            assertEquals("Order #" + i + ", has a quantity mismatch", quantities[i], o.getQuantity().intValue());
        }
    }

    @Test
    public void testNew()
    {
        final OrderLimitQueue marketLimit = new OrderLimitQueue(VOD.getMarket(), OrderSide.BUY);
        final OrderLimitQueue limitLimit  = new OrderLimitQueue(LevelTest.makeLevel(100.0, VOD), OrderSide.BUY);
        final PriorityOrders priorityOrders = new PriorityOrders(VOD, Arrays.asList(marketLimit, limitLimit));
        assertTrue(priorityOrders.getLevel().isMarket());
    }

    @Test
    public void testPriorityOrders()
    {
        // After an auction the orders need to be in price -> time priority order
        final OrderLimitQueue marketLimit = new OrderLimitQueue(VOD.getMarket(), OrderSide.BUY);
        marketLimit.add(OrderTest.marketOrder(1L, OrderSide.BUY, VOD, 400, nullAgent, timeMachine, true));
        final OrderLimitQueue L100  = new OrderLimitQueue(LevelTest.makeLevel(100.0, VOD), OrderSide.BUY);
        L100.add(OrderTest.limitOrder(1L, OrderSide.BUY, VOD, 100.0, 100, nullAgent, timeMachine, true));
        final OrderLimitQueue L101 = new OrderLimitQueue(LevelTest.makeLevel(101.0, VOD), OrderSide.BUY);
        L101.add(OrderTest.limitOrder(1L, OrderSide.BUY, VOD, 101.0, 200, nullAgent, timeMachine, true));
        PriorityOrders priorityOrders = new PriorityOrders(L101.getLevel(), Arrays.asList(marketLimit, L100, L101));
        checkQuantitiesInPriorityOrder(priorityOrders, 400, 200, 100);
    }
}
