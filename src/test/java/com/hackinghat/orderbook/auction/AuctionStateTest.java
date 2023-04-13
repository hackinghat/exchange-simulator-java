package com.hackinghat.orderbook.auction;

import com.hackinghat.instrument.ConstantTickSizeToLevelConverter;
import com.hackinghat.model.Currency;
import com.hackinghat.model.Instrument;
import com.hackinghat.model.Level;
import com.hackinghat.order.OrderSide;
import com.hackinghat.orderbook.OrderBook;
import com.hackinghat.util.*;
import org.junit.*;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

public class AuctionStateTest
{

    private AuctionStateTestHelper testHelper;
    private TimeMachine            timeMachine;
    private Instrument             instrument;
    private Level                  referenceLevel;
    private OrderBook              bidBook;
    private OrderBook              offerBook;

    @Before
    public void setUp()
    {
        timeMachine = new TimeMachine();
        instrument = new Instrument("VOD.L", new Currency("GBP"), new ConstantTickSizeToLevelConverter(1, 2, 2));
        referenceLevel = instrument.getLevel(100.0);
        bidBook = new OrderBook(OrderSide.BUY, instrument);
        offerBook = new OrderBook(OrderSide.SELL, instrument);
        testHelper = new AuctionStateTestHelper(timeMachine, instrument, referenceLevel, bidBook, offerBook);
    }

    @After
    public void teardown()
    {
        testHelper.shutdown();
    }

    @Test
    @Ignore
    public void testNoAuction() throws AuctionException
    {
        final Instrument LLOY = new Instrument("LLOY.L", new Currency("GBP"), new ConstantTickSizeToLevelConverter(1, 2, 2));
        final AuctionState empty = new AuctionState(timeMachine.toSimulationTime(), referenceLevel, LLOY, new OrderBook(OrderSide.BUY, LLOY), new OrderBook(OrderSide.SELL, LLOY));
    }

    @Test
    public void testMaximumVolume() throws AuctionException
    {
        final AuctionState state = testHelper.makeState1();
        assertEquals(10400L, state.getMaximumVolume());
        assertEquals(104.5, state.getBid().getLevel().getPrice().doubleValue(), 1E-6);
    }

    @Test
    public void testAllOrdersAreSameSide() throws AuctionException
    {
        testHelper.addMarket(OrderSide.SELL, 2500);
        testHelper.addMarket(OrderSide.SELL, 2500);
        final AuctionState sameSide = new AuctionState(timeMachine.toSimulationTime(), referenceLevel, instrument, testHelper.sideToBook(OrderSide.BUY), testHelper.sideToBook(OrderSide.SELL));
        assertEquals(0L, sameSide.getMaximumVolume());
    }

    @Test
    public void testMinimumSurplus() throws AuctionException
    {
        final AuctionState state = testHelper.makeState2();
        assertEquals(10400L, state.getMaximumVolume());
        assertEquals(104.5, state.getBid().getLevel().getPrice().doubleValue(), 1E-6);

    }

    @Test
    public void testMarketPressure() throws AuctionException
    {
        final AuctionState state = testHelper.makeState3();
        assertEquals(10400L, state.getMaximumVolume());
        assertEquals(105.0, state.getOffer().getLevel().getPrice().doubleValue(), 1E-6);
    }

    @Test
    public void testAllOrdersMarket() throws AuctionException
    {
        testHelper.addMarket(OrderSide.BUY, 2500);
        testHelper.addMarket(OrderSide.SELL, 1000);
        final AuctionState state = new AuctionState(timeMachine.toSimulationTime(), referenceLevel, instrument, testHelper.sideToBook(OrderSide.BUY), testHelper.sideToBook(OrderSide.SELL));
        assertEquals(1000L, state.getMaximumVolume());
        assertEquals(referenceLevel, state.getBid().getLevel());
    }

    @Test
    public void testReferenceLevelLow() throws AuctionException
    {
        final AuctionState stateLow = testHelper.makeState4(referenceLevel);
        assertEquals(104.0, stateLow.getBid().getLevel().getPrice().doubleValue(), 1E-6);
        assertEquals(7000L, stateLow.getBid().getQuantity());
    }

    @Test
    public void testReferenceLevelHigh() throws AuctionException
    {
        final AuctionState stateHi = testHelper.makeState4(instrument.getLevel(110.0));
        assertEquals(104.5, stateHi.getBid().getLevel().getPrice().doubleValue(), 1E-6);
        assertEquals(7000L, stateHi.getBid().getQuantity());
    }
}
