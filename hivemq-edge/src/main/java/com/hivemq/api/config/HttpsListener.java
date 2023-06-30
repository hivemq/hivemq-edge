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
package com.hivemq.api.config;

import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class HttpsListener implements ApiListener {

    private final int port;
    private final @NotNull String bindAddress;
    private final @NotNull List<String> protocols;
    private final @NotNull List<String> cipherSuites;
    private final @NotNull String keystorePath;
    private final @NotNull String keystorePassword;
    private final @NotNull String privateKeyPassword;

    public HttpsListener(
            final int port,
            final @NotNull String bindAddress,
            final @NotNull List<String> protocols,
            final @NotNull List<String> cipherSuites,
            final @NotNull String keystorePath,
            final @NotNull String keystorePassword,
            final @NotNull String privateKeyPassword) {

        checkNotNull(bindAddress, "Bind address must not be null");
        checkNotNull(protocols, "Protocols must not be null");
        checkNotNull(cipherSuites, "Cipher suites must not be null");
        checkNotNull(keystorePath, "Path must not be null");
        checkNotNull(keystorePassword, "Password must not be null");
        checkNotNull(privateKeyPassword, "Private key password must not be null");

        this.port = port;
        this.bindAddress = bindAddress;
        this.protocols = protocols;
        this.cipherSuites = cipherSuites;
        this.keystorePath = keystorePath;
        this.keystorePassword = keystorePassword;
        this.privateKeyPassword = privateKeyPassword;
    }

    public int getPort() {
        return port;
    }

    public @NotNull String getBindAddress() {
        return bindAddress;
    }

    public @NotNull List<String> getProtocols() {
        return protocols;
    }

    public @NotNull List<String> getCipherSuites() {
        return cipherSuites;
    }

    public @NotNull String getKeystorePath() {
        return keystorePath;
    }

    public @NotNull String getKeystorePassword() {
        return keystorePassword;
    }

    public @NotNull String getPrivateKeyPassword() {
        return privateKeyPassword;
    }

}
