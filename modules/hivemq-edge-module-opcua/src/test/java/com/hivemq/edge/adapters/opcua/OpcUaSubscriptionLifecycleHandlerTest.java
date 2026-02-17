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

import static com.hivemq.edge.adapters.opcua.listeners.OpcUaSubscriptionLifecycleHandler.KEEP_ALIVE_SAFETY_MARGIN_MS;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.edge.adapters.opcua.config.ConnectionOptions;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import com.hivemq.edge.adapters.opcua.listeners.OpcUaSubscriptionLifecycleHandler;
import java.util.List;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpcUaSubscriptionLifecycleHandlerTest {

    private static final @NotNull String ADAPTER_ID = "test-adapter";
    private static final @NotNull String NODE_ID = "ns=2;i=1001";

    @Mock
    private @NotNull ProtocolAdapterMetricsService metricsService;

    @Mock
    private @NotNull ProtocolAdapterTagStreamingService tagStreamingService;

    @Mock
    private @NotNull FakeEventService eventService;

    @Mock
    private @NotNull OpcUaClient opcUaClient;

    @Mock
    private @NotNull DataPointFactory dataPointFactory;

    private static @NotNull OpcUaSpecificAdapterConfig createConfig(
            final @NotNull ConnectionOptions connectionOptions) {
        return new OpcUaSpecificAdapterConfig(
                "opc.tcp://localhost:4840", // uri
                false, // overrideUri
                null, // applicationUri
                null, // auth
                null, // tls
                OpcUaToMqttConfig.defaultOpcUaToMqttConfig(), // opcuaToMqtt
                null, // security
                connectionOptions // connectionOptions
                );
    }

    private static @NotNull OpcuaTag createTestTag() {
        return new OpcuaTag("test-tag", "Test tag for keep-alive testing", new OpcuaTagDefinition(NODE_ID));
    }

    /**
     * Test that keep-alive timeout is correctly calculated from configuration with default values.
     * Default: keepAliveInterval=10s, failuresAllowed=3, safetyMargin=5s
     * Expected: 10 × (3 + 1) + 5 = 45 seconds
     */
    @Test
    void testKeepAliveTimeout_withDefaultConfiguration() {
        // Given: Default configuration
        final ConnectionOptions connectionOptions = ConnectionOptions.defaultConnectionOptions();
        final OpcUaSpecificAdapterConfig config = createConfig(connectionOptions);

        // When: Handler is created
        final OpcUaSubscriptionLifecycleHandler handler = createHandler(config);

        // Then: Timeout should be calculated correctly
        final long expectedTimeout = 10_000 * (3 + 1) + KEEP_ALIVE_SAFETY_MARGIN_MS; // 45 seconds
        assertEquals(
                expectedTimeout,
                handler.getKeepAliveTimeoutMs(),
                "Keep-alive timeout should be keepAliveInterval × (failuresAllowed + 1) + safetyMargin");
    }

    /**
     * Test keep-alive timeout calculation with custom short intervals.
     * Custom: keepAliveInterval=5s, failuresAllowed=2, safetyMargin=5s
     * Expected: 5 × (2 + 1) + 5 = 20 seconds
     */
    @Test
    void testKeepAliveTimeout_withCustomShortInterval() {
        // Given: Custom short interval configuration
        final ConnectionOptions connectionOptions = new ConnectionOptions(
                120_000L, // sessionTimeout
                30_000L, // requestTimeout
                5_000L, // keepAliveInterval: 5s
                2, // keepAliveFailuresAllowed: 2
                30_000L, // connectionTimeout
                30_000L, // healthCheckInterval
                "30_000L", // retryInterval
                true, // autoReconnect
                true // reconnectOnServiceFault
                );
        final OpcUaSpecificAdapterConfig config = createConfig(connectionOptions);

        // When: Handler is created
        final OpcUaSubscriptionLifecycleHandler handler = createHandler(config);

        // Then: Timeout should be calculated correctly
        final long expectedTimeout = 5_000 * (2 + 1) + KEEP_ALIVE_SAFETY_MARGIN_MS; // 20 seconds
        assertEquals(expectedTimeout, handler.getKeepAliveTimeoutMs());
    }

    /**
     * Test keep-alive timeout calculation with custom long intervals.
     * Custom: keepAliveInterval=20s, failuresAllowed=5, safetyMargin=5s
     * Expected: 20 × (5 + 1) + 5 = 125 seconds
     */
    @Test
    void testKeepAliveTimeout_withCustomLongInterval() {
        // Given: Custom long interval configuration
        final ConnectionOptions connectionOptions = new ConnectionOptions(
                120_000L, // sessionTimeout
                30_000L, // requestTimeout
                20_000L, // keepAliveInterval: 20s
                5, // keepAliveFailuresAllowed: 5
                30_000L, // connectionTimeout
                30_000L, // healthCheckInterval
                "30_000L", // retryInterval
                true, // autoReconnect
                true // reconnectOnServiceFault
                );
        final OpcUaSpecificAdapterConfig config = createConfig(connectionOptions);

        // When: Handler is created
        final OpcUaSubscriptionLifecycleHandler handler = createHandler(config);

        // Then: Timeout should be calculated correctly
        final long expectedTimeout = 125_000L; // 125 seconds
        assertEquals(expectedTimeout, handler.getKeepAliveTimeoutMs());
    }

    /**
     * Test that health check returns true immediately after handler creation.
     * The lastKeepAliveTimestamp is initialized to current time in constructor.
     */
    @Test
    void testKeepAliveHealthy_immediatelyAfterCreation() {
        // Given: Newly created handler
        final OpcUaSpecificAdapterConfig config = createConfig(ConnectionOptions.defaultConnectionOptions());
        final OpcUaSubscriptionLifecycleHandler handler = createHandler(config);

        // When: Health check is called immediately
        // Then: Should be healthy
        assertTrue(handler.isKeepAliveHealthy(), "Handler should be healthy immediately after creation");
    }

    /**
     * Test that health check returns true within timeout period.
     */
    @Test
    void testKeepAliveHealthy_withinTimeoutPeriod() throws InterruptedException {
        // Given: Handler with default configuration (45s timeout)
        final OpcUaSpecificAdapterConfig config = createConfig(ConnectionOptions.defaultConnectionOptions());
        final OpcUaSubscriptionLifecycleHandler handler = createHandler(config);

        // When: Small amount of time passes (well within timeout)
        Thread.sleep(100);

        // Then: Should still be healthy
        assertTrue(handler.isKeepAliveHealthy(), "Handler should be healthy well within timeout period");
    }

    /**
     * Test that health check returns true shortly before timeout expires.
     * Uses a short timeout and waits just under it to verify boundary behavior.
     */
    @Test
    void testKeepAliveHealthy_shortlyBeforeTimeoutExpires() throws InterruptedException {
        // Given: Handler with short timeout for testing
        // keepAliveInterval=10ms, failuresAllowed=1 -> timeout = 10 × (1+1) + 5000 = 5020ms
        final ConnectionOptions connectionOptions = new ConnectionOptions(
                120_000L, // sessionTimeout
                30_000L, // requestTimeout
                10L, // keepAliveInterval: 10ms
                1, // keepAliveFailuresAllowed: 1
                30_000L, // connectionTimeout
                30_000L, // healthCheckInterval
                "30_000L", // retryInterval
                true, // autoReconnect
                true // reconnectOnServiceFault
                );
        final OpcUaSpecificAdapterConfig config = createConfig(connectionOptions);
        final OpcUaSubscriptionLifecycleHandler handler = createHandler(config);

        // Expected timeout: 5020ms
        // Wait 4800ms (well under timeout but significant time passed)
        Thread.sleep(4800);

        // Then: Should still be healthy (4800ms < 5020ms timeout)
        assertTrue(handler.isKeepAliveHealthy(), "Handler should be healthy shortly before timeout expires");
    }

    /**
     * Test that onKeepAliveReceived updates the timestamp and resets health.
     */
    @Test
    void testOnKeepAliveReceived_resetsHealthTimer() throws InterruptedException {
        // Given: Handler with short timeout
        // keepAliveInterval=100ms, failuresAllowed=1 -> timeout = 100 × (1+1) + 5000 = 5200ms
        final ConnectionOptions connectionOptions = new ConnectionOptions(
                120_000L, // sessionTimeout
                30_000L, // requestTimeout
                100L, // keepAliveInterval: 100ms
                1, // keepAliveFailuresAllowed: 1
                30_000L, // connectionTimeout
                30_000L, // healthCheckInterval
                "30_000L", // retryInterval
                true, // autoReconnect
                true // reconnectOnServiceFault
                );
        final OpcUaSpecificAdapterConfig config = createConfig(connectionOptions);
        final OpcUaSubscriptionLifecycleHandler handler = createHandler(config);

        // When: Some time passes
        Thread.sleep(50);

        // Then: Still healthy
        assertTrue(handler.isKeepAliveHealthy(), "Should be healthy before keep-alive");

        // When: Keep-alive is received (simulated via subscription callback)
        final OpcUaSubscription mockSubscription = org.mockito.Mockito.mock(OpcUaSubscription.class);
        when(mockSubscription.getSubscriptionId()).thenReturn(java.util.Optional.of(uint(12345)));
        handler.onKeepAliveReceived(mockSubscription);

        // Then: Health should be reset and remain healthy
        assertTrue(handler.isKeepAliveHealthy(), "Should be healthy after keep-alive received");
    }

    /**
     * Test the problematic scenario from production (CRASH_ZEISS.md):
     * - Default keep-alive interval: 10s
     * - Default failures allowed: 3
     * - Old hardcoded timeout: 30s (causing false positives)
     * - New calculated timeout: 45s (should prevent false positives)
     */
    @Test
    void testKeepAliveTimeout_productionScenario() {
        // Given: Default production configuration
        final ConnectionOptions connectionOptions = ConnectionOptions.defaultConnectionOptions();
        final OpcUaSpecificAdapterConfig config = createConfig(connectionOptions);
        final OpcUaSubscriptionLifecycleHandler handler = createHandler(config);

        // Then: Timeout should be greater than worst-case keep-alive failure time
        final long worstCaseFailureTime = 10_000 * 3; // 30 seconds (3 failures at 10s interval)
        final long actualTimeout = handler.getKeepAliveTimeoutMs();

        assertTrue(
                actualTimeout > worstCaseFailureTime,
                "Timeout (" + actualTimeout
                        + "ms) should be greater than worst-case failure time ("
                        + worstCaseFailureTime
                        + "ms) to prevent false positives");

        // Verify it has appropriate buffer (should be 45s = 30s + 15s buffer)
        assertEquals(45_000L, actualTimeout, "Should have 15s buffer (10s extra interval + 5s safety margin)");
    }

    /**
     * Test that health check returns false after timeout period expires.
     * This simulates the scenario where OPC UA server stops responding.
     */
    @Test
    void testKeepAliveHealthy_afterTimeoutExpires() throws InterruptedException {
        // Given: Handler with very short timeout for testing
        // keepAliveInterval=20ms, failuresAllowed=1 -> timeout = 20 × (1+1) + 5000 = 5040ms
        final ConnectionOptions connectionOptions = new ConnectionOptions(
                120_000L, // sessionTimeout
                30_000L, // requestTimeout
                20L, // keepAliveInterval: 20ms
                1, // keepAliveFailuresAllowed: 1
                30_000L, // connectionTimeout
                30_000L, // healthCheckInterval
                "30_000L", // retryInterval
                true, // autoReconnect
                true // reconnectOnServiceFault
                );
        final OpcUaSpecificAdapterConfig config = createConfig(connectionOptions);
        final OpcUaSubscriptionLifecycleHandler handler = createHandler(config);

        // When: Wait longer than timeout (5040ms, wait 5100ms to be sure)
        Thread.sleep(5100);

        // Then: Should be unhealthy
        assertFalse(handler.isKeepAliveHealthy(), "Handler should be unhealthy after timeout period expires");
    }

    /**
     * Test multiple keep-alive cycles to ensure timestamp is properly updated.
     */
    @Test
    void testMultipleKeepAliveCycles() throws InterruptedException {
        // Given: Handler with moderate timeout
        // keepAliveInterval=100ms, failuresAllowed=2 -> timeout = 100 × (2+1) + 5000 = 5300ms
        final ConnectionOptions connectionOptions = new ConnectionOptions(
                120_000L, // sessionTimeout
                30_000L, // requestTimeout
                100L, // keepAliveInterval: 100ms
                2, // keepAliveFailuresAllowed: 2
                30_000L, // connectionTimeout
                30_000L, // healthCheckInterval
                "30_000L", // retryInterval
                true, // autoReconnect
                true // reconnectOnServiceFault
                );
        final OpcUaSpecificAdapterConfig config = createConfig(connectionOptions);
        final OpcUaSubscriptionLifecycleHandler handler = createHandler(config);

        final OpcUaSubscription mockSubscription = org.mockito.Mockito.mock(OpcUaSubscription.class);
        when(mockSubscription.getSubscriptionId()).thenReturn(java.util.Optional.of(uint(12345)));

        // When: Multiple keep-alive cycles occur
        for (int i = 0; i < 5; i++) {
            assertTrue(handler.isKeepAliveHealthy(), "Should be healthy in cycle " + i);
            handler.onKeepAliveReceived(mockSubscription);
            Thread.sleep(50); // Wait half the interval
        }

        // Then: Should still be healthy after all cycles
        assertTrue(handler.isKeepAliveHealthy(), "Should remain healthy through multiple cycles");
    }

    /**
     * Test edge case with minimum configuration values.
     */
    @Test
    void testKeepAliveTimeout_minimumConfiguration() {
        // Given: Minimum allowed configuration values
        // keepAliveInterval=1s, failuresAllowed=1 -> timeout = 1000 × (1+1) + 5000 = 7000ms
        final ConnectionOptions connectionOptions = new ConnectionOptions(
                10_000L, // sessionTimeout: 10s (minimum)
                5_000L, // requestTimeout: 5s (minimum)
                1_000L, // keepAliveInterval: 1s (minimum)
                1, // keepAliveFailuresAllowed: 1 (minimum)
                2_000L, // connectionTimeout: 2s (minimum)
                10_000L, // healthCheckInterval: 10s (minimum)
                "5_000L", // retryInterval: 5s (minimum)
                true, // autoReconnect
                true // reconnectOnServiceFault
                );
        final OpcUaSpecificAdapterConfig config = createConfig(connectionOptions);

        // When: Handler is created
        final OpcUaSubscriptionLifecycleHandler handler = createHandler(config);

        // Then: Timeout should be calculated correctly even with minimum values
        final long expectedTimeout = 7_000L; // 7 seconds
        assertEquals(expectedTimeout, handler.getKeepAliveTimeoutMs());
        assertTrue(handler.isKeepAliveHealthy(), "Should be healthy with minimum configuration");
    }

    /**
     * Test keep-alive timeout calculation with maximum allowed configuration values.
     * Maximum: keepAliveInterval=60s, failuresAllowed=10, safetyMargin=5s
     * Expected: 60000 × (10 + 1) + 5000 = 665000ms (11 minutes 5 seconds)
     */
    @Test
    void testKeepAliveTimeout_maximumConfiguration() {
        // Given: Maximum allowed configuration values
        final ConnectionOptions connectionOptions = new ConnectionOptions(
                3600_000L, // sessionTimeout: 1 hour (maximum)
                300_000L, // requestTimeout: 5 min (maximum)
                60_000L, // keepAliveInterval: 60s (maximum)
                10, // keepAliveFailuresAllowed: 10 (maximum)
                300_000L, // connectionTimeout: 5 min (maximum)
                300_000L, // healthCheckInterval: 5 min (maximum)
                "300_000L", // retryInterval: 5 min (maximum)
                true, // autoReconnect
                true // reconnectOnServiceFault
                );
        final OpcUaSpecificAdapterConfig config = createConfig(connectionOptions);

        // When: Handler is created
        final OpcUaSubscriptionLifecycleHandler handler = createHandler(config);

        // Then: Timeout should be calculated correctly with maximum values (no overflow)
        final long expectedTimeout = 60_000L * (10 + 1) + KEEP_ALIVE_SAFETY_MARGIN_MS; // 665000ms = 11 min 5 sec
        assertEquals(expectedTimeout, handler.getKeepAliveTimeoutMs());
        assertTrue(handler.isKeepAliveHealthy(), "Should be healthy with maximum configuration");
    }

    /**
     * Test that the boundary condition at exact timeout returns unhealthy.
     * When timeSinceLastKeepAlive == timeout, the check uses strict less-than,
     * so it should return false (unhealthy).
     */
    @Test
    void testKeepAliveHealthy_atExactTimeoutBoundary() throws InterruptedException {
        // Given: Handler with short timeout for testing
        // keepAliveInterval=10ms, failuresAllowed=1 -> timeout = 10 × (1+1) + 5000 = 5020ms
        final ConnectionOptions connectionOptions = new ConnectionOptions(
                120_000L, // sessionTimeout
                30_000L, // requestTimeout
                10L, // keepAliveInterval: 10ms
                1, // keepAliveFailuresAllowed: 1
                30_000L, // connectionTimeout
                30_000L, // healthCheckInterval
                "30_000L", // retryInterval
                true, // autoReconnect
                true // reconnectOnServiceFault
                );
        final OpcUaSpecificAdapterConfig config = createConfig(connectionOptions);
        final OpcUaSubscriptionLifecycleHandler handler = createHandler(config);
        final long timeout = handler.getKeepAliveTimeoutMs();

        // When: Wait exactly at timeout plus small buffer for timing variance
        Thread.sleep(timeout + 50);

        // Then: Should be unhealthy (uses strict less-than comparison)
        assertFalse(
                handler.isKeepAliveHealthy(), "Handler should be unhealthy when time since last keep-alive >= timeout");
    }

    /**
     * Test that receiving keep-alive after being unhealthy restores health.
     * Simulates recovery scenario where server comes back online.
     */
    @Test
    void testKeepAliveHealthy_recoveryAfterTimeout() throws InterruptedException {
        // Given: Handler with short timeout
        // keepAliveInterval=10ms, failuresAllowed=1 -> timeout = 5020ms
        final ConnectionOptions connectionOptions = new ConnectionOptions(
                120_000L, // sessionTimeout
                30_000L, // requestTimeout
                10L, // keepAliveInterval: 10ms
                1, // keepAliveFailuresAllowed: 1
                30_000L, // connectionTimeout
                30_000L, // healthCheckInterval
                "30_000L", // retryInterval
                true, // autoReconnect
                true // reconnectOnServiceFault
                );
        final OpcUaSpecificAdapterConfig config = createConfig(connectionOptions);
        final OpcUaSubscriptionLifecycleHandler handler = createHandler(config);

        // When: Wait for timeout to expire
        Thread.sleep(5100);

        // Then: Should be unhealthy
        assertFalse(handler.isKeepAliveHealthy(), "Handler should be unhealthy after timeout");

        // When: Keep-alive is received (server recovered)
        final OpcUaSubscription mockSubscription = org.mockito.Mockito.mock(OpcUaSubscription.class);
        when(mockSubscription.getSubscriptionId()).thenReturn(java.util.Optional.of(uint(99999)));
        handler.onKeepAliveReceived(mockSubscription);

        // Then: Should be healthy again
        assertTrue(handler.isKeepAliveHealthy(), "Handler should recover to healthy after receiving keep-alive");
    }

    /**
     * Test that concurrent calls to isKeepAliveHealthy and onKeepAliveReceived are thread-safe.
     * The lastKeepAliveTimestamp field is volatile, ensuring visibility across threads.
     */
    @Test
    void testKeepAliveHealthy_threadSafety() throws InterruptedException {
        // Given: Handler with default configuration
        final OpcUaSpecificAdapterConfig config = createConfig(ConnectionOptions.defaultConnectionOptions());
        final OpcUaSubscriptionLifecycleHandler handler = createHandler(config);

        final OpcUaSubscription mockSubscription = org.mockito.Mockito.mock(OpcUaSubscription.class);
        when(mockSubscription.getSubscriptionId()).thenReturn(java.util.Optional.of(uint(12345)));

        final java.util.concurrent.atomic.AtomicBoolean failed = new java.util.concurrent.atomic.AtomicBoolean(false);
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(2);

        // When: Multiple threads concurrently read and write
        final Thread reader = new Thread(() -> {
            try {
                for (int i = 0; i < 1000; i++) {
                    handler.isKeepAliveHealthy(); // Should not throw
                }
            } catch (final Exception e) {
                failed.set(true);
            } finally {
                latch.countDown();
            }
        });

        final Thread writer = new Thread(() -> {
            try {
                for (int i = 0; i < 1000; i++) {
                    handler.onKeepAliveReceived(mockSubscription);
                }
            } catch (final Exception e) {
                failed.set(true);
            } finally {
                latch.countDown();
            }
        });

        reader.start();
        writer.start();
        assertTrue(latch.await(10, java.util.concurrent.TimeUnit.SECONDS));

        // Then: No exceptions should have occurred
        assertFalse(failed.get(), "Concurrent access should not cause exceptions");
        assertTrue(handler.isKeepAliveHealthy(), "Handler should remain healthy after concurrent operations");
    }

    private OpcUaSubscriptionLifecycleHandler createHandler(final @NotNull OpcUaSpecificAdapterConfig config) {
        return new OpcUaSubscriptionLifecycleHandler(
                metricsService,
                tagStreamingService,
                eventService,
                ADAPTER_ID,
                List.of(createTestTag()),
                opcUaClient,
                dataPointFactory,
                config);
    }
}
