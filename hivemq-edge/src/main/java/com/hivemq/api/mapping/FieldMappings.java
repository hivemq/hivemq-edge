package com.hivemq.api.mapping;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import org.bouncycastle.asn1.cms.MetaData;

import java.util.List;

public class FieldMappings {

    @JsonProperty("topicFilter")
    @Schema(description = "The topic filter according to the MQTT specification.", format = "mqtt-topic-filter")
    private final @NotNull String topicFilter;

    @JsonProperty("tag")
    @Schema(description = "")
    private final @NotNull String tagName;


    @JsonProperty("fieldMapping")
    @Schema(description = "")
    private final @NotNull List<FieldMapping> fieldMappings;


    @JsonProperty("metadata")
    @Schema(description = "")
    private final @NotNull MetaData metaData;

    public FieldMappings(
            @JsonProperty("topicFilter") final @NotNull String topicFilter,
            @JsonProperty("tag") final @NotNull String tagName,
            @JsonProperty("fieldMapping") final @NotNull List<FieldMapping> fieldMappings,
            @JsonProperty("metadata") final @NotNull MetaData metaData) {
        this.topicFilter = topicFilter;
        this.tagName = tagName;
        this.fieldMappings = fieldMappings;
        this.metaData = metaData;
    }
}
