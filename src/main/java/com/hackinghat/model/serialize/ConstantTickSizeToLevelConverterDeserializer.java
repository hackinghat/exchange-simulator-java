package com.hackinghat.model.serialize;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.hackinghat.model.ConstantTickSizeToLevelConverter;

import java.io.IOException;

public class ConstantTickSizeToLevelConverterDeserializer extends StdDeserializer<ConstantTickSizeToLevelConverter> {

    public ConstantTickSizeToLevelConverterDeserializer() {
        super(ConstantTickSizeToLevelConverter.class);
    }

    @Override
    public ConstantTickSizeToLevelConverter deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)  {
        return null;
    }
}
