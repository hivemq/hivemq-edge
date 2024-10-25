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
import com.hivemq.adapter.sdk.api.config.ProtocolAdapterConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class S7AdapterConfig implements ProtocolAdapterConfig {

    private static final @NotNull String ID_REGEX = "^([a-zA-Z_0-9-_])*$";

    private static final int PORT_MIN = 1;
    private static final int PORT_MAX = 65535;

    public static final String DEFAULT_POLLING_INTERVAL_MS = "1000";
    public static final String DEFAULT_MAX_POLLING_ERRORS = "10";
    public static final String DEFAULT_PUBLISH_CHANGED_DATA_ONLY = "true";
    public static final String DEFAULT_S7_PORT = "102";
    public static final String DEFAULT_CONTROLER_TYPE = "S7_300";

    public static final String PROPERTY_ID = "id";
    public static final String PROPERTY_PORT = "port";
    public static final String PROPERTY_HOST = "host";
    public static final String PROPERTY_CONTROLLER_TYPE = "controllerType";
    public static final String PROPERTY_REMOTE_RACK = "remoteRack";
    public static final String PROPERTY_REMOTE_SLOT = "remoteSlot";
    public static final String PROPERTY_PDU_LENGTH = "pduLength";
    public static final String PROPERTY_POLLING_INTERVAL_MILLIS = "pollingIntervalMillis";
    public static final String PROPERTY_MAX_POLLING_ERRORS_BEFORE_REMOVAL = "maxPollingErrorsBeforeRemoval";
    public static final String PROPERTY_PUBLISH_CHANGED_DATA_ONLY = "publishChangedDataOnly";
    public static final String PROPERTY_S_7_TO_MQTT_MAPPINGS = "s7ToMqttMappings";

    public enum ControllerType {
        S7_200,
        S7_200_SMART,
        S7_300,
        S7_400,
        S7_1200,
        S7_1500,
        SINUMERIK_828D
    }

    @JsonProperty(PROPERTY_POLLING_INTERVAL_MILLIS)
    @ModuleConfigField(title = "Polling Interval [ms]",
                       description = "Time in millisecond that this endpoint will be polled",
                       numberMin = 1,
                       defaultValue = DEFAULT_POLLING_INTERVAL_MS)
    private final int pollingIntervalMillis;

    @JsonProperty(PROPERTY_MAX_POLLING_ERRORS_BEFORE_REMOVAL)
    @ModuleConfigField(title = "Max. Polling Errors",
                       description = "Max. errors polling the endpoint before the polling daemon is stopped (-1 for unlimited retries)",
                       numberMin = -1,
                       defaultValue = DEFAULT_MAX_POLLING_ERRORS)
    private final int maxPollingErrorsBeforeRemoval;

    @JsonProperty(PROPERTY_PUBLISH_CHANGED_DATA_ONLY)
    @ModuleConfigField(title = "Only publish data items that have changed since last poll",
                       defaultValue = DEFAULT_PUBLISH_CHANGED_DATA_ONLY,
                       format = ModuleConfigField.FieldType.BOOLEAN)
    private final boolean publishChangedDataOnly;

    @JsonProperty(value = PROPERTY_ID, required = true)
    @ModuleConfigField(title = "Identifier",
                       description = "Unique identifier for this protocol adapter",
                       format = ModuleConfigField.FieldType.IDENTIFIER,
                       required = true,
                       stringPattern = ID_REGEX,
                       stringMinLength = 1,
                       stringMaxLength = 1024)
    private final @NotNull String id;

    @JsonProperty(value = PROPERTY_PORT, required = true)
    @ModuleConfigField(title = "Port",
                       description = "The port number on the device to connect to",
                       required = true,
                       defaultValue = DEFAULT_S7_PORT,
                       numberMin = PORT_MIN,
                       numberMax = PORT_MAX)
    private final int port;

    @JsonProperty(value = PROPERTY_HOST, required = true)
    @ModuleConfigField(title = "Host",
                       description = "IP Address or hostname of the device you wish to connect to",
                       required = true,
                       format = ModuleConfigField.FieldType.HOSTNAME)
    private final @NotNull String host;

    @JsonProperty(value = PROPERTY_CONTROLLER_TYPE, required = true)
    @ModuleConfigField(title = "S7 Controller Type",
                       description = "The type of the S7 Controller",
                       required = true,
                       defaultValue = DEFAULT_CONTROLER_TYPE)
    private final @NotNull S7AdapterConfig.ControllerType controllerType;

    @JsonProperty(PROPERTY_REMOTE_RACK)
    @ModuleConfigField(title = "Remote Rack",
                       description = "Rack value for the remote main CPU (PLC).")
    private final Integer remoteRack;

    @JsonProperty(PROPERTY_REMOTE_SLOT)
    @ModuleConfigField(title = "Remote Slot",
                       description = "Slot value for the remote main CPU (PLC).")
    private final Integer remoteSlot;

    @JsonProperty(PROPERTY_PDU_LENGTH)
    @ModuleConfigField(title = "PDU length",
                       description = "")
    private final Integer pduLength;

    @JsonProperty(value = PROPERTY_S_7_TO_MQTT_MAPPINGS, required = true)
    @ModuleConfigField(title = "S7 To MQTT Config",
                       description = "The configuration for a data stream from S7 to MQTT",
                       required = true)
    private final @NotNull List<S7ToMqttConfig> s7ToMqttConfig;

    @JsonCreator
    public S7AdapterConfig(
            @JsonProperty(value = PROPERTY_ID, required = true) final @NotNull String id,
            @JsonProperty(value = PROPERTY_PORT, required = true) final Integer port,
            @JsonProperty(value = PROPERTY_HOST, required = true) final @NotNull String host,
            @JsonProperty(value = PROPERTY_CONTROLLER_TYPE, required = true) final @NotNull ControllerType controllerType,
            @JsonProperty(value = PROPERTY_REMOTE_RACK) final @Nullable Integer remoteRack,
            @JsonProperty(value = PROPERTY_REMOTE_SLOT) final @Nullable Integer remoteSlot,
            @JsonProperty(value = PROPERTY_PDU_LENGTH) final @Nullable Integer pduLength,
            @JsonProperty(value = PROPERTY_POLLING_INTERVAL_MILLIS) final @Nullable Integer pollingIntervalMillis,
            @JsonProperty(value = PROPERTY_MAX_POLLING_ERRORS_BEFORE_REMOVAL) final @Nullable Integer maxPollingErrorsBeforeRemoval,
            @JsonProperty(value = PROPERTY_PUBLISH_CHANGED_DATA_ONLY) final @Nullable Boolean publishChangedDataOnly,
            @JsonProperty(value = PROPERTY_S_7_TO_MQTT_MAPPINGS, required = true) final @NotNull List<S7ToMqttConfig> s7ToMqttConfig) {
        this.id = id;
        this.host = host;
        this.controllerType = controllerType;
        this.port = port;
        this.pollingIntervalMillis = Objects.requireNonNullElse(pollingIntervalMillis,
                Integer.valueOf(DEFAULT_POLLING_INTERVAL_MS));
        this.maxPollingErrorsBeforeRemoval = Objects.requireNonNullElse(maxPollingErrorsBeforeRemoval,
                Integer.valueOf(DEFAULT_MAX_POLLING_ERRORS));
        this.publishChangedDataOnly = Objects.requireNonNullElse(publishChangedDataOnly,
                Boolean.valueOf(DEFAULT_PUBLISH_CHANGED_DATA_ONLY));
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
