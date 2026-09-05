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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.reader.BridgeExtractor;
import com.hivemq.edge.HiveMQEdgeRemoteService;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientsession.ClientSessionSubscriptionPersistence;
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
     * And on a node with no bridges at all. The gate is what tells the periodic clean-up that a
     * forwarder queue nobody owns is genuinely abandoned; if it only opened when a bridge started, a
     * node whose bridges were all removed would never reclaim the queues they left behind.
     */
    @Test
    void updateBridges_whenThereAreNoBridgesAtAll_thenTheReapingGateIsStillOpened() {
        bridgeService.updateBridges(List.of());

        verify(messageForwarder).markBridgeConfigurationApplied();
    }

    /**
     * And the reaping gate opens even when a bridge fails. Left closed, forwarder queues are never
     * reclaimed again for the life of the node — a storage leak in place of a message loss, and silent
     * either way.
     */
    @Test
    void updateBridges_whenABridgeThrows_thenTheReapingGateIsStillOpened() {
        final MqttBridge broken = bridge("edg-882-broken", List.of(KEPT));
        when(clientFactory.createRemoteClient(any())).thenThrow(new RuntimeException("unreadable keystore"));

        bridgeService.updateBridges(List.of(broken));

        verify(messageForwarder).markBridgeConfigurationApplied();
    }

    /**
     * EDG-882 review v02, R2-07. A bridge the operator stopped by hand must stay stopped.
     * <p>
     * {@code bridgeNameToLastError} is written by two different things: a bridge that could not be
     * <em>started</em>, and a bridge that started fine but could not <em>connect</em>. Only a later
     * successful connect cleared it. The reload path reads it as "this bridge failed and its
     * configuration has just been corrected, so try again" — which for the second kind, after the
     * operator had deliberately stopped it, means the next unrelated edit to the file starts a bridge
     * they turned off.
     */
    @Test
    void updateBridges_whenAStoppedBridgeHadAConnectFailure_thenAReloadDoesNotStartItAgain() {
        final MqttBridge before = bridge(List.of(KEPT));
        final MqttBridge after = bridge(List.of(KEPT, EDITED_AFTER));
        final BridgeMqttClient clientBefore = client(before, List.of(forwarder(KEPT)));
        // it starts, and then fails to reach the remote -- which is what writes the error
        when(clientBefore.start())
                .thenReturn(Futures.immediateFailedFuture(new RuntimeException("remote unreachable")));
        when(clientFactory.createRemoteClient(any())).thenReturn(clientBefore);

        bridgeService.updateBridges(List.of(before));
        assertNotNull(bridgeService.getLastError(BRIDGE_ID), "the connect failure must have been recorded");

        bridgeService.stopBridge(BRIDGE_ID, false, List.of());
        bridgeService.updateBridges(List.of(after));

        assertFalse(
                bridgeService.isRunning(BRIDGE_ID),
                "a reload restarted a bridge the operator had stopped, because its old connect error was"
                        + " still on record");
        verify(clientFactory, times(1)).createRemoteClient(any());
    }

    /** Thrown where {@code internalStartBridge}'s {@code catch (Exception)} cannot see it. */
    private static final class SynchronizationError extends Error {
        private SynchronizationError() {
            super("out of stack reading the keystore");
        }
    }

    /**
     * EDG-882 review v02, R2-03. The gate opens in a {@code finally}, so it opens whether or not every
     * bridge got its turn — and each bridge claims its own queues only when its turn comes, inside
     * {@code internalStartBridge}. Anything escaping the synchronization part way therefore used to open
     * the gate with the bridges after it neither registered nor held, and the periodic clean-up reclaims
     * a forwarder queue nobody holds. On the first call at boot that is every configured bridge, and
     * what it reclaims is the backlog they accumulated before the restart.
     * <p>
     * Asserted against a real {@link MessageForwarderImpl}, because the reading that decides whether a
     * queue is deleted is {@code isForwarderQueue}, and a mock would answer whatever the test wanted.
     * Both halves are asserted: the gate must open <em>and</em> the queues must be held. Either one
     * alone passes for the wrong reason — a gate that stayed shut would also leave the queues intact.
     */
    @Test
    void updateBridges_whenTheSynchronizationThrows_thenTheUnstartedBridgesStillHoldTheirQueues() {
        final MessageForwarder realForwarder = new MessageForwarderImpl(
                mock(LocalTopicTree.class),
                new HivemqId(),
                () -> null,
                () -> mock(ClientSessionSubscriptionPersistence.class),
                mock(SingleWriterService.class),
                new ShutdownHooks());
        final BridgeService service = new BridgeService(
                mock(BridgeExtractor.class),
                realForwarder,
                clientFactory,
                MoreExecutors.newDirectExecutorService(),
                mock(HiveMQEdgeRemoteService.class),
                new ShutdownHooks(),
                new MetricRegistry());
        final List<MqttBridge> bridges = List.of(
                bridge("edg-882-one", List.of(KEPT)),
                bridge("edg-882-two", List.of(KEPT)),
                bridge("edg-882-three", List.of(KEPT)));
        // On the very first invocation, so no bridge has been through internalStartBridge. Which bridge
        // draws it does not matter and must not: toAdd is a HashSet, so the iteration order is the hash
        // order, and a test that named a position would be pinning that instead of the behaviour.
        when(clientFactory.createRemoteClient(any())).thenThrow(new SynchronizationError());

        assertThrows(SynchronizationError.class, () -> service.updateBridges(bridges));

        assertTrue(realForwarder.hasAppliedBridgeConfiguration(), "the reaping gate never opened");
        for (final MqttBridge configured : bridges) {
            for (final String filter : KEPT.getFilters()) {
                final String queueId = MessageForwarderImpl.FORWARDER_PREFIX
                        + BridgeMqttClient.createForwarderId(configured.getId(), KEPT) + "/" + filter;
                assertTrue(
                        realForwarder.isForwarderQueue(queueId),
                        "queue '" + queueId + "' reads as orphaned, so the clean-up will delete it");
            }
        }
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
