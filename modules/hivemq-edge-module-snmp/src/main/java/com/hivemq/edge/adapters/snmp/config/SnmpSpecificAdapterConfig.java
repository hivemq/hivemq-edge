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
package com.hivemq.edge.adapters.snmp.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@SuppressWarnings("FieldCanBeLocal")
public class SnmpSpecificAdapterConfig implements ProtocolSpecificAdapterConfig {

    public static final int PORT_MIN = 1;
    public static final int PORT_MAX = 65535;
    public static final int DEFAULT_PORT = 161;
    public static final int DEFAULT_TIMEOUT_MILLIS = 3000;
    public static final int DEFAULT_RETRIES = 1;
    public static final String DEFAULT_COMMUNITY = "public";

    private static final @NotNull String ID_REGEX = "^([a-zA-Z_0-9-_])*$";

    @JsonProperty(value = "id", required = true, access = JsonProperty.Access.WRITE_ONLY)
    @ModuleConfigField(title = "Identifier",
                       description = "Unique identifier for this protocol adapter",
                       format = ModuleConfigField.FieldType.IDENTIFIER,
                       required = true,
                       stringPattern = ID_REGEX,
                       stringMinLength = 1,
                       stringMaxLength = 1024)
    private @Nullable String id;

    @JsonProperty(value = "host", required = true)
    @ModuleConfigField(title = "Host",
                       description = "IP address or hostname of the SNMP agent",
                       required = true,
                       format = ModuleConfigField.FieldType.HOSTNAME)
    private final @NotNull String host;

    @JsonProperty(value = "port")
    @ModuleConfigField(title = "Port",
                       description = "SNMP port number",
                       numberMin = PORT_MIN,
                       numberMax = PORT_MAX,
                       defaultValue = "161")
    private final int port;

    @JsonProperty(value = "snmpVersion", required = true)
    @ModuleConfigField(title = "SNMP Version",
                       description = "SNMP protocol version to use",
                       required = true,
                       defaultValue = "V2C")
    private final @NotNull SnmpVersion snmpVersion;

    // SNMPv1/v2c settings
    @JsonProperty(value = "community")
    @ModuleConfigField(title = "Community String",
                       description = "Community string for SNMPv1/v2c authentication",
                       defaultValue = "public")
    private final @NotNull String community;

    // SNMPv3 settings
    @JsonProperty(value = "securityName")
    @ModuleConfigField(title = "Security Name",
                       description = "SNMPv3 username (required for SNMPv3)")
    private final @Nullable String securityName;

    @JsonProperty(value = "authProtocol")
    @ModuleConfigField(title = "Authentication Protocol",
                       description = "SNMPv3 authentication protocol",
                       defaultValue = "NONE")
    private final @NotNull SnmpAuthProtocol authProtocol;

    @JsonProperty(value = "authPassword")
    @ModuleConfigField(title = "Authentication Password",
                       description = "SNMPv3 authentication password")
    private final @Nullable String authPassword;

    @JsonProperty(value = "privProtocol")
    @ModuleConfigField(title = "Privacy Protocol",
                       description = "SNMPv3 encryption protocol",
                       defaultValue = "NONE")
    private final @NotNull SnmpPrivProtocol privProtocol;

    @JsonProperty(value = "privPassword")
    @ModuleConfigField(title = "Privacy Password",
                       description = "SNMPv3 encryption password")
    private final @Nullable String privPassword;

    // Timeout & retry settings
    @JsonProperty(value = "timeoutMillis")
    @ModuleConfigField(title = "Timeout",
                       description = "Request timeout in milliseconds",
                       numberMin = 500,
                       numberMax = 30000,
                       defaultValue = "3000")
    private final int timeoutMillis;

    @JsonProperty(value = "retries")
    @ModuleConfigField(title = "Retries",
                       description = "Number of retry attempts for failed requests",
                       numberMin = 0,
                       numberMax = 10,
                       defaultValue = "1")
    private final int retries;

