package com.hackinghat.model.serialize;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.hackinghat.model.Instrument;
import com.hackinghat.model.Trade;
import com.hackinghat.util.MemoryCache;
import com.hackinghat.util.SimulatorObjectMapper;

import java.io.IOException;
import java.util.Objects;

public class TradeDeserializer extends StdDeserializer<Trade> {
    private final SimulatorObjectMapper mapper;

    public TradeDeserializer(final SimulatorObjectMapper mapper) {
        super(Trade.class);
        this.mapper = mapper;
    }

    @Override
    public Trade deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext) throws IOException, JacksonException {
        final Trade trade = new Trade();
        final ObjectCodec codec = jsonParser.getCodec();
        final JsonNode node = codec.readTree(jsonParser);
        MemoryCache<String, Instrument> instrumentCache = mapper.getCache(Instrument.class);
        Objects.requireNonNull(instrumentCache);
        final Instrument instrument = instrumentCache.get(node.get("instrument").textValue()).get();
        Objects.requireNonNull(instrument);
        trade.setTimestamp(mapper.getTimeMachine().parseTimeFromUTCISO(node.get("timestamp").textValue()));
        trade.setOrder1(node.get("order1").textValue());
        trade.setOrder1(node.get("order2").textValue());
        trade.setQuantity(node.get("quantity").intValue());
        trade.setFlags(node.get("flags").textValue());
        trade.setInstrument(instrument);
        trade.setLevel(instrument.getLevel(Float.parseFloat(node.get("price").toString())));
        return trade;
    }
}
