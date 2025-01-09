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
package com.hivemq.edge.adapters.postgresql.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import org.jetbrains.annotations.NotNull;


@SuppressWarnings({"unused", "FieldCanBeLocal", "FieldMayBeFinal"})
@JsonPropertyOrder({
        "url", "destination"})
public class PostgreSQLAdapterConfig implements ProtocolSpecificAdapterConfig {

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
                       description = "Server port",
                       format = ModuleConfigField.FieldType.UNSPECIFIED,
                       required = true,
                       stringPattern = ID_REGEX,
                       stringMinLength = 1,
                       stringMaxLength = 6,
                       defaultValue = "5432")
    protected @NotNull Integer port;

    @JsonProperty(value = "database", required = true)
    @ModuleConfigField(title = "Database",
                       description = "Database name",
                       format = ModuleConfigField.FieldType.UNSPECIFIED,
                       required = true,
                       stringPattern = ID_REGEX,
                       stringMinLength = 1,
                       stringMaxLength = 1024)
    protected @NotNull String database;

    @JsonProperty(value = "username", required = true)
    @ModuleConfigField(title = "Username",
                       description = "Username for the connection to the database",
                       format = ModuleConfigField.FieldType.UNSPECIFIED,
                       required = true,
                       stringPattern = ID_REGEX,
                       stringMinLength = 1,
                       stringMaxLength = 1024)
    protected @NotNull String username;

    @JsonProperty(value = "password", required = true)
    @ModuleConfigField(title = "Password",
                       description = "Password for the connection to the database",
                       format = ModuleConfigField.FieldType.UNSPECIFIED,
                       required = true,
                       stringPattern = ID_REGEX,
                       stringMinLength = 1,
                       stringMaxLength = 1024)
    protected @NotNull String password;

    @JsonProperty(value = "connectionTimeoutSeconds")
    @ModuleConfigField(title = "connectionTimeoutSeconds",
                       description = "The timeout for connection establishment to the database.",
                       numberMax = 0,
                       defaultValue = "30")
    protected int connectionTimeoutSeconds = 30;

    @JsonProperty("pollingIntervalMillis")
    @ModuleConfigField(title = "Polling Interval [ms]",
                       description = "Time in millisecond that this endpoint will be polled",
                       numberMin = 1,
                       defaultValue = "1000")
    private int pollingIntervalMillis = 1000;

    @JsonProperty("maxPollingErrorsBeforeRemoval")
    @ModuleConfigField(title = "Max. Polling Errors",
                       description = "Max. errors polling the endpoint before the polling daemon is stopped",
                       numberMin = -1,
                       defaultValue = "10")
    private int maxPollingErrorsBeforeRemoval = 10;


    public PostgreSQLAdapterConfig() {
        id = "";
        password = "";
        username = "";
        database = "";
        port = 5432;
        server = "";
    }

    public @NotNull String getServer() {
        return server;
    }

    public @NotNull String getDatabase() {
        return database;
    }

    public @NotNull Integer getPort() {
        return port;
    }

    public @NotNull String getUsername() {
        return username;
    }

    public @NotNull String getPassword() {
        return password;
    }


    public int getPollingIntervalMillis() {
        return pollingIntervalMillis;
    }

    public int getMaxPollingErrorsBeforeRemoval() {
        return maxPollingErrorsBeforeRemoval;
    }

    public int getConnectionTimeout() {
        return connectionTimeoutSeconds;
    }
}
