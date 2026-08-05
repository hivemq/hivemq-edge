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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Writing an unchanged configuration back out must be a no-op.
 *
 * <p>Edge rewrites the configuration file whenever anything in it is edited through the API, so a
 * model that derives or normalizes values on read would silently rewrite settings the operator never
 * touched — replacing what they wrote with whatever it happened to resolve to. These tests assert the
 * stronger property: what goes in comes back out, byte for byte, including the absence of fields.
 */
class TlsWriteBackTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static void assertRoundTripIsIdentical(final String json) throws Exception {
        final Tls parsed = MAPPER.readValue(json, Tls.class);
        final JsonNode writtenBack = MAPPER.valueToTree(parsed);

        assertThat(writtenBack)
                .as("writing back an unchanged configuration must not alter it")
                .isEqualTo(MAPPER.readTree(json));
    }

    @Test
    void neitherDoorConfigured_staysEmpty() throws Exception {
        // The most important case: an untouched legacy configuration must not sprout derived fields.
        assertRoundTripIsIdentical("{\"enabled\":true}");
    }

    @ParameterizedTest
    @EnumSource(TlsChecks.class)
    void presetIsPreservedVerbatim(final TlsChecks preset) throws Exception {
        assertRoundTripIsIdentical("{\"enabled\":true,\"tlsChecks\":\"" + preset.name() + "\"}");
    }

    @Test
    void presetIsNotExpandedIntoAxes() throws Exception {
        final Tls parsed = MAPPER.readValue("{\"enabled\":true,\"tlsChecks\":\"STANDARD\"}", Tls.class);

        assertThat(parsed.tlsChecks()).isEqualTo(TlsChecks.STANDARD);
        assertThat(parsed.tlsChecksFull())
                .as("the preset must not be expanded into axes at parse time")
                .isNull();
        assertThat(MAPPER.writeValueAsString(parsed))
                .doesNotContain("tlsChecksFull")
                .doesNotContain("trustMode");
    }

    @Test
    void emptyAxesObjectStaysEmpty() throws Exception {
        // The strict defaults must be applied at read time only. Materializing them here would turn
        // "I want everything" into six values the operator now has to maintain by hand.
        assertRoundTripIsIdentical("{\"enabled\":true,\"tlsChecksFull\":{}}");
    }

    @Test
    void partiallySpecifiedAxesKeepExactlyWhatWasWritten() throws Exception {
        assertRoundTripIsIdentical(
                "{\"enabled\":true,\"tlsChecksFull\":{\"trustMode\":\"ANY_CERT\"," + "\"revocation\":\"NONE\"}}");
    }

    @ParameterizedTest
    @MethodSource("everyAxisAlone")
    void everySingleAxisRoundTrips(final String axisJson) throws Exception {
        assertRoundTripIsIdentical("{\"enabled\":true,\"tlsChecksFull\":{" + axisJson + "}}");
    }

    @Test
    void fullyPopulatedConfigurationRoundTrips() throws Exception {
        assertRoundTripIsIdentical("{\"enabled\":true,"
                + "\"tlsChecksFull\":{\"trustMode\":\"ALLOW_LIST\",\"sanUri\":\"APPLICATION_URI\","
                + "\"hostname\":\"HOSTNAME\",\"validity\":\"NOT_BEFORE_OR_AFTER\",\"revocation\":\"NONE\","
                + "\"keyUsage\":\"NONE\"},"
                + "\"keystore\":{\"path\":\"/k.jks\",\"password\":\"p\",\"privateKeyPassword\":\"q\"},"
                + "\"truststore\":{\"path\":\"/t.jks\",\"password\":\"p\"},"
                + "\"allowList\":{\"path\":\"/fingerprints.txt\"}}");
    }

    @Test
    void allowListRoundTrips() throws Exception {
        assertRoundTripIsIdentical(
                "{\"enabled\":true,\"tlsChecks\":\"SELF_SIGNED\",\"allowList\":{\"path\":\"/fingerprints.txt\"}}");
    }

    @Test
    void aContradictoryConfigurationSurvivesTheRoundTripUnaltered() throws Exception {
        // Both doors set is a startup error, not a parse-time rewrite: the file must still come back
        // as written, so the operator can see and fix what they wrote.
        assertRoundTripIsIdentical("{\"enabled\":true,\"tlsChecks\":\"STANDARD\",\"tlsChecksFull\":{}}");
    }

    static List<String> everyAxisAlone() {
        return List.of(
                "\"trustMode\":\"CHAIN\"",
                "\"trustMode\":\"ALLOW_LIST\"",
                "\"trustMode\":\"ANY_CERT\"",
                "\"sanUri\":\"NONE\"",
                "\"sanUri\":\"APPLICATION_URI\"",
                "\"hostname\":\"NONE\"",
                "\"hostname\":\"HOSTNAME\"",
                "\"validity\":\"NONE\"",
                "\"validity\":\"NOT_BEFORE_OR_AFTER\"",
                "\"revocation\":\"NONE\"",
                "\"revocation\":\"CHECK\"",
                "\"revocation\":\"REQUIRE_CRLS\"",
                "\"keyUsage\":\"NONE\"",
                "\"keyUsage\":\"KEY_USAGE\"",
                "\"keyUsage\":\"SERVER_AUTH\"");
    }
}
