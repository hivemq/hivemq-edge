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

import static com.hivemq.edge.adapters.opcua.Constants.PROTOCOL_ID_OPCUA;
import static com.hivemq.protocols.ProtocolAdapterUtils.createProtocolAdapterMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.edge.adapters.opcua.OpcUaProtocolAdapterFactory;
import com.hivemq.protocols.ProtocolAdapterConfig;
import com.hivemq.protocols.ProtocolAdapterConfigConverter;
import com.hivemq.protocols.ProtocolAdapterFactoryManager;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Certificate-validation settings read at the layer an operator actually writes at: a real
 * {@code config.xml}, through {@link ConfigFileReaderWriter} and the adapter config converter.
 *
 * <p>This layer is not the Jackson layer, and the difference is load-bearing. Edge's XML-to-map
 * conversion collapses a nested element to its text content whenever the element's first child element
 * is itself empty, so {@code <tlsChecksFull/>} and {@code <allowList/>} reach Jackson as Strings, not
 * as objects. Deserializing those shapes used to throw — the historical motivation for the lenient
 * creators: at the time adapter configurations were converted in a single pass over every adapter, so
 * that one exception stopped <em>every</em> adapter in the deployment from being reconfigured.
 * Conversion is per-adapter today, but the creators remain so the refusal stays on the affected
 * adapter with an actionable message rather than a raw coercion error.
 *
 * <p>{@link TlsChecksParsingTest} pins the same behaviour one layer down, on the JSON the collapse
 * produces. Both are needed: a Jackson-layer test alone certifies a behaviour the product does not
 * have.
 */
class TlsChecksConfigFileParsingTest {

    private static final @NotNull String COLLAPSED = "/opcua-adapter-tls-collapsed-elements-config.xml";
    private static final @NotNull String INVALID = "/opcua-adapter-tls-invalid-values-config.xml";

    /** Every axis at its strictest value — what an unset axis resolves to. */
    private static final @NotNull EffectiveChecks MAXIMUM_VALIDATION = new EffectiveChecks(
            TrustMode.CHAIN,
            SanUriCheck.APPLICATION_URI,
            HostnameCheck.HOSTNAME,
            ValidityCheck.NOT_BEFORE_OR_AFTER,
            RevocationCheck.REQUIRE_CRLS,
            KeyUsageCheck.SERVER_AUTH);

    private final @NotNull ObjectMapper mapper = createProtocolAdapterMapper(new ObjectMapper());

    @Test
    void everyAdapterInTheFileConverts_soOneEmptyElementCannotStopTheRest() {
        // The historical blast radius, reproduced: `loadAdapters` deliberately keeps the old
        // single-stream shape of ProtocolAdapterManager.refresh, where one throw aborted the whole
        // refresh before any adapter was created, updated or deleted. Six adapters in this file carry
        // a collapsed element; the seventh is healthy and must still arrive. The production refresh
        // converts each adapter in isolation now, so this pins the parsing rather than the isolation -
        // that is ProtocolAdapterManagerTest's job.
        assertThatCode(() -> loadAdapters(COLLAPSED)).doesNotThrowAnyException();

        assertThat(loadAdapters(COLLAPSED))
                .extracting(ProtocolAdapterConfig::getAdapterId)
                .containsExactly(
                        "collapsed-empty-axes",
                        "collapsed-partial-axes",
                        "collapsed-empty-allow-list",
                        "collapsed-empty-allow-list-path",
                        "empty-later-axis",
                        "empty-tls",
                        "healthy-neighbour");
    }

