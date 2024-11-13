package com.hivemq.configuration.entity.adapter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.fieldmapping.FieldMappingMetaData;

import javax.xml.bind.annotation.XmlElement;

@SuppressWarnings("unused")
public class FieldMappingMetaDataEntity {

    @XmlElement(name = "sourceSchema")
    private final @NotNull String sourceJsonSchema;

    @XmlElement(name = "destinationSchema")
    private final @NotNull String destinationJsonSchema;

    // default constructor needed for JaxB
    public FieldMappingMetaDataEntity() {
        this.sourceJsonSchema = "";
        this.destinationJsonSchema = "";
    }

    public FieldMappingMetaDataEntity(
            final @NotNull String sourceJsonSchema, final @NotNull String destinationJsonSchema) {
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
