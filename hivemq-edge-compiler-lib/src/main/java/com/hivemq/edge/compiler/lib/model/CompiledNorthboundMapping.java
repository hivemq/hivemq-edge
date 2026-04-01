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
package com.hivemq.edge.compiler.lib.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public record CompiledNorthboundMapping(
        @JsonProperty("tagName") @NotNull String tagName,
        @JsonProperty("topic") @NotNull String topic,
        @JsonProperty("maxQos") int maxQos,
        @JsonProperty("includeTagNames") boolean includeTagNames,
        @JsonProperty("includeTimestamp") boolean includeTimestamp,
        @JsonProperty("includeMetadata") boolean includeMetadata,
        @JsonProperty("mqttUserProperties") @NotNull List<CompiledMqttUserProperty> userProperties,
        @JsonProperty("messageExpiryInterval") long messageExpiryInterval) {

    @JsonCreator
    public CompiledNorthboundMapping(
            @JsonProperty("tagName") final @NotNull String tagName,
            @JsonProperty("topic") final @NotNull String topic,
            @JsonProperty("maxQos") final int maxQos,
            @JsonProperty("includeTagNames") final boolean includeTagNames,
            @JsonProperty("includeTimestamp") final boolean includeTimestamp,
            @JsonProperty("includeMetadata") final boolean includeMetadata,
            @JsonProperty("mqttUserProperties") final @NotNull List<CompiledMqttUserProperty> userProperties,
            @JsonProperty("messageExpiryInterval") final long messageExpiryInterval) {
        this.tagName = tagName;
        this.topic = topic;
        this.maxQos = maxQos;
        this.includeTagNames = includeTagNames;
        this.includeTimestamp = includeTimestamp;
        this.includeMetadata = includeMetadata;
        this.userProperties = userProperties;
        this.messageExpiryInterval = messageExpiryInterval;
    }
}
