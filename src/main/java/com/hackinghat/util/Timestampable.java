package com.hackinghat.util;

import java.time.LocalDateTime;

public interface Timestampable {
    LocalDateTime getTimestamp();

    void setTimestamp(final LocalDateTime timestamp);
}
