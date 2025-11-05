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
package com.hivemq.edge.adapters.opcua.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.Constants;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static java.util.Objects.requireNonNullElse;

public class OpcUaSpecificAdapterConfig implements ProtocolSpecificAdapterConfig {

    @JsonProperty(value = "id", required = true, access = JsonProperty.Access.WRITE_ONLY)
    @ModuleConfigField(title = "Identifier",
                       description = "Unique identifier for this protocol adapter",
                       format = ModuleConfigField.FieldType.IDENTIFIER,
                       required = true,
                       stringPattern = Constants.ID_REGEX,
                       stringMinLength = 1,
                       stringMaxLength = 1024)
    private @Nullable String id;

    @JsonProperty(value = "uri", required = true)
    @ModuleConfigField(title = "OPC UA Server URI",
                       description = "URI of the OPC UA server to connect to",
                       format = ModuleConfigField.FieldType.URI,
                       required = true)
    private final @NotNull String uri;

    @JsonProperty("overrideUri")
    @ModuleConfigField(title = "Override server returned endpoint URI",
                       description = "Overrides the endpoint URI returned from the OPC UA server with the hostname and port from the specified URI.",
                       format = ModuleConfigField.FieldType.BOOLEAN,
                       defaultValue = "false")
    private final boolean overrideUri;

    @JsonProperty("applicationUri")
    @ModuleConfigField(title = "Application URI Override",
                       description = "Overrides the Application URI used for OPC UA client identification. If not specified, the URI from the certificate SAN extension is used, or the default URI 'urn:hivemq:edge:client' as fallback.",
                       format = ModuleConfigField.FieldType.URI)
    private final @Nullable String applicationUri;

    @JsonProperty("auth")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final @Nullable Auth auth;

    @JsonProperty("tls")
    private final @NotNull Tls tls;

    @JsonProperty("security")
    private final @NotNull Security security;

    @JsonProperty(value = "opcuaToMqtt")
    @ModuleConfigField(title = "OPC UA To MQTT Config",
                       description = "The configuration for a data stream from OPC UA to MQTT")
    private final @NotNull OpcUaToMqttConfig opcuaToMqttConfig;

    @JsonProperty("sessionTimeout")
    @ModuleConfigField(title = "Session Timeout (seconds)",
                       description = "OPC UA session timeout in seconds. Session will be renewed at this interval.",
                       numberMin = 10,
                       numberMax = 3600,
                       defaultValue = "120")
    private final int sessionTimeout;

    @JsonProperty("requestTimeout")
    @ModuleConfigField(title = "Request Timeout (seconds)",
                       description = "Timeout for OPC UA requests in seconds",
                       numberMin = 5,
                       numberMax = 300,
                       defaultValue = "30")
    private final int requestTimeout;

    @JsonProperty("keepAliveInterval")
    @ModuleConfigField(title = "Keep-Alive Interval (seconds)",
                       description = "Interval between OPC UA keep-alive pings in seconds",
                       numberMin = 1,
                       numberMax = 60,
                       defaultValue = "10")
    private final int keepAliveInterval;

    @JsonProperty("keepAliveFailuresAllowed")
    @ModuleConfigField(title = "Keep-Alive Failures Allowed",
                       description = "Number of consecutive keep-alive failures before connection is considered dead",
                       numberMin = 1,
                       numberMax = 10,
                       defaultValue = "3")
    private final int keepAliveFailuresAllowed;

    @JsonProperty("connectionTimeout")
    @ModuleConfigField(title = "Connection Timeout (seconds)",
                       description = "Timeout for establishing connection to OPC UA server in seconds",
                       numberMin = 2,
                       numberMax = 300,
                       defaultValue = "30")
    private final int connectionTimeout;

    @JsonProperty("healthCheckInterval")
    @ModuleConfigField(title = "Health Check Interval (seconds)",
                       description = "Interval between connection health checks in seconds",
                       numberMin = 10,
                       numberMax = 300,
                       defaultValue = "30")
    private final int healthCheckInterval;

    @JsonProperty("retryInterval")
    @ModuleConfigField(title = "Retry Interval (seconds)",
                       description = "Interval between connection retry attempts in seconds",
                       numberMin = 5,
                       numberMax = 300,
                       defaultValue = "30")
    private final int retryInterval;

    @JsonProperty("autoReconnect")
    @ModuleConfigField(title = "Automatic Reconnection",
                       description = "Enable automatic reconnection when health check detects connection issues",
                       defaultValue = "true")
    private final boolean autoReconnect;

