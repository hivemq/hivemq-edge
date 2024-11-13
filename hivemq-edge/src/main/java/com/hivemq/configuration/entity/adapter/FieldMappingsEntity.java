package com.hivemq.configuration.entity.adapter;

import com.hivemq.api.model.mapping.FieldMappingsModel;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.fieldmapping.FieldMappings;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@XmlAccessorType(XmlAccessType.FIELD)
public class FieldMappingsEntity {

    @XmlElement(name = "topic-filter")
    private final @NotNull String topicFilter;

    @XmlElement(name = "tagName")
    private final @NotNull String tagName;

    @XmlElementWrapper(name = "field-mappings")
    @XmlElement(name = "field-mapping")
    private final @NotNull List<FieldMappingEntity> fieldMappingEntities;

    @XmlElement(name = "metadata")
    private final @NotNull FieldMappingMetaDataEntity metaData;

    // default constructor needed for JaxB
    public FieldMappingsEntity(){
        topicFilter = "";
        tagName = "";
        fieldMappingEntities = new ArrayList<>();
        metaData = new FieldMappingMetaDataEntity();
    }

    public FieldMappingsEntity(
            final @NotNull String topicFilter,
            final @NotNull String tagName,
            final @NotNull List<FieldMappingEntity> fieldMappingEntities,
            final @NotNull FieldMappingMetaDataEntity metaData) {
        this.topicFilter = topicFilter;
        this.tagName = tagName;
        this.fieldMappingEntities = fieldMappingEntities;
        this.metaData = metaData;
    }

    public static @NotNull FieldMappingsEntity from(final @NotNull FieldMappings model) {
        final List<FieldMappingEntity> fieldMappingEntityList =
                model.getFieldMappingModels().stream().map(FieldMappingEntity::from).collect(Collectors.toList());



        return new FieldMappingsEntity(model.getTopicFilter(),
                model.getTagName(), fieldMappingEntityList,
                FieldMappingMetaDataEntity.from(model.getMetaData()));
    }

    public @NotNull List<FieldMappingEntity> getFieldMappingModels() {
        return fieldMappingEntities;
    }

    public @NotNull FieldMappingMetaDataEntity getMetaData() {
        return metaData;
    }

    public @NotNull String getTagName() {
        return tagName;
    }

    public @NotNull String getTopicFilter() {
        return topicFilter;
    }

    @Override
    public String toString() {
        return "IM SO FUCKED";
    }
}
