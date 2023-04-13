package com.hackinghat.util;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Formatters {
    public final static ThreadLocalFormat<DecimalFormat> QUANTITY_FORMAT = new ThreadLocalFormat<>(DecimalFormat.class, "#,###");
    public final static ThreadLocalFormat<DecimalFormat> FILE_QUANTITY_FORMAT = new ThreadLocalFormat<>(DecimalFormat.class, "#");
    public final static ThreadLocalFormat<DecimalFormat> PRICE_FORMAT = new ThreadLocalFormat<>(DecimalFormat.class, "#,###.####");
    public final static ThreadLocalFormat<DecimalFormat> STATISTIC_FORMAT = new ThreadLocalFormat<>(DecimalFormat.class, "#.############");
    public final static ThreadLocalFormat<SimpleDateFormat> TIME_FORMAT = new ThreadLocalFormat<>(SimpleDateFormat.class,  "hh:mm:ss.SSS");
    public final static String SEPARATOR = ",";

    public static StringBuilder addIfNotNull(StringBuilder b, final Object o)
    {
        if (o != null)
        {
            b.append("\"");
            b.append(o);
            b.append("\"");
        }
        return b;
    }

    public static String dateFormat(final long millis)
    {
        return TIME_FORMAT.get().format(new Date(millis));
    }
}
