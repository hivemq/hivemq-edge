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
import com.hivemq.adapter.sdk.api.config.ProtocolAdapterConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import static com.hivemq.adapter.sdk.api.config.MessageHandlingOptions.MQTTMessagePerTag;
import static java.util.Objects.requireNonNullElse;

public class S7AdapterConfig implements ProtocolAdapterConfig, PollingContext {

    private static final @NotNull String ID_REGEX = "^([a-zA-Z_0-9-_])*$";

    private static final int PORT_MIN = 1;
    private static final int PORT_MAX = 65535;

    public enum ControllerType {
        S7_200,
        S7_200_SMART,
        S7_300,
        S7_400,
        S7_1200,
        S7_1500,
        SINUMERIK_828D
    }

    @JsonProperty(value = "id", required = true)
    @ModuleConfigField(title = "Identifier",
                       description = "Unique identifier for this protocol adapter",
                       format = ModuleConfigField.FieldType.IDENTIFIER,
                       required = true,
                       stringPattern = ID_REGEX,
                       stringMinLength = 1,
                       stringMaxLength = 1024)
    private final @NotNull String id;

    @JsonProperty(value = "port", required = true)
    @ModuleConfigField(title = "Port",
                       description = "The port number on the device to connect to",
                       required = true,
                       numberMin = PORT_MIN,
                       numberMax = PORT_MAX)
    private final int port;

    @JsonProperty(value = "host", required = true)
    @ModuleConfigField(title = "Host",
                       description = "IP Address or hostname of the device you wish to connect to",
                       required = true,
                       format = ModuleConfigField.FieldType.HOSTNAME)
    private final @NotNull String host;

    @JsonProperty(value = "controllerType", required = true)
    @ModuleConfigField(title = "S7 Controller Type",
                       description = "The type of the S7 Controller",
                       required = true,
                       defaultValue = "S7_300")
    private final @NotNull S7AdapterConfig.ControllerType controllerType;

    @JsonProperty("remoteRack")
    @ModuleConfigField(title = "Remote Rack",
                       description = "Rack value for the remote main CPU (PLC).",
                       defaultValue = "0")
    private final int remoteRack;

    @JsonProperty("remoteRack2")
    @ModuleConfigField(title = "Remote Rack 2",
                       description = "Rack value for the remote secondary CPU (PLC).",
                       defaultValue = "0")
    private final int remoteRack2;

    @JsonProperty("remoteSlot")
    @ModuleConfigField(title = "Remote Slot",
                       description = "Slot value for the remote main CPU (PLC).",
                       defaultValue = "0")
    private final int remoteSlot;

    @JsonProperty("remoteSlot2")
    @ModuleConfigField(title = "Remote Slot 2",
                       description = "Slot value for the remote secondary CPU (PLC).",
                       defaultValue = "0")
    private final int remoteSlot2;

    @JsonProperty("remoteTsap")
    @ModuleConfigField(title = "Remote TSAP",
                       description = "Remote TSAP value. The TSAP (Transport Services Access Point) mechanism is used as a further addressing level in the S7 PLC network. Usually only required for PLC from the LOGO series.",
                       defaultValue = "0")
    private final int remoteTsap;

    @JsonProperty(value = "s7ToMqtt", required = true)
    @ModuleConfigField(title = "S7 To MQTT Config",
                       description = "The configuration for a data stream from S7 to MQTT",
                       required = true)
    private final @NotNull S7ToMqttConfig s7ToMqttConfig;

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

    @JsonCreator
    public S7AdapterConfig(
            @JsonProperty(value = "id", required = true) final @NotNull String id,
            @JsonProperty(value = "port", required = true) final int port,
            @JsonProperty(value = "host", required = true) final @NotNull String host,
            @JsonProperty(value = "controllerType", required = true) final @NotNull ControllerType controllerType,
            @JsonProperty(value = "remoteRack") final @Nullable Integer remoteRack,
            @JsonProperty(value = "remoteRack2") final @Nullable Integer remoteRack2,
            @JsonProperty(value = "remoteSlot") final @Nullable Integer remoteSlot,
            @JsonProperty(value = "remoteSlot2") final @Nullable Integer remoteSlot2,
            @JsonProperty(value = "remoteTsap") final @Nullable Integer remoteTsap,
            @JsonProperty(value = "s7ToMqtt", required = true) final @NotNull S7ToMqttConfig s7ToMqttConfig,
            @JsonProperty(value = "mqttTopic", required = true) final @NotNull String mqttTopic,
            @JsonProperty(value = "mqttQos") final @Nullable Integer qos,
            @JsonProperty(value = "messageHandlingOptions") final @Nullable MessageHandlingOptions messageHandlingOptions,
            @JsonProperty(value = "includeTimestamp") final @Nullable Boolean includeTimestamp,
            @JsonProperty(value = "includeTagNames") final @Nullable Boolean includeTagNames,
            @JsonProperty(value = "mqttUserProperties") final @Nullable List<MqttUserProperty> userProperties) {
        this.id = id;
        this.port = port;
        this.host = host;
        this.controllerType = controllerType;
        this.remoteRack = Objects.requireNonNullElse(remoteRack, 0);
        this.remoteRack2 = Objects.requireNonNullElse(remoteRack2, 0);
        this.remoteSlot = Objects.requireNonNullElse(remoteSlot, 0);
        this.remoteSlot2 = Objects.requireNonNullElse(remoteSlot2, 0);
        this.remoteTsap = Objects.requireNonNullElse(remoteTsap, 0);
        this.s7ToMqttConfig = s7ToMqttConfig;
        this.mqttTopic = mqttTopic;
        this.qos = requireNonNullElse(qos, 0);
        this.messageHandlingOptions = requireNonNullElse(messageHandlingOptions, MQTTMessagePerTag);
        this.includeTimestamp = requireNonNullElse(includeTimestamp, true);
        this.includeTagNames = requireNonNullElse(includeTagNames, false);
        this.userProperties = requireNonNullElse(userProperties, List.of());

    }

    public int getPort() {
        return port;
    }

    public int getRemoteRack() {
        return remoteRack;
    }

    public int getRemoteRack2() {
        return remoteRack2;
    }

    public int getRemoteSlot() {
        return remoteSlot;
    }

    public int getRemoteSlot2() {
        return remoteSlot2;
    }

    public int getRemoteTsap() {
        return remoteTsap;
    }

    public @NotNull ControllerType getControllerType() {
        return controllerType;
    }

    @Override
    public @NotNull String getId() {
        return id;
    }

    public @NotNull String getHost() {
        return host;
    }

    public @NotNull S7ToMqttConfig getS7ToMqttConfig() {
        return s7ToMqttConfig;
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
