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
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hivemq.adapter.sdk.api.ProtocolAdapterConnectionDirection;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.factories.AdapterFactories;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.edge.adapters.opcua.client.ParsedConfig;
import com.hivemq.edge.adapters.opcua.client.Success;
import com.hivemq.edge.adapters.opcua.config.ConnectionOptions;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import com.hivemq.edge.adapters.opcua.listeners.OpcUaServiceFaultListener;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import util.EmbeddedOpcUaServerExtension;

/**
 * Review-05 finding 1, adapter half: whose attempt is this, and does the adapter still want one?
 * <p>
 * A connection attempt runs asynchronously and can take minutes — a connect, then up to three blocking round
 * trips per condition tag. Its completion callback used to ask one question, {@code stopped}, and act on the
 * answer as though it settled two.
 * <p>
 * <b>It did not settle whether the adapter still wanted the work.</b> {@code destroy()} cleared only
 * {@code started}, never {@code stopped}, so a completion arriving afterwards found {@code stopped} false and
 * proceeded: it overwrote the browse client and called {@code scheduleHealthCheck()}, against the scheduler
 * field {@code destroy()} had just nulled — an NPE thrown inside a {@code whenComplete} on a future nobody
 * holds, which is to say silently.
 * <p>
 * <b>And it did not settle whose attempt had completed.</b> A retry or a reconnect installs a newer
 * connection while an older attempt is still running, and every branch below the guard is then about the wrong
 * object. Ignoring the result is not enough on its own: an older attempt that <em>succeeded</em> holds a live
 * session and subscription reachable through nothing else, so it has to be destroyed rather than dropped.
 * <p>
 * Every ordering under test is decided by which thread gets there first, so it is driven directly through
 * {@code attemptConnection} rather than raced for — the same technique, and the same reason, as
 * {@code OpcUaDuplicateStartTest.aStaleConnectionAttemptCannotClearANewerOne}. Most of these need no server:
 * an attempt against a dead address fails, which is the ordinary "hardware is not online yet" path and
 * leaves the adapter started with its connection slot cleared. The last one needs the embedded server,
 * because the defect it pins only exists when the superseded attempt <em>succeeded</em>.
 */
class OpcUaAttemptOwnershipTest {

    @RegisterExtension
    public final @NotNull EmbeddedOpcUaServerExtension opcUaServerExtension = new EmbeddedOpcUaServerExtension();

    private @NotNull ProtocolAdapterState protocolAdapterState;
    private @NotNull ModuleServices moduleServices;
    private @Nullable OpcUaProtocolAdapter adapter;

