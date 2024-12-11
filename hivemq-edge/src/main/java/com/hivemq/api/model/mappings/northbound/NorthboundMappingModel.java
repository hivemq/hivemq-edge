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
import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.adapter.sdk.api.config.MqttUserProperty;
import com.hivemq.api.model.JavaScriptConstants;
import com.hivemq.api.model.QoSModel;
import com.hivemq.persistence.mappings.NorthboundMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Schema(name = "NorthboundMapping")
public class NorthboundMappingModel {

    @JsonProperty(value = "topic", required = true)
    @Schema(description = "The target mqtt topic where received tags should be sent to.",
            format = " mqtt-topic",
            minLength = 1,
            maxLength = 65_535)
    private final @NotNull String topic;

    @JsonProperty(value = "tagName", required = true)
    @Schema(description = "The tag for which values hould be collected and sent out.",
            format = "mqtt-tag",
            minLength = 1)
    private final @NotNull String tagName;

    @JsonProperty(value = "messageHandlingOptions")
    @Schema(description = "How collected tags should or shouldnÖT be aggregated.", defaultValue = "MQTTMessagePerTag")
    private final @NotNull MessageHandlingOptions messageHandlingOptions;

    @JsonProperty(value = "includeTagNames")
    @Schema(description = "Should tag names be included when sent out.", defaultValue = "false")
    private final boolean includeTagNames;

    @JsonProperty(value = "includeTimestamp")
    @Schema(description = "Should the timestamp be included when sent out.", defaultValue = "false")
    private final boolean includeTimestamp;

    @JsonProperty(value = "userProperties")
    @Schema(description = "User properties to be added to each outgoing mqtt message.")
    private final @NotNull List<MqttUserPropertyModel> userProperties;

    @JsonProperty(value = "maxQoS")
    @Schema(description = "The maximum MQTT-QoS for the outgoing messages.", defaultValue = "AT_LEAST_ONCE")
    private final @NotNull QoSModel maxQoS;

    @JsonProperty(value = "messageExpiryInterval")
    @Schema(description = "The message expiry interval.",
            minimum = "0",
            maximum = "" + JavaScriptConstants.JS_MAX_SAFE_INTEGER,
            defaultValue = "" + JavaScriptConstants.JS_MAX_SAFE_INTEGER)
    private final long messageExpiryInterval;

    @JsonCreator
    public NorthboundMappingModel(
            @JsonProperty(value = "topic", required = true) final @NotNull String topic,
            @JsonProperty(value = "tagName", required = true) final @NotNull String tagName,
            @JsonProperty(value = "messageHandlingOptions") final @Nullable MessageHandlingOptions messageHandlingOptions,
            @JsonProperty(value = "includeTagNames") final @Nullable Boolean includeTagNames,
            @JsonProperty(value = "includeTimestamp") final @Nullable Boolean includeTimestamp,
            @JsonProperty(value = "userProperties") final @Nullable List<MqttUserPropertyModel> userProperties,
            @JsonProperty(value = "maxQoS") final @Nullable QoSModel maxQoS,
            @JsonProperty(value = "messageExpiryInterval") final @Nullable Long messageExpiryInterval) {
        this.topic = topic;
        this.tagName = tagName;
        this.messageHandlingOptions =
                Objects.requireNonNullElse(messageHandlingOptions, MessageHandlingOptions.MQTTMessagePerTag);
        this.includeTagNames = Objects.requireNonNullElse(includeTagNames, false);
        this.includeTimestamp = Objects.requireNonNullElse(includeTimestamp, false);
        this.userProperties = Objects.requireNonNullElse(userProperties, List.of());
        this.maxQoS = Objects.requireNonNullElse(maxQoS, QoSModel.AT_LEAST_ONCE);
        // we must set a upper limit for the expiry interval as JS otherwise will wrongly round it which leads to an exception when sending it back to the backend
        this.messageExpiryInterval = Math.min(Objects.requireNonNullElse(messageExpiryInterval, Long.MAX_VALUE),
                JavaScriptConstants.JS_MAX_SAFE_INTEGER);
    }

    public @NotNull String getTopic() {
        return topic;
    }

    public @NotNull String getTagName() {
        return tagName;
    }

    public @NotNull MessageHandlingOptions getMessageHandlingOptions() {
        return messageHandlingOptions;
    }

    public boolean isIncludeTagNames() {
        return includeTagNames;
    }

    public boolean isIncludeTimestamp() {
        return includeTimestamp;
    }

    public @NotNull List<MqttUserPropertyModel> getUserProperties() {
        return userProperties;
    }

    public long getMessageExpiryInterval() {
        return messageExpiryInterval;
    }

    public @NotNull NorthboundMapping to() {
        // re-translate the max safe js value to the max java value.
        final long messageExpiry = messageExpiryInterval == JavaScriptConstants.JS_MAX_SAFE_INTEGER ?
                Long.MAX_VALUE :
                messageExpiryInterval;

        return new NorthboundMapping(this.tagName,
                this.topic,
                this.maxQoS.getQosNumber(),
                messageExpiry,
                this.messageHandlingOptions,
                this.includeTagNames,
                this.includeTimestamp,
                userProperties.stream()
                        .map(prop -> new MqttUserProperty(prop.getName(), prop.getValue()))
                        .collect(Collectors.toList()));
    }

    public static NorthboundMappingModel from(final @NotNull NorthboundMapping northboundMapping) {
        return new NorthboundMappingModel(northboundMapping.getMqttTopic(),
                northboundMapping.getTagName(),
                northboundMapping.getMessageHandlingOptions(),
                northboundMapping.getIncludeTagNames(),
                northboundMapping.getIncludeTimestamp(),
                northboundMapping.getUserProperties()
                        .stream()
                        .map(prop -> new MqttUserPropertyModel(prop.getName(), prop.getValue()))
                        .collect(Collectors.toList()),
                QoSModel.fromNumber(northboundMapping.getMqttQos()),
                northboundMapping.getMessageExpiryInterval());
    }
}
