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
package com.hivemq.bridge.mqtt;

import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.api.mqtt.PublishReturnCode;
import com.hivemq.bridge.config.LocalSubscription;
import com.hivemq.bridge.config.MqttBridge;
import com.hivemq.bridge.metrics.PerBridgeMetrics;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult;
import com.hivemq.mqtt.message.publish.PUBLISH;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import util.TestMessageUtil;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Reproducer tests for the bridge message forwarding stall issue.
 * <p>
 * These tests document the buggy behavior that causes the bridge to permanently stall
 * after connection failures during message forwarding. The tests PASS with the current
 * code, demonstrating the problematic behavior exists.
 * <p>
 * When the bugs are fixed, some of these tests will need to be updated to reflect
 * the corrected behavior.
 * <p>
 * See FINDINGS_BRIDGE.md for detailed root cause analysis.
 */
class RemoteMqttForwarderReconnectStallTest {

    private final @NotNull BridgeMqttClient bridgeClient = mock(BridgeMqttClient.class);
    private final @NotNull Mqtt5AsyncClient mqtt5AsyncClient = mock(Mqtt5AsyncClient.class);
    private final @NotNull MetricRegistry metricRegistry = new MetricRegistry();
    private final @NotNull ExecutorService executorService = MoreExecutors.newDirectExecutorService();

    private RemoteMqttForwarder forwarder;
    private AtomicInteger afterForwardCallbackCount;
    private AtomicInteger resetInflightMarkerCallbackCount;

    @BeforeEach
    void setup() {
        afterForwardCallbackCount = new AtomicInteger(0);
        resetInflightMarkerCallbackCount = new AtomicInteger(0);
        forwarder = createForwarder();
        when(bridgeClient.getMqtt5Client()).thenReturn(mqtt5AsyncClient);
    }

    @Nested
    @DisplayName("Bug 1: Inflight counter remains high while disconnected")
    class InflightCounterImbalanceTests {

        /**
         * This test demonstrates that when messages are buffered during disconnect,
         * the inflightCounter remains elevated because finishProcessing() is never
         * called for buffered messages until they are drained.
         * <p>
         * This causes MessageForwarderImpl.pollForBuffer() to stop polling when
         * inflightCount exceeds FORWARDER_POLL_THRESHOLD_MESSAGES (32).
         */
        @Test
        @DisplayName("inflightCounter stays elevated while messages are buffered during disconnect")
        void inflightCounter_staysElevatedWhileBuffered() {
            when(bridgeClient.isConnected()).thenReturn(false);
            forwarder.start();

            // Send messages while disconnected - they get buffered
            final PUBLISH publish1 = TestMessageUtil.createMqtt5Publish("topic1");
            final PUBLISH publish2 = TestMessageUtil.createMqtt5Publish("topic2");
            forwarder.onMessage(publish1, "queue1");
            forwarder.onMessage(publish2, "queue1");

            // BUG: inflightCounter is 2 but messages are just sitting in buffer
            // No callbacks are triggered, so MessageForwarderImpl thinks processing is in progress
            final int inflightWhileBuffered = forwarder.getInflightCount();

            // This demonstrates the problem: counter is elevated while messages aren't actually in-flight
            assertEquals(2, inflightWhileBuffered,
                    "inflightCounter should be 2 while messages are buffered (this is the bug)");

            // No afterForwardCallback was called - system doesn't know messages are "paused"
            assertEquals(0, afterForwardCallbackCount.get(),
                    "afterForwardCallback should not have been called for buffered messages");
        }

