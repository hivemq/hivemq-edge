package com.hivemq.edge.adapters.etherip.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class EipPollingContextSerializer extends JsonSerializer<List<EipPollingContext>> {

    @Override
    public void serialize(
            final @NotNull List<EipPollingContext> value,
            final @NotNull JsonGenerator gen,
            final @NotNull SerializerProvider provider) throws IOException {
        gen.writeStartArray();
        for (EipPollingContext pollingContext : value) {
            gen.writeStartObject();
            gen.writeObjectField("eipToMqttMapping", pollingContext);
            gen.writeEndObject();
        }
        gen.writeEndArray();
    }
}
