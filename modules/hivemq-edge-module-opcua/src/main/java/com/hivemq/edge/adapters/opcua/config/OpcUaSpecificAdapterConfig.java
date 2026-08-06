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

import static java.util.Objects.requireNonNullElse;
import static java.util.Objects.requireNonNullElseGet;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.Constants;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpcUaSpecificAdapterConfig implements ProtocolSpecificAdapterConfig {

    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(OpcUaSpecificAdapterConfig.class);

    /**
     * The settings this configuration accepts, derived from the declared properties so the list can
     * never drift from the class it describes. Quoted back when an unknown setting is refused.
     */
    private static final @NotNull String KNOWN_SETTINGS = Arrays.stream(
                    OpcUaSpecificAdapterConfig.class.getDeclaredFields())
            .map(field -> field.getAnnotation(JsonProperty.class))
            .filter(Objects::nonNull)
            .map(JsonProperty::value)
            .collect(Collectors.joining(", "));

    /** Existed in released versions and was retired; a configuration carrying it must keep starting. */
    private static final @NotNull String RETIRED_INCLUDE_METADATA = "includeMetadata";

    @JsonProperty(value = "id", required = true, access = JsonProperty.Access.WRITE_ONLY)
    @ModuleConfigField(
            title = "Identifier",
            description = "Unique identifier for this protocol adapter",
            format = ModuleConfigField.FieldType.IDENTIFIER,
            required = true,
            stringPattern = Constants.ID_REGEX,
            stringMinLength = 1,
            stringMaxLength = 1024)
    private @Nullable String id;

    @JsonProperty(value = "uri", required = true)
    @ModuleConfigField(
            title = "OPC UA Server URI",
            description = "URI of the OPC UA server to connect to",
            format = ModuleConfigField.FieldType.URI,
            required = true)
    private final @NotNull String uri;

    @JsonProperty("overrideUri")
    @ModuleConfigField(
            title = "Override server returned endpoint URI",
            description =
                    "Overrides the endpoint URI returned from the OPC UA server with the hostname and port from the specified URI.",
            format = ModuleConfigField.FieldType.BOOLEAN,
            defaultValue = "false")
    private final boolean overrideUri;

    @JsonProperty("applicationUri")
    @ModuleConfigField(
            title = "Application URI Override",
            description =
                    "Overrides the Application URI used for OPC UA client identification. If not specified, the URI from the certificate SAN extension is used, or the default URI 'urn:hivemq:edge:client' as fallback.")
    private final @Nullable String applicationUri;

    @JsonProperty("auth")
    @ModuleConfigField(
            title = "Authentication Configuration",
            description = "Select the authentication mode to use for connecting.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final @Nullable Auth auth;

    @JsonProperty("tls")
    @ModuleConfigField(
            title = "TLS Configuration",
            description = "Configure TLS for use with X509 or connecting to a TLS enabled OPC UA server.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final @NotNull Tls tls;

    @JsonProperty("security")
    @ModuleConfigField(
            title = "Message Security Configuration",
            description = "Configure how the security of PC UA messages should be treated.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final @NotNull Security security;

    @JsonProperty(value = "opcuaToMqtt")
    @ModuleConfigField(
            title = "OPC UA To MQTT Config",
            description = "The configuration for a data stream from OPC UA to MQTT")
    private final @NotNull OpcUaToMqttConfig opcuaToMqttConfig;

    @JsonProperty(value = "connectionOptions")
    @ModuleConfigField(
            title = "Options for connection handling",
            description = "Controls how heartbeats and reconnects are handled")
    private final @NotNull ConnectionOptions connectionOptions;

    @JsonCreator
    public OpcUaSpecificAdapterConfig(
            @JsonProperty(value = "uri", required = true) final @NotNull String uri,
            @JsonProperty("overrideUri") final @Nullable Boolean overrideUri,
            @JsonProperty("applicationUri") final @Nullable String applicationUri,
            @JsonProperty("auth") final @Nullable Auth auth,
            @JsonProperty("tls") final @Nullable Tls tls,
            @JsonProperty("opcuaToMqtt") final @Nullable OpcUaToMqttConfig opcuaToMqttConfig,
            @JsonProperty("security") final @Nullable Security security,
            @JsonProperty("connectionOptions") final @Nullable ConnectionOptions connectionOptions) {
        this.uri = uri;
        this.overrideUri = requireNonNullElse(overrideUri, false);
        this.applicationUri = (applicationUri != null && !applicationUri.isBlank()) ? applicationUri : "";
        this.auth = auth;
        this.tls = requireNonNullElseGet(tls, Tls::defaultTls);
        this.opcuaToMqttConfig = requireNonNullElseGet(opcuaToMqttConfig, OpcUaToMqttConfig::defaultOpcUaToMqttConfig);
        this.security = requireNonNullElse(security, new Security(Constants.DEFAULT_SECURITY_POLICY));
        this.connectionOptions = requireNonNullElseGet(connectionOptions, ConnectionOptions::defaultConnectionOptions);
    }

    /**
     * Refuses a setting this configuration does not have, at conversion time. The application-wide
     * handling for unknown adapter settings warns and drops, which is the wrong posture here: the
     * enclosing settings carry the security-relevant elements, so a misspelled {@code <tlls>},
     * {@code <securtiy>} or {@code <aut>} would drop the whole block and quietly run the adapter on
     * the insecure default — TLS off, policy NONE, anonymous access — instead of what the operator
     * wrote. An any-setter runs ahead of that handler, so the entry is refused with the mistake
     * named. The rejection is contained: ProtocolAdapterManager converts each adapter's
     * configuration in isolation, and a running instance stays unchanged.
     *
     * <p>Inherited by subtypes; a subtype's own declared settings are known to its deserializer and
     * never arrive here.
     *
     * <p>{@code includeMetadata} is the one exception: it is a retired setting that released
     * versions accepted, so refusing it would break an upgrade. It is reported and ignored.
     */
    @JsonAnySetter
    void refuseUnknownSetting(final @NotNull String name, final @Nullable Object value) {
        if (RETIRED_INCLUDE_METADATA.equals(name)) {
            LOGGER.warn(
                    "Adapter configuration: '{}' is a retired setting and has been ignored. Remove the entry.",
                    RETIRED_INCLUDE_METADATA);
            return;
        }
        throw new IllegalArgumentException(("The adapter configuration contains '%s', which is not a setting it "
                        + "has. It cannot be applied, and dropping it could mean running with weaker security than "
                        + "was written, so the configuration is rejected instead. Correct or remove the entry. "
                        + "Known settings: %s.")
                .formatted(name, KNOWN_SETTINGS));
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

    public @NotNull ConnectionOptions getConnectionOptions() {
        return connectionOptions;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof final OpcUaSpecificAdapterConfig that)) return false;
        return getOverrideUri().equals(that.getOverrideUri())
                && Objects.equals(id, that.id)
                && Objects.equals(getUri(), that.getUri())
                && Objects.equals(getApplicationUri(), that.getApplicationUri())
                && Objects.equals(getAuth(), that.getAuth())
                && Objects.equals(getTls(), that.getTls())
                && Objects.equals(getSecurity(), that.getSecurity())
                && Objects.equals(getOpcuaToMqttConfig(), that.getOpcuaToMqttConfig())
                && Objects.equals(connectionOptions, that.connectionOptions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                getUri(),
                getOverrideUri(),
                getApplicationUri(),
                getAuth(),
                getTls(),
                getSecurity(),
                getOpcuaToMqttConfig(),
                connectionOptions);
    }

    @Override
    public String toString() {
        return "OpcUaSpecificAdapterConfig{" + "id='"
                + id
                + '\''
                + ", uri='"
                + uri
                + '\''
                + ", overrideUri="
                + overrideUri
                + ", applicationUri='"
                + applicationUri
                + '\''
                + ", auth="
                + auth
                + ", tls="
                + tls
                + ", security="
                + security
                + ", opcuaToMqttConfig="
                + opcuaToMqttConfig
                + ", connectionOptions="
                + connectionOptions
                + '}';
    }
}
