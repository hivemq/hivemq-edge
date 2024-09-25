package com.hivemq.edge.adapters.http.mqtt2http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;

public class JsonSchema {

    private static final @NotNull ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static @NotNull JsonNode createJsonSchema() {
        try {
            return OBJECT_MAPPER.readTree("{\n" +
                    "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" +
                    "  \"type\": \"object\",\n" +
                    "  \"properties\": {\n" +
                    "    \"value\": {\n" +
                    "      \"type\": \"object\"\n" +
                    "    }\n" +
                    "  },\n" +
                    "  \"required\": [\n" +
                    "    \"value\"\n" +
                    "  ]\n" +
                    "}");
        } catch (final JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