    @JsonCreator
    public OpcUaSpecificAdapterConfig(
            @JsonProperty(value = "uri", required = true) final @NotNull String uri,
            @JsonProperty("overrideUri") final @Nullable Boolean overrideUri,
            @JsonProperty("applicationUri") final @Nullable String applicationUri,
            @JsonProperty("auth") final @Nullable Auth auth,
            @JsonProperty("tls") final @Nullable Tls tls,
            @JsonProperty(value = "opcuaToMqtt") final @Nullable OpcUaToMqttConfig opcuaToMqttConfig,
            @JsonProperty("security") final @Nullable Security security,
            @JsonProperty("sessionTimeout") final @Nullable Integer sessionTimeout,
            @JsonProperty("requestTimeout") final @Nullable Integer requestTimeout,
            @JsonProperty("keepAliveInterval") final @Nullable Integer keepAliveInterval,
            @JsonProperty("keepAliveFailuresAllowed") final @Nullable Integer keepAliveFailuresAllowed,
            @JsonProperty("connectionTimeout") final @Nullable Integer connectionTimeout,
            @JsonProperty("healthCheckInterval") final @Nullable Integer healthCheckInterval,
            @JsonProperty("retryInterval") final @Nullable Integer retryInterval,
            @JsonProperty("autoReconnect") final @Nullable Boolean autoReconnect) {
        this.uri = uri;
        this.overrideUri = requireNonNullElse(overrideUri, false);
        this.applicationUri = (applicationUri != null && !applicationUri.isBlank()) ? applicationUri : null;
        this.auth = auth;
        this.tls = requireNonNullElse(tls, new Tls(false, TlsChecks.NONE, null, null));
        this.opcuaToMqttConfig =
                Objects.requireNonNullElseGet(opcuaToMqttConfig, () -> new OpcUaToMqttConfig(1, 1000));
        this.security = requireNonNullElse(security, new Security(Constants.DEFAULT_SECURITY_POLICY));

        // Timeout configurations with sensible defaults
        this.sessionTimeout = requireNonNullElse(sessionTimeout, 120);
        this.requestTimeout = requireNonNullElse(requestTimeout, 30);
        this.keepAliveInterval = requireNonNullElse(keepAliveInterval, 10);
        this.keepAliveFailuresAllowed = requireNonNullElse(keepAliveFailuresAllowed, 3);
        this.connectionTimeout = requireNonNullElse(connectionTimeout, 30);
        this.healthCheckInterval = requireNonNullElse(healthCheckInterval, 30);
        this.retryInterval = requireNonNullElse(retryInterval, 30);
        this.autoReconnect = requireNonNullElse(autoReconnect, true);
    }


    public @NotNull String getUri() {
        return uri;
    }

    public @Nullable Auth getAuth() {
        return auth;
    }

    public @NotNull Tls getTls() {
        return tls;
    }

    public @NotNull Security getSecurity() {
        return security;
    }

    public @NotNull OpcUaToMqttConfig getOpcuaToMqttConfig() {
        return opcuaToMqttConfig;
    }

    public @NotNull Boolean getOverrideUri() {
        return overrideUri;
    }

    public @Nullable String getApplicationUri() {
        return applicationUri;
    }

    public int getSessionTimeout() {
        return sessionTimeout;
    }

    public int getRequestTimeout() {
        return requestTimeout;
    }

    public int getKeepAliveInterval() {
        return keepAliveInterval;
    }

    public int getKeepAliveFailuresAllowed() {
        return keepAliveFailuresAllowed;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public int getHealthCheckInterval() {
        return healthCheckInterval;
    }

    public int getRetryInterval() {
        return retryInterval;
    }

    public boolean isAutoReconnect() {
        return autoReconnect;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final OpcUaSpecificAdapterConfig that = (OpcUaSpecificAdapterConfig) o;
        return getOverrideUri().equals(that.getOverrideUri()) &&
                sessionTimeout == that.sessionTimeout &&
                requestTimeout == that.requestTimeout &&
                keepAliveInterval == that.keepAliveInterval &&
                keepAliveFailuresAllowed == that.keepAliveFailuresAllowed &&
                connectionTimeout == that.connectionTimeout &&
                healthCheckInterval == that.healthCheckInterval &&
                retryInterval == that.retryInterval &&
                autoReconnect == that.autoReconnect &&
                Objects.equals(id, that.id) &&
                Objects.equals(getUri(), that.getUri()) &&
                Objects.equals(getApplicationUri(), that.getApplicationUri()) &&
                Objects.equals(getAuth(), that.getAuth()) &&
                Objects.equals(getTls(), that.getTls()) &&
                Objects.equals(getSecurity(), that.getSecurity()) &&
                Objects.equals(getOpcuaToMqttConfig(), that.getOpcuaToMqttConfig());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOverrideUri(), id, getUri(), getApplicationUri(), getAuth(), getTls(), getSecurity(), getOpcuaToMqttConfig(),
                sessionTimeout, requestTimeout, keepAliveInterval, keepAliveFailuresAllowed, connectionTimeout, healthCheckInterval, retryInterval, autoReconnect);
    }
}
