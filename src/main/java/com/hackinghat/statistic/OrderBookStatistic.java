package com.hackinghat.statistic;

import com.hackinghat.orderbook.FullDepth;
import com.hackinghat.orderbook.Level1;
import com.hackinghat.orderbook.OrderInterest;
import com.hackinghat.util.Pair;
import com.hackinghat.util.TimeMachine;

import java.time.LocalDateTime;
import java.util.Collection;

public class OrderBookStatistic implements SampledStatistic<Pair<Level1, FullDepth>> {
    private final VarianceStatistic bidTouchPriceVar;
    private final VarianceStatistic offerTouchPriceVar;
    private final VarianceStatistic midTouchPriceVar;

    private final VarianceStatistic bidReturnVar;
    private final VarianceStatistic offerReturnVar;
    private final VarianceStatistic midReturnVar;

    private final VarianceStatistic bidTouchDepthVar;
    private final VarianceStatistic offerTouchDepthVar;

    private final VarianceStatistic bidOfferSpreadVar;

    private final LaggingStatistic bidPrice;
    private final LaggingStatistic offerPrice;
    private final LaggingStatistic midPrice;

    private LocalDateTime simulationTime;
    private String marketState;
    private double bidTouchPrice;
    private double offerTouchPrice;
    private double midTouchPrice;
    private double bidOfferSpread;
    private double bidTouchDepth;
    private double offerTouchDepth;
    private double bidReturn;
    private double offerReturn;
    private double midReturn;
    private double imbalance;

    public OrderBookStatistic(int varPeriodLength, int lagLength) {
        bidTouchPriceVar = new VarianceStatistic("Bid Touch Price Var", 0.0, varPeriodLength);
        offerTouchPriceVar = new VarianceStatistic("Offer Touch Price Var", 0.0, varPeriodLength);
        midTouchPriceVar = new VarianceStatistic("Mid Touch Price Var", 0.0, varPeriodLength);
        bidReturnVar = new VarianceStatistic("Bid Return Var", 0.0, varPeriodLength);
        offerReturnVar = new VarianceStatistic("Offer Return Var", 0.0, varPeriodLength);
        midReturnVar = new VarianceStatistic("Mid Return Var", 0.0, varPeriodLength);
        bidTouchDepthVar = new VarianceStatistic("Bid Touch Depth Var", 0.0, varPeriodLength);
        offerTouchDepthVar = new VarianceStatistic("Offer Touch Depth Var", 0.0, varPeriodLength);
        bidOfferSpreadVar = new VarianceStatistic("Bid Offer Spread Var", 0.0, varPeriodLength);

        bidPrice = new LaggingStatistic("Bid Price", 0.0, lagLength, lagLength);
        offerPrice = new LaggingStatistic("Offer Price", 0.0, lagLength, lagLength);
        midPrice = new LaggingStatistic("Mid Price", 0.0, lagLength, lagLength);
    }

    @Override
    public void update(final Pair<Level1, FullDepth> sample) {
        if (sample == null)
            return;

        final Level1 level1 = sample.getFirst();
        final FullDepth fullDepth = sample.getSecond();

        // Level1 could be null at start of day
        if (level1 == null)
            return;

        final OrderInterest bid = level1.getBid();
        final OrderInterest offer = level1.getOffer();

        // State
        simulationTime = level1.getTimestamp();
        marketState = level1.getMarketState().toString();

        // Touch prices
        bidTouchPrice = bid.getLevel().getPrice();
        offerTouchPrice = offer.getLevel().getPrice();
        midTouchPrice = (bidTouchPrice + offerTouchPrice) / 2.0;
        bidPrice.update(bidTouchPrice);
        offerPrice.update(offerTouchPrice);
        midPrice.update(midTouchPrice);

        bidTouchPriceVar.update(bidTouchPrice);
        offerTouchPriceVar.update(offerTouchPrice);
        midTouchPriceVar.update(midTouchPrice);

        // Spread
        bidOfferSpread = offerTouchPrice - bidTouchPrice;
        bidOfferSpreadVar.update(bidOfferSpread);

        // Depth
        bidTouchDepth = bid.getQuantity();
        offerTouchDepth = offer.getQuantity();
        bidTouchDepthVar.update(bidTouchDepth);
        offerTouchDepthVar.update(offerTouchDepth);

        // Returns
        bidReturn = bidTouchPrice == 0 ? 0.0 : bidPrice.getValue() / bidTouchPrice;
        offerReturn = offerTouchPrice == 0 ? 0.0 : offerPrice.getValue() / offerTouchPrice;
        midReturn = midTouchPrice == 0 ? 0.0 : midPrice.getValue() / midTouchPrice;
        bidReturnVar.update(bidReturn);
        offerReturnVar.update(offerReturn);
        midReturnVar.update(midReturn);

        // Imbalance
        imbalance = fullDepth == null ? 0.0 : calculateImbalance(fullDepth);
    }

    public String getHeaders() {
        return "T,Day#,State,Bid,Mid,Offer,Spd,BVol,OVol,BRet,MRet,ORet,SpdVar,BVar,OVar,MVar,BRetVar,MRetVar,ORetVar,BVolVar,OVolVar,Imb";
    }

    private double calculateImbalance(final FullDepth fullDepth) {
        final Collection<OrderInterest> bidDepth = fullDepth.getBidDepth();
        double bidVol = bidDepth.stream().mapToLong(OrderInterest::getQuantity).sum();
        final Collection<OrderInterest> offerDepth = fullDepth.getOfferDepth();
        double offerVol = offerDepth.stream().mapToLong(OrderInterest::getQuantity).sum();
        double totalVol = (bidVol + offerVol);
        return totalVol < 1 ? 0 : (bidVol - offerVol) / (bidVol + offerVol);
    }

    @Override
    public String formatStatistic(final TimeMachine timeMachine) {
        StringBuilder statistic = new StringBuilder();
        // A race condition may result in format statistic being called before we have been 'updated'
        // TODO: probably needs some sort of atomic int or bloolean to count the number of updates before
        // producing output
        if (simulationTime != null) {
            formatTime(statistic, timeMachine, simulationTime, false);
            format(statistic, null, timeMachine.getStartCount(), false);
            formatString(statistic, marketState, false);
            formatPrice(statistic, bidTouchPrice, false);
            formatPrice(statistic, midTouchPrice, false);
            formatPrice(statistic, offerTouchPrice, false);
            formatPrice(statistic, bidOfferSpread, false);
            formatQuantity(statistic, bidTouchDepth, false);
            formatQuantity(statistic, offerTouchDepth, false);
            formatStatistic(statistic, bidReturn, false);
            formatStatistic(statistic, midReturn, false);
            formatStatistic(statistic, offerReturn, false);
            formatStatistic(statistic, bidOfferSpreadVar.getValue(), false);
            formatStatistic(statistic, bidTouchPriceVar.getValue(), false);
            formatStatistic(statistic, offerTouchPriceVar.getValue(), false);
            formatStatistic(statistic, midTouchPriceVar.getValue(), false);
            formatStatistic(statistic, bidReturnVar.getValue(), false);
            formatStatistic(statistic, midReturnVar.getValue(), false);
            formatStatistic(statistic, offerReturnVar.getValue(), false);
            formatStatistic(statistic, bidTouchDepthVar.getValue(), false);
            formatStatistic(statistic, offerTouchDepthVar.getValue(), false);
            formatStatistic(statistic, imbalance, true);
            return statistic.toString();
        }
        return null;
    }
}