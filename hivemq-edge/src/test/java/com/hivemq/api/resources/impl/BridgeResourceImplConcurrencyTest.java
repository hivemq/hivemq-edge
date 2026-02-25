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
package com.hivemq.api.resources.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hivemq.bridge.BridgeService;
import com.hivemq.bridge.config.MqttBridge;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.reader.BridgeExtractor;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.api.model.Bridge;
import com.hivemq.edge.api.model.StatusTransitionCommand;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Concurrency test for BridgeResourceImpl to verify that all CRUD operations are properly synchronized
 * and that race conditions documented in BRIDGE_RACE_CONDITION_FIX.md are prevented.
 * <p>
 * Tests verify that the synchronization fixes using bridgeUpdateLock prevent:
 * 1. Update vs Remove race conditions (lost updates)
 * 2. Update vs Add race conditions (duplicate IDs)
 * 3. Add vs Remove race conditions (stale checks)
 * 4. Remove vs Remove race conditions (double removal)
 * 5. Concurrent reads during modifications (inconsistent snapshots)
 */
class BridgeResourceImplConcurrencyTest {

    private BridgeResourceImpl bridgeResource;

    // Thread-safe bridge storage to simulate the real bridge extractor
    private ConcurrentHashMap<String, MqttBridge> bridgeStore;

    private static @NotNull Bridge createTestBridge(final @NotNull String bridgeId) {
        return new Bridge().id(bridgeId).host("test.example.com").port(1883).clientId(bridgeId + "-client");
    }

    @BeforeEach
    void setUp() {
        final ConfigurationService configurationService = mock(ConfigurationService.class);
        final BridgeService bridgeService = mock(BridgeService.class);
        final SystemInformation systemInformation = mock(SystemInformation.class);
        final BridgeExtractor bridgeExtractor = mock(BridgeExtractor.class);
        bridgeStore = new ConcurrentHashMap<>();

        when(systemInformation.isConfigWriteable()).thenReturn(true);
        when(configurationService.bridgeExtractor()).thenReturn(bridgeExtractor);

        // Mock bridge extractor to use our thread-safe store
        when(bridgeExtractor.getBridges()).thenAnswer(invocation -> new ArrayList<>(bridgeStore.values()));

        doAnswer(invocation -> {
                    final MqttBridge bridge = invocation.getArgument(0);
                    bridgeStore.put(bridge.getId(), bridge);
                    return null;
                })
                .when(bridgeExtractor)
                .addBridge(any(MqttBridge.class));

        doAnswer(invocation -> {
                    final String bridgeId = invocation.getArgument(0);
                    bridgeStore.remove(bridgeId);
                    return null;
                })
                .when(bridgeExtractor)
                .removeBridge(anyString());

        bridgeResource = new BridgeResourceImpl(configurationService, bridgeService, systemInformation);
    }

