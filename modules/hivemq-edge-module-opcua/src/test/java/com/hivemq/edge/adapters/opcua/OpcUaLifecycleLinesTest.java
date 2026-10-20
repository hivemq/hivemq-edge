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

import com.hivemq.adapter.sdk.api.ProtocolAdapterConnectionDirection;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.factories.AdapterFactories;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

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

    private final @NotNull PausingEventService eventService = new PausingEventService();
    private @NotNull ConnectionOptions connectionOptions = ConnectionOptions.defaultConnectionOptions();
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
        adapter.publishReadyFrom(first);
        assertThat(adapter.releaseCurrentConnection()).isSameAs(first);
        assertThat(adapter.claimConnection(second)).isTrue();

        adapter.publishReadyFrom(second);

        assertThat(lifecycleLines().getLast()).matches(ID + ", connection 2; connected, .*");
    }

    @Test
    void retriesOfAConnectionThatNeverConnected_keepItsNumber() {
        // Connections are numbered, attempts are not: three failed attempts and a success are all "connection 1",
        // and the retry lines tell the attempts apart.
        final OpcUaProtocolAdapter adapter = startedAdapter();
        for (int attempt = 0; attempt < 3; attempt++) {
            final OpcUaClientConnection failed = connection(true);
            assertThat(adapter.claimConnection(failed)).isTrue();
            assertThat(adapter.completeFailedAttempt(failed, startInput(), null))
                    .isTrue();
        }
        final OpcUaClientConnection succeeding = connection(true);
        assertThat(adapter.claimConnection(succeeding)).isTrue();

        adapter.publishReadyFrom(succeeding);

        final List<String> lines = lifecycleLines();
        assertThat(lines).hasSize(4);
        assertThat(lines.get(2)).matches(ID + ", connection 1; PROBLEM: could not connect \\(attempt 3\\), .*");
        assertThat(lines.getLast()).matches(ID + ", connection 1; connected, .*");
    }

    @Test
    @Timeout(10)
    void aConnectFinishingWhileTheAdapterStops_cannotLeaveAWorkingLineLast() throws Exception {
        // Reported by Miguel on #1772: the stopping line used to be written before the adapter was marked stopped,
        // so a connect finishing in that gap wrote "connected ... working" after it. Paused exactly there.
        final OpcUaProtocolAdapter adapter = startedAdapter();
        final OpcUaClientConnection connection = connection(true);
        assertThat(adapter.claimConnection(connection)).isTrue();
        eventService.pauseAfter("; stopping;");

        final CompletableFuture<Void> stop = CompletableFuture.runAsync(() -> adapter.stop(
                ProtocolAdapterConnectionDirection.Northbound,
                mock(ProtocolAdapterStopInput.class),
                mock(ProtocolAdapterStopOutput.class)));
        assertThat(eventService.awaitPaused()).isTrue();
        final CompletableFuture<Void> connect = CompletableFuture.runAsync(() -> adapter.publishReadyFrom(connection));
        Thread.sleep(200);
        eventService.resume();
        stop.get(5, TimeUnit.SECONDS);
        connect.get(5, TimeUnit.SECONDS);

        assertThat(lifecycleLines().getLast()).matches(ID + "; stopping; state now - .*");
    }

    @Test
    void withAutomaticReconnectionOff_aRecoveredConnectionReplacesTheRestartAdvice() {
        // Reported by Miguel on #1772: the health check said "a restart is needed" and a connection that came
        // back by itself left that line last. The adapter status deliberately stays ERROR; only the line changes.
        connectionOptions = withoutAutoReconnect();
        final OpcUaProtocolAdapter adapter = startedAdapter();
        final OpcUaClientConnection connection = connection(true);
        assertThat(adapter.claimConnection(connection)).isTrue();
        adapter.publishReadyFrom(connection);

        when(connection.isHealthy()).thenReturn(false);
        adapter.runHealthCheck();
        adapter.runHealthCheck();
        when(connection.isHealthy()).thenReturn(true);
        adapter.runHealthCheck();
        adapter.runHealthCheck();

        final List<String> lines = lifecycleLines();
        assertThat(lines).hasSize(3);
        assertThat(lines.get(1))
                .matches(ID + ", connection 1; PROBLEM: connection lost\\. Automatic reconnection is switched off, so "
                        + "a restart is needed; state now - value reads: not working, .*");
        assertThat(lines.get(2))
                .matches(ID + ", connection 1; connection recovered by itself\\. The adapter status still shows ERROR "
                        + "until the adapter is restarted; state now - value reads: working, value writes: working, "
                        + "method requests: none");
        assertThat(protocolAdapterState.getConnectionStatus()).isEqualTo(ProtocolAdapterState.ConnectionStatus.ERROR);
    }

    private static @NotNull ConnectionOptions withoutAutoReconnect() {
        return new ConnectionOptions(
                ConnectionOptions.DEFAULT_SESSION_TIMEOUT,
                ConnectionOptions.DEFAULT_REQUEST_TIMEOUT,
                ConnectionOptions.DEFAULT_KEEP_ALIVE_INTERVAL,
                ConnectionOptions.DEFAULT_KEEP_ALIVE_FAILURES_ALLOWED,
                ConnectionOptions.DEFAULT_CONNECTION_TIMEOUT,
                ConnectionOptions.DEFAULT_HEALTHCHECK_INTERVAL,
                ConnectionOptions.DEFAULT_RETRY_INTERVALS,
                false,
                ConnectionOptions.DEFAULT_RECONNECT_ON_SERVICE_FAULT);
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
                        connectionOptions));
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

    /// An event service that can hold the thread firing one particular event, after the event is stored. That is the
    /// gap in which a concurrent callback could write a line overtaking the one just fired.
    private static final class PausingEventService extends FakeEventService {
        private volatile @Nullable String pauseAfter;
        private final @NotNull CountDownLatch paused = new CountDownLatch(1);
        private final @NotNull CountDownLatch resumed = new CountDownLatch(1);

        void pauseAfter(final @NotNull String fragment) {
            pauseAfter = fragment;
        }

        boolean awaitPaused() throws InterruptedException {
            return paused.await(5, TimeUnit.SECONDS);
        }

        void resume() {
            resumed.countDown();
        }

        @Override
        public synchronized void fireEvent(final @NotNull Event event) {
            super.fireEvent(event);
            final String fragment = pauseAfter;
            if (fragment != null && event.getMessage().contains(fragment)) {
                pauseAfter = null;
                paused.countDown();
                try {
                    if (!resumed.await(5, TimeUnit.SECONDS)) {
                        throw new AssertionError("timed out waiting to resume");
                    }
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        @Override
        public synchronized @NotNull List<Event> readEvents(
                final @Nullable Long sinceTimestamp, final @Nullable Integer limit) {
            return super.readEvents(sinceTimestamp, limit);
        }
    }
}
