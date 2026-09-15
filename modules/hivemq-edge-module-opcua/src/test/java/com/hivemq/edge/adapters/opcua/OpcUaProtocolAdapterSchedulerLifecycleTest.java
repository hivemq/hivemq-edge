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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hivemq.adapter.sdk.api.ProtocolAdapterConnectionDirection;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.events.EventService;
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
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.Tls;
import com.hivemq.edge.adapters.opcua.config.TlsChecks;
import com.hivemq.edge.adapters.opcua.config.TlsChecksFull;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * EDG-585 F4: when the adapter's scheduler pair is created, and when it is not.
 *
 * <p>{@code start()} used to create both {@link ScheduledExecutorService}s before it had parsed the
 * configuration. Every failure return afterwards leaves via {@code output.failStart}, and
 * {@code ProtocolAdapterWrapper.startNorthbound} catches that, transitions the adapter to
 * {@code Error} and returns {@code false} — it never calls {@code stop()}. Nothing was left holding a
 * reference to the pair, so nothing could ever shut it down.
 *
 * <p>The sharper case is starting an adapter that is already started: the fields were overwritten
 * before the already-started check, orphaning the running adapter's schedulers — those genuinely do
 * own live threads, because retry and health-check tasks have been submitted to them.
 *
 * <p>These assertions read the two private fields reflectively, which is deliberate. A thread count
 * cannot express the contract: {@code Executors.newSingleThreadScheduledExecutor()} does not create
 * its thread until a task is submitted (measured), so the abandoned-on-parse-failure pair holds no
 * thread at all and a thread-counting test would pass against the defect.
 */
class OpcUaProtocolAdapterSchedulerLifecycleTest {

    private static final @NotNull String UNREACHABLE_URI = "opc.tcp://localhost:1/never-connected";

    private final @NotNull List<ExecutorService> liveSchedulers = new ArrayList<>();

    private @Nullable OpcUaProtocolAdapter adapter;
    private @NotNull ProtocolAdapterState protocolAdapterState;

    @BeforeEach
    void setUp() {
        protocolAdapterState = new ProtocolAdapterStateImpl(mock(), "test-adapter-id", "opcua");
    }

    @AfterEach
    void tearDown() {
        if (adapter != null) {
            adapter.destroy();
        }
        liveSchedulers.forEach(ExecutorService::shutdownNow);
    }

    @Test
    void anUnparseableConfiguration_createsNoSchedulersAtAll() throws Exception {
        // Both validation doors set at once, which the projection rejects, so fromConfig returns a
        // Failure and start() returns before a connection is ever built. Nothing was committed, so
        // nothing may have been allocated.
        adapter = adapterWith(bothDoorsSet());
        final ProtocolAdapterStartOutput output = mock(ProtocolAdapterStartOutput.class);

        adapter.start(ProtocolAdapterConnectionDirection.Northbound, startInput(), output);

        verify(output).failStart(any(), any());
        verify(output, never()).startedSuccessfully();
        assertThat(schedulerField(adapter, "retryScheduler")).isNull();
        assertThat(schedulerField(adapter, "healthCheckScheduler")).isNull();
    }

    @Test
    void startingAnAlreadyStartedAdapter_leavesTheRunningSchedulersAlone() throws Exception {
        // The branch where real threads leak. The adapter is put in the state a started adapter is
        // in - a connection present, a scheduler pair with live threads - and started again. The
        // second start must be refused without touching either.
        adapter = adapterWith(noTls());

        final ScheduledExecutorService runningRetry = liveScheduler();
        final ScheduledExecutorService runningHealthCheck = liveScheduler();
        setSchedulerField(adapter, "retryScheduler", runningRetry);
        setSchedulerField(adapter, "healthCheckScheduler", runningHealthCheck);
        setConnection(adapter, mock(OpcUaClientConnection.class));

        final ProtocolAdapterStartOutput output = mock(ProtocolAdapterStartOutput.class);
        adapter.start(ProtocolAdapterConnectionDirection.Northbound, startInput(), output);

        verify(output).failStart(any(), any());
        assertThat(schedulerField(adapter, "retryScheduler"))
                .as("the running adapter's retry scheduler must not be replaced")
                .isSameAs(runningRetry);
        assertThat(schedulerField(adapter, "healthCheckScheduler"))
                .as("the running adapter's health-check scheduler must not be replaced")
                .isSameAs(runningHealthCheck);
        assertThat(runningRetry.isShutdown()).isFalse();
        assertThat(runningHealthCheck.isShutdown()).isFalse();
    }

