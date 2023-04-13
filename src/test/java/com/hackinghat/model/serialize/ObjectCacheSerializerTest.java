package com.hackinghat.model.serialize;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hackinghat.instrument.ConstantTickSizeToLevelConverter;
import com.hackinghat.model.Currency;
import com.hackinghat.model.Instrument;
import com.hackinghat.model.Trade;
import com.hackinghat.util.*;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;

public class ObjectCacheSerializerTest {

    @Test
    public void testObjectCacheSerialise() throws JsonProcessingException {
        final Currency gbp = new Currency("GBP", LocalDateTime.MIN);
        final Currency usd = new Currency("USD", LocalDateTime.MIN);
        final MemoryCache<String, Instrument> instrumentCache = new MemoryCache<>(Instrument.class);
        instrumentCache.upsert(new Instrument("VOD", "Vodafone Plc", gbp, new ConstantTickSizeToLevelConverter(1, 2, 1), LocalDateTime.MIN));
        final MemoryCache<String, Currency> currencyCache = new MemoryCache<>(Currency.class);
        currencyCache.upsert(gbp);
        currencyCache.upsert(usd);

        final ObjectCache objectCache = new ObjectCache();
        objectCache.addCache(instrumentCache);
        objectCache.addCache(currencyCache);
        final SimulatorObjectMapper mapper  = new SimulatorObjectMapper(SimulatorObjectMapperAudience.PUBLIC, new TimeMachine());
        Assert.assertEquals("{\"currency\":[{\"currency\":\"GBP\",\"scale\":2,\"timestamp\":\"-999999999-01-01T00:01:15\"}," +
                                                    "{\"currency\":\"USD\",\"scale\":2,\"timestamp\":\"-999999999-01-01T00:01:15\"}]," +
                "\"instrument\":[{\"ticker\":\"VOD\",\"description\":\"Vodafone Plc\",\"currency\":\"GBP\",\"tickConverter\":{\"type\":\"constant\",\"tickSize\":0.5},\"timestamp\":\"-999999999-01-01T00:01:15\"}]}", mapper.writeValueAsString(objectCache));
    }
}
