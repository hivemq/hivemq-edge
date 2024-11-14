package com.hivemq.persistence.fieldmapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.api.model.mapping.FieldMappingsModel;
import com.hivemq.configuration.entity.adapter.FieldMappingsEntity;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class FieldMappings {

    private final @NotNull String topicFilter;
    private final @NotNull String tagName;
    private final @NotNull List<FieldMapping> fieldMappings;
    private final @NotNull FieldMappingMetaData metaData;

    public FieldMappings(
            final @NotNull String topicFilter,
            final @NotNull String tagName,
            final @NotNull List<FieldMapping> fieldMappings,
            final @NotNull FieldMappingMetaData metaData) {
        this.topicFilter = topicFilter;
        this.tagName = tagName;
        this.fieldMappings = fieldMappings;
        this.metaData = metaData;
    }

    public static @NotNull FieldMappings fromModel(final @NotNull FieldMappingsModel model) {
        final List<FieldMapping> fieldMappingList =
                model.getFieldMappingModels().stream().map(FieldMapping::fromModel).collect(Collectors.toList());
        return new FieldMappings(model.getTopicFilter(),
                model.getTagName(),
                fieldMappingList,
                FieldMappingMetaData.fromModel(model.getMetaData()));
    }

    public @NotNull List<FieldMapping> getFieldMappings() {
        return fieldMappings;
    }

    public @NotNull FieldMappingMetaData getMetaData() {
        return metaData;
    }

    public @NotNull String getTagName() {
        return tagName;
    }

    public @NotNull String getTopicFilter() {
        return topicFilter;
    }

    public static FieldMappings fromEntity(final @NotNull FieldMappingsEntity fieldMappingsEntity, final @NotNull
                                           ObjectMapper objectMapper) {

        final List<FieldMapping> fieldMappingList = fieldMappingsEntity.getFieldMappingModels()
                .stream()
                .map(FieldMapping::from)
                .collect(Collectors.toList());

        return new FieldMappings(fieldMappingsEntity.getTopicFilter(), fieldMappingsEntity.getTagName(), fieldMappingList, FieldMappingMetaData.fromEntity(fieldMappingsEntity.getMetaData(), objectMapper));


    }
}
