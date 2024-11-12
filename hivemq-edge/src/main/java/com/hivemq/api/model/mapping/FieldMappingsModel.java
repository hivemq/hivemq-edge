package com.hivemq.api.model.mapping;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import org.bouncycastle.asn1.cms.MetaData;

import java.util.List;

public class FieldMappingsModel {

    @JsonProperty("topicFilter")
    @Schema(description = "The topic filter according to the MQTT specification.", format = "mqtt-topic-filter")
    private final @NotNull String topicFilter;

    @JsonProperty("tag")
    @Schema(description = "")
    private final @NotNull String tagName;

    @JsonProperty("fieldMapping")
    @Schema(description = "")
    private final @NotNull List<FieldMappingModel> fieldMappingModels;

    @JsonProperty("metadata")
    @Schema(description = "")
    private final @NotNull FieldMappingMetaDataModel metaData;

    public FieldMappingsModel(
            @JsonProperty("topicFilter") final @NotNull String topicFilter,
            @JsonProperty("tag") final @NotNull String tagName,
            @JsonProperty("fieldMapping") final @NotNull List<FieldMappingModel> fieldMappingModels,
            @JsonProperty("metadata") final @NotNull FieldMappingMetaDataModel metaData) {
        this.topicFilter = topicFilter;
        this.tagName = tagName;
        this.fieldMappingModels = fieldMappingModels;
        this.metaData = metaData;
    }

    public @NotNull List<FieldMappingModel> getFieldMappingModels() {
        return fieldMappingModels;
    }

    public @NotNull FieldMappingMetaDataModel getMetaData() {
        return metaData;
    }

    public @NotNull String getTagName() {
        return tagName;
    }

    public @NotNull String getTopicFilter() {
        return topicFilter;
    }
}
