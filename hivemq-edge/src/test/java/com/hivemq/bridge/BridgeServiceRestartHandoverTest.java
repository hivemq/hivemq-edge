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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableSet;
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
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * EDG-882 F-02: a bridge queue that a restart keeps must never read as unowned while it is being
 * handed from one generation of forwarders to the next.
 * <p>
 * {@code restartBridge} deliberately does not clear the queues of forwarders that survive into the new
 * configuration. Between the two generations, though, the old forwarders have been removed and
 * released and the replacements are not registered yet — and the periodic clean-up, which runs on its
 * own schedule and clears every forwarder queue no registered forwarder owns, does not know that a
 * restart is in progress. A sweep landing in that gap deletes exactly the messages the restart was
 * careful to keep, and the bridge comes back to an empty queue with nothing logged.
 * <p>
 * The gap cannot be observed from outside {@link BridgeService}, so it is observed from inside: the
 * real {@link MessageForwarderImpl} is wired in, and the mocked client answers at each point of the
 * hand-over by recording what {@code isForwarderQueue} says at that instant. Every recording must be
 * {@code true} — that is what the clean-up would have read.
 */
class BridgeServiceRestartHandoverTest {

    private static final @NotNull String BRIDGE_ID = "edg-882-restart-bridge";
    private static final @NotNull String FILTER = "plant/line1/from-plc";
    private static final @NotNull String DESTINATION = "{#}";

    private @NotNull BridgeService bridgeService;
    private @NotNull BridgeMqttClientFactory clientFactory;
    private @NotNull MessageForwarderImpl messageForwarder;

    /** Every answer to "does a registered forwarder own this queue?" taken during a hand-over. */
    private final @NotNull List<Boolean> ownershipDuringHandover = new ArrayList<>();

    private static @NotNull MqttBridge bridge(final @NotNull String filter) {
        return new MqttBridge.Builder()
                .withId(BRIDGE_ID)
                .withHost("remote.example.com")
                .withPort(1883)
                .withClientId(BRIDGE_ID + "-client")
                .withLocalSubscriptions(List.of(new LocalSubscription(List.of(filter), DESTINATION)))
                .withRemoteSubscriptions(List.of())
                .build();
    }

    private static @NotNull String forwarderId(final @NotNull String filter) {
        return BridgeMqttClient.createForwarderId(BRIDGE_ID, new LocalSubscription(List.of(filter), DESTINATION));
    }

    private static @NotNull String queueId(final @NotNull String filter) {
        return MessageForwarderImpl.FORWARDER_PREFIX + forwarderId(filter) + "/" + filter;
    }

    private @NotNull MqttForwarder forwarder(final @NotNull String filter) {
        final MqttForwarder forwarder = mock(MqttForwarder.class);
        when(forwarder.getId()).thenReturn(forwarderId(filter));
        when(forwarder.getTopics()).thenReturn(List.of(filter));
        // the moment the old generation lets go
        org.mockito.Mockito.doAnswer(invocation -> {
                    ownershipDuringHandover.add(messageForwarder.isForwarderQueue(queueId(FILTER)));
                    return null;
                })
                .when(forwarder)
                .stop();
        return forwarder;
    }

    private @NotNull BridgeMqttClient client(final @NotNull MqttBridge bridge, final @NotNull String filter) {
        final BridgeMqttClient client = mock(BridgeMqttClient.class);
        final MqttForwarder forwarder = forwarder(filter);
        when(client.getBridge()).thenReturn(bridge);
        when(client.start()).thenReturn(Futures.immediateFuture(null));
        when(client.stop()).thenReturn(Futures.immediateFuture(null));
        when(client.getActiveForwarders()).thenReturn(List.of(forwarder));
        // the widest point of the gap: the old generation is gone, the new one not registered yet
        when(client.createForwarders()).thenAnswer(invocation -> {
            ownershipDuringHandover.add(messageForwarder.isForwarderQueue(queueId(FILTER)));
            return List.of(forwarder);
        });
        return client;
    }

