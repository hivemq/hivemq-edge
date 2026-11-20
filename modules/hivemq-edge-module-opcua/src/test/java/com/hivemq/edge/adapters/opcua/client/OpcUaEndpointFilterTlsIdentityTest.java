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

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.edge.adapters.opcua.config.Keystore;
import com.hivemq.edge.adapters.opcua.config.MaterialFormat;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.SecPolicy;
import com.hivemq.edge.adapters.opcua.config.Tls;
import com.hivemq.edge.adapters.opcua.config.TlsChecks;
import com.hivemq.edge.adapters.opcua.config.TlsIdentity;
import java.util.List;
import java.util.Optional;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/// An adapter whose client certificate is written as `tls-identity` is offered secure endpoints, as
/// one written with the superseded `keystore` is.
class OpcUaEndpointFilterTlsIdentityTest {

    private static final @NotNull String SECURE_POLICY_URI =
            SecPolicy.BASIC256SHA256.getSecurityPolicy().getUri();

    /// The superseded `keystore` names a client certificate, so a secure endpoint is selected.
    @Test
    void givenALegacyKeystore_whenASecureEndpointIsOffered_thenItIsSelected() {
        final Tls tls = new Tls(true, TlsChecks.NONE, null, new Keystore("path", "pass", "passPriv"), null, null);

        assertThat(select(tls)).isPresent();
    }

    /// A `tls-identity` alone names the client certificate a secure policy needs; the filter skips a
    /// secure endpoint when it finds neither a `tls-identity` nor a `keystore`.
    @Test
    void givenATlsIdentity_whenASecureEndpointIsOffered_thenItIsSelected() {
        final Tls tls = Tls.superseding(
                true,
                TlsChecks.NONE,
                null,
                new TlsIdentity(MaterialFormat.P12, "/etc/edge/client.p12", "password", null, null),
                null,
                null,
                null);

        assertThat(select(tls))
                .as("a tls-identity must be enough for the adapter to be offered a secure endpoint")
                .isPresent();
    }

    private static @NotNull Optional<EndpointDescription> select(final @NotNull Tls tls) {
        final OpcUaSpecificAdapterConfig config =
                new OpcUaSpecificAdapterConfig("opc.tcp://127.0.0.1:49320", false, null, null, tls, null, null, null);
        final List<EndpointDescription> endpoints = List.of(new EndpointDescription(
                "opc.tcp://127.0.0.1:49320",
                null,
                null,
                MessageSecurityMode.SignAndEncrypt,
                SECURE_POLICY_URI,
                null,
                null,
                null));
        return new OpcUaEndpointFilter("id", SECURE_POLICY_URI, null, config).apply(endpoints);
    }
}
