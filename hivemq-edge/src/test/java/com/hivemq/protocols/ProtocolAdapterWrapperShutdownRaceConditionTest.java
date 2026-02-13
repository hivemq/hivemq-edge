/*
 * Copyright 2019-present HiveMQ GmbH
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
package com.hivemq.protocols;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hivemq.adapter.sdk.api.ProtocolAdapterCategory;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.ProtocolAdapterTag;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.EventBuilder;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.adapter.sdk.api.writing.WritingInput;
import com.hivemq.adapter.sdk.api.writing.WritingOutput;
import com.hivemq.adapter.sdk.api.writing.WritingPayload;
import com.hivemq.adapter.sdk.api.writing.WritingProtocolAdapter;
import com.hivemq.edge.modules.adapters.data.TagManager;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPollingService;
import com.hivemq.edge.modules.api.events.model.EventBuilderImpl;
import com.hivemq.protocols.northbound.NorthboundConsumerFactory;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class ProtocolAdapterWrapperShutdownRaceConditionTest {

    private @NotNull EventService eventService;
    private @NotNull InternalProtocolAdapterWritingService protocolAdapterWritingService;
    private @NotNull ProtocolAdapterPollingService protocolAdapterPollingService;
    private @NotNull NorthboundConsumerFactory northboundConsumerFactory;
    private @NotNull TagManager tagManager;
    private @NotNull ModuleServices moduleServices;

    @BeforeEach
    void setUp() {
        eventService = mock();
        protocolAdapterWritingService = mock();
        protocolAdapterPollingService = mock();
        northboundConsumerFactory = mock();
        tagManager = mock();
        moduleServices = mock();

        final EventBuilder eventBuilder = new EventBuilderImpl(mock());
        when(eventService.createAdapterEvent(anyString(), anyString())).thenReturn(eventBuilder);
        when(protocolAdapterWritingService.writingEnabled()).thenReturn(true);
        when(protocolAdapterWritingService.startWritingAsync(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(true));
        when(moduleServices.eventService()).thenReturn(eventService);
    }

    @Test
    @Timeout(10)
    void test_stopAsync_preventsStateChangesDuringShutdown() throws Exception {
        final ProtocolAdapterStateImpl adapterState =
                new ProtocolAdapterStateImpl(eventService, "test-adapter", "test-protocol");
        final TestWritingAdapter adapter = new TestWritingAdapter(true, adapterState);

        final ProtocolAdapterWrapper wrapper = new ProtocolAdapterWrapper(
                mock(),
                protocolAdapterWritingService,
                protocolAdapterPollingService,
                mock(),
                adapter,
                mock(),
                mock(),
                adapterState,
                northboundConsumerFactory,
                tagManager);

        // Start the adapter first
        wrapper.startAsync(true, moduleServices).get(5, TimeUnit.SECONDS);
        adapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);

        // Now stop it
        final CompletableFuture<Void> stopFuture = wrapper.stopAsync(false);

        // Try to change connection status during shutdown (simulating async cleanup)
        final boolean changed = adapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.ERROR);

        stopFuture.get(5, TimeUnit.SECONDS);

        // Status change should have been prevented
        assertThat(changed).isFalse();
        assertThat(wrapper.getRuntimeStatus()).isEqualTo(ProtocolAdapterState.RuntimeStatus.STOPPED);
    }

    @Test
    @Timeout(10)
    void test_concurrentStopAndStatusChange_noRaceCondition() throws Exception {
        final int numIterations = 20;
        final ExecutorService executor = Executors.newFixedThreadPool(10);
        final AtomicInteger failedAssertions = new AtomicInteger(0);

        for (int i = 0; i < numIterations; i++) {
            final ProtocolAdapterStateImpl adapterState =
                    new ProtocolAdapterStateImpl(eventService, "test-adapter-" + i, "test-protocol");
            final TestWritingAdapter adapter = new TestWritingAdapter(true, adapterState);

            final ProtocolAdapterWrapper wrapper = new ProtocolAdapterWrapper(
                    mock(),
                    protocolAdapterWritingService,
                    protocolAdapterPollingService,
                    mock(),
                    adapter,
                    mock(),
                    mock(),
                    adapterState,
                    northboundConsumerFactory,
                    tagManager);

            // Start the adapter
            wrapper.startAsync(true, moduleServices).get(5, TimeUnit.SECONDS);
            adapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);

            final CountDownLatch bothStarted = new CountDownLatch(2);
            final AtomicBoolean statusChangeAttemptedDuringShutdown = new AtomicBoolean(false);

            // Thread 1: Stop the adapter
            final CompletableFuture<Void> stopFuture = CompletableFuture.runAsync(
                    () -> {
                        try {
                            bothStarted.countDown();
                            bothStarted.await(2, TimeUnit.SECONDS);
                            wrapper.stopAsync(false).get(5, TimeUnit.SECONDS);
                        } catch (final Exception e) {
                            throw new RuntimeException(e);
                        }
                    },
                    executor);

            // Thread 2: Try to change status during shutdown
            final CompletableFuture<Void> statusChangeFuture = CompletableFuture.runAsync(
                    () -> {
                        try {
                            bothStarted.countDown();
                            bothStarted.await(2, TimeUnit.SECONDS);
                            // Small delay to increase chance of hitting the race window
                            Thread.sleep(10);
                            final boolean changed =
                                    adapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.ERROR);
                            if (!changed) {
                                statusChangeAttemptedDuringShutdown.set(true);
                            }
                        } catch (final Exception e) {
                            throw new RuntimeException(e);
                        }
                    },
                    executor);

            CompletableFuture.allOf(stopFuture, statusChangeFuture).get(5, TimeUnit.SECONDS);

            // Verify the adapter is in a valid state
            assertThat(wrapper.getRuntimeStatus()).isEqualTo(ProtocolAdapterState.RuntimeStatus.STOPPED);
        }

        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        assertThat(failedAssertions.get()).isEqualTo(0);
    }

    @Test
    @Timeout(10)
    void test_listenerNotCalledDuringShutdown() throws Exception {
        final ProtocolAdapterStateImpl adapterState =
                new ProtocolAdapterStateImpl(eventService, "test-adapter", "test-protocol");
        final TestWritingAdapter adapter = new TestWritingAdapter(true, adapterState);
        final AtomicInteger listenerCallCount = new AtomicInteger(0);

        final ProtocolAdapterWrapper wrapper = new ProtocolAdapterWrapper(
                mock(),
                protocolAdapterWritingService,
                protocolAdapterPollingService,
                mock(),
                adapter,
                mock(),
                mock(),
                adapterState,
                northboundConsumerFactory,
                tagManager);

        // Set up listener
        adapterState.setConnectionStatusListener(status -> listenerCallCount.incrementAndGet());

        // Start the adapter
        wrapper.startAsync(true, moduleServices).get(5, TimeUnit.SECONDS);
        adapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);

        final int callCountBeforeStop = listenerCallCount.get();

        // Stop the adapter
        wrapper.stopAsync(false).get(5, TimeUnit.SECONDS);

        // Try to trigger listener after stop
        adapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.ERROR);

        // Listener should not have been called after stop
        assertThat(listenerCallCount.get()).isEqualTo(callCountBeforeStop);
    }

    @Test
    @Timeout(10)
    void test_startAfterStop_clearsShutdownFlag() throws Exception {
        final ProtocolAdapterStateImpl adapterState =
                new ProtocolAdapterStateImpl(eventService, "test-adapter", "test-protocol");
        final TestWritingAdapter adapter = new TestWritingAdapter(true, adapterState);

        final ProtocolAdapterWrapper wrapper = new ProtocolAdapterWrapper(
                mock(),
                protocolAdapterWritingService,
                protocolAdapterPollingService,
                mock(),
                adapter,
                mock(),
                mock(),
                adapterState,
                northboundConsumerFactory,
                tagManager);

        // Start, stop, then start again
        wrapper.startAsync(true, moduleServices).get(5, TimeUnit.SECONDS);
        adapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);
        wrapper.stopAsync(false).get(5, TimeUnit.SECONDS);

        // After stop, status changes should be blocked
        boolean changed = adapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.ERROR);
        assertThat(changed).isFalse();

        // Start again
        wrapper.startAsync(true, moduleServices).get(5, TimeUnit.SECONDS);

        // Now status changes should work again (change from CONNECTED to ERROR should succeed)
        changed = adapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.ERROR);
        assertThat(changed).isTrue();
        assertThat(adapterState.getConnectionStatus()).isEqualTo(ProtocolAdapterState.ConnectionStatus.ERROR);
    }

    @Test
    @Timeout(10)
    void test_failedStart_marksShutdownDuringCleanup() throws Exception {
        final ProtocolAdapterStateImpl adapterState =
                new ProtocolAdapterStateImpl(eventService, "test-adapter", "test-protocol");
        final TestWritingAdapter adapter = new TestWritingAdapter(false, adapterState); // Will fail

        final ProtocolAdapterWrapper wrapper = new ProtocolAdapterWrapper(
                mock(),
                protocolAdapterWritingService,
                protocolAdapterPollingService,
                mock(),
                adapter,
                mock(),
                mock(),
                adapterState,
                northboundConsumerFactory,
                tagManager);

        // Try to start (will fail)
        wrapper.startAsync(true, moduleServices).get(5, TimeUnit.SECONDS);

        // After failed start, status changes should be blocked during cleanup
        final boolean changed = adapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);
        assertThat(changed).isFalse();
        assertThat(wrapper.getRuntimeStatus()).isEqualTo(ProtocolAdapterState.RuntimeStatus.STOPPED);
    }

    // Test adapter implementations
    static class TestWritingProtocolAdapterInformation implements ProtocolAdapterInformation {
        @Override
        public @NotNull String getProtocolName() {
            return "Test Writing Protocol";
        }

        @Override
        public @NotNull String getProtocolId() {
            return "test-writing-protocol";
        }

        @Override
        public @NotNull String getDisplayName() {
            return "";
        }

        @Override
        public @NotNull String getDescription() {
            return "";
        }

        @Override
        public @NotNull String getUrl() {
            return "";
        }

        @Override
        public @NotNull String getVersion() {
            return "";
        }

        @Override
        public @NotNull String getLogoUrl() {
            return "";
        }

        @Override
        public @NotNull String getAuthor() {
            return "";
        }

        @Override
        public @Nullable ProtocolAdapterCategory getCategory() {
            return null;
        }

        @Override
        public @Nullable List<ProtocolAdapterTag> getTags() {
            return List.of();
        }

        @Override
        public @NotNull Class<? extends Tag> tagConfigurationClass() {
            return null;
        }

        @Override
        public @NotNull Class<? extends ProtocolSpecificAdapterConfig> configurationClassNorthbound() {
            return null;
        }

        @Override
        public @NotNull Class<? extends ProtocolSpecificAdapterConfig> configurationClassNorthAndSouthbound() {
            return null;
        }

        @Override
        public int getCurrentConfigVersion() {
            return 1;
        }
    }

    static class TestWritingAdapter implements WritingProtocolAdapter {
        final boolean success;
        final @NotNull ProtocolAdapterState adapterState;

        TestWritingAdapter(final boolean success, final @NotNull ProtocolAdapterState adapterState) {
            this.success = success;
            this.adapterState = adapterState;
        }

        @Override
        public void write(final @NotNull WritingInput writingInput, final @NotNull WritingOutput writingOutput) {}

        @Override
        public @NotNull Class<? extends WritingPayload> getMqttPayloadClass() {
            return null;
        }

        @Override
        public @NotNull String getId() {
            return "test-writing";
        }

        @Override
        public void start(
                final @NotNull ProtocolAdapterStartInput input, final @NotNull ProtocolAdapterStartOutput output) {
            if (success) {
                adapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);
                output.startedSuccessfully();
            } else {
                output.failStart(new RuntimeException("failed"), "could not start");
            }
        }

        @Override
        public void stop(
                final @NotNull ProtocolAdapterStopInput input, final @NotNull ProtocolAdapterStopOutput output) {
            if (success) {
                adapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
                output.stoppedSuccessfully();
            } else {
                output.failStop(new RuntimeException("failed"), "could not stop");
            }
        }

        @Override
        public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
            return new TestWritingProtocolAdapterInformation();
        }
    }
}
