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

public class S7AdapterConfig implements ProtocolAdapterConfig {

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

    @JsonProperty("pollingIntervalMillis")
    @ModuleConfigField(title = "Polling Interval [ms]",
                       description = "Time in millisecond that this endpoint will be polled",
                       numberMin = 1,
                       defaultValue = "1000")
    private final int pollingIntervalMillis;

    @JsonProperty("maxPollingErrorsBeforeRemoval")
    @ModuleConfigField(title = "Max. Polling Errors",
                       description = "Max. errors polling the endpoint before the polling daemon is stopped (-1 for unlimited retries)",
                       numberMin = -1,
                       defaultValue = "10")
    private final int maxPollingErrorsBeforeRemoval;

    @JsonProperty("publishChangedDataOnly")
    @ModuleConfigField(title = "Only publish data items that have changed since last poll",
                       defaultValue = "true",
                       format = ModuleConfigField.FieldType.BOOLEAN)
    private final boolean publishChangedDataOnly;

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
                       description = "Rack value for the remote main CPU (PLC).")
    private final Integer remoteRack;

    @JsonProperty("remoteSlot")
    @ModuleConfigField(title = "Remote Slot",
                       description = "Slot value for the remote main CPU (PLC).")
    private final Integer remoteSlot;

    @JsonProperty("pduLength")
    @ModuleConfigField(title = "PDU length",
                       description = "")
    private final Integer pduLength;

    @JsonProperty(value = "s7ToMqttMappings", required = true)
    @ModuleConfigField(title = "S7 To MQTT Config",
                       description = "The configuration for a data stream from S7 to MQTT",
                       required = true)
    private final @NotNull List<S7ToMqttConfig> s7ToMqttConfig;

    @JsonCreator
    public S7AdapterConfig(
            @JsonProperty(value = "id", required = true) final @NotNull String id,
            @JsonProperty(value = "port", required = true, defaultValue = "102") final int port,
            @JsonProperty(value = "host", required = true) final @NotNull String host,
            @JsonProperty(value = "controllerType", required = true) final @NotNull ControllerType controllerType,
            @JsonProperty(value = "remoteRack") final @Nullable Integer remoteRack,
            @JsonProperty(value = "remoteSlot") final @Nullable Integer remoteSlot,
            @JsonProperty(value = "pduLength") final @Nullable Integer pduLength,
            @JsonProperty(value = "pollingIntervalMillis") final @Nullable Integer pollingIntervalMillis,
            @JsonProperty(value = "maxPollingErrorsBeforeRemoval") final @Nullable Integer maxPollingErrorsBeforeRemoval,
            @JsonProperty(value = "publishChangedDataOnly") final @Nullable Boolean publishChangedDataOnly,
            @JsonProperty(value = "s7ToMqttMappings", required = true) final @NotNull List<S7ToMqttConfig> s7ToMqttConfig) {
        this.id = id;
        this.port = port;
        this.host = host;
        this.controllerType = controllerType;
        this.pollingIntervalMillis = Objects.requireNonNullElse(pollingIntervalMillis, 1000);
        this.maxPollingErrorsBeforeRemoval = Objects.requireNonNullElse(maxPollingErrorsBeforeRemoval, 10);
        this.publishChangedDataOnly = Objects.requireNonNullElse(publishChangedDataOnly, true);
        this.remoteRack = remoteRack;
        this.remoteSlot = remoteSlot;
        this.pduLength = pduLength;
        this.s7ToMqttConfig = s7ToMqttConfig;
    }

    public int getPollingIntervalMillis() {
        return pollingIntervalMillis;
    }

    public int getMaxPollingErrorsBeforeRemoval() {
        return maxPollingErrorsBeforeRemoval;
    }

    public boolean getPublishChangedDataOnly() {
        return publishChangedDataOnly;
    }

    public int getPort() {
        return port;
    }

    public @Nullable Integer getRemoteRack() {
        return remoteRack;
    }

    public @Nullable Integer getRemoteSlot() {
        return remoteSlot;
    }

    public @Nullable Integer getPduLength() {
        return pduLength;
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

    public @NotNull List<S7ToMqttConfig> getS7ToMqttMappings() {
        return s7ToMqttConfig;
    }
}