    /**
     * Test Scenario 1: Update vs Remove race condition
     * Thread 1 updates bridge, Thread 2 removes it concurrently
     * Expected: Operations are serialized, no lost updates
     */
    @Test
    @Timeout(10)
    void test_concurrentUpdateAndRemove_noLostUpdates() throws Exception {
        // Setup initial bridge
        final String bridgeId = "test-bridge";
        final Bridge initialBridge = createTestBridge(bridgeId);
        bridgeResource.addBridge(initialBridge);

        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(2);
        final AtomicInteger updateSuccessCount = new AtomicInteger(0);
        final AtomicInteger removeSuccessCount = new AtomicInteger(0);
        final AtomicInteger notFoundErrors = new AtomicInteger(0);

        try (final ExecutorService executor = Executors.newFixedThreadPool(2)) {
            // Thread 1: Update
            executor.submit(() -> {
                try {
                    startLatch.await();
                    final Bridge updatedBridge = createTestBridge(bridgeId).port(8883);
                    final Response response = bridgeResource.updateBridge(bridgeId, updatedBridge);
                    if (response.getStatus() == 200) {
                        updateSuccessCount.incrementAndGet();
                    } else if (response.getStatus() == 404) {
                        notFoundErrors.incrementAndGet();
                    }
                } catch (final Exception e) {
                    // Ignored for test
                } finally {
                    completionLatch.countDown();
                }
            });

            // Thread 2: Remove
            executor.submit(() -> {
                try {
                    startLatch.await();
                    final Response response = bridgeResource.removeBridge(bridgeId);
                    if (response.getStatus() == 200) {
                        removeSuccessCount.incrementAndGet();
                    } else if (response.getStatus() == 404) {
                        notFoundErrors.incrementAndGet();
                    }
                } catch (final Exception e) {
                    // Ignored for test
                } finally {
                    completionLatch.countDown();
                }
            });

            startLatch.countDown();
            assertTrue(completionLatch.await(5, TimeUnit.SECONDS), "Operations should complete");

            // Verify: Either update then remove succeeded, or remove then update failed with 404
            final int totalSuccesses = updateSuccessCount.get() + removeSuccessCount.get();
            assertTrue(
                    totalSuccesses >= 1 && totalSuccesses <= 2,
                    "At least one operation should succeed: update=" + updateSuccessCount.get()
                            + ", remove="
                            + removeSuccessCount.get());

            // If both succeeded, verify final state is deleted
            if (totalSuccesses == 2) {
                assertTrue(bridgeStore.isEmpty(), "Bridge should be deleted after both operations");
            }
        }
    }

    /**
     * Test Scenario 2: Add vs Remove race condition
     * Thread 1 adds bridge, Thread 2 tries to remove it concurrently
     * Expected: Operations are serialized, deterministic outcome
     */
    @Test
    @Timeout(10)
    void test_concurrentAddAndRemove_deterministicOutcome() throws Exception {
        final String bridgeId = "race-bridge";
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(2);
        final AtomicInteger addSuccessCount = new AtomicInteger(0);
        final AtomicInteger removeSuccessCount = new AtomicInteger(0);
        final AtomicInteger errors = new AtomicInteger(0);

        try (final ExecutorService executor = Executors.newFixedThreadPool(2)) {
            // Thread 1: Add
            executor.submit(() -> {
                try {
                    startLatch.await();
                    final Bridge bridge = createTestBridge(bridgeId);
                    final Response response = bridgeResource.addBridge(bridge);
                    if (response.getStatus() == 200) {
                        addSuccessCount.incrementAndGet();
                    } else {
                        errors.incrementAndGet();
                    }
                } catch (final Exception e) {
                    errors.incrementAndGet();
                } finally {
                    completionLatch.countDown();
                }
            });

            // Thread 2: Remove
            executor.submit(() -> {
                try {
                    startLatch.await();
                    final Response response = bridgeResource.removeBridge(bridgeId);
                    if (response.getStatus() == 200) {
                        removeSuccessCount.incrementAndGet();
                    }
                    // 404 is acceptable here - means add hasn't happened yet
                } catch (final Exception e) {
                    // Ignored for test
                } finally {
                    completionLatch.countDown();
                }
            });

            startLatch.countDown();
            assertTrue(completionLatch.await(5, TimeUnit.SECONDS), "Operations should complete");

            // Verify: Add should always succeed, remove may or may not succeed
            assertEquals(1, addSuccessCount.get(), "Add should succeed exactly once");
            assertTrue(removeSuccessCount.get() <= 1, "Remove should succeed at most once");
        }
    }

