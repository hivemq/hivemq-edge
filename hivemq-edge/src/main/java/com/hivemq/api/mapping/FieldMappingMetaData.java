package com.hivemq.api.mapping;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

public class FieldMappingMetaData {


    @JsonProperty("source")
    @Schema(description = "The json schema validating the incoming data for the transformation")
    private final @NotNull JsonNode sourceJsonSchema;


    @JsonProperty("destination")
    @Schema(description = "The json schema validating the outgoing data for the transformation")
    private final @NotNull JsonNode destintionJsonSchema;


    @JsonCreator
    public FieldMappingMetaData(
            @JsonProperty("source") final @NotNull JsonNode sourceJsonSchema,
            @JsonProperty("destination") final @NotNull JsonNode destintionJsonSchema) {
        this.sourceJsonSchema = sourceJsonSchema;
        this.destintionJsonSchema = destintionJsonSchema;
    }
}
