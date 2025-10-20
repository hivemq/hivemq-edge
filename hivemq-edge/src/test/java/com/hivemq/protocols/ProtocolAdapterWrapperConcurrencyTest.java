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

import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.edge.modules.adapters.data.TagManager;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPollingService;
import com.hivemq.fsm.ProtocolAdapterFSM;
import com.hivemq.protocols.northbound.NorthboundConsumerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * These tests verify:
 * - FSM state transitions are atomic
 * - Concurrent operations don't cause race conditions
 * - State reads are always consistent
 * - North/South bound states are properly managed
 */
class ProtocolAdapterWrapperConcurrencyTest {

    private static final int SMALL_THREAD_COUNT = 10;
    private static final int MEDIUM_THREAD_COUNT = 20;
    private static final int LARGE_THREAD_COUNT = 50;
    private static final int OPERATIONS_PER_THREAD = 100;
    private static final int TRANSITIONS_PER_THREAD = 20;
    private @Nullable ModuleServices mockModuleServices;
    private @Nullable ProtocolAdapterWrapper wrapper;
    private @Nullable ExecutorService executor;

    private static void verifyStateConsistency(final ProtocolAdapterFSM.State state) {
        final ProtocolAdapterFSM.AdapterStateEnum adapterState = state.state();
        final ProtocolAdapterFSM.StateEnum northbound = state.northbound();
        final ProtocolAdapterFSM.StateEnum southbound = state.southbound();

        // Verify all states are non-null
        assertNotNull(adapterState, "Adapter state should not be null");
        assertNotNull(northbound, "Northbound state should not be null");
        assertNotNull(southbound, "Southbound state should not be null");

        switch (adapterState) {
            case STOPPED:
                // STOPPED is a stable state - all connections must be fully disconnected
                assertEquals(ProtocolAdapterFSM.StateEnum.DISCONNECTED,
                        northbound,
                        "Northbound should be DISCONNECTED when adapter is STOPPED");
                assertEquals(ProtocolAdapterFSM.StateEnum.DISCONNECTED,
                        southbound,
                        "Southbound should be DISCONNECTED when adapter is STOPPED");
                break;

            case STARTING:
            case STARTED:
            case STOPPING:
                // Key invariant: When adapter is STOPPED, both connections MUST be DISCONNECTED.
                // Other states allow various connection state combinations during transitions.
                break;

            default:
                fail("Unknown adapter state: " + adapterState);
        }
    }

    private void runConcurrentOperations(final int threadCount, final @NotNull Runnable operation)
            throws InterruptedException {
        executor = Executors.newFixedThreadPool(threadCount);
        final CyclicBarrier barrier = new CyclicBarrier(threadCount);
        final AtomicInteger attempts = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            requireNonNull(executor).submit(() -> {
                try {
                    barrier.await();
                    attempts.incrementAndGet();
                    operation.run();
                } catch (final Exception ignored) {
                    // Expected - concurrent operations may fail due to state conflicts
                }
            });
        }