    @Test
    void aCommittedStart_createsTheSchedulers() throws Exception {
        // The control. The fix must not stop the schedulers from being created on the path that
        // needs them: the adapter starts successfully even when the endpoint is unreachable, because
        // the hardware may come online later - and that retry is what the retry scheduler is for.
        adapter = adapterWith(noTls());
        final ProtocolAdapterStartOutput output = mock(ProtocolAdapterStartOutput.class);

        adapter.start(ProtocolAdapterConnectionDirection.Northbound, startInput(), output);

        verify(output).startedSuccessfully();
        assertThat(schedulerField(adapter, "retryScheduler")).isNotNull();
        assertThat(schedulerField(adapter, "healthCheckScheduler")).isNotNull();
    }

    @Test
    void stoppingAStartedAdapter_shutsTheSchedulersDown() throws Exception {
        // The other half of the control: the pair created by a committed start is still reclaimed on
        // the normal path, so the fix has not moved the leak rather than removed it.
        adapter = adapterWith(noTls());
        adapter.start(
                ProtocolAdapterConnectionDirection.Northbound, startInput(), mock(ProtocolAdapterStartOutput.class));

        final ScheduledExecutorService retry = schedulerField(adapter, "retryScheduler");
        final ScheduledExecutorService healthCheck = schedulerField(adapter, "healthCheckScheduler");
        assertThat(retry).isNotNull();
        assertThat(healthCheck).isNotNull();

        adapter.stop(
                ProtocolAdapterConnectionDirection.Northbound,
                mock(ProtocolAdapterStopInput.class),
                mock(ProtocolAdapterStopOutput.class));

        assertThat(retry.isShutdown()).isTrue();
        assertThat(healthCheck.isShutdown()).isTrue();
        assertThat(schedulerField(adapter, "retryScheduler")).isNull();
        assertThat(schedulerField(adapter, "healthCheckScheduler")).isNull();
    }

    @Test
    void aFailedStartCanStillBeStoppedAndDestroyed() {
        // shutdownSchedulers() and the cancel helpers have to tolerate the fields never having been
        // populated, otherwise the fix trades a leak for a NullPointerException on the teardown path
        // the manager takes after a failed start.
        adapter = adapterWith(bothDoorsSet());
        adapter.start(
                ProtocolAdapterConnectionDirection.Northbound, startInput(), mock(ProtocolAdapterStartOutput.class));

        assertThatCode(() -> {
                    adapter.stop(
                            ProtocolAdapterConnectionDirection.Northbound,
                            mock(ProtocolAdapterStopInput.class),
                            mock(ProtocolAdapterStopOutput.class));
                    adapter.destroy();
                })
                .doesNotThrowAnyException();
    }

    @Test
    void repeatedFailedStarts_neverAccumulateSchedulers() throws Exception {
        // The shape that made the original defect unbounded: ProtocolAdapterManager retries a failed
        // adapter, and each attempt used to allocate a fresh pair over the last one.
        adapter = adapterWith(bothDoorsSet());

        for (int attempt = 0; attempt < 5; attempt++) {
            adapter.start(
                    ProtocolAdapterConnectionDirection.Northbound,
                    startInput(),
                    mock(ProtocolAdapterStartOutput.class));
            assertThat(schedulerField(adapter, "retryScheduler"))
                    .as("attempt %d", attempt)
                    .isNull();
            assertThat(schedulerField(adapter, "healthCheckScheduler"))
                    .as("attempt %d", attempt)
                    .isNull();
        }
    }

    @Test
    void aSouthboundStart_createsNoSchedulers() throws Exception {
        // Southbound is a no-op that returns before any of this; pinned so the early return is not
        // quietly changed into a second scheduler-creating path.
        adapter = adapterWith(noTls());
        final ProtocolAdapterStartOutput output = mock(ProtocolAdapterStartOutput.class);

        adapter.start(ProtocolAdapterConnectionDirection.Southbound, startInput(), output);

        verify(output).startedSuccessfully();
        assertThat(schedulerField(adapter, "retryScheduler")).isNull();
        assertThat(schedulerField(adapter, "healthCheckScheduler")).isNull();
    }

