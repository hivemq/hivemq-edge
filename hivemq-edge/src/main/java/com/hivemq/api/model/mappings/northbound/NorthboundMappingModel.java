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

    @JsonProperty(value = "tagName", required = true)
    @Schema(description = "The tag for which values should be collected and sent out.", format = "mqtt-tag")
    private final @NotNull String tagName;

    @JsonProperty(value = "topic", required = true)
    @Schema(description = "The target mqtt topic where received tags should be sent to.")
    private final @NotNull String topic;

    @JsonProperty(value = "includeTagNames", required = true)
    @Schema(description = "Should tag names be included when sent out.")
    private final boolean includeTagNames;

    @JsonProperty(value = "includeTimestamp", required = true)
    @Schema(description = "Should the timestamp be included when sent out.")
    private final boolean includeTimestamp;

    @JsonProperty(value = "userProperties")
    @Schema(description = "User properties to be added to each outgoing mqtt message.")
    private final @NotNull List<MqttUserPropertyModel> userProperties;

    @JsonProperty(value = "maxQoS", required = true)
    @Schema(description = "The maximum MQTT-QoS for the outgoing messages.")
    private final @NotNull QoSModel maxQoS;

    @JsonProperty(value = "messageExpiryInterval", required = true)
    @Schema(description = "The message expiry interval.")
    private final long messageExpiryInterval;

    @JsonCreator
    public NorthboundMappingModel(
            @JsonProperty(value = "tagName", required = true) final @NotNull String tagName,
            @JsonProperty(value = "topic", required = true) final @NotNull String topic,
            @JsonProperty(value = "includeTagNames") final @Nullable Boolean includeTagNames,
            @JsonProperty(value = "includeTimestamp") final @Nullable Boolean includeTimestamp,
            @JsonProperty(value = "userProperties") final @Nullable List<MqttUserPropertyModel> userProperties,
            @JsonProperty(value = "maxQoS") final @Nullable QoSModel maxQoS,
            @JsonProperty(value = "messageExpiryInterval") final @Nullable Long messageExpiryInterval) {
        this.tagName = tagName;
        this.topic = topic;
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
                mapping.getIncludeTagNames(),
                mapping.getIncludeTimestamp(),
                mapping.getUserProperties().stream().map(NorthboundMappingModel::userProp).toList(),
                QoSModel.fromNumber(mapping.getMqttQos()),
                mapping.getMessageExpiryInterval());
    }

    public static NorthboundMappingModel fromEntity(final @NotNull NorthboundMappingEntity northboundMapping) {
        return new NorthboundMappingModel(northboundMapping.getTagName(),
                northboundMapping.getTopic(),
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
