package com.hivemq.persistence.fieldmapping;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.api.model.mapping.FieldMappingModel;
import com.hivemq.api.model.mapping.FieldMappingsModel;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

public class FieldMapping {


    private final @NotNull String sourceFieldName;
    private final @NotNull String destinationFieldName;
    private final @NotNull JsonNode transformation;

    @JsonCreator
    public FieldMapping(
            final @NotNull String sourceFieldName,
            final @NotNull String destinationFieldName,
           final @NotNull JsonNode transformation) {
        this.sourceFieldName = sourceFieldName;
        this.destinationFieldName = destinationFieldName;
        this.transformation = transformation;
    }

    public static @NotNull FieldMapping fromModel(final @NotNull FieldMappingModel model){
        return new FieldMapping(model.getSourceFieldName(), model.getDestinationFieldName(), model.getTransformation());
    }


    public @NotNull String getDestinationFieldName() {
        return destinationFieldName;
    }

    public @NotNull String getSourceFieldName() {
        return sourceFieldName;
    }

    public @NotNull JsonNode getTransformation() {
        return transformation;
    }
}
