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
package com.hivemq.edge.adapters.opcua.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/// That the superseding `tls-*` elements bind from a configuration file as written.
///
/// Worth its own test because these are the first elements on this adapter whose names carry
/// hyphens: the rest of its configuration is camelCase, so nothing else here would catch a property
/// name that does not match what an operator types.
class TlsSupersedingParsingTest {

    private static final @NotNull ObjectMapper MAPPER = new ObjectMapper();

    private static final @NotNull String URI = "\"uri\":\"opc.tcp://machine.local:4840\"";

    @Test
    void aPemIdentityBindsFromAConfigurationFile() throws Exception {
        final OpcUaSpecificAdapterConfig config = MAPPER.readValue(
                "{"
                        + URI
                        + ",\"tls\":{\"enabled\":true,\"tls-identity\":{"
                        + "\"format\":\"pem\","
                        + "\"key-file\":\"/etc/edge/client.key\","
                        + "\"certificate-file\":\"/etc/edge/client.crt\","
                        + "\"chain-file\":\"/etc/edge/chain.crt\","
                        + "\"key-password-file\":\"/run/secrets/key-pw\"}}}",
                OpcUaSpecificAdapterConfig.class);

        final TlsIdentity identity = config.getTls().tlsIdentity();
        assertThat(identity).isNotNull();
        assertThat(identity.format()).isEqualTo(MaterialFormat.PEM);
        assertThat(identity.keyFile()).isEqualTo("/etc/edge/client.key");
        assertThat(identity.certificateFile()).isEqualTo("/etc/edge/client.crt");
        assertThat(identity.chainFile()).isEqualTo("/etc/edge/chain.crt");
        assertThat(identity.keyPasswordFile()).isEqualTo("/run/secrets/key-pw");
    }

    @Test
    void aTruststoreWithRepeatedSourcesBindsFromAConfigurationFile() throws Exception {
        final OpcUaSpecificAdapterConfig config = MAPPER.readValue(
                "{"
                        + URI
                        + ",\"tls\":{\"enabled\":true,\"tls-truststore\":{"
                        + "\"format\":\"PEM\","
                        + "\"file\":[\"/a.pem\",\"/b.pem\"],"
                        + "\"directory\":[{\"location\":\"/ca.d\",\"may-be-empty\":true}],"
                        + "\"pattern\":[{\"location\":\"/etc/*.pem\"}],"
                        + "\"builtin\":[\"JVM-cacerts\"],"
                        + "\"skip-integrity-check\":true}}}",
                OpcUaSpecificAdapterConfig.class);

        final TlsTruststore truststore = config.getTls().tlsTruststore();
        assertThat(truststore).isNotNull();
        // Upper case on the wire, because matching is case-insensitive like every other enum here.
        assertThat(truststore.format()).isEqualTo(MaterialFormat.PEM);
        assertThat(truststore.files()).containsExactly("/a.pem", "/b.pem");
        assertThat(truststore.directories()).containsExactly(new FileLocation("/ca.d", true));
        // may-be-empty omitted means false, which is the strict reading.
        assertThat(truststore.patterns()).containsExactly(new FileLocation("/etc/*.pem"));
        assertThat(truststore.patterns().get(0).isMayBeEmpty()).isFalse();
        assertThat(truststore.builtins()).containsExactly("JVM-cacerts");
        assertThat(truststore.isSkipIntegrityCheck()).isTrue();
    }

    @Test
    void aReferenceBindsFromAConfigurationFile() throws Exception {
        final OpcUaSpecificAdapterConfig config = MAPPER.readValue(
                "{"
                        + URI
                        + ",\"tls\":{\"enabled\":true,"
                        + "\"tls-allowlist\":{\"reference-name\":\"plant::pinned\"},"
                        + "\"tls-revocationlist\":{\"reference-uuid\":\"a-uuid\"}}}",
                OpcUaSpecificAdapterConfig.class);

        assertThat(config.getTls().tlsAllowlist()).isNotNull();
        assertThat(config.getTls().tlsAllowlist().referenceName()).isEqualTo("plant::pinned");
        assertThat(config.getTls().tlsRevocationlist()).isNotNull();
        assertThat(config.getTls().tlsRevocationlist().referenceUuid()).isEqualTo("a-uuid");
    }

    @Test
    void theSupersededElementsStillBindUnchanged() throws Exception {
        // Every configuration that exists today is written this way.
        final OpcUaSpecificAdapterConfig config = MAPPER.readValue(
                "{"
                        + URI
                        + ",\"tls\":{\"enabled\":true,"
                        + "\"keystore\":{\"path\":\"/c.jks\",\"password\":\"p\",\"privateKeyPassword\":\"k\"},"
                        + "\"truststore\":{\"path\":\"/t.jks\",\"password\":\"q\"},"
                        + "\"allowList\":{\"path\":\"/pinned.txt\"},"
                        + "\"revocationList\":{\"path\":\"/crl.pem\"}}}",
                OpcUaSpecificAdapterConfig.class);

        assertThat(config.getTls().keystore()).isNotNull();
        assertThat(config.getTls().keystore().path()).isEqualTo("/c.jks");
        assertThat(config.getTls().truststore()).isNotNull();
        assertThat(config.getTls().truststore().password()).isEqualTo("q");
        assertThat(config.getTls().allowList()).isNotNull();
        assertThat(config.getTls().revocationList()).isNotNull();
        // And none of the superseding elements materialise from a configuration that omits them.
        assertThat(config.getTls().tlsIdentity()).isNull();
        assertThat(config.getTls().tlsTruststore()).isNull();
    }

    @Test
    void writingAnUnchangedConfigurationBackOutReturnsWhatWasWritten() throws Exception {
        // The property the whole design rests on: a configuration Edge read and wrote again is the
        // configuration the operator wrote. These elements are serialised by the same records that
        // read them, so a name that round-trips wrongly would silently rewrite an operator's file.
        final String written = "{"
                + URI
                + ",\"tls\":{\"enabled\":true,\"tls-truststore\":{"
                + "\"format\":\"PEM\",\"file\":[\"/a.pem\"],"
                + "\"directory\":[{\"location\":\"/ca.d\",\"may-be-empty\":true}],"
                + "\"skip-integrity-check\":true}}}";

        final OpcUaSpecificAdapterConfig config = MAPPER.readValue(written, OpcUaSpecificAdapterConfig.class);
        final OpcUaSpecificAdapterConfig again =
                MAPPER.readValue(MAPPER.writeValueAsString(config), OpcUaSpecificAdapterConfig.class);

        assertThat(again.getTls().tlsTruststore()).isEqualTo(config.getTls().tlsTruststore());
    }
}
