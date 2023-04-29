package com.hackinghat.orderbook;


import com.hackinghat.agent.Agent;
import com.hackinghat.agent.NullAgent;
import com.hackinghat.model.ConstantTickSizeToLevelConverter;
import com.hackinghat.model.Currency;
import com.hackinghat.model.Instrument;
import com.hackinghat.model.Level;
import com.hackinghat.order.Order;
import com.hackinghat.order.OrderSide;
import com.hackinghat.order.OrderState;
import com.hackinghat.util.NotSoRandomSource;
import com.hackinghat.util.TimeMachine;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.*;

public class OrderTest {
    private TimeMachine timeMachine;
    private Instrument instrument;
    private NullAgent nullAgent;

    /**
     * Create a new limit order
     *
     * @param id          the id that we want to refer to this order by
     * @param side        the side
     * @param inst        the instrument
     * @param limit       the price
     * @param quantity    the quantity
     * @param nullAgent   the agent
     * @param timeMachine the time machine
     * @param accepted    whether this order should be treated as accepted by the order manager
     * @return a limit order ready for use in testing
     */
    public static Order limitOrder(Long id, OrderSide side, Instrument inst, float limit, int quantity, Agent nullAgent, TimeMachine timeMachine, boolean accepted) {
        Order c1 = new Order(side.toString() + id, side, inst, inst.roundToTick(limit), quantity, nullAgent, timeMachine);
        c1.setId(id);
        if (accepted)
            c1.resetState(timeMachine.toSimulationTime());
        return c1;
    }

    /**
     * Create a new limit order
     *
     * @param id          the id that we want to refer to this order by
     * @param side        the side
     * @param inst        the instrument
     * @param quantity    the quantity
     * @param nullAgent   the agent
     * @param timeMachine the time machine
     * @param accepted    whether this order should be treated as accepted by the order manager
     * @return a market order ready for use in testing
     */
    public static Order marketOrder(Long id, OrderSide side, Instrument inst, int quantity, Agent nullAgent, TimeMachine timeMachine, boolean accepted) {
        Order c1 = new Order(side.toString() + id, side, inst, Level.MARKET, quantity, nullAgent, timeMachine);
        c1.setId(id);
        if (accepted)
            c1.resetState(timeMachine.toSimulationTime());
        return c1;
    }

    @Before
    public void setUp() {
        timeMachine = new TimeMachine();
        instrument = new Instrument("VOD.L", new Currency("GBP"), new ConstantTickSizeToLevelConverter(1, 100, 3));
        nullAgent = new NullAgent(0L, instrument, new NotSoRandomSource(), timeMachine, "AGENT-0", null);
    }

    @After
    public void teardown() {
        nullAgent.shutdown();
    }

    private void checkStateAndVersion(Order order, OrderState state, int version) {
        assertEquals(state, order.getState());
        assertEquals(version, order.getVersion());
    }

    @Test
    public void testToString() {
        Order order = new Order("C1", OrderSide.BUY, instrument, Level.MARKET, 1000, nullAgent, timeMachine);
        order.toString();
    }

    @Test
    public void testEquals() {
        Order order1 = new Order("C1", OrderSide.BUY, instrument, Level.MARKET, 1000, nullAgent, timeMachine);
        Order order2 = new Order("C1", OrderSide.BUY, instrument, Level.MARKET, 1000, nullAgent, timeMachine);
        assertEquals(order1, order2);
        order2.setId(1L);
        assertEquals(order1, order2);
        order2.setId(1L);
        assertEquals(order1, order2);
        order2.setId(3L);
        assertNotSame(order1, order2);
    }

    @Test
    public void testCopy() {
        Order order1 = new Order("C1", OrderSide.BUY, instrument, Level.MARKET, 1000, nullAgent, timeMachine);
        assertEquals(order1, order1.copy());
    }

