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

import ch.qos.logback.classic.Level;
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
 * as objects. Deserializing those shapes used to throw, and because adapter configurations are
 * converted in a single pass over every adapter, that exception stopped <em>every</em> adapter in the
 * deployment from being reconfigured — not just the one with the empty element.
 *
 * <p>{@link TlsChecksParsingTest} pins the same behaviour one layer down, on the JSON the collapse
 * produces. Both are needed: a Jackson-layer test alone certifies a behaviour the product does not
 * have.
 */
class TlsChecksConfigFileParsingTest {

    private static final @NotNull String COLLAPSED = "/opcua-adapter-tls-collapsed-elements-config.xml";

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
        // The blast radius, reproduced. ProtocolAdapterManager.refresh maps every adapter entity in one
        // stream, and a single throw there aborts the whole refresh before any adapter is created,
        // updated or deleted - with one stack trace and nothing in the event stream. Four adapters in
        // this file carry a collapsed element; the fifth is healthy and must still arrive.
        assertThatCode(() -> loadAdapters(COLLAPSED)).doesNotThrowAnyException();

        assertThat(loadAdapters(COLLAPSED))
                .extracting(ProtocolAdapterConfig::getAdapterId)
                .containsExactly(
                        "collapsed-empty-axes",
                        "collapsed-partial-axes",
                        "collapsed-empty-allow-list",
                        "collapsed-empty-allow-list-path",
                        "empty-later-axis",
                        "healthy-neighbour");
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
    void anEmptyFirstAxisCollapsesTheWholeElementAndFallsBackToMaximumValidation() throws Exception {
        // `<trustMode></trustMode><revocation>NONE</revocation>` arrives as the String "NONE" - the
        // concatenated text of the axes that are left. There is no way to recover which axis that text
        // belonged to, so every axis is treated as unset. The operator's revocation=NONE is dropped,
        // which is the safe direction: unset means the strictest value, so the fallback can only ever
        // check more than was asked for, never less.
        final Tls tls = tlsOf("collapsed-partial-axes");

        assertThat(tls.tlsChecksFull()).isEqualTo(TlsChecksFull.allAxesUnset());
        assertThat(TlsChecksProjection.project(tls).revocation())
                .as("the relaxation the operator wrote is not honoured")
                .isEqualTo(RevocationCheck.REQUIRE_CRLS);
        assertThat(TlsChecksProjection.project(tls)).isEqualTo(MAXIMUM_VALIDATION);
    }

    @Test
    void aCollapsedTlsChecksFullIsReportedToTheOperator() {
        // Dropping the axes silently would leave the operator with a connection that fails for no
        // visible reason. The WARN quotes the text back and names the shape that caused it.
        final List<ILoggingEvent> events = whileCapturing(TlsChecksFull.class, () -> loadAdapters(COLLAPSED));

        assertThat(events).anySatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getFormattedMessage())
                    .contains("tlsChecksFull")
                    .contains("NONE")
                    .contains("strictest");
        });
    }

    @Test
    void anEmptyTlsChecksFullElementIsNotWorthAWarning() {
        // `<tlsChecksFull/>` is a documented, correct thing to write. Warning about it would train
        // operators to ignore the warning that matters.
        final List<ILoggingEvent> events =
                whileCapturing(TlsChecksFull.class, () -> loadAdapters("/opcua-adapter-full-config.xml"));

        assertThat(events).isEmpty();
    }

    @Test
    void anEmptyAllowListElementBecomesAnActionableStartUpErrorRatherThanARefreshFailure() {
        // `<allowList/>` under a trust mode that needs one is a genuine misconfiguration, and it must
        // be reported as this adapter's problem at start-up - naming the element to add - instead of
        // aborting the conversion of every adapter in the file.
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

        // Deliberately the same shape as ProtocolAdapterManager.refresh: one stream over every adapter,
        // so a throw on any of them is a throw on all of them.
        final ProtocolAdapterConfigConverter converter = createConverter();
        return configEntity.getProtocolAdapterConfig().stream()
                .map((ProtocolAdapterEntity entity) -> converter.fromEntity(entity))
                .toList();
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
