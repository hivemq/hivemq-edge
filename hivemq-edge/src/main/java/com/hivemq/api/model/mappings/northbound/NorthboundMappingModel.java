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
import com.hivemq.api.model.QoSModel;
import com.hivemq.configuration.entity.adapter.MqttUserPropertyEntity;
import com.hivemq.configuration.entity.adapter.NorthboundMappingEntity;
import com.hivemq.persistence.mappings.NorthboundMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.hivemq.api.model.JavaScriptConstants.JS_MAX_SAFE_INTEGER;
import static java.util.Objects.requireNonNullElse;

@Schema(name = "NorthboundMapping")
public class NorthboundMappingModel {

    private static final @NotNull String TAG_NAME = "tagName";
    private static final @NotNull String TOPIC = "topic";
    private static final @NotNull String MAX_QoS = "maxQoS";
    private static final @NotNull String MESSAGE_HANDLING_OPTIONS = "messageHandlingOptions";
    private static final @NotNull String INCLUDE_TAG_NAMES = "includeTagNames";
    private static final @NotNull String INCLUDE_TIMESTAMP = "includeTimestamp";
    private static final @NotNull String USER_PROPERTIES = "userProperties";
    private static final @NotNull String MESSAGE_EXPIRY_INTERVAL = "messageExpiryInterval";

    @JsonProperty(value = TAG_NAME, required = true)
    @Schema(description = "The tag for which values should be collected and sent out.", format = "mqtt-tag")
    private final @NotNull String tagName;

    @JsonProperty(value = TOPIC, required = true)
    @Schema(description = "The target mqtt topic where received tags should be sent to.")
    private final @NotNull String topic;

    @JsonProperty(value = MESSAGE_HANDLING_OPTIONS, required = true)
    @Schema(description = "How collected tags should or shouldn't be aggregated.")
    private final @NotNull MessageHandlingOptions messageHandlingOptions;

    @JsonProperty(value = INCLUDE_TAG_NAMES, required = true)
    @Schema(description = "Should tag names be included when sent out.")
    private final boolean includeTagNames;

    @JsonProperty(value = INCLUDE_TIMESTAMP, required = true)
    @Schema(description = "Should the timestamp be included when sent out.")
    private final boolean includeTimestamp;

    @JsonProperty(value = USER_PROPERTIES)
    @Schema(description = "User properties to be added to each outgoing mqtt message.")
    private final @NotNull List<MqttUserPropertyModel> userProperties;

    @JsonProperty(value = MAX_QoS, required = true)
    @Schema(description = "The maximum MQTT-QoS for the outgoing messages.")
    private final @NotNull QoSModel maxQoS;

    @JsonProperty(value = MESSAGE_EXPIRY_INTERVAL, required = true)
    @Schema(description = "The message expiry interval.")
    private final long messageExpiryInterval;

    @JsonCreator
    public NorthboundMappingModel(
            @JsonProperty(value = TAG_NAME, required = true) final @NotNull String tagName,
            @JsonProperty(value = TOPIC, required = true) final @NotNull String topic,
            @JsonProperty(value = MESSAGE_HANDLING_OPTIONS) final @Nullable MessageHandlingOptions messageHandlingOptions,
            @JsonProperty(value = INCLUDE_TAG_NAMES) final @Nullable Boolean includeTagNames,
            @JsonProperty(value = INCLUDE_TIMESTAMP) final @Nullable Boolean includeTimestamp,
            @JsonProperty(value = USER_PROPERTIES) final @Nullable List<MqttUserPropertyModel> userProperties,
            @JsonProperty(value = MAX_QoS) final @Nullable QoSModel maxQoS,
            @JsonProperty(value = MESSAGE_EXPIRY_INTERVAL) final @Nullable Long messageExpiryInterval) {
        this.tagName = tagName;
        this.topic = topic;
        this.messageHandlingOptions =
                requireNonNullElse(messageHandlingOptions, MessageHandlingOptions.MQTTMessagePerTag);
        this.includeTagNames = requireNonNullElse(includeTagNames, false);
        this.includeTimestamp = requireNonNullElse(includeTimestamp, false);
        this.userProperties = requireNonNullElse(userProperties, List.of());
        this.maxQoS = requireNonNullElse(maxQoS, QoSModel.AT_LEAST_ONCE);
        // we must set a upper limit for the expiry interval as JS otherwise will wrongly 
        // round it which leads to an exception when sending it back to the backend.
        this.messageExpiryInterval =
                Math.min(requireNonNullElse(messageExpiryInterval, Long.MAX_VALUE), JS_MAX_SAFE_INTEGER);
    }

    public static NorthboundMappingModel fromPersistence(final @NotNull NorthboundMapping mapping) {
        return new NorthboundMappingModel(mapping.getTagName(),
                mapping.getMqttTopic(),
                mapping.getMessageHandlingOptions(),
                mapping.getIncludeTagNames(),
                mapping.getIncludeTimestamp(),
                mapping.getUserProperties().stream().map(NorthboundMappingModel::userProp).toList(),
                QoSModel.fromNumber(mapping.getMqttQos()),
                mapping.getMessageExpiryInterval());
    }

    public static NorthboundMappingModel fromEntity(final @NotNull NorthboundMappingEntity northboundMapping) {
        return new NorthboundMappingModel(northboundMapping.getTagName(),
                northboundMapping.getTopic(),
                northboundMapping.getMessageHandlingOptions(),
                northboundMapping.isIncludeTagNames(),
                northboundMapping.isIncludeTimestamp(),
                northboundMapping.getUserProperties().stream().map(NorthboundMappingModel::userProp).toList(),
                QoSModel.fromNumber(northboundMapping.getMaxQoS()),
                northboundMapping.getMessageExpiryInterval());
    }

    private static @NotNull MqttUserProperty userProp(final @NotNull MqttUserPropertyModel p) {
        return new MqttUserProperty(p.getName(), p.getValue());
    }

    private static @NotNull MqttUserPropertyModel userProp(final @NotNull MqttUserPropertyEntity p) {
        return new MqttUserPropertyModel(p.getName(), p.getValue());
    }

    private static @NotNull MqttUserPropertyModel userProp(final @NotNull MqttUserProperty p) {
        return new MqttUserPropertyModel(p.getName(), p.getValue());

    }

    public @NotNull NorthboundMapping toPersistence() {
        // re-translate the max safe js value to the max java value.
        return new NorthboundMapping(tagName,
                topic,
                maxQoS.getQosNumber(),
                messageHandlingOptions,
                includeTagNames,
                includeTimestamp,
                userProperties.stream().map(NorthboundMappingModel::userProp).toList(),
                messageExpiryInterval == JS_MAX_SAFE_INTEGER ? Long.MAX_VALUE : messageExpiryInterval);
    }

    public @NotNull String getTagName() {
        return tagName;
    }

    public @NotNull String getTopic() {
        return topic;
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
}
