package com.hackinghat.model;

import com.hackinghat.statistic.Statistic;
import com.hackinghat.util.Event;
import com.hackinghat.util.Identifiable;
import com.hackinghat.util.TimeMachine;

import java.time.LocalDateTime;
import java.util.Objects;

/** Immutable trade object event that is broadcast by the order manager on the successful matching
 *  of a trade.
 */
public class Trade extends Event implements Statistic, Identifiable<String>
{
    private String    tradeId;
    private Instrument      instrument;
    private String          order1;
    private String          order2;
    private String          flags;
    private Level           level;
    private Integer         quantity;

    public Trade() {
        super();
    }

    public Trade(final Object sender, final String tradeId, final Instrument instrument, LocalDateTime simulationTime, final String flags, final String order1, final String order2, final Level level, final Integer quantity)
    {
        super(sender, simulationTime);
        Objects.requireNonNull(tradeId);
        Objects.requireNonNull(instrument);
        Objects.requireNonNull(sender);
        Objects.requireNonNull(order1);
        Objects.requireNonNull(order2);
        Objects.requireNonNull(level);
        Objects.requireNonNull(level);
        Objects.requireNonNull(quantity);
        this.instrument = instrument;
        this.order1 = order1;
        this.order2 = order2;
        this.flags = flags;
        this.level = level;
        this.quantity = quantity;
    }

    public String getTradeId() { return tradeId; }
    public void setTradeId(final String tradeId) { this.tradeId = tradeId; }

    public String getOrder1() { return order1; }
    public void setOrder1(final String order1) { this.order1 = order1; }

    public String getOrder2() { return order2; }
    public void setOrder2(final String order2) { this.order2 = order2; }

    public String getFlags() { return flags; }
    public void setFlags(final String flags) { this.flags = flags; }

    public Level getLevel() { return level; }
    public void setLevel(final Level level) { this.level = level; }

    public Integer getQuantity() {
        return quantity;
    }
    public void setQuantity(final Integer quantity) {
        this.quantity = quantity;
    }

    public Instrument getInstrument() { return instrument; }
    public void setInstrument(final Instrument instrument) { this.instrument = instrument; }

    public static String getStatisticNames()
    {
        return "\"T\",\"Day#\",\"Flags\",\"Order1\",\"Order2\",\"Price\",\"Quantity\"";
    }

    public String formatStatistic(final TimeMachine timeMachine)
    {
        final StringBuilder builder = new StringBuilder();
        formatTime(builder, timeMachine, simulationTime, false);
        format(builder, null, timeMachine.getStartCount(), false);
        formatString(builder, flags, false);
        formatString(builder, order1, false);
        formatString(builder, order2, false);
        formatPrice(builder, getLevel().getPrice(), false);
        format(builder, null, getQuantity(), true);
        return builder.toString();
    }

    @Override
    public String toString() {
        return "Trade{" +
                "timestamp=" + simulationTime +
                ", flags=" + flags +
                ", level=" + level +
                ", quantity=" + quantity +
                '}';
    }

    @Override
    public String getId() {
        return getTradeId();
    }
}
