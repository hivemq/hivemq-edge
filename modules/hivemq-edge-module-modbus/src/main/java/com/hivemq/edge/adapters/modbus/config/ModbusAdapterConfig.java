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
import com.hivemq.adapter.sdk.api.config.WriteContext;
import com.hivemq.edge.adapters.modbus.writing.ModbusWriteContext;
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

    @JsonProperty("pollingIntervalMillis")
    @JsonAlias(value = "publishingInterval") //-- Ensure we cater for properties created with legacy configuration
    @ModuleConfigField(title = "Polling Interval [ms]",
                       description = "Time in millisecond that this endpoint will be polled",
                       numberMin = 1,
                       required = true,
                       defaultValue = "1000")
    private final int pollingIntervalMillis;

    @JsonProperty("maxPollingErrorsBeforeRemoval")
    @ModuleConfigField(title = "Max. Polling Errors",
                       description = "Max. errors polling the endpoint before the polling daemon is stopped",
                       defaultValue = "10")
    private final int maxPollingErrorsBeforeRemoval;

    @JsonProperty("port")
    @ModuleConfigField(title = "Port",
                       description = "The port number on the device you wish to connect to",
                       required = true,
                       numberMin = PORT_MIN,
                       numberMax = PORT_MAX)
    private final int port;

    @JsonProperty("host")
    @ModuleConfigField(title = "Host",
                       description = "IP Address or hostname of the device you wish to connect to",
                       required = true,
                       format = ModuleConfigField.FieldType.HOSTNAME)
    private final @NotNull String host;

    @JsonProperty("timeout")
    @ModuleConfigField(title = "Timeout",
                       description = "Time (in milliseconds) to await a connection before the client gives up",
                       numberMin = 1000,
                       numberMax = 15000,
                       defaultValue = "5000")
    private final int timeout;

    @JsonProperty("publishChangedDataOnly")
    @ModuleConfigField(title = "Only publish data items that have changed since last poll",
                       defaultValue = "true",
                       format = ModuleConfigField.FieldType.BOOLEAN)
    private final boolean publishChangedDataOnly;

    @JsonProperty("subscriptions")
    @ModuleConfigField(title = "Subscriptions", description = "Map your sensor data to MQTT Topics")
    private final @NotNull List<PollingContextImpl> subscriptions;


    @JsonProperty("mqtt-to-modbus-mappings")
    @ModuleConfigField(title = "MQTT to Modbus Mappings ", description = "Map your mqtt data to sensors")
    private final @NotNull List<ModbusWriteContext> writeContexts;

    @JsonCreator
    public ModbusAdapterConfig(
            @JsonProperty(value = "id", required = true) final @NotNull String id,
            @JsonProperty(value = "pollingIntervalMillis") final @Nullable Integer pollingIntervalMillis,
            @JsonProperty(value = "maxPollingErrorsBeforeRemoval") final @Nullable Integer maxPollingErrorsBeforeRemoval,
            @JsonProperty(value = "port", required = true) final int port,
            @JsonProperty(value = "host", required = true) final @NotNull String host,
            @JsonProperty(value = "timeout") final @Nullable Integer timeout,
            @JsonProperty(value = "publishChangedDataOnly") final @Nullable Boolean publishChangedDataOnly,
            @JsonProperty(value = "subscriptions") final @Nullable List<PollingContextImpl> subscriptions,
            @JsonProperty(value = "mqtt-to-modbus-mappings") final @Nullable List<ModbusWriteContext> modbusWriteContexts
    ) {
        this.id = id;
        this.pollingIntervalMillis = Objects.requireNonNullElse(pollingIntervalMillis, 1000);
        this.maxPollingErrorsBeforeRemoval = Objects.requireNonNullElse(maxPollingErrorsBeforeRemoval, 10);
        this.port = port;
        this.host = host;
        this.timeout = Objects.requireNonNullElse(timeout, 5000);
        this.publishChangedDataOnly = Objects.requireNonNullElse(publishChangedDataOnly, true);
        this.subscriptions = Objects.requireNonNullElse(subscriptions, List.of());
        this.writeContexts = Objects.requireNonNullElse(modbusWriteContexts, List.of());
    }

    public @NotNull String getId() {
        return id;
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

    public @NotNull String getHost() {
        return host;
    }

    public @NotNull List<PollingContextImpl> getSubscriptions() {
        return subscriptions;
    }

    public int getTimeout() {
        return timeout;
    }

    public @NotNull List<ModbusWriteContext> getWriteContexts() {
        return writeContexts;
    }
}