    /**
     * Test Scenario 3: Multiple concurrent updates on same bridge
     * Multiple threads try to update the same bridge concurrently
     * Expected: Operations are serialized, no corruption
     */
    @Test
    @Timeout(10)
    void test_multipleConcurrentUpdates_serialized() throws Exception {
        // Setup initial bridge
        final String bridgeId = "multi-update-bridge";
        final Bridge initialBridge = createTestBridge(bridgeId);
        bridgeResource.addBridge(initialBridge);

        final int numThreads = 5;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(numThreads);
        final AtomicInteger successCount = new AtomicInteger(0);

        try (final ExecutorService executor = Executors.newFixedThreadPool(numThreads)) {
            for (int i = 0; i < numThreads; i++) {
                final int port = 2000 + i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        final Bridge updatedBridge = createTestBridge(bridgeId).port(port);
                        final Response response = bridgeResource.updateBridge(bridgeId, updatedBridge);
                        if (response.getStatus() == 200) {
                            successCount.incrementAndGet();
                        }
                    } catch (final Exception e) {
                        // Ignored for test
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(completionLatch.await(5, TimeUnit.SECONDS), "All updates should complete");

            // All updates should succeed (they're serialized, not conflicting)
            assertEquals(numThreads, successCount.get(), "All updates should succeed");

            // Bridge should still exist
            assertEquals(1, bridgeStore.size(), "Bridge should still exist after updates");
        }
    }

    /**
     * Test Scenario 4: Concurrent reads during modifications
     * Multiple threads read while others modify
     * Expected: No exceptions, consistent snapshots
     */
    @Test
    @Timeout(10)
    void test_concurrentReadsAndWrites_noExceptions() throws Exception {
        final int numReaders = 5;
        final int numWriters = 3;
        final int operationsPerThread = 10;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(numReaders + numWriters);
        final AtomicBoolean errorOccurred = new AtomicBoolean(false);

        try (final ExecutorService executor = Executors.newFixedThreadPool(numReaders + numWriters)) {
            // Reader threads
            for (int i = 0; i < numReaders; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < operationsPerThread; j++) {
                            bridgeResource.getBridges();
                            Thread.sleep(1);
                        }
                    } catch (final Exception e) {
                        errorOccurred.set(true);
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }

            // Writer threads
            for (int i = 0; i < numWriters; i++) {
                final int writerIndex = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < operationsPerThread; j++) {
                            final String bridgeId = "writer-" + writerIndex + "-bridge-" + j;
                            final Bridge bridge = createTestBridge(bridgeId);
                            bridgeResource.addBridge(bridge);
                            Thread.sleep(1);
                            bridgeResource.removeBridge(bridgeId);
                        }
                    } catch (final Exception e) {
                        errorOccurred.set(true);
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(completionLatch.await(10, TimeUnit.SECONDS), "All operations should complete");
            assertFalse(errorOccurred.get(), "No errors should occur during concurrent operations");
        }
    }

    /**
     * Test Scenario 5: Stress test with mixed operations
     * Multiple threads performing random add/remove/update/read operations
     * Expected: No race conditions, no data corruption
     */
    @Test
    @Timeout(15)
    void test_mixedConcurrentOperations_noRaceConditions() throws Exception {
        final int numThreads = 10;
        final int operationsPerThread = 20;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(numThreads);
        final AtomicBoolean errorOccurred = new AtomicBoolean(false);
        final List<String> bridgeIds = Collections.synchronizedList(new ArrayList<>());

        // Pre-create some bridges
        for (int i = 0; i < 5; i++) {
            final String bridgeId = "stress-bridge-" + i;
            bridgeIds.add(bridgeId);
            bridgeResource.addBridge(createTestBridge(bridgeId));
        }

        try (final ExecutorService executor = Executors.newFixedThreadPool(numThreads)) {
            for (int i = 0; i < numThreads; i++) {
                final int threadIndex = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < operationsPerThread; j++) {
                            final int operation = (threadIndex + j) % 5;
                            final String bridgeId = bridgeIds.get((threadIndex + j) % bridgeIds.size());

                            switch (operation) {
                                case 0 -> { // Add
                                    final String newBridgeId = "stress-bridge-" + threadIndex + "-" + j;
                                    bridgeIds.add(newBridgeId);
                                    bridgeResource.addBridge(createTestBridge(newBridgeId));
                                }
                                case 1 -> // Remove
                                    bridgeResource.removeBridge(bridgeId);
                                case 2 -> { // Update
                                    final Bridge updatedBridge =
                                            createTestBridge(bridgeId).port(9000 + j);
                                    bridgeResource.updateBridge(bridgeId, updatedBridge);
                                }
                                case 3 -> // Read by name
                                    bridgeResource.getBridgeByName(bridgeId);
                                case 4 -> // Read all
                                    bridgeResource.getBridges();
                                default -> {}
                            }

                            if (j % 5 == 0) {
                                Thread.sleep(1);
                            }
                        }
                    } catch (final Exception e) {
                        errorOccurred.set(true);
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(completionLatch.await(10, TimeUnit.SECONDS), "All operations should complete");
            assertFalse(errorOccurred.get(), "No errors should occur during stress test");
        }
    }

