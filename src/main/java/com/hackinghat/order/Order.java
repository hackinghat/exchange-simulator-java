package com.hackinghat.order;

import com.hackinghat.agent.Agent;
import com.hackinghat.model.Instrument;
import com.hackinghat.model.Level;
import com.hackinghat.orderbook.Level1;
import com.hackinghat.statistic.Statistic;
import com.hackinghat.util.Event;
import com.hackinghat.util.TimeMachine;

import java.time.LocalDateTime;
import java.util.Objects;

import static com.hackinghat.util.Formatters.PRICE_FORMAT;
import static com.hackinghat.util.Formatters.QUANTITY_FORMAT;


public class Order extends Event implements Statistic {
    private final Instrument instrument;
    private final OrderSide side;
    private final StringBuilder notes;

    private Long id;
    private String clientId;
    private int version;
    private OrderState state;
    private Level level;
    private Integer quantity;
    private Integer filledQuantity;
    private Level1 referencePrice;
    private Agent sender;

    /**
     * @param clientId    this is an identifier assigned by the creator of the order, it's use is primarily for tracing purposes
     * @param side        the side of the order from the point of view of the market participant placing the order
     * @param instrument  the instrument of the order
     * @param price       the price (not adjusted to a tick-level)
     * @param quantity    the quantity of the order
     * @param sender      the agent responsible
     * @param timeMachine the order keeps a reference to the time machine primarily because an order is an event
     */
    public Order(final String clientId, final OrderSide side, final Instrument instrument, final float price, final Integer quantity, final Agent sender, final TimeMachine timeMachine) {
        this(clientId, side, instrument, instrument.getLevel(price), quantity, sender, timeMachine);
        if (price < 0.f)
            throw new IllegalArgumentException("Price should be greater than or equal to zero");
    }

    public Order(final String clientId, final OrderSide side, final Instrument instrument, final Level level, final Integer quantity, final Agent sender, final TimeMachine timeMachine) {
        this(clientId, side, instrument, level, quantity, sender, timeMachine, true);
    }

    /**
     * This is the generic constructor required for when 'notify' needs to be false.  This is useful in situations where the order is being
     * created by an agent and it needs to build some aspects of the order externally before changing the state of the order (and hence
     * notifying itself).
     *
     * @param clientId    this is an identifier assigned by the creator of the order, it's use is primarily for tracing purposes
     * @param side        the side of the order from the point of view of the market participant placing the order
     * @param instrument  the instrument of the order
     * @param level       the price (not adjusted to a tick-level)
     * @param quantity    the quantity of the order
     * @param sender      the agent responsible
     * @param timeMachine the order keeps a reference to the time machine primarily because an order is an event
     * @param init        whether to initialise the order when it's constructed
     */
    public Order(final String clientId, final OrderSide side, final Instrument instrument, final Level level, final Integer quantity, final Agent sender, final TimeMachine timeMachine, final boolean init) {
        super(sender, timeMachine.toSimulationTime());
        if (instrument == null)
            throw new IllegalArgumentException("Instrument can not be null");
        if (side == null)
            throw new IllegalArgumentException("Side can not be null");
        if (quantity == null)
            throw new IllegalArgumentException("Quantity can not be null");
        if (quantity <= 0)
            throw new IllegalArgumentException("Quantity can not be zero");
        if (level == null)
            throw new IllegalArgumentException("Price can not be null");
        this.clientId = clientId;
        this.side = side;
        this.instrument = instrument;
        this.level = level;
        this.quantity = quantity;
        this.sender = sender;
        this.version = 0;
        this.filledQuantity = 0;
        this.notes = new StringBuilder();
        if (init)
            init(timeMachine);
    }

    public static String getStatisticNames() {
        return "\"T\",\"Day#\",\"Id\",\"ClientId\",\"Agent\",\"State\",\"Side\",\"Quantity\",\"Price\",\"RefBid\",\"RefOffer\",\"Notes\"";
    }

