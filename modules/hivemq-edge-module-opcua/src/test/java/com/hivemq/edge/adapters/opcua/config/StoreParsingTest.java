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

import static com.hivemq.protocols.ProtocolAdapterUtils.createProtocolAdapterMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * How the {@code keystore} and {@code truststore} elements are read when the configuration layer hands
 * them over as text.
 *
 * <p>Edge's XML-to-map conversion replaces a nested element with its text content whenever the
 * element's first child element is empty, so {@code <truststore/>} arrives as {@code ""} and
 * {@code <truststore><path></path><password>pw</password></truststore>} arrives as {@code "pw"}. Two
 * different meanings from an operator's point of view, and before the creators below both produced the
 * same thing at this layer: the first was coerced to {@code null} and the second failed with a raw
 * {@code Cannot construct instance of Truststore} error that named neither the element nor the fix.
 *
 * <p>The empty case keeps meaning "not configured", which is what the "system truststore" example in
 * the documentation writes. What the store does when it <em>is</em> configured but its path is blank is
 * {@code ParsedConfigTest}'s subject, not this one.
 */
class StoreParsingTest {

    /** The mapper the API path uses; the file path adds only the unknown-property reporting. */
    private static final @NotNull ObjectMapper MAPPER = createProtocolAdapterMapper(new ObjectMapper());

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void anEmptyTruststoreElementMeansNoTruststore(final @NotNull String collapsed) throws Exception {
        // `<truststore/>` - and the JVM cacerts, which is exactly what omitting the element does.
        final Tls tls = MAPPER.readValue("{\"enabled\":true,\"truststore\":\"" + collapsed + "\"}", Tls.class);

        assertThat(tls.truststore()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void anEmptyKeystoreElementMeansNoKeystore(final @NotNull String collapsed) throws Exception {
        final Tls tls = MAPPER.readValue("{\"enabled\":true,\"keystore\":\"" + collapsed + "\"}", Tls.class);

        assertThat(tls.keystore()).isNull();
    }

    @Test
    void aCollapsedTruststoreIsRejectedNamingTheElementAndTheText() {
        // `<truststore><path></path><password>pw</password></truststore>` arrives as "pw". Which
        // element that text belonged to cannot be recovered, and guessing would mean choosing trust
        // anchors the operator did not write.
        assertThatThrownBy(() -> MAPPER.readValue("{\"enabled\":true,\"truststore\":\"pw\"}", Tls.class))
                .hasMessageContaining("'truststore'")
                .hasMessageContaining("could not be read")
                .hasMessageContaining("'pw'")
                .hasMessageContaining("<truststore/> is valid");
    }

    @Test
    void aCollapsedKeystoreIsRejectedNamingTheElementAndTheText() {
        assertThatThrownBy(() -> MAPPER.readValue("{\"enabled\":true,\"keystore\":\"secret\"}", Tls.class))
                .hasMessageContaining("'keystore'")
                .hasMessageContaining("could not be read")
                .hasMessageContaining("'secret'")
                .hasMessageContaining("<keystore/> is valid");
    }

    @Test
    void aStoreWrittenAsAnObjectIsUnaffected() {
        // The creators must not shadow the ordinary binding path.
        assertThat(readTls("{\"enabled\":true,\"truststore\":{\"path\":\"/t.jks\",\"password\":\"pw\"}}")
                        .truststore())
                .isEqualTo(new Truststore("/t.jks", "pw"));
        assertThat(readTls("{\"enabled\":true,\"keystore\":{\"path\":\"/k.jks\",\"password\":\"pw\","
                                + "\"privateKeyPassword\":\"kp\"}}")
                        .keystore())
                .isEqualTo(new Keystore("/k.jks", "pw", "kp"));
    }

    @Test
    void aBlankPathIsCarriedThroughParsingRatherThanCorrected() {
        // Parsing keeps what the operator wrote; the refusal is ParsedConfig's, at start-up. Rewriting
        // a blank path to null here would make the writeback of an unrelated edit silently delete the
        // element - after which the configuration is valid and the adapter quietly runs on the JVM
        // cacerts, which is the outcome the refusal exists to prevent.
        final Tls tls = readTls("{\"enabled\":true,\"truststore\":{\"path\":\"  \",\"password\":\"pw\"}}");

        assertThat(tls.truststore()).isNotNull();
        assertThat(tls.truststore().path()).isEqualTo("  ");
    }

    private static @NotNull Tls readTls(final @NotNull String json) {
        try {
            return MAPPER.readValue(json, Tls.class);
        } catch (final Exception e) {
            throw new AssertionError(e);
        }
    }
}
