package com.hackinghat.agent;


import com.hackinghat.model.Instrument;
import com.hackinghat.model.Level;
import com.hackinghat.order.Order;
import com.hackinghat.order.OrderState;
import com.hackinghat.statistic.Statistic;
import com.hackinghat.util.*;
import com.hackinghat.util.component.AbstractComponent;
import com.hackinghat.util.mbean.MBeanAttribute;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ScheduledFuture;

public abstract class Agent extends AbstractComponent implements Nameable, Identifiable<Long>, Runnable, Statistic
{
    private static final Logger LOG = LogManager.getLogger(Agent.class);
    protected final Object                sync;

    private double                        initialCash;
    private int                           initialShares;

    private final Long                    id;
    private final String                  name;
    private final boolean                 canBeOverdrawn;
    private ScheduledFuture<?>            future;
    private boolean                       first;
    private double                        cash;
    private int                           shares;
    private boolean                       overdrawn;

    protected int                         tooLateCount;
    protected int                         rejectedCount;
    protected int                         newOrderCount;
    protected int                         cancelCount;
    protected int                         amendCount;
    protected int                         fillCount;

    protected final Set<Order>                  outstandingOrders;
    protected final RandomSource                randomSource;
    protected final ThreadLocalFormat<DecimalFormat> decimalFormatThread;
    protected final TimeMachine                 timeMachine;
    protected final Instrument                  instrument;
    protected final EventDispatcher             dispatcher;

    public Agent(final Long id, final Instrument instrument, final RandomSource randomSource, final TimeMachine timeMachine, final String name, final EventDispatcher dispatcher, final boolean canBeOverdrawn)
    {
        super(name);
        this.id = id;
        this.name = name;
        this.future = null;
        this.first = true;
        this.outstandingOrders = Collections.synchronizedSet(new  HashSet<>());
        this.sync = new Object();
        this.randomSource = randomSource;
        this.decimalFormatThread = new ThreadLocalFormat<>(DecimalFormat.class, "#,##0.##");
        this.timeMachine = timeMachine;
        this.instrument = instrument;
        this.dispatcher = require(dispatcher);
        this.overdrawn = false;
        this.canBeOverdrawn = canBeOverdrawn;
        this.tooLateCount = 0;
        this.rejectedCount = 0;
        this.newOrderCount = 0;
        this.amendCount = 0;
        this.cancelCount = 0;
        this.fillCount = 0;
    }

    @Override
    @MBeanAttribute(description = "Id")
    public Long getId() {
        return id;
    }

    public ScheduledFuture<?> getFuture() {
        return future;
    }

    @MBeanAttribute(description = "Cash")
    public double getCash() {
        return cash;
    }

    @MBeanAttribute(description = "Shares")
    public int getShares() {
        return shares;
    }

    @MBeanAttribute(description = "Too Late")
    public int getTooLateCount() { return tooLateCount; }

    @MBeanAttribute(description = "Rejected")
    public int getRejectedCount() { return rejectedCount; }

    @MBeanAttribute(description = "Filled")
    public int getFillCount() { return fillCount; }

    @MBeanAttribute(description = "New Count")
    public int getNewOrderCount() { return newOrderCount; }

    @MBeanAttribute(description = "Cancelled")
    public int getCancelCount() { return cancelCount; }

    @MBeanAttribute(description = "Amended")
    public int getAmendCount() { return amendCount; }

    public Pair<Double, Integer> getBalance() { synchronized(sync) {
        return Pair.instanceOf(cash, shares);
    }}

    public void setBalances(double cash, int shares)
    {
        synchronized (sync)
        {
            this.initialCash = cash;
            this.initialShares = shares;
            this.cash = cash;
            this.shares = shares;
        }
    }

    public double cashChange()
    {
        return cash - initialCash;
    }

    public int sharesChange()
    {
        return shares - initialShares;
    }


    public void fill(final Order order, final int quantity, final Level price)
    {
        synchronized (sync)
        {
            fillCount++;
            switch (order.getSide())
            {
                case BUY:
                    shares += quantity;
                    cash -= price.getPrice().doubleValue()*quantity;
                    break;
                case SELL:
                    shares -= quantity;
                    cash += price.getPrice().doubleValue()*quantity;
                    break;
            }
            if (canBeOverdrawn && (cash <= 0 && shares <= 0))
            {
                overdrawn = true;
            }
            else if (initialCash / 4.0 > cash && initialShares / 4 > shares)
            {
                LOG.info("Agent " + getName() + " is down to 25% stake (cash & shares)");
            }
        }
    }

