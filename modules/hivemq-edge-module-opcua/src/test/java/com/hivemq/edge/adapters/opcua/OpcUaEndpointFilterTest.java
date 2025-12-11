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
package com.hivemq.edge.adapters.opcua;

import static com.hivemq.edge.adapters.opcua.Constants.DEFAULT_SECURITY_POLICY;
import static com.hivemq.edge.adapters.opcua.config.SecPolicy.AES128_SHA256_RSAOAEP;
import static com.hivemq.edge.adapters.opcua.config.SecPolicy.AES256_SHA256_RSAPSS;
import static com.hivemq.edge.adapters.opcua.config.SecPolicy.BASIC128RSA15;
import static com.hivemq.edge.adapters.opcua.config.SecPolicy.BASIC256;
import static com.hivemq.edge.adapters.opcua.config.SecPolicy.BASIC256SHA256;
import static com.hivemq.edge.adapters.opcua.config.SecPolicy.NONE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.hivemq.edge.adapters.opcua.client.OpcUaEndpointFilter;
import com.hivemq.edge.adapters.opcua.config.Keystore;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.SecPolicy;
import com.hivemq.edge.adapters.opcua.config.Tls;
import com.hivemq.edge.adapters.opcua.config.TlsChecks;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

class OpcUaEndpointFilterTest {

    private final @NotNull List<String> allUris = convertToUri(
            List.of(NONE, BASIC128RSA15, BASIC256, BASIC256SHA256, AES128_SHA256_RSAOAEP, AES256_SHA256_RSAPSS));

    @Test
    public void whenSingleEndpointConfigSet_thenPickCorrectEndpoint() {
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(
                "opc.tcp://127.0.0.1:49320",
                false,
                null,
                null,
                new Tls(true, TlsChecks.NONE, new Keystore("path", "pass", "passPriv"), null),
                null,
                null,
                null,
                null);

        final String configUri = convertToUri(BASIC256SHA256);
        final OpcUaEndpointFilter opcUaEndpointFilter = new OpcUaEndpointFilter("id", configUri, null, config);

        final Optional<EndpointDescription> result =
                opcUaEndpointFilter.apply(convertToEndpointDescription(allUris, MessageSecurityMode.SignAndEncrypt));

        assertThat(result)
                .isPresent()
                .get()
                .extracting(EndpointDescription::getSecurityPolicyUri)
                .isEqualTo(BASIC256SHA256.getSecurityPolicy().getUri());
    }

    @Test
    public void whenSingleEndpointConfigSetAndNoKeystorePresent_thenPickNoEndpoint() {
        final OpcUaSpecificAdapterConfig config =
                new OpcUaSpecificAdapterConfig("opc.tcp://127.0.0.1:49320", false, null, null, null, null, null, null, null);

        final String configUri = convertToUri(BASIC256SHA256);
        final OpcUaEndpointFilter opcUaEndpointFilter = new OpcUaEndpointFilter("id", configUri, null, config);

        final Optional<EndpointDescription> result =
                opcUaEndpointFilter.apply(convertToEndpointDescription(allUris, MessageSecurityMode.SignAndEncrypt));

        assertThat(result.isPresent()).isFalse();
    }

    @Test
    public void whenSingleEndpointConfigSetAndNotAvailOnServer_thenPickNoEndpoint() {
        final String configUri = convertToUri(BASIC256SHA256);
        final OpcUaSpecificAdapterConfig config =
                new OpcUaSpecificAdapterConfig("opc.tcp://127.0.0.1:49320", false, null, null, null, null, null, null, null);
        final OpcUaEndpointFilter opcUaEndpointFilter = new OpcUaEndpointFilter("id", configUri, null, config);

        final Optional<EndpointDescription> result = opcUaEndpointFilter.apply(
                convertToEndpointDescription(convertToUri(List.of(NONE)), MessageSecurityMode.None));

        assertThat(result.isPresent()).isFalse();
    }

    @Test
    public void whenDefaultEndpointConfigSet_thenPickMatchingEndpoint() {
        final OpcUaSpecificAdapterConfig config =
                new OpcUaSpecificAdapterConfig("opc.tcp://127.0.0.1:49320", false, null, null, null, null, null, null, null);
        final OpcUaEndpointFilter opcUaEndpointFilter = new OpcUaEndpointFilter("id", convertToUri(
                DEFAULT_SECURITY_POLICY), null, config);

        final Optional<EndpointDescription> result =
                opcUaEndpointFilter.apply(convertToEndpointDescription(allUris, MessageSecurityMode.None));

        assertThat(result)
                .isPresent()
                .get()
                .extracting(EndpointDescription::getSecurityPolicyUri)
                .isEqualTo(NONE.getSecurityPolicy().getUri());
    }

