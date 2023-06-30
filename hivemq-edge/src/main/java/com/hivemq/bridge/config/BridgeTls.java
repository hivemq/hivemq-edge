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
package com.hivemq.bridge.config;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class BridgeTls {

    private final @Nullable String keystorePath;
    private final @NotNull String keystorePassword;
    private final @NotNull String privateKeyPassword;
    private final @Nullable String truststorePath;
    private final @NotNull String truststorePassword;
    private final @NotNull List<String> protocols;
    private final @NotNull List<String> cipherSuites;
    private final @NotNull String keystoreType;
    private final @NotNull String truststoreType;
    private final boolean verifyHostname;
    private final int handshakeTimeout;

    public BridgeTls(
            final @Nullable String keystorePath,
            final @NotNull String keystorePassword,
            final @NotNull String privateKeyPassword,
            final @Nullable String truststorePath,
            final @NotNull String truststorePassword,
            final @NotNull List<String> protocols,
            final @NotNull List<String> cipherSuites,
            final @NotNull String keystoreType,
            final @NotNull String truststoreType,
            final boolean verifyHostname,
            final int handshakeTimeout) {
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

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BridgeTls)) {
            return false;
        }

        BridgeTls bridgeTls = (BridgeTls) o;

        if (!Objects.equals(keystorePath, bridgeTls.keystorePath)) {
            return false;
        }
        if (!Objects.equals(keystorePassword, bridgeTls.keystorePassword)) {
            return false;
        }
        if (!Objects.equals(privateKeyPassword, bridgeTls.privateKeyPassword)) {
            return false;
        }
        if (!Objects.equals(truststorePath, bridgeTls.truststorePath)) {
            return false;
        }
        if (!Objects.equals(truststorePassword, bridgeTls.truststorePassword)) {
            return false;
        }
        if (!protocols.equals(bridgeTls.protocols)) {
            return false;
        }
        return cipherSuites.equals(bridgeTls.cipherSuites);
    }

    @Override
    public int hashCode() {
        int result = keystorePath != null ? keystorePath.hashCode() : 0;
        result = 31 * result + (keystorePassword != null ? keystorePassword.hashCode() : 0);
        result = 31 * result + (privateKeyPassword != null ? privateKeyPassword.hashCode() : 0);
        result = 31 * result + (truststorePath != null ? truststorePath.hashCode() : 0);
        result = 31 * result + (truststorePassword != null ? truststorePassword.hashCode() : 0);
        result = 31 * result + protocols.hashCode();
        result = 31 * result + cipherSuites.hashCode();
        return result;
    }

    public static class Builder {
        private @Nullable String keystorePath;
        private @NotNull String keystorePassword = "";
        private @NotNull String privateKeyPassword = "";
        private @Nullable String truststorePath;
        private @NotNull String truststorePassword = "";
        private @NotNull List<String> protocols = List.of();
        private @NotNull List<String> cipherSuites = List.of();
        private @NotNull String keystoreType = "JKS";
        private @NotNull String truststoreType = "JKS";
        private boolean verifyHostname = true;
        private int handshakeTimeoutSeconds = 10;

        public @NotNull Builder withKeystorePath(@Nullable String keystorePath) {
            this.keystorePath = keystorePath;
            return this;
        }

        public @NotNull Builder withKeystorePassword(@NotNull String keystorePassword) {
            this.keystorePassword = keystorePassword;
            return this;
        }

        public @NotNull Builder withPrivateKeyPassword(@NotNull String privateKeyPassword) {
            this.privateKeyPassword = privateKeyPassword;
            return this;
        }

        public @NotNull Builder withTruststorePath(@Nullable String truststorePath) {
            this.truststorePath = truststorePath;
            return this;
        }

        public @NotNull Builder withTruststorePassword(@NotNull String truststorePassword) {
            this.truststorePassword = truststorePassword;
            return this;
        }

        public @NotNull Builder withProtocols(@NotNull List<String> protocols) {
            this.protocols = protocols;
            return this;
        }

        public @NotNull Builder withCipherSuites(@NotNull List<String> cipherSuites) {
            this.cipherSuites = cipherSuites;
            return this;
        }

        public @NotNull Builder withKeystoreType(@NotNull String keystoreType) {
            this.keystoreType = keystoreType;
            return this;
        }

        public @NotNull Builder withTruststoreType(@NotNull String truststoreType) {
            this.truststoreType = truststoreType;
            return this;
        }

        public @NotNull Builder withVerifyHostname(boolean verifyHostname) {
            this.verifyHostname = verifyHostname;
            return this;
        }

        public @NotNull Builder withHandshakeTimeout(int handshakeTimeoutMs) {
            this.handshakeTimeoutSeconds = handshakeTimeoutMs;
            return this;
        }

        public @NotNull BridgeTls build() {
            return new BridgeTls(keystorePath,
                    keystorePassword,
                    privateKeyPassword,
                    truststorePath,
                    truststorePassword,
                    protocols,
                    cipherSuites,
                    keystoreType,
                    truststoreType,
                    verifyHostname,
                    handshakeTimeoutSeconds);
        }
    }
}