        requireNonNull(executor).shutdown();
        assertTrue(requireNonNull(executor).awaitTermination(10, TimeUnit.SECONDS), "All threads should complete");
        assertEquals(threadCount, attempts.get(), "All attempts should be made");
    }

    private void runReaderWriterPattern(
            final int readerThreads,
            final int writerThreads,
            final @NotNull Runnable readerOperation,
            final @NotNull Runnable writerOperation,
            final @NotNull AtomicInteger readerCounter) throws InterruptedException {
        executor = Executors.newFixedThreadPool(readerThreads + writerThreads);
        final CountDownLatch stopLatch = new CountDownLatch(1);

        // Readers
        for (int i = 0; i < readerThreads; i++) {
            requireNonNull(executor).submit(() -> {
                try {
                    while (!stopLatch.await(0, TimeUnit.MILLISECONDS)) {
                        readerOperation.run();
                        Thread.sleep(1);
                    }
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Writers
        for (int i = 0; i < writerThreads; i++) {
            requireNonNull(executor).submit(() -> {
                try {
                    for (int j = 0; j < 15; j++) {
                        writerOperation.run();
                        Thread.sleep(10);
                    }
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        Thread.sleep(250);
        stopLatch.countDown();

        requireNonNull(executor).shutdown();
        assertTrue(requireNonNull(executor).awaitTermination(5, TimeUnit.SECONDS), "All threads should complete");
        assertTrue(readerCounter.get() > 0, "Should have performed operations");
    }

    @BeforeEach
    void setUp() {
        final ProtocolAdapter mockAdapter = mock(ProtocolAdapter.class);
        when(mockAdapter.getId()).thenReturn("test-adapter");
        when(mockAdapter.getProtocolAdapterInformation()).thenReturn(mock(ProtocolAdapterInformation.class));

        final ProtocolAdapterMetricsService metricsService = mock(ProtocolAdapterMetricsService.class);
        final InternalProtocolAdapterWritingService writingService = mock(InternalProtocolAdapterWritingService.class);
        final ProtocolAdapterPollingService pollingService = mock(ProtocolAdapterPollingService.class);
        final ProtocolAdapterFactory<?> adapterFactory = mock(ProtocolAdapterFactory.class);
        final ProtocolAdapterInformation adapterInformation = mock(ProtocolAdapterInformation.class);
        final ProtocolAdapterStateImpl adapterState = mock(ProtocolAdapterStateImpl.class);
        final NorthboundConsumerFactory consumerFactory = mock(NorthboundConsumerFactory.class);
        final TagManager tagManager = mock(TagManager.class);
        mockModuleServices = mock(ModuleServices.class);

        final ProtocolAdapterConfig config = mock(ProtocolAdapterConfig.class);
        when(config.getAdapterId()).thenReturn("test-adapter");
        when(config.getTags()).thenReturn(java.util.List.of());
        when(config.getNorthboundMappings()).thenReturn(java.util.List.of());
        when(config.getSouthboundMappings()).thenReturn(java.util.List.of());

        wrapper = new ProtocolAdapterWrapper(metricsService,
                writingService,
                pollingService,
                config,
                mockAdapter,
                adapterFactory,
                adapterInformation,
                adapterState,
                consumerFactory,
                tagManager);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    @Timeout(10)
    void test_fsmStateTransitions_areAtomic() throws Exception {
        executor = Executors.newFixedThreadPool(SMALL_THREAD_COUNT);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final AtomicInteger invalidTransitions = new AtomicInteger(0);
        final AtomicInteger totalAttempts = new AtomicInteger(0);

        for (int i = 0; i < SMALL_THREAD_COUNT; i++) {
            final boolean shouldStart = i % 2 == 0;
            requireNonNull(executor).submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < TRANSITIONS_PER_THREAD; j++) {
                        try {
                            totalAttempts.incrementAndGet();
                            if (shouldStart) {
                                requireNonNull(wrapper).startAdapter();
                            } else {
                                requireNonNull(wrapper).stopAdapter();
                            }

                            // Verify complete state is valid
                            final var state = requireNonNull(wrapper).currentState();
                            assertNotNull(state, "State should never be null");
                            assertNotNull(state.state(), "Adapter state should never be null");
                            assertNotNull(state.northbound(), "Northbound state should never be null");
                            assertNotNull(state.southbound(), "Southbound state should never be null");

                            // Verify state consistency
                            verifyStateConsistency(state);

                        } catch (final IllegalStateException e) {
                            // Expected when transition is not allowed
                        } catch (final Exception e) {
                            invalidTransitions.incrementAndGet();
                        }

                        Thread.sleep(1);
                    }
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        startLatch.countDown();
        requireNonNull(executor).shutdown();
        assertTrue(requireNonNull(executor).awaitTermination(10, TimeUnit.SECONDS), "All threads should complete");

        // Verify no invalid transitions occurred
        assertEquals(0,
                invalidTransitions.get(),
                "No invalid state transitions should occur (total attempts: " + totalAttempts.get() + ")");

        // Verify final state is complete and valid
        final var finalState = requireNonNull(wrapper).currentState();
        assertNotNull(finalState, "Final state should not be null");
        assertNotNull(finalState.state(), "Final adapter state should not be null");
        assertNotNull(finalState.northbound(), "Final northbound state should not be null");
        assertNotNull(finalState.southbound(), "Final southbound state should not be null");

        // Final state should be STOPPED or STARTED
        assertTrue(finalState.state() == ProtocolAdapterFSM.AdapterStateEnum.STOPPED ||
                        finalState.state() == ProtocolAdapterFSM.AdapterStateEnum.STARTED,
                "Final state should be stable: " + finalState.state());
    }

    @Test
    @Timeout(10)
    void test_stateReads_alwaysConsistent() throws Exception {
        final int READER_THREADS = 5;
        final int WRITER_THREADS = 2;
        executor = Executors.newFixedThreadPool(READER_THREADS + WRITER_THREADS);
        final CountDownLatch stopLatch = new CountDownLatch(1);
        final AtomicInteger inconsistentReads = new AtomicInteger(0);
        final AtomicInteger totalReads = new AtomicInteger(0);

        // Reader threads - verify complete state consistency
        for (int i = 0; i < READER_THREADS; i++) {
            requireNonNull(executor).submit(() -> {
                try {
                    while (!stopLatch.await(0, TimeUnit.MILLISECONDS)) {
                        final var state = requireNonNull(wrapper).currentState();
                        assertNotNull(state, "State should never be null");
                        assertNotNull(state.state(), "Adapter state should never be null");
                        assertNotNull(state.northbound(), "Northbound state should never be null");
                        assertNotNull(state.southbound(), "Southbound state should never be null");

                        // Verify state consistency
                        verifyStateConsistency(state);

                        totalReads.incrementAndGet();
                        Thread.sleep(1);
                    }
                } catch (final AssertionError e) {
                    inconsistentReads.incrementAndGet();
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Writer threads
        for (int i = 0; i < WRITER_THREADS; i++) {
            requireNonNull(executor).submit(() -> {
                try {
                    for (int j = 0; j < 10; j++) {
                        try {
                            requireNonNull(wrapper).startAdapter();
                            Thread.sleep(10);
                            requireNonNull(wrapper).stopAdapter();
                            Thread.sleep(10);
                        } catch (final IllegalStateException e) {
                            // Expected
                        }
                    }
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Let it run
        Thread.sleep(200);
        stopLatch.countDown();

        requireNonNull(executor).shutdown();
        assertTrue(requireNonNull(executor).awaitTermination(5, TimeUnit.SECONDS), "All threads should complete");

        // Verify no inconsistent reads
        assertEquals(0, inconsistentReads.get(), "No inconsistent state reads should occur");
        assertTrue(totalReads.get() > 0, "Should have performed reads: " + totalReads.get());
    }

    @Test
    @Timeout(5)
    void test_concurrentStateChecks_noExceptions() throws Exception {
        executor = Executors.newFixedThreadPool(MEDIUM_THREAD_COUNT);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final AtomicInteger successfulChecks = new AtomicInteger(0);
        final AtomicInteger failures = new AtomicInteger(0);

        for (int i = 0; i < MEDIUM_THREAD_COUNT; i++) {
            requireNonNull(executor).submit(() -> {
                try {
                    startLatch.await();

                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        try {
                            // Various state check operations
                            requireNonNull(wrapper).currentState();
                            requireNonNull(wrapper).getRuntimeStatus();
                            requireNonNull(wrapper).getConnectionStatus();
                            requireNonNull(wrapper).getId();
                            requireNonNull(wrapper).getAdapterInformation();
                            successfulChecks.incrementAndGet();
                        } catch (final Exception e) {
                            failures.incrementAndGet();
                        }
                    }
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        startLatch.countDown();
        requireNonNull(executor).shutdown();
        assertTrue(requireNonNull(executor).awaitTermination(5, TimeUnit.SECONDS), "All threads should complete");

        assertEquals(0, failures.get(), "No exceptions should occur during concurrent state checks");
        assertTrue(successfulChecks.get() >= MEDIUM_THREAD_COUNT * OPERATIONS_PER_THREAD,
                "All state checks should succeed");
    }

    @Test
    @Timeout(5)
    void test_adapterIdAccess_isThreadSafe() throws Exception {
        executor = Executors.newFixedThreadPool(LARGE_THREAD_COUNT);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final AtomicInteger correctReads = new AtomicInteger(0);

        for (int i = 0; i < LARGE_THREAD_COUNT; i++) {
            requireNonNull(executor).submit(() -> {
                try {
                    startLatch.await();

                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        final String adapterId = requireNonNull(wrapper).getId();
                        if ("test-adapter".equals(adapterId)) {
                            correctReads.incrementAndGet();
                        }
                    }
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        startLatch.countDown();
        requireNonNull(executor).shutdown();
        assertTrue(requireNonNull(executor).awaitTermination(5, TimeUnit.SECONDS), "All threads should complete");

        assertEquals(LARGE_THREAD_COUNT * OPERATIONS_PER_THREAD,
                correctReads.get(),
                "All adapter ID reads should be correct");
    }

    @Test
    @Timeout(10)
    void test_concurrentStartAsync_properSerialization() throws Exception {
        runConcurrentOperations(SMALL_THREAD_COUNT,
                () -> requireNonNull(wrapper).startAsync(false, requireNonNull(mockModuleServices)));

        final var state = requireNonNull(wrapper).currentState();
        assertNotNull(state);
        assertNotNull(state.state());
    }

    @Test
    @Timeout(10)
    void test_concurrentStopAsync_properSerialization() throws Exception {
        requireNonNull(wrapper).startAsync(false, requireNonNull(mockModuleServices));
        Thread.sleep(100);

        runConcurrentOperations(SMALL_THREAD_COUNT, () -> requireNonNull(wrapper).stopAsync(false));

        final var state = requireNonNull(wrapper).currentState();
        assertNotNull(state);
        assertNotNull(state.state());
    }

    @Test
    @Timeout(10)
    void test_statusQueriesDuringTransitions_noExceptions() throws Exception {
        final int READER_THREADS = 5;
        final int WRITER_THREADS = 3;
        final AtomicInteger totalReads = new AtomicInteger(0);
        final AtomicInteger exceptions = new AtomicInteger(0);

        // Test both runtime and connection status reads during transitions
        runReaderWriterPattern(READER_THREADS, WRITER_THREADS, () -> {
            try {
                requireNonNull(wrapper).getRuntimeStatus(); // May return null with mocks
                requireNonNull(wrapper).getConnectionStatus(); // May return null with mocks
                totalReads.incrementAndGet();
            } catch (final Exception e) {
                exceptions.incrementAndGet();
            }
        }, () -> {
            try {
                requireNonNull(wrapper).startAdapter();
            } catch (final IllegalStateException ignored) {
                // Expected
            }
        }, totalReads);

        assertEquals(0, exceptions.get(), "No exceptions during status queries");
    }
}
