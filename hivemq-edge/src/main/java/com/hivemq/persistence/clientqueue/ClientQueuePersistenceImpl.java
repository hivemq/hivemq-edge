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
package com.hivemq.persistence.clientqueue;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.persistence.clientsession.SharedSubscriptionServiceImpl.SharedSubscription;
import static com.hivemq.sampling.SamplingService.SAMPLER_PREFIX;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.ImmutableIntArray;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.bridge.MessageForwarder;
import com.hivemq.bridge.MessageForwarderImpl;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.mqtt.message.MessageWithID;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.mqtt.services.PublishPollService;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.AbstractPersistence;
import com.hivemq.persistence.ProducerQueues;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientsession.ClientSession;
import com.hivemq.persistence.clientsession.SharedSubscriptionServiceImpl;
import com.hivemq.persistence.connection.ConnectionPersistence;
import com.hivemq.persistence.local.ClientSessionLocalPersistence;
import com.hivemq.persistence.payload.PayloadPersistenceException;
import com.hivemq.sampling.SamplingService;
import com.hivemq.util.Checkpoints;
import dagger.Lazy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@SuppressWarnings("FutureReturnValueIgnored")
public class ClientQueuePersistenceImpl extends AbstractPersistence implements ClientQueuePersistence {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ClientQueuePersistenceImpl.class);

    public static final int SHARED_IN_FLIGHT_MARKER = 1;

    private final @NotNull ClientQueueLocalPersistence localPersistence;
    private final @NotNull ProducerQueues singleWriter;
    private final @NotNull MqttConfigurationService mqttConfigurationService;
    private final @NotNull ClientSessionLocalPersistence clientSessionLocalPersistence;
    private final @NotNull MessageDroppedService messageDroppedService;
    private final @NotNull LocalTopicTree topicTree;
    private final @NotNull ConnectionPersistence connectionPersistence;
    private final @NotNull Lazy<PublishPollService> publishPollService;
    private final @NotNull MessageForwarder messageForwarder;
    private final @NotNull ShutdownHooks shutdownHooks;
    private final @NotNull Map<String, PublishAvailableCallback> queueidCallbackMap;

    @Inject
    public ClientQueuePersistenceImpl(
            final @NotNull ClientQueueLocalPersistence localPersistence,
            final @NotNull SingleWriterService singleWriterService,
            final @NotNull MqttConfigurationService mqttConfigurationService,
            final @NotNull ClientSessionLocalPersistence clientSessionLocalPersistence,
            final @NotNull MessageDroppedService messageDroppedService,
            final @NotNull LocalTopicTree topicTree,
            final @NotNull ConnectionPersistence connectionPersistence,
            final @NotNull Lazy<PublishPollService> publishPollService,
            final @NotNull MessageForwarder messageForwarder,
            final @NotNull ShutdownHooks shutdownHooks) {
        this.shutdownHooks = shutdownHooks;
        this.localPersistence = localPersistence;
        this.mqttConfigurationService = mqttConfigurationService;
        this.clientSessionLocalPersistence = clientSessionLocalPersistence;
        this.messageDroppedService = messageDroppedService;
        this.topicTree = topicTree;
        this.connectionPersistence = connectionPersistence;
        this.publishPollService = publishPollService;
        singleWriter = singleWriterService.getQueuedMessagesQueue();
        this.messageForwarder = messageForwarder;
        this.queueidCallbackMap = new ConcurrentHashMap<>();
    }

    @Override
    @NotNull
    public ListenableFuture<Void> add(
            final @NotNull String queueId,
            final boolean shared,
            final @NotNull PUBLISH publish,
            final boolean retained,
            final long queueLimit,
            final @NotNull QueuePolicy policy) {
        try {
            checkNotNull(queueId, "Queue ID must not be null");
            checkNotNull(publish, "Publish must not be null");
        } catch (final Exception exception) {
            return Futures.immediateFailedFuture(exception);
        }

        return singleWriter.submit(queueId, (bucketIndex) -> {
            // What to do when the queue is full comes from the producer (see QueuePolicy). It used to be
            // read off the queue ID -- a share name a client chooses -- so an ordinary subscription to
            // $share/$SAMPLER::customer/alerts had its own messages discarded under a policy meant for
            // diagnostics (EDG-882 F-05).
            final MqttConfigurationService.QueuedMessagesStrategy queuedMessagesStrategy =
                    policy == QueuePolicy.SAMPLE_RING
                            ? MqttConfigurationService.QueuedMessagesStrategy.DISCARD_OLDEST
                            : mqttConfigurationService.getQueuedMessagesStrategy();
            final boolean applyMaxToQos0 = policy == QueuePolicy.SAMPLE_RING;

            localPersistence.add(
                    queueId,
                    shared,
                    publish,
                    queueLimit,
                    queuedMessagesStrategy,
                    retained,
                    applyMaxToQos0,
                    bucketIndex);
            final int queueSize = localPersistence.size(queueId, shared, bucketIndex);
            if (queueSize == 1) {
                if (shared) {
                    sharedPublishAvailable(queueId);
                } else {
                    publishAvailable(queueId);
                }
            }
            return null;
        });
    }

    @Override
    @NotNull
    public ListenableFuture<Void> add(
            final @NotNull String queueId,
            final boolean shared,
            final @NotNull List<PUBLISH> publishes,
            final boolean retained,
            final long queueLimit,
            final @NotNull QueuePolicy policy) {
        try {
            checkNotNull(queueId, "Queue ID must not be null");
            checkNotNull(publishes, "Publishes must not be null");
        } catch (final Exception exception) {
            return Futures.immediateFailedFuture(exception);
        }

        return singleWriter.submit(queueId, (bucketIndex) -> {
            final boolean queueWasEmpty = localPersistence.size(queueId, shared, bucketIndex) == 0;
            // What to do when the queue is full comes from the producer (see QueuePolicy). It used to be
            // read off the queue ID -- a share name a client chooses -- so an ordinary subscription to
            // $share/$SAMPLER::customer/alerts had its own messages discarded under a policy meant for
            // diagnostics (EDG-882 F-05).
            final MqttConfigurationService.QueuedMessagesStrategy queuedMessagesStrategy =
                    policy == QueuePolicy.SAMPLE_RING
                            ? MqttConfigurationService.QueuedMessagesStrategy.DISCARD_OLDEST
                            : mqttConfigurationService.getQueuedMessagesStrategy();
            final boolean applyMaxToQos0 = policy == QueuePolicy.SAMPLE_RING;

            localPersistence.add(
                    queueId,
                    shared,
                    publishes,
                    queueLimit,
                    queuedMessagesStrategy,
                    retained,
                    applyMaxToQos0,
                    bucketIndex);
            if (queueWasEmpty) {
                if (shared) {
                    sharedPublishAvailable(queueId);
                } else {
                    publishAvailable(queueId);
                }
            }
            return null;
        });
    }

    @Override
    public void publishAvailable(final @NotNull String client) {
        final PublishAvailableCallback availableCallback = queueidCallbackMap.get(client);
        if (availableCallback != null) {
            availableCallback.onPublishAvailable(client);
            return;
        }

        final ClientSession session = clientSessionLocalPersistence.getSession(client);
        if (session == null || !session.isConnected()) {
            return;
        }

        final ClientConnection clientConnection = connectionPersistence.get(client);
        if (clientConnection == null || !clientConnection.getChannel().isActive()) {
            return;
        }

        if (clientConnection.isMessagesInFlight()) {
            return;
        }
        clientConnection.getChannel().eventLoop().submit(() -> publishPollService
                .get()
                .pollNewMessages(client, clientConnection.getChannel()));
    }

    @Override
    public void sharedPublishAvailable(final @NotNull String queueId) {
        // Asked of the forwarder registry rather than read off the queue ID, for the same reason the
        // producer side asks (EDG-882 F-05): a queue ID is built from a share name, and a share name is
        // the client's to choose. A client subscribing to $share/$FORWARDER::anything/t used to have its
        // notification handed to the message forwarder, which owns no such queue -- so the client was
        // never told to poll, and the ID stayed in the forwarder's notEmptyQueues set for the life of
        // the node, once per distinct share name the client cared to invent.
        if (messageForwarder.isForwarderQueue(queueId)) {
            messageForwarder.messageAvailable(queueId);
        } else {
            final PublishAvailableCallback availableCallback = queueidCallbackMap.get(queueId);
            if (availableCallback != null) {
                availableCallback.onPublishAvailable(queueId);
            } else {
                publishPollService.get().pollSharedPublishes(queueId);
            }
        }
    }

    @Override
    public void addPublishAvailableCallback(
            final @NotNull PublishAvailableCallback callback, final @NotNull String queueId) {
        queueidCallbackMap.put(queueId, callback);
    }

    @Override
    public void removePublishAvailableCallback(final @NotNull String queueId) {
        queueidCallbackMap.remove(queueId);
    }

    @Override
    @NotNull
    public ListenableFuture<ImmutableList<PUBLISH>> readNew(
            final @NotNull String queueId,
            final boolean shared,
            final @NotNull ImmutableIntArray packetIds,
            final long byteLimit) {
        try {
            checkNotNull(queueId, "Queue ID must not be null");
            checkNotNull(packetIds, "Message ID's must not be null");
        } catch (final Exception exception) {
            return Futures.immediateFailedFuture(exception);
        }
        return singleWriter.submit(
                queueId,
                (bucketIndex) -> checkPayloadReference(
                        localPersistence.readNew(queueId, shared, packetIds, byteLimit, bucketIndex), queueId, shared));
    }

    @Override
    public @NotNull ListenableFuture<ImmutableList<PUBLISH>> peek(
            final @NotNull String queueId, final boolean shared, final long byteLimit, final int maxMessages) {
        try {
            checkNotNull(queueId, "Queue ID must not be null");
        } catch (final Exception exception) {
            return Futures.immediateFailedFuture(exception);
        }
        return singleWriter.submit(
                queueId,
                (bucketIndex) -> checkPayloadReference(
                        localPersistence.peek(queueId, shared, byteLimit, maxMessages, bucketIndex), queueId, shared));
    }

    @NotNull
    private <T extends MessageWithID> ImmutableList<T> checkPayloadReference(
            final @NotNull ImmutableList<T> publishes, final @NotNull String queueId, final boolean shared) {
        List<T> reducedList = null;
        for (final T message : publishes) {
            if (message instanceof PUBLISH publish) {
                try {
                    publish.dereferencePayload();
                } catch (final PayloadPersistenceException e) {
                    messageDroppedService.failed(
                            queueId, publish.getTopic(), publish.getQoS().getQosNumber());
                    if (shared) {
                        removeShared(queueId, publish.getUniqueId());
                    } else {
                        remove(queueId, publish.getPacketIdentifier());
                    }
                    if (reducedList == null) {
                        reducedList = new ArrayList<>(publishes);
                    }
                    reducedList.remove(message);
                }
            }
        }
        if (reducedList == null) {
            return publishes;
        }
        return ImmutableList.copyOf(reducedList);
    }

    @Override
    @NotNull
    public ListenableFuture<ImmutableList<PUBLISH>> readShared(
            final @NotNull String sharedSubscription, final int messageLimit, final long byteLimit) {
        checkNotNull(sharedSubscription, "Shared subscription must not be null");
        // We reuse the non shared read new logic but without providing real message ID's.
        final ImmutableIntArray.Builder builder = ImmutableIntArray.builder(messageLimit);
        for (int i = 0; i < messageLimit; i++) {
            builder.add(SHARED_IN_FLIGHT_MARKER); // We don't need a real message id here, messages are just marked as
            // in-flight
        }
        return readNew(sharedSubscription, true, builder.build(), byteLimit);
    }

    @Override
    @NotNull
    public ListenableFuture<ImmutableList<MessageWithID>> readInflight(
            final @NotNull String client, final long byteLimit, final int messageLimit) {
        checkNotNull(client, "Client ID must not be null");
        return singleWriter.submit(client, (bucketIndex) -> {
            final ImmutableList<MessageWithID> messages =
                    localPersistence.readInflight(client, false, messageLimit, byteLimit, bucketIndex);
            return checkPayloadReference(messages, client, false);
        });
    }

    @Override
    @NotNull
    public ListenableFuture<Void> remove(final @NotNull String client, final int packetId) {
        checkNotNull(client, "Client ID must not be null");
        return singleWriter.submit(client, (bucketIndex) -> {
            localPersistence.remove(client, packetId, bucketIndex);
            return null;
        });
    }

    @Override
    @NotNull
    public ListenableFuture<Void> putPubrel(final @NotNull String client, final int packetId) {
        checkNotNull(client, "Client must not be null");
        return singleWriter.submit(client, (bucketIndex) -> {
            localPersistence.replace(client, new PUBREL(packetId), bucketIndex);
            return null;
        });
    }

    @Override
    @NotNull
    public ListenableFuture<Void> clear(final @NotNull String queueId, final boolean shared) {
        checkNotNull(queueId, "Queue ID must not be");
        return singleWriter.submit(queueId, (bucketIndex) -> {
            localPersistence.clear(queueId, shared, bucketIndex);
            return null;
        });
    }

    @Override
    @NotNull
    public ListenableFuture<Void> closeDB() {
        return closeDB(localPersistence, singleWriter);
    }

    @Override
    @NotNull
    public ListenableFuture<Void> cleanUp(final int bucketIndex) {
        return singleWriter.submit(bucketIndex, (bucketIndex1) -> {
            final ImmutableSet<String> sharedQueues = localPersistence.cleanUp(bucketIndex1);
            // Expiry above still runs; reclaiming abandoned queues does not, once the node is going
            // down. The bridge shutdown hook has priority HIGH and runs early, and it un-registers every
            // forwarder -- so from that moment until the process exits, every live bridge queue reads as
            // unowned while this job is still scheduled, and a sweep landing there clears the backlog
            // the shutdown was careful not to clear. The start-up side of exactly this window is the
            // hasAppliedBridgeConfiguration gate in isOrphaned; this is its missing other half
            // (EDG-882 QA round 1). Nothing leaks: whatever is genuinely abandoned is still there for
            // the next start's first sweep.
            if (shutdownHooks.isShuttingDown()) {
                if (log.isDebugEnabled()) {
                    log.debug("Node is shutting down, skipping reclamation of {} shared queue(s)", sharedQueues.size());
                }
            } else {
                for (final String sharedQueue : sharedQueues) {
                    if (isOrphaned(sharedQueue)) {
                        localPersistence.clear(sharedQueue, true, bucketIndex);
                    }
                }
            }
            // Visited once per bucket, after the sweep has finished, so that a test can wait for the
            // thing it means to observe. Regressions for the queues this clean-up used to delete had to
            // sleep for long enough that a pass had "probably" happened, which passes just as green on a
            // node where the job stopped being scheduled at all -- the one failure the sleep was there
            // to catch (EDG-882 F-09). A checkpoint is inert unless a test enables it.
            Checkpoints.checkpoint(ClientQueuePersistence.CLIENT_QUEUE_CLEAN_UP_FINISHED);
            return null;
        });
    }

    /**
     * A shared queue is orphaned when no subscriber holds it any more. Queue IDs are
     * {@code <share name>/<topic filter>}, but two internal producers put a '/' inside the share name
     * itself — bridge forwarders through the Base64 subscription hash, samplers through the sampled
     * topic — so splitting at the first '/' resolves an owner that never existed and would clear a
     * live queue. Both shapes are therefore resolved by their own convention, and a queue is only
     * declared orphaned when no reading of it finds an owner.
     */
    private boolean isOrphaned(final @NotNull String queueId) {
        if (queueId.startsWith(MessageForwarderImpl.FORWARDER_PREFIX)) {
            // Before any bridge configuration has been applied, "no forwarder owns this" means the
            // bridges have not started yet rather than that the queue is abandoned. This service is
            // scheduled during persistence bootstrap and the bridge subsystem is built after it, so a
            // sweep in between would delete the queues of every bridge on the node.
            if (!messageForwarder.hasAppliedBridgeConfiguration()) {
                return false;
            }
            if (messageForwarder.isForwarderQueue(queueId)) {
                return false;
            }
        }
        final SharedSubscription sharedSubscription = SharedSubscriptionServiceImpl.splitTopicAndGroup(queueId);
        if (!topicTree
                .getSharedSubscriber(sharedSubscription.getShareName(), sharedSubscription.getTopicFilter())
                .isEmpty()) {
            return false;
        }
        final String sampledTopic = SamplingService.extractSampledTopic(queueId);
        return sampledTopic == null
                || topicTree
                        .getSharedSubscriber(SAMPLER_PREFIX + sampledTopic, sampledTopic)
                        .isEmpty();
    }

    @Override
    @NotNull
    public ListenableFuture<Integer> size(final @NotNull String queueId, final boolean shared) {
        return singleWriter.submit(queueId, (bucketIndex) -> localPersistence.size(queueId, shared, bucketIndex));
    }

    @NotNull
    @Override
    public ListenableFuture<Void> removeShared(
            final @NotNull String sharedSubscription, final @NotNull String uniqueId) {
        return singleWriter.submit(sharedSubscription, (bucketIndex) -> {
            localPersistence.removeShared(sharedSubscription, uniqueId, bucketIndex);
            return null;
        });
    }

    @NotNull
    @Override
    public ListenableFuture<Void> removeInFlightMarker(
            final @NotNull String sharedSubscription, final @NotNull String uniqueId) {
        return singleWriter.submit(sharedSubscription, (bucketIndex) -> {
            localPersistence.removeInFlightMarker(sharedSubscription, uniqueId, bucketIndex);
            // We notify the clients that there are new messages to poll.
            sharedPublishAvailable(sharedSubscription);
            return null;
        });
    }

    @NotNull
    @Override
    public ListenableFuture<Void> removeAllInFlightMarkers(final @NotNull String sharedSubscription) {
        return singleWriter.submit(sharedSubscription, (bucketIndex) -> {
            localPersistence.removeAllInFlightMarkers(sharedSubscription, bucketIndex);
            // We notify the clients that there are new messages to poll.
            sharedPublishAvailable(sharedSubscription);
            return null;
        });
    }

    @NotNull
    @Override
    public ListenableFuture<Void> removeAllQos0Messages(final @NotNull String queueId, final boolean shared) {
        return singleWriter.submit(queueId, (bucketIndex) -> {
            localPersistence.removeAllQos0Messages(queueId, shared, bucketIndex);
            return null;
        });
    }

    public static class Key implements Comparable<Key> {

        @NotNull
        private final String queueId;

        private final boolean shared;

        public Key(final @NotNull String queueId, final boolean shared) {
            this.queueId = queueId;
            this.shared = shared;
        }

        @NotNull
        public String getQueueId() {
            return queueId;
        }

        public boolean isShared() {
            return shared;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Key key)) {
                return false;
            }
            return shared == key.shared && Objects.equals(queueId, key.queueId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(queueId, shared);
        }

        @Override
        public int compareTo(final @NotNull Key other) {
            int compare = queueId.compareTo(other.queueId);
            if (compare == 0) {
                compare = Boolean.compare(shared, other.shared);
            }
            return compare;
        }
    }
}
