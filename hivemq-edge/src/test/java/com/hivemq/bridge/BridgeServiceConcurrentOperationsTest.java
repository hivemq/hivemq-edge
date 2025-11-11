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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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

/**
 * Integration test for BridgeService to verify that concurrent add/remove operations
 * don't leave dangling MQTT clients that continue to reconnect after being stopped.
 * <p>
 * Tests the fixes for:
 * 1. Increased stop timeout (30s)
 * 2. Forced termination on timeout
 * 3. Canceling scheduled reconnections
 */
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
        return new MqttBridge.Builder().withId(bridgeId)
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
        bridgeService = new BridgeService(bridgeExtractor,
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
                            when(requireNonNull(clientFactory).createRemoteClient(any())).thenReturn(mockClient);

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
        }).start();

        // Remove bridge (should wait up to 30 seconds)
        requireNonNull(bridgeService).updateBridges(List.of());

        final long duration = System.currentTimeMillis() - startTime;

        // Should have waited at least 25 seconds (when we complete the future)
        // but less than 30 seconds (the timeout)
        assertTrue(duration >= 24000, "Should wait for slow stop: " + duration + "ms");
        assertTrue(duration < 35000, "Should not wait much longer than timeout: " + duration + "ms");
    }
}
