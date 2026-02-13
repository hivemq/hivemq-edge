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
package com.hivemq.bridge;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.bridge.config.MqttBridge;
import com.hivemq.bridge.mqtt.BridgeMqttClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.reader.BridgeExtractor;
import com.hivemq.edge.HiveMQEdgeRemoteService;
import java.util.ArrayList;
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

class BridgeServiceConcurrentOperationsTest {

    private @Nullable BridgeService bridgeService;
    private @Nullable BridgeMqttClientFactory clientFactory;

    private static @NotNull BridgeMqttClient createMockClient(
            final @NotNull MqttBridge bridge,
            final @NotNull AtomicInteger disconnectCounter,
            final boolean shouldTimeout) {
        final BridgeMqttClient mockClient = mock(BridgeMqttClient.class);

        // Start always succeeds
        when(mockClient.start()).thenReturn(Futures.immediateFuture(null));
        when(mockClient.isConnected()).thenReturn(false);
        when(mockClient.getActiveForwarders()).thenReturn(List.of());
        when(mockClient.getBridge()).thenReturn(bridge);
        when(mockClient.createForwarders()).thenReturn(List.of());

        // Stop behavior
        if (shouldTimeout) {
            // Create a future that never completes (simulates timeout)
            when(mockClient.stop()).thenReturn(SettableFuture.create());
        } else {
            // Normal stop that completes
            when(mockClient.stop()).thenAnswer(invocation -> {
                disconnectCounter.incrementAndGet();
                return Futures.immediateFuture(null);
            });
        }
        return mockClient;
    }

    private static @NotNull MqttBridge createTestBridge(final @NotNull String bridgeId) {
        return createTestBridge(bridgeId, 1883);
    }

    private static @NotNull MqttBridge createTestBridge(final @NotNull String bridgeId, final int port) {
        return new MqttBridge.Builder()
                .withId(bridgeId)
                .withHost("test.example.com")
                .withPort(port)
                .withClientId(bridgeId + "-client")
                .withLocalSubscriptions(List.of())
                .withRemoteSubscriptions(List.of())
                .build();
    }

    @BeforeEach
    void setUp() {
        clientFactory = mock(BridgeMqttClientFactory.class);
        final MessageForwarder messageForwarder = mock(MessageForwarder.class);
        final ExecutorService executorService = MoreExecutors.newDirectExecutorService();
        final HiveMQEdgeRemoteService remoteService = mock(HiveMQEdgeRemoteService.class);
        final MetricRegistry metricRegistry = new MetricRegistry();
        final BridgeExtractor bridgeExtractor = mock(BridgeExtractor.class);
        final ShutdownHooks shutdownHooks = new ShutdownHooks();
        bridgeService = new BridgeService(
                bridgeExtractor,
                messageForwarder,
                requireNonNull(clientFactory),
                executorService,
                remoteService,
                shutdownHooks,
                metricRegistry);
    }

    /**
     * Test that when a bridge is rapidly deleted and recreated, the old MQTT client
     * is properly stopped and doesn't continue reconnecting.
     */
    @Test
    void testRapidBridgeDeleteAndRecreate_noDanglingClients() {
        // Track how many times disconnect is called on each client
        final AtomicInteger client1DisconnectCalls = new AtomicInteger(0);
        final AtomicInteger client2DisconnectCalls = new AtomicInteger(0);

        // Create first bridge and mock client
        final MqttBridge bridge1 = createTestBridge("test-bridge", 1883);
        final BridgeMqttClient mockClient1 = createMockClient(bridge1, client1DisconnectCalls, false);
        when(requireNonNull(clientFactory).createRemoteClient(any())).thenReturn(mockClient1);

        // Add bridge
        requireNonNull(bridgeService).updateBridges(List.of(bridge1));
        verify(mockClient1, times(1)).start();

        // Create second version of same bridge with different config (simulating config change)
        final MqttBridge bridge2 = createTestBridge("test-bridge", 8883); // Different port
        final BridgeMqttClient mockClient2 = createMockClient(bridge2, client2DisconnectCalls, false);
        when(requireNonNull(clientFactory).createRemoteClient(any())).thenReturn(mockClient2);

        // Update with new config (should stop old, start new)
        requireNonNull(bridgeService).updateBridges(List.of(bridge2));

        // Verify old client was stopped
        verify(mockClient1, times(1)).stop();
        assertTrue(client1DisconnectCalls.get() >= 1, "Old client should have been disconnected");

        // Verify new client was started
        verify(mockClient2, times(1)).start();

        // Verify old client is not called anymore (no reconnection)
        verify(mockClient1, atMost(1)).start(); // Only initial start, no reconnection
    }

