package com.hackinghat.simulator;

import com.hackinghat.agent.Agent;
import com.hackinghat.agent.AgentBuilder;
import com.hackinghat.agent.ZeroIntelligenceAgent;
import com.hackinghat.agent.parameter.AgentParameterSet;
import com.hackinghat.agent.parameter.ConstantAgentParameter;
import com.hackinghat.model.*;
import com.hackinghat.order.MarketState;
import com.hackinghat.order.Order;
import com.hackinghat.orderbook.FullDepth;
import com.hackinghat.orderbook.Level1;
import com.hackinghat.orderbook.OrderManager;
import com.hackinghat.orderbook.auction.AuctionSchedule;
import com.hackinghat.orderbook.auction.MarketManager;
import com.hackinghat.statistic.AgentStatistic;
import com.hackinghat.statistic.OrderBookStatistic;
import com.hackinghat.util.*;
import com.hackinghat.util.component.AbstractComponent;
import com.hackinghat.util.mbean.MBeanHolder;
import com.hackinghat.util.mbean.MBeanType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;

import static com.hackinghat.agent.parameter.AgentParameterSet.P_INSPREAD;

@MBeanType(description = "Order book simulator")
public class OrderBookSimulatorImpl extends AbstractComponent implements OrderBookSimulator {
    private static final Logger LOG = LogManager.getLogger(OrderBookSimulatorImpl.class);
    // The probability that the next action will be a market order
    private static final double P_MARKET = 0.15;
    // This defines the power law distribution for out of spread orders
    private static final double ALPHA = 1.4;
    // The preference for buys vs sells
    private static final double P_BUY = 0.5;
    // The time a participant will sleep for before evaluating its choices (seconds)
    private static final long MAX_SLEEP_TIME_T1 = 60;
    private static final long MAX_SLEEP_TIME_T2 = 180;
    // The total number of agents in the system
    private static final int N_AGENTS = 100;
    private static final int N_APPENDERS = 1;
    private static final int N_DISPATCHERS = 5;
    // The number of spread levels a market maker will try to maintain between bid & offer
    private static final int MM_N_SPREAD_LEVELS = 100;
    private static final int MM_SPREAD_TOLERANCE = 10;
    private static final int MM_QUANTITY = 1000;
    private static final boolean MM_CANCEL_IF_TOP = false;
    private final static Duration DEFAULT_MARKET_DATA_DELAY = Duration.of(100L, ChronoUnit.MILLIS);
    // The probability that the next action will be a cancel
    private static double P_CANCEL = 0.5;
    private final Instrument instrument;
    private final OrderManager manager;
    private final MarketManager marketManager;
    private final AgentBuilder agentBuilder;
    private final EventDispatcher eventDispatcher;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService cachedThreadPool;
    private final Collection<Agent> agentSet;
    private final AbstractStatisticsAppender orderStatsAppender;
    private final AbstractStatisticsAppender tape;
    private final TimeMachine timeMachine;
    private final Instant startTime;
    private final ArrayList<MBeanHolder> mbeans;
    private Future<?> managerFuture;
    private AbstractStatisticsAppender agentStatisticAppender;
    private AbstractStatisticsAppender agentDescription;

    /**
     * Create a new simulator
     *
     * @param instrument      the instrument we are running the market simulation for
     * @param marketManager   the manager responsible for running auctions
     * @param eventDispatcher event propagation dispatcher
     * @param randomSource    source of randomness (or non-randomness for tests)
     * @param timeMachine     a converter of wall-clock time to simulation time
     */
    public OrderBookSimulatorImpl(final Instrument instrument, final MarketManager marketManager, final EventDispatcher eventDispatcher, final RandomSource randomSource, final TimeMachine timeMachine, final Duration marketDataDelay, final ScheduledExecutorService scheduler, boolean publish) {
        super("OrderBookSimulator");
        this.mbeans = new ArrayList<>();
        this.cachedThreadPool = Executors.newCachedThreadPool();
        this.scheduler = scheduler;
        this.instrument = instrument;
        this.timeMachine = timeMachine;
        this.eventDispatcher = require(eventDispatcher);
        this.marketManager = require(marketManager);
        this.startTime = Instant.now();
        this.tape = new FileStatisticsAppender<Trade>(Trade::getStatisticNames, startTime, instrument.getTicker() + "-TRADE");
        if (publish) {
            this.orderStatsAppender = new FileStatisticsAppender<Order>(Order::getStatisticNames, startTime, instrument.getTicker() + "-ORDER");
        } else {
            this.orderStatsAppender = null;
        }
        this.manager = require(new OrderManager(marketManager, timeMachine, instrument.getLevel(100.0f), MarketState.CLOSED, instrument, eventDispatcher, tape, orderStatsAppender, marketDataDelay));
        this.agentBuilder = new AgentBuilder(instrument, randomSource, 100000.0, 1000, ALPHA, 4.5, 0.8);
        this.agentSet = new ArrayList<>();
    }

