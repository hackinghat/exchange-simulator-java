package com.hackinghat.orderbook;

import com.hackinghat.model.Level;
import com.hackinghat.order.OrderSide;

import java.math.BigDecimal;

import static com.hackinghat.util.Formatters.PRICE_FORMAT;
import static com.hackinghat.util.Formatters.QUANTITY_FORMAT;

public class OrderInterest implements Cloneable, Comparable<OrderInterest>
{
    final static OrderInterest EMPTY = new OrderInterest(null, null, 0L);

    private final OrderSide  side;
    private final Level      level;
    private long             quantity;
    private int              count;

    public OrderInterest(OrderSide side, Level level, long quantity)
    {
        this(side, level, quantity, 0);
    }

    public OrderInterest(OrderSide side, Level level, long quantity, int count)
    {
        this.side = side;
        this.quantity = quantity;
        this.level = level;
        this.count = count;
    }

    OrderInterest getTouchInterest(final OrderInterest marketInterest)
    {
        //Nothing to merge,
        if (getLevel().isMarket())
            return marketInterest;

        if (!marketInterest.getLevel().isMarket())
            throw new IllegalArgumentException("Limit interest provided, expected market interest: " + marketInterest);

        return new OrderInterest(this.side, this.level, this.quantity + marketInterest.getQuantity(), this.count + marketInterest.getCount());
    }

    @Override
    public String toString() {

        return "OrderInterest{" +
                QUANTITY_FORMAT.get().format(quantity) + "@" +
                PRICE_FORMAT.get().format(level.getPrice()) + '}';
    }

    public String formatInterest()
    {
        switch (side)
        {
            case BUY:
                return "(" + count  + ")" + QUANTITY_FORMAT.get().format(quantity) +  " " + PRICE_FORMAT.get().format(level.getPrice());
            case SELL:
                return PRICE_FORMAT.get().format(level.getPrice()) + " " + QUANTITY_FORMAT.get().format(quantity) + "(" + count + ")";
            default:
                throw new IllegalArgumentException("Unrecognised state");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderInterest)) return false;

        OrderInterest that = (OrderInterest) o;

        if (level != that.level) return false;
        if (quantity != that.quantity) return false;
        if (count != that.count) return false;
        return side == that.side;
    }

    public OrderInterest copy() {
        try
        {
            return (OrderInterest)this.clone();
        }
        catch (final CloneNotSupportedException noclone)
        {
            // Can't happen throw a fake runtime
            throw new IllegalArgumentException("Clone not supported");
        }
    }

    @Override
    public int hashCode() {
        int result = side != null ? side.hashCode() : 0;
        result = 31 * result + level.hashCode();
        result = 31 * result + (int) (quantity ^ (quantity >>> 32));
        result = 31 * result + count;
        return result;
    }

    public long getQuantity() {
        return quantity;
    }

    /**
     * Get the value of the interest, market orders return zero value
     * @param marketPrice the current market price
     * @return the value of the interest
     */
    public BigDecimal getValue(final BigDecimal marketPrice)
    {
        return (level.isMarket() ? marketPrice : level.getPrice()).multiply(new BigDecimal(quantity));
    }

    public Level getLevel() {
        return level;
    }

    public int getCount() {
        return count;
    }

    public void add(Integer quantity)
    {
        if (this == EMPTY)
            throw new IllegalArgumentException("can't add to empty interest");
        this.quantity += quantity;
        this.count++;
        check(quantity);
    }

    public void reduce(Integer quantity)
    {
        if (this == EMPTY)
            throw new IllegalArgumentException("can't remove from empty interest");
        this.quantity -= quantity;
        check(quantity);
    }

    public void remove(Integer quantity)
    {
        if (quantity <= 0)
            throw new IllegalArgumentException("can't reduce interest by less or equal zero");
        if (this == EMPTY)
            throw new IllegalArgumentException("can't remove from empty interest");
        this.quantity -= quantity;
        this.count--;
        check(quantity);
    }

    private void check(Integer _quantity)
    {
        if (count < 0)
            throw new IllegalArgumentException("Order count on interest would be negative");
        if (quantity < 0)
            throw new IllegalArgumentException("Order quantity on interest would be negative: quantity = " + _quantity + ", prev quantity = " + (quantity + _quantity));
    }

    @Override
    public int compareTo(final OrderInterest o)
    {
        if (o.side != side)
            throw new IllegalArgumentException("Can't conpare bid and offer interests directly");
        return level.levelCompare(side, o.level);
    }
}
