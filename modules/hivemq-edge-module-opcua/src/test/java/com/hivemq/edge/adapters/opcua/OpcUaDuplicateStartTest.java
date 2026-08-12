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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hivemq.adapter.sdk.api.ProtocolAdapterConnectionDirection;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.factories.AdapterFactories;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.edge.adapters.opcua.client.ParsedConfig;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * What a second {@code start()} does to the executors the first one created.
 * <p>
 * Review-02 finding 13. The v01 pass moved scheduler creation after configuration validation, so a rejected
 * configuration no longer left two threads behind. It stayed <em>before</em> the compare-and-set that claims
 * the connection slot, though — and that swap is what decides whether this call is entitled to create anything
 * at all.
 * <p>
 * So a duplicate start overwrote both fields and then failed. The original executors were still running the
 * retry and health-check work of the connection that was still live, and nothing held a reference to them any
 * more: a later {@code stop()} or {@code destroy()} could only shut down the replacements. One duplicate
 * lifecycle call, two orphaned scheduler threads, permanently — and orphaned threads that go on reconnecting
 * and health-checking an adapter, rather than merely idling.
 * <p>
 * Asserted by holding the executor <em>instances</em>, which is the point: the fields tell you what the
 * adapter can still reach, and the leak is precisely what it can no longer reach.
 */
class OpcUaDuplicateStartTest {

    private @NotNull ProtocolAdapterState protocolAdapterState;
    private @NotNull ModuleServices moduleServices;
    private @Nullable OpcUaProtocolAdapter adapter;

