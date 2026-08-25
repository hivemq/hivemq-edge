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

import static com.hivemq.configuration.service.InternalConfigurations.FORWARDER_POLL_THRESHOLD_MESSAGES;
import static com.hivemq.configuration.service.InternalConfigurations.PUBLISH_POLL_BATCH_SIZE_BYTES;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.common.shutdown.HiveMQShutdownHook;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.services.PublishDistributorImpl;
import com.hivemq.mqtt.topic.SubscriberWithQoS;
import com.hivemq.mqtt.topic.SubscriptionFlag;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.persistence.clientsession.ClientSessionSubscriptionPersistence;
import com.hivemq.persistence.util.FutureUtils;
import com.hivemq.util.Checkpoints;
import com.hivemq.util.ThreadFactoryUtil;
import dagger.Lazy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@SuppressWarnings("FutureReturnValueIgnored")
public class MessageForwarderImpl implements MessageForwarder {

    public static final @NotNull String FORWARDER_PREFIX = "$FORWARDER::";

    /** How a client writes a shared subscription, and therefore how one is stored for its session. */
    private static final @NotNull String SHARED_SUBSCRIPTION_PREFIX = "$share/";

    private static final @NotNull Logger log = LoggerFactory.getLogger(MessageForwarderImpl.class);
    public static final int RESET_INFLIGHT_COUNTERS_TIMEOUT_IN_SECONDS = 30;

    private final @NotNull LocalTopicTree topicTree;
    private final @NotNull HivemqId hivemqId;
    private final @NotNull Lazy<ClientQueuePersistence> queuePersistence;
    private final @NotNull Lazy<ClientSessionSubscriptionPersistence> subscriptionPersistence;
    private final @NotNull SingleWriterService singleWriterService;
    private final @NotNull Set<String> notEmptyQueues;
    private final @NotNull Map<String, MqttForwarder> forwarders;
    private final @NotNull Map<String, Set<String>> queueIdsForForwarder;
    /**
     * How many registered forwarders own each queue ID. Exactly the multiset union of the values of
     * {@link #queueIdsForForwarder}, maintained so {@link #isForwarderQueue(String)} answers in
     * constant time instead of scanning every forwarder.
     */
    private final @NotNull Map<String, Integer> forwarderQueueRefs;
    /**
     * Queue sets held for bridges that could not register their forwarders, keyed by reservation id.
     * Counted in {@link #forwarderQueueRefs} exactly as a forwarder's own set is, which is what keeps
     * the periodic clean-up off them; see {@link MessageForwarder#reserveQueues}.
     */
    private final @NotNull Map<String, Set<String>> reservedQueues;

    /**
     * Whether the bridge configuration has been applied at least once. Read by the periodic clean-up
     * through {@link #hasAppliedBridgeConfiguration()}; see {@link MessageForwarder} for why.
     */
    private volatile boolean bridgeConfigurationApplied;

    private final @NotNull ExecutorService executorService;
    private final @NotNull Lock pollLock;
    private volatile boolean polling;
    private volatile boolean pollAgain;

    @Inject
    public MessageForwarderImpl(
            final @NotNull LocalTopicTree topicTree,
            final @NotNull HivemqId hivemqId,
            final @NotNull Lazy<ClientQueuePersistence> queuePersistence,
            final @NotNull Lazy<ClientSessionSubscriptionPersistence> subscriptionPersistence,
            final @NotNull SingleWriterService singleWriterService,
            final @NotNull ShutdownHooks shutdownHooks) {
        this.topicTree = topicTree;
        this.hivemqId = hivemqId;
        this.queuePersistence = queuePersistence;
        this.subscriptionPersistence = subscriptionPersistence;
        this.singleWriterService = singleWriterService;
        this.notEmptyQueues = new ConcurrentSkipListSet<>();
        this.forwarders = new ConcurrentHashMap<>(0);
        this.queueIdsForForwarder = new ConcurrentHashMap<>(0);
        this.forwarderQueueRefs = new ConcurrentHashMap<>(0);
        this.reservedQueues = new ConcurrentHashMap<>(0);
        this.pollLock = new ReentrantLock();
        final int threadCount = InternalConfigurations.BRIDGE_MESSAGE_FORWARDER_POOL_THREADS_COUNT.get();
        this.executorService =
                Executors.newScheduledThreadPool(threadCount, ThreadFactoryUtil.create("bridge-message-forwarder-%d"));

        if (log.isDebugEnabled()) {
            log.debug("MessageForwarder initialized with {} thread(s) for bridge message forwarding", threadCount);
        }

        shutdownHooks.add(new HiveMQShutdownHook() {
            @Override
            public @NotNull String name() {
                return "MessageForwarder-Shutdown";
            }

            @Override
            public void run() {
                if (!executorService.isShutdown()) {
                    if (log.isDebugEnabled()) {
                        log.debug(
                                "Shutting down MessageForwarder executor service with {} active forwarder(s)",
                                forwarders.size());
                    }
                    try {
                        executorService.shutdown();
                        if (!executorService.awaitTermination(1, TimeUnit.MILLISECONDS)) {
                            log.warn("MessageForwarder executor did not terminate gracefully, forcing shutdown");
                            executorService.shutdownNow();
                        } else {
                            if (log.isDebugEnabled()) {
                                log.debug("MessageForwarder executor service shutdown complete");
                            }
                        }
                    } catch (final Throwable e) {
                        log.warn("Error encountered while shutting down MessageForwarder executor service", e);
                    }
                }
            }
        });
    }

