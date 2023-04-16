package com.hackinghat.agent;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import com.hackinghat.instrument.ConstantTickSizeToLevelConverter;
import com.hackinghat.model.Currency;
import com.hackinghat.model.Instrument;
import com.hackinghat.order.Order;
import com.hackinghat.order.OrderSide;
import com.hackinghat.order.OrderState;
import com.hackinghat.util.RandomSource;
import com.hackinghat.util.RandomSourceImpl;
import com.hackinghat.util.TimeMachine;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalTime;

public class AgentTest
{

    private RandomSource randomSource;
    private TimeMachine  timeMachine;
    private Instrument   instrument;
    private void checkBalances(double cash, int shares, Agent a)
    {
        assertEquals(cash, a.getCash(), 0.01);
        assertEquals(shares, a.getShares());

    }

    @Before
    public void setup()
    {
        timeMachine = new TimeMachine(LocalTime.of(8, 0, 0), 1.0d);
        timeMachine.start();
        randomSource = new RandomSourceImpl(0L);
        instrument = new Instrument("VOD", new Currency("GBP"), new ConstantTickSizeToLevelConverter(1, 100, 3));
    }

    @After
    public void tearDown() {

    }


    @Test
    public void testCancel()
    {
        try (final AgentImplTest ai = new AgentImplTest(instrument, randomSource, 1L)) {
            Order buy = new Order("C1", OrderSide.BUY, instrument, 1.0, 1000, ai, timeMachine);
            Order managerBuyOrder = (Order)buy.copy();
            managerBuyOrder.setId(1L);
            buy.cancel(timeMachine.toSimulationTime());
            Assert.assertEquals(OrderState.PENDING_CANCEL, buy.getState());
            managerBuyOrder.resetState(timeMachine.toSimulationTime());
            assertEquals(OrderState.PENDING_CANCEL, buy.getState());
        }
    }

    @Test
    public void testBalances()
    {
        try (final AgentImplTest ai = new AgentImplTest(instrument, randomSource, 1L)) {
            ai.setBalances(10000.0, 1000);
            Order buy = new Order("B1", OrderSide.BUY, instrument, 1.0, 1000, ai, timeMachine);
            Order sell = new Order("B1", OrderSide.SELL, instrument, 1.01, 1000, ai, timeMachine);
            checkBalances(10000.0, 1000, ai);
            ai.fill(buy, 500, instrument.getLevel(1.0));
            checkBalances(9500.0, 1500, ai);
            ai.fill(sell, 500, instrument.getLevel(1.01));
            checkBalances(10005.0, 1000, ai);
            ai.fill(buy, 500, instrument.getLevel(1.0));
            checkBalances(9505.0, 1500, ai);
            ai.fill(sell, 500, instrument.getLevel(1.01));
            checkBalances(10010.0, 1000, ai);
        }
    }
}
