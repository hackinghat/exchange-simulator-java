package com.hackinghat.util;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

class RandomObject implements CopyableAndIdentifiable<String> {
    final String id;
    final Object random;
    LocalDateTime timestamp;

    public RandomObject(final String id, final Object object) {
        this.id = id;
        this.random = object;
        this.timestamp = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    @Override
    public RandomObject cloneEx() throws CloneNotSupportedException {
        return RandomObject.class.cast(this.clone());
    }

    @Override
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(final Object other) {
        Objects.requireNonNull(other);
        if (!other.getClass().equals(getClass()))
            return false;
        final RandomObject randOther = (RandomObject) other;
        return id.equals(randOther.id) && random.equals(randOther.random) && timestamp.equals(randOther.timestamp);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + id + ", " + random + ")";
    }
}

