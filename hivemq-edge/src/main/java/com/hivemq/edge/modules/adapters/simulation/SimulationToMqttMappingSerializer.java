package com.hivemq.edge.modules.adapters.simulation;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class SimulationToMqttMappingSerializer extends JsonSerializer<List<SimulationToMqttMapping>> {

    @Override
    public void serialize(
            final List<SimulationToMqttMapping> value,
            final JsonGenerator gen,
            final @NotNull SerializerProvider provider) throws IOException {
        gen.writeStartArray();
        for (final SimulationToMqttMapping pollingContext : value) {
            gen.writeStartObject();
            gen.writeObjectField("simulationToMqttMapping", pollingContext);
            gen.writeEndObject();
        }
        gen.writeEndArray();
    }
}