    private static @NotNull String createQueueId(final @NotNull String forwarderId, final @NotNull String topic) {
        return FORWARDER_PREFIX + forwarderId + "/" + topic;
    }

    /**
     * Claims one reference on each queue ID. Every {@code retain} is paired with exactly one
     * {@link #release(Set)} of the same set — the set itself when the registration is refused, or the
     * value the {@code remove} returns — which is what keeps the counts exact under any interleaving.
     */
    private void retain(final @NotNull Set<String> queueIds) {
        for (final String queueId : queueIds) {
            forwarderQueueRefs.merge(queueId, 1, Integer::sum);
        }
    }

    /** Drops one reference on each queue ID, removing the entry when the last owner lets go. */
    private void release(final @NotNull Set<String> queueIds) {
        for (final String queueId : queueIds) {
            forwarderQueueRefs.computeIfPresent(queueId, (key, count) -> count == 1 ? null : count - 1);
        }
    }

    @Override
    public void addForwarder(final @NotNull MqttForwarder mqttForwarder) {
        final String forwarderId = mqttForwarder.getId();
        final String shareName = FORWARDER_PREFIX + forwarderId;
        final String clientId = shareName + "#" + hivemqId.get();

        if (log.isDebugEnabled()) {
            log.debug(
                    "Adding forwarder '{}' for {} topic(s): {}",
                    forwarderId,
                    mqttForwarder.getTopics().size(),
                    mqttForwarder.getTopics());
        }

        final ImmutableSet.Builder<@NotNull String> queueIdsBuilder = ImmutableSet.builder();
        for (final String topic : mqttForwarder.getTopics()) {
            queueIdsBuilder.add(createQueueId(forwarderId, topic));
        }
        final ImmutableSet<@NotNull String> queueIds = queueIdsBuilder.build();
        evictForeignSubscribers(forwarderId, mqttForwarder.getTopics());
        // Ownership is registered before anything else can observe the queues: the periodic clean-up
        // clears forwarder queues it finds unowned, so a pre-existing persisted queue must never be
        // visible while its forwarder is mid-registration.
        //
        // The statement order below is load-bearing. None of these four rules can be pinned by a test
        // without a seam into this class -- they are intra-method orderings only observable from
        // another thread -- so they are enforced here and in review. Getting any of them wrong loses
        // customer messages silently.
        //
        //   O0  ownership is claimed before the topicTree.addTopic loop, or a persisted queue becomes
        //       pollable while still reading as unowned.
        //   O1  retain() runs before the put. forwarderQueueRefs -- not queueIdsForForwarder -- is the
        //       map isForwarderQueue reads, so it, not the put, is the moment registration takes
        //       effect. Publishing first opens a window in which the clean-up clears a live queue.
        //   O2  retain(new) runs before release(superseded). For a queue in both sets the count would
        //       otherwise go 1 -> absent -> 1, and a sweep landing in that instant clears a queue that
        //       is live, registered and in the current sweep set.
        //   O3  see removeForwarder.
        //
        // A forwarder ID does not imply one queue set -- the ID embeds a digest over the filters
        // joined with an empty separator, so {"ab","c"} and {"a","bc"} share an ID with different
        // queue sets -- which is why a registration may not displace an existing one under the same
        // ID: the displaced queue set would be un-owned while its forwarder is still live and
        // polling, and the next clean-up sweep would delete the messages in it. Distinct IDs are
        // established one level up, by BridgeMqttClient.verifyForwarderIdsAreUnique; this is the
        // invariant that makes the index sound on its own, and it is enforced rather than assumed.
        //
        // putIfAbsent is the claim, so two threads racing the same ID cannot both proceed; the loser
        // undoes exactly the references it took, which for a queue ID it shares with the incumbent
        // moves the count 1 -> 2 -> 1 and never through zero (O2 again).
        retain(queueIds);
        final Set<String> incumbent = queueIdsForForwarder.putIfAbsent(forwarderId, queueIds);
        if (incumbent != null) {
            release(queueIds);
            throw new IllegalStateException("Forwarder '"
                    + forwarderId
                    + "' is already registered with queues "
                    + incumbent
                    + "; registering it again with queues "
                    + queueIds
                    + " would leave the first set un-owned while it is still live. Forwarder IDs must be"
                    + " unique among registered forwarders, and a forwarder must be removed before an"
                    + " object with the same ID is added.");
        }
        // Everything from here to the start() below is fallible, and a registration that fails part way
        // used to stay claimed for ever: ownership held by an object that never ran, so the periodic
        // clean-up preserved a queue nobody would ever drain, and the ID could not be registered again
        // (EDG-882 F-10). The rollback undoes exactly the steps that were taken, in the reverse of the
        // order they were taken in -- unpublish and stop before releasing ownership, as removeForwarder
        // does -- and only for this instance and this claim, so a concurrent registration of the same
        // ID cannot be torn down by another's failure.
        final List<String> subscribedTopics = new ArrayList<>();
        boolean registered = false;
        try {
            for (final String topic : mqttForwarder.getTopics()) {
                topicTree.addTopic(
                        clientId,
                        new Topic(topic, QoS.AT_LEAST_ONCE, false, true),
                        SubscriptionFlag.getDefaultFlags(true, true, false),
                        shareName);
                subscribedTopics.add(topic);
            }
            mqttForwarder.setExecutorService(executorService);
            mqttForwarder.setAfterForwardCallback(
                    (qos, uniqueId, queueId, cancelled) -> messageProcessed(qos, uniqueId, forwarderId, queueId));
            mqttForwarder.setResetInflightMarkerCallback((sharedSubscriptionId, uniqueId) -> {
                final var qPersistence = queuePersistence.get();
                try {
                    if (qPersistence != null) {
                        qPersistence
                                .removeInFlightMarker(sharedSubscriptionId, uniqueId)
                                .get();
                    }
                } catch (final InterruptedException | ExecutionException e) {
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    log.error(
                            "Failed to remove inflight marker for forwarder '{}', queue '{}', messageId '{}'",
                            forwarderId,
                            sharedSubscriptionId,
                            uniqueId,
                            e);
                    throw new RuntimeException(e);
                }
            });
            mqttForwarder.setResetAllInflightMarkersCallback((fwdId) -> {
                // Reset ALL inflight markers for all queues associated with this forwarder.
                // This is called on reconnection to handle messages that were read from persistence
                // but never made it to the forwarder's local queues.
                //
                // IMPORTANT: We collect all futures and wait for them using Futures.allAsList to
                // ensure all inflight markers are reset before onReconnect triggers checkBuffers().
                // This is safe because the persistence operations are submitted to SingleWriter
                // and don't hold any locks that could cause deadlock.
                final Set<String> forwarderQueueIds = queueIdsForForwarder.get(fwdId);
                if (forwarderQueueIds != null && !forwarderQueueIds.isEmpty()) {
                    if (log.isDebugEnabled()) {
                        log.debug(
                                "Resetting inflight markers for forwarder '{}', {} queue(s)",
                                fwdId,
                                forwarderQueueIds.size());
                    }
                    final ImmutableList.Builder<ListenableFuture<Void>> futuresBuilder = ImmutableList.builder();
                    final var qPersistence = queuePersistence.get();
                    if (qPersistence != null) {
                        for (final String queueIdToReset : forwarderQueueIds) {
                            futuresBuilder.add(qPersistence.removeAllInFlightMarkers(queueIdToReset));
                        }
                    }
                    try {
                        // Wait for all inflight markers to be reset before returning
                        // This ensures onReconnect() will see clean queues when it triggers checkBuffers()
                        Futures.allAsList(futuresBuilder.build())
                                .get(RESET_INFLIGHT_COUNTERS_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
                        if (log.isDebugEnabled()) {
                            log.debug(
                                    "Reset all inflight markers for forwarder '{}', {} queue(s)",
                                    fwdId,
                                    forwarderQueueIds.size());
                        }
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("Interrupted while resetting inflight markers for forwarder '{}'", fwdId, e);
                    } catch (final ExecutionException e) {
                        log.error("Failed to reset inflight markers for forwarder '{}'", fwdId, e);
                    } catch (final TimeoutException e) {
                        log.warn(
                                "Timeout resetting inflight markers for forwarder '{}' - forcing reconnect to retry",
                                fwdId,
                                e);
                        final MqttForwarder forwarder = forwarders.get(fwdId);
                        if (forwarder != null) {
                            forwarder.forceReconnect();
                        }
                    }
                }
            });
            mqttForwarder.setOnReconnectCallback(() -> {
                if (log.isDebugEnabled()) {
                    log.debug("OnReconnect callback triggered for forwarder '{}', checking buffers", forwarderId);
                }
                // Re-add all queue IDs to notEmptyQueues to ensure they get polled after reconnect
                final Set<String> forwarderQueueIds = queueIdsForForwarder.get(forwarderId);
                if (forwarderQueueIds != null) {
                    notEmptyQueues.addAll(forwarderQueueIds);
                }
                checkBuffers();
            });

            forwarders.put(forwarderId, mqttForwarder);
            notEmptyQueues.addAll(queueIds);
            mqttForwarder.start();
            registered = true;
        } finally {
            if (!registered) {
                rollbackRegistration(mqttForwarder, forwarderId, clientId, shareName, queueIds, subscribedTopics);
            }
        }

        if (log.isInfoEnabled()) {
            log.info(
                    "Forwarder '{}' started successfully, total active forwarders: {}", forwarderId, forwarders.size());
        }

        // Outside the rollback on purpose: by now the forwarder is registered, started and owns its
        // queues, so a failure to kick off a poll is a poll that did not happen, not a registration
        // that did not happen. Undoing a live forwarder here would be the more destructive answer.
        checkBuffers();
    }

    /**
     * Undoes a registration that threw part way, leaving the index exactly as it was before it started.
     * <p>
     * Every removal is conditional on this instance and this claim. A plain {@code remove(forwarderId)}
     * would let one failed registration tear down a different forwarder that had since taken the same
     * ID, which is the failure this whole ticket is about, arrived at from the other direction.
     */
    private void rollbackRegistration(
            final @NotNull MqttForwarder mqttForwarder,
            final @NotNull String forwarderId,
            final @NotNull String clientId,
            final @NotNull String shareName,
            final @NotNull Set<String> queueIds,
            final @NotNull List<String> subscribedTopics) {
        log.error("Registration of forwarder '{}' failed, undoing it", forwarderId);
        try {
            if (forwarders.remove(forwarderId, mqttForwarder)) {
                mqttForwarder.stop();
            }
            for (final String topic : subscribedTopics) {
                topicTree.removeSubscriber(clientId, topic, shareName);
            }
            notEmptyQueues.removeAll(queueIds);
        } catch (final Throwable rollbackFailure) {
            // Reported, never rethrown: the caller must see why the registration failed, not why the
            // clean-up after it did.
            log.error(
                    "Undoing the failed registration of forwarder '{}' did not complete", forwarderId, rollbackFailure);
        } finally {
            // Last, and unconditionally: ownership is what the periodic clean-up reads, so releasing it
            // before the forwarder is unpublished would expose a live queue, and not releasing it at all
            // would keep a queue nobody drains alive for the life of the node.
            if (queueIdsForForwarder.remove(forwarderId, queueIds)) {
                release(queueIds);
            }
        }
    }

    /**
     * Takes this forwarder's shared-subscription group back from any external client that holds it.
     * <p>
     * Shared subscribers of one group take turns, so a client subscribed to
     * {@code $share/$FORWARDER::<this forwarder's id>/<its filter>} receives the messages this bridge is
     * meant to forward, and they never reach the remote broker. The group name embeds a digest a client
     * can compute from the bridge's own configuration, which makes it reachable rather than theoretical
     * (EDG-882 QA round 2).
     * <p>
     * A SUBSCRIBE is already refused while the queue is claimed —
     * {@link com.hivemq.mqtt.handler.subscribe.IncomingSubscribeService} asks
     * {@link #isForwarderQueue(String)}, and the queues are claimed by
     * {@code BridgeService.internalStartBridge}'s reservation from the top of every start, restart and
     * node bootstrap. That leaves exactly one window this cannot cover: a client subscribing to the
     * group of a bridge that does not exist yet, which is legal at the time and becomes a collision the
     * moment the operator creates that bridge. This is that window, closed from the other side.
     * <p>
     * The subscription is removed rather than the bridge refused: a bridge that an arbitrary client can
     * stop from starting would be a worse defect than the one being fixed. Removal goes through the
     * subscription persistence, not the topic tree alone, or the client's session would restore it on
     * its next reconnect and take the group straight back.
     * <p>
     * Deliberately outside the registration rollback: the subscription being removed is one that must
     * not exist while this forwarder does, so putting it back if the registration then fails would be
     * restoring the defect.
     * <p>
     * <b>Every decomposition of the queue ID is searched, not just this forwarder's own</b> (EDG-882
     * review v02, R2-02). A forwarder registers its node by passing the group and the filter to
     * {@link LocalTopicTree#addTopic} directly, so its own node is split where this class puts the '/'.
     * A client cannot do that: it sends one string, and {@link com.hivemq.util.Topics} splits it at the
     * <em>first</em> '/' after {@code $share/} -- which, for the slash-bearing digest this ticket exists
     * for, falls inside the digest and puts the client on an entirely different node. Looking only under
     * this forwarder's own split therefore finds nothing and evicts nothing, while
     * {@code PublishPollServiceImpl} keys the queue the client polls off the concatenated string, which
     * is the same string whichever '/' it was split at -- so the intruder drains this queue anyway.
     * <p>
     * Enumerating the splits is both simpler than special-casing the two that can hold a subscriber and
     * independent of how {@code Topics} chooses to split, which is the assumption that broke here in the
     * first place. The extra nodes cannot hold an innocent client: a group taken from beyond the first
     * '/' contains a '/' itself, and no SUBSCRIBE can be stored under such a group. The cost is the
     * topic's depth in lookups, on a registration.
     */
    private void evictForeignSubscribers(final @NotNull String forwarderId, final @NotNull List<String> topics) {
        for (final String topic : topics) {
            final String queueId = createQueueId(forwarderId, topic);
            for (int slash = queueId.indexOf('/'); slash >= 0; slash = queueId.indexOf('/', slash + 1)) {
                final String group = queueId.substring(0, slash);
                final String filter = queueId.substring(slash + 1);
                for (final SubscriberWithQoS subscriber : topicTree.getSharedSubscriber(group, filter)) {
                    final String client = subscriber.getSubscriber();
                    // An internal component's own entry is not an intruder -- including a previous
                    // generation of this forwarder, which a slow stop can leave behind for an instant.
                    if (PublishDistributorImpl.isReservedClientId(client)) {
                        continue;
                    }
                    log.warn(
                            "Client '{}' holds the shared subscription group of bridge forwarder '{}' (as group"
                                    + " '{}', filter '{}'); removing its subscription to '{}', because it would"
                                    + " otherwise receive the messages the bridge is there to forward and they"
                                    + " would never reach the remote broker.",
                            client,
                            forwarderId,
                            group,
                            filter,
                            topic);
                    // The full '$share/' + queue ID string, because that is what the client subscribed
                    // with and therefore what its session stores: removing it from the topic tree alone
                    // would let the next reconnect restore it. It does not depend on which split found
                    // the client -- group + '/' + filter is the queue ID however it was cut.
                    final ListenableFuture<Void> removed =
                            subscriptionPersistence.get().remove(client, SHARED_SUBSCRIPTION_PREFIX + queueId);
                    FutureUtils.addExceptionLogger(removed);
                    Futures.addCallback(
                            removed,
                            new FutureCallback<>() {
                                @Override
                                public void onSuccess(final @Nullable Void result) {
                                    Checkpoints.checkpoint(MessageForwarder.FOREIGN_SUBSCRIBER_EVICTED);
                                }

                                @Override
                                public void onFailure(final @NotNull Throwable throwable) {
                                    // already reported by the exception logger above
                                }
                            },
                            MoreExecutors.directExecutor());
                }
            }
        }
    }

    @Override
    public void markBridgeConfigurationApplied() {
        bridgeConfigurationApplied = true;
    }

    @Override
    public boolean hasAppliedBridgeConfiguration() {
        return bridgeConfigurationApplied;
    }

    @Override
    public void reserveQueues(
            final @NotNull String reservationId, final @NotNull Map<String, List<String>> topicsByForwarderId) {
        final ImmutableSet.Builder<@NotNull String> queueIdsBuilder = ImmutableSet.builder();
        topicsByForwarderId.forEach((forwarderId, topics) -> {
            for (final String topic : topics) {
                queueIdsBuilder.add(createQueueId(forwarderId, topic));
            }
        });
        final ImmutableSet<@NotNull String> queueIds = queueIdsBuilder.build();
        // Same ordering as addForwarder, for the same reason (O2): retain the new set before releasing
        // the one it replaces, so a queue held by both never drops to zero references in between.
        retain(queueIds);
        final Set<String> superseded = reservedQueues.put(reservationId, queueIds);
        if (superseded != null) {
            release(superseded);
        }
        if (log.isInfoEnabled()) {
            log.info(
                    "Holding {} queue(s) of '{}' against the periodic clean-up until it can be started or is removed",
                    queueIds.size(),
                    reservationId);
        }
    }

    @Override
    public void releaseReservedQueues(final @NotNull String reservationId) {
        final Set<String> released = reservedQueues.remove(reservationId);
        if (released != null) {
            release(released);
            if (log.isDebugEnabled()) {
                log.debug("Released {} held queue(s) of '{}'", released.size(), reservationId);
            }
        }
    }

    @Override
    public void removeForwarder(final @NotNull MqttForwarder mqttForwarder, final boolean clearQueue) {
        final String forwarderId = mqttForwarder.getId();
        final String clientId = FORWARDER_PREFIX + forwarderId + "#" + hivemqId.get();

        if (log.isDebugEnabled()) {
            log.debug(
                    "Removing forwarder '{}' for {} topic(s), clearQueue: {}",
                    forwarderId,
                    mqttForwarder.getTopics().size(),
                    clearQueue);
        }

        for (final String topic : mqttForwarder.getTopics()) {
            topicTree.removeSubscriber(clientId, topic, FORWARDER_PREFIX + forwarderId);
            final String queueId = createQueueId(forwarderId, topic);
            notEmptyQueues.remove(queueId);
            if (clearQueue) {
                final var qPersistence = queuePersistence.get();
                if (qPersistence != null) {
                    qPersistence.clear(queueId, true); // clear up queue
                    if (log.isTraceEnabled()) {
                        log.trace("Cleared queue '{}' for forwarder '{}'", queueId, forwarderId);
                    }
                }
            }
        }
        // O3: unpublish and stop the forwarder first, drop its ownership second. Ownership is what the
        // periodic clean-up reads, so releasing it while the object is still in `forwarders` and still
        // polling leaves a live queue reading as unowned -- a sweep landing in that window clears a
        // queue that is being filled and drained as it does so. The reverse order is safe: for the
        // instant between the stop and the release the queue reads as owned by a forwarder that has
        // gone, which costs one clean-up cycle and no messages.
        //
        // The set the map returns -- not mqttForwarder.getTopics() -- is what was actually registered,
        // and is what must be released.
        final MqttForwarder removed = forwarders.remove(forwarderId);
        if (removed != null) {
            removed.stop();
            // After the stop, not before: stopping drains the forwarder's buffers, and every message it
            // hands back re-adds its queue id through messageProcessed. Removing the ids first left an
            // entry for a queue no forwarder owns, which nothing ever takes out again -- a set that
            // grows by one per retired subscription for the life of the node (EDG-882 QA round 3).
            for (final String topic : mqttForwarder.getTopics()) {
                notEmptyQueues.remove(createQueueId(forwarderId, topic));
            }
            if (log.isInfoEnabled()) {
                log.info(
                        "Forwarder '{}' removed and stopped, total active forwarders: {}",
                        forwarderId,
                        forwarders.size());
            }
        } else {
            log.warn("Attempted to remove forwarder '{}' but it was not found in active forwarders", forwarderId);
        }
        final Set<String> removedQueueIds = queueIdsForForwarder.remove(forwarderId);
        if (removedQueueIds != null) {
            release(removedQueueIds);
        }
    }

    @SuppressWarnings("NullAway") // Task<Void> lambda returning null is required for Void type
    public void messageProcessed(
            final @NotNull QoS qos,
            final @NotNull String uniqueId,
            final @NotNull String forwarderId,
            final @NotNull String queueId) {
        // QoS 0 has no inflight marker
        if (qos != QoS.AT_MOST_ONCE) {
            // -- 15665 - > QoS 0 causes republishing
            final var qPersistence = queuePersistence.get();
            if (qPersistence != null) {
                FutureUtils.addExceptionLogger(qPersistence.removeShared(queueId, uniqueId));
            }
        }

        if (log.isTraceEnabled()) {
            log.trace("Message processed for forwarder '{}', queueId: '{}', QoS: {}", forwarderId, queueId, qos);
        }

        FutureUtils.addExceptionLogger(
                singleWriterService.getQueuedMessagesQueue().submit(queueId, bucketIndex -> {
                    notEmptyQueues.add(queueId);
                    final MqttForwarder forwarder = forwarders.get(forwarderId);
                    if (forwarder != null) {
                        final int inflightCount = forwarder.getInflightCount();
                        if (inflightCount < FORWARDER_POLL_THRESHOLD_MESSAGES) {
                            if (log.isTraceEnabled()) {
                                log.trace(
                                        "Forwarder '{}' inflight count {} below threshold {}, triggering buffer check",
                                        forwarderId,
                                        inflightCount,
                                        FORWARDER_POLL_THRESHOLD_MESSAGES);
                            }
                            checkBuffers();
                        }
                    } else {
                        if (log.isTraceEnabled()) {
                            log.trace(
                                    "Forwarder '{}' not found during message processing, may have been removed",
                                    forwarderId);
                        }
                    }
                    return null;
                }));
    }

    @Override
    @SuppressWarnings("NullAway") // Task<Void> lambda returning null is required for Void type
    public void messageAvailable(final @NotNull String queueId) {
        if (log.isTraceEnabled()) {
            log.trace("Message available notification for queue '{}'", queueId);
        }
        singleWriterService.getQueuedMessagesQueue().submit(queueId, bucketIndex -> {
            notEmptyQueues.add(queueId);
            checkBuffers();
            return null;
        });
    }

    /**
     * Whether any registered forwarder owns this queue.
     * <p>
     * A reference count rather than a queue-ID-to-owner map: a share name may contain '/', so
     * concatenating it with the topic filter is not injective and two forwarders can in principle mint
     * the same queue ID. A plain map would drop the entry when the first of them unregisters, and the
     * periodic clean-up would then clear a queue the second still owns. Counting owners is exact for
     * every input without depending on that argument.
     */
    @Override
    public boolean isForwarderQueue(final @NotNull String queueId) {
        return forwarderQueueRefs.containsKey(queueId);
    }

    @Override
    public void checkBuffers() {
        pollLock.lock();
        try {
            if (polling) {
                pollAgain = true;
                if (log.isTraceEnabled()) {
                    log.trace("Polling already in progress, setting pollAgain flag");
                }
                return;
            } else {
                polling = true;
                if (log.isTraceEnabled()) {
                    log.trace("Starting buffer polling cycle, {} non-empty queue(s)", notEmptyQueues.size());
                }
            }
        } finally {
            pollLock.unlock();
        }
        checkBuffersAfterLock();
    }

    private void checkBuffersAfterLock() {
        if (notEmptyQueues.isEmpty()) {
            if (log.isTraceEnabled()) {
                log.trace("No queues to poll, ending polling cycle");
            }
            polling = false;
            return;
        }

        final int forwarderCount = forwarders.size();
        if (log.isDebugEnabled()) {
            log.debug("Polling {} forwarder(s) with {} non-empty queue(s)", forwarderCount, notEmptyQueues.size());
        }

        final ImmutableList.Builder<@NotNull ListenableFuture<Boolean>> pollFuturesBuilder = ImmutableList.builder();
        for (final MqttForwarder forwarder : forwarders.values()) {
            pollFuturesBuilder.addAll(pollForBuffer(forwarder));
        }
        final long pollingStartTime = log.isDebugEnabled() ? System.nanoTime() : 0;

        Futures.addCallback(
                Futures.allAsList(pollFuturesBuilder.build()),
                new FutureCallback<>() {
                    @Override
                    public void onSuccess(final @Nullable List<Boolean> result) {
                        if (result == null) {
                            return;
                        }
                        if (log.isDebugEnabled()) {
                            final long durationMicros = (System.nanoTime() - pollingStartTime) / 1000;
                            final long nonEmptyQueues = result.stream()
                                    .filter(Boolean::booleanValue)
                                    .count();
                            log.debug(
                                    "Poll cycle completed in {} μs, {} queue(s) had messages",
                                    durationMicros,
                                    nonEmptyQueues);
                        }

                        for (final Boolean queueNotEmpty : result) {
                            if (queueNotEmpty) {
                                // At least one queue was not empty and not over the threshold
                                if (log.isTraceEnabled()) {
                                    log.trace("Queues still have messages, continuing polling");
                                }
                                checkBuffersAfterLock();
                                return;
                            }
                        }

                        pollLock.lock();
                        try {
                            // we don't need to poll again
                            if (!pollAgain) {
                                if (log.isTraceEnabled()) {
                                    log.trace("All queues empty, ending polling cycle");
                                }
                                polling = false;
                                return;
                            }
                            // we need to poll again
                            if (log.isTraceEnabled()) {
                                log.trace("pollAgain flag set, restarting polling cycle");
                            }
                            pollAgain = false;
                        } finally {
                            pollLock.unlock();
                        }
                        checkBuffersAfterLock();
                    }

                    @Override
                    public void onFailure(final @NotNull Throwable throwable) {
                        log.error(
                                "Exception thrown while polling messages for bridge forwarders, will retry", throwable);

                        // we need to reset the polling flag and re-schedule a poll here
                        pollLock.lock();
                        try {
                            polling = false;
                        } finally {
                            pollLock.unlock();
                        }

                        // which callback executor does not matter, but it must be scheduled to prevent a stack-overflow
                        // if multiple errors occur back-to-back
                        @SuppressWarnings("NullAway") // Task<Void> lambda returning null is required for Void type
                        final var ignored = singleWriterService
                                .getQueuedMessagesQueue()
                                .submit("forwarder", bucketIndex -> {
                                    if (log.isDebugEnabled()) {
                                        log.debug("Retrying buffer check after polling failure");
                                    }
                                    checkBuffers();
                                    return null;
                                });
                    }
                },
                MoreExecutors.directExecutor());
    }

    @NotNull
    private List<ListenableFuture<Boolean>> pollForBuffer(final @NotNull MqttForwarder mqttForwarder) {
        final ImmutableList.Builder<@NotNull ListenableFuture<Boolean>> pollFuturesBuilder = ImmutableList.builder();
        final Set<String> forwarderNonEmptyQueue = queueIdsForForwarder.get(mqttForwarder.getId());
        if (forwarderNonEmptyQueue != null) {
            final int inflightCount = mqttForwarder.getInflightCount();
            if (log.isTraceEnabled()) {
                log.trace(
                        "Polling forwarder '{}' with {} inflight message(s), {} queue(s)",
                        mqttForwarder.getId(),
                        inflightCount,
                        forwarderNonEmptyQueue.size());
            }

            for (final String queueId : forwarderNonEmptyQueue) {
                if (inflightCount <= FORWARDER_POLL_THRESHOLD_MESSAGES) {
                    pollFuturesBuilder.add(pollForQueue(queueId, mqttForwarder));
                } else {
                    if (log.isTraceEnabled()) {
                        log.trace(
                                "Skipping poll for forwarder '{}', inflight count {} exceeds threshold {}",
                                mqttForwarder.getId(),
                                inflightCount,
                                FORWARDER_POLL_THRESHOLD_MESSAGES);
                    }
                }
            }
        } else {
            if (log.isTraceEnabled()) {
                log.trace("No queues found for forwarder '{}'", mqttForwarder.getId());
            }
        }
        return pollFuturesBuilder.build();
    }

    @NotNull
    private ListenableFuture<Boolean> pollForQueue(
            final @NotNull String queueId, final @NotNull MqttForwarder mqttForwarder) {
        if (log.isTraceEnabled()) {
            log.trace(
                    "Polling queue '{}' for forwarder '{}', batchSize: {}, byteLimit: {}",
                    queueId,
                    mqttForwarder.getId(),
                    FORWARDER_POLL_THRESHOLD_MESSAGES,
                    PUBLISH_POLL_BATCH_SIZE_BYTES);
        }
        final var qPersistence = queuePersistence.get();
        if (qPersistence != null) {
            return Futures.transform(
                    qPersistence.readShared(queueId, FORWARDER_POLL_THRESHOLD_MESSAGES, PUBLISH_POLL_BATCH_SIZE_BYTES),
                    publishes -> {
                        if (publishes == null) {
                            if (log.isTraceEnabled()) {
                                log.trace("Queue '{}' is empty, removing from non-empty queues", queueId);
                            }
                            notEmptyQueues.remove(queueId);
                            return false;
                        }

                        final int messageCount = publishes.size();
                        if (log.isDebugEnabled()) {
                            log.debug(
                                    "Retrieved {} message(s) from queue '{}' for forwarder '{}'",
                                    messageCount,
                                    queueId,
                                    mqttForwarder.getId());
                        }

                        for (final PUBLISH publish : publishes) {
                            mqttForwarder.onMessage(publish, queueId);
                        }
                        return !publishes.isEmpty();
                    },
                    executorService);
        } else {
            return Futures.immediateFuture(false);
        }
    }
}
