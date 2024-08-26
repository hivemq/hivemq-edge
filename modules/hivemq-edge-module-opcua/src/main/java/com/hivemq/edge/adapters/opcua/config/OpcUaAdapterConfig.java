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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.config.ProtocolAdapterConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.Objects.requireNonNullElse;

public class OpcUaAdapterConfig implements ProtocolAdapterConfig {

    private static final @NotNull String ID_REGEX = "^([a-zA-Z_0-9-_])*$";

    @JsonProperty(value = "id", required = true)
    @ModuleConfigField(title = "Identifier",
                       description = "Unique identifier for this protocol adapter",
                       format = ModuleConfigField.FieldType.IDENTIFIER,
                       required = true,
                       stringPattern = ID_REGEX,
                       stringMinLength = 1,
                       stringMaxLength = 1024)
    private final @NotNull String id;

    @JsonProperty("uri")
    @ModuleConfigField(title = "OPC-UA Server URI",
                       description = "URI of the OPC-UA server to connect to",
                       format = ModuleConfigField.FieldType.URI,
                       required = true)
    private final @NotNull String uri;

    @JsonProperty("overrideUri")
    @ModuleConfigField(title = "Override server returned endpoint URI",
                       description = "Overrides the endpoint URI returned from the OPC-UA server with the hostname and port from the specified URI.",
                       format = ModuleConfigField.FieldType.BOOLEAN,
                       defaultValue = "false")
    private final boolean overrideUri;

    @JsonProperty("auth")
    private final @NotNull Auth auth;

    @JsonProperty("tls")
    private final @NotNull Tls tls;

    @JsonProperty("security")
    private final @NotNull Security security;

    @JsonProperty("opcuaToMqtt")
    @ModuleConfigField(title = "OpcUA To MQTT Config",
                       description = "The configuration for a data stream from OpcUA to MQTT",
                       required = true)
    private final @Nullable OpcuaToMqttConfig opcuaToMqttConfig;

    public OpcUaAdapterConfig(
            @JsonProperty(value = "id", required = true) final @NotNull String id,
            @JsonProperty(value = "uri", required = true) final @NotNull String uri,
            @JsonProperty("overrideUri") final Boolean overrideUri,
            @JsonProperty("auth") final @Nullable Auth auth,
            @JsonProperty("tls") final @Nullable Tls tls,
            @JsonProperty("opcuaToMqtt") final @Nullable OpcuaToMqttConfig opcuaToMqttConfig,
            @JsonProperty("security") final @Nullable Security security) {
        this.id = id;
        this.uri = uri;
        this.overrideUri = requireNonNullElse(overrideUri, false);
        this.auth = requireNonNullElse(auth, new Auth(null, null));
        this.tls = requireNonNullElse(tls, new Tls(false, null, null));
        this.opcuaToMqttConfig = opcuaToMqttConfig;
        this.security = requireNonNullElse(security, new Security(SecPolicy.DEFAULT));
    }

    public @NotNull String getId() {
        return id;
    }

    public @NotNull String getUri() {
        return uri;
    }

    public @NotNull Auth getAuth() {
        return auth;
    }

    public @NotNull Tls getTls() {
        return tls;
    }

    public @NotNull Security getSecurity() {
        return security;
    }

    public @Nullable OpcuaToMqttConfig getOpcuaToMqttConfig() {
        return opcuaToMqttConfig;
    }

    public @NotNull Boolean getOverrideUri() {
        return overrideUri;
    }
}
