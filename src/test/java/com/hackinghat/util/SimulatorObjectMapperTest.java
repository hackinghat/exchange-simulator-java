package com.hackinghat.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hackinghat.model.ConstantTickSizeToLevelConverter;
import com.hackinghat.model.Currency;
import com.hackinghat.model.Instrument;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;

public class SimulatorObjectMapperTest {
    @Test
    public void testInstrumentSerialize() throws JsonProcessingException {
        final SimulatorObjectMapper mapper = new SimulatorObjectMapper(SimulatorObjectMapperAudience.PUBLIC, new TimeMachine());
        final Instrument i = new Instrument("VOD.L", "Vodafone PLC", new Currency("GBP"), new ConstantTickSizeToLevelConverter(1, 2, 1), LocalDateTime.of(2022, 5, 19, 17, 0, 0, 500));
        Assert.assertEquals("{\"ticker\":\"VOD.L\",\"description\":\"Vodafone PLC\",\"currency\":\"GBP\",\"tickConverter\":{\"type\":\"constant\",\"tickSize\":0.5},\"timestamp\":\"2022-05-19T16:00:00.000000500\"}", mapper.writeValueAsString(i));
    }

    @Test
    public void testInstrumentDeserialize() throws JsonProcessingException {
        final SimulatorObjectMapper mapper = new SimulatorObjectMapper(SimulatorObjectMapperAudience.PUBLIC, new TimeMachine());
        final String s = "{\"ticker\":\"VOD.L\",\"description\":\"Vodafone PLC\",\"currency\":\"GBP\",\"tickConverter\":{\"type\":\"constant\",\"tickSize\":0.5},\"timestamp\":\"2022-05-19T16:00:00.000000500\"}";
        final Instrument i = mapper.readValue(s, Instrument.class);
        Assert.assertEquals("VOD.L", i.getTicker());
    }

}