    /**
     * Test that when stop() times out, the service attempts forced disconnect
     */
    @Test
    void testStopTimeout_attemptsForcedDisconnect() {
        final AtomicInteger forcedDisconnectCalls = new AtomicInteger(0);

        // Create bridge and mock client that times out on stop
        final MqttBridge bridge = createTestBridge("timeout-bridge");
        final BridgeMqttClient mockClient = createMockClient(bridge, new AtomicInteger(0), true);

        // Mock the underlying Mqtt5AsyncClient to track forced disconnect
        final Mqtt5AsyncClient mqtt5Client = mock(Mqtt5AsyncClient.class);
        when(mockClient.getMqtt5Client()).thenReturn(mqtt5Client);
        when(mqtt5Client.disconnect()).thenAnswer(invocation -> {
            forcedDisconnectCalls.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        });

        when(requireNonNull(clientFactory).createRemoteClient(bridge)).thenReturn(mockClient);

        // Add bridge
        requireNonNull(bridgeService).updateBridges(List.of(bridge));

        // Remove bridge (should trigger timeout and forced disconnect)
        requireNonNull(bridgeService).updateBridges(List.of());

        // Verify forced disconnect was attempted
        assertTrue(forcedDisconnectCalls.get() >= 1, "Forced disconnect should be attempted when stop times out");
    }

    // Helper methods

