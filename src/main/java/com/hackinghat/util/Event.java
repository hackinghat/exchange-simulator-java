package com.hackinghat.util;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

public abstract class Event implements Copyable<Event>, Comparable<Event>
{
    protected Object        sender;
    protected LocalDateTime simulationTime;

    public Event() { }

    public Event(final Object sender, final LocalDateTime simulationTime)
    {
        Objects.requireNonNull(sender);
        this.sender = sender;
        this.simulationTime = simulationTime;
    }

    @JsonIgnore
    public Object getSender() { return sender; }

    @JsonIgnore
    public void setSender(final Object sender) { this.sender = sender; }

    public LocalDateTime getTimestamp()
    {
        return simulationTime;
    }
    public void setTimestamp(final LocalDateTime simulationTime)
    {
        this.simulationTime = simulationTime;
    }

    public boolean isSender(final Object querySender)
    {
        return System.identityHashCode(querySender) == System.identityHashCode(sender);
    }

    @Override
    public Event cloneEx() throws CloneNotSupportedException
    {
        return Event.class.cast(clone());
    }


    @Override
    public int compareTo(final Event o)
    {
        Objects.requireNonNull(o);
        Objects.requireNonNull(simulationTime);
        return simulationTime.compareTo(o.simulationTime);
    }
}
