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
import org.eclipse.milo.opcua.sdk.client.identity.AnonymousProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class OpcUaClientConfiguratorTest {

    private static final String ADAPTER_ID = "test-adapter";
    private static final String EXTRACTED_URI = "urn:hivemq:edge:testclient";

    // Minimal config for tests - defaults will be used for timeout values
    private static final OpcUaSpecificAdapterConfig TEST_CONFIG =
            new OpcUaSpecificAdapterConfig("opc.tcp://test:4840", false, null, null, null, null, null, null);

    @Test
    void testAccept_withExtractedUri_usesExtractedUri() {
        // Given
        // Don't use keyPairWithChain to avoid needing to fully mock the certificate chain
        final ParsedConfig parsedConfig = new ParsedConfig(
                false,  // TLS disabled to avoid certificate configuration
                null,
                null,
                new AnonymousProvider(),
                EXTRACTED_URI  // Application URI from certificate
        );

        final OpcUaClientConfigurator configurator = new OpcUaClientConfigurator(ADAPTER_ID, parsedConfig, TEST_CONFIG);
        final OpcUaClientConfigBuilder configBuilder = spy(new OpcUaClientConfigBuilder());

        // When
        configurator.accept(configBuilder);

        // Then
        final ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(configBuilder).setApplicationUri(uriCaptor.capture());

        assertThat(uriCaptor.getValue())
                .as("Should use extracted Application URI from certificate")
                .isEqualTo(EXTRACTED_URI);
    }

    @Test
    void testAccept_withoutExtractedUri_usesDefaultUri() {
        // Given
        final ParsedConfig parsedConfig = new ParsedConfig(
                false,  // TLS disabled to avoid certificate configuration
                null,
                null,
                new AnonymousProvider(),
                null  // No Application URI from certificate
        );

        final OpcUaClientConfigurator configurator = new OpcUaClientConfigurator(ADAPTER_ID, parsedConfig, TEST_CONFIG);
        final OpcUaClientConfigBuilder configBuilder = spy(new OpcUaClientConfigBuilder());

        // When
        configurator.accept(configBuilder);

        // Then
        final ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(configBuilder).setApplicationUri(uriCaptor.capture());

        assertThat(uriCaptor.getValue())
                .as("Should use default Application URI when certificate URI is not available")
                .isEqualTo(Constants.OPCUA_APPLICATION_URI);
    }

    @Test
    void testAccept_tlsDisabled_usesDefaultUri() {
        // Given
        final ParsedConfig parsedConfig = new ParsedConfig(
                false,  // TLS disabled
                null,
                null,
                new AnonymousProvider(),
                null  // No Application URI
        );

        final OpcUaClientConfigurator configurator = new OpcUaClientConfigurator(ADAPTER_ID, parsedConfig, TEST_CONFIG);
        final OpcUaClientConfigBuilder configBuilder = spy(new OpcUaClientConfigBuilder());

        // When
        configurator.accept(configBuilder);

        // Then
        final ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(configBuilder).setApplicationUri(uriCaptor.capture());

        assertThat(uriCaptor.getValue())
                .as("Should use default Application URI when TLS is disabled")
                .isEqualTo(Constants.OPCUA_APPLICATION_URI);
    }

    @Test
    void testAccept_withExtractedUri_configuresOtherSettings() {
        // Given
        final ParsedConfig parsedConfig = new ParsedConfig(
                false,
                null,
                null,
                new AnonymousProvider(),
                EXTRACTED_URI
        );

        final OpcUaClientConfigurator configurator = new OpcUaClientConfigurator(ADAPTER_ID, parsedConfig, TEST_CONFIG);
        final OpcUaClientConfigBuilder configBuilder = spy(new OpcUaClientConfigBuilder());

        // When
        configurator.accept(configBuilder);

        // Then
        verify(configBuilder).setApplicationUri(EXTRACTED_URI);
        verify(configBuilder).setApplicationName(org.mockito.ArgumentMatchers.any());
        verify(configBuilder).setProductUri(Constants.OPCUA_PRODUCT_URI);
        verify(configBuilder).setSessionName(org.mockito.ArgumentMatchers.any());
        verify(configBuilder).setIdentityProvider(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void testAccept_nullApplicationUri_usesDefault() {
        // Given - Explicitly test null vs not provided
        final ParsedConfig parsedConfig = new ParsedConfig(
                false,  // TLS disabled to avoid certificate configuration
                null,
                null,
                new AnonymousProvider(),
                null  // Explicitly null
        );

        final OpcUaClientConfigurator configurator = new OpcUaClientConfigurator(ADAPTER_ID, parsedConfig, TEST_CONFIG);
        final OpcUaClientConfigBuilder configBuilder = spy(new OpcUaClientConfigBuilder());

        // When
        configurator.accept(configBuilder);

        // Then
        final ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(configBuilder).setApplicationUri(uriCaptor.capture());

        assertThat(uriCaptor.getValue())
                .as("Should fall back to default URI when applicationUri is explicitly null")
                .isEqualTo(Constants.OPCUA_APPLICATION_URI);
    }

    @Test
    void testAccept_withConfiguredApplicationUri_usesConfiguredUri() {
        // Given - Test configured override URI (Priority 1)
        final String configuredUri = "urn:custom:configured:uri";
        final ParsedConfig parsedConfig = new ParsedConfig(
                false,  // TLS disabled to avoid certificate configuration
                null,
                null,
                new AnonymousProvider(),
                configuredUri  // Configured override URI
        );

        final OpcUaClientConfigurator configurator = new OpcUaClientConfigurator(ADAPTER_ID, parsedConfig, TEST_CONFIG);
        final OpcUaClientConfigBuilder configBuilder = spy(new OpcUaClientConfigBuilder());

        // When
        configurator.accept(configBuilder);

        // Then
        final ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(configBuilder).setApplicationUri(uriCaptor.capture());

        assertThat(uriCaptor.getValue())
                .as("Should use configured Application URI override (Priority 1)")
                .isEqualTo(configuredUri);
    }
}
