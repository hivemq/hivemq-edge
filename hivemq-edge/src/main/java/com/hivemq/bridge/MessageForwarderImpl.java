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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.hivemq.configuration.service.InternalConfigurations.FORWARDER_POLL_THRESHOLD_MESSAGES;
import static com.hivemq.configuration.service.InternalConfigurations.PUBLISH_POLL_BATCH_SIZE_BYTES;

@Singleton
public class MessageForwarderImpl implements MessageForwarder {

    private static final Logger log = LoggerFactory.getLogger(MessageForwarderImpl.class);

    public static final String FORWARDER_PREFIX = "forwarder#";

    private final @NotNull LocalTopicTree topicTree;
    private final @NotNull HivemqId hivemqId;
    private final @NotNull Lazy<ClientQueuePersistence> queuePersistence;
    private final @NotNull SingleWriterService singleWriterService;
    private final @NotNull Set<String> notEmptyQueues = new ConcurrentSkipListSet<>();
    private final @NotNull Map<String, MqttForwarder> forwarders = new ConcurrentHashMap<>(0);
    private final @NotNull Map<String, Set<String>> queueIdsForForwarder = new ConcurrentHashMap<>(0);
    private final @NotNull ExecutorService executorService =
            Executors.newScheduledThreadPool(InternalConfigurations.BRIDGE_MESSAGE_FORWARDER_POOL_THREADS_COUNT.get(),
                    ThreadFactoryUtil.create("bridge-message-forwarder-%d"));

    @Inject
    public MessageForwarderImpl(
            final @NotNull LocalTopicTree topicTree,
            final @NotNull HivemqId hivemqId,
            final @NotNull Lazy<ClientQueuePersistence> queuePersistence,
            final @NotNull SingleWriterService singleWriterService) {
        this.topicTree = topicTree;
        this.hivemqId = hivemqId;
        this.queuePersistence = queuePersistence;
        this.singleWriterService = singleWriterService;
    }

