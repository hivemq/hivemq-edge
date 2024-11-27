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
package com.hivemq.api.model.tomapping;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.protocols.ToEdgeMapping;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

public class ToEdgeMappingModel {

    @JsonProperty(value = "topicFilter", required = true)
    @Schema(description = "The filter defining what topics we will receive messages from.")
    private final @NotNull String topicFilter;

    @JsonProperty(value = "tagName", required = true)
    @Schema(description = "The tag for which values hould be collected and sent out.")
    private final @NotNull String tagName;

    @JsonProperty(value = "maxQoS", required = true)
    @Schema(description = "The maximum MQTT-QoS for the outgoing messages.")
    private final int maxQoS;

    @JsonCreator
    public ToEdgeMappingModel(
            @JsonProperty(value = "topicFilter", required = true) final @NotNull String topicFilter,
            @JsonProperty(value = "tagName", required = true) final @NotNull String tagName,
            @JsonProperty(value = "maxQoS") final @Nullable Integer maxQoS) {
        this.topicFilter = topicFilter;
        this.tagName = tagName;
        this.maxQoS = Objects.requireNonNullElse(maxQoS, 1);
    }

    public @NotNull String getTopicFilter() {
        return topicFilter;
    }

    public @NotNull String getTagName() {
        return tagName;
    }

    public int getMaxQoS() {
        return maxQoS;
    }

    public ToEdgeMapping toToEdgeMapping() {
        return new ToEdgeMapping(
                this.topicFilter,
                this.tagName,
                this.maxQoS
                );
    }

    public static ToEdgeMappingModel from(ToEdgeMapping toEdgeMapping) {
        return new ToEdgeMappingModel(
                toEdgeMapping.getTopicFilter(),
                toEdgeMapping.getTagName(),
                toEdgeMapping.getMaxQoS());
    }
}