    public void init(final TimeMachine timeMachine) {
        if (version != 0)
            throw new IllegalArgumentException("Can not init an order that is not at version 0!");

        changeState(OrderState.PENDING_NEW, timeMachine.toSimulationTime());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClientId() {
        return this.clientId;
    }

    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    public Agent getSender() {
        return sender;
    }

    public void setSender(Agent sender) {
        this.sender = sender;
    }

    public OrderSide getSide() {
        return side;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public double getConsideration(final float lastTradedPrice) {
        return (double) lastTradedPrice * getRemainingQuantity();
    }

    public boolean hasLimitPrice() {
        return !level.isMarket();
    }

    public boolean isMarket() {
        return !hasLimitPrice();
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(final Level level) {
        this.level = level;
        this.state = OrderState.PENDING_REPLACE;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        this.state = OrderState.PENDING_REPLACE;
    }

    public Integer getRemainingQuantity() {
        return Math.max(0, quantity - filledQuantity);
    }

    public int getVersion() {
        return version;
    }

    public OrderState getState() {
        return state;
    }

    public void setState(final OrderState state) {
        if (OrderState.isTerminal(this.state))
            throw new IllegalArgumentException("Order is terminal");
        if (!OrderState.isPending(state))
            throw new IllegalArgumentException("Only pending states can be 'set', attempt to set: " + state);
        this.state = state;
    }

    public Integer getFilledQuantity() {
        return filledQuantity;
    }

    public Level1 getReferencePrice() {
        return referencePrice;
    }

    public void setReferencePrice(final Level1 referencePrice) {
        this.referencePrice = referencePrice;
    }

    /**
     * Given the state of the quantity and the other state information chooses
     * the most appropriate state for it.   This controls the transitions between states to a single point
     *
     * @param simulationTime the time of the change
     */
    public void resetState(final LocalDateTime simulationTime) {
        if (id == null)
            throw new NullPointerException("Order not identified by manager yet");

        if (state == OrderState.PENDING_CANCEL) {
            changeState(OrderState.CANCELLED, simulationTime);
        } else {
            if (!OrderState.isTerminal(state)) {
                if (filledQuantity == 0)
                    changeState(OrderState.NEW, simulationTime);
                else
                    changeState(filledQuantity < quantity ? OrderState.PARTIALLY_FILLED : OrderState.FILLED, simulationTime);
            }
        }
    }

    private void incrementVersion() {
        this.version++;
    }

    /**
     * Changes the state of the order and notifies and associated agent of the change, note that
     * this should only be called by tests, the OrderManager & in the constructor of this class.
     *
     * @param newState  the state that we require
     * @param timestamp the simulation time of the state change
     */
    void changeState(final OrderState newState, final LocalDateTime timestamp) {
        this.simulationTime = timestamp;
        this.state = newState;
        incrementVersion();
        notifyAgent();
    }

    /**
     * Notify the agent of the update using a cloned copy of the order this keeps the internal representation
     * of the order private to the current owner of the order
     */
    private void notifyAgent() {
        if (sender != null) {
            sender.orderUpdate((Order) this.copy());
        }
    }

    public void tooLate() {
        if (sender != null) {
            sender.tooLate((Order) copy());
        }
    }

    public void rejected(final String reason) {
        if (sender != null) {
            sender.rejected((Order) copy(), reason);
        }
    }

    public void fillQuantity(final Integer filledQuantity, final Level price, final LocalDateTime simulationTime) {
        this.filledQuantity += filledQuantity;
        resetState(simulationTime);
        if (sender != null) {
            sender.fill(this, filledQuantity, price);
        }
    }

    public boolean cancel(final LocalDateTime simulationTime) {
        return cancel(simulationTime, true);
    }

    /**
     * This method is called by the agent (to cancel its own order) and by the order manager when it wants
     * to forcibly cancel an agent's order.  In the later case the order manager will change it straight
     * to cancelled (without passing through the intermediate pending cancel state).
     *
     * @param simulationTime the simulation time of the cancellation
     * @param agent          whether the caller is the owning agent or not
     * @return true if the state was changed
     */
    public boolean cancel(final LocalDateTime simulationTime, boolean agent) {
        if (!OrderState.isTerminal(state)) {
            changeState(agent ? OrderState.PENDING_CANCEL : OrderState.CANCELLED, simulationTime);
            return true;
        }
        return false;
    }

    public boolean replace(final Level price, final Integer quantity, final LocalDateTime simulationTimestamp) {
        // No change
        if (price == null && quantity == null)
            return false;

        if (OrderState.isTerminal(state))
            return false;

        if (price != null && !this.level.equals(price)) {
            setLevel(price);
        }
        if (quantity != null && !this.quantity.equals(quantity)) {
            setQuantity(quantity);
        }
        changeState(OrderState.PENDING_REPLACE, simulationTimestamp);
        return true;
    }

    /**
     * Two orders are equal if their client ids match.
     *
     * @param o to compare
     * @return true if the client id equals the client id of the other
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof Order)) return false;

        Order order = (Order) o;
        if (clientId == null || order.clientId == null)
            return false;
        return clientId.equals(order.clientId);
    }

    public void addNote(final String note) {
        notes.append(note);
    }

    public String formatStatistic(final TimeMachine timeMachine) {
        final StringBuilder builder = new StringBuilder();
        formatTime(builder, timeMachine, getTimestamp(), false);
        format(builder, null, timeMachine.getStartCount(), false);
        format(builder, null, getId(), false);
        formatString(builder, getClientId(), false);
        if (getSender() != null)
            formatString(builder, getSender().getName(), false);
        format(builder, null, getState(), false);
        format(builder, null, getSide(), false);
        format(builder, null, getQuantity(), false);
        formatPrice(builder, getLevel().getPrice(), false);

        final float referenceBid = getReferencePrice() == null ? 0.f : referencePrice.getBid().getLevel().getPrice();
        final float referenceOffer = getReferencePrice() == null ? 0.f : referencePrice.getOffer().getLevel().getPrice();
        formatPrice(builder, referenceBid, false);
        formatPrice(builder, referenceOffer, false);
        formatString(builder, notes.toString(), true);
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return clientId == null ? 0 : clientId.hashCode();
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", clientId=" + clientId +
                ", version=" + version +
                ", side=" + side +
                ", state=" + state +
                ", price=" + (level.isMarket() ? "Mkt" : PRICE_FORMAT.get().format(level.getPrice())) +
                ", remaining=" + QUANTITY_FORMAT.get().format(getRemainingQuantity()) +
                ", sender=" + (sender == null ? "Unknown" : sender.getName()) +
                ", timestamp=" + simulationTime +
                '}';
    }

    public int compareTo(final Order other) {
        Objects.requireNonNull(other);
        if (other.getSide() != getSide())
            throw new IllegalArgumentException("Can't compare buys to sells!");
        int price = Level.comparator(getSide()).compare(getLevel(), other.getLevel());
        return price == 0 ? super.compareTo(other) : price;
    }
}
