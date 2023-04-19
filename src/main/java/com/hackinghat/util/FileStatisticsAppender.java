package com.hackinghat.util;

import com.hackinghat.statistic.Statistic;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;

public class FileStatisticsAppender<T extends Statistic> extends AbstractStatisticsAppender
{
    private final static DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmssSSSSSS");
    private final static int               DEFAULT_FLUSH_ITEMS = 10;

    private final String                fileName;
    private final Supplier<String>      headerFunction;
    private final int                   flushLines;

    private BufferedWriter              writer;
    private int                         linesWritten;

    public FileStatisticsAppender(final Supplier<String> headerFunction, final Instant creationTime, final String fileName) {
        this(headerFunction, creationTime, fileName, DEFAULT_FLUSH_ITEMS);
    }

    public FileStatisticsAppender(final Supplier<String> headerFunction, final Instant creationTime, final String fileName, int flushLines) {
        super();
        Objects.requireNonNull(fileName);
        Objects.requireNonNull(headerFunction);

        if (fileName.length() == 0)
            throw new IllegalArgumentException("No filename specified for statistics file");

        this.fileName = fileName + "." + TIME_FORMATTER.format(creationTime.atZone(ZoneId.systemDefault())) + ".csv";
        this.headerFunction = headerFunction;
        this.linesWritten = 0;
        this.flushLines = flushLines;
    }

    @Override
    public void configure() {
        try {
            writer = new BufferedWriter(new FileWriter(this.fileName));
            writer.write(String.join(",", headerFunction.get()));
            writer.newLine();
            writer.flush();
        }
        catch (final IOException ioex) {
            throw new IllegalArgumentException("Unable to create filewriter tape", ioex);
        }
    }

    @Override
    public void close() {
        super.close();
        try {
            writer.flush();
            writer.close();
        }
        catch (final IOException ioex) {
            LOG.error("Couldn't close file: " + fileName + ", reason: ", ioex);
        }
    }

    @Override
    protected void process(final Collection<String> lines) {
        try {
            for (final String line : lines) {
                writer.write(line);
                writer.newLine();
                linesWritten++;
            }
            if (linesWritten % flushLines == 0)
                writer.flush();
        }
        catch (final IOException ioex) {
            LOG.error("Couldn't log statistics, reason: ", ioex);
        }
    }
}
