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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * The {@code revocationList} element as configuration: how it parses, what a mistake in it does, and
 * that writing an unchanged configuration back out leaves it exactly as written.
 *
 * <p>It is the fourth certificate input beside {@code keystore}, {@code truststore} and
 * {@code allowList}, and it inherits their rules deliberately rather than inventing its own.
 */
class RevocationListConfigTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void aPathIsParsed() throws Exception {
        final Tls tls =
                mapper.readValue("{\"enabled\":true,\"revocationList\":{\"path\":\"/etc/edge/ca.crl\"}}", Tls.class);

        assertThat(tls.revocationList()).isNotNull();
        assertThat(tls.revocationList().path()).isEqualTo("/etc/edge/ca.crl");
    }

    @Test
    void absenceIsTheNormalCase() throws Exception {
        // Only a path through a CA needs one, so most configurations will never mention it.
        assertThat(mapper.readValue("{\"enabled\":true}", Tls.class).revocationList())
                .isNull();
    }

    @Test
    void aMisspelledChildRefusesTheAdapterRatherThanLeavingThePathUnset() throws Exception {
        // <pth> for <path>: without the trap this silently becomes "no revocation list configured",
        // and the operator debugs a Bad_CertificateRevocationUnknown with the file sitting right there.
        final Tls tls =
                mapper.readValue("{\"enabled\":true,\"revocationList\":{\"pth\":\"/etc/edge/ca.crl\"}}", Tls.class);

        assertThatThrownBy(() -> TlsChecksProjection.project(tls))
                .isInstanceOf(TlsChecksProjection.InvalidTlsChecksConfigException.class)
                .hasMessageContaining("'pth'")
                .hasMessageContaining("revocationList");
    }

    @Test
    void aCollapsedElementYieldsNoPathRatherThanFailingTheWholeAdapter() throws Exception {
        // `<revocationList/>` and `<revocationList><path></path></revocationList>` both arrive as "".
        final Tls tls = mapper.readValue("{\"enabled\":true,\"revocationList\":\"\"}", Tls.class);

        assertThat(tls.revocationList()).isNotNull();
        assertThat(tls.revocationList().path()).isNull();
        assertThat(TlsChecksProjection.hasRevocationListPath(tls.revocationList()))
                .isFalse();
    }

    @Test
    void textIsNotGuessedAtAsThePath() throws Exception {
        // An operator who wrote the path directly instead of nesting it should be told, not guessed at.
        final Tls tls = mapper.readValue("{\"enabled\":true,\"revocationList\":\"/etc/edge/ca.crl\"}", Tls.class);

        assertThat(tls.revocationList().path()).isNull();
    }

    @Test
    void writingAnUnchangedConfigurationBackOutIsANoOp() throws Exception {
        final String json = "{\"enabled\":true,\"revocationList\":{\"path\":\"/etc/edge/ca.crl\"}}";

        final JsonNode writtenBack = mapper.valueToTree(mapper.readValue(json, Tls.class));

        assertThat(writtenBack)
                .as("the write-back guarantee covers every certificate input, not just the ones that existed first")
                .isEqualTo(mapper.readTree(json));
    }

    @Test
    void aConfigurationWithoutOneDoesNotSproutOne() throws Exception {
        // The RJSF trap: a schema default here would be written into adapters that never set it.
        final String json = "{\"enabled\":true,\"tlsChecks\":\"SELF_SIGNED\"}";

        assertThat(mapper.writeValueAsString(mapper.readValue(json, Tls.class))).doesNotContain("revocationList");
    }

    @Test
    void presenceIsDecidedByAUsablePathNotByTheElement() {
        assertThat(TlsChecksProjection.hasRevocationListPath(null)).isFalse();
        assertThat(TlsChecksProjection.hasRevocationListPath(new RevocationList(null)))
                .isFalse();
        assertThat(TlsChecksProjection.hasRevocationListPath(new RevocationList("   ")))
                .as("a whitespace path is not a path")
                .isFalse();
        assertThat(TlsChecksProjection.hasRevocationListPath(new RevocationList("/etc/edge/ca.crl")))
                .isTrue();
    }

    @Test
    void configuringOneDoesNotDisturbTheAxes() throws Exception {
        // The revocation list is an input, not an axis: supplying it must not change what is checked.
        final Tls withList = mapper.readValue(
                "{\"enabled\":true,\"tlsChecks\":\"STANDARD\",\"revocationList\":{\"path\":\"/etc/edge/ca.crl\"}}",
                Tls.class);
        final Tls without = mapper.readValue("{\"enabled\":true,\"tlsChecks\":\"STANDARD\"}", Tls.class);

        assertThat(TlsChecksProjection.project(withList)).isEqualTo(TlsChecksProjection.project(without));
    }
}
