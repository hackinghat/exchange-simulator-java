package com.hackinghat.agent;

import com.hackinghat.simulator.OrderBookSimulatorHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

import static com.hackinghat.simulator.OrderBookSimulatorHelper.ONE_SECOND;
public class ZeroIntelligenceAgentTest
{
    private OrderBookSimulatorHelper simulatorHelper;

    @Before
    public void setup() {
        simulatorHelper = new OrderBookSimulatorHelper();
    }

    @After
    public void teardown() { if (simulatorHelper != null) simulatorHelper.shutdown(); }

    @Test
    public void testSharesAffordability()
    {
        simulatorHelper.setDoubleSource(new double[] {
                1.0,  // LIMIT
                2.0,  // Quantity
                1.0,  // SELL
                0.0   // In-spread limit (<=0.5)
        });
        simulatorHelper.setIntSource(new int[] {
                2     // 2 levels towards bid
        });
        try (final ZeroIntelligenceAgent agent = new ZeroIntelligenceAgent(1L, simulatorHelper.getInst(), simulatorHelper.getRandomSource(), simulatorHelper.getTimeMachine(), ONE_SECOND, ONE_SECOND, "Z1", simulatorHelper.getSimulator(), 0.0, 0.0, 0.0, 0.5)) {
            agent.setBalances(1.0, 1);
            agent.setSizeMean(2.0);
            agent.doActions();
            // The order wasn't affordable, so we'd not expect any outstanding orders
            assertEquals(0, agent.getOutstandingOrders().size());
            simulatorHelper.resetRandomSource();
            agent.setBalances(10000.0, 1000);
            agent.doActions();
            // With the limits set higher the order is affordable
            assertEquals(1, agent.getOutstandingOrders().size());
        }
    }

    @Test
    public void testAuctionProcessing() throws Exception
    {
        simulatorHelper.getRandomSource().setDoubleSource(new double[]{
                0.5,        //LIMIT
                50.0});     //Quantity
        simulatorHelper.getRandomSource().setIntSource(new int[]{10});
        simulatorHelper.transitionClosedToAuction();
        try (final ZeroIntelligenceAgent ai = new ZeroIntelligenceAgent(1L, simulatorHelper.getInst(), simulatorHelper.getRandomSource(), simulatorHelper.getTimeMachine(), ONE_SECOND, ONE_SECOND, "Z1", simulatorHelper.getSimulator(), 0.0, 0.0, 0.0, 0.5)) {
            ai.setBalances(10000.0, 100);
            ai.setSizeMean(2.0);
            assertEquals(0, ai.getOutstandingOrders().size());
            // This will create an order to add to the auction
            ai.doActions();
            assertEquals(1, ai.getOutstandingOrders().size());
            // Add some additional volume that will uncross
            simulatorHelper.submitOrder(10, 1, 1, true);
            simulatorHelper.transitionAuctionToContinuous();
            assertEquals(1, ai.getOutstandingOrders().size());
            // Now create another 'random' order, but this time in 'continuous'
            ai.doActions();
            assertEquals(2, ai.getOutstandingOrders().size());
        }
    }
}