    /**
     * Test Scenario 6: Concurrent status transitions during modifications
     * Threads call transitionBridgeStatus while others add/remove bridges
     * Expected: No race conditions where bridge is removed between check and action
     */
    @Test
    @Timeout(10)
    void test_concurrentStatusTransitionsAndModifications_noRaceConditions() throws Exception {
        // Setup initial bridges
        for (int i = 0; i < 5; i++) {
            final String bridgeId = "status-bridge-" + i;
            bridgeResource.addBridge(createTestBridge(bridgeId));
        }

        final int numThreads = 10;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(numThreads);
        final AtomicBoolean errorOccurred = new AtomicBoolean(false);

        try (final ExecutorService executor = Executors.newFixedThreadPool(numThreads)) {
            for (int i = 0; i < numThreads; i++) {
                final int threadIndex = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < 10; j++) {
                            final String bridgeId = "status-bridge-" + (j % 5);
                            final StatusTransitionCommand command =
                                    new StatusTransitionCommand().command(StatusTransitionCommand.CommandEnum.START);

                            if (threadIndex % 2 == 0) {
                                // Status transition
                                bridgeResource.transitionBridgeStatus(bridgeId, command);
                            } else {
                                // Remove and re-add
                                bridgeResource.removeBridge(bridgeId);
                                Thread.sleep(1);
                                bridgeResource.addBridge(createTestBridge(bridgeId));
                            }
                            Thread.sleep(2);
                        }
                    } catch (final Exception e) {
                        errorOccurred.set(true);
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(completionLatch.await(10, TimeUnit.SECONDS), "All operations should complete");
            assertFalse(errorOccurred.get(), "No errors should occur");
        }
    }

