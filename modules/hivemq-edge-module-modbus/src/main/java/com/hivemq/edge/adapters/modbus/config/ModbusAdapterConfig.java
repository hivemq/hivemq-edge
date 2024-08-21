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
import com.hivemq.adapter.sdk.api.config.ProtocolAdapterConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;


@SuppressWarnings("FieldCanBeLocal")
public class ModbusAdapterConfig implements ProtocolAdapterConfig {

    private static final @NotNull String ID_REGEX = "^([a-zA-Z_0-9-_])*$";
    public static final int PORT_MIN = 1;
    public static final int PORT_MAX = 65535;

    @JsonProperty(value = "id", required = true)
    @ModuleConfigField(title = "Identifier",
                       description = "Unique identifier for this protocol adapter",
                       format = ModuleConfigField.FieldType.IDENTIFIER,
                       required = true,
                       stringPattern = ID_REGEX,
                       stringMinLength = 1,
                       stringMaxLength = 1024)
    private final @NotNull String id;

    @JsonProperty("host")
    @ModuleConfigField(title = "Host",
                       description = "IP Address or hostname of the device you wish to connect to",
                       required = true,
                       format = ModuleConfigField.FieldType.HOSTNAME)
    private final @NotNull String host;

    @JsonProperty("port")
    @ModuleConfigField(title = "Port",
                       description = "The port number on the device you wish to connect to",
                       required = true,
                       numberMin = PORT_MIN,
                       numberMax = PORT_MAX)
    private final int port;

    @JsonProperty("timeout")
    @ModuleConfigField(title = "Timeout",
                       description = "Time (in milliseconds) to await a connection before the client gives up",
                       numberMin = 1000,
                       numberMax = 15000,
                       defaultValue = "5000")
    private final int timeout;

    @JsonProperty("modbusToMqtt")
    @ModuleConfigField(title = "Modbus To MQTT Config",
                       description = "The configuration for a data stream from Modbus to MQTT",
                       required = true)
    private final @NotNull ModbusToMQTTConfig modbusToMQTTConfig;

    @JsonCreator
    public ModbusAdapterConfig(
            @JsonProperty(value = "id", required = true) final @NotNull String id,
            @JsonProperty(value = "port", required = true) final int port,
            @JsonProperty(value = "host", required = true) final @NotNull String host,
            @JsonProperty(value = "timeout") final @Nullable Integer timeout,
            @JsonProperty(value = "modbusToMqtt") final @NotNull ModbusToMQTTConfig modbusToMQTTConfig) {
        this.id = id;
        this.port = port;
        this.host = host;
        this.timeout = Objects.requireNonNullElse(timeout, 5000);
        this.modbusToMQTTConfig = modbusToMQTTConfig;
    }

    public @NotNull String getId() {
        return id;
    }

    public @NotNull String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getTimeout() {
        return timeout;
    }

    public @NotNull ModbusToMQTTConfig getModbusToMQTTConfig() {
        return modbusToMQTTConfig;
    }
}
