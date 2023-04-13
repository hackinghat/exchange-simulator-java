package com.hackinghat.util;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class TimeSpeedChangeEvent extends Event
{
    private final double oldDelta;
    private final double newDelta;

    public TimeSpeedChangeEvent(Object sender, final LocalDateTime simulationTime, final double oldDelta, final double newDelta)
    {
        super(sender, simulationTime);
        if (Math.abs(oldDelta - newDelta) - 1E-9 > 0)
            throw new IllegalArgumentException("Delta hasn't changed, not creating event for no change");
        this.oldDelta = oldDelta;
        this.newDelta = newDelta;
    }

    public double getOldDelta()
    {
        return oldDelta;
    }

    public double getNewDelta()
    {
        return newDelta;
    }
}