    @BeforeEach
    void setUp() {
        protocolAdapterState = spy(new ProtocolAdapterStateImpl(mock(), "test-adapter-id", "opcua"));
        moduleServices = mock(ModuleServices.class);
        when(moduleServices.eventService()).thenReturn(new FakeEventService());
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
    @Timeout(60)
    void destroyStopsBackgroundWorkSoALateCompletionCannotScheduleAnything() {
        // The flag, stated as the property it stands for. `started` answers "may this object be started
        // again", which destroy() has always cleared; `stopped` answers "should background work keep
        // running", which it never did -- and the completion callback asks the second one.
        final OpcUaProtocolAdapter started = startedAdapter();
        awaitTheConnectionAttemptFailing(started);
        started.destroy();

        assertThat(flag(started, "stopped"))
                .as("a destroyed adapter must not be reporting that background work is still wanted")
                .isTrue();
        // Reachable only through that flag: the schedulers are gone by now, so anything that believed
        // otherwise would dereference null.
        assertThat(scheduler(started, "healthCheckScheduler"))
                .as("precondition: destroy() has already shut the schedulers down")
                .isNull();
    }

    @Test
    @Timeout(60)
    void andAStartAfterwardsWantsBackgroundWorkAgain() {
        // The other direction, and the reason the flag cannot simply be left set. The same instance is
        // reused across a configuration change, and an adapter that came back from destroy() with
        // `stopped` still true would start, report success, and then quietly schedule nothing.
        final OpcUaProtocolAdapter started = startedAdapter();
        awaitTheConnectionAttemptFailing(started);
        started.destroy();

        started.start(
                ProtocolAdapterConnectionDirection.Northbound, startInput(), mock(ProtocolAdapterStartOutput.class));

        assertThat(flag(started, "stopped"))
                .as("a restarted adapter wants its background work back")
                .isFalse();
    }

    @Test
    @Timeout(60)
    void destroyReleasesTheBrowseClientItCanNoLongerReach() {
        // stop() has always cleared it; destroy() is the more final of the two and did not. The client it
        // names is being disconnected, so keeping it only offers browse callers a dead session.
        final OpcUaProtocolAdapter started = startedAdapter();
        awaitTheConnectionAttemptFailing(started);
        browseClientSlot(started).set(mock(OpcUaClient.class));

        started.destroy();

        assertThat(browseClientSlot(started).get())
                .as("a destroyed adapter must not go on offering a browse client")
                .isNull();
    }

    @Test
    @Timeout(180)
    void aSupersededAttemptThatSucceedsIsDestroyedRatherThanLeftPublishing() {
        // The identity half, and it only bites when the stale attempt *succeeds* -- which is why this one
        // test needs a server where the rest of the class does not. Against a dead address the attempt fails
        // and takes the failure branch, which never touched the browse client anyway. The damage is done by
        // an attempt that connected and subscribed and only then found the adapter had moved on: written
        // against a dead address, this test passes on the unfixed code and proves nothing.
        //
        // Two consequences, asserted together because one guard answers both. The live browse client must
        // not be replaced by a superseded one -- browse is the adapter's answer to "look at the device while
        // a restart is in flight", so it would be answering from a session nothing else can reach. And the
        // superseded session must be closed rather than dropped: this completion holds the only reference to
        // it, so whatever it declines to close goes on publishing for the lifetime of the process.
        final OpcUaProtocolAdapter started = startedAdapter();
        awaitTheConnectionAttemptFailing(started);

        final OpcUaClient liveBrowseClient = mock(OpcUaClient.class);
        browseClientSlot(started).set(liveBrowseClient);
        connectionSlot(started).set(newConnection());

        final OpcUaClientConnection superseded = newConnectionAgainstTheServer();
        attemptConnectionOn(started, superseded, parsedServerConfig());

        // Waited for first, and the test is worthless without it. The attempt is asynchronous, so every
        // assertion below is trivially true at the instant it is submitted -- no client, no subscription,
        // browse client untouched. An earlier version of this test asserted the teardown straight away and
        // passed on the unfixed code, because awaitility's first poll ran before the connect did.
        await().atMost(Duration.ofSeconds(60)).untilAsserted(() -> verify(protocolAdapterState, atLeastOnce())
                .setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED));

        await().atMost(Duration.ofSeconds(60)).untilAsserted(() -> {
            assertThat(superseded.client())
                    .as("a superseded attempt must not be left holding a live session")
                    .isEmpty();
            assertThat(serverSubscriptions())
                    .as("and must take its subscription off the server with it")
                    .isZero();
        });
        assertThat(browseClientSlot(started).get())
                .as("a superseded attempt must not speak for the connection that replaced it")
                .isSameAs(liveBrowseClient);
    }

    // ── helpers ─────────────────────────────────────────────────────────────────────────────────────

    private @NotNull OpcUaProtocolAdapter startedAdapter() {
        final OpcuaTag tag =
                new OpcuaTag("boiler-temperature", "", new OpcuaTagDefinition("ns=2;s=Boiler1.Temperature"));

        final ProtocolAdapterInformation adapterInformation = mock(ProtocolAdapterInformation.class);
        when(adapterInformation.getProtocolId()).thenReturn("opcua");

        @SuppressWarnings("unchecked")
        final ProtocolAdapterInput<OpcUaSpecificAdapterConfig> input = mock(ProtocolAdapterInput.class);
        when(input.getAdapterId()).thenReturn("test-adapter-id");
        when(input.getProtocolAdapterState()).thenReturn(protocolAdapterState);
        when(input.getConfig()).thenReturn(adapterConfig());
        when(input.getTags()).thenReturn(new ArrayList<Tag>(List.of(tag)));
        when(input.adapterFactories()).thenReturn(mock(AdapterFactories.class));
        when(input.getProtocolAdapterMetricsHelper()).thenReturn(mock(ProtocolAdapterMetricsService.class));
        when(input.moduleServices()).thenReturn(moduleServices);

        final OpcUaProtocolAdapter created = new OpcUaProtocolAdapter(adapterInformation, input);
        adapter = created;
        created.start(
                ProtocolAdapterConnectionDirection.Northbound, startInput(), mock(ProtocolAdapterStartOutput.class));
        return created;
    }

    private @NotNull ProtocolAdapterStartInput startInput() {
        final ProtocolAdapterStartInput startInput = mock(ProtocolAdapterStartInput.class);
        when(startInput.moduleServices()).thenReturn(moduleServices);
        return startInput;
    }

    private static @NotNull OpcUaSpecificAdapterConfig adapterConfig() {
        // An address with nothing behind it: the attempt fails, which is the ordinary "hardware is not
        // online yet" path and leaves the adapter started with its slot cleared.
        return new OpcUaSpecificAdapterConfig(
                "opc.tcp://127.0.0.1:4840",
                false,
                null,
                null,
                null,
                OpcUaToMqttConfig.defaultOpcUaToMqttConfig(),
                null,
                ConnectionOptions.defaultConnectionOptions());
    }

