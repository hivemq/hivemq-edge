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
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static java.util.Objects.requireNonNullElse;

public class OpcUaSpecificAdapterConfig implements ProtocolSpecificAdapterConfig {

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
    private final @Nullable OpcUaToMqttConfig opcuaToMqttConfig;

    @JsonCreator
    public OpcUaSpecificAdapterConfig(
            @JsonProperty(value = "uri", required = true) final @NotNull String uri,
            @JsonProperty("overrideUri") final @Nullable Boolean overrideUri,
            @JsonProperty("auth") final @Nullable Auth auth,
            @JsonProperty("tls") final @Nullable Tls tls,
            @JsonProperty(value = "opcuaToMqtt") final @Nullable OpcUaToMqttConfig opcuaToMqttConfig,
            @JsonProperty("security") final @Nullable Security security) {
        this.uri = uri;
        this.overrideUri = requireNonNullElse(overrideUri, false);
        this.auth = auth;
        this.tls = requireNonNullElse(tls, new Tls(false, null, null));
        this.opcuaToMqttConfig =
                Objects.requireNonNullElseGet(opcuaToMqttConfig, () -> new OpcUaToMqttConfig(null, null));

        this.security = requireNonNullElse(security, new Security(SecPolicy.DEFAULT));
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

    public @Nullable OpcUaToMqttConfig getOpcuaToMqttConfig() {
        return opcuaToMqttConfig;
    }

    public @NotNull Boolean getOverrideUri() {
        return overrideUri;
    }
}
