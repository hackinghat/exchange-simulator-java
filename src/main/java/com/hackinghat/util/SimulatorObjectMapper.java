package com.hackinghat.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.hackinghat.instrument.ConstantTickSizeToLevelConverter;
import com.hackinghat.model.Currency;
import com.hackinghat.model.Instrument;
import com.hackinghat.model.Trade;
import com.hackinghat.model.serialize.*;

public class SimulatorObjectMapper extends ObjectMapper {

    private static final ObjectCache            OBJECT_CACHE;

    static {
        OBJECT_CACHE  = new ObjectCache();
        OBJECT_CACHE.addCache(new MemoryCache<>(Instrument.class));
    }

    private final TimeMachine                       timeMachine;
    private final SimulatorObjectMapperAudience     audience;

    public TimeMachine getTimeMachine() { return timeMachine; }
    public SimulatorObjectMapperAudience getAudience() { return audience; }

    public <K extends Comparable<K>, V extends CopyableAndIdentifiable<K>> MemoryCache<K, V> getCache(Class<V> valueClazz) {
        return OBJECT_CACHE.getCache(valueClazz);
    }

    public SimulatorObjectMapper(final SimulatorObjectMapperAudience audience, final TimeMachine timeMachine) {
        super();
        this.audience = audience;
        this.timeMachine = timeMachine;
        SimpleModule module = new SimpleModule();
        // ConstantTickSizeToLevelConverter
        module.addSerializer(ConstantTickSizeToLevelConverter.class, new ConstantTickSizeToLevelConverterSerializer());
        // Currency
        module.addSerializer(Currency.class, new CurrencySerializer(timeMachine));
        // Instrument
        module.addSerializer(Instrument.class, new InstrumentSerializer(timeMachine));
        module.addSerializer(MemoryCache.class, new MemoryCacheSerializer());
        module.addDeserializer(Instrument.class, new InstrumentDeserializer(OBJECT_CACHE));
        // Trades
        module.addSerializer(Trade.class, new TradeSerializer(this));
        module.addDeserializer(Trade.class, new TradeDeserializer(this));
        // ObjectCache
        module.addSerializer(ObjectCache.class, new ObjectCacheSerializer());
        registerModule(module);
    }
}