    private static void awaitTheConnectionAttemptFailing(final @NotNull OpcUaProtocolAdapter adapter) {
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(connectionSlot(adapter).get())
                        .as("the connection attempt should have failed against an address with no server")
                        .isNull());
    }

    private @NotNull OpcUaClientConnection newConnection() {
        final OpcuaTag tag =
                new OpcuaTag("boiler-temperature", "", new OpcuaTagDefinition("ns=2;s=Boiler1.Temperature"));
        return new OpcUaClientConnection(
                "test-adapter-id",
                List.of(tag),
                protocolAdapterState,
                mock(ProtocolAdapterTagStreamingService.class),
                new FakeEventService(),
                mock(ProtocolAdapterMetricsService.class),
                adapterConfig(),
                mock(OpcUaServiceFaultListener.class));
    }

    /** A connection that will actually reach the embedded server, so its attempt succeeds. */
    private @NotNull OpcUaClientConnection newConnectionAgainstTheServer() {
        final OpcuaTag tag = new OpcuaTag(
                "temperature",
                "",
                new OpcuaTagDefinition(
                        "ns=" + opcUaServerExtension.getTestNamespace().getNamespaceIndex() + ";i=10"));
        return new OpcUaClientConnection(
                "test-adapter-id",
                List.of(tag),
                protocolAdapterState,
                mock(ProtocolAdapterTagStreamingService.class),
                new FakeEventService(),
                mock(ProtocolAdapterMetricsService.class),
                serverConfig(),
                new OpcUaServiceFaultListener(
                        mock(ProtocolAdapterMetricsService.class),
                        new FakeEventService(),
                        "test-adapter-id",
                        () -> {},
                        true));
    }

    private @NotNull OpcUaSpecificAdapterConfig serverConfig() {
        return new OpcUaSpecificAdapterConfig(
                opcUaServerExtension.getServerUri(),
                false,
                null,
                null,
                null,
                new OpcUaToMqttConfig(1, 1000),
                null,
                ConnectionOptions.defaultConnectionOptions());
    }

    private @NotNull ParsedConfig parsedServerConfig() {
        final var result = ParsedConfig.fromConfig(serverConfig());
        assertThat(result).isInstanceOf(Success.class);
        return ((Success<ParsedConfig, String>) result).result();
    }

    private int serverSubscriptions() {
        return java.util.Objects.requireNonNull(opcUaServerExtension.getOpcUaServer())
                .getSubscriptions()
                .size();
    }

    /**
     * Runs one connection attempt, as the adapter does internally.
     * <p>
     * Driven by reflection because the ordering under test — an older attempt completing after a newer
     * connection is in place — is decided by the scheduler and cannot be provoked by timing.
     */
    private void attemptConnectionOn(
            final @NotNull OpcUaProtocolAdapter adapter,
            final @NotNull OpcUaClientConnection connection,
            final @NotNull ParsedConfig parsedConfig) {
        try {
            final Method attempt = OpcUaProtocolAdapter.class.getDeclaredMethod(
                    "attemptConnection",
                    OpcUaClientConnection.class,
                    ParsedConfig.class,
                    ProtocolAdapterStartInput.class);
            attempt.setAccessible(true);
            attempt.invoke(adapter, connection, parsedConfig, startInput());
        } catch (final ReflectiveOperationException e) {
            throw new LinkageError("the adapter no longer connects through 'attemptConnection'", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static @NotNull AtomicReference<OpcUaClientConnection> connectionSlot(
            final @NotNull OpcUaProtocolAdapter adapter) {
        return (AtomicReference<OpcUaClientConnection>) field(adapter, "opcUaClientConnection");
    }

    @SuppressWarnings("unchecked")
    private static @NotNull AtomicReference<OpcUaClient> browseClientSlot(final @NotNull OpcUaProtocolAdapter adapter) {
        return (AtomicReference<OpcUaClient>) field(adapter, "browseClient");
    }

    private static boolean flag(final @NotNull OpcUaProtocolAdapter adapter, final @NotNull String name) {
        return (Boolean) java.util.Objects.requireNonNull(field(adapter, name));
    }

    private static @Nullable Object scheduler(final @NotNull OpcUaProtocolAdapter adapter, final @NotNull String name) {
        return field(adapter, name);
    }

    private static @Nullable Object field(final @NotNull OpcUaProtocolAdapter adapter, final @NotNull String name) {
        try {
            final Field field = OpcUaProtocolAdapter.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(adapter);
        } catch (final ReflectiveOperationException e) {
            // LinkageError rather than AssertionError: this fails only when the field has been renamed or
            // removed, which is a broken assumption about the class rather than a failed assertion about it.
            throw new LinkageError("the adapter no longer has a '" + name + "'", e);
        }
    }
}
