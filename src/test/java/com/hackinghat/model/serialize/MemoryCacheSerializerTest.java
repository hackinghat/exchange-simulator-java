package com.hackinghat.model.serialize;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hackinghat.instrument.ConstantTickSizeToLevelConverter;
import com.hackinghat.model.Currency;
import com.hackinghat.model.Instrument;
import com.hackinghat.model.Trade;
import com.hackinghat.util.MemoryCache;
import com.hackinghat.util.SimulatorObjectMapper;
import com.hackinghat.util.SimulatorObjectMapperAudience;
import com.hackinghat.util.TimeMachine;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;

public class MemoryCacheSerializerTest {
    @Test
    public void testCacheSerialise() throws JsonProcessingException  {
        final MemoryCache<String, Instrument> instrumentCache = new MemoryCache<>(Instrument.class);
        instrumentCache.upsert(new Instrument("VOD", "Vodafone Plc", new Currency("GBP"), new ConstantTickSizeToLevelConverter(1, 2, 1), LocalDateTime.MIN));
        instrumentCache.upsert(new Instrument("LLOY", "Lloyds Bank Plc", new Currency("GBP"), new ConstantTickSizeToLevelConverter(1, 2, 1), LocalDateTime.MAX));
        final SimulatorObjectMapper mapper  = new SimulatorObjectMapper(SimulatorObjectMapperAudience.PUBLIC, new TimeMachine());
        Assert.assertEquals("[{\"ticker\":\"LLOY\",\"description\":\"Lloyds Bank Plc\",\"currency\":\"GBP\",\"tickConverter\":{\"type\":\"constant\",\"tickSize\":0.5},\"timestamp\":\"+999999999-12-31T23:59:59.999999999\"},{\"ticker\":\"VOD\",\"description\":\"Vodafone Plc\",\"currency\":\"GBP\",\"tickConverter\":{\"type\":\"constant\",\"tickSize\":0.5},\"timestamp\":\"-999999999-01-01T00:01:15\"}]", mapper.writeValueAsString(instrumentCache));
    }
}
