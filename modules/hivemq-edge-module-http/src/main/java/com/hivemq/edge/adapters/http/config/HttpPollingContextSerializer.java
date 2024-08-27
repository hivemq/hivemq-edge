package com.hivemq.edge.adapters.http.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class HttpPollingContextSerializer extends JsonSerializer<List<HttpPollingContext>> {

    @Override
    public void serialize(
            final List<HttpPollingContext> value,
            final JsonGenerator gen,
            final @NotNull SerializerProvider provider) throws IOException {
        gen.writeStartArray();
        for (HttpPollingContext pollingContext : value) {
            gen.writeStartObject();
            gen.writeObjectField("httpToMqttMapping", pollingContext);
            gen.writeEndObject();
        }
        gen.writeEndArray();
    }
}
