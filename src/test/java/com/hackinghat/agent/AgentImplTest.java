package com.hackinghat.agent;

import com.hackinghat.model.Instrument;
import com.hackinghat.order.Order;
import com.hackinghat.util.RandomSource;
import com.hackinghat.util.RandomSourceImpl;
import com.hackinghat.util.TimeMachine;
import com.hackinghat.util.mbean.MBeanType;

import java.time.Duration;

@MBeanType(description = "Agent Impl Test")
public class AgentImplTest extends Agent
{
    public AgentImplTest(final Instrument instrument, final RandomSource randomSource, Long id)
    {
        super(id, instrument, randomSource, new TimeMachine(), "TestAgent #" + id, null, false);
    }

    public AgentImplTest(final Instrument instrument, final Long id)
    {
        this(instrument, new RandomSourceImpl(0L), id);
    }

    public Order getOrder(final String clientId)
    {
        for (Order order : getOutstandingOrders())
        {
            if (order.getClientId().equals(clientId))
                return order;
        }
        return null;
    }

    @Override
    public Duration wakeUp() {
        return Duration.ZERO;
    }

    @Override
    protected void doActions() {

    }
}
