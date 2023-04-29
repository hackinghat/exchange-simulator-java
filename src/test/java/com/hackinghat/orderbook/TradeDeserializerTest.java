package com.hackinghat.orderbook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hackinghat.model.*;
import com.hackinghat.util.SimulatorObjectMapper;
import com.hackinghat.util.SimulatorObjectMapperAudience;
import com.hackinghat.util.TimeMachine;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.time.LocalDateTime;

public class TradeDeserializerTest {
    TickConverter tickConverter;
    SimulatorObjectMapper publicMapper;
    SimulatorObjectMapper privateMapper;

    @Before
    public void testSetup() {
        tickConverter = new ConstantTickSizeToLevelConverter(1, 100, 3);
        publicMapper = new SimulatorObjectMapper(SimulatorObjectMapperAudience.PUBLIC, new TimeMachine());
        privateMapper = new SimulatorObjectMapper(SimulatorObjectMapperAudience.PRIVATE, new TimeMachine());
    }

//    @Test
//    public void testSerializeDeserialize() {
//        final Trade t = new Trade(this, new Instrument("VOD.L", tickConverter), LocalTime.now(), "A", "O1", "O2", "XXX", Level.MARKET, 500);
//
//        final KafkaJsonSerializerDeserializer<Trade> ts = new KafkaJsonSerializerDeserializer<>(Trade.class);
//        byte[] bytes = ts.serialize(null, t);
//        final Trade t2 = ts.deserialize(null, bytes);
//        Assert.assertEquals(t.getLevel(), t2.getLevel());
//        Assert.assertEquals(t.getFlags(), t2.getFlags());
//        Assert.assertEquals(t.getOrder1(), t2.getOrder1());
//        Assert.assertEquals(t.getOrder2(), t2.getOrder2());
//        Assert.assertEquals(t.getTimestamp(), t2.getTimestamp());
//        Assert.assertNull(t2.getSender());
//    }

    @Test
    public void testSerialisePublic() throws JsonProcessingException {
        final Trade t = new Trade(this, "T1", new Instrument("VOD.L", new Currency("GBP"), new ConstantTickSizeToLevelConverter(0.001f)), LocalDateTime.MAX, "A", "O1", "O2", tickConverter.calculateLevel(100.1f), 500);
        Assert.assertFalse(publicMapper.writeValueAsString(t).contains("O1"));
        Assert.assertEquals("{\"instrument\":\"VOD.L\",\"quantity\":500,\"price\":100.1,\"flags\":\"A\",\"timestamp\":\"+999999999-12-31T23:59:59.999999999\"}", publicMapper.writeValueAsString(t));
    }

    @Test
    public void testSerialisePrivate() throws JsonProcessingException {
        final Trade t = new Trade(this, "T1", new Instrument("VOD.L", new Currency("GBP"), new ConstantTickSizeToLevelConverter(0.001f)), LocalDateTime.MAX, "A", "O1", "O2", tickConverter.calculateLevel(100.1f), 500);
        Assert.assertEquals("{\"order1\":\"O1\",\"order2\":\"O2\",\"instrument\":\"VOD.L\",\"quantity\":500,\"price\":100.1,\"flags\":\"A\",\"timestamp\":\"+999999999-12-31T23:59:59.999999999\"}", privateMapper.writeValueAsString(t));
        Assert.assertTrue(privateMapper.writeValueAsString(t).contains("O1"));
    }

    @Test
    public void testDeserialise() throws IOException {
        final String order1 = "{\"order1\":\"O1\",\"order2\":\"O2\",\"instrument\":\"VOD.L\",\"quantity\":500,\"price\":100.1,\"flags\":\"A\",\"timestamp\":\"2022-05-23T17:11:30.734866897\"}";
        final SimulatorObjectMapper mapper = new SimulatorObjectMapper(SimulatorObjectMapperAudience.PRIVATE, new TimeMachine());
        Assert.assertThrows(NullPointerException.class, () -> mapper.readValue(order1, Trade.class));
        final Instrument VOD = new Instrument("VOD.L", "Vodafone PLC", new Currency("GBP"), new ConstantTickSizeToLevelConverter(1, 2, 2));
        mapper.getCache(Instrument.class).insert(VOD);
        final Trade t1 = mapper.readValue(order1, Trade.class);
        Assert.assertEquals(100.1f, t1.getLevel().getPrice(), VOD.getTickConverter().getTickSize(100.1f));
    }
}
