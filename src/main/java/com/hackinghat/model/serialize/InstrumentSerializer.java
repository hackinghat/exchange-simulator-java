package com.hackinghat.model.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.hackinghat.model.Instrument;
import com.hackinghat.util.TimeMachine;

import java.io.IOException;

public class InstrumentSerializer extends StdSerializer<Instrument> {

    private final TimeMachine timeMachine;

    public InstrumentSerializer(final TimeMachine timeMachine) {
        super(Instrument.class);
        this.timeMachine = timeMachine;
    }

    @Override
    public void serialize(final Instrument instrument, final JsonGenerator jsonGenerator, final SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("ticker", instrument.getTicker());
        final String description = instrument.getDescription();
        if (description != null && description.length() > 0) {
            jsonGenerator.writeStringField("description", description);
        }
        jsonGenerator.writeStringField("currency", instrument.getCurrency().getIso3());
        jsonGenerator.writeObjectField("tickConverter", instrument.getLevelDefinition().getTickConverter());
        jsonGenerator.writeStringField("timestamp", timeMachine.formatTimeAsUTCISO(instrument.getTimestamp()));
        jsonGenerator.writeEndObject();
    }
}
