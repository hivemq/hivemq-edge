package com.hivemq.configuration.entity.adapter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.fieldmapping.FieldMappingMetaData;

public class FieldMappingMetaDataEntity {

    @JsonProperty("source-schema")
    private final @NotNull String sourceJsonSchema;

    @JsonProperty("destination-schema")
    private final @NotNull String destinationJsonSchema;

    @JsonCreator
    public FieldMappingMetaDataEntity(
            @JsonProperty("source-schema") final @NotNull String sourceJsonSchema,
            @JsonProperty("destination-schema") final @NotNull String destinationJsonSchema) {
        this.sourceJsonSchema = sourceJsonSchema;
        this.destinationJsonSchema = destinationJsonSchema;
    }

    public static @NotNull FieldMappingMetaDataEntity from(
            final @NotNull FieldMappingMetaData model) {
        return new FieldMappingMetaDataEntity(model.getSourceJsonSchema().toString(),
                model.getDestinationJsonSchema().toString());
    }

    public @NotNull String getDestinationJsonSchema() {
        return destinationJsonSchema;
    }

    public @NotNull String getSourceJsonSchema() {
        return sourceJsonSchema;
    }
}
