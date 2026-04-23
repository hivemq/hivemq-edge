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

// region InternalTopicFilterSubscriber — base class for internal topic-tree subscribers
// =====================================================================================================================
// Base class for internal Edge components that need to receive messages from the topic tree without
// being an MQTT client. Extend this class, supply a COMPONENT_PREFIX constant and a unique
// instanceId, and implement process().
//
// Call start() once to subscribe and stop() to unsubscribe. Both methods are safe to call on any
// thread.
//
// @see <a href="https://hivemq.github.io/hivemq-edge-lore/2-implementation/internal-topic-filter-subscriber/">Edge Lore
// — Internal Topic Filter Subscriber</a>
//
@SuppressWarnings({"FutureReturnValueIgnored", "CheckReturnValue"})
public abstract class InternalTopicFilterSubscriber {

    private static final @NotNull Logger log = LoggerFactory.getLogger(InternalTopicFilterSubscriber.class);

    public static final @NotNull String INTERNAL_SUBSCRIBER_PREFIX = "$INTERNAL::";

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

    // endregion

    // region InternalTopicFilterSubscriber() constructor
    // =================================================================================================================
    // componentPrefix — identifies the Edge component type (e.g. "combiner", "sampler"). Must be
    //                   unique across all components in Edge. Becomes the middle segment of the
    //                   internal client ID.
    // instanceId      — identifies this specific subscriber instance within the component. Must be
    //                   unique within the componentPrefix namespace. Typically derived from the
    //                   configuration ID of the owning instance.
    // topicFilter(s)  — the MQTT topic filter(s) to subscribe to. All filters share the same
    //                   client queue; messages matching any of them are delivered to process().
    //
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
        this.clientId = INTERNAL_SUBSCRIBER_PREFIX + componentPrefix + "::" + instanceId;
        this.callback = id -> submitPoll();
    }

    // endregion

    // region process(message) - handler that must be defined by the user of this mechanism
    // =================================================================================================================
    // Called once per message, on the SingleWriter thread. Implementations must not block.
    // Any exception thrown is caught, logged, and the message is dropped.
    //
    public abstract void process(final @NotNull PUBLISH message);

    // endregion

    // region start() - starts the subscription for the topic filters
    // =================================================================================================================
    // Registers the queue callback, schedules the initial poll, and subscribes to all configured
    // topic filters in the topic tree. Safe to call on any thread.
    //
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

    // endregion

    // region stop() - stops the subscription
    // =================================================================================================================
    // Removes all topic tree subscriptions and deregisters the queue callback. Safe to call on
    // any thread.
    //
    public void stop() {
        // Unsubscribe from the topic tree first so no new messages are routed here,
        // then deregister the callback.
        for (final String topicFilter : topicFilters) {
            topicTree.removeSubscriber(clientId, topicFilter, null);
        }
        clientQueuePersistence.removePublishAvailableCallback(clientId);
    }

    // endregion

    // region internal wiring
    // =================================================================================================================
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

    private void processPublish(final @NotNull PUBLISH message) {
        try {
            process(message);
            removeMessage(message);
            submitPoll();
        } catch (final Exception e) {
            log.error(
                    "Failed to process message for internal subscriber '{}', message will be dropped: {}",
                    clientId,
                    e.getMessage());
            removeMessage(message);
            submitPoll();
        }
    }

    private void removeMessage(final @NotNull PUBLISH message) {
        if (message.getQoS() != QoS.AT_MOST_ONCE) {
            FutureUtils.addExceptionLogger(clientQueuePersistence.removeShared(clientId, message.getUniqueId()));
        }
    }

    // endregion

}
