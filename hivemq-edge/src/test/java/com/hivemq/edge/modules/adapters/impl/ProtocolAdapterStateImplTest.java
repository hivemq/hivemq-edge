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
package com.hivemq.edge.modules.adapters.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.EventBuilder;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.edge.modules.api.events.model.EventBuilderImpl;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProtocolAdapterStateImplTest {

    private @NotNull ProtocolAdapterStateImpl adapterState;

    @BeforeEach
    void setUp() {
        final EventService eventService = mock();
        final EventBuilder eventBuilder = new EventBuilderImpl(mock());
        when(eventService.createAdapterEvent(anyString(), anyString())).thenReturn(eventBuilder);
        adapterState = new ProtocolAdapterStateImpl(eventService, "test-adapter", "test-protocol");
    }

    @Test
    void test_setConnectionStatus_normalOperation_statusChanged() {
        final boolean changed = adapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);
        assertThat(changed).isTrue();
        assertThat(adapterState.getConnectionStatus()).isEqualTo(ProtocolAdapterState.ConnectionStatus.CONNECTED);
    }

    @Test
    void test_setConnectionStatus_afterMarkShuttingDown_statusNotChanged() {
        adapterState.markShuttingDown();
        final boolean changed = adapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);
        assertThat(changed).isFalse();
        assertThat(adapterState.getConnectionStatus()).isEqualTo(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
    }

    @Test
    void test_setConnectionStatus_listenerCalled_duringNormalOperation() {
        final AtomicInteger callCount = new AtomicInteger(0);
        final AtomicBoolean receivedConnected = new AtomicBoolean(false);
        adapterState.setConnectionStatusListener(status -> {
            callCount.incrementAndGet();
            if (status == ProtocolAdapterState.ConnectionStatus.CONNECTED) {
                receivedConnected.set(true);
            }
        });

        // First call from setConnectionStatusListener with initial status (DISCONNECTED)
        assertThat(callCount.get()).isEqualTo(1);

        adapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);

        // Second call from setConnectionStatus
        assertThat(callCount.get()).isEqualTo(2);
        assertThat(receivedConnected.get()).isTrue();
    }

    @Test
    void test_setConnectionStatus_listenerNotCalled_afterMarkShuttingDown() {
        final AtomicInteger callCount = new AtomicInteger(0);

        adapterState.setConnectionStatusListener(status -> callCount.incrementAndGet());

        // First call from setConnectionStatusListener with initial status
        assertThat(callCount.get()).isEqualTo(1);

        adapterState.markShuttingDown();

        adapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);

        // No additional calls after marking as shutting down
        assertThat(callCount.get()).isEqualTo(1);
    }

    @Test
    void test_clearShuttingDown_allowsStateChangesAgain() {
        adapterState.markShuttingDown();

        // First attempt should fail
        boolean changed = adapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);
        assertThat(changed).isFalse();

        adapterState.clearShuttingDown();

        // Second attempt should succeed
        changed = adapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);
        assertThat(changed).isTrue();
        assertThat(adapterState.getConnectionStatus()).isEqualTo(ProtocolAdapterState.ConnectionStatus.CONNECTED);
    }

    @Test
    void test_markShuttingDown_clearsListener() {
        final AtomicInteger callCount = new AtomicInteger(0);

        adapterState.setConnectionStatusListener(status -> callCount.incrementAndGet());
        assertThat(callCount.get()).isEqualTo(1);

        adapterState.markShuttingDown();

        // Try to change status - listener should not be called
        adapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);
        assertThat(callCount.get()).isEqualTo(1);

        // Set new listener after marking as shutting down
        adapterState.setConnectionStatusListener(status -> callCount.addAndGet(10));
        // New listener should be called immediately with current status
        assertThat(callCount.get()).isEqualTo(11);

        // But subsequent status changes should still be blocked
        adapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.ERROR);
        assertThat(callCount.get()).isEqualTo(11);
    }

    @Test
    void test_concurrentStateChanges_duringShutdown_areThreadSafe() throws InterruptedException {
        final int numThreads = 50;
        try (final ExecutorService executor = Executors.newFixedThreadPool(numThreads)) {
            final CountDownLatch startLatch = new CountDownLatch(1);
            final CountDownLatch doneLatch = new CountDownLatch(numThreads);
            final AtomicInteger successfulChanges = new AtomicInteger(0);

            // Start threads that will try to change status
            for (int i = 0; i < numThreads; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        startLatch.await(); // Wait for all threads to be ready

                        // Half the threads try to mark shutting down
                        if (threadId % 2 == 0) {
                            adapterState.markShuttingDown();
                        } else {
                            // Other half try to change status
                            final boolean changed = adapterState.setConnectionStatus(
                                    threadId % 4 == 1
                                            ? ProtocolAdapterState.ConnectionStatus.CONNECTED
                                            : ProtocolAdapterState.ConnectionStatus.ERROR);
                            if (changed) {
                                successfulChanges.incrementAndGet();
                            }
                        }
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown(); // Start all threads
            assertThat(doneLatch.await(10, TimeUnit.SECONDS)).isTrue();
            executor.shutdown();
        }

        // After shutdown is marked, state should remain consistent
        assertThat(adapterState.getConnectionStatus())
                .isIn(
                        ProtocolAdapterState.ConnectionStatus.DISCONNECTED,
                        ProtocolAdapterState.ConnectionStatus.CONNECTED,
                        ProtocolAdapterState.ConnectionStatus.ERROR);

        // Verify no more changes are possible after shutdown
        adapterState.markShuttingDown();
        final boolean changed = adapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);
        assertThat(changed).isFalse();
    }

    @Test
    void test_raceCondition_shutdownDuringListenerCallback() throws InterruptedException {
        final CountDownLatch listenerStarted = new CountDownLatch(1);
        final CountDownLatch shutdownCanProceed = new CountDownLatch(1);
        final AtomicBoolean listenerCompleted = new AtomicBoolean(false);
        final AtomicBoolean shutdownCompleted = new AtomicBoolean(false);

        // Set up a slow listener that will be interrupted by shutdown
        adapterState.setConnectionStatusListener(status -> {
            if (status == ProtocolAdapterState.ConnectionStatus.CONNECTED) {
                listenerStarted.countDown();
                try {
                    // Simulate slow processing
                    shutdownCanProceed.await(5, TimeUnit.SECONDS);
                    listenerCompleted.set(true);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        // Thread 1: Trigger status change
        final Thread changeThread =
                new Thread(() -> adapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED));

        // Thread 2: Mark as shutting down
        final Thread shutdownThread = new Thread(() -> {
            try {
                listenerStarted.await(); // Wait for listener to start
                adapterState.markShuttingDown();
                shutdownCompleted.set(true);
                shutdownCanProceed.countDown();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        changeThread.start();
        shutdownThread.start();

        changeThread.join(5000);
        shutdownThread.join(5000);

        // Shutdown should complete successfully
        assertThat(shutdownCompleted.get()).isTrue();

        // After shutdown, no more status changes should work
        final boolean changed = adapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.ERROR);
        assertThat(changed).isFalse();
    }

    @Test
    void test_errorConnectionStatus_respectsShutdownFlag() {
        adapterState.markShuttingDown();

        adapterState.setErrorConnectionStatus(new RuntimeException("test"), "test error");

        // Status should not change during shutdown
        assertThat(adapterState.getConnectionStatus()).isEqualTo(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
        assertThat(adapterState.getLastErrorMessage()).isEqualTo("test error");
    }

    @Test
    void test_setRuntimeStatus_notAffectedByShutdownFlag() {
        adapterState.markShuttingDown();

        adapterState.setRuntimeStatus(ProtocolAdapterState.RuntimeStatus.STARTED);

        // Runtime status should still be changeable
        assertThat(adapterState.getRuntimeStatus()).isEqualTo(ProtocolAdapterState.RuntimeStatus.STARTED);
    }
}