    @BeforeEach
    void setUp() {
        protocolAdapterState = new ProtocolAdapterStateImpl(mock(), "test-adapter-id", "opcua");
        // Built once and completely, before anything stubs it into another mock. Creating it inside a
        // thenReturn(...) argument stubs one mock while another's stubbing is unfinished, which Mockito
        // rejects as UnfinishedStubbingException -- and reports against whichever line it noticed on.
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
    void aSecondStartDoesNotReplaceTheExecutorsTheFirstOneCreated() {
        final OpcUaProtocolAdapter started = startedAdapter();
        final ExecutorService retryAfterFirst = scheduler(started, "retryScheduler");
        final ExecutorService healthAfterFirst = scheduler(started, "healthCheckScheduler");
        assertThat(retryAfterFirst)
                .as("precondition: the first start created them")
                .isNotNull();
        assertThat(healthAfterFirst).isNotNull();

        awaitTheConnectionAttemptFailing(started);
        started.start(
                ProtocolAdapterConnectionDirection.Northbound, startInput(), mock(ProtocolAdapterStartOutput.class));

        assertThat(scheduler(started, "retryScheduler"))
                .as("the adapter must still be able to reach the executors doing its work")
                .isSameAs(retryAfterFirst);
        assertThat(scheduler(started, "healthCheckScheduler")).isSameAs(healthAfterFirst);
    }

    @Test
    void andTheOnesTheFirstStartCreatedAreShutDownByDestroy() {
        // The consequence the identity check stands for. Held by instance rather than read back from the
        // fields, because a leaked executor is exactly one the fields no longer name -- reading the fields
        // after a duplicate start would have found the replacements and pronounced them healthy.
        final OpcUaProtocolAdapter started = startedAdapter();
        final ExecutorService retry = scheduler(started, "retryScheduler");
        final ExecutorService health = scheduler(started, "healthCheckScheduler");

        awaitTheConnectionAttemptFailing(started);
        started.start(
                ProtocolAdapterConnectionDirection.Northbound, startInput(), mock(ProtocolAdapterStartOutput.class));
        started.destroy();
        adapter = null;

        assertThat(retry.isShutdown())
                .as("no executor may outlive the adapter that created it")
                .isTrue();
        assertThat(health.isShutdown()).isTrue();
    }

    @Test
    void andTheSecondStartIsStillReportedAsAFailure() {
        // The refusal itself must survive being moved earlier: a caller starting twice has made a mistake and
        // has to be told, not quietly given a second adapter's worth of resources.
        final OpcUaProtocolAdapter started = startedAdapter();
        final ProtocolAdapterStartOutput output = mock(ProtocolAdapterStartOutput.class);

        awaitTheConnectionAttemptFailing(started);
        started.start(ProtocolAdapterConnectionDirection.Northbound, startInput(), output);

        verify(output).failStart(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.contains("already"));
    }

    // ── review-03 finding 5: the lifecycle is not the connection ────────────────────────────────────

    @Test
    void anAdapterWhoseConnectionAttemptFailedIsStillStarted() {
        // The finding, stated directly. The guard used to read `opcUaClientConnection != null`, and that
        // reference is the current connection *attempt* -- the failure path clears it while the adapter goes
        // on being started, schedulers running and a retry queued. This is the ordinary "the hardware is not
        // online yet" window, which is the whole reason start() reports success without a connection.
        //
        // No pinning and no reflection into the slot: the fix makes the losing ordering the *deterministic*
        // one to test, by waiting for it rather than racing to get in before it.
        final OpcUaProtocolAdapter started = startedAdapter();
        awaitTheConnectionAttemptFailing(started);

        final ProtocolAdapterStartOutput output = mock(ProtocolAdapterStartOutput.class);
        started.start(ProtocolAdapterConnectionDirection.Northbound, startInput(), output);

        verify(output)
                .failStart(
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.contains("already started"));
    }

    @Test
    void butStoppingItReleasesTheLifecycleSoItCanBeStartedAgain() {
        // The other direction, and the reason a lifecycle flag has to be released somewhere. Refusing every
        // start after the first would be a worse bug than the one being fixed: an adapter is stopped and
        // started again on every configuration change.
        final OpcUaProtocolAdapter started = startedAdapter();
        awaitTheConnectionAttemptFailing(started);
        started.stop(
                ProtocolAdapterConnectionDirection.Northbound,
                mock(ProtocolAdapterStopInput.class),
                mock(ProtocolAdapterStopOutput.class));

        final ProtocolAdapterStartOutput output = mock(ProtocolAdapterStartOutput.class);
        started.start(ProtocolAdapterConnectionDirection.Northbound, startInput(), output);

        verify(output).startedSuccessfully();
        verify(output, org.mockito.Mockito.never())
                .failStart(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void andDestroyingItDoesTooEvenWithoutAStop() {
        // destroy() is reachable without a stop() -- the framework may discard an adapter it never stopped --
        // and the same instance is reused across a configuration change, so a lifecycle left claimed here
        // would make the object permanently unstartable.
        final OpcUaProtocolAdapter started = startedAdapter();
        awaitTheConnectionAttemptFailing(started);
        started.destroy();

        final ProtocolAdapterStartOutput output = mock(ProtocolAdapterStartOutput.class);
        started.start(ProtocolAdapterConnectionDirection.Northbound, startInput(), output);

        verify(output).startedSuccessfully();
    }

    @Test
    void aStaleConnectionAttemptCannotClearANewerOne() {
        // The second half of the finding. The failure path did an unconditional set(null), so a completion
        // belonging to an attempt the adapter had already moved on from discarded whichever connection was
        // current -- while that one was live and reachable by nothing else. The adapter would then believe it
        // had no connection, and the live one would go on publishing with no way to stop it.
        final OpcUaProtocolAdapter started = startedAdapter();
        awaitTheConnectionAttemptFailing(started);

        final OpcUaClientConnection current = newConnection();
        connectionSlot(started).set(current);

        // A completion for a different, older attempt, driven directly because it is the ordering that
        // matters and it cannot be provoked by timing alone.
        attemptConnectionOn(started, newConnection());

        await().during(Duration.ofMillis(500)).atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(
                        connectionSlot(started).get())
                .as("a stale attempt's failure must not discard the connection that replaced it")
                .isSameAs(current));
    }

    // ── helpers ─────────────────────────────────────────────────────────────────────────────────────

    /**
     * An adapter that has completed one Northbound start.
     * <p>
     * No server: the connection attempt is asynchronous and its failure is the ordinary "hardware may come
     * online later" path, so the start still succeeds and the schedulers still exist — which is all these
     * tests are about.
     */
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

    /**
     * Waits until the adapter's own connection attempt has failed and cleared the slot.
     * <p>
     * This is the state the whole finding is about, and it used to be the state these tests were <em>racing
     * to get in front of</em>. {@code attemptConnection} runs asynchronously against an address with no
     * server behind it, and its failure path clears the connection reference while the adapter stays started.
     * The v02 tests pinned a connection into the slot to guarantee the second {@code start()} was a duplicate
     * — which papered over the defect, because with the guard reading that slot the second call was a
     * legitimate fresh start once it had been cleared, and CI proved it by failing on the losing ordering.
     * <p>
     * Now the guard reads the lifecycle instead, so this window is simply waited for. The tests assert the
     * previously-broken ordering rather than avoiding it, and there is nothing left to pin.
     */
    private static void awaitTheConnectionAttemptFailing(final @NotNull OpcUaProtocolAdapter adapter) {
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(connectionSlot(adapter).get())
                        .as("the connection attempt should have failed against an address with no server")
                        .isNull());
    }

    /** A connection object like the adapter's own, built without connecting anything. */
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

    /**
     * Runs one connection attempt for a given connection, as the adapter does internally.
     * <p>
     * Driven by reflection because the ordering under test — an older attempt completing after a newer
     * connection is in place — is decided by the scheduler and cannot be provoked by timing.
     */
    private void attemptConnectionOn(
            final @NotNull OpcUaProtocolAdapter adapter, final @NotNull OpcUaClientConnection connection) {
        try {
            final Field parsed = OpcUaProtocolAdapter.class.getDeclaredField("parsedConfig");
            parsed.setAccessible(true);
            final Method attempt = OpcUaProtocolAdapter.class.getDeclaredMethod(
                    "attemptConnection",
                    OpcUaClientConnection.class,
                    ParsedConfig.class,
                    ProtocolAdapterStartInput.class);
            attempt.setAccessible(true);
            attempt.invoke(adapter, connection, parsed.get(adapter), startInput());
        } catch (final ReflectiveOperationException e) {
            throw new LinkageError("the adapter no longer connects through 'attemptConnection'", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static @NotNull AtomicReference<OpcUaClientConnection> connectionSlot(
            final @NotNull OpcUaProtocolAdapter adapter) {
        try {
            final Field field = OpcUaProtocolAdapter.class.getDeclaredField("opcUaClientConnection");
            field.setAccessible(true);
            return (AtomicReference<OpcUaClientConnection>) field.get(adapter);
        } catch (final ReflectiveOperationException e) {
            throw new LinkageError("the adapter no longer holds its connection in 'opcUaClientConnection'", e);
        }
    }

    private static @NotNull ExecutorService scheduler(
            final @NotNull OpcUaProtocolAdapter adapter, final @NotNull String name) {
        try {
            final Field field = OpcUaProtocolAdapter.class.getDeclaredField(name);
            field.setAccessible(true);
            final Object value = field.get(adapter);
            if (value == null) {
                throw new AssertionError("the adapter has no '" + name + "' -- it was expected to be started");
            }
            return (ExecutorService) value;
        } catch (final ReflectiveOperationException e) {
            // LinkageError rather than AssertionError: this fails only when the field has been renamed or
            // removed, which is a broken assumption about the class rather than a failed assertion about it.
            throw new LinkageError("the adapter no longer schedules on a '" + name + "'", e);
        }
    }
}
