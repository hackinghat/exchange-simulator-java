package com.hackinghat.fix;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.ConfigError;
import quickfix.DataDictionaryProvider;
import quickfix.DoNotSend;
import quickfix.FieldConvertError;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.LogUtil;
import quickfix.Message;
import quickfix.MessageCracker;
import quickfix.MessageUtils;
import quickfix.RejectLogon;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.SessionSettings;
import quickfix.UnsupportedMessageType;
import quickfix.examples.executor.MarketDataProvider;
import quickfix.field.ApplVerID;
import quickfix.field.AvgPx;
import quickfix.field.CumQty;
import quickfix.field.ExecID;
import quickfix.field.ExecTransType;
import quickfix.field.ExecType;
import quickfix.field.LastPx;
import quickfix.field.LastQty;
import quickfix.field.LastShares;
import quickfix.field.LeavesQty;
import quickfix.field.OrdStatus;
import quickfix.field.OrdType;
import quickfix.field.OrderID;
import quickfix.field.OrderQty;
import quickfix.field.Price;
import quickfix.fix40.ExecutionReport;
import quickfix.fix40.NewOrderSingle;

public class FixApplication extends MessageCracker implements quickfix.Application {
    private static final String DEFAULT_MARKET_PRICE_KEY = "DefaultMarketPrice";
    private static final String ALWAYS_FILL_LIMIT_KEY = "AlwaysFillLimitOrders";
    private static final String VALID_ORDER_TYPES_KEY = "ValidOrderTypes";
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final boolean alwaysFillLimitOrders;
    private final HashSet<String> validOrderTypes = new HashSet();
    private MarketDataProvider marketDataProvider;
    private int m_orderID = 0;
    private int m_execID = 0;

    public FixApplication(SessionSettings settings) throws ConfigError, FieldConvertError {
        this.initializeValidOrderTypes(settings);
        this.initializeMarketDataProvider(settings);
        this.alwaysFillLimitOrders = settings.isSetting("AlwaysFillLimitOrders") && settings.getBool("AlwaysFillLimitOrders");
    }

    private void initializeMarketDataProvider(SessionSettings settings) throws ConfigError, FieldConvertError {
        if (settings.isSetting("DefaultMarketPrice")) {
            if (this.marketDataProvider == null) {
                final double defaultMarketPrice = settings.getDouble("DefaultMarketPrice");
                this.marketDataProvider = new MarketDataProvider() {
                    public double getAsk(String symbol) {
                        return defaultMarketPrice;
                    }

                    public double getBid(String symbol) {
                        return defaultMarketPrice;
                    }
                };
            } else {
                this.log.warn("Ignoring {} since provider is already defined.", "DefaultMarketPrice");
            }
        }

    }

    private void initializeValidOrderTypes(SessionSettings settings) throws ConfigError, FieldConvertError {
        if (settings.isSetting("ValidOrderTypes")) {
            List<String> orderTypes = Arrays.asList(settings.getString("ValidOrderTypes").trim().split("\\s*,\\s*"));
            this.validOrderTypes.addAll(orderTypes);
        } else {
            this.validOrderTypes.add("2");
        }

    }

    public void onCreate(SessionID sessionID) {
        Session.lookupSession(sessionID).getLog().onEvent("Valid order types: " + this.validOrderTypes);
    }

    public void onLogon(SessionID sessionID) {
    }

    public void onLogout(SessionID sessionID) {
    }

    public void toAdmin(Message message, SessionID sessionID) {
    }

    public void toApp(Message message, SessionID sessionID) throws DoNotSend {
    }

    public void fromAdmin(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
    }

    public void fromApp(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        this.crack(message, sessionID);
    }

