package com.hackinghat.orderbook;

import com.hackinghat.model.ConstantTickSizeToLevelConverter;
import com.hackinghat.model.Currency;
import com.hackinghat.model.Instrument;
import com.hackinghat.model.Level;
import com.hackinghat.order.MarketState;
import com.hackinghat.order.OrderSide;
import com.hackinghat.util.TimeMachine;
import org.junit.Before;
import org.junit.Test;

import static com.hackinghat.order.LevelTest.makeLevel;
import static org.junit.Assert.assertEquals;

public class TouchTest {
    private TimeMachine timeMachine;

    @Before
    public void before() {
        timeMachine = new TimeMachine();
    }


    @Test(expected = InvalidMarketStateException.class)
    public void testTicksBetweenChoice() throws InvalidMarketStateException {
        final Instrument vod = new Instrument("VOD", new Currency("GBP"), new ConstantTickSizeToLevelConverter(1, 100, 3));
        Level level = makeLevel(100.1f, vod);
        Touch t = new Touch(timeMachine.toSimulationTime(), MarketState.CONTINUOUS, new OrderInterest(OrderSide.BUY, level, 1, 1000),
                new OrderInterest(OrderSide.SELL, level, 1, 1000));
        assertEquals(MarketState.CHOICE, t.getTouchState());
        // Whilst we could calculate this we want it to throw because there is no valid spread
        t.ticksBetweenBidAndOffer();
    }

    @Test(expected = InvalidMarketStateException.class)
    public void testTicksBetweenBack() throws InvalidMarketStateException {
        Instrument vod = new Instrument("VOD", new Currency("GBP"), new ConstantTickSizeToLevelConverter(1, 100, 3));
        Touch t = new Touch(timeMachine.toSimulationTime(), MarketState.CONTINUOUS, new OrderInterest(OrderSide.BUY, makeLevel(100.1f, vod), 200, 1000),
                new OrderInterest(OrderSide.SELL, makeLevel(99.1f, vod), 100, 1000));
        assertEquals(MarketState.BACK, t.getTouchState());
        // Whilst we could calculate this we want it to throw because there is no valid spread
        t.ticksBetweenBidAndOffer();
    }

    @Test
    public void testNoMarket() {
        Instrument vod = new Instrument("VOD", new Currency("GBP"), new ConstantTickSizeToLevelConverter(1, 100, 3));
        final Touch t = new Touch(timeMachine.toSimulationTime(), MarketState.CONTINUOUS, new OrderInterest(OrderSide.BUY, vod.getMarket(), 0), new OrderInterest(OrderSide.SELL, vod.getMarket(), 0));
        assertEquals(MarketState.CHOICE, t.getTouchState());
    }
}