    @Test
    void aCollapsedTlsElementIsRejectedAtConversionQuotingTheText() {
        // The same collapse one element up: `<tls><enabled></enabled><tlsChecks>ALL</tlsChecks></tls>`
        // arrives as the String "ALL". Deliberately a conversion rejection, not the sentinel treatment
        // tlsChecksFull gets: a sentinel would let the conversion succeed and put the unreadable
        // configuration on the GET/PUT surface, where it serializes as a clean enabled=false. The
        // rejection is contained to this adapter, and the message names the mistake instead of a raw
        // Jackson coercion error.
        final ProtocolAdapterConfigConverter converter = createConverter();
        final ProtocolAdapterEntity entity = entitiesOf(INVALID).stream()
                .filter(candidate -> "collapsed-tls".equals(candidate.getAdapterId()))
                .findFirst()
                .orElseThrow();

        assertThatThrownBy(() -> converter.fromEntity(entity))
                .hasMessageContaining("could not be read")
                .hasMessageContaining("'ALL'")
                .hasMessageContaining("<tls/> is valid");
    }

    @Test
    void anEmptyTlsElementMeansTlsDisabled() throws Exception {
        // `<tls/>` collapses to "" and binds to the default: TLS off - the same outcome the REST path
        // has always produced for an empty value, so file and API agree.
        final Tls tls = tlsOf("empty-tls");

        assertThat(tls).isEqualTo(Tls.defaultTls());
        assertThat(tls.enabled()).isFalse();
        assertThat(TlsChecksProjection.project(tls)).isEqualTo(TlsChecksProjection.fromPreset(TlsChecks.STANDARD));
    }

    @Test
    void aTrappedUnknownSettingSurvivesTheProductionWritebackVerbatim() {
        // The load-bearing writeback claim, pinned on the mapper the product actually uses:
        // unconvertConfigObject with the protocol-adapter mapper, not a plain ObjectMapper. The
        // trapped entry must come back under its own name - never a literal 'unknownSettings', and
        // never dropped, because a dropped entry would make the next reload quietly start the adapter
        // under the STANDARD default.
        final Map<String, Object> writtenBack = writtenBackTlsOf(INVALID, "misspelled-setting-name");

        assertThat(writtenBack).containsEntry("tlsCheks", "ALL").doesNotContainKey("unknownSettings");
    }

    @Test
    void anEmptyAxisAfterTheFirstDoesNotCollapseTheElement() throws Exception {
        // The boundary of the collapse, and the reason it is worth stating precisely: it is triggered
        // by the first child element being empty, not by any empty child. `<revocation></revocation>`
        // in second position leaves an object, so the axes around it are read normally and only the
        // blank one falls back to unset.
        final Tls tls = tlsOf("empty-later-axis");

        assertThat(tls.tlsChecksFull()).isNotNull();
        assertThat(tls.tlsChecksFull())
                .as("nothing collapsed, so this is not the all-unset fallback")
                .isNotEqualTo(TlsChecksFull.allAxesUnset());
        assertThat(tls.tlsChecksFull().trustMode()).isEqualTo(TrustMode.CHAIN);
        assertThat(tls.tlsChecksFull().hostname()).isEqualTo(HostnameCheck.NONE);
        assertThat(tls.tlsChecksFull().revocation())
                .as("a blank value is unset, which resolves to the strictest setting")
                .isNull();

        assertThat(TlsChecksProjection.project(tls).revocation()).isEqualTo(RevocationCheck.REQUIRE_CRLS);
        assertThat(TlsChecksProjection.project(tls).hostname()).isEqualTo(HostnameCheck.NONE);
    }

    @Test
    void anEmptyTlsChecksFullElementMeansMaximumValidation() throws Exception {
        // The form the documentation and TlsChecksFull's javadoc both advertise. Before the String
        // creator existed this threw "Cannot coerce empty String" and the documented behaviour did not
        // exist at all from a configuration file.
        final Tls tls = tlsOf("collapsed-empty-axes");

        assertThat(tls.tlsChecksFull()).isNotNull();
        assertThat(tls.tlsChecksFull()).isEqualTo(TlsChecksFull.allAxesUnset());
        assertThat(tls.tlsChecks()).as("the other door stays untouched").isNull();
        assertThat(TlsChecksProjection.project(tls)).isEqualTo(MAXIMUM_VALIDATION);
    }