    private final @NotNull ClientSessionSubscriptionPersistence subscriptionPersistence =
            mock(ClientSessionSubscriptionPersistence.class);

    @BeforeEach
    void setUp() {
        clientFactory = mock(BridgeMqttClientFactory.class);
        final LocalTopicTree topicTree = mock(LocalTopicTree.class);
        when(topicTree.getSharedSubscriber(anyString(), anyString())).thenReturn(ImmutableSet.of());
        messageForwarder = new MessageForwarderImpl(
                topicTree,
                new HivemqId(),
                () -> null,
                () -> subscriptionPersistence,
                mock(SingleWriterService.class),
                mock(ShutdownHooks.class));
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
    @Timeout(10)
    void restartBridge_whenTheSubscriptionSurvives_thenItsQueueIsOwnedThroughout() {
        final MqttBridge configured = bridge(FILTER);
        when(clientFactory.createRemoteClient(any())).thenAnswer(invocation -> client(configured, FILTER));
        bridgeService.updateBridges(List.of(configured));
        assertTrue(messageForwarder.isForwarderQueue(queueId(FILTER)), "the bridge did not register its queue");
        ownershipDuringHandover.clear();

        bridgeService.restartBridge(BRIDGE_ID, configured);

        assertFalse(ownershipDuringHandover.isEmpty(), "the hand-over was never observed; this run proves nothing");
        assertFalse(
                ownershipDuringHandover.contains(false),
                "a retained queue read as unowned during the restart; a clean-up sweep there deletes its messages "
                        + "and the bridge comes back empty. Readings in order: " + ownershipDuringHandover);
        assertTrue(messageForwarder.isForwarderQueue(queueId(FILTER)), "the replacement must own it afterwards");
    }

    /**
     * The hold is not a leak. Once the replacement forwarders own the queues the hold is dropped, so a
     * later removal of the bridge still makes them reclaimable — otherwise a restarted bridge could
     * never have its queues cleaned up again, and every restart would strand another set for the life
     * of the node.
     */
    @Test
    @Timeout(10)
    void restartBridge_whenTheBridgeIsAfterwardsRemoved_thenItsQueuesBecomeReclaimable() {
        final MqttBridge configured = bridge(FILTER);
        when(clientFactory.createRemoteClient(any())).thenAnswer(invocation -> client(configured, FILTER));
        bridgeService.updateBridges(List.of(configured));
        bridgeService.restartBridge(BRIDGE_ID, configured);
        assertTrue(messageForwarder.isForwarderQueue(queueId(FILTER)));

        bridgeService.updateBridges(List.of());

        assertFalse(
                messageForwarder.isForwarderQueue(queueId(FILTER)),
                "the restart hold outlived the bridge; the queue can now never be reclaimed");
    }

    /**
     * A subscription that does not survive the restart is a different matter: its queue is cleared by
     * the stop, and nothing must go on holding it afterwards.
     */
    @Test
    @Timeout(10)
    void restartBridge_whenTheSubscriptionChanges_thenTheOldQueueIsNotHeld() {
        final String newFilter = "plant/line2/from-plc";
        final MqttBridge configured = bridge(FILTER);
        final MqttBridge updated = bridge(newFilter);
        when(clientFactory.createRemoteClient(any())).thenAnswer(invocation -> {
            final MqttBridge requested = invocation.getArgument(0);
            return client(
                    requested,
                    requested.getLocalSubscriptions().getFirst().getFilters().getFirst());
        });
        bridgeService.updateBridges(List.of(configured));

        bridgeService.restartBridge(BRIDGE_ID, updated);

        assertTrue(messageForwarder.isForwarderQueue(queueId(newFilter)), "the new subscription must own its queue");
        assertFalse(
                messageForwarder.isForwarderQueue(queueId(FILTER)),
                "the queue of a subscription that was removed and cleared must be reclaimable");
    }
}
