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
import com.hivemq.mqtt.topic.SubscriptionFlag;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.persistence.util.FutureUtils;
import com.hivemq.util.ThreadFactoryUtil;
import dagger.Lazy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@SuppressWarnings("FutureReturnValueIgnored")
public class MessageForwarderImpl implements MessageForwarder {

    public static final @NotNull String FORWARDER_PREFIX = "forwarder#";

    private static final @NotNull Logger log = LoggerFactory.getLogger(MessageForwarderImpl.class);
    public static final int RESET_INFLIGHT_COUNTERS_TIMEOUT_IN_SECONDS = 30;

    private final @NotNull LocalTopicTree topicTree;
    private final @NotNull HivemqId hivemqId;
    private final @NotNull Lazy<ClientQueuePersistence> queuePersistence;
    private final @NotNull SingleWriterService singleWriterService;
    private final @NotNull Set<String> notEmptyQueues;
    private final @NotNull Map<String, MqttForwarder> forwarders;
    private final @NotNull Map<String, Set<String>> queueIdsForForwarder;
    private final @NotNull ExecutorService executorService;
    private final @NotNull Lock pollLock;
    private volatile boolean polling;
    private volatile boolean pollAgain;

    @Inject
    public MessageForwarderImpl(
            final @NotNull LocalTopicTree topicTree,
            final @NotNull HivemqId hivemqId,
            final @NotNull Lazy<ClientQueuePersistence> queuePersistence,
            final @NotNull SingleWriterService singleWriterService,
            final @NotNull ShutdownHooks shutdownHooks) {
        this.topicTree = topicTree;
        this.hivemqId = hivemqId;
        this.queuePersistence = queuePersistence;
        this.singleWriterService = singleWriterService;
        this.notEmptyQueues = new ConcurrentSkipListSet<>();
        this.forwarders = new ConcurrentHashMap<>(0);
        this.queueIdsForForwarder = new ConcurrentHashMap<>(0);
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
            topicTree.addTopic(
                    clientId,
                    new Topic(topic, QoS.AT_LEAST_ONCE, false, true),
                    SubscriptionFlag.getDefaultFlags(true, true, false),
                    shareName);
            queueIdsBuilder.add(createQueueId(forwarderId, topic));
        }
        final ImmutableSet<@NotNull String> queueIds = queueIdsBuilder.build();
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
        queueIdsForForwarder.put(forwarderId, queueIds);
        notEmptyQueues.addAll(queueIds);
        mqttForwarder.start();

        if (log.isInfoEnabled()) {
            log.info(
                    "Forwarder '{}' started successfully, total active forwarders: {}", forwarderId, forwarders.size());
        }

        checkBuffers();
    }

    @Override
    public void removeForwarder(final @NotNull MqttForwarder mqttForwarder, final boolean clearQueue) {
        final String forwarderId = mqttForwarder.getId();
        final String clientId = forwarderId + hivemqId.get();

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
        queueIdsForForwarder.remove(forwarderId);
        final MqttForwarder removed = forwarders.remove(forwarderId);
        if (removed != null) {
            removed.stop();
            if (log.isInfoEnabled()) {
                log.info(
                        "Forwarder '{}' removed and stopped, total active forwarders: {}",
                        forwarderId,
                        forwarders.size());
            }
        } else {
            log.warn("Attempted to remove forwarder '{}' but it was not found in active forwarders", forwarderId);
        }
    }

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
                    public void onSuccess(final @NotNull List<Boolean> result) {
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
                        singleWriterService.getQueuedMessagesQueue().submit("forwarder", bucketIndex -> {
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