    @Test
    void anEmptyFirstAxisCollapsesTheWholeElementAndTheAdapterIsRefused() throws Exception {
        // `<trustMode></trustMode><revocation>NONE</revocation>` arrives as the String "NONE" - the
        // concatenated text of the axes that are left. Which axis that text belonged to cannot be
        // recovered, so the operator has configured certificate validation that Edge cannot read.
        // Silently resolving it to maximum validation would check more than they asked for, which is
        // safe, but it would also discard a security setting they deliberately wrote. The adapter is
        // refused at start-up instead, with the same shape as every other TLS misconfiguration.
        final Tls tls = tlsOf("collapsed-partial-axes");

        assertThat(tls.tlsChecksFull()).isNotNull();
        assertThat(tls.tlsChecksFull().collapsedText()).isEqualTo("NONE");
        assertThatThrownBy(() -> TlsChecksProjection.project(tls))
                .isInstanceOf(TlsChecksProjection.InvalidTlsChecksConfigException.class)
                .hasMessageContaining("could not be read")
                .hasMessageContaining("'NONE'");
    }

    @Test
    void aCollapsedTlsChecksFullStillConvertsCleanly() {
        // The refusal is deliberately a start-up failure and not a conversion failure, and the reason
        // is no longer blast radius: conversion rejection is isolated to this adapter, logged, and
        // raised as an adapter-scoped CRITICAL event, so it is neither silent nor contagious. What a
        // start-up refusal buys instead is that the adapter is converted - so it is visible in an
        // error state on the API surface, and the TLS fields the operator actually wrote survive
        // writeback rather than being dropped on the next save. The sentinel is worth its complexity
        // only where that is preferable; nowhere else.
        assertThatCode(() -> loadAdapters(COLLAPSED)).doesNotThrowAnyException();
        assertThat(whileCapturing(TlsChecksFull.class, () -> loadAdapters(COLLAPSED)))
                .as("nothing is reported at parse time; the start-up failure is the single signal")
                .isEmpty();
    }

    @Test
    void anEmptyAllowListElementBecomesAnActionableStartUpErrorRatherThanARefreshFailure() {
        // `<allowList/>` under a trust mode that needs one is a genuine misconfiguration, and it is
        // reported as this adapter's problem at start-up, naming the element to add. Refusing it at
        // conversion instead would also be contained today, but it would cost the visible error state
        // and the writeback of what the operator wrote; historically it would additionally have
        // aborted the conversion of every adapter in the file.
        assertThatThrownBy(() -> TlsChecksProjection.project(tlsOf("collapsed-empty-allow-list")))
                .isInstanceOf(TlsChecksProjection.InvalidTlsChecksConfigException.class)
                .hasMessageContaining("ALLOW_LIST")
                .hasMessageContaining("<allowList><path>");
    }

    @Test
    void anAllowListWithAnEmptyPathElementCollapsesTheSameWay() {
        // `<allowList><path></path></allowList>`: the nested element is present but empty, which is the
        // same collapse. Same outcome, reached by a different route.
        final Tls tls = tlsOf("collapsed-empty-allow-list-path");

        assertThat(tls.allowList()).isNotNull();
        assertThat(tls.allowList().path()).isNull();
        assertThatThrownBy(() -> TlsChecksProjection.project(tls))
                .isInstanceOf(TlsChecksProjection.InvalidTlsChecksConfigException.class);
    }