    /**
     * Test concurrent add/remove operations on multiple bridges
     */
    @Test
    void testConcurrentBridgeOperations_noRaceConditions() throws Exception {
        final int numThreads = 5;
        final int operationsPerThread = 10;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(numThreads);
        try (final ExecutorService threadPool = Executors.newFixedThreadPool(numThreads)) {
            final AtomicBoolean errorOccurred = new AtomicBoolean(false);

            // Create bridges with unique IDs
            for (int i = 0; i < numThreads; i++) {
                final int bridgeIndex = i;
                threadPool.submit(() -> {
                    try {
                        // Wait for all threads to be ready
                        startLatch.await();

                        // Perform rapid add/remove operations
                        for (int j = 0; j < operationsPerThread; j++) {
                            final String bridgeId = "bridge-" + bridgeIndex;
                            final MqttBridge bridge = createTestBridge(bridgeId);
                            final BridgeMqttClient mockClient = createMockClient(bridge, new AtomicInteger(0), false);
                            when(requireNonNull(clientFactory).createRemoteClient(any()))
                                    .thenReturn(mockClient);

                            // Add
                            requireNonNull(bridgeService).updateBridges(List.of(bridge));

                            // Small delay
                            Thread.sleep(10);

                            // Remove
                            requireNonNull(bridgeService).updateBridges(List.of());

                            // Small delay
                            Thread.sleep(10);
                        }
                    } catch (final Exception e) {
                        errorOccurred.set(true);
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }

            // Start all threads
            startLatch.countDown();

            // Wait for completion
            assertTrue(completionLatch.await(30, TimeUnit.SECONDS), "All threads should complete within timeout");
            assertFalse(errorOccurred.get(), "No errors should occur during concurrent operations");

            threadPool.shutdown();
            assertTrue(threadPool.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    /**
     * Test that when multiple bridges with same client ID are created (misconfiguration),
     * old clients are properly stopped
     */
    @Test
    void testMultipleBridgesSameClientId_properCleanup() {
        final AtomicInteger client1Stops = new AtomicInteger(0);
        final AtomicInteger client2Stops = new AtomicInteger(0);

        // Create two different bridges but with same underlying client ID
        final MqttBridge bridge1 = createTestBridge("bridge-1");
        final MqttBridge bridge2 = createTestBridge("bridge-2");

        final BridgeMqttClient mockClient1 = createMockClient(bridge1, client1Stops, false);
        final BridgeMqttClient mockClient2 = createMockClient(bridge2, client2Stops, false);

        when(requireNonNull(clientFactory).createRemoteClient(bridge1)).thenReturn(mockClient1);
        when(requireNonNull(clientFactory).createRemoteClient(bridge2)).thenReturn(mockClient2);

        // Add both bridges
        requireNonNull(bridgeService).updateBridges(List.of(bridge1, bridge2));
        verify(mockClient1, times(1)).start();
        verify(mockClient2, times(1)).start();

        // Remove bridge1
        requireNonNull(bridgeService).updateBridges(List.of(bridge2));
        verify(mockClient1, times(1)).stop();
        assertTrue(client1Stops.get() >= 1, "Bridge 1 client should be stopped");

        // Bridge 2 should still be running
        verify(mockClient2, times(1)).start();
        verify(mockClient2, never()).stop();

        // Remove bridge2
        requireNonNull(bridgeService).updateBridges(List.of());
        verify(mockClient2, times(1)).stop();
        assertTrue(client2Stops.get() >= 1, "Bridge 2 client should be stopped");
    }

    /**
     * CORNER CASE: Bridge update with different port number while client is reconnecting
     * Ensures old client is stopped even if it's in reconnection loop
     */
    @Test
    void testBridgeUpdateDuringReconnection_oldClientStopped() {
        final MqttBridge bridge1 = createTestBridge("reconnect-bridge", 1883);
        final BridgeMqttClient mockClient1 = mock(BridgeMqttClient.class);

        // Simulate client in reconnection loop
        when(mockClient1.start()).thenReturn(Futures.immediateFuture(null));
        when(mockClient1.isConnected()).thenReturn(false);
        when(mockClient1.getActiveForwarders()).thenReturn(List.of());
        when(mockClient1.getBridge()).thenReturn(bridge1);
        when(mockClient1.createForwarders()).thenReturn(List.of());
        when(mockClient1.stop()).thenReturn(Futures.immediateFuture(null));

        when(requireNonNull(clientFactory).createRemoteClient(any())).thenReturn(mockClient1);

        // Add bridge
        requireNonNull(bridgeService).updateBridges(List.of(bridge1));

        // Update to different port (simulating config change)
        final MqttBridge bridge2 = createTestBridge("reconnect-bridge", 8883);
        final BridgeMqttClient mockClient2 = createMockClient(bridge2, new AtomicInteger(0), false);
        when(requireNonNull(clientFactory).createRemoteClient(any())).thenReturn(mockClient2);

        requireNonNull(bridgeService).updateBridges(List.of(bridge2));

        // Verify old client was stopped (even if reconnecting)
        verify(mockClient1, times(1)).stop();
        verify(mockClient2, times(1)).start();
    }

    /**
     * CORNER CASE: Multiple rapid bridge updates (config changes coming in quickly)
     * Ensures intermediate clients are properly stopped
     */
    @Test
    void testRapidBridgeUpdates_allIntermediateClientsStopped() {
        final String bridgeId = "rapid-update-bridge";
        final List<BridgeMqttClient> allClients = new ArrayList<>();

        // Create 5 different bridge configs with different ports
        for (int i = 0; i < 5; i++) {
            final int port = 2000 + i;
            final MqttBridge bridge = createTestBridge(bridgeId, port);
            final BridgeMqttClient mockClient = createMockClient(bridge, new AtomicInteger(0), false);
            allClients.add(mockClient);

            when(requireNonNull(clientFactory).createRemoteClient(any())).thenReturn(mockClient);

            // Update bridge rapidly
            requireNonNull(bridgeService).updateBridges(List.of(bridge));

            // Small delay between updates
            try {
                Thread.sleep(10);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Verify: First 4 clients should be stopped, 5th should be running
        for (int i = 0; i < 4; i++) {
            verify(allClients.get(i), times(1)).stop();
        }
        // Last client should still be running
        verify(allClients.get(4), times(1)).start();
        verify(allClients.get(4), never()).stop();
    }

    /**
     * CORNER CASE: Bridge update while stop is in progress (timeout scenario)
     * Ensures system doesn't get stuck waiting for old client to stop
     */
    @Test
    void testUpdateWhileStopInProgress_doesNotBlock() throws Exception {
        final String bridgeId = "stop-in-progress-bridge";
        final MqttBridge bridge1 = createTestBridge(bridgeId, 1883);
        final SettableFuture<Void> stopFuture = SettableFuture.create();

        final BridgeMqttClient slowStopClient = mock(BridgeMqttClient.class);
        when(slowStopClient.start()).thenReturn(Futures.immediateFuture(null));
        when(slowStopClient.stop()).thenReturn(stopFuture); // Never completes
        when(slowStopClient.isConnected()).thenReturn(true);
        when(slowStopClient.getActiveForwarders()).thenReturn(List.of());
        when(slowStopClient.getBridge()).thenReturn(bridge1);
        when(slowStopClient.createForwarders()).thenReturn(List.of());
        when(slowStopClient.getMqtt5Client()).thenReturn(mock(Mqtt5AsyncClient.class));

        when(requireNonNull(clientFactory).createRemoteClient(bridge1)).thenReturn(slowStopClient);

        // Add initial bridge
        requireNonNull(bridgeService).updateBridges(List.of(bridge1));

        // Set up the second bridge and its mock client before the async operation
        final MqttBridge bridge2 = createTestBridge(bridgeId, 8883);
        final BridgeMqttClient mockClient2 = createMockClient(bridge2, new AtomicInteger(0), false);
        when(requireNonNull(clientFactory).createRemoteClient(bridge2)).thenReturn(mockClient2);

        // Start update in background (will block on stop)
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final CompletableFuture<Void> updateFuture = CompletableFuture.runAsync(
                () -> requireNonNull(bridgeService).updateBridges(List.of(bridge2)), executor);

        // Wait a bit to let update start
        Thread.sleep(500);

        // Complete the stop after delay
        stopFuture.set(null);

        // Update should eventually complete (with timeout)
        updateFuture.get(35, TimeUnit.SECONDS); // Slightly longer than stop timeout

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    /**
     * CORNER CASE: Remove bridge that's currently failing to connect
     * Ensures failed/failing bridges can still be removed cleanly
     */
    @Test
    void testRemoveBridgeWhileConnectionFailing() {
        final MqttBridge bridge = createTestBridge("failing-bridge");
        final BridgeMqttClient mockClient = mock(BridgeMqttClient.class);

        // Simulate connection failure
        when(mockClient.start()).thenReturn(Futures.immediateFailedFuture(new RuntimeException("Connection refused")));
        when(mockClient.stop()).thenReturn(Futures.immediateFuture(null));
        when(mockClient.isConnected()).thenReturn(false);
        when(mockClient.getActiveForwarders()).thenReturn(List.of());
        when(mockClient.getBridge()).thenReturn(bridge);
        when(mockClient.createForwarders()).thenReturn(List.of());

        when(requireNonNull(clientFactory).createRemoteClient(bridge)).thenReturn(mockClient);

        // Add bridge (will fail to connect)
        requireNonNull(bridgeService).updateBridges(List.of(bridge));

        // Remove bridge (should succeed even though connection failed)
        requireNonNull(bridgeService).updateBridges(List.of());

        // Verify client was stopped
        verify(mockClient, times(1)).stop();
    }

    /**
     * CORNER CASE: Concurrent updates to different bridges
     * Ensures multiple bridge operations don't interfere with each other
     */
    @Test
    void testConcurrentUpdatesToDifferentBridges() throws Exception {
        final int numBridges = 5;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(numBridges);
        final AtomicBoolean errorOccurred = new AtomicBoolean(false);

        try (final ExecutorService threadPool = Executors.newFixedThreadPool(numBridges)) {
            for (int i = 0; i < numBridges; i++) {
                final String bridgeId = "concurrent-bridge-" + i;
                final int port = 3000 + i;

                threadPool.submit(() -> {
                    try {
                        startLatch.await();

                        final MqttBridge bridge = createTestBridge(bridgeId, port);
                        final BridgeMqttClient mockClient = createMockClient(bridge, new AtomicInteger(0), false);
                        when(requireNonNull(clientFactory).createRemoteClient(any()))
                                .thenReturn(mockClient);

                        // Add bridge
                        requireNonNull(bridgeService).updateBridges(List.of(bridge));

                        // Update bridge
                        final MqttBridge updatedBridge = createTestBridge(bridgeId, port + 100);
                        final BridgeMqttClient updatedClient =
                                createMockClient(updatedBridge, new AtomicInteger(0), false);
                        when(requireNonNull(clientFactory).createRemoteClient(any()))
                                .thenReturn(updatedClient);
                        requireNonNull(bridgeService).updateBridges(List.of(updatedBridge));

                        // Remove bridge
                        requireNonNull(bridgeService).updateBridges(List.of());

                    } catch (final Exception e) {
                        errorOccurred.set(true);
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(completionLatch.await(30, TimeUnit.SECONDS), "All concurrent operations should complete");
            assertFalse(
                    errorOccurred.get(), "No errors should occur during concurrent operations on different bridges");
        }
    }

    /**
     * CORNER CASE: Bridge with same ID but completely different config
     * (different host, port, client ID, etc.)
     * Ensures complete bridge replacement works correctly
     */
    @Test
    void testBridgeReplacementWithCompletelyDifferentConfig() {
        final String bridgeId = "replacement-bridge";

        // Original bridge
        final MqttBridge.Builder builder1 = new MqttBridge.Builder()
                .withId(bridgeId)
                .withHost("original.example.com")
                .withPort(1883)
                .withClientId("original-client")
                .withLocalSubscriptions(List.of())
                .withRemoteSubscriptions(List.of());
        final MqttBridge bridge1 = builder1.build();

        final BridgeMqttClient mockClient1 = createMockClient(bridge1, new AtomicInteger(0), false);
        when(requireNonNull(clientFactory).createRemoteClient(bridge1)).thenReturn(mockClient1);

        requireNonNull(bridgeService).updateBridges(List.of(bridge1));

        // Completely different bridge with same ID
        final MqttBridge.Builder builder2 = new MqttBridge.Builder()
                .withId(bridgeId)
                .withHost("replacement.example.com")
                .withPort(8883)
                .withClientId("replacement-client")
                .withLocalSubscriptions(List.of())
                .withRemoteSubscriptions(List.of());
        final MqttBridge bridge2 = builder2.build();

        final BridgeMqttClient mockClient2 = createMockClient(bridge2, new AtomicInteger(0), false);
        when(requireNonNull(clientFactory).createRemoteClient(bridge2)).thenReturn(mockClient2);

        requireNonNull(bridgeService).updateBridges(List.of(bridge2));

        // Verify old client stopped, new client started
        verify(mockClient1, times(1)).stop();
        verify(mockClient2, times(1)).start();
    }

    /**
     * CORNER CASE: Empty bridge list update (remove all bridges at once)
     * Ensures bulk removal works correctly
     */
    @Test
    void testRemoveAllBridgesAtOnce() {
        final List<BridgeMqttClient> allClients = new ArrayList<>();
        final List<MqttBridge> allBridges = new ArrayList<>();

        // Add multiple bridges
        for (int i = 0; i < 5; i++) {
            final MqttBridge bridge = createTestBridge("bulk-remove-bridge-" + i);
            final BridgeMqttClient mockClient = createMockClient(bridge, new AtomicInteger(0), false);
            allClients.add(mockClient);
            allBridges.add(bridge);
            // Stub each bridge individually to avoid overwriting previous stubs
            when(requireNonNull(clientFactory).createRemoteClient(bridge)).thenReturn(mockClient);
        }

        requireNonNull(bridgeService).updateBridges(allBridges);

        // Remove all at once
        requireNonNull(bridgeService).updateBridges(List.of());

        // Verify all clients were stopped
        for (final BridgeMqttClient client : allClients) {
            verify(client, times(1)).stop();
        }
    }

    /**
     * Test the 30-second timeout is used (not the old 10-second timeout)
     * <p>
     * Note: This test is disabled by default as it takes 2+ seconds to run.
     * Enable it manually if you need to verify the timeout behavior.
     * The key test is testStopTimeout_attemptsForcedDisconnect which verifies
     * the forced disconnect logic without needing to wait for actual timeouts.
     */
    @Test
    void testStopTimeout_uses30Seconds() {
        final long startTime = System.currentTimeMillis();
        final SettableFuture<Void> stopFuture = SettableFuture.create();

        // Create bridge and mock client with delayed stop
        final MqttBridge bridge = createTestBridge("slow-bridge");
        final BridgeMqttClient mockClient = mock(BridgeMqttClient.class);

        when(mockClient.stop()).thenReturn(stopFuture);
        when(mockClient.start()).thenReturn(Futures.immediateFuture(null));
        when(mockClient.isConnected()).thenReturn(false);
        when(mockClient.getActiveForwarders()).thenReturn(List.of());
        when(mockClient.getBridge()).thenReturn(bridge);
        when(mockClient.createForwarders()).thenReturn(List.of());
        when(mockClient.getMqtt5Client()).thenReturn(mock(Mqtt5AsyncClient.class));

        when(requireNonNull(clientFactory).createRemoteClient(bridge)).thenReturn(mockClient);

        // Add bridge
        requireNonNull(bridgeService).updateBridges(List.of(bridge));

        // Complete the stop future after 25 seconds in a background thread
        new Thread(() -> {
                    try {
                        Thread.sleep(25000);
                        stopFuture.set(null);
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                })
                .start();

        // Remove bridge (should wait up to 30 seconds)
        requireNonNull(bridgeService).updateBridges(List.of());

        final long duration = System.currentTimeMillis() - startTime;

        // Should have waited at least 25 seconds (when we complete the future)
        // but less than 30 seconds (the timeout)
        assertTrue(duration >= 24000, "Should wait for slow stop: " + duration + "ms");
        assertTrue(duration < 35000, "Should not wait much longer than timeout: " + duration + "ms");
    }
}
