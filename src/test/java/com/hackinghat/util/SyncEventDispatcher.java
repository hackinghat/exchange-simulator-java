package com.hackinghat.util;

import com.hackinghat.util.mbean.MBeanType;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;

/**
 * An observer publisher that does everything in the current call context.  Most useful for unit tests.
 */
@MBeanType(description = "AsyncEventDispatcher")
public class SyncEventDispatcher extends EventDispatcher {
    private static int DISPATCHER_INDEX = 0;
    private final List<SyncScheduledFuture<?>> futures;
    private final TimeMachine timeMachine;

    public SyncEventDispatcher(final TimeMachine timeMachine) {
        super("SyncEventDispatcher-" + DISPATCHER_INDEX++);
        this.futures = new ArrayList<>();
        this.timeMachine = timeMachine;
    }

    @Override
    public <T extends Event> void dispatch(T event) {
        dispatchSync(event.copy());
    }

    @Override
    public ScheduledFuture<?> schedule(final Callable<?> callable, final long nanosToWait) {
        final SyncScheduledFuture<?> future = new SyncScheduledFuture<>(callable, nanosToWait);
        futures.add(future);
        return future;
    }

    public void executeLapsedTasks(final LocalDateTime currentTime) throws Exception {
        final Instant latest = timeMachine.fromSimulationTime(currentTime);
        for (final SyncScheduledFuture<?> future : new ArrayList<>(futures)) {
            if (!future.isDone() && latest.isAfter(future.getExecutionInstant()))
                future.get();
        }
    }
}
