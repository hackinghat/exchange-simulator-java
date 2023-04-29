package com.hackinghat.agent;

import com.hackinghat.model.Instrument;
import com.hackinghat.util.EventDispatcher;
import com.hackinghat.util.RandomSource;
import com.hackinghat.util.TimeMachine;
import com.hackinghat.util.mbean.MBeanType;

import java.time.Duration;

@MBeanType(description = "Simulation agent")
public class NullAgent extends Agent {
    public NullAgent(final Long id, final Instrument instrument, final RandomSource randomSource, final TimeMachine timeMachine, final String name, final EventDispatcher dispatcher) {
        super(id, instrument, randomSource, timeMachine, name, dispatcher, false);
    }

    @Override
    public Duration wakeUp() {
        return Duration.ZERO;
    }


    @Override
    protected void doActions() {
    }
}