    // SNMP to MQTT config
    @JsonProperty(value = "snmpToMqtt")
    @ModuleConfigField(title = "SNMP To MQTT Config",
                       description = "Configuration for polling and publishing SNMP data to MQTT")
    private final @Nullable SnmpToMqttConfig snmpToMqttConfig;

    @JsonCreator
    public SnmpSpecificAdapterConfig(
            @JsonProperty(value = "host", required = true) final @NotNull String host,
            @JsonProperty(value = "port") final @Nullable Integer port,
            @JsonProperty(value = "snmpVersion", required = true) final @NotNull SnmpVersion snmpVersion,
            @JsonProperty(value = "community") final @Nullable String community,
            @JsonProperty(value = "securityName") final @Nullable String securityName,
            @JsonProperty(value = "authProtocol") final @Nullable SnmpAuthProtocol authProtocol,
            @JsonProperty(value = "authPassword") final @Nullable String authPassword,
            @JsonProperty(value = "privProtocol") final @Nullable SnmpPrivProtocol privProtocol,
            @JsonProperty(value = "privPassword") final @Nullable String privPassword,
            @JsonProperty(value = "timeoutMillis") final @Nullable Integer timeoutMillis,
            @JsonProperty(value = "retries") final @Nullable Integer retries,
            @JsonProperty(value = "snmpToMqtt") final @Nullable SnmpToMqttConfig snmpToMqttConfig) {
        this.host = host;
        this.port = Objects.requireNonNullElse(port, DEFAULT_PORT);
        this.snmpVersion = snmpVersion;
        this.community = Objects.requireNonNullElse(community, DEFAULT_COMMUNITY);
        this.securityName = securityName;
        this.authProtocol = Objects.requireNonNullElse(authProtocol, SnmpAuthProtocol.NONE);
        this.authPassword = authPassword;
        this.privProtocol = Objects.requireNonNullElse(privProtocol, SnmpPrivProtocol.NONE);
        this.privPassword = privPassword;
        this.timeoutMillis = Objects.requireNonNullElse(timeoutMillis, DEFAULT_TIMEOUT_MILLIS);
        this.retries = Objects.requireNonNullElse(retries, DEFAULT_RETRIES);
        this.snmpToMqttConfig = Objects.requireNonNullElseGet(snmpToMqttConfig,
                () -> new SnmpToMqttConfig(null, null, null));
    }

    public @NotNull String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public @NotNull SnmpVersion getSnmpVersion() {
        return snmpVersion;
    }

    public @NotNull String getCommunity() {
        return community;
    }

    public @Nullable String getSecurityName() {
        return securityName;
    }

    public @NotNull SnmpAuthProtocol getAuthProtocol() {
        return authProtocol;
    }

    public @Nullable String getAuthPassword() {
        return authPassword;
    }

    public @NotNull SnmpPrivProtocol getPrivProtocol() {
        return privProtocol;
    }

    public @Nullable String getPrivPassword() {
        return privPassword;
    }

    public int getTimeoutMillis() {
        return timeoutMillis;
    }

    public int getRetries() {
        return retries;
    }

    public @Nullable SnmpToMqttConfig getSnmpToMqttConfig() {
        return snmpToMqttConfig;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (!(o instanceof SnmpSpecificAdapterConfig that)) {
            return false;
        }
        return port == that.port &&
                timeoutMillis == that.timeoutMillis &&
                retries == that.retries &&
                Objects.equals(host, that.host) &&
                snmpVersion == that.snmpVersion &&
                Objects.equals(community, that.community) &&
                Objects.equals(securityName, that.securityName) &&
                authProtocol == that.authProtocol &&
                Objects.equals(authPassword, that.authPassword) &&
                privProtocol == that.privProtocol &&
                Objects.equals(privPassword, that.privPassword) &&
                Objects.equals(snmpToMqttConfig, that.snmpToMqttConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, snmpVersion, community, securityName, authProtocol,
                authPassword, privProtocol, privPassword, timeoutMillis, retries, snmpToMqttConfig);
    }
}
