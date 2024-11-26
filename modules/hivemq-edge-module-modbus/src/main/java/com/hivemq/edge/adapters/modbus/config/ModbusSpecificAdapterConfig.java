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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.config.AdapterConfigWithPollingContexts;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


@SuppressWarnings("FieldCanBeLocal")
public class ModbusSpecificAdapterConfig implements ProtocolSpecificAdapterConfig, AdapterConfigWithPollingContexts {
    public static final int PORT_MIN = 1;
    public static final int PORT_MAX = 65535;

    @JsonProperty(value = "host", required = true)
    @ModuleConfigField(title = "Host",
                       description = "IP Address or hostname of the device you wish to connect to",
                       required = true,
                       format = ModuleConfigField.FieldType.HOSTNAME)
    private final @NotNull String host;

    @JsonProperty(value = "port", required = true)
    @ModuleConfigField(title = "Port",
                       description = "The port number on the device you wish to connect to",
                       required = true,
                       numberMin = PORT_MIN,
                       numberMax = PORT_MAX)
    private final int port;

    @JsonProperty("timeoutMillis")
    @ModuleConfigField(title = "Timeout",
                       description = "Time (in milliseconds) to await a connection before the client gives up",
                       numberMin = 1000,
                       numberMax = 15000,
                       defaultValue = "5000")
    private final int timeoutMillis;

    @JsonProperty(value = "modbusToMqtt", required = true)
    @ModuleConfigField(title = "Modbus To MQTT Config",
                       description = "The configuration for a data stream from Modbus to MQTT",
                       required = true)
    private final @Nullable ModbusToMqttConfig modbusToMQTTConfig;

    @JsonCreator
    public ModbusSpecificAdapterConfig(
            @JsonProperty(value = "port", required = true) final int port,
            @JsonProperty(value = "host", required = true) final @NotNull String host,
            @JsonProperty(value = "timeoutMillis") final @Nullable Integer timeoutMillis,
            @JsonProperty(value = "modbusToMqtt") final @NotNull ModbusToMqttConfig modbusToMQTTConfig) {
        this.port = port;
        this.host = host;
        this.timeoutMillis = Objects.requireNonNullElse(timeoutMillis, 5000);
        this.modbusToMQTTConfig = modbusToMQTTConfig;
    }

    public @NotNull String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getTimeoutMillis() {
        return timeoutMillis;
    }

    public @Nullable ModbusToMqttConfig getModbusToMQTTConfig() {
        return modbusToMQTTConfig;
    }

    @Override
    public @NotNull List<? extends PollingContext> getPollingContexts() {
        if (modbusToMQTTConfig==null){
            return  List.of();
        }
        return modbusToMQTTConfig.getMappings();
    }
}