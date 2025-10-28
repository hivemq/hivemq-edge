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

import com.hivemq.edge.adapters.opcua.config.Auth;
import com.hivemq.edge.adapters.opcua.config.Keystore;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.SecPolicy;
import com.hivemq.edge.adapters.opcua.config.Security;
import com.hivemq.edge.adapters.opcua.config.Tls;
import com.hivemq.edge.adapters.opcua.config.Truststore;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import org.eclipse.milo.opcua.sdk.client.identity.AnonymousProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import util.KeyChain;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ParsedConfigTest {

    private static final String KEYSTORE_PASSWORD = "password";
    private static final String PRIVATE_KEY_PASSWORD = "password";
    private static final String TEST_URI = "opc.tcp://localhost:4840";

    @TempDir
    Path tempDir;

    @Test
    void testFromConfig_withCertificateSanUri_extractsAndStoresUri() throws Exception {
        // Given
        final String domain = "testclient";
        final String expectedUri = "urn:hivemq:edge:" + domain;

        final KeyChain keyChain = KeyChain.createKeyChain(domain);
        final File keystoreFile = keyChain.wrapInKeyStoreWithPrivateKey(
                tempDir.resolve("test-keystore").toString(),
                domain,
                KEYSTORE_PASSWORD,
                PRIVATE_KEY_PASSWORD
        );

        final OpcUaSpecificAdapterConfig adapterConfig = createAdapterConfig(
                true,  // TLS enabled
                keystoreFile.getAbsolutePath(),
                null   // No truststore, will use default
        );

        // When
        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(adapterConfig);

        // Then
        assertThat(result).isInstanceOf(Success.class);
        final ParsedConfig parsedConfig = ((Success<ParsedConfig, String>) result).result();

        assertThat(parsedConfig.applicationUri())
                .as("Application URI should be extracted from certificate SAN")
                .isNotNull()
                .isEqualTo(expectedUri);
        assertThat(parsedConfig.tlsEnabled()).isTrue();
        assertThat(parsedConfig.keyPairWithChain()).isNotNull();
        assertThat(parsedConfig.identityProvider()).isInstanceOf(AnonymousProvider.class);
    }

    @Test
    void testFromConfig_noKeystore_applicationUriIsNull() {
        // Given
        final OpcUaSpecificAdapterConfig adapterConfig = createAdapterConfig(
                true,  // TLS enabled but no keystore
                null,
                null
        );

        // When
        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(adapterConfig);

        // Then
        assertThat(result).isInstanceOf(Success.class);
        final ParsedConfig parsedConfig = ((Success<ParsedConfig, String>) result).result();

        assertThat(parsedConfig.applicationUri())
                .as("Application URI should be null when no keystore is provided")
                .isNull();
        assertThat(parsedConfig.keyPairWithChain()).isNull();
    }

    @Test
    void testFromConfig_tlsDisabled_applicationUriIsNull() {
        // Given
        final OpcUaSpecificAdapterConfig adapterConfig = createAdapterConfig(
                false,  // TLS disabled
                null,
                null
        );

        // When
        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(adapterConfig);

        // Then
        assertThat(result).isInstanceOf(Success.class);
        final ParsedConfig parsedConfig = ((Success<ParsedConfig, String>) result).result();

        assertThat(parsedConfig.applicationUri())
                .as("Application URI should be null when TLS is disabled")
                .isNull();
        assertThat(parsedConfig.tlsEnabled()).isFalse();
        assertThat(parsedConfig.keyPairWithChain()).isNull();
    }

    @Test
    void testFromConfig_multipleCertificates_extractsCorrectUri() throws Exception {
        // Given
        final String domain1 = "client1";
        final String domain2 = "client2";
        final String expectedUri = "urn:hivemq:edge:" + domain1;

        final KeyChain keyChain = KeyChain.createKeyChain(domain1, domain2);
        final File keystoreFile = keyChain.wrapInKeyStoreWithPrivateKey(
                tempDir.resolve("test-keystore-multi").toString(),
                domain1,  // First domain is used for key entry
                KEYSTORE_PASSWORD,
                PRIVATE_KEY_PASSWORD
        );

        final OpcUaSpecificAdapterConfig adapterConfig = createAdapterConfig(
                true,
                keystoreFile.getAbsolutePath(),
                null
        );

        // When
        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(adapterConfig);

        // Then
        assertThat(result).isInstanceOf(Success.class);
        final ParsedConfig parsedConfig = ((Success<ParsedConfig, String>) result).result();

        assertThat(parsedConfig.applicationUri())
                .as("Should extract URI from first certificate")
                .isNotNull()
                .isEqualTo(expectedUri);
    }

    @Test
    void testFromConfig_invalidKeystorePath_failsGracefully() {
        // Given
        final OpcUaSpecificAdapterConfig adapterConfig = createAdapterConfig(
                true,
                "/path/that/does/not/exist/keystore.jks",
                null
        );

        // When
        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(adapterConfig);

        // Then
        assertThat(result).isInstanceOf(Failure.class);
        final String errorMessage = ((Failure<ParsedConfig, String>) result).failure();
        assertThat(errorMessage)
                .contains("Failed to load keypair with chain from keystore");
    }

    private OpcUaSpecificAdapterConfig createAdapterConfig(
            final boolean tlsEnabled,
            final String keystorePath,
            final String truststorePath) {

        final Keystore keystore = keystorePath != null
                ? new Keystore(keystorePath, KEYSTORE_PASSWORD, PRIVATE_KEY_PASSWORD)
                : null;

        final Truststore truststore = truststorePath != null
                ? new Truststore(truststorePath, KEYSTORE_PASSWORD)
                : null;

        final Tls tls = new Tls(tlsEnabled, keystore, truststore);
        final Security security = new Security(SecPolicy.NONE);
        final OpcUaToMqttConfig opcUaToMqttConfig = new OpcUaToMqttConfig(1, 1000);

        return new OpcUaSpecificAdapterConfig(
                TEST_URI,
                false,
                null,  // no auth
                tls,
                opcUaToMqttConfig,
                security
        );
    }
}
