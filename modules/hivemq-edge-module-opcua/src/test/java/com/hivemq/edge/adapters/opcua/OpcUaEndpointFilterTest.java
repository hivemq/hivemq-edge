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

import com.hivemq.edge.adapters.opcua.client.OpcUaEndpointFilter;
import com.hivemq.edge.adapters.opcua.config.Keystore;
import com.hivemq.edge.adapters.opcua.config.OpcUaAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.SecPolicy;
import com.hivemq.edge.adapters.opcua.config.Tls;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.hivemq.edge.adapters.opcua.config.SecPolicy.AES128_SHA256_RSAOAEP;
import static com.hivemq.edge.adapters.opcua.config.SecPolicy.AES256_SHA256_RSAPSS;
import static com.hivemq.edge.adapters.opcua.config.SecPolicy.BASIC128RSA15;
import static com.hivemq.edge.adapters.opcua.config.SecPolicy.BASIC256;
import static com.hivemq.edge.adapters.opcua.config.SecPolicy.BASIC256SHA256;
import static com.hivemq.edge.adapters.opcua.config.SecPolicy.DEFAULT;
import static com.hivemq.edge.adapters.opcua.config.SecPolicy.NONE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class OpcUaEndpointFilterTest {

    private final @NotNull List<String> allUris = convertToUri(List.of(NONE,
            BASIC128RSA15,
            BASIC256,
            BASIC256SHA256,
            AES128_SHA256_RSAOAEP,
            AES256_SHA256_RSAPSS));

    @Test
    public void whenSingleEndpointConfigSet_thenPickCorrectEndpoint() {
        final OpcUaAdapterConfig config = new OpcUaAdapterConfig("id",
                "opc.tcp://127.0.0.1:49320",
                false,
                null,
                new Tls(true, new Keystore("path", null, null), null),
                null,
                null);

        final String configUri = convertToUri(BASIC256SHA256);
        final OpcUaEndpointFilter opcUaEndpointFilter = new OpcUaEndpointFilter(configUri, config);

        final Optional<EndpointDescription> result = opcUaEndpointFilter.apply(convertToEndpointDescription(allUris));

        assertThat(result.isPresent()).isTrue();
        assertThat(BASIC256SHA256.getSecurityPolicy().getUri()).isEqualTo(result.get().getSecurityPolicyUri());
    }

    @Test
    public void whenSingleEndpointConfigSetAndNoKeystorePresent_thenPickNoEndpoint() {
        final OpcUaAdapterConfig config =
                new OpcUaAdapterConfig(
                        "id",
                        "opc.tcp://127.0.0.1:49320",
                        false,
                        null,
                        null,
                        null,
                        null);

        final String configUri = convertToUri(BASIC256SHA256);
        final OpcUaEndpointFilter opcUaEndpointFilter = new OpcUaEndpointFilter(configUri, config);

        final Optional<EndpointDescription> result = opcUaEndpointFilter.apply(convertToEndpointDescription(allUris));

        assertThat(result.isPresent()).isFalse();
    }

    @Test
    public void whenSingleEndpointConfigSetAndNotAvailOnServer_thenPickNoEndpoint() {
        final String configUri = convertToUri(BASIC256SHA256);
        final OpcUaAdapterConfig config =
                new OpcUaAdapterConfig(
                        "id",
                        "opc.tcp://127.0.0.1:49320",
                        false,
                        null,
                        null,
                        null,
                        null);
        final OpcUaEndpointFilter opcUaEndpointFilter = new OpcUaEndpointFilter(configUri, config);

        final Optional<EndpointDescription> result =
                opcUaEndpointFilter.apply(convertToEndpointDescription(convertToUri(List.of(NONE))));

        assertThat(result.isPresent()).isFalse();
    }

    @Test
    public void whenDefaultEndpointConfigSet_thenPickMatchingEndpoint() {
        final OpcUaAdapterConfig config =
                new OpcUaAdapterConfig("id",
                        "opc.tcp://127.0.0.1:49320",
                        false,
                        null,
                        null,
                        null,
                        null);
        final OpcUaEndpointFilter opcUaEndpointFilter = new OpcUaEndpointFilter(convertToUri(DEFAULT), config);

        final Optional<EndpointDescription> result = opcUaEndpointFilter.apply(convertToEndpointDescription(allUris));

        assertThat(result.isPresent()).isTrue();
        assertThat(NONE.getSecurityPolicy().getUri()).isEqualTo(result.get().getSecurityPolicyUri());
    }

    @NotNull
    private static List<EndpointDescription> convertToEndpointDescription(List<String> allUris) {
        final ArrayList<EndpointDescription> endpointList = allUris.stream()
                .map(policyUri -> new EndpointDescription("opc.tcp://127.0.0.1:49320",
                        null,
                        null,
                        null,
                        policyUri,
                        null,
                        null,
                        null))
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(endpointList);
        return endpointList;
    }

    private @NotNull List<String> convertToUri(final @NotNull List<SecPolicy> policies) {
        return policies.stream()
                .map(secPolicy -> secPolicy.getSecurityPolicy().getUri())
                .collect(Collectors.toUnmodifiableList());
    }

    private @NotNull String convertToUri(final @NotNull SecPolicy policy) {
        return policy.getSecurityPolicy().getUri();
    }

}
