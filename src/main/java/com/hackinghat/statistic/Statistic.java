package com.hackinghat.statistic;

import com.hackinghat.util.TimeMachine;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.function.Function;

import static com.hackinghat.util.Formatters.*;

public interface Statistic
{
    String formatStatistic(final TimeMachine timeMachine);

    default  <T> void format(final StringBuilder builder, final Function<T, String> formatter, final T value, final boolean last)
    {
        if (value != null) {
            builder.append("\"");
            builder.append(formatter == null ? value.toString() : formatter.apply(value));
            builder.append("\"");
        }
        if (!last)
            builder.append(SEPARATOR);
    }

    default void formatPrice(final StringBuilder builder, final double price, final boolean last)
    {
        format(builder, (p) -> PRICE_FORMAT.get().format(p), price, last);
    }

    default void formatPrice(final StringBuilder builder, final BigDecimal price, final boolean last)
    {
        format(builder, (p) -> PRICE_FORMAT.get().format(p), price == null ? null : price.doubleValue(), last);
    }

    default void formatStatistic(final StringBuilder builder, final double statistic, final boolean last)
    {
        format(builder, (s) -> STATISTIC_FORMAT.get().format(s), statistic, last);
    }

    default void formatStatistic(final StringBuilder builder, final long statistic, final boolean last)
    {
        format(builder, (s) -> QUANTITY_FORMAT.get().format(s), statistic, last);
    }

    default void formatQuantity(final StringBuilder builder, final double quantity, final boolean last)
    {
        format(builder, (q) -> FILE_QUANTITY_FORMAT.get().format(q), quantity, last);
    }

    default void formatTime(final StringBuilder builder, final TimeMachine timeMachine, final LocalDateTime time, final boolean last)
    {
        format(builder, timeMachine::formatTime, time, last);
    }

    default void formatString(final StringBuilder builder, final String string, final boolean last)
    {
        format(builder, null, string, last);
    }
}