    @Test
    public void testSimpleResets() {
        Order order = new Order("C1", OrderSide.BUY, instrument, Level.MARKET, 1000, nullAgent, timeMachine);
        order.setId(1L);
        checkStateAndVersion(order, OrderState.PENDING_NEW, 1);
        order.resetState(timeMachine.toSimulationTime());
        checkStateAndVersion(order, OrderState.NEW, 2);
        order.fillQuantity(50, instrument.getLevel(100.5f), timeMachine.toSimulationTime());
        checkStateAndVersion(order, OrderState.PARTIALLY_FILLED, 3);
        order.fillQuantity(950, instrument.getLevel(100.1f), timeMachine.toSimulationTime());
        checkStateAndVersion(order, OrderState.FILLED, 4);
        assertFalse(order.cancel(timeMachine.toSimulationTime()));
    }

    @Test
    public void testTimePriority() {
        final Order order1 = new Order("O1", OrderSide.BUY, instrument, Level.MARKET, 1000, nullAgent, timeMachine);
        final Order order2 = new Order("O2", OrderSide.BUY, instrument, Level.MARKET, 1000, nullAgent, timeMachine);
        order2.setTimestamp(order1.getTimestamp().plus(1L, ChronoUnit.MILLIS));
        assertEquals(0, order1.compareTo(order1));
        assertEquals(-1, order1.compareTo(order2));
        assertEquals(1, order2.compareTo(order1));
    }

    @Test
    public void testLevelPriority() {
        final LocalDateTime now = LocalDateTime.now();
        Order order1 = new Order("C1", OrderSide.BUY, instrument, instrument.getLevel(100.0f), 1000, nullAgent, timeMachine);
        order1.setTimestamp(now);
        Order order2 = new Order("C2", OrderSide.BUY, instrument, Level.MARKET, 1000, nullAgent, timeMachine);
        order2.setTimestamp(now);
        Order order3 = new Order("C3", OrderSide.BUY, instrument, instrument.getLevel(110.0f), 1000, nullAgent, timeMachine);
        order3.setTimestamp(now);
        assertEquals(0, order1.compareTo(order1));
        // Order 1 is worse priority (its bid level is lower) than order 2 & 3 (i.e. they compare larger)
        assertEquals(1, order1.compareTo(order3));
        assertEquals(1, order1.compareTo(order2));
        // Order 2 is market and so has better priority than order 1 & 3 (i.e. they compare larger)
        assertEquals(-1, order2.compareTo(order1));
        assertEquals(-1, order2.compareTo(order3));
        // Order 3 is better priority than order 1 (its bid level is higher) but worse than order 2
        assertEquals(-1, order3.compareTo(order1));
        assertEquals(1, order3.compareTo(order2));
    }

    @Test
    public void testRounding() {
        final float px1 = 100.005f;
        Order order1 = new Order("O1", OrderSide.BUY, instrument, px1, 1000, nullAgent, timeMachine);
        // Order price must exactly divide tick size and this can't be true because px1 doesn't
        assertEquals(order1.getLevel(), instrument.getLevel(px1));
        final float px2 = 100.010001f;
        Order order2 = new Order("O2", OrderSide.BUY, instrument, px2, 1000, nullAgent, timeMachine);
        // Order price must exactly divide tick size
        assertEquals(order2.getLevel(), instrument.getLevel(px2));
    }

    @Test
    public void testCancel() {
        Order order = new Order("O1", OrderSide.BUY, instrument, Level.MARKET, 1000, nullAgent, timeMachine);
        order.setId(1L);
        order.resetState(timeMachine.toSimulationTime());
        checkStateAndVersion(order, OrderState.NEW, 2);
        order.cancel(timeMachine.toSimulationTime());
        checkStateAndVersion(order, OrderState.PENDING_CANCEL, 3);
        order.resetState(timeMachine.toSimulationTime());
        checkStateAndVersion(order, OrderState.CANCELLED, 4);
        order.fillQuantity(20, instrument.getLevel(100.0f), timeMachine.toSimulationTime());
        checkStateAndVersion(order, OrderState.CANCELLED, 4);
        assertFalse(order.replace(null, 2000, timeMachine.toSimulationTime()));
        checkStateAndVersion(order, OrderState.CANCELLED, 4);
    }
}
