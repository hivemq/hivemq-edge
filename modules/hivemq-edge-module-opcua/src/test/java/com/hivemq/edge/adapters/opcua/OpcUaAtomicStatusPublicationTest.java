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
import static org.mockito.Mockito.when;

import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
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
import com.hivemq.edge.adapters.opcua.listeners.OpcUaSessionActivityListener;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.milo.opcua.sdk.client.UaSession;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Review-07 finding 3: connection ownership validation and its shared status write are one operation.
 * <p>
 * A predicate suppressed callbacks that were already stale when they arrived, but its check and the following
 * {@link ProtocolAdapterState#setConnectionStatus} were separate operations. Connection A could pass the check,
 * pause, connection B could replace it and publish CONNECTED, and then A could overwrite B with DISCONNECTED.
 * The adapter now owns a lock covering both the generation slot and every production status publication.
 */
class OpcUaAtomicStatusPublicationTest {

    private @NotNull BlockingProtocolAdapterState protocolAdapterState;
    private @NotNull ModuleServices moduleServices;
    private @Nullable OpcUaProtocolAdapter adapter;

    @BeforeEach
    void setUp() {
        protocolAdapterState = new BlockingProtocolAdapterState();
        moduleServices = mock(ModuleServices.class);
        when(moduleServices.eventService()).thenReturn(new FakeEventService());
        when(moduleServices.protocolAdapterTagStreamingService())
                .thenReturn(mock(ProtocolAdapterTagStreamingService.class));
        adapter = newAdapter();
    }

    @AfterEach
    void tearDown() {
        protocolAdapterState.releaseBlockedWrite();
        if (adapter != null) {
            adapter.destroy();
        }
    }

    @Test
    @Timeout(10)
    void aGenerationCannotBeReplacedBetweenItsOwnershipCheckAndStatusWrite() throws Exception {
        final OpcUaProtocolAdapter currentAdapter = adapter;
        assertThat(currentAdapter).isNotNull();
        final OpcUaClientConnection first = mock(OpcUaClientConnection.class);
        final OpcUaClientConnection replacement = mock(OpcUaClientConnection.class);
        assertThat(currentAdapter.claimConnection(first)).isTrue();

        // This latch is inside setConnectionStatus: the old predicate implementation had already completed
        // its ownership check when it reached this point. The session listener takes the same callback route
        // production uses, and holding it here pins that route's exact check-then-act gap.
        protocolAdapterState.blockNextWriteOf(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
        final OpcUaSessionActivityListener firstSession = new OpcUaSessionActivityListener(
                mock(ProtocolAdapterMetricsService.class),
                new FakeEventService(),
                "test-adapter-id",
                status -> currentAdapter.publishStatusFrom(first, status),
                () -> true);
        final CompletableFuture<Void> staleWrite =
                CompletableFuture.runAsync(() -> firstSession.onSessionInactive(mock(UaSession.class)));
        assertThat(protocolAdapterState.awaitBlockedWrite()).isTrue();

        final CountDownLatch replacementStarted = new CountDownLatch(1);
        final AtomicReference<Thread> replacementThread = new AtomicReference<>();
        final CompletableFuture<OpcUaClientConnection> replacementWork = CompletableFuture.supplyAsync(() -> {
            replacementThread.set(Thread.currentThread());
            replacementStarted.countDown();
            final OpcUaClientConnection released = currentAdapter.releaseCurrentConnection();
            assertThat(currentAdapter.claimConnection(replacement)).isTrue();
            currentAdapter.publishStatusFrom(replacement, ProtocolAdapterState.ConnectionStatus.CONNECTED);
            return released;
        });
        assertThat(replacementStarted.await(5, TimeUnit.SECONDS)).isTrue();
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(replacementThread.get())
                .as("replacement must wait on the generation lock until A's status write is complete")
                .extracting(Thread::getState)
                .isEqualTo(Thread.State.WAITING));

        protocolAdapterState.releaseBlockedWrite();
        staleWrite.get(5, TimeUnit.SECONDS);
        assertThat(replacementWork.get(5, TimeUnit.SECONDS)).isSameAs(first);
        assertThat(protocolAdapterState.getConnectionStatus())
                .as("B's CONNECTED must be ordered after A's already-authorized DISCONNECTED")
                .isEqualTo(ProtocolAdapterState.ConnectionStatus.CONNECTED);
        assertThat(currentAdapter.releaseCurrentConnection()).isSameAs(replacement);
    }

    @Test
    void aSupersededFailedAttemptNeitherReportsErrorNorSchedulesARetry() {
        final OpcUaProtocolAdapter currentAdapter = adapter;
        assertThat(currentAdapter).isNotNull();
        final OpcUaClientConnection superseded = mock(OpcUaClientConnection.class);
        final OpcUaClientConnection replacement = mock(OpcUaClientConnection.class);
        assertThat(currentAdapter.claimConnection(replacement)).isTrue();
        protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);

        assertThat(currentAdapter.completeFailedAttempt(superseded, startInput(), new IllegalStateException("late")))
                .isFalse();

        assertThat(protocolAdapterState.getConnectionStatus())
                .isEqualTo(ProtocolAdapterState.ConnectionStatus.CONNECTED);
        assertThat(currentAdapter.consecutiveRetryAttempts()).isZero();
        assertThat(currentAdapter.releaseCurrentConnection()).isSameAs(replacement);
    }

    @Test
    void aCurrentFailedAttemptStillReportsErrorAndEntersTheRetryPath() {
        final OpcUaProtocolAdapter currentAdapter = adapter;
        assertThat(currentAdapter).isNotNull();
        final OpcUaClientConnection current = mock(OpcUaClientConnection.class);
        assertThat(currentAdapter.claimConnection(current)).isTrue();
        protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTING);

        assertThat(currentAdapter.completeFailedAttempt(current, startInput(), null))
                .isTrue();

        assertThat(protocolAdapterState.getConnectionStatus()).isEqualTo(ProtocolAdapterState.ConnectionStatus.ERROR);
        assertThat(currentAdapter.consecutiveRetryAttempts()).isOne();
        assertThat(currentAdapter.releaseCurrentConnection()).isNull();
    }

    @Test
    void onlyTheCurrentGenerationCanPublishReadinessAndConnectedTogether() {
        final OpcUaProtocolAdapter currentAdapter = adapter;
        assertThat(currentAdapter).isNotNull();
        final OpcUaClientConnection superseded = mock(OpcUaClientConnection.class);
        final OpcUaClientConnection current = mock(OpcUaClientConnection.class);
        when(current.hasBrowseMetadata()).thenReturn(true);
        assertThat(currentAdapter.claimConnection(current)).isTrue();

        currentAdapter.publishReadyFrom(superseded);

        assertThat(currentAdapter.isBrowseReady()).isFalse();
        assertThat(protocolAdapterState.getConnectionStatus())
                .isEqualTo(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);

        currentAdapter.publishReadyFrom(current);

        assertThat(currentAdapter.isBrowseReady()).isTrue();
        assertThat(protocolAdapterState.getConnectionStatus())
                .isEqualTo(ProtocolAdapterState.ConnectionStatus.CONNECTED);
        assertThat(currentAdapter.releaseCurrentConnection()).isSameAs(current);
    }

    /**
     * Review-09 finding 2: connected and browse-ready are published together but are not the same fact.
     * <p>
     * A connection that could not build the data-type tree is fully connected — verified tags, live
     * subscriptions, events and values — but a browse served from an unhydrated address space is
     * non-deterministic, so the endpoint must keep answering 503. Claiming browse-ready here would be the
     * same untruth as the optimistic {@code CONNECTED} this readiness model was added to remove.
     */
    @Test
    void aConnectionThatCouldNotBuildItsMetadataIsConnectedButNotBrowseReady() {
        final OpcUaProtocolAdapter currentAdapter = adapter;
        assertThat(currentAdapter).isNotNull();
        final OpcUaClientConnection withoutMetadata = mock(OpcUaClientConnection.class);
        when(withoutMetadata.hasBrowseMetadata()).thenReturn(false);
        assertThat(currentAdapter.claimConnection(withoutMetadata)).isTrue();

        currentAdapter.publishReadyFrom(withoutMetadata);

        assertThat(protocolAdapterState.getConnectionStatus())
                .as("the data path is up and the adapter says so")
                .isEqualTo(ProtocolAdapterState.ConnectionStatus.CONNECTED);
        assertThat(currentAdapter.isBrowseReady())
                .as("but browse is not claimed on the strength of an attempt that did not hydrate it")
                .isFalse();
    }

    private @NotNull OpcUaProtocolAdapter newAdapter() {
        final ProtocolAdapterInformation adapterInformation = mock(ProtocolAdapterInformation.class);
        when(adapterInformation.getProtocolId()).thenReturn("opcua");

        @SuppressWarnings("unchecked")
        final ProtocolAdapterInput<OpcUaSpecificAdapterConfig> input = mock(ProtocolAdapterInput.class);
        when(input.getAdapterId()).thenReturn("test-adapter-id");
        when(input.getProtocolAdapterState()).thenReturn(protocolAdapterState);
        when(input.getConfig()).thenReturn(adapterConfig());
        when(input.getTags()).thenReturn(List.<Tag>of());
        when(input.adapterFactories()).thenReturn(mock(AdapterFactories.class));
        when(input.getProtocolAdapterMetricsHelper()).thenReturn(mock(ProtocolAdapterMetricsService.class));
        when(input.moduleServices()).thenReturn(moduleServices);
        return new OpcUaProtocolAdapter(adapterInformation, input);
    }

    private @NotNull ProtocolAdapterStartInput startInput() {
        final ProtocolAdapterStartInput input = mock(ProtocolAdapterStartInput.class);
        when(input.moduleServices()).thenReturn(moduleServices);
        return input;
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

    /** A real state whose decisive write can be paused after the ownership check. */
    private static final class BlockingProtocolAdapterState extends ProtocolAdapterStateImpl {
        private final @NotNull AtomicReference<ConnectionStatus> blockedStatus = new AtomicReference<>();
        private final @NotNull AtomicBoolean armed = new AtomicBoolean();
        private final @NotNull CountDownLatch writeEntered = new CountDownLatch(1);
        private final @NotNull CountDownLatch releaseWrite = new CountDownLatch(1);

        private BlockingProtocolAdapterState() {
            super(mock(), "test-adapter-id", "opcua");
        }

        void blockNextWriteOf(final @NotNull ConnectionStatus status) {
            blockedStatus.set(status);
            armed.set(true);
        }

        boolean awaitBlockedWrite() throws InterruptedException {
            return writeEntered.await(5, TimeUnit.SECONDS);
        }

        void releaseBlockedWrite() {
            releaseWrite.countDown();
        }

        @Override
        public boolean setConnectionStatus(final @NotNull ConnectionStatus connectionStatus) {
            if (connectionStatus == blockedStatus.get() && armed.compareAndSet(true, false)) {
                writeEntered.countDown();
                try {
                    if (!releaseWrite.await(5, TimeUnit.SECONDS)) {
                        throw new AssertionError("timed out waiting to release blocked status write");
                    }
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError("interrupted while waiting to release blocked status write", e);
                }
            }
            return super.setConnectionStatus(connectionStatus);
        }
    }
}
