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
package com.hivemq.edge.adapters.modbus.config;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.config.MqttUserProperty;
import com.hivemq.adapter.sdk.api.exceptions.ProtocolAdapterException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.hivemq.adapter.sdk.api.config.MessageHandlingOptions.MQTTMessagePerSubscription;
import static java.util.Objects.requireNonNullElse;
import static java.util.Objects.requireNonNullElseGet;

public class ModbusToMqttMapping implements PollingContext {

    @JsonProperty(value = "mqttTopic", required = true)
    @ModuleConfigField(title = "Destination MQTT Topic",
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
    private final boolean includeTimestamp;

    @JsonProperty(value = "includeTagNames")
    @ModuleConfigField(title = "Include Tag Names In Publish?",
                       description = "Include the names of the tags in the resulting MQTT publish",
                       defaultValue = "false")
    private final boolean includeTagNames;

    @JsonProperty(value = "mqttUserProperties")
    @ModuleConfigField(title = "MQTT User Properties",
                       description = "Arbitrary properties to associate with the mapping",
                       arrayMaxItems = 10)
    private final @NotNull List<MqttUserProperty> userProperties;

    @JsonProperty(value = "addressRange", required = true)
    @ModuleConfigField(title = "Address Range",
                       description = "Define the start and end index values for your memory addresses",
                       required = true)
    private final @NotNull AddressRange addressRange;

    @JsonProperty("dataType")
    @ModuleConfigField(title = "Data Type", description = "Define how the read registers are interpreted", defaultValue = "INT_16")
    private final @NotNull ModbusDataType dataType;

    @JsonCreator
    public ModbusToMqttMapping(
            @JsonProperty(value = "mqttTopic", required = true) final @NotNull String mqttTopic,
            @JsonProperty(value = "mqttQos") final @Nullable Integer qos,
            @JsonProperty("messageHandlingOptions") final @Nullable MessageHandlingOptions messageHandlingOptions,
            @JsonProperty("includeTimestamp") final @Nullable Boolean includeTimestamp,
            @JsonProperty("includeTagNames") final @Nullable Boolean includeTagNames,
            @JsonProperty("mqttUserProperties") final @Nullable List<MqttUserProperty> userProperties,
            @JsonProperty(value = "addressRange", required = true) final @NotNull AddressRange addressRange,
            @JsonProperty(value = "dataType") final @Nullable ModbusDataType dataType) throws ProtocolAdapterException {
        this.mqttTopic = mqttTopic;
        this.qos = requireNonNullElse(qos, 0);
        this.messageHandlingOptions = requireNonNullElse(messageHandlingOptions, MQTTMessagePerSubscription);
        this.includeTimestamp = requireNonNullElse(includeTimestamp, true);
        this.includeTagNames = requireNonNullElse(includeTagNames, false);
        this.addressRange = addressRange;
        this.userProperties = requireNonNullElseGet(userProperties, List::of);
        this.dataType = requireNonNullElse(dataType, ModbusDataType.INT_16);

        final int registerCount = addressRange.endIdx - addressRange.startIdx;
        switch (this.dataType) {
            case INT_16:
            case UINT_16:
                if (registerCount != 1) {
                    throw new ProtocolAdapterException("The data type " +
                            this.dataType +
                            " needs exactly 1 register, but " +
                            registerCount +
                            " registers were configured.");
                }
                break;
            case INT_32:
            case UINT_32:
            case FLOAT_32:
                if (registerCount != 2) {
                    throw new ProtocolAdapterException("The data type " +
                            this.dataType +
                            " needs exactly 2 registers, but " +
                            registerCount +
                            " registers were configured.");
                }
                break;
            case INT_64:
                if (registerCount != 4) {
                    throw new ProtocolAdapterException("The data type " +
                            this.dataType +
                            " needs exactly 4 registers, but " +
                            registerCount +
                            " registers were configured.");
                }
                break;
            case UTF_8:
            default:
        }
    }

    public @NotNull AddressRange getAddressRange() {
        return addressRange;
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

    public @NotNull ModbusDataType getDataType() {
        return dataType;
    }
}
