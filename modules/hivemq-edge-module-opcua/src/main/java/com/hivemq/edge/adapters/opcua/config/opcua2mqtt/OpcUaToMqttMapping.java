/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.opcua.config.opcua2mqtt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.adapter.sdk.api.config.MqttUserProperty;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import static com.hivemq.adapter.sdk.api.config.MessageHandlingOptions.MQTTMessagePerTag;
import static java.util.Objects.requireNonNullElse;
import static java.util.Objects.requireNonNullElseGet;

public class OpcUaToMqttMapping implements PollingContext {

    @JsonProperty("messageExpiryInterval")
    @ModuleConfigField(title = "MQTT message expiry interval [s]",
                       description = "Time in seconds until an MQTT publish message expires",
                       numberMin = 1,
                       numberMax = 4294967295L)
    private final long messageExpiryInterval;

    @JsonProperty(value = "tagName", required = true)
    @ModuleConfigField(title = "Tag Name", description = "The name of the tag that defines the data point on the plc.",
                       required = true,
                       format = ModuleConfigField.FieldType.IDENTIFIER)
    private final @NotNull String tagName;

    @JsonProperty(value = "mqttTopic", required = true)
    @ModuleConfigField(title = "Destination Mqtt Topic",
                       description = "The topic to publish data on",
                       required = true,
                       format = ModuleConfigField.FieldType.MQTT_TOPIC)
    private final @NotNull String mqttTopic;

    @JsonProperty(value = "mqttQos")
    @ModuleConfigField(title = "MQTT QoS",
                       description = "MQTT Quality of Service level",
                       numberMin = 0,
                       numberMax = 2,
                       defaultValue = "0")
    private final int qos;

    @JsonCreator
    public OpcUaToMqttMapping(
            @JsonProperty(value = "mqttTopic", required = true) final @NotNull String mqttTopic,
            @JsonProperty(value = "mqttQos") final @Nullable Integer qos,
            @JsonProperty(value = "messageExpiryInterval") final @Nullable Long messageExpiryInterval,
            @JsonProperty(value = "tagName", required = true) final @NotNull String tagName) {
        this.mqttTopic = mqttTopic;
        this.qos = requireNonNullElse(qos, 1);
        this.tagName = tagName;
        this.messageExpiryInterval = requireNonNullElse(messageExpiryInterval, Long.MAX_VALUE);
    }

    public @NotNull String getTagName() {
        return tagName;
    }

    @Override
    public @NotNull String getMqttTopic() {
        return mqttTopic;
    }

    @Override
    public int getMqttQos() {
        return qos;
    }

    @Override
    public @NotNull MessageHandlingOptions getMessageHandlingOptions() {
        return MQTTMessagePerTag;
    }

    @Override
    public @NotNull Boolean getIncludeTimestamp() {
        return false;
    }

    @Override
    public @NotNull Boolean getIncludeTagNames() {
        return false;
    }

    @Override
    public @NotNull List<MqttUserProperty> getUserProperties() {
        return List.of();
    }

    @Override
    public @Nullable Long getMessageExpiryInterval() {
        return messageExpiryInterval;
    }
}
