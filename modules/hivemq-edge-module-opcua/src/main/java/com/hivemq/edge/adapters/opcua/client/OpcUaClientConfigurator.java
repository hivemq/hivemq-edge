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
package com.hivemq.edge.adapters.opcua.client;

import com.hivemq.edge.adapters.opcua.Constants;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import org.eclipse.milo.opcua.sdk.client.OpcUaClientConfigBuilder;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.util.function.Consumer;

public class OpcUaClientConfigurator implements Consumer<OpcUaClientConfigBuilder> {

    private static final @NotNull Logger log = LoggerFactory.getLogger(OpcUaClientConfigurator.class);

    private final @NotNull String adapterId;
    private final @NotNull ParsedConfig parsedConfig;
    private final @NotNull OpcUaSpecificAdapterConfig config;

    public OpcUaClientConfigurator(final @NotNull String adapterId, final @NotNull ParsedConfig parsedConfig, final @NotNull OpcUaSpecificAdapterConfig config) {
        this.adapterId = adapterId;
        this.parsedConfig = parsedConfig;
        this.config = config;
    }

    @Override
    public void accept(final @NotNull OpcUaClientConfigBuilder configBuilder) {
        // Use Application URI from certificate if available, otherwise fall back to default
        final String applicationUri = parsedConfig.applicationUri() != null
                ? parsedConfig.applicationUri()
                : Constants.OPCUA_APPLICATION_URI;

        if (parsedConfig.applicationUri() == null) {
            log.info("Using default Application URI: {}", applicationUri);
        } else {
            log.info("Using Application URI from certificate: {}", applicationUri);
        }

        // Convert seconds to milliseconds for SDK configuration
        final int sessionTimeoutMs = config.getSessionTimeout() * 1000;
        final int requestTimeoutMs = config.getRequestTimeout() * 1000;
        final int keepAliveIntervalMs = config.getKeepAliveInterval() * 1000;

        configBuilder
                .setApplicationName(LocalizedText.english(Constants.OPCUA_APPLICATION_NAME))
                .setApplicationUri(applicationUri)
                .setProductUri(Constants.OPCUA_PRODUCT_URI)
                .setSessionName(() -> Constants.OPCUA_SESSION_NAME_PREFIX + adapterId)
                // Configure timeouts to prevent silent disconnects
                .setSessionTimeout(UInteger.valueOf(sessionTimeoutMs))
                .setRequestTimeout(UInteger.valueOf(requestTimeoutMs))
                .setKeepAliveInterval(UInteger.valueOf(keepAliveIntervalMs))
                .setKeepAliveFailuresAllowed(UInteger.valueOf(config.getKeepAliveFailuresAllowed()));

        log.info("Configured OPC UA timeouts: session={}s, request={}s, keepAlive={}s, failuresAllowed={}",
                config.getSessionTimeout(), config.getRequestTimeout(), config.getKeepAliveInterval(), config.getKeepAliveFailuresAllowed());
        log.info("TLS is enabled: {}", parsedConfig.tlsEnabled());
        if (parsedConfig.tlsEnabled()) {
            if (log.isDebugEnabled()) {
                log.debug("Configuring TLS");
            }
            //trusted certs, either from configured truststore or system default
            configBuilder.setCertificateValidator(parsedConfig.clientCertificateValidator());

            if (parsedConfig.keyPairWithChain() != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Keystore for TLS is available");
                }
                configBuilder.setCertificate(parsedConfig.keyPairWithChain().publicKey());
                configBuilder.setCertificateChain(parsedConfig.keyPairWithChain().certificateChain());
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Keystore for TLS is not available");
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Configuring Authentication");
        }
        configBuilder.setIdentityProvider(parsedConfig.identityProvider());

        if (parsedConfig.keyPairWithChain() != null) {
            log.info("Setting up keypair with chain");
            configBuilder.setKeyPair(new KeyPair(parsedConfig.keyPairWithChain().publicKey().getPublicKey(),
                    parsedConfig.keyPairWithChain().privateKey()));
        }
    }
}
