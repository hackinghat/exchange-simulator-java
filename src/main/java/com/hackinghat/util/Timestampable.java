package com.hackinghat.util;

import java.time.LocalDateTime;
import java.time.LocalTime;

public interface Timestampable
{
    LocalDateTime   getTimestamp();
    void            setTimestamp(final LocalDateTime timestamp);
}
