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
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

public class Tls {

    @JsonProperty("enabled")
    @ModuleConfigField(title = "Enable TLS", description = "Enables TLS encrypted connection", defaultValue = "false")
    private final boolean enabled;

    @JsonProperty("keystore")
    @JsonInclude(NON_NULL)
    @ModuleConfigField(title = "Keystore",
                       description = "Keystore that contains the client certificate including the chain. Required for X509 authentication.")
    private final @Nullable Keystore keystore;

    @JsonProperty("truststore")
    @JsonInclude(NON_NULL)
    @ModuleConfigField(title = "Truststore",
                       description = "Truststore wich contains the trusted server certificates or trusted intermediates.")
    private final @Nullable Truststore truststore;

    @JsonCreator
    public Tls(
            @JsonProperty("enabled") final @Nullable Boolean enabled,
            @JsonProperty("keystore") final @Nullable Keystore keystore,
            @JsonProperty("truststore") final @Nullable Truststore truststore) {
        this.enabled = Objects.requireNonNullElse(enabled, false);
        this.keystore = keystore;
        this.truststore = truststore;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public @Nullable Keystore getKeystore() {
        return keystore;
    }

    public @Nullable Truststore getTruststore() {
        return truststore;
    }
}
