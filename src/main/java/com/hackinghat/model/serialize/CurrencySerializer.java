package com.hackinghat.model.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.hackinghat.model.Currency;
import com.hackinghat.util.TimeMachine;

import java.io.IOException;

public class CurrencySerializer extends StdSerializer<Currency> {

    private final TimeMachine timeMachine;

    public CurrencySerializer(final TimeMachine timeMachine) {
        super(Currency.class);
        this.timeMachine = timeMachine;
    }

    @Override
    public void serialize(final Currency currency, final JsonGenerator jsonGenerator, final SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("currency", currency.getId());
        jsonGenerator.writeNumberField("scale", currency.getScale());
        jsonGenerator.writeStringField("timestamp", timeMachine.formatTimeAsUTCISO(currency.getTimestamp()));
        jsonGenerator.writeEndObject();
    }
}