    /**
     * Test Scenario 7: Rapid add/remove cycles on same bridge ID
     * Simulates the exact scenario from BRIDGE_RACE_CONDITION_FIX.md where
     * updateBridge removes then adds, while removeBridge tries to remove
     * Expected: No double-removal, no lost updates
     */
    @Test
    @Timeout(10)
    void test_rapidAddRemoveCycles_noDanglingState() throws Exception {
        final String bridgeId = "cycle-bridge";
        final int cycles = 50;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(2);
        final AtomicInteger addCount = new AtomicInteger(0);
        final AtomicInteger removeCount = new AtomicInteger(0);

        try (final ExecutorService executor = Executors.newFixedThreadPool(2)) {
            // Thread 1: Rapid add/remove
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < cycles; i++) {
                        final Bridge bridge = createTestBridge(bridgeId);
                        final Response addResponse = bridgeResource.addBridge(bridge);
                        if (addResponse.getStatus() == 200) {
                            addCount.incrementAndGet();
                        }
                        Thread.sleep(1);
                        final Response removeResponse = bridgeResource.removeBridge(bridgeId);
                        if (removeResponse.getStatus() == 200) {
                            removeCount.incrementAndGet();
                        }
                    }
                } catch (final Exception e) {
                    // Ignored for test
                } finally {
                    completionLatch.countDown();
                }
            });

            // Thread 2: Concurrent updates
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < cycles; i++) {
                        final Bridge updatedBridge = createTestBridge(bridgeId).port(3000 + i);
                        bridgeResource.updateBridge(bridgeId, updatedBridge);
                        Thread.sleep(1);
                    }
                } catch (final Exception e) {
                    // Ignored for test
                } finally {
                    completionLatch.countDown();
                }
            });

            startLatch.countDown();
            assertTrue(completionLatch.await(10, TimeUnit.SECONDS), "All operations should complete");

            // Final state should be consistent (either bridge exists or doesn't)
            final int finalBridgeCount = bridgeStore.size();
            assertTrue(
                    finalBridgeCount == 0 || finalBridgeCount == 1,
                    "Bridge count should be 0 or 1, not " + finalBridgeCount);
        }
    }

    /**
     * SPECIFIC TEST: This test would have CAUGHT the original bug!
     * Tests the exact scenario from BRIDGE_RACE_CONDITION_FIX.md Scenario 1:
     * Thread 1 (updateBridge) and Thread 2 (removeBridge) race on the same bridge.
     * <p>
     * WITHOUT synchronization on removeBridge(), this test would demonstrate race conditions.
     * WITH proper synchronization: Operations are serialized correctly.
     */
    @Test
    @Timeout(10)
    void test_updateBridgeVsRemoveBridge_exactRaceConditionScenario() throws Exception {
        // Setup initial bridge
        final String bridgeId = "race-test-bridge";
        final Bridge initialBridge = createTestBridge(bridgeId).port(1883);
        bridgeResource.addBridge(initialBridge);

        final CountDownLatch bothThreadsReady = new CountDownLatch(2);
        final CountDownLatch startRace = new CountDownLatch(1);
        final CountDownLatch bothCompleted = new CountDownLatch(2);

        final AtomicInteger updateSuccessCount = new AtomicInteger(0);
        final AtomicInteger removeSuccessCount = new AtomicInteger(0);

        try (final ExecutorService executor = Executors.newFixedThreadPool(2)) {
            // Thread 1: updateBridge
            executor.submit(() -> {
                try {
                    bothThreadsReady.countDown();
                    startRace.await(5, TimeUnit.SECONDS);

                    final Bridge updatedBridge = createTestBridge(bridgeId).port(8883);
                    final Response response = bridgeResource.updateBridge(bridgeId, updatedBridge);
                    if (response.getStatus() == 200) {
                        updateSuccessCount.incrementAndGet();
                    }
                } catch (final Exception e) {
                    // Ignored
                } finally {
                    bothCompleted.countDown();
                }
            });

            // Thread 2: removeBridge
            executor.submit(() -> {
                try {
                    bothThreadsReady.countDown();
                    startRace.await(5, TimeUnit.SECONDS);

                    final Response response = bridgeResource.removeBridge(bridgeId);
                    if (response.getStatus() == 200) {
                        removeSuccessCount.incrementAndGet();
                    }
                } catch (final Exception e) {
                    // Ignored
                } finally {
                    bothCompleted.countDown();
                }
            });

            bothThreadsReady.await(5, TimeUnit.SECONDS);
            startRace.countDown(); // Start the race!

            assertTrue(bothCompleted.await(10, TimeUnit.SECONDS), "Both operations should complete");

            // Verify: With proper synchronization, operations are serialized
            // Either update then remove, or remove then update (which fails with 404)
            final int totalSuccesses = updateSuccessCount.get() + removeSuccessCount.get();
            assertTrue(totalSuccesses >= 1 && totalSuccesses <= 2, "At least one operation should succeed");

            // If both succeeded, bridge should be gone (remove was last)
            if (totalSuccesses == 2) {
                assertEquals(0, bridgeStore.size(), "If both operations succeeded, bridge should be gone");
            }
        }
    }

    /**
     * SPECIFIC TEST: Check-then-act race condition in addBridge()
     * <p>
     * WITHOUT synchronization: Two threads both check bridge doesn't exist, both try to add.
     * The second add would either:
     * 1. Succeed (overwriting first), causing non-deterministic behavior
     * 2. Fail with "already exists" error (but after the check said it doesn't exist!)
     * <p>
     * WITH synchronization: Only one add succeeds, the other gets "already exists" error
     * consistently.
     */
    @Test
    @Timeout(10)
    void test_checkThenActRaceInAddBridge() throws Exception {
        final String bridgeId = "check-race-bridge";

        // Latches to force both threads into the race window
        final CountDownLatch bothThreadsReady = new CountDownLatch(2);
        final CountDownLatch startRace = new CountDownLatch(1);
        final CountDownLatch bothCompleted = new CountDownLatch(2);

        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger alreadyExistsCount = new AtomicInteger(0);

        try (final ExecutorService executor = Executors.newFixedThreadPool(2)) {
            for (int i = 0; i < 2; i++) {
                final int threadNum = i;
                executor.submit(() -> {
                    try {
                        bothThreadsReady.countDown();
                        startRace.await(5, TimeUnit.SECONDS);

                        // Both threads try to add at exactly the same time
                        final Bridge bridge = createTestBridge(bridgeId).port(2000 + threadNum);
                        final Response response = bridgeResource.addBridge(bridge);

                        if (response.getStatus() == 200) {
                            successCount.incrementAndGet();
                        } else if (response.getStatus() == 400 || response.getStatus() == 409) {
                            // "Already exists" error
                            alreadyExistsCount.incrementAndGet();
                        }
                    } catch (final Exception e) {
                        // Ignored
                    } finally {
                        bothCompleted.countDown();
                    }
                });
            }

            bothThreadsReady.await(5, TimeUnit.SECONDS);
            startRace.countDown(); // Start the race!

            assertTrue(bothCompleted.await(5, TimeUnit.SECONDS), "Both threads should complete");

            // With proper synchronization: exactly one succeeds, one gets "already exists"
            assertEquals(1, successCount.get(), "Exactly one add should succeed");
            assertEquals(1, alreadyExistsCount.get(), "Exactly one add should fail with 'already exists'");
            assertEquals(1, bridgeStore.size(), "Exactly one bridge should exist");
        }
    }

    /**
     * SPECIFIC TEST: Check-then-act race condition in removeBridge()
     * <p>
     * WITHOUT synchronization: Two threads both check bridge exists, both try to remove.
     * The second remove operates on non-existent bridge.
     * <p>
     * WITH synchronization: Only one remove succeeds, the other gets 404.
     */
    @Test
    @Timeout(10)
    void test_checkThenActRaceInRemoveBridge() throws Exception {
        final String bridgeId = "remove-race-bridge";

        // Setup bridge
        bridgeResource.addBridge(createTestBridge(bridgeId));

        final CountDownLatch bothThreadsReady = new CountDownLatch(2);
        final CountDownLatch startRace = new CountDownLatch(1);
        final CountDownLatch bothCompleted = new CountDownLatch(2);

        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger notFoundCount = new AtomicInteger(0);

        try (final ExecutorService executor = Executors.newFixedThreadPool(2)) {
            for (int i = 0; i < 2; i++) {
                executor.submit(() -> {
                    try {
                        bothThreadsReady.countDown();
                        startRace.await(5, TimeUnit.SECONDS);

                        // Both threads try to remove at exactly the same time
                        final Response response = bridgeResource.removeBridge(bridgeId);

                        if (response.getStatus() == 200) {
                            successCount.incrementAndGet();
                        } else if (response.getStatus() == 404) {
                            notFoundCount.incrementAndGet();
                        }
                    } catch (final Exception e) {
                        // Ignored
                    } finally {
                        bothCompleted.countDown();
                    }
                });
            }

            bothThreadsReady.await(5, TimeUnit.SECONDS);
            startRace.countDown(); // Start the race!

            assertTrue(bothCompleted.await(5, TimeUnit.SECONDS), "Both threads should complete");

            // With proper synchronization: exactly one succeeds, one gets 404
            assertEquals(1, successCount.get(), "Exactly one remove should succeed");
            assertEquals(1, notFoundCount.get(), "Exactly one remove should fail with 404");
            assertEquals(0, bridgeStore.size(), "Bridge should be gone");
        }
    }

    /**
     * SPECIFIC TEST: transitionBridgeStatus() race with removeBridge()
     * <p>
     * Tests the scenario where:
     * - Thread 1 calls transitionBridgeStatus(bridgeId, RESTART)
     * - Thread 2 calls removeBridge(bridgeId) at the same time
     * <p>
     * WITHOUT synchronization on transitionBridgeStatus():
     * - Thread 1 checks bridge exists (true)
     * - Thread 2 removes bridge
     * - Thread 1 calls bridgeService.restartBridge() on non-existent bridge
     * - Possible NPE or "bridge not found" error
     * <p>
     * WITH synchronization: Operations are serialized, one completes before the other starts
     */
    @Test
    @Timeout(10)
    void test_transitionBridgeStatusVsRemove_raceCondition() throws Exception {
        final String bridgeId = "transition-race-bridge";

        // Setup bridge
        bridgeResource.addBridge(createTestBridge(bridgeId));

        final CountDownLatch bothThreadsReady = new CountDownLatch(2);
        final CountDownLatch startRace = new CountDownLatch(1);
        final CountDownLatch bothCompleted = new CountDownLatch(2);

        final AtomicInteger transitionSuccessCount = new AtomicInteger(0);
        final AtomicInteger transitionNotFoundCount = new AtomicInteger(0);
        final AtomicInteger removeSuccessCount = new AtomicInteger(0);

        try (final ExecutorService executor = Executors.newFixedThreadPool(2)) {
            // Thread 1: Transition status
            executor.submit(() -> {
                try {
                    bothThreadsReady.countDown();
                    startRace.await(5, TimeUnit.SECONDS);

                    final StatusTransitionCommand command =
                            new StatusTransitionCommand().command(StatusTransitionCommand.CommandEnum.RESTART);
                    final Response response = bridgeResource.transitionBridgeStatus(bridgeId, command);

                    if (response.getStatus() == 200) {
                        transitionSuccessCount.incrementAndGet();
                    } else if (response.getStatus() == 404) {
                        transitionNotFoundCount.incrementAndGet();
                    }
                } catch (final Exception e) {
                    // Ignored
                } finally {
                    bothCompleted.countDown();
                }
            });

            // Thread 2: Remove bridge
            executor.submit(() -> {
                try {
                    bothThreadsReady.countDown();
                    startRace.await(5, TimeUnit.SECONDS);

                    final Response response = bridgeResource.removeBridge(bridgeId);
                    if (response.getStatus() == 200) {
                        removeSuccessCount.incrementAndGet();
                    }
                } catch (final Exception e) {
                    // Ignored
                } finally {
                    bothCompleted.countDown();
                }
            });

            bothThreadsReady.await(5, TimeUnit.SECONDS);
            startRace.countDown(); // Start the race!

            assertTrue(bothCompleted.await(5, TimeUnit.SECONDS), "Both threads should complete");

            // With proper synchronization: Either transition then remove, or remove then transition
            // One of these must be true:
            // 1. Both succeeded (transition happened first, then remove)
            // 2. Remove succeeded, transition got 404 (remove happened first)

            final int totalSuccesses = transitionSuccessCount.get() + removeSuccessCount.get();
            assertTrue(
                    totalSuccesses >= 1 && totalSuccesses <= 2,
                    "At least one operation should succeed: transition=" + transitionSuccessCount.get()
                            + ", transition-404="
                            + transitionNotFoundCount.get()
                            + ", remove="
                            + removeSuccessCount.get());

            // If both succeeded, bridge should be gone (remove was last)
            if (totalSuccesses == 2) {
                assertEquals(0, bridgeStore.size(), "Bridge should be gone if both operations succeeded");
            }
        }
    }

    /**
     * SPECIFIC TEST: Verify getBridges() returns consistent snapshot during concurrent modifications
     * <p>
     * WITHOUT synchronization on getBridges():
     * - Thread 1 reads bridge list while Thread 2 modifies it
     * - Could get ConcurrentModificationException or inconsistent snapshot
     * - Might see bridge in list that's being removed, or miss bridge being added
     * <p>
     * WITH synchronization: getBridges() always returns consistent snapshot
     */
    @Test
    @Timeout(10)
    void test_getBridgesReturnsConsistentSnapshotDuringModifications() throws Exception {
        // Add initial bridges
        for (int i = 0; i < 5; i++) {
            bridgeResource.addBridge(createTestBridge("initial-bridge-" + i));
        }

        final int iterations = 100;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(2);
        final AtomicBoolean errorOccurred = new AtomicBoolean(false);

        try (final ExecutorService executor = Executors.newFixedThreadPool(2)) {
            // Thread 1: Constantly read bridge list
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < iterations; i++) {
                        final Response response = bridgeResource.getBridges();
                        if (response.getStatus() != 200) {
                            errorOccurred.set(true);
                            break;
                        }
                        // With proper synchronization, this should never throw exception
                        // and should always return a valid list
                    }
                } catch (final Exception e) {
                    errorOccurred.set(true);
                } finally {
                    completionLatch.countDown();
                }
            });

            // Thread 2: Constantly modify bridges
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < iterations; i++) {
                        final String bridgeId = "concurrent-bridge-" + i;
                        bridgeResource.addBridge(createTestBridge(bridgeId));
                        Thread.sleep(1);
                        bridgeResource.removeBridge(bridgeId);
                    }
                } catch (final Exception e) {
                    errorOccurred.set(true);
                } finally {
                    completionLatch.countDown();
                }
            });

            startLatch.countDown();
            assertTrue(completionLatch.await(15, TimeUnit.SECONDS), "Both threads should complete");
            assertFalse(
                    errorOccurred.get(), "No errors should occur when reading bridges during concurrent modifications");
        }
    }

    /**
     * SPECIFIC TEST: Repetitive test to catch timing-dependent race conditions
     * <p>
     * This test repeats the check-then-act race condition many times to increase
     * probability of catching the bug if synchronization is missing.
     */
    @Test
    @Timeout(15)
    void test_repeatedRaceConditionAttempts_shouldAlwaysBeConsistent() throws Exception {
        final int repetitions = 50;

        for (int rep = 0; rep < repetitions; rep++) {
            final String bridgeId = "repeat-race-bridge-" + rep;

            final CountDownLatch bothReady = new CountDownLatch(2);
            final CountDownLatch start = new CountDownLatch(1);
            final CountDownLatch bothDone = new CountDownLatch(2);

            final AtomicInteger addSuccess = new AtomicInteger(0);
            final AtomicInteger addFailed = new AtomicInteger(0);

            try (final ExecutorService executor = Executors.newFixedThreadPool(2)) {
                for (int i = 0; i < 2; i++) {
                    final int port = 3000 + i;
                    executor.submit(() -> {
                        try {
                            bothReady.countDown();
                            start.await();

                            final Bridge bridge = createTestBridge(bridgeId).port(port);
                            final Response response = bridgeResource.addBridge(bridge);

                            if (response.getStatus() == 200) {
                                addSuccess.incrementAndGet();
                            } else {
                                addFailed.incrementAndGet();
                            }
                        } catch (final Exception e) {
                            // Ignored
                        } finally {
                            bothDone.countDown();
                        }
                    });
                }

                bothReady.await();
                start.countDown();
                assertTrue(bothDone.await(2, TimeUnit.SECONDS), "Repetition " + rep + " should complete");

                // CRITICAL: With proper synchronization, this should ALWAYS be true
                assertEquals(1, addSuccess.get(), "Repetition " + rep + ": Exactly one add should succeed");
                assertEquals(1, addFailed.get(), "Repetition " + rep + ": Exactly one add should fail");

                // Clean up for next iteration
                if (bridgeStore.containsKey(bridgeId)) {
                    bridgeResource.removeBridge(bridgeId);
                }
            }
        }
    }
}
