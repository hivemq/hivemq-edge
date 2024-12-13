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
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class S7AdapterConfig implements ProtocolSpecificAdapterConfig {

    private static final @NotNull String ID_REGEX = "^([a-zA-Z_0-9-_])*$";

    private static final int PORT_MIN = 1;
    private static final int PORT_MAX = 65535;

    public static final String DEFAULT_S7_PORT = "102";
    public static final String DEFAULT_CONTROLER_TYPE = "S7_300";

    public static final String PROPERTY_ID = "id";
    public static final String PROPERTY_PORT = "port";
    public static final String PROPERTY_HOST = "host";
    public static final String PROPERTY_CONTROLLER_TYPE = "controllerType";
    public static final String PROPERTY_REMOTE_RACK = "remoteRack";
    public static final String PROPERTY_REMOTE_SLOT = "remoteSlot";
    public static final String PROPERTY_PDU_LENGTH = "pduLength";
    public static final String PROPERTY_S_7_TO_MQTT = "s7ToMqtt";

    public enum ControllerType {
        S7_200,
        S7_200_SMART,
        S7_300,
        S7_400,
        S7_1200,
        S7_1500,
        SINUMERIK_828D
    }

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

    @JsonProperty(value = PROPERTY_S_7_TO_MQTT, required = true)
    @ModuleConfigField(title = "S7 To MQTT Config",
                       description = "The configuration for a data stream from S7 to MQTT",
                       required = true)
    private final @NotNull S7ToMqttConfig s7ToMqttConfig;

    @JsonCreator
    public S7AdapterConfig(
            @JsonProperty(value = PROPERTY_ID, required = true) final @NotNull String id,
            @JsonProperty(value = PROPERTY_PORT, required = true) final Integer port,
            @JsonProperty(value = PROPERTY_HOST, required = true) final @NotNull String host,
            @JsonProperty(value = PROPERTY_CONTROLLER_TYPE, required = true) final @NotNull ControllerType controllerType,
            @JsonProperty(value = PROPERTY_REMOTE_RACK) final @Nullable Integer remoteRack,
            @JsonProperty(value = PROPERTY_REMOTE_SLOT) final @Nullable Integer remoteSlot,
            @JsonProperty(value = PROPERTY_PDU_LENGTH) final @Nullable Integer pduLength,
            @JsonProperty(value = PROPERTY_S_7_TO_MQTT, required = true) final @NotNull S7ToMqttConfig s7ToMqttConfig) {
        this.host = host;
        this.controllerType = controllerType;
        this.port = port;
        this.remoteRack = remoteRack;
        this.remoteSlot = remoteSlot;
        this.pduLength = pduLength;
        this.s7ToMqttConfig = s7ToMqttConfig;
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

    public @NotNull String getHost() {
        return host;
    }

    public @NotNull S7ToMqttConfig getS7ToMqttConfig() {
        return s7ToMqttConfig;
    }

    @Override
    public String toString() {
        return "S7AdapterConfig{" +
                "port=" +
                port +
                ", host='" +
                host +
                '\'' +
                ", controllerType=" +
                controllerType +
                ", remoteRack=" +
                remoteRack +
                ", remoteSlot=" +
                remoteSlot +
                ", pduLength=" +
                pduLength +
                ", s7ToMqttConfig=" +
                s7ToMqttConfig +
                '}';
    }
}
