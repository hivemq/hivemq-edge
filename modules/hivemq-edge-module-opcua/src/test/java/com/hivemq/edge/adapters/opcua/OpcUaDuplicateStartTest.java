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
import com.hivemq.edge.adapters.opcua.config.ConnectionOptions;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import com.hivemq.edge.adapters.opcua.listeners.OpcUaServiceFaultListener;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import java.lang.reflect.Field;
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

        pinTheConnection(started);
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

        pinTheConnection(started);
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

        pinTheConnection(started);
        started.start(ProtocolAdapterConnectionDirection.Northbound, startInput(), output);

        verify(output).failStart(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.contains("already"));
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
     * Guarantees the adapter is holding a connection, so the next {@code start()} is unambiguously a duplicate.
     * <p>
     * Without this the test races the adapter's own connect. {@code attemptConnection} runs asynchronously
     * against an address with no server behind it, and its failure path clears the connection slot — so on a
     * slower machine the second {@code start()} finds an empty slot, which is a legitimate fresh start rather
     * than the duplicate this is about, and creating new executors there is correct. It passed locally and
     * failed on CI for exactly that reason: {@code to refer to the same object}, with two different
     * executors, because the second call was never a duplicate at all.
     * <p>
     * A fresh connection object rather than the one the first start installed, because reading that one back
     * is the same race one step earlier. Constructing it is cheap and does no I/O — the connect happens in
     * {@code attemptConnection}, not here — and the package-private constructor is reachable because this
     * test shares the adapter's package.
     */
    private void pinTheConnection(final @NotNull OpcUaProtocolAdapter adapter) {
        final OpcuaTag tag =
                new OpcuaTag("boiler-temperature", "", new OpcuaTagDefinition("ns=2;s=Boiler1.Temperature"));
        final OpcUaClientConnection connection = new OpcUaClientConnection(
                "test-adapter-id",
                List.of(tag),
                protocolAdapterState,
                mock(ProtocolAdapterTagStreamingService.class),
                new FakeEventService(),
                mock(ProtocolAdapterMetricsService.class),
                adapterConfig(),
                mock(OpcUaServiceFaultListener.class));
        connectionSlot(adapter).set(connection);
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
