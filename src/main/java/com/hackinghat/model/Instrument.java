package com.hackinghat.model;


import com.hackinghat.order.OrderSide;
import com.hackinghat.util.CopyableAndIdentifiable;
import com.hackinghat.util.Identifiable;
import com.hackinghat.util.Timestampable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/**
 * The instrument is simply a loose collection of things that are instrument specific.  At this stage
 * all that we need to know about the instrument is its ticker (for display) and it's tick definition.
 *
 * In respect of the tick definition this is handled by the LevelDefinition which keeps a map of tick
 * levels.
 */
public class Instrument implements CopyableAndIdentifiable<String>
{
    private LocalDateTime   timestamp;
    private LevelDefinition levelDefinition;
    private String          ticker;
    private String          description;
    private Currency        currency;

    @Override
    public Instrument cloneEx() throws CloneNotSupportedException {
        return new Instrument(ticker, description, currency, getLevelDefinition().getTickConverter(), timestamp);
    }

    @Override
    public String getId() {
        return ticker;
    }

    public void setTicker(final String ticker) {
        this.ticker = ticker;
    }

    @Override
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public void setTimestamp(final LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Instrument() {}

    public  Instrument(final String ticker, final Currency currency, final TickConverter tickConverter) {
        this(ticker, null, currency, tickConverter, LocalDateTime.now());
    }

    public  Instrument(final String ticker, final String description, final Currency currency, final TickConverter tickConverter) {
        this(ticker, description, currency, tickConverter, LocalDateTime.now());
    }

    public  Instrument(final String ticker, final String description , final Currency currency, final TickConverter tickConverter, final LocalDateTime timestamp) {
        this.ticker = ticker;
        this.description = description;
        this.currency = currency;
        this.levelDefinition = new LevelDefinition(tickConverter);
        this.timestamp = timestamp;
    }

    public String getTicker() {
        return ticker;
    }
    public LevelDefinition getLevelDefinition() { return levelDefinition; }

    public String getDescription() { return description; }
    public void setDescription(final String description) { this.description = description; }

    public Currency getCurrency() { return currency; }

    @Override
    public String toString() {
        return "Instrument{" +
                "tickSize=" + levelDefinition.getTickConverter() +
                ", ticker='" + ticker + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Instrument)) return false;
        Instrument that = (Instrument) o;
        if (!Objects.equals(levelDefinition, that.levelDefinition)) return false;
        if (!Objects.equals(currency, that.currency)) return false;
        return Objects.equals(ticker, that.ticker);
    }

    @Override
    public int hashCode() {
        int result = levelDefinition.hashCode();
        result = 31 * result + (ticker != null ? ticker.hashCode() : 0);
        result = 31 * result + (currency != null ? currency.hashCode() : 0);
        return result;
    }

    public BigDecimal roundToTick(Number price)
    {
        return levelDefinition.getTickConverter().roundToTick(price);
    }

    /**
     * Calculate a new level by 'bettering' the given one.  The bettering is from the point-of-view
     * of the book, considering the 'best' price is at the top of the book.
     * @param level the reference level
     * @param queueSide the side the bettering is for
     * @param nLevels the number of levels to better by
     * @return a new level
     * @throws IllegalArgumentException if nLevels < 0
     */
    public Level betterOnBook(final Level level, final OrderSide queueSide, int nLevels)
    {
        if (nLevels < 0)
            throw new IllegalArgumentException("Can't negatively worsenOnBook, try better!");
        return adjustBy(level, queueSide, -nLevels);
    }

    /**
     * Calculate a new level by 'worsening' the given one.  The bettering is from the point-of-view
     * of the book, considering the 'best' price is at the top of the book.
     * @param level the reference level
     * @param queueSide the side the worsening is for
     * @param nLevels the number of levels to worsen by
     * @return a new level
     * @throws IllegalArgumentException if nLevels < 0
     */
    public Level worsenOnBook(final Level level, final OrderSide queueSide, int nLevels)
    {
        if (nLevels < 0)
            throw new IllegalArgumentException("Can't negatively better, try worsenOnBook!");
        return adjustBy(level, queueSide, nLevels);
    }

    Level adjustBy(Level level, OrderSide queueSide, int nLevels)
    {
        return levelDefinition.adjustBy(level, queueSide, nLevels);
    }

    public Level getMarket()
    {
        return LevelDefinition.makeMarket();
    }

    public Level getLevel(final Double price)
    {
        return getLevel(new BigDecimal(price));
    }

    public Level getLevel(final BigDecimal price)
    {
        return price == null ? LevelDefinition.makeMarket() : levelDefinition.makeLimit(price);
    }
}
