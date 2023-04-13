package com.hackinghat.util;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * A replacement interface for {@linkplain Cloneable} designed to set (or reset the simulation time of the
 * copied entity appropriately
 */
public interface Copyable<E> extends Cloneable, Timestampable
{
    /**
     * A necessary evil to weaken access to the {@linkplain Object#clone()} method, implementors should
     * use the following template {@code
     *      @Override
            public SomeClass cloneEx() throws CloneNotSupportedException
            {
                return SomeClass.class.cast(clone());
            }}
     * @return
     * @throws CloneNotSupportedException
     */
    Copyable<E> cloneEx() throws CloneNotSupportedException;

    default E copy()
    {
        return copy(getTimestamp());
    }

    /**
     * Get a copy but with a specified simulation time, this is because the copier may wish to start
     * the simulation time on the event otherwise it risks 'trusting' the simulation time from another
     * source (and effectively overriding the priority queue)
     * @param simulationTime the require simulation time
     * @return a clone of this object
     */
    @SuppressWarnings("unchecked")
    default E copy(final LocalDateTime simulationTime)
    {
        try
        {
            final Copyable<E> copy = cloneEx();
            copy.setTimestamp(simulationTime);
            return (E)copy;
        }
        catch (final CloneNotSupportedException clonex)
        {
            throw new IllegalArgumentException(clonex);
        }
    }
}
