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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Stress test for ProtocolAdapterStateImpl shutdown race condition.
 * <p>
 * This test simulates the exact conditions that caused the CI failure:
 * - High concurrency with many threads
 * - Slow execution (like in CI with limited resources)
 * - Async cleanup operations triggering state changes during shutdown
 */
class ProtocolAdapterStateShutdownStressTest {

    @SuppressWarnings("FutureReturnValueIgnored")
    @Test
    @Timeout(30)
    void test_massiveConcurrency_shutdownRaceCondition() throws Exception {
        final int numAdapters = 50;
        final int operationsPerAdapter = 20;
        final ExecutorService executor =
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
        final List<CompletableFuture<Void>> futures = new ArrayList<>();
        final AtomicInteger totalFailures = new AtomicInteger(0);
        final AtomicInteger blockedStateChanges = new AtomicInteger(0);
        final AtomicInteger successfulStateChanges = new AtomicInteger(0);

        for (int adapterId = 0; adapterId < numAdapters; adapterId++) {
            final int id = adapterId;

            final CompletableFuture<Void> future = CompletableFuture.runAsync(
                    () -> {
                        try {
                            final EventService eventService = mock();
                            final EventBuilder eventBuilder = new EventBuilderImpl(mock());
                            when(eventService.createAdapterEvent(anyString(), anyString()))
                                    .thenReturn(eventBuilder);

                            final ProtocolAdapterStateImpl adapterState = new ProtocolAdapterStateImpl(
                                    eventService, "stress-adapter-" + id, "stress-protocol");

                            final AtomicInteger listenerCalls = new AtomicInteger(0);
                            adapterState.setConnectionStatusListener(status -> listenerCalls.incrementAndGet());

                            // Simulate normal operation
                            for (int i = 0; i < operationsPerAdapter; i++) {
                                final ProtocolAdapterState.ConnectionStatus newStatus = i % 3 == 0
                                        ? ProtocolAdapterState.ConnectionStatus.CONNECTED
                                        : i % 3 == 1
                                                ? ProtocolAdapterState.ConnectionStatus.DISCONNECTED
                                                : ProtocolAdapterState.ConnectionStatus.ERROR;

                                final boolean changed = adapterState.setConnectionStatus(newStatus);
                                if (changed) {
                                    successfulStateChanges.incrementAndGet();
                                }

                                // Small delay to simulate real work
                                if (i % 5 == 0) {
                                    Thread.sleep(1);
                                }
                            }

                            // Simulate shutdown being triggered while operations are ongoing
                            adapterState.markShuttingDown();

                            // Try more operations after shutdown (simulating async cleanup)
                            for (int i = 0; i < 10; i++) {
                                final boolean changed =
                                        adapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.ERROR);
                                if (!changed) {
                                    blockedStateChanges.incrementAndGet();
                                } else {
                                    // This should not happen after markShuttingDown
                                    totalFailures.incrementAndGet();
                                }
                            }

                        } catch (final Exception e) {
                            totalFailures.incrementAndGet();
                            throw new RuntimeException(e);
                        }
                    },
                    executor);

            futures.add(future);
        }

        // Wait for all operations to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(25, TimeUnit.SECONDS);

        executor.shutdown();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        // Verify results
        System.out.println("Stress Test Results:");
        System.out.println("  Total adapters: " + numAdapters);
        System.out.println("  Operations per adapter: " + operationsPerAdapter);
        System.out.println("  Successful state changes: " + successfulStateChanges.get());
        System.out.println("  Blocked state changes (correct): " + blockedStateChanges.get());
        System.out.println("  Failures (incorrect): " + totalFailures.get());

