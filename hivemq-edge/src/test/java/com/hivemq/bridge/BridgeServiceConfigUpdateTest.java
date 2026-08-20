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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * EDG-882 F-07: what a configuration change costs the subscriptions it did not touch.
 * <p>
 * The reload path answered any change to a bridge by stopping it with an empty retain list, which
 * clears every queue the bridge owns. Editing one subscription's filter therefore threw away the
 * messages queued for all the others — a bridge with one busy subscription and one being tuned lost
 * the busy one's backlog on every save, with nothing logged and nothing the operator could do.
 */
class BridgeServiceConfigUpdateTest {

    private static final @NotNull String BRIDGE_ID = "edg-882-update-bridge";
    private static final @NotNull String DESTINATION = "{#}";

    private static final @NotNull LocalSubscription KEPT =
            new LocalSubscription(List.of("plant/a", "plant/b"), DESTINATION);
    private static final @NotNull LocalSubscription EDITED_BEFORE =
            new LocalSubscription(List.of("other/x"), DESTINATION);
    private static final @NotNull LocalSubscription EDITED_AFTER =
            new LocalSubscription(List.of("other/y"), DESTINATION);

    private @NotNull BridgeService bridgeService;
    private @NotNull BridgeMqttClientFactory clientFactory;
    private @NotNull MessageForwarder messageForwarder;

    private static @NotNull MqttBridge bridge(final @NotNull List<LocalSubscription> subscriptions) {
        return new MqttBridge.Builder()
                .withId(BRIDGE_ID)
                .withHost("remote.example.com")
                .withPort(1883)
                .withClientId(BRIDGE_ID + "-client")
                .withLocalSubscriptions(subscriptions)
                .withRemoteSubscriptions(List.of())
                .build();
    }

    private static @NotNull MqttForwarder forwarder(final @NotNull LocalSubscription subscription) {
        final MqttForwarder forwarder = mock(MqttForwarder.class);
        when(forwarder.getId()).thenReturn(BridgeMqttClient.createForwarderId(BRIDGE_ID, subscription));
        when(forwarder.getTopics()).thenReturn(subscription.getFilters());
        return forwarder;
    }

