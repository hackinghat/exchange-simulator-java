package com.hackinghat.model;

import com.hackinghat.util.Copyable;
import com.hackinghat.util.CopyableAndIdentifiable;

import java.time.LocalDateTime;

public class Currency implements CopyableAndIdentifiable<String> {
    private final String iso3;
    private final int scale;
    private LocalDateTime timeStamp;

    public Currency(final String iso3) {
        this(iso3, 2, LocalDateTime.now());
    }

    public Currency(final String iso3, final LocalDateTime timeStamp) {
        this(iso3, 2, timeStamp);
    }

    public Currency(final String iso3, final int scale, final LocalDateTime timestamp) {
        this.iso3 = iso3;
        this.scale = scale;
        this.timeStamp = timestamp;
    }

    @Override
    public Copyable cloneEx() {
        return new Currency(iso3, scale, timeStamp);
    }

    @Override
    public String getId() {
        return iso3;
    }

    public int getScale() {
        return scale;
    }

    public String getIso3() {
        return iso3;
    }

    @Override
    public LocalDateTime getTimestamp() {
        return timeStamp;
    }

    @Override
    public void setTimestamp(final LocalDateTime timestamp) {
        this.timeStamp = timestamp;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == null) return false;
        if (!(other instanceof Currency)) return false;
        return iso3.equals(((Currency) other).getIso3());
    }
}