    public OrderBookSimulatorImpl(final Instrument instrument, final MarketManager marketManager, final EventDispatcher eventDispatcher, final RandomSource randomSource, final TimeMachine timeMachine, final ScheduledExecutorService scheduler) {
        this(instrument, marketManager, eventDispatcher, randomSource, timeMachine, DEFAULT_MARKET_DATA_DELAY, scheduler, true);
    }

    public static void main(String[] args) {
        final Instrument VOD = new Instrument("VOD", "Vodafone Plc", new Currency("GBP"), new ConstantTickSizeToLevelConverter(2, 100, 3));
        final RandomSource randomSource = new RandomSourceImpl(2L);
        final TimeMachine timeMachine = new TimeMachine(LocalTime.of(7, 54, 0), 60.0);
        final ScheduledExecutorService dispatcherScheduler = Executors.newScheduledThreadPool(N_DISPATCHERS);
        final EventDispatcher dispatcher = new AsyncEventDispatcher(dispatcherScheduler, timeMachine);
        final Level referenceLevel = VOD.getLevel(100.0f);
        final MarketManager marketManager = new MarketManager(referenceLevel, 0.1, Duration.of(5L, ChronoUnit.MINUTES), timeMachine, dispatcher, AuctionSchedule.makeLSESchedule(LocalDate.now()));
        try (OrderBookSimulatorImpl orderBookSimulator = new OrderBookSimulatorImpl(VOD, marketManager, dispatcher, randomSource, timeMachine, dispatcherScheduler)) {
            orderBookSimulator.start(randomSource).get();
            LOG.info("Shutting down");
            dispatcherScheduler.shutdownNow();
            if (!dispatcherScheduler.awaitTermination(10000, TimeUnit.MILLISECONDS)) {
                LOG.warn("Scheduler didn't shutdown cleanly, timeout reached");
            }
            orderBookSimulator.agentSummary();
            orderBookSimulator.shutdown();
        } catch (InterruptedException ignored) {
        } catch (Exception e) {
            LOG.error("Unexpected problem running simulator", e);
        }
    }

    @Override
    public void close() {
        super.close();
        cachedThreadPool.shutdownNow();
        try {
            if (!cachedThreadPool.awaitTermination(2L, TimeUnit.SECONDS)) {
                LOG.warn("One of the order manager processes isn't shutting down cleanly timeout reached");
            }
        } catch (final InterruptedException iex) {
            LOG.error("Appender pool terminated", iex);
        }
        LOG.info("Appenders stopped");
        for (Closeable c : Set.of(tape, orderStatsAppender, agentDescription, agentStatisticAppender)) {
            if (c != null) {
                try {
                    c.close();
                } catch (final IOException e) {
                    LOG.error("Couldn't close: " + c.getClass().getSimpleName(), e);
                }
            }
        }
        manager.close();
    }

    public double getPCancel() {
        return P_CANCEL;
    }

