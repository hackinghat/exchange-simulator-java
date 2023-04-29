package com.hackinghat.model.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.hackinghat.model.ConstantTickSizeToLevelConverter;

import java.io.IOException;

public class ConstantTickSizeToLevelConverterSerializer extends StdSerializer<ConstantTickSizeToLevelConverter> {

    public ConstantTickSizeToLevelConverterSerializer() {
        super(ConstantTickSizeToLevelConverter.class);
    }

    @Override
    public void serialize(ConstantTickSizeToLevelConverter constantTickSizeToLevelConverter, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("type", "constant");
        jsonGenerator.writeNumberField("tickSize", constantTickSizeToLevelConverter.getTickSize());
        jsonGenerator.writeEndObject();
    }
}