    private @NotNull BridgeMqttClient client(
            final @NotNull MqttBridge bridge, final @NotNull List<MqttForwarder> forwarders) {
        final BridgeMqttClient client = mock(BridgeMqttClient.class);
        when(client.getBridge()).thenReturn(bridge);
        when(client.start()).thenReturn(Futures.immediateFuture(null));
        when(client.stop()).thenReturn(Futures.immediateFuture(null));
        when(client.createForwarders()).thenReturn(forwarders);
        when(client.getActiveForwarders()).thenReturn(forwarders);
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
    void updateBridges_whenOneSubscriptionChanges_thenOnlyItsQueueIsCleared() {
        final MqttForwarder keptForwarder = forwarder(KEPT);
        final MqttForwarder editedForwarder = forwarder(EDITED_BEFORE);
        final MqttBridge before = bridge(List.of(KEPT, EDITED_BEFORE));
        final MqttBridge after = bridge(List.of(KEPT, EDITED_AFTER));
        // the clients are built outside the stubbing call: creating a mock inside when(...) leaves
        // Mockito with an unfinished stubbing
        final BridgeMqttClient clientBefore = client(before, List.of(keptForwarder, editedForwarder));
        final BridgeMqttClient clientAfter = client(after, List.of(forwarder(KEPT), forwarder(EDITED_AFTER)));
        when(clientFactory.createRemoteClient(any())).thenReturn(clientBefore).thenReturn(clientAfter);
        bridgeService.updateBridges(List.of(before));

        bridgeService.updateBridges(List.of(after));

        verify(messageForwarder).removeForwarder(keptForwarder, false); // survives the change: its messages stay
        verify(messageForwarder).removeForwarder(editedForwarder, true); // gone from the configuration: cleared
    }

    /**
     * The other half of F-07, at this level: a reorder-only edit is not a change at all, so the bridge
     * must not even be stopped. Restarting it drops the connection to the remote broker and
     * re-subscribes, which is not what writing the same filters in another order should cost.
     */
    @Test
    void updateBridges_whenOnlyTheFilterOrderChanges_thenTheBridgeIsNotRestarted() {
        final MqttForwarder keptForwarder = forwarder(KEPT);
        final MqttBridge before = bridge(List.of(KEPT));
        final MqttBridge reordered = bridge(List.of(new LocalSubscription(List.of("plant/b", "plant/a"), DESTINATION)));
        final BridgeMqttClient clientBefore = client(before, List.of(keptForwarder));
        when(clientFactory.createRemoteClient(any())).thenReturn(clientBefore);
        bridgeService.updateBridges(List.of(before));

        bridgeService.updateBridges(List.of(reordered));

        verify(messageForwarder, never()).removeForwarder(any(), anyBoolean());
        verify(messageForwarder, times(1)).addForwarder(keptForwarder);
    }

    /**
     * F-07 one level up (EDG-882 QA round 1). Canonicalising the filters inside a subscription made a
     * reorder of the filters harmless; the order of the {@code <forwarded-topic>} blocks themselves was
     * still compared positionally, so moving one block above another was a "changed" bridge — restarted,
     * with the queues of everything the new configuration could not match cleared.
     */
    @Test
    void updateBridges_whenOnlyTheSubscriptionOrderChanges_thenTheBridgeIsNotRestarted() {
        final MqttForwarder keptForwarder = forwarder(KEPT);
        final MqttForwarder editedForwarder = forwarder(EDITED_BEFORE);
        final MqttBridge before = bridge(List.of(KEPT, EDITED_BEFORE));
        final MqttBridge reordered = bridge(List.of(EDITED_BEFORE, KEPT));
        final BridgeMqttClient clientBefore = client(before, List.of(keptForwarder, editedForwarder));
        when(clientFactory.createRemoteClient(any())).thenReturn(clientBefore);
        bridgeService.updateBridges(List.of(before));

        bridgeService.updateBridges(List.of(reordered));

        verify(messageForwarder, never()).removeForwarder(any(), anyBoolean());
    }

    /** But a repeated block is a different configuration from a single one, and must still restart. */
    @Test
    void updateBridges_whenASubscriptionIsDuplicated_thenTheBridgeIsRestarted() {
        final MqttForwarder keptForwarder = forwarder(KEPT);
        final MqttBridge before = bridge(List.of(KEPT));
        final MqttBridge duplicated = bridge(List.of(KEPT, KEPT));
        final BridgeMqttClient clientBefore = client(before, List.of(keptForwarder));
        final BridgeMqttClient clientAfter = client(duplicated, List.of(forwarder(KEPT)));
        when(clientFactory.createRemoteClient(any())).thenReturn(clientBefore).thenReturn(clientAfter);
        bridgeService.updateBridges(List.of(before));

        bridgeService.updateBridges(List.of(duplicated));

        verify(messageForwarder).removeForwarder(keptForwarder, false);
    }

    /**
     * EDG-882 QA round 1: only restartBridge used to write allKnownBridgeConfigs, so a bridge that had
     * been stopped through the API kept the configuration it was stopped with. A later start ran a
     * stale subscription set — and the next reload then read the difference as a change and cleared the
     * queues of the subscriptions that had "disappeared".
     */
    @Test
    void updateBridges_whenTheBridgeIsNotRunning_thenTheNewConfigurationIsUsedByTheNextStart() {
        final MqttBridge before = bridge(List.of(EDITED_BEFORE));
        final MqttBridge after = bridge(List.of(EDITED_AFTER));
        final BridgeMqttClient clientBefore = client(before, List.of(forwarder(EDITED_BEFORE)));
        final BridgeMqttClient clientAfter = client(after, List.of(forwarder(EDITED_AFTER)));
        when(clientFactory.createRemoteClient(any())).thenReturn(clientBefore).thenReturn(clientAfter);
        bridgeService.updateBridges(List.of(before));
        bridgeService.stopBridge(BRIDGE_ID, false, List.of());

        bridgeService.updateBridges(List.of(after));
        bridgeService.startBridge(BRIDGE_ID);

        verify(clientFactory).createRemoteClient(after);
    }

    /**
     * EDG-882 QA round 1: constructing the client reads the TLS material, and that statement sat one
     * short of the try that is there precisely so one bad bridge cannot take the synchronization down
     * with it. A mistyped keystore path left every bridge after it in the iteration unstarted.
     */
    @Test
    void updateBridges_whenOneBridgeCannotBeConstructed_thenTheOthersStillStart() {
        final MqttBridge broken = bridge("edg-882-broken", List.of(KEPT));
        final MqttBridge healthy = bridge("edg-882-healthy", List.of(KEPT));
        final BridgeMqttClient healthyClient = client(healthy, List.of(forwarder("edg-882-healthy", KEPT)));
        when(clientFactory.createRemoteClient(broken)).thenThrow(new RuntimeException("unreadable keystore"));
        when(clientFactory.createRemoteClient(healthy)).thenReturn(healthyClient);

        bridgeService.updateBridges(List.of(broken, healthy));

        verify(clientFactory).createRemoteClient(healthy);
        assertNotNull(bridgeService.getLastError("edg-882-broken"));
        assertTrue(bridgeService.isRunning("edg-882-healthy"));
        assertFalse(bridgeService.isRunning("edg-882-broken"));
    }

    /**
     * And the reaping gate opens even then. It is what tells the periodic clean-up that a forwarder
     * queue nobody owns is genuinely abandoned; left closed, forwarder queues are never reclaimed again
     * for the life of the node — a storage leak in place of a message loss, and silent either way.
     */
    /**
     * And on a node with no bridges at all. The gate is what tells the periodic clean-up that a
     * forwarder queue nobody owns is genuinely abandoned; if it only opened when a bridge started, a
     * node whose bridges were all removed would never reclaim the queues they left behind.
     */
    @Test
    void updateBridges_whenThereAreNoBridgesAtAll_thenTheReapingGateIsStillOpened() {
        bridgeService.updateBridges(List.of());

        verify(messageForwarder).markBridgeConfigurationApplied();
    }

    @Test
    void updateBridges_whenABridgeThrows_thenTheReapingGateIsStillOpened() {
        final MqttBridge broken = bridge("edg-882-broken", List.of(KEPT));
        when(clientFactory.createRemoteClient(any())).thenThrow(new RuntimeException("unreadable keystore"));

        bridgeService.updateBridges(List.of(broken));

        verify(messageForwarder).markBridgeConfigurationApplied();
    }

    /**
     * EDG-882 QA round 2: a start that failed part way left the forwarders it had already registered
     * live and polling, draining persisted messages into a client that was never started and would
     * never reconnect. Nothing is cleared on the way out — the queues stay, held by the reservation.
     */
    @Test
    void internalStartBridge_whenTheSecondForwarderFailsToRegister_thenTheFirstIsUnregistered() {
        final MqttForwarder first = forwarder(KEPT);
        final MqttForwarder second = forwarder(EDITED_BEFORE);
        final MqttBridge bridge = bridge(List.of(KEPT, EDITED_BEFORE));
        // built outside the stubbing call: a mock created inside when(...) leaves Mockito with an
        // unfinished stubbing
        final BridgeMqttClient bridgeClient = client(bridge, List.of(first, second));
        when(clientFactory.createRemoteClient(any())).thenReturn(bridgeClient);
        doThrow(new IllegalStateException("already registered"))
                .when(messageForwarder)
                .addForwarder(second);

        bridgeService.updateBridges(List.of(bridge));

        verify(messageForwarder).removeForwarder(first, false);
        verify(messageForwarder, never()).removeForwarder(eq(first), eq(true));
        verify(messageForwarder, never()).releaseReservedQueues(BRIDGE_ID);
        assertNotNull(bridgeService.getLastError(BRIDGE_ID));
    }

    private static @NotNull MqttBridge bridge(
            final @NotNull String bridgeId, final @NotNull List<LocalSubscription> subscriptions) {
        return new MqttBridge.Builder()
                .withId(bridgeId)
                .withHost("remote.example.com")
                .withPort(1883)
                .withClientId(bridgeId + "-client")
                .withLocalSubscriptions(subscriptions)
                .withRemoteSubscriptions(List.of())
                .build();
    }

    private static @NotNull MqttForwarder forwarder(
            final @NotNull String bridgeId, final @NotNull LocalSubscription subscription) {
        final MqttForwarder forwarder = mock(MqttForwarder.class);
        when(forwarder.getId()).thenReturn(BridgeMqttClient.createForwarderId(bridgeId, subscription));
        when(forwarder.getTopics()).thenReturn(subscription.getFilters());
        return forwarder;
    }
}
