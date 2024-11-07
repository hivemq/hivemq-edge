package com.hivemq.api.model.mapping;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

public class FieldMappingModel {

    @JsonProperty("source")
    @Schema(description = "The field name in the incoming data.")
    private final @NotNull String sourceFieldName;


    @JsonProperty("destination")
    @Schema(description = "The field name in the outgoing data")
    private final @NotNull String destinationFieldName;


    @JsonProperty("transformation")
    @Schema(description = "The kind of transformation that is performed on the data")
    private final @NotNull JsonNode transformation;

    @JsonCreator
    public FieldMappingModel(
            @JsonProperty("source") final @NotNull String sourceFieldName,
            @JsonProperty("destination") final @NotNull String destinationFieldName,
            @JsonProperty("transformation") final @NotNull JsonNode transformation) {
        this.sourceFieldName = sourceFieldName;
        this.destinationFieldName = destinationFieldName;
        this.transformation = transformation;
    }
}
