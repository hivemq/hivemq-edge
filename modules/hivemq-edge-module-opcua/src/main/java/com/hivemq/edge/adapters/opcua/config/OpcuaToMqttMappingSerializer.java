package com.hivemq.edge.adapters.opcua.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class OpcuaToMqttMappingSerializer extends JsonSerializer<List<OpcUaToMqttMapping>> {

    @Override
    public void serialize(
            final @NotNull List<OpcUaToMqttMapping> value,
            final @NotNull JsonGenerator gen,
            final @NotNull SerializerProvider provider) throws IOException {
        gen.writeStartArray();
        for (final OpcUaToMqttMapping pollingContext : value) {
            gen.writeStartObject();
            gen.writeObjectField("opcuaToMqttMapping", pollingContext);
            gen.writeEndObject();
        }
        gen.writeEndArray();
    }

}