    @Override
    public void addForwarder(final @NotNull MqttForwarder mqttForwarder) {
        final String forwarderId = mqttForwarder.getId();
        final String clientId = FORWARDER_PREFIX + forwarderId + "#" + hivemqId.get();
        final String shareName = FORWARDER_PREFIX + forwarderId;

        final ImmutableSet.Builder<String> queueIdsBuilder = ImmutableSet.builder();
        for (String topic : mqttForwarder.getTopics()) {
            topicTree.addTopic(clientId,
                    new Topic(topic, QoS.AT_LEAST_ONCE, false, true),
                    SubscriptionFlag.getDefaultFlags(true, true, false),
                    shareName);
            final String queueId = createQueueId(forwarderId, topic);
            queueIdsBuilder.add(queueId);
        }
        final ImmutableSet<String> queueIds = queueIdsBuilder.build();
        mqttForwarder.setExecutorService(executorService);
        mqttForwarder.setAfterForwardCallback((qos, uniqueId, queueId, cancelled) -> messageProcessed(qos,
                uniqueId,
                forwarderId,
                queueId,
                cancelled));
        mqttForwarder.setResetInflightMarkerCallback((sharedSubscriptionId, uniqueId)->{
            try {
                queuePersistence.get().removeInFlightMarker(sharedSubscriptionId, uniqueId).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });


        forwarders.put(forwarderId, mqttForwarder);
        queueIdsForForwarder.put(forwarderId, queueIds);
        notEmptyQueues.addAll(queueIds);
        mqttForwarder.start();
        checkBuffers();
    }

    @Override
    public void removeForwarder(final @NotNull MqttForwarder mqttForwarder, final boolean clearQueue) {
        final String forwarderId = mqttForwarder.getId();
        final String clientId = forwarderId + hivemqId.get();
        for (String topic : mqttForwarder.getTopics()) {
            topicTree.removeSubscriber(clientId, topic, FORWARDER_PREFIX + forwarderId);
            final String queueId = createQueueId(forwarderId, topic);
            notEmptyQueues.remove(queueId);
            if(clearQueue) {
                queuePersistence.get().clear(queueId, true); //clear up queue
            }
        }
        queueIdsForForwarder.remove(forwarderId);
        forwarders.get(forwarderId).stop();
        forwarders.remove(forwarderId);
    }

    public void messageProcessed(
            final @NotNull QoS qos,
            final @NotNull String uniqueId,
            final @NotNull String forwarderId,
            final @NotNull String queueId,
            boolean cancelled) {
        singleWriterService.callbackExecutor(queueId).execute(() -> {
            //QoS 0 has no inflight marker
            if (qos != QoS.AT_MOST_ONCE) {
                //-- 15665 - > QoS 0 causes republishing
                FutureUtils.addExceptionLogger(queuePersistence.get().removeShared(queueId, uniqueId));
            }
            continueForwarding(queueId, forwarders.get(forwarderId));
        });
    }

    private void continueForwarding(final @NotNull String queueId, final @NotNull MqttForwarder mqttForwarder) {
        notEmptyQueues.add(queueId);
        if (mqttForwarder.getInflightCount() < FORWARDER_POLL_THRESHOLD_MESSAGES) {
            checkBuffers();
        }
    }

    @Override
    public void messageAvailable(final @NotNull String queueId) {
        singleWriterService.callbackExecutor(queueId).execute(() -> {
            notEmptyQueues.add(queueId);
            checkBuffers();
        });
    }

    private static @NotNull String createQueueId(final @NotNull String forwarderId, final @NotNull String topic) {
        return FORWARDER_PREFIX + forwarderId + "/" + topic;
    }

    @NotNull
    private final Lock pollLock = new ReentrantLock();
    private boolean polling = false;
    private boolean pollAgain = false;

    @Override
    public void checkBuffers() {

        pollLock.lock();
        try {
            if (polling) {
                pollAgain = true;
                return;
            } else {
                polling = true;
            }
        } finally {
            pollLock.unlock();
        }
        checkBuffersAfterLock();
    }

    private void checkBuffersAfterLock() {
        if (notEmptyQueues.isEmpty()) {
            polling = false;
            return;
        }
        final ImmutableList.Builder<ListenableFuture<Boolean>> pollFuturesBuilder = ImmutableList.builder();
        for (MqttForwarder forwarder : forwarders.values()) {
            final List<ListenableFuture<Boolean>> singlePollFuture = pollForBuffer(forwarder);
            pollFuturesBuilder.addAll(singlePollFuture);
        }
        final ImmutableList<ListenableFuture<Boolean>> pollFutures = pollFuturesBuilder.build();
        final ListenableFuture<List<Boolean>> pollFuture = Futures.allAsList(pollFutures);
        Futures.addCallback(pollFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(final @NotNull List<Boolean> result) {
                for (final Boolean queueNotEmpty : result) {
                    if (queueNotEmpty) {
                        // At least one queue was not empty and not over the threshold
                        checkBuffersAfterLock();
                        return;
                    }
                }

                pollLock.lock();
                try {
                    //we don't need to poll again
                    if (!pollAgain) {
                        polling = false;
                        return;
                    }
                    //we need to poll again
                    pollAgain = false;
                } finally {
                    pollLock.unlock();
                }
                checkBuffersAfterLock();
            }

            @Override
            public void onFailure(final @NotNull Throwable throwable) {
                log.error("An exception was thrown while polling messages for an extension consumer.", throwable);

                //we need to reset the polling flag and re-schedule a poll here
                pollLock.lock();
                try {
                    polling = false;
                } finally {
                    pollLock.unlock();
                }

                //which callback executor does not matter, but it must be scheduled to prevent a stack-overflow if multiple errors occur back-to-back
                final Executor callbackExecutor = singleWriterService.callbackExecutor("forwarder");
                callbackExecutor.execute(() -> {
                    checkBuffers();
                });
            }
        }, MoreExecutors.directExecutor());
    }

    @NotNull
    private List<ListenableFuture<Boolean>> pollForBuffer(final @NotNull MqttForwarder mqttForwarder) {
        final ImmutableList.Builder<ListenableFuture<Boolean>> pollFuturesBuilder = ImmutableList.builder();

        final Set<String> forwarderNonEmptyQueue = queueIdsForForwarder.get(mqttForwarder.getId());
        if (forwarderNonEmptyQueue != null) {
            for (final String queueId : forwarderNonEmptyQueue) {
                if (mqttForwarder.getInflightCount() <= FORWARDER_POLL_THRESHOLD_MESSAGES) {
                    final ListenableFuture<Boolean> pollFuture = pollForQueue(queueId, mqttForwarder);
                    pollFuturesBuilder.add(pollFuture);
                }
            }
        }
        return pollFuturesBuilder.build();
    }

    @NotNull
    private ListenableFuture<Boolean> pollForQueue(
            final @NotNull String queueId, final @NotNull MqttForwarder mqttForwarder) {
        final ListenableFuture<ImmutableList<PUBLISH>> pollFuture = queuePersistence.get()
                .readShared(queueId, FORWARDER_POLL_THRESHOLD_MESSAGES, PUBLISH_POLL_BATCH_SIZE_BYTES);
        return Futures.transformAsync(pollFuture, publishes -> {
            if (publishes == null) {
                notEmptyQueues.remove(queueId);
                return Futures.immediateFuture(false);
            }
            for (final PUBLISH publish : publishes) {
                mqttForwarder.onMessage(publish, queueId);
            }
            return Futures.immediateFuture(!publishes.isEmpty());
        }, executorService);
    }

}