    // -- fixtures --------------------------------------------------------------------------------

    /** Rejected by TlsChecksProjection: the two validation doors are mutually exclusive. */
    private static @NotNull OpcUaSpecificAdapterConfig bothDoorsSet() {
        return config(new Tls(true, TlsChecks.STANDARD, TlsChecksFull.allAxesUnset(), null, null, null));
    }

    private static @NotNull OpcUaSpecificAdapterConfig noTls() {
        return config(null);
    }

    private static @NotNull OpcUaSpecificAdapterConfig config(final @Nullable Tls tls) {
        return new OpcUaSpecificAdapterConfig(
                UNREACHABLE_URI, false, null, null, tls, new OpcUaToMqttConfig(1, 1000), null, null);
    }

    private @NotNull OpcUaProtocolAdapter adapterWith(final @NotNull OpcUaSpecificAdapterConfig config) {
        final ProtocolAdapterInformation information = mock(ProtocolAdapterInformation.class);
        when(information.getProtocolId()).thenReturn("opcua");

        // Every collaborator is built before the stubbing that returns it: creating a mock inside
        // when(...) reads to Mockito as an unfinished stubbing and fails the test.
        final List<Tag> tags = List.of(new OpcuaTag("tag", "a tag", new OpcuaTagDefinition("ns=1;i=1")));
        final AdapterFactories adapterFactories = mock(AdapterFactories.class);
        final ProtocolAdapterMetricsService metrics = mock(ProtocolAdapterMetricsService.class);
        final ModuleServices services = moduleServices();

        @SuppressWarnings("unchecked")
        final ProtocolAdapterInput<OpcUaSpecificAdapterConfig> input = mock(ProtocolAdapterInput.class);
        when(input.getAdapterId()).thenReturn("test-adapter-id");
        when(input.getProtocolAdapterState()).thenReturn(protocolAdapterState);
        when(input.getConfig()).thenReturn(config);
        when(input.getTags()).thenReturn(tags);
        when(input.adapterFactories()).thenReturn(adapterFactories);
        when(input.getProtocolAdapterMetricsHelper()).thenReturn(metrics);
        when(input.moduleServices()).thenReturn(services);

        return new OpcUaProtocolAdapter(information, input);
    }

    private static @NotNull ProtocolAdapterStartInput startInput() {
        final ModuleServices services = moduleServices();
        final ProtocolAdapterStartInput input = mock(ProtocolAdapterStartInput.class);
        when(input.moduleServices()).thenReturn(services);
        return input;
    }

    private static @NotNull ModuleServices moduleServices() {
        final EventService eventService = mock(EventService.class);
        final ProtocolAdapterTagStreamingService streaming = mock(ProtocolAdapterTagStreamingService.class);
        final ModuleServices services = mock(ModuleServices.class);
        when(services.eventService()).thenReturn(eventService);
        when(services.protocolAdapterTagStreamingService()).thenReturn(streaming);
        return services;
    }

    /** A scheduler that has been given work, so it actually owns a thread — as a running one does. */
    private @NotNull ScheduledExecutorService liveScheduler() {
        final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        liveSchedulers.add(scheduler);
        scheduler.schedule(() -> {}, 1, TimeUnit.HOURS);
        return scheduler;
    }

    // -- reflective access to the two private fields ---------------------------------------------

    private static @Nullable ScheduledExecutorService schedulerField(
            final @NotNull OpcUaProtocolAdapter adapter, final @NotNull String name) throws Exception {
        return (ScheduledExecutorService) field(name).get(adapter);
    }

    private static void setSchedulerField(
            final @NotNull OpcUaProtocolAdapter adapter,
            final @NotNull String name,
            final @NotNull ScheduledExecutorService value)
            throws Exception {
        field(name).set(adapter, value);
    }

    @SuppressWarnings("unchecked")
    private static void setConnection(
            final @NotNull OpcUaProtocolAdapter adapter, final @NotNull OpcUaClientConnection connection)
            throws Exception {
        ((AtomicReference<OpcUaClientConnection>) field("opcUaClientConnection").get(adapter)).set(connection);
    }

    private static @NotNull Field field(final @NotNull String name) throws Exception {
        final Field field = OpcUaProtocolAdapter.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