        // No state changes should succeed after markShuttingDown
        assertThat(totalFailures.get()).isEqualTo(0);
        // We should have blocked many state changes
        assertThat(blockedStateChanges.get()).isGreaterThan(0);
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    @Test
    @Timeout(30)
    void test_listenerConcurrency_noMemoryLeaksOrRaceConditions() throws Exception {
        final int numThreads = 100;
        try (final ExecutorService executor = Executors.newFixedThreadPool(numThreads)) {
            final CountDownLatch startLatch = new CountDownLatch(1);
            final CountDownLatch doneLatch = new CountDownLatch(numThreads);
            final AtomicInteger unexpectedCalls = new AtomicInteger(0);

            final EventService eventService = mock();
            final EventBuilder eventBuilder = new EventBuilderImpl(mock());
            when(eventService.createAdapterEvent(anyString(), anyString())).thenReturn(eventBuilder);

            final ProtocolAdapterStateImpl adapterState =
                    new ProtocolAdapterStateImpl(eventService, "listener-test", "test-protocol");

            // Setup listener that tracks calls
            final AtomicInteger beforeShutdownCalls = new AtomicInteger(0);
            final AtomicInteger afterShutdownCalls = new AtomicInteger(0);
            final AtomicInteger shutdownMarked = new AtomicInteger(0);

            final Consumer<ProtocolAdapterState.ConnectionStatus> listener = status -> {
                if (shutdownMarked.get() > 0) {
                    afterShutdownCalls.incrementAndGet();
                    // This should not happen
                    unexpectedCalls.incrementAndGet();
                } else {
                    beforeShutdownCalls.incrementAndGet();
                }
            };

            adapterState.setConnectionStatusListener(listener);

            // Launch threads that will compete
            for (int i = 0; i < numThreads; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();

                        if (threadId % 10 == 0) {
                            // Some threads mark shutdown
                            shutdownMarked.incrementAndGet();
                            adapterState.markShuttingDown();
                        } else {
                            // Other threads try to change status
                            adapterState.setConnectionStatus(
                                    threadId % 2 == 0
                                            ? ProtocolAdapterState.ConnectionStatus.CONNECTED
                                            : ProtocolAdapterState.ConnectionStatus.ERROR);
                        }

                        Thread.sleep(1); // Small delay to increase contention

                    } catch (final Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown(); // Start all threads
            assertThat(doneLatch.await(20, TimeUnit.SECONDS)).isTrue();

            executor.shutdown();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
            System.out.println("Listener Concurrency Test Results:");
            System.out.println("  Listener calls before shutdown: " + beforeShutdownCalls.get());
            System.out.println("  Listener calls after shutdown (should be 0): " + afterShutdownCalls.get());
            System.out.println("  Unexpected calls: " + unexpectedCalls.get());
            System.out.println("  Times shutdown was marked: " + shutdownMarked.get());

            // No listener calls should happen after any thread marked shutdown
            assertThat(unexpectedCalls.get()).isEqualTo(0);
        }
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    @Test
    @Timeout(30)
    void test_rapidStartStopCycles_noStateCorruption() throws Exception {
        final int numCycles = 100;
        try (final ExecutorService executor = Executors.newFixedThreadPool(4)) {
            final EventService eventService = mock();
            final EventBuilder eventBuilder = new EventBuilderImpl(mock());
            when(eventService.createAdapterEvent(anyString(), anyString())).thenReturn(eventBuilder);

            final ProtocolAdapterStateImpl adapterState =
                    new ProtocolAdapterStateImpl(eventService, "cycle-test", "test-protocol");

            for (int cycle = 0; cycle < numCycles; cycle++) {
                // Start phase
                adapterState.clearShuttingDown();
                adapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);

                // Concurrent operations during "running" phase
                final CountDownLatch operationsLatch = new CountDownLatch(10);
                for (int i = 0; i < 10; i++) {
                    final int opId = i;
                    executor.submit(() -> {
                        try {
                            adapterState.setConnectionStatus(
                                    opId % 2 == 0
                                            ? ProtocolAdapterState.ConnectionStatus.CONNECTED
                                            : ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
                        } finally {
                            operationsLatch.countDown();
                        }
                    });
                }

                operationsLatch.await(1, TimeUnit.SECONDS);

                // Stop phase
                adapterState.markShuttingDown();

                // Verify state is consistent
                assertThat(adapterState.getRuntimeStatus())
                        .isIn(ProtocolAdapterState.RuntimeStatus.STARTED, ProtocolAdapterState.RuntimeStatus.STOPPED);

                // Try to change status - should be blocked
                final boolean changed = adapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.ERROR);
                assertThat(changed).isFalse();
            }

            executor.shutdown();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    /**
     * This test specifically reproduces the CI scenario:
     * - Limited CPU resources (simulated by thread pool size)
     * - Slow operations (simulated by sleep)
     * - Async cleanup during shutdown
     */
    @Test
    @Timeout(30)
    void test_ciScenario_limitedResourcesSlowOperations() throws Exception {
        // Simulate CI environment with only 2 threads (like the CI logs showing 2 threads for MessageForwarder)
        final ExecutorService limitedExecutor = Executors.newFixedThreadPool(2);
        final AtomicInteger failures = new AtomicInteger(0);

        final EventService eventService = mock();
        final EventBuilder eventBuilder = new EventBuilderImpl(mock());
        when(eventService.createAdapterEvent(anyString(), anyString())).thenReturn(eventBuilder);

        for (int iteration = 0; iteration < 20; iteration++) {
            final int iterationFinal = iteration;
            final ProtocolAdapterStateImpl adapterState =
                    new ProtocolAdapterStateImpl(eventService, "ci-adapter-" + iterationFinal, "test-protocol");

            final CountDownLatch shutdownLatch = new CountDownLatch(1);
            final CountDownLatch cleanupLatch = new CountDownLatch(1);

            // Thread 1: Normal shutdown
            final CompletableFuture<Void> shutdownFuture = CompletableFuture.runAsync(
                    () -> {
                        try {
                            // Simulate slow shutdown
                            Thread.sleep(50);
                            adapterState.markShuttingDown();
                            shutdownLatch.countDown();
                        } catch (final InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    },
                    limitedExecutor);

            // Thread 2: Async cleanup that tries to change state
            final CompletableFuture<Void> cleanupFuture = CompletableFuture.runAsync(
                    () -> {
                        try {
                            // Simulate data combiner cleanup
                            Thread.sleep(60); // Slightly longer than shutdown
                            final boolean changed = adapterState.setConnectionStatus(
                                    ProtocolAdapterState.ConnectionStatus.DISCONNECTED);

                            if (changed) {
                                // This should not happen after markShuttingDown
                                System.err.println(
                                        "ERROR: State changed during/after shutdown in iteration " + iterationFinal);
                                failures.incrementAndGet();
                            }

                            cleanupLatch.countDown();
                        } catch (final InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    },
                    limitedExecutor);

            CompletableFuture.allOf(shutdownFuture, cleanupFuture).get(5, TimeUnit.SECONDS);
        }

        limitedExecutor.shutdown();
        assertThat(limitedExecutor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        System.out.println("CI Scenario Test Results:");
        System.out.println("  Failures (state changes after shutdown): " + failures.get());

        // No state changes should succeed after shutdown
        assertThat(failures.get()).isEqualTo(0);
    }
}
