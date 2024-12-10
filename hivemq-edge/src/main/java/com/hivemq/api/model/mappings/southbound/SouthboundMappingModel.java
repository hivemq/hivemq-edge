/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.api.model.mappings.southbound;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.api.model.mappings.fieldmapping.FieldMappingModel;
import com.hivemq.persistence.mappings.SouthboundMapping;
import com.hivemq.persistence.mappings.fieldmapping.FieldMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Schema(name = "SouthboundMapping")
public class SouthboundMappingModel {

    @JsonProperty(value = "topicFilter", required = true)
    @Schema(description = "The filter defining what topics we will receive messages from.")
    private final @NotNull String topicFilter;

    @JsonProperty(value = "tagName", required = true)
    @Schema(description = "The tag for which values hould be collected and sent out.", format = "mqtt-tag")
    private final @NotNull String tagName;

    @JsonProperty(value = "fieldMapping")
    @Schema(description = "Defines how incoming data should be transformed before being sent out.")
    private final @Nullable FieldMappingModel fieldMapping;

    @JsonCreator
    public SouthboundMappingModel(
            @JsonProperty(value = "topicFilter", required = true) final @NotNull String topicFilter,
            @JsonProperty(value = "tagName", required = true) final @NotNull String tagName,
            @JsonProperty(value = "fieldMapping") final @Nullable FieldMappingModel fieldMapping) {
        this.topicFilter = topicFilter;
        this.tagName = tagName;
        this.fieldMapping = fieldMapping;
    }

    public @NotNull String getTopicFilter() {
        return topicFilter;
    }

    public @NotNull String getTagName() {
        return tagName;
    }

    public @Nullable FieldMappingModel getFieldMapping() {
        return fieldMapping;
    }

    public @NotNull SouthboundMapping toToEdgeMapping(final @NotNull String schema) {
        return new SouthboundMapping(this.tagName,
                this.topicFilter,
                this.fieldMapping != null ? FieldMapping.fromModel(this.fieldMapping) : null,
                schema);
                this.maxQoS,
                this.fieldMapping != null ? FieldMapping.fromModel(this.fieldMapping) : DEFAULT_FIELD_MAPPING,
                schema);
    }

    public static SouthboundMappingModel from(final @NotNull SouthboundMapping southboundMapping) {
        return new SouthboundMappingModel(southboundMapping.getTopicFilter(),
                southboundMapping.getTagName(),
                FieldMappingModel.from(southboundMapping.getFieldMapping()));
    }
}
