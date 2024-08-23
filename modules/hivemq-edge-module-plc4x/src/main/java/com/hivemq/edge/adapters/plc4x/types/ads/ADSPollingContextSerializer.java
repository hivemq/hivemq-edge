package com.hivemq.edge.adapters.plc4x.types.ads;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.hivemq.edge.adapters.plc4x.model.Plc4xPollingContext;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class ADSPollingContextSerializer extends JsonSerializer<List<Plc4xPollingContext>> {

    @Override
    public void serialize(
            final @NotNull List<Plc4xPollingContext> value,
            final @NotNull JsonGenerator gen,
            final @NotNull SerializerProvider provider) throws IOException {
        gen.writeStartArray();
        for (Plc4xPollingContext pollingContext : value) {
            gen.writeStartObject();
            gen.writeObjectField("adsToMqttMapping", pollingContext);
            gen.writeEndObject();
        }
        gen.writeEndArray();
    }
}
