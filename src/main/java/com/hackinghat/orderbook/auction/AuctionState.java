package com.hackinghat.orderbook.auction;

import com.hackinghat.model.Instrument;
import com.hackinghat.model.Level;
import com.hackinghat.order.MarketState;
import com.hackinghat.order.OrderSide;
import com.hackinghat.orderbook.Level1;
import com.hackinghat.orderbook.OrderBook;
import com.hackinghat.orderbook.OrderInterest;
import com.hackinghat.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.hackinghat.util.Formatters.QUANTITY_FORMAT;

/**
 * The state of the order book if it is in auction.  The behaviour of this class is largely taken
 * from the LSE's technical documentation on how auction prices are set.  The central idea is to
 * start with a set of possible prices and then gradually restrict the set until a single price remains.
 * <p>
 * 1. Maximum volume - the maximum quantity that can execute at the chosen price
 * 2. Market pressure - when multiple prices result on the same side the pressure of the
 * remaining un-executed orders would tend to make the price move there
 * 3. Minimum getSurplus - the market pressure failed so the prices must be on opposite sides of the book
 * pick the price that gives the smallest remaining order quantity at the price
 * 4. Reference price - the price that is closest to the last traded price is chosen
 * <p>
 * Under some circumstances the below process may not lead to a price, in that circumstance auction
 * extensions are added:
 * <p>
 * 1. Price monitoring extension - if the auction price would be far from the last traded
 * 2. Market monitoring extension - ??
 * <p>
 * AuctionState implements a the {@link Level1} interface which means it can be seen by all market
 * participants.  Therefore it would suit being cached and re-calculated only when require and only
 * if the auction state could potentially have changed (due to new or cancelled orders).
 */
public class AuctionState implements Level1 {
    private static final Logger LOG = LogManager.getLogger(AuctionState.class);
    private final Instrument instrument;
    private final Level referenceLevel;
    private Map<Level, AuctionRecord> auctionRecords;
    private long maximumVolume;
    private LocalDateTime simulationTime;

    public AuctionState(final LocalDateTime simulationTime, final Level referenceLevel, final Instrument instrument) {
        this.instrument = instrument;
        this.auctionRecords = Collections.emptyMap();
        this.maximumVolume = 0;
        this.referenceLevel = referenceLevel;
        this.simulationTime = simulationTime;
    }

    public AuctionState(final LocalDateTime simulationTime, final Level referenceLevel, final Instrument instrument, final OrderBook... bidOfferBooks) {
        this(simulationTime, referenceLevel, instrument);

        // If we're constructed without books it means that we tried before ??
        if (bidOfferBooks != null && bidOfferBooks.length > 0) {
            if (bidOfferBooks.length != 2 || bidOfferBooks[0].getQueueSide() == bidOfferBooks[1].getQueueSide())
                throw new IllegalArgumentException("Auction state can only be calculated for opposing books");

            this.auctionRecords = new HashMap<>(bidOfferBooks[0].size() + bidOfferBooks[1].size());
            addAllLevels(bidOfferBooks);
            for (final OrderBook book : bidOfferBooks) {
                calculateAggregateVolumes(book.getQueueSide());
            }
            maximumVolume = calculateMaximumVolume(auctionRecords.values());
        }
        if (LOG.isTraceEnabled())
            LOG.trace("Maximum executable volume: " + maximumVolume);
    }

    private static long calculateMaximumVolume(final Collection<AuctionRecord> auctionRecords) {
        final OptionalLong maxVol = auctionRecords.stream()
                .mapToLong(AuctionRecord::maxVolume)
                .max();
        return maxVol.isPresent() ? maxVol.getAsLong() : 0L;
    }

    long getMaximumVolume() {
        return maximumVolume;
    }

    /**
     * Add all the required levels to the auction table
     *
     * @param books the books to inspect
     */
    private void addAllLevels(final OrderBook... books) {
        for (final OrderBook book : books) {
            for (final OrderInterest interest : book.getExecutableLevels()) {
                AuctionRecord record = auctionRecords.get(interest.getLevel());
                if (record == null) {
                    record = new AuctionRecord(interest.getLevel());
                    auctionRecords.put(interest.getLevel(), record);
                }
                record.setVolume(book.getQueueSide(), interest.getQuantity());
            }
        }
        // Now fill in the levels in-between
        fillLevels();
    }