    @Test
    public void whenMessageSecurityModeSpecified_thenFilterByMode() {
        // Given - Config with keystore and specific mode preference
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(
                "opc.tcp://127.0.0.1:49320",
                false,
                null,
                null,
                new Tls(true, TlsChecks.NONE, new Keystore("path", "pass", "passPriv"), null),
                null,
                null,
                null,
                null);

        final String policyUri = convertToUri(BASIC256SHA256);

        // Create endpoints with different modes for the same policy
        final List<EndpointDescription> endpoints = new ArrayList<>();
        endpoints.add(new EndpointDescription(
                "opc.tcp://127.0.0.1:49320", null, null, MessageSecurityMode.Sign, policyUri, null, null, null));
        endpoints.add(new EndpointDescription(
                "opc.tcp://127.0.0.1:49320",
                null,
                null,
                MessageSecurityMode.SignAndEncrypt,
                policyUri,
                null,
                null,
                null));

        // When - Filter with Sign mode preference
        final OpcUaEndpointFilter filterForSign =
                new OpcUaEndpointFilter("id", policyUri, MessageSecurityMode.Sign, config);
        final Optional<EndpointDescription> resultSign = filterForSign.apply(endpoints);

        // Then - Sign endpoint is selected
        assertThat(resultSign)
                .isPresent()
                .get()
                .extracting(EndpointDescription::getSecurityMode)
                .isEqualTo(MessageSecurityMode.Sign);

        // When - Filter with SignAndEncrypt mode preference
        final OpcUaEndpointFilter filterForSignAndEncrypt =
                new OpcUaEndpointFilter("id", policyUri, MessageSecurityMode.SignAndEncrypt, config);
        final Optional<EndpointDescription> resultSignAndEncrypt = filterForSignAndEncrypt.apply(endpoints);

        // Then - SignAndEncrypt endpoint is selected
        assertThat(resultSignAndEncrypt)
                .isPresent()
                .get()
                .extracting(EndpointDescription::getSecurityMode)
                .isEqualTo(MessageSecurityMode.SignAndEncrypt);
    }

    @Test
    public void whenNoMessageSecurityModeSpecified_thenAcceptAnyMode() {
        // Given - Config without mode preference (backwards compatibility)
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(
                "opc.tcp://127.0.0.1:49320",
                false,
                null,
                null,
                new Tls(true, TlsChecks.NONE, new Keystore("path", "pass", "passPriv"), null),
                null,
                null,
                null,
                null);

        final String policyUri = convertToUri(BASIC256SHA256);

        // Create endpoints with different modes
        final List<EndpointDescription> endpoints = new ArrayList<>();
        endpoints.add(new EndpointDescription(
                "opc.tcp://127.0.0.1:49320", null, null, MessageSecurityMode.Sign, policyUri, null, null, null));
        endpoints.add(new EndpointDescription(
                "opc.tcp://127.0.0.1:49320",
                null,
                null,
                MessageSecurityMode.SignAndEncrypt,
                policyUri,
                null,
                null,
                null));

        // When - Filter without mode preference (null)
        final OpcUaEndpointFilter filter = new OpcUaEndpointFilter("id", policyUri, null, config);
        final Optional<EndpointDescription> result = filter.apply(endpoints);

        // Then - Any endpoint with matching policy is accepted (backwards compatible)
        assertThat(result)
                .isPresent()
                .get()
                .extracting(EndpointDescription::getSecurityPolicyUri)
                .isEqualTo(policyUri);
    }

    @Test
    public void whenWrongMessageSecurityMode_thenNoEndpointSelected() {
        // Given - Config requesting SignAndEncrypt
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(
                "opc.tcp://127.0.0.1:49320",
                false,
                null,
                null,
                new Tls(true, TlsChecks.NONE, new Keystore("path", "pass", "passPriv"), null),
                null,
                null,
                null,
                null);

        final String policyUri = convertToUri(BASIC256SHA256);

        // Server only offers Sign mode
        final List<EndpointDescription> endpoints = List.of(new EndpointDescription(
                "opc.tcp://127.0.0.1:49320", null, null, MessageSecurityMode.Sign, policyUri, null, null, null));

        // When - Filter with SignAndEncrypt preference but server only has Sign
        final OpcUaEndpointFilter filter =
                new OpcUaEndpointFilter("id", policyUri, MessageSecurityMode.SignAndEncrypt, config);
        final Optional<EndpointDescription> result = filter.apply(endpoints);

        // Then - No endpoint selected (mode mismatch)
        assertThat(result.isPresent()).isFalse();
    }

    @NotNull
    private static List<EndpointDescription> convertToEndpointDescription(
            final @NotNull List<String> allUris, final @Nullable MessageSecurityMode mode) {
        final ArrayList<EndpointDescription> endpointList = allUris.stream()
                .map(policyUri -> new EndpointDescription(
                        "opc.tcp://127.0.0.1:49320", null, null, mode, policyUri, null, null, null))
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(endpointList);
        return endpointList;
    }

    private @NotNull List<String> convertToUri(final @NotNull List<SecPolicy> policies) {
        return policies.stream()
                .map(secPolicy -> secPolicy.getSecurityPolicy().getUri())
                .toList();
    }

    private @NotNull String convertToUri(final @NotNull SecPolicy policy) {
        return policy.getSecurityPolicy().getUri();
    }
}
