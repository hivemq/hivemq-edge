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
package com.hivemq.api.model.mappings.northbound;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.adapter.sdk.api.config.MqttUserProperty;
import com.hivemq.api.model.mappings.fieldmapping.FieldMappingModel;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.persistence.mappings.NorthboundMapping;
import com.hivemq.persistence.mappings.fieldmapping.FieldMapping;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@JsonTypeName("NorthboundMapping")
public class NorthboundMappingModel {

    @JsonProperty(value = "topic", required = true)
    @Schema(description = "The target mqtt topic where received tags should be sent to.")
    private final @NotNull String topic;

    @JsonProperty(value = "tagName", required = true)
    @Schema(description = "The tag for which values hould be collected and sent out.")
    private final @NotNull String tagName;

    @JsonProperty(value = "messageHandlingOptions", required = true)
    @Schema(description = "How collected tags should or shouldnÖT be aggregated.")
    private final @Nullable MessageHandlingOptions messageHandlingOptions;

    @JsonProperty(value = "includeTagNames", required = true)
    @Schema(description = "Should tag names be included when sent out.")
    private final boolean includeTagNames;

    @JsonProperty(value = "includeTimestamp", required = true)
    @Schema(description = "Should the timestamp be included when sent out.")
    private final boolean includeTimestamp;

    @JsonProperty(value = "userProperties")
    @Schema(description = "User properties to be added to each outgoing mqtt message.")
    private final @Nullable List<MqttUserPropertyModel> userProperties;

    @JsonProperty(value = "maxQoS", required = true)
    @Schema(description = "The maximum MQTT-QoS for the outgoing messages.")
    private final int maxQoS;

    @JsonProperty(value = "messageExpiryInterval", required = true)
    @Schema(description = "The message expiry interval.")
    private final long messageExpiryInterval;

    @JsonCreator
    public NorthboundMappingModel(
            @JsonProperty(value = "topic", required = true) final @NotNull String topic,
            @JsonProperty(value = "tagName", required = true) final @NotNull String tagName,
            @JsonProperty(value = "messageHandlingOptions") final @Nullable MessageHandlingOptions messageHandlingOptions,
            @JsonProperty(value = "includeTagNames") final @Nullable Boolean includeTagNames,
            @JsonProperty(value = "includeTimestamp") final @Nullable Boolean includeTimestamp,
            @JsonProperty(value = "userProperties") final @Nullable List<MqttUserPropertyModel> userProperties,
            @JsonProperty(value = "maxQoS") final @Nullable Integer maxQoS,
            @JsonProperty(value = "messageExpiryInterval") final @Nullable Long messageExpiryInterval) {
        this.topic = topic;
        this.tagName = tagName;
        this.messageHandlingOptions = Objects.requireNonNullElse(messageHandlingOptions, MessageHandlingOptions.MQTTMessagePerTag);
        this.includeTagNames = Objects.requireNonNullElse(includeTagNames, false);
        this.includeTimestamp = Objects.requireNonNullElse(includeTimestamp, false);
        this.userProperties = Objects.requireNonNullElse(userProperties, List.of());
        this.maxQoS = Objects.requireNonNullElse(maxQoS, 1);
        this.messageExpiryInterval = Objects.requireNonNullElse(messageExpiryInterval, Long.MAX_VALUE);
    }

    public @NotNull String getTopic() {
        return topic;
    }

    public @NotNull String getTagName() {
        return tagName;
    }

    public @Nullable MessageHandlingOptions getMessageHandlingOptions() {
        return messageHandlingOptions;
    }

    public boolean isIncludeTagNames() {
        return includeTagNames;
    }

    public boolean isIncludeTimestamp() {
        return includeTimestamp;
    }

    public @Nullable List<MqttUserPropertyModel> getUserProperties() {
        return userProperties;
    }

    public int getMaxQoS() {
        return maxQoS;
    }

    public long getMessageExpiryInterval() {
        return messageExpiryInterval;
    }

    public NorthboundMapping to() {

        return new NorthboundMapping(
                this.tagName,
                this.topic,
                this.maxQoS,
                this.messageExpiryInterval,
                this.messageHandlingOptions,
                this.includeTagNames,
                this.includeTimestamp,
                userProperties.stream()
                        .map(prop -> new MqttUserProperty(prop.getName(),prop.getValue()))
                        .collect(Collectors.toList()));
    }

    public static NorthboundMappingModel from(NorthboundMapping northboundMapping) {
        return new NorthboundMappingModel(
                northboundMapping.getMqttTopic(),
                northboundMapping.getTagName(),
                northboundMapping.getMessageHandlingOptions(),
                northboundMapping.getIncludeTagNames(),
                northboundMapping.getIncludeTimestamp(),
                northboundMapping.getUserProperties().stream()
                        .map(prop -> new MqttUserPropertyModel(prop.getName(),prop.getValue()))
                        .collect(Collectors.toList()),
                northboundMapping.getMqttQos(),
                northboundMapping.getMessageExpiryInterval());
    }
}
