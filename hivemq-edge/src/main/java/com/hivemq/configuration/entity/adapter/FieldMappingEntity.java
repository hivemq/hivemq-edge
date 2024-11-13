package com.hivemq.configuration.entity.adapter;

import com.hivemq.persistence.fieldmapping.FieldMapping;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import javax.xml.bind.annotation.XmlElement;

@SuppressWarnings("unused")
public class FieldMappingEntity {

    @XmlElement(name = "source")
    private final @NotNull String sourceFieldName;
    @XmlElement(name = "destination")
    private final @NotNull String destinationFieldName;
    @XmlElement(name = "transformation")
    private final @NotNull TransformationEntity transformation;

    // default constructor needed for JaxB
    public FieldMappingEntity() {
        sourceFieldName = "";
        destinationFieldName = "";
        transformation = new TransformationEntity();
    }

    public FieldMappingEntity(
            final @NotNull String sourceFieldName,
            final @NotNull String destinationFieldName,
            final @NotNull TransformationEntity transformation) {
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
