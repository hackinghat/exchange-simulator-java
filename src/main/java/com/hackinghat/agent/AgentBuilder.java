package com.hackinghat.agent;

import com.hackinghat.agent.parameter.AgentParameterSet;
import com.hackinghat.model.Instrument;
import com.hackinghat.simulator.OrderBookSimulatorImpl;
import com.hackinghat.util.RandomSource;
import com.hackinghat.util.TimeMachine;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static com.hackinghat.agent.parameter.AgentParameterSet.P_INSPREAD;

/**
 * The agent builder is responsible for constructing parameterised market agents.  The agents are build according to a
 * specification for the parameters.  Some specifications may hold all parameters constant.
 * <p>
 * In this scenario the agents constructed may still all behave randomly but those parameters that define their behaviour
 * are constant and provide an upper-bound on behaviour.   For example all agents that view the buy/sell preference as 50%
 * should result in a stable price around the starting reference price.  However, due to the microstructure effects (that
 * we're trying to understand) this price may well drift.
 */
public class AgentBuilder {
    private final static AtomicLong AGENT_ID_GENERATOR = new AtomicLong(0L);
    private final int defaultShares;
    private final double defaultCash;
    private final double alpha;
    private final double sizeMean;
    private final double sizeSigma;
    private final RandomSource randomSource;
    private final Instrument instrument;

    public AgentBuilder(final Instrument instrument, final RandomSource randomSource, final double defaultCash, final int defaultShares, final double alpha, final double sizeMean, final double sizeSigma) {
        this.defaultCash = defaultCash;
        this.defaultShares = defaultShares;
        this.alpha = alpha;
        this.sizeMean = sizeMean;
        this.sizeSigma = sizeSigma;
        this.randomSource = randomSource;
        this.instrument = instrument;
    }

    public static ZeroIntelligenceAgent[] makeZeroIntelligenceAgents(final Instrument instrument, final RandomSource randomSource, final TimeMachine timeMachine, final AgentParameterSet agentParameterSet, final String nameStem, final int n, final Duration sleepT1, final Duration sleeptT2, final OrderBookSimulatorImpl simulator, final Consumer<ZeroIntelligenceAgent> fnSetBalances, final double pCancel, final double pMarket, double pBuy) {
        ZeroIntelligenceAgent[] agents = new ZeroIntelligenceAgent[n];
        for (int i = 0; i < n; ++i) {
            agents[i] = new ZeroIntelligenceAgent(AGENT_ID_GENERATOR.getAndIncrement(), instrument, randomSource, timeMachine, sleepT1, sleeptT2, nameStem + " #" + i, simulator, pCancel, pMarket, pBuy,
                    agentParameterSet.getParameter(P_INSPREAD));
            fnSetBalances.accept(agents[i]);
        }
        return agents;
    }

    private void setFromDefault(final ZeroIntelligenceAgent a) {
        a.setBalances(defaultCash, defaultShares);
        a.setAlpha(alpha);
        a.setSizeMean(sizeMean);
        a.setSizeSigma(sizeSigma);
    }

    public ZeroIntelligenceAgent[] makeZeroIntelligenceAgents(final TimeMachine timeMachine, final AgentParameterSet agentParameterSet, final String nameStem, final int n, final Duration sleepT1, final Duration sleepT2, final OrderBookSimulatorImpl simulator, final double pCancel, final double pMarket, final double pBuy) {
        return makeZeroIntelligenceAgents(instrument, randomSource, timeMachine, agentParameterSet, nameStem, n, sleepT1, sleepT2, simulator, this::setFromDefault, pCancel, pMarket, pBuy);
    }

    public ZeroIntelligenceMarketMaker makeMarketMaker(final TimeMachine timeMachine, final Duration sleepTime, final OrderBookSimulatorImpl simulator, final int nSpreadLevels, final int spreadTolerance, final int bidOrderSize, final int offerOrderSize, final boolean cancelIfTop) {
        return new ZeroIntelligenceMarketMaker(AGENT_ID_GENERATOR.getAndIncrement(), instrument, randomSource, timeMachine, sleepTime, "MM", simulator, nSpreadLevels, spreadTolerance, bidOrderSize, offerOrderSize, cancelIfTop);
    }
}
