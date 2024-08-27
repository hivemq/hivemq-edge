package com.hivemq.edge.adapters.file.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class FileToMqttMappingSerializer extends JsonSerializer<List<FileToMqttMapping>> {

    @Override
    public void serialize(
            final List<FileToMqttMapping> value,
            final JsonGenerator gen,
            final @NotNull SerializerProvider provider) throws IOException {
        gen.writeStartArray();
        for (final FileToMqttMapping pollingContext : value) {
            gen.writeStartObject();
            gen.writeObjectField("fileToMqttMapping", pollingContext);
            gen.writeEndObject();
        }
        gen.writeEndArray();
    }
}
