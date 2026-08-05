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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

/**
 * How configuration values are read.
 *
 * <p>The load-bearing property here is that a misspelled value can never loosen validation. It is
 * reported and then treated as absent, and absent means the strictest available setting — so the
 * observable consequence of a typo is a refused connection next to a warning, never a connection that
 * was checked less than the operator believed.
 */
class TlsChecksParsingTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @ParameterizedTest
    @EnumSource(TlsChecks.class)
    void presetNamesParse(final TlsChecks preset) {
        assertThat(TlsChecks.fromString(preset.name())).isEqualTo(preset);
    }

    @ParameterizedTest
    @CsvSource({
        "self_signed, SELF_SIGNED",
        "SelfSigned, SELF_SIGNED",
        "selfsigned, SELF_SIGNED",
        "no_verification, NO_VERIFICATION",
        "standard, STANDARD",
        "  ALL  , ALL"
    })
    void presetParsingIsForgivingAboutCaseAndUnderscores(final String written, final TlsChecks expected) {
        assertThat(TlsChecks.fromString(written)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void blankMeansUnset(final String written) {
        assertThat(TlsChecks.fromString(written)).isNull();
        assertThat(TrustMode.fromString(written)).isNull();
        assertThat(RevocationCheck.fromString(written)).isNull();
    }

    @Test
    void nullMeansUnset() {
        assertThat(TlsChecks.fromString(null)).isNull();
        assertThat(TrustMode.fromString(null)).isNull();
    }

    @Test
    void anUnknownValueFallsBackToUnsetAndIsLogged() {
        // Deliberately not an exception: adapter configurations are converted in a single pass over
        // every adapter, so throwing here would stop unrelated adapters from being reconfigured. Unset
        // is the safe fallback because every default in this model is the strictest value.
        final ListAppender<ILoggingEvent> appender = attachAppender();
        try {
            assertThat(TlsChecks.fromString("SELFSIGNEND")).isNull();

            assertThat(appender.list).anySatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getFormattedMessage()).contains("SELFSIGNEND").contains("SELF_SIGNED");
            });
        } finally {
            detachAppender(appender);
        }
    }

    @Test
    void everyAxisFallsBackToUnsetOnAnUnknownValue() {
        assertThat(TrustMode.fromString("ANYCERTIFICATE")).isNull();
        assertThat(RevocationCheck.fromString("REQUIRED")).isNull();
        assertThat(KeyUsageCheck.fromString("SERVERAUTHENTICATION")).isNull();
        assertThat(HostnameCheck.fromString("STRICT")).isNull();
        assertThat(ValidityCheck.fromString("EXPIRY")).isNull();
        assertThat(SanUriCheck.fromString("URI")).isNull();
    }

    @Test
    void aMisspelledPresetCanOnlyEverProduceMoreValidation() throws Exception {
        // The direction that matters. 'NOVERIFY' does not quietly become NO_VERIFICATION; it becomes
        // nothing, and the configuration falls back to the STANDARD default.
        final Tls tls = MAPPER.readValue("{\"enabled\":true,\"tlsChecks\":\"NOVERIFY\"}", Tls.class);

        assertThat(tls.tlsChecks()).isNull();
        assertThat(TlsChecksProjection.project(tls)).isEqualTo(TlsChecksProjection.fromPreset(TlsChecks.STANDARD));
    }

    @Test
    void aMisspelledAxisValueFallsBackToTheStrictestValue() throws Exception {
        // 'TRUST' was the name on a pre-release branch; it is not a trustMode value. It must not be
        // read as ANY_CERT, and it must not be read as "no trust mode configured, do whatever".
        final Tls tls = MAPPER.readValue("{\"enabled\":true,\"tlsChecksFull\":{\"trustMode\":\"TRUST\"}}", Tls.class);

        assertThat(TlsChecksProjection.project(tls).trustMode()).isEqualTo(TrustMode.CHAIN);
    }

    @Test
    void anAllowListElementWithNoPathParsesToANullPath() throws Exception {
        // `<allowList/>` is a plausible thing for an operator to write, and Jackson binds the missing
        // element to a null path rather than refusing it. Pinned here because the downstream code
        // reads that path: if the component is ever declared non-null again, the null still arrives
        // and the projection guarding it becomes a NullPointerException at adapter start.
        final Tls tls =
                MAPPER.readValue("{\"enabled\":true,\"tlsChecks\":\"SELF_SIGNED\",\"allowList\":{}}", Tls.class);

        assertThat(tls.allowList()).isNotNull();
        assertThat(tls.allowList().path()).isNull();
        assertThatThrownBy(() -> TlsChecksProjection.project(tls))
                .isInstanceOf(TlsChecksProjection.InvalidTlsChecksConfigException.class);
    }

    @Test
    void theRetiredTrustLevelFieldIsRejectedUnderJacksonDefaults() {
        // trustLevel never shipped, but it existed on a pre-release branch. Under Jackson's default
        // settings a field the model does not know is an error rather than a value quietly dropped.
        assertThatThrownBy(() -> MAPPER.readValue("{\"enabled\":true,\"trustLevel\":\"TRUST\"}", Tls.class))
                .isInstanceOf(com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException.class)
                .hasMessageContaining("trustLevel");
    }

    private static @NotNull ListAppender<ILoggingEvent> attachAppender() {
        final Logger logger = (Logger) LoggerFactory.getLogger("com.hivemq.edge.adapters.opcua.config.EnumParsing");
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private static void detachAppender(final @NotNull ListAppender<ILoggingEvent> appender) {
        ((Logger) LoggerFactory.getLogger("com.hivemq.edge.adapters.opcua.config.EnumParsing"))
                .detachAppender(appender);
        appender.stop();
    }
}