    /**
     * Fill in empty auction levels with zero volumes
     */
    private void fillLevels() {
        Level last = null;
        for (final Level level : getAuctionLevels(OrderSide.BUY)) {
            if (last != null && !level.isMarket() && !last.isMarket()) {
                while (level.levelCompare(OrderSide.BUY, last) == 1) {
                    last = instrument.worsenOnBook(level, OrderSide.BUY, 1);
                    if (!auctionRecords.containsKey(last))
                        auctionRecords.put(last, new AuctionRecord(last));
                }
            }
            last = level;
        }
    }

    private Collection<Level> getAuctionLevels(final OrderSide side) {
        LinkedList<Level> levels = new LinkedList<>(auctionRecords.keySet());
        levels.sort(Level.comparator(side));
        return levels;
    }

    private void calculateAggregateVolumes(final OrderSide side) {
        long aggregateVolume = 0;
        for (final Level level : getAuctionLevels(side)) {
            final AuctionRecord record = auctionRecords.get(level);
            aggregateVolume += record.getUncrossingVolume(side);
            record.addVolume(side, aggregateVolume);
        }
    }

    @Override
    public LocalDateTime getTimestamp() {
        return simulationTime;
    }

    @Override
    public void setTimestamp(final LocalDateTime timestamp) {
        this.simulationTime = timestamp;
    }

    private List<AuctionRecord> getRecordsAtVolume(final long maximumVolume) {
        return auctionRecords.values().stream()
                .filter(auctionRecord -> auctionRecord.getUncrossingVolume() == maximumVolume)
                .collect(Collectors.toList());
    }

    private List<AuctionRecord> findLevelOfMinimumSurplus(final Collection<AuctionRecord> records, final long maximumVolume) {
        long minSurplus = Long.MAX_VALUE;
        final List<AuctionRecord> result = new ArrayList<>(records.size());
        for (final AuctionRecord record : records) {
            //  Surplus is signed, at this point we only care about the magnitude, not the direction
            final long surplus = Math.abs(record.getSurplus(maximumVolume));
            if (surplus <= minSurplus) {
                if (surplus < minSurplus) {
                    result.clear();
                    minSurplus = surplus;
                }
                result.add(record);
            }
        }
        return result;
    }

    private List<AuctionRecord> findMinimumPressure(final List<AuctionRecord> records, final long maximumVolume) {
        OrderSide[] remainingPressure = new OrderSide[records.size()];
        for (int i = 0; i < records.size(); ++i) {
            final AuctionRecord record = records.get(i);
            final long pressure = records.stream().filter(r -> r != record)
                    .mapToLong(r -> r.getSurplus(maximumVolume)).sum();
            remainingPressure[i] = pressure == 0 ? null : pressure > 0 ? OrderSide.BUY : OrderSide.SELL;
        }
        final List<OrderSide> uniqueSides = Arrays.stream(remainingPressure)
                .distinct()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // If all sides are balanced then we can't use pressure to decide
        if (uniqueSides.size() != 1)
            return records;

        // All the executable prices are on the same side so pick the side closest to the centre
        Comparator<Level> sorter = Level.comparator(uniqueSides.get(0));

        final List<AuctionRecord> limitRecords = records.stream()
                .distinct()
                .filter(ar -> !ar.getLevel().isMarket())
                .sorted((a, b) -> sorter.compare(a.getLevel(), b.getLevel()))
                .collect(Collectors.toList());

        // We want only the limit records and we want them in book order (we will pick the record at the top)
        return limitRecords.size() > 0 ? Collections.singletonList(limitRecords.get(0)) : records;
    }

    private List<AuctionRecord> findClosestToReference(final List<AuctionRecord> records) {
        AuctionRecord minimum = null;
        for (final AuctionRecord record : records) {
            if (minimum == null || (record.getLevel().absoluteticksBetween(referenceLevel) < minimum.getLevel().absoluteticksBetween(referenceLevel))) {
                minimum = record;
            }
        }
        return Collections.singletonList(minimum);
    }

    private AuctionRecord chooseSolution(Collection<AuctionRecord> records, final String reason) throws AuctionException {
        if (records.size() == 0)
            throw new AuctionException(reason);

        // Although market records are useful in calculating they can't be a solution for the level
        final List<AuctionRecord> stripMarkets = records.stream()
                .filter(r -> !r.getLevel().isMarket())
                .collect(Collectors.toList());

        if (stripMarkets.size() == 1)
            return stripMarkets.iterator().next();

        return null;
    }

