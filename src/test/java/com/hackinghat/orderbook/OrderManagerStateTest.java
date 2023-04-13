package com.hackinghat.orderbook;

import com.hackinghat.order.MarketState;
import org.junit.Test;

public class OrderManagerStateTest
{
    @Test
    public void testAccept()
    {
        OrderManagerState orderManagerState = new OrderManagerState(MarketState.CLOSED);
        orderManagerState.accept(MarketState.AUCTION);
        orderManagerState.accept(MarketState.CONTINUOUS);
        orderManagerState.accept(MarketState.AUCTION);
        orderManagerState.accept(MarketState.CLOSED);
    }

    @Test(expected = IllegalStateException.class)
    public void testNotAccepted()
    {
        OrderManagerState orderManagerState = new OrderManagerState(MarketState.CLOSED);
        orderManagerState.accept(MarketState.CONTINUOUS);
    }

    @Test
    public void testNoTransition()
    {
        OrderManagerState orderManagerState = new OrderManagerState(MarketState.CONTINUOUS);
        orderManagerState.accept(MarketState.CONTINUOUS);

    }
}
