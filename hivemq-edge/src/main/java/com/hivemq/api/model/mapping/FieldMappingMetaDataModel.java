package com.hivemq.api.model.mapping;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

public class FieldMappingMetaDataModel {


    @JsonProperty("source")
    @Schema(description = "The json schema validating the incoming data for the transformation",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private final @NotNull JsonNode sourceJsonSchema;


    @JsonProperty("destination")
    @Schema(description = "The json schema validating the outgoing data for the transformation",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private final @NotNull JsonNode destinationJsonSchema;


    @JsonCreator
    public FieldMappingMetaDataModel(
            @JsonProperty("source") final @NotNull JsonNode sourceJsonSchema,
            @JsonProperty("destination") final @NotNull JsonNode destinationJsonSchema) {
        this.sourceJsonSchema = sourceJsonSchema;
        this.destinationJsonSchema = destinationJsonSchema;
    }

    public @NotNull JsonNode getDestinationJsonSchema() {
        return destinationJsonSchema;
    }

    public @NotNull JsonNode getSourceJsonSchema() {
        return sourceJsonSchema;
    }
}