    public Pair<Level, Long> getUncrossingInterest() throws AuctionException {
        final long maximumVolume = getMaximumVolume();
        if (maximumVolume == 0L)
            return Pair.instanceOf(referenceLevel, 0L);

        List<AuctionRecord> records = getRecordsAtVolume(maximumVolume);
        AuctionRecord result = chooseSolution(records, "Internal error, no auction records with volume: " + maximumVolume);
        if (result == null) {
            records = findLevelOfMinimumSurplus(records, maximumVolume);
            result = chooseSolution(records, "Internal error, should be at least one record returned when examining minimum surplus: " + maximumVolume);
        }
        if (result == null) {
            records = findMinimumPressure(records, maximumVolume);
            result = chooseSolution(records, "Internal error, should be at least one record from calculating minimum pressure");
        }
        if (result == null) {
            records = findClosestToReference(records);
            result = chooseSolution(records, "Internal error, should be at least one record found from finding closest to reference");
        }
        if (result == null) {
            // No solution found (most likely there are only market orders in the books)
            return Pair.instanceOf(referenceLevel, calculateMaximumVolume(records));
        }
        return Pair.instanceOf(result.getLevel(), result.getUncrossingVolume());
    }

    public OrderInterest getInterest(OrderSide side) {
        try {
            final Pair<Level, Long> interest = getUncrossingInterest();
            return new OrderInterest(side, interest.getFirst(), interest.getSecond());
        } catch (final AuctionException aucex) {
            return new OrderInterest(side, Level.MARKET, 0L);
        }
    }

    @Override
    public OrderInterest getBid() {
        return getInterest(OrderSide.BUY);
    }

    @Override
    public OrderInterest getOffer() {
        return getInterest(OrderSide.SELL);
    }

    @Override
    public MarketState getMarketState() {
        return MarketState.AUCTION;
    }

    @Override
    public MarketState getTouchState() {
        return MarketState.AUCTION;
    }

    @Override
    public AuctionState cloneEx() throws CloneNotSupportedException {
        return (AuctionState) clone();
    }

    private static class AuctionRecord implements Comparable<AuctionRecord> {
        @Nonnull
        private final Level level;

        private long bidVolume;
        private long bidAggVolume;
        private long offerVolume;
        private long offerAggVolume;
        private long volume;

        private AuctionRecord(@Nonnull final Level level) {
            this.level = level;
            this.bidAggVolume = 0;
            this.offerAggVolume = 0;
        }

        /**
         * By the definition of surplus and aggregate volume both sides must have aggregate volume less than or equal to
         * the maximum volume.   Further, one side (or more) will have surplus equal to zero.
         *
         * @param maximumVolume the maximum volume
         * @return the auction surplus
         */
        long getSurplus(final long maximumVolume) {
            final long bidSurplus = bidAggVolume - maximumVolume;
            final long offerSurplus = maximumVolume - offerAggVolume;
            return bidSurplus == 0 ? offerSurplus : bidSurplus;
        }

        @Override
        public int hashCode() {
            return level.hashCode();
        }

        @Override
        public int compareTo(@Nonnull  final AuctionRecord other) {
            return level.levelCompare(OrderSide.BUY, level);
        }

        private void setVolume(final OrderSide side, final long volume) {
            switch (side) {
                case BUY:
                    bidVolume = volume;
                    break;
                case SELL:
                    offerVolume = volume;
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected side: " + side);
            }
        }

        private long maxVolume() {
            return Math.min(bidAggVolume, offerAggVolume);
        }

        private void addVolume(final OrderSide side, final long volume) {
            switch (side) {
                case BUY:
                    bidAggVolume = volume;
                    break;
                case SELL:
                    offerAggVolume = volume;
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected side: " + side);
            }
            this.volume = Math.min(bidAggVolume, offerAggVolume);
        }

        private long getUncrossingVolume(final OrderSide side) {
            switch (side) {
                case BUY:
                    return bidVolume;
                case SELL:
                    return offerVolume;
                default:
                    throw new IllegalArgumentException("Unexpected side: " + side);
            }
        }

        private long getUncrossingVolume() {
            return volume;
        }

        @Nonnull
        private Level getLevel() {
            return level;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "(level= " + level + ", bidAggVolume=" + QUANTITY_FORMAT.get().format(bidAggVolume) +
                    ", offerAggVolume=" + QUANTITY_FORMAT.get().format(offerAggVolume) + ")";
        }
    }
}
