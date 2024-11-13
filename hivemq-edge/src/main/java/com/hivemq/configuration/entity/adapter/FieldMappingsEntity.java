package com.hivemq.configuration.entity.adapter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.fieldmapping.FieldMappings;

import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class FieldMappingsEntity {

    @JsonProperty("topic-filter")
    private final @NotNull String topicFilter;

    @JsonProperty("tag-name")
    private final @NotNull String tagName;

    @JsonProperty("field-mappings")
    private final @NotNull List<FieldMappingEntity> fieldMappingEntities;

    @JsonProperty("metadata")
    private final @NotNull FieldMappingMetaDataEntity metaData;

    @JsonCreator
    public FieldMappingsEntity(
            @JsonProperty("topic-filter") final @NotNull String topicFilter,
            @JsonProperty("tag-name") final @NotNull String tagName,
            @JsonProperty("field-mappings") final @NotNull List<FieldMappingEntity> fieldMappingEntities,
            @JsonProperty("metadata") final @NotNull FieldMappingMetaDataEntity metaData) {
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
}
