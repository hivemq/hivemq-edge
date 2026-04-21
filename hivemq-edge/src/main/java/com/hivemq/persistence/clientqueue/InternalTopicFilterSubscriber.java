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

import static com.hivemq.configuration.service.InternalConfigurations.PUBLISH_POLL_BATCH_SIZE_BYTES;
import static com.hivemq.mqtt.services.PublishDistributorImpl.INTERNAL_SUBSCRIBER_PREFIX;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.ImmutableIntArray;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.SubscriptionFlag;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.util.FutureUtils;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for internal components that need to receive messages from the topic tree without
 * being an MQTT client. Extend this class, supply a {@code COMPONENT_PREFIX} constant and a
 * unique {@code instanceId}, and implement {@link #process(PUBLISH)}.
 *
 * <p>The client ID has the form {@code "internal#<componentPrefix>#<instanceId>"}, e.g.
 * {@code "internal#tynebridge#my-bridge"} or {@code "internal#combiner#combiner:123/inst:2"}.
 *
 * <p>Call {@link #start()} once to subscribe and {@link #stop()} to unsubscribe. Both methods
 * are safe to call on any thread.
 *
 * @see com.hivemq.mqtt.services.PublishDistributorImpl#INTERNAL_SUBSCRIBER_PREFIX
 */
@SuppressWarnings({"FutureReturnValueIgnored", "CheckReturnValue"})
public abstract class InternalTopicFilterSubscriber {

    private static final @NotNull Logger log = LoggerFactory.getLogger(InternalTopicFilterSubscriber.class);

    // Built once, reused on every poll. SHARED_IN_FLIGHT_MARKER acts as a boolean inflight flag —
    // not a real wire packet ID, since messages never go to an MQTT client.
    // removeShared() uses uniqueId, not the packet ID, so the value here does not matter.
    private static final @NotNull ImmutableIntArray POLL_PACKET_IDS =
            ImmutableIntArray.of(ClientQueuePersistenceImpl.SHARED_IN_FLIGHT_MARKER);

    private final @NotNull ClientQueuePersistence clientQueuePersistence;
    private final @NotNull SingleWriterService singleWriterService;
    private final @NotNull LocalTopicTree topicTree;
    private final @NotNull List<String> topicFilters;
    private final @NotNull String clientId;
    private final @NotNull ClientQueuePersistence.PublishAvailableCallback callback;

    protected InternalTopicFilterSubscriber(
            final @NotNull String componentPrefix,
            final @NotNull String instanceId,
            final @NotNull String topicFilter,
            final @NotNull LocalTopicTree topicTree,
            final @NotNull ClientQueuePersistence clientQueuePersistence,
            final @NotNull SingleWriterService singleWriterService) {
        this(componentPrefix, instanceId, List.of(topicFilter), topicTree, clientQueuePersistence, singleWriterService);
    }

    protected InternalTopicFilterSubscriber(
            final @NotNull String componentPrefix,
            final @NotNull String instanceId,
            final @NotNull List<String> topicFilters,
            final @NotNull LocalTopicTree topicTree,
            final @NotNull ClientQueuePersistence clientQueuePersistence,
            final @NotNull SingleWriterService singleWriterService) {
        this.clientQueuePersistence = clientQueuePersistence;
        this.singleWriterService = singleWriterService;
        this.topicTree = topicTree;
        this.topicFilters = topicFilters;
        this.clientId = INTERNAL_SUBSCRIBER_PREFIX + componentPrefix + "#" + instanceId;
        this.callback = id -> submitPoll();
    }

    /**
     * Registers the queue callback, schedules the initial poll, and subscribes to all configured
     * topic filters in the topic tree. Safe to call on any thread.
     */
    public void start() {
        // Register the callback and kick off the initial poll first, so that no message
        // arriving during topic registration is missed.
        clientQueuePersistence.addPublishAvailableCallback(callback, clientId);
        submitPoll();
        for (final String topicFilter : topicFilters) {
            topicTree.addTopic(
                    clientId,
                    new Topic(topicFilter, QoS.AT_LEAST_ONCE, false, true),
                    SubscriptionFlag.getDefaultFlags(false, true, false),
                    null);
        }
    }

    /**
     * Removes all topic tree subscriptions and deregisters the queue callback. Safe to call on
     * any thread.
     */
    public void stop() {
        // Unsubscribe from the topic tree first so no new messages are routed here,
        // then deregister the callback.
        for (final String topicFilter : topicFilters) {
            topicTree.removeSubscriber(clientId, topicFilter, null);
        }
        clientQueuePersistence.removePublishAvailableCallback(clientId);
    }

    private void submitPoll() {
        singleWriterService.getQueuedMessagesQueue().submit(clientId, bucketIndex -> {
            pollAndForward();
            return null;
        });
    }

    private void pollAndForward() {
        try {
            final ListenableFuture<ImmutableList<PUBLISH>> future =
                    clientQueuePersistence.readNew(clientId, false, POLL_PACKET_IDS, PUBLISH_POLL_BATCH_SIZE_BYTES);
            Futures.transform(
                    future,
                    publishes -> {
                        if (publishes == null || publishes.isEmpty()) {
                            return null;
                        }
                        processPublish(publishes.get(0));
                        return null;
                    },
                    MoreExecutors.directExecutor());
        } catch (final Throwable t) {
            log.error("Failed to poll internal subscriber '{}': {}", clientId, t.getMessage());
            submitPoll();
        }
    }

    private void processPublish(final @NotNull PUBLISH publish) {
        try {
            process(publish);
            removeMessage(publish);
            submitPoll();
        } catch (final Exception e) {
            log.error(
                    "Failed to process message for internal subscriber '{}', message will be dropped: {}",
                    clientId,
                    e.getMessage());
            removeMessage(publish);
            submitPoll();
        }
    }

    private void removeMessage(final @NotNull PUBLISH publish) {
        if (publish.getQoS() != QoS.AT_MOST_ONCE) {
            FutureUtils.addExceptionLogger(clientQueuePersistence.removeShared(clientId, publish.getUniqueId()));
        }
    }

    /**
     * Called once per message, on the SingleWriter thread. Implementations must not block.
     * Any exception thrown is caught, logged, and the message is dropped.
     */
    public abstract void process(final @NotNull PUBLISH publish);
}
