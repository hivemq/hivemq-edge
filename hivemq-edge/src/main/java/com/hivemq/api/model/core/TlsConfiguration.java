/*
 * Copyright 2019-present HiveMQ GmbH
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
package com.hivemq.api.model.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Generic model to transport TLS configuration across the API
 *
 * @author Simon L Johnson
 */
public class TlsConfiguration {

    @JsonProperty("enabled")
    @Schema(description = "If TLS is used")
    private final boolean enabled;

    @JsonProperty("keystorePath")
    @Schema(description = "The keystorePath from the config", nullable = true)
    private final @Nullable String keystorePath;

    @JsonProperty("keystorePassword")
    @Schema(description = "The keystorePassword from the config")
    private final @NotNull String keystorePassword;

    @JsonProperty("privateKeyPassword")
    @Schema(description = "The privateKeyPassword from the config")
    private final @NotNull String privateKeyPassword;

    @JsonProperty("truststorePath")
    @Schema(description = "The truststorePath from the config", nullable = true)
    private final @Nullable String truststorePath;

    @JsonProperty("truststorePassword")
    @Schema(description = "The truststorePassword from the config")
    private final @NotNull String truststorePassword;

    @JsonProperty("protocols")
    @Schema(description = "The protocols from the config")
    private final @NotNull List<String> protocols;

    @JsonProperty("cipherSuites")
    @Schema(description = "The cipherSuites from the config")
    private final @NotNull List<String> cipherSuites;

    @JsonProperty("keystoreType")
    @Schema(description = "The keystoreType from the config")
    private final @NotNull String keystoreType;

    @JsonProperty("truststoreType")
    @Schema(description = "The truststoreType from the config")
    private final @NotNull String truststoreType;

    @JsonProperty("verifyHostname")
    @Schema(description = "The verifyHostname from the config", defaultValue = "false")
    private final boolean verifyHostname;

    @JsonProperty("handshakeTimeout")
    @Schema(description = "The handshakeTimeout from the config")
    private final int handshakeTimeout;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public TlsConfiguration(
            @JsonProperty("enabled") final boolean enabled,
            @JsonProperty("keystorePath") final @Nullable String keystorePath,
            @JsonProperty("keystorePassword") final @NotNull String keystorePassword,
            @JsonProperty("privateKeyPassword") final @NotNull String privateKeyPassword,
            @JsonProperty("truststorePath") final @Nullable String truststorePath,
            @JsonProperty("truststorePassword") final @NotNull String truststorePassword,
            @JsonProperty("protocols") final @NotNull List<String> protocols,
            @JsonProperty("cipherSuites") final @NotNull List<String> cipherSuites,
            @JsonProperty("keystoreType") final @NotNull String keystoreType,
            @JsonProperty("truststoreType") final @NotNull String truststoreType,
            @JsonProperty("verifyHostname") final boolean verifyHostname,
            @JsonProperty("handshakeTimeout") final int handshakeTimeout) {
        this.enabled = enabled;
        this.keystorePath = keystorePath;
        this.keystorePassword = keystorePassword;
        this.privateKeyPassword = privateKeyPassword;
        this.truststorePath = truststorePath;
        this.truststorePassword = truststorePassword;
        this.protocols = protocols;
        this.cipherSuites = cipherSuites;
        this.keystoreType = keystoreType;
        this.truststoreType = truststoreType;
        this.verifyHostname = verifyHostname;
        this.handshakeTimeout = handshakeTimeout;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public @Nullable String getKeystorePath() {
        return keystorePath;
    }

    public @NotNull String getKeystorePassword() {
        return keystorePassword;
    }

    public @NotNull String getPrivateKeyPassword() {
        return privateKeyPassword;
    }

    public @Nullable String getTruststorePath() {
        return truststorePath;
    }

    public @NotNull String getTruststorePassword() {
        return truststorePassword;
    }

    public @NotNull List<String> getProtocols() {
        return protocols;
    }

    public @NotNull List<String> getCipherSuites() {
        return cipherSuites;
    }

    public @NotNull String getKeystoreType() {
        return keystoreType;
    }

    public @NotNull String getTruststoreType() {
        return truststoreType;
    }

    public boolean isVerifyHostname() {
        return verifyHostname;
    }

    public int getHandshakeTimeout() {
        return handshakeTimeout;
    }
}
