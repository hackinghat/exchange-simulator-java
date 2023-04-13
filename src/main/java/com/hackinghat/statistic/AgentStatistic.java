package com.hackinghat.statistic;

import com.hackinghat.agent.Agent;
import com.hackinghat.util.Pair;
import com.hackinghat.util.TimeMachine;

import java.util.Collection;
import java.util.Iterator;

import static com.hackinghat.util.Formatters.SEPARATOR;

public class AgentStatistic implements SampledStatistic<Collection<Agent>> {

    private final Collection<Agent>   agents;
    private final TimeMachine         timeMachine;
    private String              last;

    public String getHeaders()
    {
        final StringBuilder builder = new StringBuilder();
        formatString(builder, "T", false);
        formatString(builder,"Day#", false);
        for (Agent next : agents) {
            formatString(builder, next.getName() + " Cash", false);
            formatString(builder, next.getName() + " Shares", false);
        }
        formatString(builder, "Total Agent Shares", true);
        return builder.toString();
    }

    public AgentStatistic(final TimeMachine timeMachine, final Collection<Agent> agents)
    {
        this.timeMachine = timeMachine;
        this.agents = agents;
    }

    @Override
    public String formatStatistic(final TimeMachine timeMachine) {
        return last;
    }

    @Override
    public void update(Collection<Agent> item) {
        assert(item == agents);

        final StringBuilder builder = new StringBuilder();
        builder.append(timeMachine.formatTime());
        builder.append(SEPARATOR);
        builder.append(timeMachine.getStartCount());
        builder.append(SEPARATOR);
        final Iterator<Agent> agentIterator = agents.iterator();
        long total = 0L;
        while (agentIterator.hasNext())
        {
            final Agent next = agentIterator.next();
            final Pair<Double, Integer> balance = next.getBalance();
            formatStatistic(builder, balance.getFirst(), false);
            formatStatistic(builder, balance.getSecond(), false);
            total += balance.getSecond();
        }
        formatStatistic(builder, total, true);
        last = builder.toString();
    }
}
