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
package com.hivemq.edge.adapters.opcua;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.factories.AdapterFactories;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.edge.adapters.opcua.config.ConnectionOptions;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagKind;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/// The lifecycle lines of EDG-1130: what each one says, and that the last one always states what works now.
///
/// Every line has four parts -- the prefix, the identity, what just happened, the state now -- and a user reads
/// the last one to decide whether value writes and method requests may be sent. So the tests pin the whole
/// line, not fragments of it: a line that names the right event with the wrong state is the failure that
/// matters.
class OpcUaLifecycleLinesTest {

    private static final @NotNull String ID =
            "Lifecycle 'test-adapter-id'; start \\d{4}-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d\\.\\d{3}Z";
    private static final @NotNull String NOTHING_WORKS =
            "value reads: not working, value writes: not working, method requests: not working";

    private final @NotNull FakeEventService eventService = new FakeEventService();
    private final @NotNull ProtocolAdapterStateImpl protocolAdapterState =
            new ProtocolAdapterStateImpl(mock(), "test-adapter-id", "opcua");
    private final @NotNull ModuleServices moduleServices = mock(ModuleServices.class);
    private @Nullable OpcUaProtocolAdapter adapter;

    @BeforeEach
    void setUp() {
        when(moduleServices.eventService()).thenReturn(eventService);
        when(moduleServices.protocolAdapterTagStreamingService())
                .thenReturn(mock(ProtocolAdapterTagStreamingService.class));
    }

    @AfterEach
    void tearDown() {
        if (adapter != null) {
            adapter.destroy();
        }
    }

    @Test
    void nothingIsWrittenBeforeTheFirstStart() {
        final OpcUaProtocolAdapter adapter = newAdapter();
        final OpcUaClientConnection connection = connection(true);
        assertThat(adapter.claimConnection(connection)).isTrue();

        adapter.publishReadyFrom(connection);

        assertThat(lifecycleLines()).isEmpty();
    }

    @Test
    void anAdapterWithoutMethodTags_isFullyWorkingOnceConnected() {
        final OpcUaProtocolAdapter adapter = startedAdapter();
        final OpcUaClientConnection connection = connection(true);
        assertThat(adapter.claimConnection(connection)).isTrue();

        adapter.publishReadyFrom(connection);

        assertThat(lifecycleLines())
                .singleElement()
                .asString()
                .matches(ID + ", connection 1; connected, browse is ready; state now - value reads: working, "
                        + "value writes: working, method requests: none");
    }

    @Test
    void anAdapterWithAMethodTag_isWorkingWithDelaysForMethodRequestsOnceConnected() {
        // The method read runs after CONNECTED, and until it has finished every method request reads its tag's
        // methods itself. "connected" must therefore not claim method requests are working.
        final OpcUaProtocolAdapter adapter = startedAdapter(methodTag("boiler"));
        final OpcUaClientConnection connection = connection(true);
        assertThat(adapter.claimConnection(connection)).isTrue();

        adapter.publishReadyFrom(connection);

        assertThat(lifecycleLines())
                .singleElement()
                .asString()
                .matches(ID + ", connection 1; connected, browse is ready; state now - value reads: working, "
                        + "value writes: working, method requests: starting \\(working with delays\\)");
    }

    @Test
    void aConnectionWithoutBrowseMetadata_isReportedAsAProblem() {
        final OpcUaProtocolAdapter adapter = startedAdapter();
        final OpcUaClientConnection connection = connection(false);
        assertThat(adapter.claimConnection(connection)).isTrue();

        adapter.publishReadyFrom(connection);

        final List<Event> events = lifecycleEvents();
        assertThat(events).singleElement().extracting(Event::getSeverity).isEqualTo(Event.SEVERITY.WARN);
        assertThat(events.getFirst().getMessage())
                .matches(ID + ", connection 1; PROBLEM: connected, but browse is not ready\\. .* Retrying by itself; "
                        + "state now - value reads: working, value writes: working, method requests: none");
    }

