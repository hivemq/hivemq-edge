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
import org.eclipse.milo.opcua.sdk.client.OpcUaClientConfigBuilder;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.util.function.Consumer;

public class OpcUaClientConfigurator implements Consumer<OpcUaClientConfigBuilder> {

    private static final Logger log = LoggerFactory.getLogger(OpcUaClientConfigurator.class);

    private final @NotNull String adapterId;
    private final @NotNull ParsedConfig parsedConfig;

    public OpcUaClientConfigurator(
            final @NotNull String adapterId,
            final @NotNull ParsedConfig parsedConfig) {
        this.adapterId = adapterId;
        this.parsedConfig = parsedConfig;
    }

    @Override
    public void accept(final OpcUaClientConfigBuilder opcUaClientConfigBuilder) {
        opcUaClientConfigBuilder.setApplicationName(LocalizedText.english(Constants.OPCUA_APPLICATION_NAME));
        opcUaClientConfigBuilder.setApplicationUri(Constants.OPCUA_APPLICATION_URI);
        opcUaClientConfigBuilder.setProductUri(Constants.OPCUA_PRODUCT_URI);
        opcUaClientConfigBuilder.setSessionName(() -> Constants.OPCUA_SESSION_NAME_PREFIX + adapterId);

        log.info("TLS is enabled: {}", parsedConfig.tlsEnabled());
        if(parsedConfig.tlsEnabled()) {
            log.debug("Configuring TLS");
            //trusted certs, either from configured truststore or system default
            opcUaClientConfigBuilder.setCertificateValidator(parsedConfig.clientCertificateValidator());

            if (parsedConfig.keyPairWithChain() != null) {
                log.debug("Keystore for TLS is available");
                opcUaClientConfigBuilder.setCertificate(parsedConfig.keyPairWithChain().publicKey());
                opcUaClientConfigBuilder.setCertificateChain(parsedConfig.keyPairWithChain().certificateChain());
            } else {
                log.debug("Keystore for TLS is not available");
            }
        }

        log.debug("Configuring Authentication");
        opcUaClientConfigBuilder.setIdentityProvider(parsedConfig.identityProvider());

        if (parsedConfig.keyPairWithChain() != null) {
            log.info("Setting up keypair with chain");
            opcUaClientConfigBuilder.setKeyPair(
                    new KeyPair(
                            parsedConfig.keyPairWithChain().publicKey().getPublicKey(),
                            parsedConfig.keyPairWithChain().privateKey()));
        }
    }


}
