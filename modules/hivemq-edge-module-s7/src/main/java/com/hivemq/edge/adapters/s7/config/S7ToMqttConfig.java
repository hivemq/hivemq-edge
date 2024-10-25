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
package com.hivemq.edge.adapters.s7.config;

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

public class S7ToMqttConfig implements PollingContext {

    public static final String PROPERTY_MQTT_TOPIC = "mqttTopic";
    public static final String PROPERTY_MQTT_QOS = "mqttQos";
    public static final String PROPERTY_MESSAGE_HANDLING_OPTIONS = "messageHandlingOptions";
    public static final String PROPERTY_INCLUDE_TIMESTAMP = "includeTimestamp";
    public static final String PROPERTY_INCLUDE_TAG_NAMES = "includeTagNames";
    public static final String PROPERTY_TAG_NAME = "tagName";
    public static final String PROPERTY_TAG_ADDRESS = "tagAddress";
    public static final String PROPERTY_DATA_TYPE = "dataType";
    public static final String PROPERTY_MQTT_USER_PROPERTIES = "mqttUserProperties";

    @JsonProperty(value = PROPERTY_MQTT_TOPIC, required = true)
    @ModuleConfigField(title = "Destination MQTT Topic",
                       description = "The topic to publish data on",
                       required = true,
                       format = ModuleConfigField.FieldType.MQTT_TOPIC)
    private final @NotNull String mqttTopic;

    @JsonProperty(value = PROPERTY_MQTT_QOS)
    @ModuleConfigField(title = "MQTT QoS",
                       description = "MQTT Quality of Service level",
                       numberMin = 0,
                       numberMax = 2,
                       defaultValue = "0")
    private final int qos;

    @JsonProperty(value = PROPERTY_MESSAGE_HANDLING_OPTIONS)
    @ModuleConfigField(title = "Message Handling Options",
                       description = "This setting defines the format of the resulting MQTT message, either a message per changed tag or a message per subscription that may include multiple data points per sample",
                       enumDisplayValues = {
                               "MQTT Message Per Device Tag",
                               "MQTT Message Per Subscription (Potentially Multiple Data Points Per Sample)"},
                       defaultValue = "MQTTMessagePerTag")
    private final @NotNull MessageHandlingOptions messageHandlingOptions;

    @JsonProperty(value = PROPERTY_INCLUDE_TIMESTAMP)
    @ModuleConfigField(title = "Include Sample Timestamp In Publish?",
                       description = "Include the unix timestamp of the sample time in the resulting MQTT message",
                       defaultValue = "true")
    private boolean includeTimestamp;

    @JsonProperty(value = PROPERTY_INCLUDE_TAG_NAMES)
    @ModuleConfigField(title = "Include Tag Names In Publish?",
                       description = "Include the names of the tags in the resulting MQTT publish",
                       defaultValue = "false")
    private boolean includeTagNames;

    @JsonProperty(value = PROPERTY_MQTT_USER_PROPERTIES)
    @ModuleConfigField(title = "MQTT User Properties",
                       description = "Arbitrary properties to associate with the mapping",
                       arrayMaxItems = 10)
    private @NotNull List<MqttUserProperty> userProperties;

    @JsonProperty(value = PROPERTY_TAG_NAME, required = true)
    @ModuleConfigField(title = "Tag Name",
                       description = "The name to assign to this address. The tag name must be unique for all subscriptions within this protocol adapter.",
                       required = true,
                       format = ModuleConfigField.FieldType.IDENTIFIER)
    private final @NotNull String tagName;

    @JsonProperty(value = PROPERTY_TAG_ADDRESS, required = true)
    @ModuleConfigField(title = "Tag Address",
                       description = "The well formed address of the tag to read",
                       required = true)
    private final @NotNull String tagAddress;

    @JsonProperty(value = PROPERTY_DATA_TYPE, required = true)
    @ModuleConfigField(title = "Data Type", description = "The expected data type of the tag", enumDisplayValues = {
            "Boolean",
            "Byte",
            "Int16",
            "UInt16",
            "Int32",
            "UInt32",
            "Int64",
            "Real (float 32)",
            "LReal (double 64)",
            "String",
            "Date (DateStamp)",
            "Time Of Day (TimeStamp)",
            "Date Time (DateTimeStamp)",
            "Timing (Duration ms)"}, required = true)
    private final @NotNull S7DataType dataType;

    @JsonCreator
    public S7ToMqttConfig(
            @JsonProperty(value = PROPERTY_MQTT_TOPIC, required = true) final @NotNull String mqttTopic,
            @JsonProperty(value = PROPERTY_MQTT_QOS) final @Nullable Integer qos,
            @JsonProperty(value = PROPERTY_MESSAGE_HANDLING_OPTIONS) final @Nullable MessageHandlingOptions messageHandlingOptions,
            @JsonProperty(value = PROPERTY_INCLUDE_TIMESTAMP) final @Nullable Boolean includeTimestamp,
            @JsonProperty(value = PROPERTY_INCLUDE_TAG_NAMES) final @Nullable Boolean includeTagNames,
            @JsonProperty(value = PROPERTY_TAG_NAME) final @NotNull String tagName,
            @JsonProperty(value = PROPERTY_TAG_ADDRESS, required = true) final @NotNull String tagAddress,
            @JsonProperty(value = PROPERTY_DATA_TYPE, required = true) final @NotNull S7DataType dataType,
            @JsonProperty(value = PROPERTY_MQTT_USER_PROPERTIES) final @Nullable List<MqttUserProperty> userProperties) {
        this.mqttTopic = mqttTopic;
        this.qos = requireNonNullElse(qos, 0);
        this.messageHandlingOptions = requireNonNullElse(messageHandlingOptions, MQTTMessagePerTag);
        this.includeTimestamp = requireNonNullElse(includeTimestamp, true);
        this.includeTagNames = requireNonNullElse(includeTagNames, false);
        this.userProperties = requireNonNullElseGet(userProperties, List::of);
        this.tagName = tagName;
        this.tagAddress = tagAddress;
        this.dataType = dataType;
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

    public @NotNull String getTagName() {
        return tagName;
    }

    public @NotNull String getTagAddress() {
        return tagAddress;
    }

    public @NotNull S7DataType getDataType() {
        return dataType;
    }
}
