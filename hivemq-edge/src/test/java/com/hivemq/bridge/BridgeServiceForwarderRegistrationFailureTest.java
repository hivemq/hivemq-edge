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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.bridge.config.LocalSubscription;
import com.hivemq.bridge.config.MqttBridge;
import com.hivemq.bridge.mqtt.BridgeMqttClient;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.reader.BridgeExtractor;
import com.hivemq.edge.HiveMQEdgeRemoteService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * EDG-882 F-01, at the level that has to survive the rejection.
 * <p>
 * A bridge whose local subscriptions collide on a forwarder id cannot start. What the service must do
 * with that is report it and carry on: {@code updateBridges} walks every configured bridge, so an
 * exception escaping the start of one would leave every bridge after it in the iteration unstarted —
 * turning one unusable bridge into an outage of all of them.
 * <p>
 * The rejected bridge must also be left strictly alone: nothing registered, nothing started, and — the
 * point of the whole ticket — no queue cleared, so its messages are still there once the configuration
 * is corrected.
 */
class BridgeServiceForwarderRegistrationFailureTest {

    private @NotNull BridgeService bridgeService;
    private @NotNull BridgeMqttClientFactory clientFactory;
    private @NotNull MessageForwarder messageForwarder;

    private static @NotNull MqttBridge bridge(final @NotNull String bridgeId) {
        return bridge(bridgeId, List.of());
    }

    private static @NotNull MqttBridge bridge(
            final @NotNull String bridgeId, final @NotNull List<LocalSubscription> localSubscriptions) {
        return new MqttBridge.Builder()
                .withId(bridgeId)
                .withHost("remote.example.com")
                .withPort(1883)
                .withClientId(bridgeId + "-client")
                .withLocalSubscriptions(localSubscriptions)
                .withRemoteSubscriptions(List.of())
                .build();
    }

    private static @NotNull BridgeMqttClient healthyClient(final @NotNull MqttBridge bridge) {
        final BridgeMqttClient client = mock(BridgeMqttClient.class);
        when(client.getBridge()).thenReturn(bridge);
        when(client.start()).thenReturn(Futures.immediateFuture(null));
        when(client.stop()).thenReturn(Futures.immediateFuture(null));
        when(client.getActiveForwarders()).thenReturn(List.of());
        when(client.createForwarders()).thenReturn(List.of());
        return client;
    }

    private static @NotNull BridgeMqttClient rejectingClient(
            final @NotNull MqttBridge bridge, final @NotNull IllegalStateException rejection) {
        final BridgeMqttClient client = healthyClient(bridge);
        when(client.createForwarders()).thenThrow(rejection);
        return client;
    }

    @BeforeEach
    void setUp() {
        clientFactory = mock(BridgeMqttClientFactory.class);
        messageForwarder = mock(MessageForwarder.class);
        bridgeService = new BridgeService(
                mock(BridgeExtractor.class),
                messageForwarder,
                clientFactory,
                MoreExecutors.newDirectExecutorService(),
                mock(HiveMQEdgeRemoteService.class),
                new ShutdownHooks(),
                new MetricRegistry());
    }

    @Test
    void updateBridges_whenForwarderRegistrationIsRejected_thenReportedAndNothingIsRegisteredOrStarted() {
        final MqttBridge colliding = bridge("colliding-bridge");
        final IllegalStateException rejection = new IllegalStateException("two subscriptions share a forwarder id");
        final BridgeMqttClient client = rejectingClient(colliding, rejection);
        when(clientFactory.createRemoteClient(any())).thenReturn(client);

        assertDoesNotThrow(() -> bridgeService.updateBridges(List.of(colliding)));

        assertSame(rejection, bridgeService.getLastError("colliding-bridge"), "the rejection must be reported");
        verify(messageForwarder, never()).addForwarder(any());
        verify(client, never()).start();
        // and above all: nothing was cleared, so the queues survive until the configuration is fixed
        verify(messageForwarder, never()).removeForwarder(any(), anyBoolean());
    }

    /**
     * The half of the refusal that the customer feels. A bridge that cannot start registers no
     * forwarder, so its persisted queues read as unowned — and the periodic clean-up deletes unowned
     * forwarder queues within seconds of the node coming up. Refusing without holding them would only
     * change which line of code destroys the messages.
     * <p>
     * The hold covers both colliding subscriptions. Their forwarder id is the same by construction, so
     * a hold built by keying on it must merge the two filter lists rather than keep whichever came
     * last — otherwise half the queues are left to the clean-up.
     */
    @Test
    void updateBridges_whenForwarderRegistrationIsRejected_thenTheQueuesOfEverySubscriptionAreHeld() {
        final LocalSubscription split = new LocalSubscription(List.of("ab", "c"), "remote/dest");
        final LocalSubscription joined = new LocalSubscription(List.of("a", "bc"), "remote/dest");
        assertEquals(split.calculateUniqueId(), joined.calculateUniqueId(), "the premise: one id, two queue sets");

        final MqttBridge colliding = bridge("colliding-bridge", List.of(split, joined));
        final BridgeMqttClient client = rejectingClient(colliding, new IllegalStateException("colliding ids"));
        when(clientFactory.createRemoteClient(any())).thenReturn(client);

        bridgeService.updateBridges(List.of(colliding));

        final ArgumentCaptor<Map<String, List<String>>> held = ArgumentCaptor.captor();
        verify(messageForwarder).reserveQueues(eq("colliding-bridge"), held.capture());
        final String forwarderId = "colliding-bridge-" + split.calculateUniqueId();
        assertEquals(Set.of(forwarderId), held.getValue().keySet());
        assertEquals(
                List.of("ab", "c", "a", "bc"),
                held.getValue().get(forwarderId),
                "a queue of either subscription that is not held is a queue the clean-up will delete");
    }