    public void setPCancel(final double pCancel) {
        LOG.debug("Cancel probability changed from " + P_CANCEL + " to " + pCancel);
        P_CANCEL = pCancel;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public void add(final Collection<Order> orders) {
        orders.forEach(manager::add);
    }

    public void add(final Order... orders) {
        manager.add(orders);
    }

    public int sizePending() {
        return manager.sizePending();
    }

    public Level1 getLevel1() {
        return manager.getLevel1();
    }

    public FullDepth getFullDepth() {
        return manager.getFullDepth();
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    private void agentSummary() {
        LOG.debug("Agents:");
        for (Agent agent : agentSet) {
            LOG.debug(agent.toString());
        }
    }

    public void process() {
        if (managerFuture != null)
            throw new IllegalArgumentException("Can't call process when manager thread is running");
        manager.process();
    }

    /**
     * The simulator shares its event dispatcher so that agents running in the same JVM don't also need a way of scheduling
     * their own tasks independently of the rest of the system.
     *
     * @return the simulator's event dispatcher
     */
    public EventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }

    public Level getReferencePrice() {
        return manager.getReferencePrice();
    }

    public AgentParameterSet makeParameterSet(final RandomSource randomSource) {
        return new AgentParameterSet(
                // The next action will be a 'limit' order with probability = 1-(P_CANCEL+P_MARKET), this is the probability that
                // this limit is within the current spread or outside it
                ConstantAgentParameter.of(P_INSPREAD, 0.2)
                //new UniformAgentParameter<>(P_INSPREAD, Double.class, randomSource, 0.01, 0.2)
        );
    }

    private void scheduleWithFixedDelay(final Runnable runnable, final long initialPeriod, final long repeatPeriod) {
        Objects.requireNonNull(runnable);
        if (scheduler != null) {
            scheduler.scheduleWithFixedDelay(runnable, initialPeriod, repeatPeriod, TimeUnit.NANOSECONDS);
        }

    }

    public void configureAgents(final RandomSource randomSource) {
        final AgentParameterSet agentParameterSet = makeParameterSet(randomSource);
        for (final Agent za : agentBuilder.makeZeroIntelligenceAgents(timeMachine, agentParameterSet, "ZERO", N_AGENTS, Duration.of(MAX_SLEEP_TIME_T1, ChronoUnit.SECONDS), Duration.of(MAX_SLEEP_TIME_T2, ChronoUnit.SECONDS), this, P_CANCEL, P_MARKET, P_BUY)) {
            za.run();
            agentSet.add(za);
        }
        // We only need a single market-maker agent, at least for now
        final Agent mmAgent = agentBuilder.makeMarketMaker(timeMachine, Duration.of(MAX_SLEEP_TIME_T1, ChronoUnit.SECONDS), this, MM_N_SPREAD_LEVELS, MM_SPREAD_TOLERANCE, MM_QUANTITY, MM_QUANTITY, MM_CANCEL_IF_TOP);
        mmAgent.run();
        agentSet.add(mmAgent);
        final AgentStatistic agentStatistic = new AgentStatistic(timeMachine, agentSet);
        agentStatisticAppender = new SamplingStatisticAppender<>(timeMachine, startTime, agentStatistic, () -> agentSet, agentStatistic::getHeaders, "AGENT");
        scheduleWithFixedDelay(agentStatisticAppender, 0, timeMachine.simulationPeriodToWall(Duration.of(1, ChronoUnit.MINUTES), ChronoUnit.NANOS));
        agentDescription = new FileStatisticsAppender<>(ZeroIntelligenceAgent::getHeaders, startTime, "AGENT-ZERO");
        scheduleWithFixedDelay(() -> {
            agentSet.forEach(a -> {
                if (a instanceof ZeroIntelligenceAgent)
                    agentDescription.append(timeMachine, a);
            });
            agentDescription.run();
        }, 0, timeMachine.simulationPeriodToWall(Duration.of(1, ChronoUnit.MINUTES), ChronoUnit.NANOS));
    }

    private Future<?> start(final RandomSource randomSource) throws InterruptedException {
        Objects.requireNonNull(startTime);

        // Although this doesn't use any resources it fixes the relative point at which all other times will be based
        timeMachine.start(startTime);

        // We want to pre-process any events that may already exist prior to starting the manager
        manager.preProcess();

        // Prepare the auction schedule items and actually schedule them
        marketManager.start();

        // Now begin the directly managed threads
        managerFuture = cachedThreadPool.submit(manager);
        cachedThreadPool.execute(orderStatsAppender);
        cachedThreadPool.execute(tape);

        final long durationOneSecond = timeMachine.simulationPeriodToWall(Duration.of(1L, ChronoUnit.SECONDS), ChronoUnit.NANOS);
        final OrderBookStatistic orderBookStatistic = new OrderBookStatistic(10, 10);
        final SamplingStatisticAppender<Pair<Level1, FullDepth>, OrderBookStatistic> samplingStatisticAppender = new SamplingStatisticAppender<>(timeMachine, startTime, orderBookStatistic,
                () -> Pair.instanceOf(manager.getLevel1(), manager.getFullDepth()), orderBookStatistic::getHeaders, "SAMPLE");
        scheduleWithFixedDelay(samplingStatisticAppender, durationOneSecond, durationOneSecond);
        configureAgents(randomSource);
        return managerFuture;
    }
}

