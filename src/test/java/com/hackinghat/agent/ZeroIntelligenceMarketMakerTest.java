package com.hackinghat.agent;

import com.hackinghat.order.Order;
import com.hackinghat.order.OrderSide;
import com.hackinghat.orderbook.Level1;
import com.hackinghat.simulator.OrderBookSimulatorHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

import static com.hackinghat.simulator.OrderBookSimulatorHelper.ONE_SECOND;
import com.hackinghat.agent.ZeroIntelligenceMarketMaker.OrderPosition;
import static org.junit.Assert.*;

public class ZeroIntelligenceMarketMakerTest
{
    private OrderBookSimulatorHelper simulatorHelper;
    private ZeroIntelligenceMarketMaker zeroMM;
    private Collection<Order> preheatedOrders;
    private int spread;
    private int spreadTolerance;

    @Before
    public void setup()
    {
        simulatorHelper = new OrderBookSimulatorHelper();
    }

    @After
    public void teardown()
    {
        simulatorHelper.shutdown();
        if (zeroMM != null)
            zeroMM.shutdown();
    }

    private void continuousTrading() throws Exception
    {
        simulatorHelper.setDoubleSource(new double[]{0.0});
        simulatorHelper.transitionClosedToAuction();
        preheatedOrders = new ArrayList<>(simulatorHelper.submitOrder(10, 1, 1, true));
        simulatorHelper.transitionAuctionToContinuous();
        preheatedOrders.addAll(simulatorHelper.submitOrder(10, 1, 1, false));

        spread = 100;
        spreadTolerance = 10;
        zeroMM = new ZeroIntelligenceMarketMaker(1L, simulatorHelper.getInst(), simulatorHelper.getRandomSource(), simulatorHelper.getTimeMachine(), ONE_SECOND, "M1", simulatorHelper.getSimulator(), spread, spreadTolerance, 1000, 1000, true);
        zeroMM.doActions();
    }

    @Test
    public void testInAuction() throws Exception
    {
        simulatorHelper.setDoubleSource(new double[]{0.0});
        simulatorHelper.transitionClosedToAuction();
        simulatorHelper.submitOrder(10, 1, 1, true);
        simulatorHelper.transitionAuctionToContinuous();
        try (final ZeroIntelligenceMarketMaker mi = new ZeroIntelligenceMarketMaker(1L, simulatorHelper.getInst(), simulatorHelper.getRandomSource(), simulatorHelper.getTimeMachine(), ONE_SECOND, "M1", simulatorHelper.getSimulator(), spread, spreadTolerance, 1000, 1000, false)) {
            mi.doActions();
            assertNull(mi.getBid());
            assertNull(mi.getOffer());
        }
    }

    @Test
    public void testContinuous() throws Exception
    {
        continuousTrading();
        assertNotNull(zeroMM.getBid());
        assertNotNull(zeroMM.getOffer());
        assertEquals(spread, zeroMM.getOffer().getLevel().absoluteticksBetween(zeroMM.getBid().getLevel()));
    }

    private static void checkOrderPosition(final ZeroIntelligenceMarketMaker marketMaker, final Level1 level1, final OrderPosition expectedBidPosition, final OrderPosition expectedOfferPosition)
    {
        assertEquals(expectedBidPosition, marketMaker.calculateOrderPosition(level1, marketMaker.getBid()));
        assertEquals(expectedOfferPosition, marketMaker.calculateOrderPosition(level1, marketMaker.getOffer()));
    }

    @Test
    public void testMidChange() throws Exception
    {
        continuousTrading();
        final Order bid = zeroMM.getBid();
        assertNotNull(bid);

        simulatorHelper.cancelOrders(preheatedOrders);
        final Level1 level1 = simulatorHelper.getSimulator().getLevel1();
        // There should be now exactly one bid and offer order market maker quote on the book
        assertEquals(1, level1.getInterest(OrderSide.BUY).getCount());
        assertEquals(1, level1.getInterest(OrderSide.SELL).getCount());
        assertEquals(zeroMM.getBid().getLevel().getLevel(), level1.getBid().getLevel().getLevel());
        assertEquals(zeroMM.getOffer().getLevel().getLevel(), level1.getOffer().getLevel().getLevel());
        checkOrderPosition(zeroMM, level1, OrderPosition.SoleTop, OrderPosition.SoleTop);
        // Now put an offer order in at the previous mid (1.5) this will change the spread to 1.0-1.5 and the mid to 1.25
        simulatorHelper.submitOrder(0, 0, 1000, false);
        final Level1 midChange = simulatorHelper.getSimulator().getLevel1();
        assertEquals(1.25, midChange.getMid(simulatorHelper.getInst()).getPrice().doubleValue(), 1E-9);
        // Now the market maker should have a different offer to the best offer
        assertEquals(zeroMM.getBid().getLevel().getLevel(), midChange.getBid().getLevel().getLevel());
        assertNotEquals(zeroMM.getOffer().getLevel().getLevel(), midChange.getOffer().getLevel().getLevel());
        // The market maker is now at the top of the bid book and outside of allowed tolerance on the offer
        checkOrderPosition(zeroMM, midChange, OrderPosition.SoleTop, OrderPosition.OutsideTolerance);
        zeroMM.doActions();
        simulatorHelper.getSimulator().process();
        // This should lead us to cancel the bid (because 'cancelIfTop' is declared in the setup) and adjust the offer
        // and the spread shouldn't change
        checkOrderPosition(zeroMM, midChange, OrderPosition.NoOrder, OrderPosition.AtSpread);
    }

}

