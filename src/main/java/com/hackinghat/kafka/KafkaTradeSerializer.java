package com.hackinghat.kafka;

import com.hackinghat.model.Trade;
import com.hackinghat.util.SimulatorObjectMapper;

public class KafkaTradeSerializer extends KafkaJsonSerializer<Trade> {
    public KafkaTradeSerializer() {
        super(null);
    }
    public KafkaTradeSerializer(final SimulatorObjectMapper mapper) {
        super(mapper);
    }
}