    @Test
    void aLostSession_saysNothingWorks_andAResumedOneCountsAsTheNextSession() {
        final OpcUaProtocolAdapter adapter = startedAdapter(methodTag("boiler"));
        final OpcUaClientConnection connection = connection(true);
        assertThat(adapter.claimConnection(connection)).isTrue();
        adapter.publishReadyFrom(connection);

        adapter.publishStatusFrom(connection, ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
        adapter.onSessionReplaced();

        final List<String> lines = lifecycleLines();
        assertThat(lines).hasSize(3);
        assertThat(lines.get(1))
                .matches(ID + ", connection 1; PROBLEM: session to the device lost\\. Waiting for it to resume; "
                        + "state now - " + NOTHING_WORKS);
        assertThat(lines.get(2))
                .matches(ID + ", connection 1, session 2; session resumed; state now - value reads: working, "
                        + "value writes: working, method requests: starting \\(working with delays\\)");
    }

    @Test
    void aDisconnectFromAConnectionTheAdapterHasReleased_writesNothing() {
        // A reconnect and a stop release the connection before closing it, and closing it publishes
        // DISCONNECTED. That is not a lost session, and must not be reported as one.
        final OpcUaProtocolAdapter adapter = startedAdapter();
        final OpcUaClientConnection connection = connection(true);
        assertThat(adapter.claimConnection(connection)).isTrue();
        adapter.publishReadyFrom(connection);
        assertThat(adapter.releaseCurrentConnection()).isSameAs(connection);

        adapter.publishStatusFrom(connection, ProtocolAdapterState.ConnectionStatus.DISCONNECTED);

        assertThat(lifecycleLines()).hasSize(1);
    }

    @Test
    void aSessionMiloActivatesDuringTheInitialConnect_isNotAResumedSession() {
        // Milo can report an activation while the connection is still being set up. That connect writes its
        // own "connected" line; a "session resumed" before it would describe a session nobody had lost.
        final OpcUaProtocolAdapter adapter = startedAdapter();
        final OpcUaClientConnection connection = connection(true);
        assertThat(adapter.claimConnection(connection)).isTrue();

        adapter.onSessionReplaced();

        assertThat(lifecycleLines()).isEmpty();
    }

    @Test
    void aFailedConnectAttempt_saysTheAdapterRetriesByItself() {
        final OpcUaProtocolAdapter adapter = startedAdapter(methodTag("boiler"));
        final OpcUaClientConnection connection = connection(true);
        assertThat(adapter.claimConnection(connection)).isTrue();

        assertThat(adapter.completeFailedAttempt(connection, startInput(), null))
                .isTrue();

        assertThat(lifecycleLines())
                .singleElement()
                .asString()
                .matches(ID + ", connection 1; PROBLEM: could not connect \\(attempt 1\\), see the preceding event\\. "
                        + "Retrying in \\d+ ms by itself\\. A restart does not help; state now - " + NOTHING_WORKS);
    }

    @Test
    void aSecondConnection_isNumberedInTheIdentity() {
        final OpcUaProtocolAdapter adapter = startedAdapter();
        final OpcUaClientConnection first = connection(true);
        final OpcUaClientConnection second = connection(true);
        assertThat(adapter.claimConnection(first)).isTrue();
        assertThat(adapter.releaseCurrentConnection()).isSameAs(first);
        assertThat(adapter.claimConnection(second)).isTrue();

        adapter.publishReadyFrom(second);

        assertThat(lifecycleLines()).singleElement().asString().matches(ID + ", connection 2; connected, .*");
    }

    @Test
    void twoStarts_carryDifferentStartTimes() throws Exception {
        final OpcUaProtocolAdapter first = startedAdapter();
        final String firstStart = startOf(connectAndReadLastLine(first));
        Thread.sleep(5);
        final OpcUaProtocolAdapter second = startedAdapter();
        final String secondStart = startOf(connectAndReadLastLine(second));
        first.destroy();

        assertThat(secondStart).isNotEqualTo(firstStart);
    }

    private @NotNull String connectAndReadLastLine(final @NotNull OpcUaProtocolAdapter adapter) {
        final OpcUaClientConnection connection = connection(true);
        assertThat(adapter.claimConnection(connection)).isTrue();
        adapter.publishReadyFrom(connection);
        final List<String> lines = lifecycleLines();
        return lines.getLast();
    }

    private static @NotNull String startOf(final @NotNull String line) {
        final String afterStart = line.substring(line.indexOf("; start ") + "; start ".length());
        return afterStart.substring(0, afterStart.indexOf(','));
    }

    private @NotNull List<Event> lifecycleEvents() {
        return eventService.readEvents(null, null).stream()
                .filter(event -> event.getMessage().startsWith(OpcUaProtocolAdapter.LIFECYCLE_PREFIX))
                .toList();
    }

    private @NotNull List<String> lifecycleLines() {
        return lifecycleEvents().stream().map(Event::getMessage).toList();
    }

    private static @NotNull OpcUaClientConnection connection(final boolean hasBrowseMetadata) {
        final OpcUaClientConnection connection = mock(OpcUaClientConnection.class);
        when(connection.hasBrowseMetadata()).thenReturn(hasBrowseMetadata);
        return connection;
    }

    private static @NotNull OpcuaTag methodTag(final @NotNull String name) {
        return new OpcuaTag(name, "", new OpcuaTagDefinition("ns=1;s=" + name, OpcuaTagKind.METHOD));
    }

    private @NotNull OpcUaProtocolAdapter startedAdapter(final @NotNull OpcuaTag... tags) {
        final OpcUaProtocolAdapter started = newAdapter(tags);
        started.beginLifecycle();
        return started;
    }

    private @NotNull OpcUaProtocolAdapter newAdapter(final @NotNull OpcuaTag... tags) {
        final ProtocolAdapterInformation adapterInformation = mock(ProtocolAdapterInformation.class);
        when(adapterInformation.getProtocolId()).thenReturn("opcua");

        @SuppressWarnings("unchecked")
        final ProtocolAdapterInput<OpcUaSpecificAdapterConfig> input = mock(ProtocolAdapterInput.class);
        when(input.getAdapterId()).thenReturn("test-adapter-id");
        when(input.getProtocolAdapterState()).thenReturn(protocolAdapterState);
        when(input.getConfig())
                .thenReturn(new OpcUaSpecificAdapterConfig(
                        "opc.tcp://127.0.0.1:4840",
                        false,
                        null,
                        null,
                        null,
                        OpcUaToMqttConfig.defaultOpcUaToMqttConfig(),
                        null,
                        ConnectionOptions.defaultConnectionOptions()));
        when(input.getTags()).thenReturn(new ArrayList<Tag>(List.of(tags)));
        when(input.adapterFactories()).thenReturn(mock(AdapterFactories.class));
        when(input.getProtocolAdapterMetricsHelper()).thenReturn(mock(ProtocolAdapterMetricsService.class));
        when(input.moduleServices()).thenReturn(moduleServices);
        adapter = new OpcUaProtocolAdapter(adapterInformation, input);
        return adapter;
    }

    private @NotNull ProtocolAdapterStartInput startInput() {
        final ProtocolAdapterStartInput input = mock(ProtocolAdapterStartInput.class);
        when(input.moduleServices()).thenReturn(moduleServices);
        return input;
    }
}
