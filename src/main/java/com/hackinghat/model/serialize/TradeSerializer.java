package com.hackinghat.model.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.hackinghat.model.Instrument;
import com.hackinghat.model.Trade;
import com.hackinghat.util.SimulatorObjectMapper;
import com.hackinghat.util.SimulatorObjectMapperAudience;

import java.io.IOException;
import java.util.function.Function;

public class TradeSerializer extends StdSerializer<Trade> {
    private final SimulatorObjectMapper mapper;

    public TradeSerializer(final SimulatorObjectMapper mapper) {
        super(Trade.class);
        this.mapper = mapper;
    }

    <T> String defaultValue(final T object, Function<T, String> method, final String defaultValue) {
        if (object == null) return defaultValue;
        return method.apply(object);
    }

    @Override
    public void serialize(final Trade trade, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        if (SimulatorObjectMapperAudience.isPrivate(mapper.getAudience())) {
            jsonGenerator.writeStringField("order1", trade.getOrder1());
            jsonGenerator.writeStringField("order2", trade.getOrder2());
        }
        jsonGenerator.writeStringField("instrument", defaultValue(trade.getInstrument(), Instrument::getTicker, null));
        jsonGenerator.writeNumberField("quantity", trade.getQuantity());
        jsonGenerator.writeNumberField("price", trade.getLevel().getPrice());
        jsonGenerator.writeStringField("flags", trade.getFlags());
        jsonGenerator.writeStringField("timestamp", mapper.getTimeMachine().formatTimeAsUTCISO(trade.getTimestamp()));
        jsonGenerator.writeEndObject();
    }
}