    @Test
    void aMisspelledPresetValueFailsOnlyThatAdaptersConversion() {
        // <tlsChecks>ALLL</tlsChecks> is a value the model cannot read, and "unset" is not a safe
        // fallback for the preset door - STANDARD checks less than the ALL the operator meant. The
        // conversion of that adapter fails, naming the value; every other adapter in the file,
        // including one with a trapped setting NAME, still converts. This is the isolation the
        // per-adapter conversion in ProtocolAdapterManager provides.
        final ProtocolAdapterConfigConverter converter = createConverter();
        final Set<String> rejectedAtConversion = Set.of(
                "misspelled-preset-value",
                "collapsed-tls",
                "misspelled-tls-name",
                "misspelled-policy-name",
                "misspelled-message-security-mode");

        for (final ProtocolAdapterEntity entity : entitiesOf(INVALID)) {
            if ("misspelled-preset-value".equals(entity.getAdapterId())) {
                assertThatThrownBy(() -> converter.fromEntity(entity))
                        .hasMessageContaining("'ALLL'")
                        .hasMessageContaining("TlsChecks");
            } else if (!rejectedAtConversion.contains(entity.getAdapterId())) {
                assertThatCode(() -> converter.fromEntity(entity))
                        .as("adapter '%s' must convert", entity.getAdapterId())
                        .doesNotThrowAnyException();
            }
        }
    }

    @Test
    void aMisspelledEnclosingTlsNameIsRejectedAtConversionNamingTheEntry() {
        // <tlls> is not a setting of the adapter configuration. The application-wide warn-and-drop
        // handling would discard the whole TLS block and quietly run the adapter without TLS - the
        // enclosing settings are where a dropped entry loosens security the most. The configuration's
        // any-setter rejects the conversion instead, naming the entry; the rejection is contained to
        // this adapter and the healthy neighbour still converts (see the loop test above).
        final ProtocolAdapterConfigConverter converter = createConverter();
        final ProtocolAdapterEntity entity = entitiesOf(INVALID).stream()
                .filter(candidate -> "misspelled-tls-name".equals(candidate.getAdapterId()))
                .findFirst()
                .orElseThrow();

        assertThatThrownBy(() -> converter.fromEntity(entity))
                .hasMessageContaining("'tlls'")
                .hasMessageContaining("Known settings")
                .hasMessageContaining("tls, security");
    }

    @Test
    void aMisspelledSecurityChildIsRejectedAtConversionNamingTheEntry() {
        // <polciy> is not a child of <security>. The Security deserializer reads a raw map, so the
        // application-wide unknown-setting handling never sees the entry - without the rejection it
        // would silently become policy NONE instead of the BASIC256SHA256 the operator wrote.
        final ProtocolAdapterConfigConverter converter = createConverter();
        final ProtocolAdapterEntity entity = entitiesOf(INVALID).stream()
                .filter(candidate -> "misspelled-policy-name".equals(candidate.getAdapterId()))
                .findFirst()
                .orElseThrow();

        assertThatThrownBy(() -> converter.fromEntity(entity))
                .hasMessageContaining("'polciy'")
                .hasMessageContaining("policy, messageSecurityMode");
    }

    @Test
    void aMisspelledSettingNameIsTrappedAndRefusedAtStartUp() {
        // <tlsCheks>ALL</tlsCheks>: with neither door configured, the application-wide
        // warn-and-drop handling for unknown settings would quietly run this adapter under the
        // STANDARD default. The entry is trapped through conversion instead and refuses the adapter
        // at start-up, naming it.
        final Tls tls = tlsOf(INVALID, "misspelled-setting-name");

        assertThat(tls.unknownSettings()).containsKey("tlsCheks");
        assertThatThrownBy(() -> TlsChecksProjection.project(tls))
                .isInstanceOf(TlsChecksProjection.InvalidTlsChecksConfigException.class)
                .hasMessageContaining("'tlsCheks'")
                .hasMessageContaining("tlsChecks");
    }

    @Test
    void aMisspelledAxisNameIsTrappedAndRefusedAtStartUp() {
        // <trustmode> is not <trustMode>. Without the trap the axis would count as omitted and
        // resolve to its strictest default - safe in the checking direction, but it discards a
        // setting the operator explicitly wrote.
        final Tls tls = tlsOf(INVALID, "misspelled-axis-name");

        assertThat(tls.tlsChecksFull()).isNotNull();
        assertThat(tls.tlsChecksFull().unknownSettings()).containsKey("trustmode");
        assertThatThrownBy(() -> TlsChecksProjection.project(tls))
                .isInstanceOf(TlsChecksProjection.InvalidTlsChecksConfigException.class)
                .hasMessageContaining("'trustmode'")
                .hasMessageContaining("trustMode");
    }

