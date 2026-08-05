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
    void anEmptyTlsChecksFullTextMeansEveryAxisUnset() throws Exception {
        // Edge's XML-to-map conversion hands `<tlsChecksFull/>` over as the empty String, not as an
        // object. That is the documented spelling of "maximum validation", so it binds to an axis set
        // with nothing in it, which resolves to the strictest value on every axis.
        // TlsChecksConfigFileParsingTest drives the same case through a real config.xml.
        final Tls tls = MAPPER.readValue("{\"enabled\":true,\"tlsChecksFull\":\"\"}", Tls.class);

        assertThat(tls.tlsChecksFull()).isEqualTo(TlsChecksFull.allAxesUnset());
        assertThat(TlsChecksProjection.project(tls))
                .isEqualTo(TlsChecksProjection.fromAxes(TlsChecksFull.allAxesUnset()));
    }

    @Test
    void tlsChecksFullTextThatIsNotEmptyIsRefusedRatherThanResolved() throws Exception {
        // An empty first axis collapses the element to the concatenated text of the remaining ones, so
        // `<trustMode></trustMode><revocation>NONE</revocation>` arrives here as "NONE". Which axis
        // that text belonged to is unrecoverable - revocation=NONE and hostname=NONE are the same
        // string - so the operator has configured validation settings that cannot be read. Resolving
        // them to maximum validation would be safe in the checking direction but would discard a
        // security setting they explicitly wrote, leaving a log line as the only trace. The adapter is
        // refused instead, quoting back what was found.
        final Tls tls = MAPPER.readValue("{\"enabled\":true,\"tlsChecksFull\":\"NONE\"}", Tls.class);

        assertThat(tls.tlsChecksFull()).isNotNull();
        assertThat(tls.tlsChecksFull().collapsedText()).isEqualTo("NONE");
        assertThatThrownBy(() -> TlsChecksProjection.project(tls))
                .isInstanceOf(TlsChecksProjection.InvalidTlsChecksConfigException.class)
                .hasMessageContaining("could not be read")
                .hasMessageContaining("'NONE'")
                .hasMessageContaining("<trustMode></trustMode>")
                .hasMessageContaining("<tlsChecksFull/> is valid");
    }

    @Test
    void aCollapsedTlsChecksFullIsRefusedAheadOfTheBothDoorsError() throws Exception {
        // Both doors are set here too, but the unreadable axes are the mistake worth naming: the
        // both-doors message would send the operator to delete a setting rather than repair it.
        final Tls tls =
                MAPPER.readValue("{\"enabled\":true,\"tlsChecks\":\"ALL\",\"tlsChecksFull\":\"NONE\"}", Tls.class);

        assertThatThrownBy(() -> TlsChecksProjection.project(tls))
                .isInstanceOf(TlsChecksProjection.InvalidTlsChecksConfigException.class)
                .hasMessageContaining("could not be read");
    }

    @Test
    void aCollapsedTlsChecksFullIsNotEqualToAnEmptyOne() throws Exception {
        // The two must keep comparing unequal. If they did not, an adapter whose axes became
        // unreadable would look unchanged to ProtocolAdapterManager's config comparison, which skips
        // an update when the new config equals the running one.
        final Tls collapsed = MAPPER.readValue("{\"enabled\":true,\"tlsChecksFull\":\"NONE\"}", Tls.class);

        assertThat(collapsed.tlsChecksFull()).isNotEqualTo(TlsChecksFull.allAxesUnset());
    }

    @Test
    void theCollapsedMarkerIsNotPartOfTheConfigurationSurface() throws Exception {
        // It must not survive a writeback, and an operator must not be able to forge it - a
        // configuration that writes it is read as though it had not, so the marker can only ever be
        // set by the collapse actually happening.
        final Tls collapsed = MAPPER.readValue("{\"enabled\":true,\"tlsChecksFull\":\"NONE\"}", Tls.class);
        assertThat(MAPPER.writeValueAsString(collapsed)).doesNotContain("collapsedText");

        final Tls forged = MAPPER.readValue(
                "{\"enabled\":true,\"tlsChecksFull\":{\"collapsedText\":\"NONE\",\"trustMode\":\"CHAIN\"}}", Tls.class);
        assertThat(forged.tlsChecksFull()).isNotNull();
        assertThat(forged.tlsChecksFull().collapsedText()).isNull();
        assertThat(forged.tlsChecksFull().trustMode()).isEqualTo(TrustMode.CHAIN);
        assertThat(TlsChecksProjection.project(forged).trustMode())
                .as("a forged marker does not refuse the adapter")
                .isEqualTo(TrustMode.CHAIN);
    }

    @Test
    void anEmptyAllowListTextMeansNoPathConfigured() throws Exception {
        // Both `<allowList/>` and `<allowList><path></path></allowList>` collapse to the empty String.
        // Neither configures a path, and the missing path is reported by the projection as this
        // adapter's start-up error rather than as a deserialization failure that takes the whole
        // adapter refresh down.
        final Tls tls =
                MAPPER.readValue("{\"enabled\":true,\"tlsChecks\":\"SELF_SIGNED\",\"allowList\":\"\"}", Tls.class);

        assertThat(tls.allowList()).isNotNull();
        assertThat(tls.allowList().path()).isNull();
        assertThatThrownBy(() -> TlsChecksProjection.project(tls))
                .isInstanceOf(TlsChecksProjection.InvalidTlsChecksConfigException.class);
    }

    @Test
    void anAllowListWrittenAsTextIsNotGuessedToBeThePath() throws Exception {
        // `<allowList>/opt/hivemq/conf/fingerprints.txt</allowList>` is a plausible mistake, and it is
        // deliberately not read as the path. Silently accepting it would make the nested `<path>`
        // element optional by accident; the operator is told what to write instead.
        final ListAppender<ILoggingEvent> appender = attachAppender(AllowList.class);
        try {
            final Tls tls = MAPPER.readValue(
                    "{\"enabled\":true,\"tlsChecks\":\"SELF_SIGNED\",\"allowList\":\"/opt/fingerprints.txt\"}",
                    Tls.class);

            assertThat(tls.allowList()).isNotNull();
            assertThat(tls.allowList().path()).isNull();
            assertThat(appender.list).anySatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getFormattedMessage())
                        .contains("/opt/fingerprints.txt")
                        .contains("<allowList><path>");
            });
        } finally {
            detachAppender(AllowList.class, appender);
        }
    }

    @Test
    void aCollapsedElementCanNeverLoosenValidation() throws Exception {
        // The invariant behind both creators, in its two halves. A blank collapse is the documented
        // "maximum validation" and resolves to the strictest setting on every axis; any other text is
        // refused outright. Neither outcome can ever check less than the operator asked for.
        for (final String blank : new String[] {"", "  "}) {
            final Tls tls = MAPPER.readValue("{\"enabled\":true,\"tlsChecksFull\":\"" + blank + "\"}", Tls.class);

            assertThat(TlsChecksProjection.project(tls))
                    .as("blank collapse '%s'", blank)
                    .isEqualTo(new EffectiveChecks(
                            TrustMode.CHAIN,
                            SanUriCheck.APPLICATION_URI,
                            HostnameCheck.HOSTNAME,
                            ValidityCheck.NOT_BEFORE_OR_AFTER,
                            RevocationCheck.REQUIRE_CRLS,
                            KeyUsageCheck.SERVER_AUTH));
        }

        for (final String text : new String[] {"NONE", "NONE ANY_CERT", "CHAIN"}) {
            final Tls tls = MAPPER.readValue("{\"enabled\":true,\"tlsChecksFull\":\"" + text + "\"}", Tls.class);

            assertThatThrownBy(() -> TlsChecksProjection.project(tls))
                    .as("collapsed text '%s'", text)
                    .isInstanceOf(TlsChecksProjection.InvalidTlsChecksConfigException.class);
        }
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
        return attachAppender(EnumParsing.class);
    }

    private static @NotNull ListAppender<ILoggingEvent> attachAppender(final @NotNull Class<?> type) {
        final Logger logger = (Logger) LoggerFactory.getLogger(type);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private static void detachAppender(final @NotNull ListAppender<ILoggingEvent> appender) {
        detachAppender(EnumParsing.class, appender);
    }

    private static void detachAppender(
            final @NotNull Class<?> type, final @NotNull ListAppender<ILoggingEvent> appender) {
        ((Logger) LoggerFactory.getLogger(type)).detachAppender(appender);
        appender.stop();
    }
}