        /**
         * This test shows that after draining, the counter returns to zero.
         * The issue is the PERIOD when messages are buffered - the counter stays high.
         */
        @Test
        @DisplayName("inflightCounter returns to zero after drain completes")
        void inflightCounter_returnsToZeroAfterDrain() {
            when(bridgeClient.isConnected()).thenReturn(false);
            forwarder.start();

            forwarder.onMessage(TestMessageUtil.createMqtt5Publish("topic1"), "queue1");
            forwarder.onMessage(TestMessageUtil.createMqtt5Publish("topic2"), "queue1");

            assertEquals(2, forwarder.getInflightCount(), "2 messages buffered");

            // Reconnect and drain
            when(bridgeClient.isConnected()).thenReturn(true);
            when(mqtt5AsyncClient.publish(any())).thenReturn(
                    CompletableFuture.completedFuture(mock(Mqtt5PublishResult.class)));

            forwarder.drainQueue();

            // After drain completes, counter returns to 0
            assertEquals(0, forwarder.getInflightCount(),
                    "inflightCounter should be 0 after drain completes");
        }

        /**
         * This test shows the contrast: onMessage() correctly increments inflightCounter,
         * so when messages complete, the counter stays balanced.
         */
        @Test
        @DisplayName("onMessage() correctly balances inflight counter when connected")
        void onMessage_correctlyBalancesInflightCounter() {
            when(bridgeClient.isConnected()).thenReturn(true);
            when(mqtt5AsyncClient.publish(any())).thenReturn(
                    CompletableFuture.completedFuture(mock(Mqtt5PublishResult.class)));

            forwarder.start();

            final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic");
            forwarder.onMessage(publish, "queue1");

            // After message completes, counter should be back to 0
            assertEquals(0, forwarder.getInflightCount(),
                    "inflightCounter should be 0 after message completes via onMessage()");
        }
    }

    @Nested
    @DisplayName("Bug 2: Missing finishProcessing() when buffering messages")
    class MissingFinishProcessingTests {

        /**
         * This test demonstrates that when sendPublishToRemote() buffers a message
         * (because the client is disconnected), it does NOT call finishProcessing().
         * <p>
         * This means:
         * 1. inflightCounter is incremented but never decremented for buffered messages
         * 2. afterForwardCallback is never called, so MessageForwarderImpl doesn't
         * know the message processing is "paused"
         * 3. The persistence layer's inflight marker is never cleared
         */
        @Test
        @DisplayName("buffered messages should trigger callback or be properly tracked")
        void bufferedMessages_shouldTriggerCallbackOrBeProperlyTracked() {
            when(bridgeClient.isConnected()).thenReturn(false);
            forwarder.start();

            final int initialInflightCount = forwarder.getInflightCount();
            assertEquals(0, initialInflightCount);

            // Send message while disconnected
            final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic");
            forwarder.onMessage(publish, "queue1");

            // BUG: inflightCounter is incremented but message is just buffered
            // without any notification to the callback
            final int inflightAfterBuffering = forwarder.getInflightCount();

            // The inflight counter grows unbounded while disconnected
            // This will eventually cause pollForBuffer() to skip polling
            // when inflightCount > FORWARDER_POLL_THRESHOLD_MESSAGES (32)
            assertEquals(1, inflightAfterBuffering,
                    "inflightCounter should be 1 after buffering (this is the problematic behavior)");

            // The callback was NOT called for the buffered message
            // This means MessageForwarderImpl thinks the message is still being processed
            assertEquals(0, afterForwardCallbackCount.get(),
                    "afterForwardCallback should NOT have been called for buffered message " +
                            "(this demonstrates the bug - no feedback to the system)");
        }

        /**
         * This test shows that after multiple messages are buffered during disconnect,
         * the inflight counter grows and is never decremented until drainQueue() runs.
         */
        @Test
        @DisplayName("multiple buffered messages cause unbounded inflight counter growth")
        void multipleBufferedMessages_causeUnboundedInflightCounterGrowth() {
            when(bridgeClient.isConnected()).thenReturn(false);
            forwarder.start();

            // Buffer many messages while disconnected
            for (int i = 0; i < 50; i++) {
                final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic" + i);
                forwarder.onMessage(publish, "queue1");
            }

            // BUG: inflightCounter is now 50, but no messages have been sent
            // When inflightCount > 32 (FORWARDER_POLL_THRESHOLD_MESSAGES),
            // MessageForwarderImpl.pollForBuffer() will skip polling entirely
            assertEquals(50, forwarder.getInflightCount(),
                    "inflightCounter grows unbounded while disconnected");

            // No callbacks were triggered
            assertEquals(0, afterForwardCallbackCount.get(),
                    "No callbacks triggered for buffered messages");
        }
    }

