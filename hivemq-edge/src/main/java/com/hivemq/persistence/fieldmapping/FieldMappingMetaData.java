package com.hivemq.persistence.fieldmapping;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.api.model.mapping.FieldMappingMetaDataModel;
import com.hivemq.extension.sdk.api.annotations.NotNull;

public class FieldMappingMetaData {

    private final @NotNull JsonNode sourceJsonSchema;
    private final @NotNull JsonNode destinationJsonSchema;


    @JsonCreator
    public FieldMappingMetaData(
            final @NotNull JsonNode sourceJsonSchema, final @NotNull JsonNode destinationJsonSchema) {
        this.sourceJsonSchema = sourceJsonSchema;
        this.destinationJsonSchema = destinationJsonSchema;
    }

    public static @NotNull FieldMappingMetaData fromModel(final @NotNull FieldMappingMetaDataModel model) {
        return new FieldMappingMetaData(model.getSourceJsonSchema(), model.getDestinationJsonSchema());
    }

    public @NotNull JsonNode getDestinationJsonSchema() {
        return destinationJsonSchema;
    }

    public @NotNull JsonNode getSourceJsonSchema() {
        return sourceJsonSchema;
    }
}
