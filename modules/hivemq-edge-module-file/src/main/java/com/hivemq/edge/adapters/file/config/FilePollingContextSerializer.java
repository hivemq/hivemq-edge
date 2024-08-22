package com.hivemq.edge.adapters.file.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class FilePollingContextSerializer extends JsonSerializer<List<FilePollingContext>> {

    @Override
    public void serialize(
            final List<FilePollingContext> value,
            final JsonGenerator gen,
            final @NotNull SerializerProvider provider) throws IOException {
        gen.writeStartArray();
        for (final FilePollingContext pollingContext : value) {
            gen.writeStartObject();
            gen.writeObjectField("fileToMqttMapping", pollingContext);
            gen.writeEndObject();
        }
        gen.writeEndArray();
    }
}