    @Test
    void aMisspelledMessageSecurityModeIsRejectedAtConversionNamingTheValue() {
        // <messageSecurityMode>SING</messageSecurityMode> under <policy>NONE</policy>. Read as
        // "unset" - which is what the mode used to fall back to - the policy picks
        // MessageSecurityMode.None and the adapter connects with neither signing nor encryption. The
        // correct spelling SIGN pairs with no endpoint under policy NONE and refuses to connect, so
        // defaulting here converted a safe failure into an insecure success. Rejected at conversion,
        // contained to this adapter.
        final ProtocolAdapterConfigConverter converter = createConverter();
        final ProtocolAdapterEntity entity = entitiesOf(INVALID).stream()
                .filter(candidate -> "misspelled-message-security-mode".equals(candidate.getAdapterId()))
                .findFirst()
                .orElseThrow();

        assertThatThrownBy(() -> converter.fromEntity(entity))
                .hasMessageContaining("'SING'")
                .hasMessageContaining("MsgSecurityMode")
                .hasMessageContaining("SIGN_AND_ENCRYPT");
    }

    @Test
    void anAdapterAfterTheMisspelledOnesKeepsItsOwnConfiguration() {
        final Tls tls = tlsOf(INVALID, "healthy-neighbour");

        assertThat(tls.tlsChecks()).isEqualTo(TlsChecks.ALL);
        assertThatCode(() -> assertThat(TlsChecksProjection.project(tls))
                        .isEqualTo(TlsChecksProjection.fromPreset(TlsChecks.ALL)))
                .doesNotThrowAnyException();
    }

    @Test
    void anAdapterAfterTheCollapsedOnesKeepsItsOwnConfiguration() {
        // Not merely present: unaltered. The point of the fix is that the neighbour is untouched, not
        // that it survives in some degraded form.
        final Tls tls = tlsOf("healthy-neighbour");

        assertThat(tls.tlsChecks()).isEqualTo(TlsChecks.ALL);
        assertThat(tls.tlsChecksFull()).isNull();
        assertThatCode(() -> assertThat(TlsChecksProjection.project(tls))
                        .isEqualTo(TlsChecksProjection.fromPreset(TlsChecks.ALL)))
                .doesNotThrowAnyException();
    }

    @Test
    void aCollapsedElementIsWrittenBackAsTheEmptyElementTheOperatorWrote() {
        // The no-op-writeback guarantee has to survive the fix. An all-axes-unset TlsChecksFull
        // serializes to an empty object, which the map-to-XML adapter writes as `<tlsChecksFull/>` -
        // exactly what was read. Filling the axes in here would silently convert "strictest, resolved
        // at read time" into "whatever today's defaults happen to be".
        assertThat(writtenBackTlsOf("collapsed-empty-axes"))
                .containsEntry("tlsChecksFull", Map.of())
                .doesNotContainKey("tlsChecks");

        assertThat(writtenBackTlsOf("collapsed-empty-allow-list"))
                .containsEntry("allowList", Map.of())
                .containsEntry("tlsChecks", "SELF_SIGNED")
                .doesNotContainKey("tlsChecksFull");
    }

    // -- harness ---------------------------------------------------------------------------------

    private @NotNull Tls tlsOf(final @NotNull String adapterId) {
        return adapterConfigOf(adapterId).getTls();
    }

    /**
     * Reads one adapter's TLS configuration from the given fixture, converting only that adapter — so
     * a fixture may also contain adapters whose conversion fails by design.
     */
    private @NotNull Tls tlsOf(final @NotNull String resourceName, final @NotNull String adapterId) {
        return convertedConfigOf(resourceName, adapterId).getTls();
    }