    @Nested
    @DisplayName("Bug 3: Publish failure does not clear inflight marker")
    class PublishFailureInflightMarkerTests {

        /**
         * This test demonstrates that when a publish fails, the afterForwardCallback
         * is called (which triggers removeShared()), but resetInflightMarkerCallback
         * is NOT called.
         * <p>
         * This means failed messages are removed from the queue entirely rather than
         * having their inflight marker cleared for retry.
         */
        @Test
        @DisplayName("publish failure should clear inflight marker for retry")
        void publishFailure_shouldClearInflightMarkerForRetry() {
            when(bridgeClient.isConnected()).thenReturn(true);

            // Setup: publish will fail
            final CompletableFuture<Mqtt5PublishResult> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new RuntimeException("Connection lost"));
            when(mqtt5AsyncClient.publish(any())).thenReturn(failedFuture);

            forwarder.start();

            final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic");
            forwarder.onMessage(publish, "queue1");

            // afterForwardCallback WAS called (message is removed from queue)
            assertEquals(1, afterForwardCallbackCount.get(),
                    "afterForwardCallback should be called on failure");

            // BUG: resetInflightMarkerCallback was NOT called
            // This means the message in the persistence queue still has its inflight
            // marker set, and will be skipped on subsequent readShared() calls
            assertEquals(0, resetInflightMarkerCallbackCount.get(),
                    "resetInflightMarkerCallback should be called on failure to allow retry " +
                            "(this assertion documents the bug - marker is not cleared)");
        }
    }

    @Nested
    @DisplayName("Combined scenario: Reconnection stall")
    class ReconnectionStallTests {

        /**
         * This test demonstrates the full reconnection stall scenario:
         * 1. Messages are being forwarded normally
         * 2. Connection drops, some messages fail with ChannelOutputShutdownException
         * 3. More messages arrive and get buffered (inflightCounter grows)
         * 4. Connection is restored, drainQueue() is called
         * 5. drainQueue() sends buffered messages without incrementing inflightCounter
         * 6. Messages complete, inflightCounter goes negative
         * 7. System is in inconsistent state
         */
        @Test
        @DisplayName("rapid disconnect/reconnect causes inflight counter inconsistency")
        void rapidDisconnectReconnect_causesInflightCounterInconsistency() {
            forwarder.start();

            // Phase 1: Normal operation - some messages in flight
            when(bridgeClient.isConnected()).thenReturn(true);
            final CompletableFuture<Mqtt5PublishResult> pendingFuture1 = new CompletableFuture<>();
            final CompletableFuture<Mqtt5PublishResult> pendingFuture2 = new CompletableFuture<>();
            when(mqtt5AsyncClient.publish(any()))
                    .thenReturn(pendingFuture1)
                    .thenReturn(pendingFuture2);

            forwarder.onMessage(TestMessageUtil.createMqtt5Publish("topic1"), "queue1");
            forwarder.onMessage(TestMessageUtil.createMqtt5Publish("topic2"), "queue1");

            assertEquals(2, forwarder.getInflightCount(), "2 messages in flight");

            // Phase 2: Connection drops - messages fail
            when(bridgeClient.isConnected()).thenReturn(false);
            pendingFuture1.completeExceptionally(new RuntimeException("Channel output shutdown"));
            pendingFuture2.completeExceptionally(new RuntimeException("Channel output shutdown"));

            // After failures, inflight should be 0
            assertEquals(0, forwarder.getInflightCount(), "inflight should be 0 after failures");

            // Phase 3: More messages arrive while disconnected - they get buffered
            forwarder.onMessage(TestMessageUtil.createMqtt5Publish("topic3"), "queue1");
            forwarder.onMessage(TestMessageUtil.createMqtt5Publish("topic4"), "queue1");

            assertEquals(2, forwarder.getInflightCount(),
                    "2 more messages buffered, inflight is 2");

            // Phase 4: Reconnection - drainQueue() is called
            when(bridgeClient.isConnected()).thenReturn(true);
            when(mqtt5AsyncClient.publish(any())).thenReturn(
                    CompletableFuture.completedFuture(mock(Mqtt5PublishResult.class)));

            forwarder.drainQueue();

            // BUG: drainQueue() didn't increment inflightCounter, but finishProcessing()
            // decremented it twice (once per drained message)
            // Expected: 0
            // Actual: -2 (or some negative number)
            final int finalInflightCount = forwarder.getInflightCount();

            assertTrue(finalInflightCount >= 0,
                    "inflightCounter should not be negative after reconnection cycle. " +
                            "Actual value: " + finalInflightCount);
        }

        /**
         * This test verifies that drainQueue() sends the correct number of messages.
         */
        @Test
        @DisplayName("drainQueue() sends all buffered messages")
        void drainQueue_sendsAllBufferedMessages() {
            when(bridgeClient.isConnected()).thenReturn(false);
            forwarder.start();

            // Buffer 5 messages
            for (int i = 0; i < 5; i++) {
                forwarder.onMessage(TestMessageUtil.createMqtt5Publish("topic" + i), "queue1");
            }

            // Reconnect and drain
            when(bridgeClient.isConnected()).thenReturn(true);
            when(mqtt5AsyncClient.publish(any())).thenReturn(
                    CompletableFuture.completedFuture(mock(Mqtt5PublishResult.class)));

            forwarder.drainQueue();

            // Verify all 5 messages were sent
            verify(mqtt5AsyncClient, times(5)).publish(any());
        }
    }

    private @NotNull RemoteMqttForwarder createForwarder() {
        final LocalSubscription localSubscription = new LocalSubscription(
                List.of("#"),
                "{#}",
                List.of(),
                List.of(),
                false,
                2,
                1000L);

        final MqttBridge bridge = new MqttBridge.Builder()
                .withId("test-bridge")
                .withHost("localhost")
                .withClientId("test-client")
                .withLocalSubscriptions(List.of(localSubscription))
                .build();

        final RemoteMqttForwarder forwarder = new RemoteMqttForwarder(
                "test-forwarder",
                bridge,
                localSubscription,
                bridgeClient,
                new PerBridgeMetrics("test-bridge", metricRegistry),
                new TestInterceptorHandler());

        forwarder.setExecutorService(executorService);
        forwarder.setAfterForwardCallback((qos, uniqueId, queueId, cancelled) -> {
            afterForwardCallbackCount.incrementAndGet();
        });
        forwarder.setResetInflightMarkerCallback((sharedSubscription, uniqueId) -> {
            resetInflightMarkerCallbackCount.incrementAndGet();
        });

        return forwarder;
    }

    private static class TestInterceptorHandler implements BridgeInterceptorHandler {
        @Override
        public @NotNull ListenableFuture<PublishReturnCode> interceptOrDelegateInbound(
                final @NotNull PUBLISH publish,
                final @NotNull ExecutorService executorService,
                final @NotNull MqttBridge bridge) {
            return Futures.immediateFuture(PublishReturnCode.DELIVERED);
        }

        @Override
        public @NotNull ListenableFuture<InterceptorResult> interceptOrDelegateOutbound(
                final @NotNull PUBLISH publish,
                final @NotNull ExecutorService executorService,
                final @NotNull MqttBridge bridge) {
            return Futures.immediateFuture(new InterceptorResult(InterceptorOutcome.SUCCESS, publish));
        }
    }
}