    /**
     * We want to collect all the parameters of the agents in a simulation so that we can analyse it later, some agents
     * aren't interesting to study though (like test ones) so we default to an unimplemented method.
     * @param timeMachine the current time
     * @return a string that represents information about the agent
     */
    public String formatStatistic(final TimeMachine timeMachine)
    {
        throw new UnsupportedOperationException();
    }

    public void orderUpdate(final Order orderChanged)
    {
        synchronized (sync)
        {
            assert (orderChanged.getSender() == this);
            if (OrderState.PENDING_NEW.equals(orderChanged.getState()) || outstandingOrders.remove(orderChanged))
            {
                if (!OrderState.isTerminal(orderChanged.getState()))
                {
                    outstandingOrders.add(orderChanged);
                }
                else
                {
                    switch (orderChanged.getState())
                    {
                        case FILLED:
                            if (LOG.isTraceEnabled())
                                LOG.trace("Filled: " + orderChanged);
                            break;
                        case CANCELLED:
                            if (LOG.isTraceEnabled())
                                LOG.trace("Cancelled: " + orderChanged);
                            break;

                    }
                }
            }
            else
            {
                throw new IllegalArgumentException(getName() + " received orderUpdate request for unknown order: " + orderChanged);
            }
        }
    }

    public void tooLate(final Order order)
    {
        tooLateCount++;
        if (LOG.isTraceEnabled())
            LOG.trace("Too late to cancel: " + order);
        if (outstandingOrders.remove(order))
            throw new IllegalArgumentException("Received too late notification on order that is still live");
    }

    public void rejected(final Order order, final String reason)
    {
        rejectedCount++;
        if (LOG.isTraceEnabled())
            LOG.trace("Order rejected, because: '" + reason + "', " + order);
        // It may be active it may not, it doesn't matter we'll just ignore it
        outstandingOrders.remove(order);
    }

    Collection<Order> getOutstandingOrders() {
        return outstandingOrders;
    }

    /**
     * This is mainly for use in tests where the presence of an order in the agent will be as a result of whether the
     * order manager has filled it or not
     * @param order the order to check
     * @return true if the order is currently outstanding
     */
    public boolean hasOutstandingOrder(final Order order) {
        return outstandingOrders.contains(order);
    }

    @Override
    public String getName() {
        return name;
    }

    public abstract Duration wakeUp();

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Agent)) return false;

        Agent agent = (Agent) o;

        return id.equals(agent.id);
    }

    public void schedule()
    {
        if (overdrawn)
        {
            LOG.info(getName() + " is overdrawn and can no longer continue (cash=" + cash + " /shares= " + shares + ")");
        }
        else
        {
            long wakeupTime = wakeUp().toNanos();
            if (LOG.isTraceEnabled())
                LOG.trace(getName() + " - wakeup in " + wakeupTime + "ms");
            future = dispatcher.schedule(this, timeMachine.simulationPeriodToWall(Duration.of(wakeupTime, ChronoUnit.NANOS), ChronoUnit.NANOS));
        }
    }

    protected abstract void doActions();

    @MBeanAttribute(description = "Outstanding orders")
    public int getOutstandingOrderCount()
    {
        return outstandingOrders.size();
    }

    @Override
    public void run()
    {
        try
        {
            if (!first)
            {
                if (LOG.isTraceEnabled()) LOG.trace(getName() + ": START");
                synchronized (sync)
                {
                    doActions();
                }
                if (LOG.isTraceEnabled()) LOG.trace(getName() + ": END");
            }
            first = false;
        }
        catch (final Exception ex)
        {
            // The fact that the agent messed itself is not very important to the simulation, it could probably
            // get added to a journal of agent's actions
            if (LOG.isTraceEnabled())
                LOG.trace(getName() + ": error processing actions: ", ex);
        }
        finally
        {
            schedule();
        }
    }

    @Override
    public String toString() {
        synchronized (sync)
        {
            return "Agent{" +
                    "name='" + name + "', cash=" + decimalFormatThread.get().format(getCash()) +
                     ", shares = " + getShares() + "}";

        }
    }
}

