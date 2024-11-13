package com.hivemq.persistence.fieldmapping;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.api.model.mapping.FieldMappingModel;
import com.hivemq.configuration.entity.adapter.FieldMappingEntity;
import com.hivemq.extension.sdk.api.annotations.NotNull;

public class FieldMapping {


    private final @NotNull String sourceFieldName;
    private final @NotNull String destinationFieldName;
    private final @NotNull Transformation transformation;

    @JsonCreator
    public FieldMapping(
            final @NotNull String sourceFieldName,
            final @NotNull String destinationFieldName,
           final @NotNull Transformation transformation) {
        this.sourceFieldName = sourceFieldName;
        this.destinationFieldName = destinationFieldName;
        this.transformation = transformation;
    }

    public static @NotNull FieldMapping fromModel(final @NotNull FieldMappingModel model){
        return new FieldMapping(model.getSourceFieldName(), model.getDestinationFieldName(), Transformation.fromModel(model.getTransformation()));
    }


    public @NotNull String getDestinationFieldName() {
        return destinationFieldName;
    }

    public @NotNull String getSourceFieldName() {
        return sourceFieldName;
    }

    public @NotNull Transformation getTransformation() {
        return transformation;
    }

    public static FieldMapping from(final @NotNull FieldMappingEntity fieldMappingEntity) {
        return new FieldMapping(fieldMappingEntity.getSourceFieldName(), fieldMappingEntity.getDestinationFieldName(), Transformation.from(fieldMappingEntity.getTransformation()));
    }
}
