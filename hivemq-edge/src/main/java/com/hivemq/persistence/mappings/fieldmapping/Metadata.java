package com.hivemq.persistence.mappings.fieldmapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.api.model.mappings.fieldmapping.MetadataModel;
import org.jetbrains.annotations.NotNull;

public class Metadata {
    private final @NotNull JsonNode destinationJsonSchema;

    private final @NotNull JsonNode sourceJsonSchema;

    public Metadata(@NotNull final JsonNode destinationJsonSchema, @NotNull final JsonNode sourceJsonSchema) {
        this.destinationJsonSchema = destinationJsonSchema;
        this.sourceJsonSchema = sourceJsonSchema;
    }

    public @NotNull JsonNode getDestinationJsonSchema() {
        return destinationJsonSchema;
    }

    public @NotNull JsonNode getSourceJsonSchema() {
        return sourceJsonSchema;
    }

    public static Metadata from(MetadataModel model) {
        return new Metadata(model.getDestinationJsonSchema(), model.getSourceJsonSchema());
    }
}