    /** A bridge that starts owns its queues through its forwarders; the hold must not outlive that. */
    @Test
    void updateBridges_whenTheBridgeStarts_thenNoQueuesAreHeldForIt() {
        final MqttBridge healthy = bridge("healthy-bridge");
        final BridgeMqttClient client = healthyClient(healthy);
        when(clientFactory.createRemoteClient(any())).thenReturn(client);

        bridgeService.updateBridges(List.of(healthy));

        verify(messageForwarder, never()).reserveQueues(any(), any());
        verify(messageForwarder).releaseReservedQueues("healthy-bridge");
    }

    /**
     * Held queues are reclaimable once the bridge is gone from the configuration — and only then.
     * Releasing on any stop would drop the hold as a node shuts down, letting a last clean-up pass
     * delete the messages the operator is coming back for.
     */
    @Test
    void updateBridges_whenTheRefusedBridgeIsRemoved_thenTheHoldIsReleased() {
        final MqttBridge colliding = bridge("colliding-bridge");
        final BridgeMqttClient client = rejectingClient(colliding, new IllegalStateException("colliding ids"));
        when(clientFactory.createRemoteClient(any())).thenReturn(client);
        bridgeService.updateBridges(List.of(colliding));
        verify(messageForwarder, never()).releaseReservedQueues("colliding-bridge");

        bridgeService.updateBridges(List.of());

        verify(messageForwarder).releaseReservedQueues("colliding-bridge");
    }

    /**
     * The reason the rejection is caught rather than allowed to propagate. {@code toAdd} is iterated in
     * hash order, so an escaping exception would take down an arbitrary subset of the other bridges —
     * a failure that would reproduce differently on every run.
     */
    @Test
    void updateBridges_whenOneBridgeIsRejected_thenTheOthersStillStart() {
        final MqttBridge colliding = bridge("colliding-bridge");
        final MqttBridge healthy = bridge("healthy-bridge");
        final MqttBridge alsoHealthy = bridge("also-healthy-bridge");
        final BridgeMqttClient collidingClient =
                rejectingClient(colliding, new IllegalStateException("two subscriptions share a forwarder id"));
        final BridgeMqttClient healthyClient = healthyClient(healthy);
        final BridgeMqttClient alsoHealthyClient = healthyClient(alsoHealthy);
        when(clientFactory.createRemoteClient(any())).thenAnswer(invocation -> {
            final MqttBridge requested = invocation.getArgument(0);
            return switch (requested.getId()) {
                case "colliding-bridge" -> collidingClient;
                case "healthy-bridge" -> healthyClient;
                default -> alsoHealthyClient;
            };
        });

        bridgeService.updateBridges(List.of(healthy, colliding, alsoHealthy));

        verify(healthyClient, times(1)).start();
        verify(alsoHealthyClient, times(1)).start();
        assertNull(bridgeService.getLastError("healthy-bridge"));
        assertNull(bridgeService.getLastError("also-healthy-bridge"));
    }

    @Test
    void updateBridges_whenTheConfigurationIsCorrected_thenTheBridgeStartsAndTheErrorClears() {
        final MqttBridge colliding = bridge("bridge");
        // built outside the stubbing call: creating a mock inside when(...) confuses Mockito
        final BridgeMqttClient collidingClient =
                rejectingClient(colliding, new IllegalStateException("two subscriptions share an id"));
        when(clientFactory.createRemoteClient(any())).thenReturn(collidingClient);
        bridgeService.updateBridges(List.of(colliding));
        requireNonNull(bridgeService.getLastError("bridge"), "the rejection must have been recorded");

        // the operator edits the offending filter: same bridge id, a configuration that now resolves
        final MqttBridge corrected = new MqttBridge.Builder()
                .withId("bridge")
                .withHost("remote.example.com")
                .withPort(8883)
                .withClientId("bridge-client")
                .withLocalSubscriptions(List.of())
                .withRemoteSubscriptions(List.of())
                .build();
        final BridgeMqttClient correctedClient = healthyClient(corrected);
        when(clientFactory.createRemoteClient(any())).thenReturn(correctedClient);

        bridgeService.updateBridges(List.of(corrected));

        verify(correctedClient, times(1)).start();
        assertNull(bridgeService.getLastError("bridge"), "the recorded rejection must not outlive the fix");
        // the restart path clears the queues of the forwarders it removes; the rejected bridge has none,
        // so what accumulated while the configuration was broken is still there to be forwarded
        verify(messageForwarder, never()).removeForwarder(any(), anyBoolean());
    }
}
