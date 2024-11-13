package com.hivemq.configuration.entity.adapter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.fieldmapping.FieldMapping;

public class FieldMappingEntity {

    @JsonProperty("source")
    private final @NotNull String sourceFieldName;
    @JsonProperty("destination")
    private final @NotNull String destinationFieldName;
    @JsonProperty("transformation")
    private final @NotNull TransformationEntity transformation;

    @JsonCreator
    public FieldMappingEntity(
            @JsonProperty("source") final @NotNull String sourceFieldName,
            @JsonProperty("destination") final @NotNull String destinationFieldName,
            @JsonProperty("transformation") final @NotNull TransformationEntity transformation) {
        this.sourceFieldName = sourceFieldName;
        this.destinationFieldName = destinationFieldName;
        this.transformation = transformation;
    }

    public static @NotNull FieldMappingEntity from(final @NotNull FieldMapping model) {
        return new FieldMappingEntity(model.getSourceFieldName(),
                model.getDestinationFieldName(),
                TransformationEntity.from(model.getTransformation()));
    }


    public @NotNull String getDestinationFieldName() {
        return destinationFieldName;
    }

    public @NotNull String getSourceFieldName() {
        return sourceFieldName;
    }

    public @NotNull TransformationEntity getTransformation() {
        return transformation;
    }
}
