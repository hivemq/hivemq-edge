package com.hivemq.edge.adapters.modbus.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class PollingContextSerializer extends JsonSerializer<List<PollingContextImpl>> {

    @Override
    public void serialize(
            final List<PollingContextImpl> value,
            final JsonGenerator gen,
            final @NotNull SerializerProvider provider) throws IOException {
        gen.writeStartArray();
        for (PollingContextImpl pollingContext : value) {
            gen.writeStartObject();
            gen.writeObjectField("modbusToMqttMapping", pollingContext);
            gen.writeEndObject();
        }
        gen.writeEndArray();
    }
}