    private @NotNull OpcUaSpecificAdapterConfig convertedConfigOf(
            final @NotNull String resourceName, final @NotNull String adapterId) {
        final ProtocolAdapterEntity entity = entitiesOf(resourceName).stream()
                .filter(candidate -> adapterId.equals(candidate.getAdapterId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no adapter '" + adapterId + "' in " + resourceName));
        return (OpcUaSpecificAdapterConfig) createConverter().fromEntity(entity).getAdapterConfig();
    }

    /** The production writeback of one adapter from the given fixture, through the real mapper. */
    @SuppressWarnings("unchecked")
    private @NotNull Map<String, Object> writtenBackTlsOf(
            final @NotNull String resourceName, final @NotNull String adapterId) {
        final OpcUaProtocolAdapterFactory factory =
                new OpcUaProtocolAdapterFactory(mock(ProtocolAdapterFactoryInput.class));
        final Map<String, Object> writtenBack =
                factory.unconvertConfigObject(mapper, convertedConfigOf(resourceName, adapterId));
        return (Map<String, Object>) writtenBack.get("tls");
    }

    private @NotNull OpcUaSpecificAdapterConfig adapterConfigOf(final @NotNull String adapterId) {
        return (OpcUaSpecificAdapterConfig) loadAdapters(COLLAPSED).stream()
                .filter(config -> adapterId.equals(config.getAdapterId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no adapter '" + adapterId + "' in " + COLLAPSED))
                .getAdapterConfig();
    }

    @SuppressWarnings("unchecked")
    private @NotNull Map<String, Object> writtenBackTlsOf(final @NotNull String adapterId) {
        final OpcUaProtocolAdapterFactory factory =
                new OpcUaProtocolAdapterFactory(mock(ProtocolAdapterFactoryInput.class));
        final Map<String, Object> writtenBack = factory.unconvertConfigObject(mapper, adapterConfigOf(adapterId));
        return (Map<String, Object>) writtenBack.get("tls");
    }

    private @NotNull List<ProtocolAdapterConfig> loadAdapters(final @NotNull String resourceName) {
        // Deliberately the same shape as the historical ProtocolAdapterManager.refresh: one stream
        // over every adapter, so a throw on any of them is a throw on all of them.
        final ProtocolAdapterConfigConverter converter = createConverter();
        return entitiesOf(resourceName).stream()
                .map((ProtocolAdapterEntity entity) -> converter.fromEntity(entity))
                .toList();
    }

    private @NotNull List<ProtocolAdapterEntity> entitiesOf(final @NotNull String resourceName) {
        final URL resource = getClass().getResource(resourceName);
        assertThat(resource).as("fixture %s", resourceName).isNotNull();

        final File file;
        try {
            file = Path.of(resource.toURI()).toFile();
        } catch (final URISyntaxException e) {
            throw new AssertionError(e);
        }

        final HiveMQConfigEntity configEntity = new ConfigFileReaderWriter(
                        mock(SystemInformation.class), new ConfigurationFile(file), List.of())
                .applyConfig();
        return configEntity.getProtocolAdapterConfig();
    }

    private @NotNull ProtocolAdapterConfigConverter createConverter() {
        final ProtocolAdapterFactoryInput input = mock(ProtocolAdapterFactoryInput.class);
        when(input.isWritingEnabled()).thenReturn(true);

        // Built before the stubbing below: the factory constructor calls into the mock, and Mockito
        // reads a nested mock interaction inside when(...) as an unfinished stubbing.
        final OpcUaProtocolAdapterFactory factory = new OpcUaProtocolAdapterFactory(input);

        final ProtocolAdapterFactoryManager manager = mock(ProtocolAdapterFactoryManager.class);
        when(manager.get(PROTOCOL_ID_OPCUA)).thenReturn(Optional.of(factory));
        return new ProtocolAdapterConfigConverter(manager, mapper);
    }

    /** Captures everything the given class logs while the body runs. */
    private static @NotNull List<ILoggingEvent> whileCapturing(
            final @NotNull Class<?> type, final @NotNull Runnable body) {
        final Logger logger = (Logger) LoggerFactory.getLogger(type);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            body.run();
            return List.copyOf(appender.list);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
