/*
 * Copyright 2024-present HiveMQ GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.hivemq.edge.adapters.redis.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.config.ProtocolAdapterConfig;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


@SuppressWarnings({"unused", "FieldCanBeLocal", "FieldMayBeFinal"})
@JsonPropertyOrder({
        "url",
        "destination"})
public class RedisAdapterConfig implements ProtocolAdapterConfig {

    private static final @NotNull String ID_REGEX = "^([a-zA-Z_0-9-_])*$";

    @JsonProperty(value = "id", required = true)
    @ModuleConfigField(title = "Identifier",
            description = "Unique identifier for this protocol adapter",
            format = ModuleConfigField.FieldType.IDENTIFIER,
            required = true,
            stringPattern = ID_REGEX,
            stringMinLength = 1,
            stringMaxLength = 1024)
    protected @NotNull String id;

    @JsonProperty(value = "server", required = true)
    @ModuleConfigField(title = "Server",
            description = "Server address",
            format = ModuleConfigField.FieldType.UNSPECIFIED,
            required = true,
            stringMinLength = 1,
            stringMaxLength = 1024)
    protected @NotNull String server;

    @JsonProperty(value = "port", required = true)
    @ModuleConfigField(title = "Port",
            description = "Server port (usually 6379)",
            format = ModuleConfigField.FieldType.UNSPECIFIED,
            required = true,
            stringPattern = ID_REGEX,
            stringMinLength = 1,
            stringMaxLength = 6)
    protected @NotNull Integer port;

    @JsonProperty(value = "username", required = false)
    @ModuleConfigField(title = "Username",
            description = "Username for the connection to the database (can be empty if only password is required)",
            format = ModuleConfigField.FieldType.UNSPECIFIED,
            required = false,
            stringPattern = ID_REGEX,
            stringMinLength = 0,
            stringMaxLength = 1024)
    protected String username;

    @JsonProperty(value = "password", required = false)
    @ModuleConfigField(title = "Password",
            description = "Password for the connection to the database",
            format = ModuleConfigField.FieldType.UNSPECIFIED,
            required = false,
            stringPattern = ID_REGEX,
            stringMinLength = 0,
            stringMaxLength = 1024)
    protected String password;

    @JsonProperty("pollingIntervalMillis")
    @ModuleConfigField(title = "Polling Interval [ms]",
            description = "Time in millisecond that this endpoint will be polled",
            numberMin = 1,
            required = true,
            defaultValue = "2000")
    private int pollingIntervalMillis = 10000;

    @JsonProperty("maxPollingErrorsBeforeRemoval")
    @ModuleConfigField(title = "Max. Polling Errors",
            description = "Max. errors polling the endpoint before the polling daemon is stopped",
            numberMin = 3,
            defaultValue = "10")
    private int maxPollingErrorsBeforeRemoval = 10;


    @JsonProperty("subscriptions")
    @ModuleConfigField(title = "subscription", description = "Map your Redis data to a MQTT Topic")
    private @NotNull List<RedisPollingContext> pollingContexts = new ArrayList<>();

    public RedisAdapterConfig() {
        id = "";
        server = "";
        port = 6379;
        username = "";
        password = "";
    }

    @Override
    public @NotNull String getId() {
        return id;
    }

    public @NotNull String getServer() {return server;}

    public @NotNull Integer getPort() {return port;}

    public String getPassword() {return password;}

    public String getUsername() {return username;}

    public int getPollingIntervalMillis() {
        return pollingIntervalMillis;
    }

    public int getMaxPollingErrorsBeforeRemoval() {
        return maxPollingErrorsBeforeRemoval;
    }

    public @NotNull List<RedisPollingContext> getPollingContexts() {
        return pollingContexts;
    }
}
