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
import com.fasterxml.jackson.databind.JsonMappingException;
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
 * <p>The load-bearing property here is that a mistake in a certificate-validation setting can never
 * loosen validation. A misspelled <em>value</em> rejects the configuration outright — "unset" is not a
 * safe fallback for the preset door, whose default {@code STANDARD} checks less than {@code ALL} — and
 * a misspelled setting <em>name</em> is trapped through deserialization and refuses the adapter at
 * start-up. The observable consequence of a typo is always a refused configuration next to a message
 * naming it, never a connection that was checked less than the operator believed.
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
    void anUnknownValueIsRejectedNamingThePermittedValues() {
        // Deliberately an exception, not a fallback: "unset" resolves to STANDARD on the preset door,
        // which checks less than ALL - so treating a typo as unset can weaken validation. Rejection is
        // contained: ProtocolAdapterManager converts each adapter's configuration in isolation, so a
        // typo in one adapter cannot stop any other adapter from being reconfigured.
        assertThatThrownBy(() -> TlsChecks.fromString("SELFSIGNEND"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'SELFSIGNEND'")
                .hasMessageContaining("SELF_SIGNED")
                .hasMessageContaining("NO_VERIFICATION");
    }

    @Test
    void everyAxisRejectsAnUnknownValue() {
        assertThatThrownBy(() -> TrustMode.fromString("ANYCERTIFICATE")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RevocationCheck.fromString("REQUIRED")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> KeyUsageCheck.fromString("SERVERAUTHENTICATION"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HostnameCheck.fromString("STRICT")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ValidityCheck.fromString("EXPIRY")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SanUriCheck.fromString("URI")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aMisspelledPresetIsRejectedRatherThanRunAsStandard() {
        // The case that forced the rejection design: 'ALLL' must not quietly become the STANDARD
        // default, because STANDARD checks neither hostname nor key usage - the two checks the
        // operator was asking ALL to enforce. A configuration that cannot be read is refused, with
        // the offending value named in the failure.
        assertThatThrownBy(() -> MAPPER.readValue("{\"enabled\":true,\"tlsChecks\":\"ALLL\"}", Tls.class))
                .isInstanceOf(JsonMappingException.class)
                .hasMessageContaining("'ALLL'")
                .hasMessageContaining("TlsChecks");
    }

    @Test
    void aMisspelledPresetAlongsideAxesIsRejectedNeverAxesOnly() {
        // If the invalid preset were parsed to "unset", the mutual-exclusion check would no longer see
        // both doors and the axes would be applied on their own - running a configuration the operator
        // never wrote. The rejection has to fire before any of that can be reasoned about.
        assertThatThrownBy(() -> MAPPER.readValue(
                        "{\"enabled\":true,\"tlsChecks\":\"ALLL\",\"tlsChecksFull\":{\"trustMode\":\"ANY_CERT\"}}",
                        Tls.class))
                .isInstanceOf(JsonMappingException.class)
                .hasMessageContaining("'ALLL'");
    }

    @Test
    void aMisspelledAxisValueIsRejected() {
        // 'TRUST' was the name on a pre-release branch; it is not a trustMode value. It must not be
        // read as ANY_CERT, and it must not be read as "no trust mode configured, do whatever".
        assertThatThrownBy(() ->
                        MAPPER.readValue("{\"enabled\":true,\"tlsChecksFull\":{\"trustMode\":\"TRUST\"}}", Tls.class))
                .isInstanceOf(JsonMappingException.class)
                .hasMessageContaining("'TRUST'")
                .hasMessageContaining("TrustMode");
    }

    @Test
    void aMisspelledSettingNameInsideTlsRefusesTheAdapter() throws Exception {
        // <tlsCheks>ALL</tlsCheks>: the name is wrong, not the value. The application-wide handling
        // for unknown adapter settings warns and drops, which here would leave neither door configured
        // and quietly run the adapter under the STANDARD default - weaker than the ALL the operator
        // wrote. The entry is trapped through deserialization instead and refuses the adapter.
        final Tls tls = MAPPER.readValue("{\"enabled\":true,\"tlsCheks\":\"ALL\"}", Tls.class);

        assertThat(tls.unknownSettings()).containsKey("tlsCheks");
        assertThatThrownBy(() -> TlsChecksProjection.project(tls))
                .isInstanceOf(TlsChecksProjection.InvalidTlsChecksConfigException.class)
                .hasMessageContaining("'tlsCheks'")
                .hasMessageContaining("tlsChecks");
    }

    @Test
    void aMisspelledAxisNameInsideTlsChecksFullRefusesTheAdapter() throws Exception {
        // A misspelled axis name would otherwise mean "axis omitted", which resolves to the strictest
        // value - safe in the checking direction, but it discards a security setting the operator
        // explicitly wrote, and they only learn when the connection fails for the wrong reason.
        final Tls tls = MAPPER.readValue("{\"enabled\":true,\"tlsChecksFull\":{\"trustMod\":\"CHAIN\"}}", Tls.class);

        assertThat(tls.tlsChecksFull()).isNotNull();
        assertThat(tls.tlsChecksFull().unknownSettings()).containsKey("trustMod");
        assertThatThrownBy(() -> TlsChecksProjection.project(tls))
                .isInstanceOf(TlsChecksProjection.InvalidTlsChecksConfigException.class)
                .hasMessageContaining("'trustMod'")
                .hasMessageContaining("trustMode");
    }

    @Test
    void aMisspelledEntryInsideAllowListRefusesTheAdapter() throws Exception {
        // <pth> instead of <path> would otherwise be dropped, leaving the path unset - and the
        // missing-path error would then send the operator to add an element they believe is there.
        final Tls tls = MAPPER.readValue(
                "{\"enabled\":true,\"tlsChecks\":\"SELF_SIGNED\",\"allowList\":{\"pth\":\"/opt/f.txt\"}}", Tls.class);

        assertThat(tls.allowList()).isNotNull();
        assertThat(tls.allowList().unknownSettings()).containsKey("pth");
        assertThatThrownBy(() -> TlsChecksProjection.project(tls))
                .isInstanceOf(TlsChecksProjection.InvalidTlsChecksConfigException.class)
                .hasMessageContaining("'pth'")
                .hasMessageContaining("path");
    }

    @Test
    void theUnknownSettingIsNamedAheadOfTheBothDoorsError() throws Exception {
        // Both doors are set here too, but the unknown entry is the mistake worth naming: the
        // both-doors message would send the operator to delete a valid setting rather than repair the
        // broken one.
        final Tls tls = MAPPER.readValue(
                "{\"enabled\":true,\"tlsCheks\":\"NONE\",\"tlsChecks\":\"STANDARD\",\"tlsChecksFull\":{}}", Tls.class);

        assertThatThrownBy(() -> TlsChecksProjection.project(tls))
                .isInstanceOf(TlsChecksProjection.InvalidTlsChecksConfigException.class)
                .hasMessageContaining("'tlsCheks'");
    }

    @Test
    void anUnknownSettingSurvivesTheWritebackVerbatim() throws Exception {
        // The trap must not leak into the file as a literal 'unknownSettings', and it must not vanish
        // either: if a writeback dropped the entry, the configuration would become valid and the
        // adapter would quietly start under the STANDARD default on the next reload - the exact
        // outcome the trap exists to prevent. What goes in comes back out, and the refusal is sticky.
        final Tls tls = MAPPER.readValue("{\"enabled\":true,\"tlsCheks\":\"ALL\"}", Tls.class);
        final String writtenBack = MAPPER.writeValueAsString(tls);

        assertThat(writtenBack).contains("\"tlsCheks\":\"ALL\"").doesNotContain("unknownSettings");

        final Tls reRead = MAPPER.readValue(writtenBack, Tls.class);
        assertThat(reRead).isEqualTo(tls);
        assertThatThrownBy(() -> TlsChecksProjection.project(reRead))
                .isInstanceOf(TlsChecksProjection.InvalidTlsChecksConfigException.class);
    }

    @Test
    void aTlsWithAnUnknownSettingIsNotEqualToACleanOne() throws Exception {
        // The two must keep comparing unequal. If they did not, an adapter whose configuration gained
        // a misspelled entry would look unchanged to ProtocolAdapterManager's config comparison, which
        // skips an update when the new config equals the running one - and the refusal would never run.
        final Tls withUnknown = MAPPER.readValue("{\"enabled\":true,\"tlsCheks\":\"ALL\"}", Tls.class);
        final Tls clean = MAPPER.readValue("{\"enabled\":true}", Tls.class);

        assertThat(withUnknown).isNotEqualTo(clean);
    }

    @Test
    void theTrapCannotBeForgedIntoAcceptance() throws Exception {
        // An operator writing a literal 'unknownSettings' element does not reach the trap component -
        // the name is itself unknown, so it lands inside the map and is refused like any other
        // unknown entry.
        final Tls tls = MAPPER.readValue("{\"enabled\":true,\"unknownSettings\":{\"x\":1}}", Tls.class);

        assertThat(tls.unknownSettings()).containsKey("unknownSettings");
        assertThatThrownBy(() -> TlsChecksProjection.project(tls))
                .isInstanceOf(TlsChecksProjection.InvalidTlsChecksConfigException.class)
                .hasMessageContaining("'unknownSettings'");
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
        // It must not survive a writeback, and an operator must not be able to forge it. Writing a
        // literal 'collapsedText' does not reach the marker - the name is unknown to the model, so it
        // lands in the unknown-settings trap and is refused as such, never read as a collapse.
        final Tls collapsed = MAPPER.readValue("{\"enabled\":true,\"tlsChecksFull\":\"NONE\"}", Tls.class);
        assertThat(MAPPER.writeValueAsString(collapsed)).doesNotContain("collapsedText");

        final Tls forged = MAPPER.readValue(
                "{\"enabled\":true,\"tlsChecksFull\":{\"collapsedText\":\"NONE\",\"trustMode\":\"CHAIN\"}}", Tls.class);
        assertThat(forged.tlsChecksFull()).isNotNull();
        assertThat(forged.tlsChecksFull().collapsedText()).isNull();
        assertThat(forged.tlsChecksFull().trustMode()).isEqualTo(TrustMode.CHAIN);
        assertThat(forged.tlsChecksFull().unknownSettings()).containsKey("collapsedText");
        assertThatThrownBy(() -> TlsChecksProjection.project(forged))
                .as("a forged marker is refused as an unknown setting, not read as a collapse")
                .isInstanceOf(TlsChecksProjection.InvalidTlsChecksConfigException.class)
                .hasMessageContaining("'collapsedText'")
                .hasMessageNotContaining("could not be read");
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
    void theRetiredTrustLevelFieldIsTrappedAndRefused() throws Exception {
        // trustLevel never shipped, but it existed on a pre-release branch. A configuration carrying
        // it is refused at start-up with the entry named, whichever way the mapper is configured -
        // the trap runs ahead of both FAIL_ON_UNKNOWN_PROPERTIES and any problem handler.
        final Tls tls = MAPPER.readValue("{\"enabled\":true,\"trustLevel\":\"TRUST\"}", Tls.class);

        assertThat(tls.unknownSettings()).containsKey("trustLevel");
        assertThatThrownBy(() -> TlsChecksProjection.project(tls))
                .isInstanceOf(TlsChecksProjection.InvalidTlsChecksConfigException.class)
                .hasMessageContaining("'trustLevel'");
    }

    private static @NotNull ListAppender<ILoggingEvent> attachAppender(final @NotNull Class<?> type) {
        final Logger logger = (Logger) LoggerFactory.getLogger(type);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private static void detachAppender(
            final @NotNull Class<?> type, final @NotNull ListAppender<ILoggingEvent> appender) {
        ((Logger) LoggerFactory.getLogger(type)).detachAppender(appender);
        appender.stop();
    }
}
