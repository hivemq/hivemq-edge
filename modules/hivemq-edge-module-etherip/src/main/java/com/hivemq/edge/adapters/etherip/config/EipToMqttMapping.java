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
package com.hivemq.edge.adapters.etherip.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.config.MqttUserProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.hivemq.adapter.sdk.api.config.MessageHandlingOptions.MQTTMessagePerTag;
import static java.util.Objects.requireNonNullElse;
import static java.util.Objects.requireNonNullElseGet;

public class EipToMqttMapping implements PollingContext {

    @JsonProperty(value = "tagName", required = true)
    @ModuleConfigField(title = "Tag Name",
                       description = "The name to assign to this address. The tag name must be unique for all subscriptions within this protocol adapter.",
                       required = true,
                       format = ModuleConfigField.FieldType.IDENTIFIER)
    private final @NotNull String tagName;

    @JsonProperty("tagAddress")
    @ModuleConfigField(title = "Tag Address",
                       description = "The well formed address of the tag to read",
                       required = true)
    private final @NotNull String tagAddress;

    @JsonProperty(value = "mqttTopic", required = true)
    @ModuleConfigField(title = "Destination Mqtt Topic",
                       description = "The topic to publish data on",
                       required = true,
                       format = ModuleConfigField.FieldType.MQTT_TOPIC)
    private final @NotNull String mqttTopic;

    @JsonProperty(value = "mqttQos")
    @ModuleConfigField(title = "QoS",
                       description = "MQTT Quality of Service level",
                       numberMin = 0,
                       numberMax = 2,
                       defaultValue = "0")
    private final int qos;

    @JsonProperty(value = "messageHandlingOptions")
    @ModuleConfigField(title = "Message Handling Options",
                       description = "This setting defines the format of the resulting MQTT message, either a message per changed tag or a message per subscription that may include multiple data points per sample",
                       enumDisplayValues = {
                               "MQTT Message Per Device Tag",
                               "MQTT Message Per Subscription (Potentially Multiple Data Points Per Sample)"},
                       defaultValue = "MQTTMessagePerTag")
    private final @NotNull MessageHandlingOptions messageHandlingOptions;

    @JsonProperty(value = "includeTimestamp")
    @ModuleConfigField(title = "Include Sample Timestamp In Publish?",
                       description = "Include the unix timestamp of the sample time in the resulting MQTT message",
                       defaultValue = "true")
    private final @NotNull Boolean includeTimestamp;

    @JsonProperty(value = "includeTagNames")
    @ModuleConfigField(title = "Include Tag Names In Publish?",
                       description = "Include the names of the tags in the resulting MQTT publish",
                       defaultValue = "false")
    private final @NotNull Boolean includeTagNames;


    @JsonProperty("dataType")
    @ModuleConfigField(title = "Data Type", description = "The expected data type of the tag", enumDisplayValues = {
            "Bool",
            "DInt",
            "Int",
            "LInt",
            "LReal",
            "LTime",
            "Real",
            "SInt",
            "String",
            "Time",
            "UDInt",
            "UInt",
            "ULInt",
            "USInt"}, required = true)
    private final @NotNull EipDataType dataType;

    @JsonProperty(value = "mqttUserProperties")
    @ModuleConfigField(title = "User Properties",
                       description = "Arbitrary properties to associate with the mapping",
                       arrayMaxItems = 10)
    private final @NotNull List<MqttUserProperty> userProperties;

    @JsonCreator
    public EipToMqttMapping(
            @JsonProperty(value = "mqttTopic", required = true) final @NotNull String mqttTopic,
            @JsonProperty(value = "mqttQos") final @Nullable Integer qos,
            @JsonProperty("messageHandlingOptions") final @Nullable MessageHandlingOptions messageHandlingOptions,
            @JsonProperty("includeTimestamp") final @Nullable Boolean includeTimestamp,
            @JsonProperty("includeTagNames") final @Nullable Boolean includeTagNames,
            @JsonProperty(value = "tagName", required = true) final @NotNull String tagName,
            @JsonProperty(value = "tagAddress", required = true) final @NotNull String tagAddress,
            @JsonProperty(value = "dataType", required = true) final @NotNull EipDataType dataType,
            @JsonProperty("mqttUserProperties") final @Nullable List<MqttUserProperty> userProperties) {
        this.mqttTopic = mqttTopic;
        this.qos = requireNonNullElse(qos, 0);
        this.messageHandlingOptions = requireNonNullElse(messageHandlingOptions, MQTTMessagePerTag);
        this.includeTimestamp = requireNonNullElse(includeTimestamp, true);
        this.includeTagNames = requireNonNullElse(includeTagNames, false);
        this.tagName = tagName;
        this.tagAddress = tagAddress;
        this.dataType = dataType;
        this.userProperties = requireNonNullElseGet(userProperties, List::of);
    }

    public @NotNull String getTagName() {
        return tagName;
    }

    public @NotNull String getTagAddress() {
        return tagAddress;
    }

    public @NotNull EipDataType getDataType() {
        return dataType;
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
        return messageHandlingOptions;
    }

    @Override
    public @NotNull Boolean getIncludeTimestamp() {
        return includeTimestamp;
    }

    @Override
    public @NotNull Boolean getIncludeTagNames() {
        return includeTagNames;
    }

    @Override
    public @NotNull List<MqttUserProperty> getUserProperties() {
        return userProperties;
    }
}