    public void onMessage(NewOrderSingle order, SessionID sessionID) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        try {
            this.validateOrder(order);
            OrderQty orderQty = order.getOrderQty();
            Price price = this.getPrice(order);
            ExecutionReport accept = new ExecutionReport(this.genOrderID(), this.genExecID(), new ExecTransType('0'), new OrdStatus('0'), order.getSymbol(), order.getSide(), orderQty, new LastShares(0.0D), new LastPx(0.0D), new CumQty(0.0D), new AvgPx(0.0D));
            accept.set(order.getClOrdID());
            this.sendMessage(sessionID, accept);
            if (this.isOrderExecutable(order, price)) {
                ExecutionReport fill = new ExecutionReport(this.genOrderID(), this.genExecID(), new ExecTransType('0'), new OrdStatus('2'), order.getSymbol(), order.getSide(), orderQty, new LastShares(orderQty.getValue()), new LastPx(price.getValue()), new CumQty(orderQty.getValue()), new AvgPx(price.getValue()));
                fill.set(order.getClOrdID());
                this.sendMessage(sessionID, fill);
            }
        } catch (RuntimeException var7) {
            LogUtil.logThrowable(sessionID, var7.getMessage(), var7);
        }

    }

    private boolean isOrderExecutable(Message order, Price price) throws FieldNotFound {
        if (order.getChar(40) != '2') {
            return true;
        } else {
            BigDecimal limitPrice = new BigDecimal(order.getString(44));
            char side = order.getChar(54);
            BigDecimal thePrice = new BigDecimal("" + price.getValue());
            return side == '1' && thePrice.compareTo(limitPrice) <= 0 || (side == '2' || side == '5') && thePrice.compareTo(limitPrice) >= 0;
        }
    }

    private Price getPrice(Message message) throws FieldNotFound {
        Price price;
        if (message.getChar(40) == '2' && this.alwaysFillLimitOrders) {
            price = new Price(message.getDouble(44));
        } else {
            if (this.marketDataProvider == null) {
                throw new RuntimeException("No market data provider specified for market order");
            }

            char side = message.getChar(54);
            if (side == '1') {
                price = new Price(this.marketDataProvider.getAsk(message.getString(55)));
            } else {
                if (side != '2' && side != '5') {
                    throw new RuntimeException("Invalid order side: " + side);
                }

                price = new Price(this.marketDataProvider.getBid(message.getString(55)));
            }
        }

        return price;
    }

    private void sendMessage(SessionID sessionID, Message message) {
        try {
            Session session = Session.lookupSession(sessionID);
            if (session == null) {
                throw new SessionNotFound(sessionID.toString());
            }

            DataDictionaryProvider dataDictionaryProvider = session.getDataDictionaryProvider();
            if (dataDictionaryProvider != null) {
                try {
                    dataDictionaryProvider.getApplicationDataDictionary(this.getApplVerID(session, message)).validate(message, true);
                } catch (Exception var6) {
                    LogUtil.logThrowable(sessionID, "Outgoing message failed validation: " + var6.getMessage(), var6);
                    return;
                }
            }

            session.send(message);
        } catch (SessionNotFound var7) {
            this.log.error(var7.getMessage(), var7);
        }

    }

    private ApplVerID getApplVerID(Session session, Message message) {
        String beginString = session.getSessionID().getBeginString();
        return "FIXT.1.1".equals(beginString) ? new ApplVerID("7") : MessageUtils.toApplVerID(beginString);
    }

    public void onMessage(quickfix.fix41.NewOrderSingle order, SessionID sessionID) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        try {
            this.validateOrder(order);
            OrderQty orderQty = order.getOrderQty();
            Price price = this.getPrice(order);
            quickfix.fix41.ExecutionReport accept = new quickfix.fix41.ExecutionReport(this.genOrderID(), this.genExecID(), new ExecTransType('0'), new ExecType('2'), new OrdStatus('0'), order.getSymbol(), order.getSide(), orderQty, new LastShares(0.0D), new LastPx(0.0D), new LeavesQty(0.0D), new CumQty(0.0D), new AvgPx(0.0D));
            accept.set(order.getClOrdID());
            this.sendMessage(sessionID, accept);
            if (this.isOrderExecutable(order, price)) {
                quickfix.fix41.ExecutionReport executionReport = new quickfix.fix41.ExecutionReport(this.genOrderID(), this.genExecID(), new ExecTransType('0'), new ExecType('2'), new OrdStatus('2'), order.getSymbol(), order.getSide(), orderQty, new LastShares(orderQty.getValue()), new LastPx(price.getValue()), new LeavesQty(0.0D), new CumQty(orderQty.getValue()), new AvgPx(price.getValue()));
                executionReport.set(order.getClOrdID());
                this.sendMessage(sessionID, executionReport);
            }
        } catch (RuntimeException var7) {
            LogUtil.logThrowable(sessionID, var7.getMessage(), var7);
        }

    }

    public void onMessage(quickfix.fix42.NewOrderSingle order, SessionID sessionID) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        try {
            this.validateOrder(order);
            OrderQty orderQty = order.getOrderQty();
            Price price = this.getPrice(order);
            quickfix.fix42.ExecutionReport accept = new quickfix.fix42.ExecutionReport(this.genOrderID(), this.genExecID(), new ExecTransType('0'), new ExecType('2'), new OrdStatus('0'), order.getSymbol(), order.getSide(), new LeavesQty(0.0D), new CumQty(0.0D), new AvgPx(0.0D));
            accept.set(order.getClOrdID());
            this.sendMessage(sessionID, accept);
            if (this.isOrderExecutable(order, price)) {
                quickfix.fix42.ExecutionReport executionReport = new quickfix.fix42.ExecutionReport(this.genOrderID(), this.genExecID(), new ExecTransType('0'), new ExecType('2'), new OrdStatus('2'), order.getSymbol(), order.getSide(), new LeavesQty(0.0D), new CumQty(orderQty.getValue()), new AvgPx(price.getValue()));
                executionReport.set(order.getClOrdID());
                executionReport.set(orderQty);
                executionReport.set(new LastShares(orderQty.getValue()));
                executionReport.set(new LastPx(price.getValue()));
                this.sendMessage(sessionID, executionReport);
            }
        } catch (RuntimeException var7) {
            LogUtil.logThrowable(sessionID, var7.getMessage(), var7);
        }

    }

    private void validateOrder(Message order) throws IncorrectTagValue, FieldNotFound {
        OrdType ordType = new OrdType(order.getChar(40));
        if (!this.validOrderTypes.contains(Character.toString(ordType.getValue()))) {
            this.log.error("Order type not in ValidOrderTypes setting");
            throw new IncorrectTagValue(ordType.getField());
        } else if (ordType.getValue() == '1' && this.marketDataProvider == null) {
            this.log.error("DefaultMarketPrice setting not specified for market order");
            throw new IncorrectTagValue(ordType.getField());
        }
    }

    public void onMessage(quickfix.fix43.NewOrderSingle order, SessionID sessionID) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        try {
            this.validateOrder(order);
            OrderQty orderQty = order.getOrderQty();
            Price price = this.getPrice(order);
            quickfix.fix43.ExecutionReport accept = new quickfix.fix43.ExecutionReport(this.genOrderID(), this.genExecID(), new ExecType('2'), new OrdStatus('0'), order.getSide(), new LeavesQty(order.getOrderQty().getValue()), new CumQty(0.0D), new AvgPx(0.0D));
            accept.set(order.getClOrdID());
            accept.set(order.getSymbol());
            this.sendMessage(sessionID, accept);
            if (this.isOrderExecutable(order, price)) {
                quickfix.fix43.ExecutionReport executionReport = new quickfix.fix43.ExecutionReport(this.genOrderID(), this.genExecID(), new ExecType('2'), new OrdStatus('2'), order.getSide(), new LeavesQty(0.0D), new CumQty(orderQty.getValue()), new AvgPx(price.getValue()));
                executionReport.set(order.getClOrdID());
                executionReport.set(order.getSymbol());
                executionReport.set(orderQty);
                executionReport.set(new LastQty(orderQty.getValue()));
                executionReport.set(new LastPx(price.getValue()));
                this.sendMessage(sessionID, executionReport);
            }
        } catch (RuntimeException var7) {
            LogUtil.logThrowable(sessionID, var7.getMessage(), var7);
        }

    }

    public void onMessage(quickfix.fix44.NewOrderSingle order, SessionID sessionID) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        try {
            this.validateOrder(order);
            OrderQty orderQty = order.getOrderQty();
            Price price = this.getPrice(order);
            quickfix.fix44.ExecutionReport accept = new quickfix.fix44.ExecutionReport(this.genOrderID(), this.genExecID(), new ExecType('2'), new OrdStatus('0'), order.getSide(), new LeavesQty(order.getOrderQty().getValue()), new CumQty(0.0D), new AvgPx(0.0D));
            accept.set(order.getClOrdID());
            accept.set(order.getSymbol());
            this.sendMessage(sessionID, accept);
            if (this.isOrderExecutable(order, price)) {
                quickfix.fix44.ExecutionReport executionReport = new quickfix.fix44.ExecutionReport(this.genOrderID(), this.genExecID(), new ExecType('2'), new OrdStatus('2'), order.getSide(), new LeavesQty(0.0D), new CumQty(orderQty.getValue()), new AvgPx(price.getValue()));
                executionReport.set(order.getClOrdID());
                executionReport.set(order.getSymbol());
                executionReport.set(orderQty);
                executionReport.set(new LastQty(orderQty.getValue()));
                executionReport.set(new LastPx(price.getValue()));
                this.sendMessage(sessionID, executionReport);
            }
        } catch (RuntimeException var7) {
            LogUtil.logThrowable(sessionID, var7.getMessage(), var7);
        }

    }

    public void onMessage(quickfix.fix50.NewOrderSingle order, SessionID sessionID) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        try {
            this.validateOrder(order);
            OrderQty orderQty = order.getOrderQty();
            Price price = this.getPrice(order);
            quickfix.fix50.ExecutionReport accept = new quickfix.fix50.ExecutionReport(this.genOrderID(), this.genExecID(), new ExecType('2'), new OrdStatus('0'), order.getSide(), new LeavesQty(order.getOrderQty().getValue()), new CumQty(0.0D));
            accept.set(order.getClOrdID());
            accept.set(order.getSymbol());
            this.sendMessage(sessionID, accept);
            if (this.isOrderExecutable(order, price)) {
                quickfix.fix50.ExecutionReport executionReport = new quickfix.fix50.ExecutionReport(this.genOrderID(), this.genExecID(), new ExecType('2'), new OrdStatus('2'), order.getSide(), new LeavesQty(0.0D), new CumQty(orderQty.getValue()));
                executionReport.set(order.getClOrdID());
                executionReport.set(order.getSymbol());
                executionReport.set(orderQty);
                executionReport.set(new LastQty(orderQty.getValue()));
                executionReport.set(new LastPx(price.getValue()));
                executionReport.set(new AvgPx(price.getValue()));
                this.sendMessage(sessionID, executionReport);
            }
        } catch (RuntimeException var7) {
            LogUtil.logThrowable(sessionID, var7.getMessage(), var7);
        }

    }

    public OrderID genOrderID() {
        return new OrderID(Integer.toString(++this.m_orderID));
    }

    public ExecID genExecID() {
        return new ExecID(Integer.toString(++this.m_execID));
    }

    public void setMarketDataProvider(MarketDataProvider marketDataProvider) {
        this.marketDataProvider = marketDataProvider;
    }
}
