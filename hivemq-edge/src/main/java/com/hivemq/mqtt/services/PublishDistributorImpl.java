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
package com.hivemq.mqtt.services;

import static com.hivemq.bridge.MessageForwarderImpl.FORWARDER_PREFIX;
import static com.hivemq.combining.runtime.DataCombiningRuntime.COMBINER_PREFIX;
import static com.hivemq.mqtt.handler.publish.PublishStatus.DELIVERED;
import static com.hivemq.mqtt.handler.publish.PublishStatus.FAILED;
import static com.hivemq.mqtt.handler.publish.PublishStatus.NOT_CONNECTED;
import static com.hivemq.persistence.clientqueue.InternalTopicFilterSubscriber.INTERNAL_SUBSCRIBER_PREFIX;
import static com.hivemq.sampling.SamplingService.SAMPLER_PREFIX;
import static com.hivemq.sampling.SamplingService.SAMPLER_QUEUE_LIMIT;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.ImmutableIntArray;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.bridge.MessageForwarder;
import com.hivemq.bridge.config.LocalSubscription;
import com.hivemq.bridge.config.MqttBridge;
import com.hivemq.configuration.reader.BridgeExtractor;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.mqtt.handler.publish.PublishStatus;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.topic.SubscriberWithIdentifiers;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.persistence.clientqueue.QueuePolicy;
import com.hivemq.persistence.clientsession.ClientSession;
import com.hivemq.persistence.clientsession.ClientSessionPersistence;
import com.hivemq.persistence.util.FutureUtils;
import com.hivemq.sampling.SamplingService;
import dagger.Lazy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Christoph Schäbel
 */
@Singleton
public class PublishDistributorImpl implements PublishDistributor {

    /**
     * Returns {@code true} if the given client ID is reserved for an internal Edge component and
     * must not be used by an external MQTT client.
     */
    public static boolean isReservedClientId(final @NotNull String clientId) {
        return clientId.startsWith(INTERNAL_SUBSCRIBER_PREFIX)
                || clientId.startsWith(FORWARDER_PREFIX)
                || clientId.startsWith(SAMPLER_PREFIX)
                || clientId.startsWith(COMBINER_PREFIX);
    }

    @NotNull
    private final ClientQueuePersistence clientQueuePersistence;

    @NotNull
    private final Lazy<ClientSessionPersistence> clientSessionPersistence;

    @NotNull
    private final MqttConfigurationService mqttConfigurationService;

    @NotNull
    private final BridgeExtractor bridgeConfiguration;

    /**
     * Lazy because sampling is built on top of the queue persistence this class also uses: asking for
     * it eagerly closes a dependency cycle at construction time.
     */
    @NotNull
    private final Lazy<SamplingService> samplingService;

    /**
     * Lazy for the same reason as {@link #samplingService}: the message forwarder is built on the queue
     * persistence this class also uses.
     */
    @NotNull
    private final Lazy<MessageForwarder> messageForwarder;

    @Inject
    public PublishDistributorImpl(
            final @NotNull ClientQueuePersistence clientQueuePersistence,
            final @NotNull Lazy<ClientSessionPersistence> clientSessionPersistence,
            final @NotNull ConfigurationService configurationService,
            final @NotNull Lazy<SamplingService> samplingService,
            final @NotNull Lazy<MessageForwarder> messageForwarder) {
        this.clientQueuePersistence = clientQueuePersistence;
        this.clientSessionPersistence = clientSessionPersistence;
        this.mqttConfigurationService = configurationService.mqttConfiguration();
        this.bridgeConfiguration = configurationService.bridgeExtractor();
        this.samplingService = samplingService;
        this.messageForwarder = messageForwarder;
    }

    @NotNull
    @Override
    public ListenableFuture<Void> distributeToNonSharedSubscribers(
            final @NotNull Map<String, SubscriberWithIdentifiers> subscribers,
            final @NotNull PUBLISH publish,
            final @NotNull ExecutorService executorService) {

        final ImmutableList.Builder<ListenableFuture<Void>> publishResultFutureBuilder = ImmutableList.builder();

        for (final Map.Entry<String, SubscriberWithIdentifiers> entry : subscribers.entrySet()) {
            final SubscriberWithIdentifiers subscriber = entry.getValue();

            final ListenableFuture<PublishStatus> publishFuture = sendMessageToSubscriber(
                    publish,
                    entry.getKey(),
                    subscriber.getQos(),
                    false,
                    subscriber.isRetainAsPublished(),
                    subscriber.getSubscriptionIdentifier());

            final SettableFuture<Void> publishFinishedFuture = SettableFuture.create();
            publishResultFutureBuilder.add(publishFinishedFuture);
            Futures.addCallback(
                    publishFuture,
                    new StandardPublishCallback(entry.getKey(), publish, publishFinishedFuture),
                    executorService);
        }

        return FutureUtils.voidFutureFromList(publishResultFutureBuilder.build());
    }

    @NotNull
    @Override
    public ListenableFuture<Void> distributeToSharedSubscribers(
            final @NotNull Set<String> sharedSubscribers,
            final @NotNull PUBLISH publish,
            final @NotNull ExecutorService executorService) {

        final ImmutableList.Builder<ListenableFuture<Void>> publishResultFutureBuilder = ImmutableList.builder();

        for (final String sharedSubscriber : sharedSubscribers) {
            final SettableFuture<Void> publishFinishedFuture = SettableFuture.create();
            final ListenableFuture<PublishStatus> future = sendMessageToSubscriber(
                    publish, sharedSubscriber, publish.getQoS().getQosNumber(), true, true, null);
            publishResultFutureBuilder.add(publishFinishedFuture);
            Futures.addCallback(
                    future,
                    new StandardPublishCallback(sharedSubscriber, publish, publishFinishedFuture),
                    executorService);
        }

        return FutureUtils.voidFutureFromList(publishResultFutureBuilder.build());
    }

    @NotNull
    @Override
    public ListenableFuture<PublishStatus> sendMessageToSubscriber(
            final @NotNull PUBLISH publish,
            final @NotNull String clientId,
            final int subscriptionQos,
            final boolean sharedSubscription,
            final boolean retainAsPublished,
            final @Nullable ImmutableIntArray subscriptionIdentifier) {

        return handlePublish(
                publish, clientId, subscriptionQos, sharedSubscription, retainAsPublished, subscriptionIdentifier);
    }

    private @NotNull ListenableFuture<PublishStatus> handlePublish(
            final @NotNull PUBLISH publish,
            final @NotNull String client,
            final int subscriptionQos,
            final boolean sharedSubscription,
            final boolean retainAsPublished,
            final @Nullable ImmutableIntArray subscriptionIdentifier) {

        if (sharedSubscription) {
            // Asked of the registry that owns forwarder queues, not read off the queue ID -- the same
            // rule the sampler branch below states, applied to the branch above it. A queue ID is built
            // from a share name, and a share name is the client's to choose: a subscription to
            // $share/$FORWARDER::<a live forwarder id>/t took this branch, so an ordinary client had a
            // bridge's queue limit applied to its own queue and, against a persist=false bridge, its
            // QoS silently rewritten to 0 (EDG-882 QA round 2).
            if (messageForwarder.get().isForwarderQueue(client)) {
                return handlePublishForBridgeForwarder(
                        publish,
                        client,
                        retainAsPublished,
                        subscriptionIdentifier,
                        mqttConfigurationService.maxQueuedMessages(),
                        subscriptionQos);
            } else if (samplingService.get().isSamplerQueue(client)) {
                // Asked of the service that creates samplers rather than read off the queue ID: the ID
                // is built from a share name, and a share name is the client's to choose. A
                // subscription to $share/$SAMPLER::customer/alerts is legal and belongs to that client,
                // so it must keep the configured queue limit and the configured overflow strategy
                // instead of being turned into a ten-message ring (EDG-882 F-05).
                return queuePublish(
                        client,
                        publish,
                        subscriptionQos,
                        true,
                        retainAsPublished,
                        subscriptionIdentifier,
                        SAMPLER_QUEUE_LIMIT,
                        QueuePolicy.SAMPLE_RING);
            } else {
                return queuePublish(
                        client,
                        publish,
                        subscriptionQos,
                        true,
                        retainAsPublished,
                        subscriptionIdentifier,
                        mqttConfigurationService.maxQueuedMessages());
            }
        }

        final boolean qos0Message = Math.min(subscriptionQos, publish.getQoS().getQosNumber()) == 0;
        Long queueLimit = null;

        if (!client.startsWith(INTERNAL_SUBSCRIBER_PREFIX)) {
            final ClientSession clientSession = clientSessionPersistence.get().getSession(client, false);
            final boolean clientConnected = clientSession != null && clientSession.isConnected();

            if (qos0Message && !clientConnected) {
                return Futures.immediateFuture(NOT_CONNECTED);
            }

            // no session present or session already expired
            if (clientSession == null) {
                return Futures.immediateFuture(NOT_CONNECTED);
            }

            queueLimit = clientSession.getQueueLimit();
        }

        return queuePublish(
                client, publish, subscriptionQos, false, retainAsPublished, subscriptionIdentifier, queueLimit);
    }

    private @NotNull SettableFuture<PublishStatus> handlePublishForBridgeForwarder(
            final @NotNull PUBLISH publish,
            final @NotNull String client,
            final boolean retainAsPublished,
            final @Nullable ImmutableIntArray subscriptionIdentifier,
            final @NotNull Long queueLimit,
            int appliedQoS) {
        // update with the configuration of the bridge, if it is a bridge client
        final CustomBridgeLimitations customBridgeLimitations = getBridgeConfig(client);
        long appliedQueueLimit = queueLimit;

        if (customBridgeLimitations != null) {
            final Long queueLimitFromConfig = customBridgeLimitations.queueLimit;
            if (queueLimitFromConfig != null) {
                // bridges can overwrite the default
                appliedQueueLimit = queueLimitFromConfig;
            }
            if (!customBridgeLimitations.persist) {
                // if the bridge has the persist flag disabled, we reduce the QoS of the messages 0, so they are not
                // stored in the file persistence in case.
                appliedQoS = 0;
            }
        }
        return queuePublish(
                client, publish, appliedQoS, true, retainAsPublished, subscriptionIdentifier, appliedQueueLimit);
    }

    @NotNull
    private SettableFuture<PublishStatus> queuePublish(
            final @NotNull String client,
            final @NotNull PUBLISH publish,
            final int subscriptionQos,
            final boolean shared,
            final boolean retainAsPublished,
            final @Nullable ImmutableIntArray subscriptionIdentifier,
            final @Nullable Long queueLimit) {
        return queuePublish(
                client,
                publish,
                subscriptionQos,
                shared,
                retainAsPublished,
                subscriptionIdentifier,
                queueLimit,
                QueuePolicy.DEFAULT);
    }

    @NotNull
    private SettableFuture<PublishStatus> queuePublish(
            final @NotNull String client,
            final @NotNull PUBLISH publish,
            final int subscriptionQos,
            final boolean shared,
            final boolean retainAsPublished,
            final @Nullable ImmutableIntArray subscriptionIdentifier,
            final @Nullable Long queueLimit,
            final @NotNull QueuePolicy policy) {

        final Long appliedQueueLimit =
                Objects.requireNonNullElseGet(queueLimit, mqttConfigurationService::maxQueuedMessages);
        final ListenableFuture<Void> future = clientQueuePersistence.add(
                client,
                shared,
                createPublish(publish, subscriptionQos, retainAsPublished, subscriptionIdentifier),
                false,
                appliedQueueLimit,
                policy);

        final SettableFuture<PublishStatus> statusFuture = SettableFuture.create();

        Futures.addCallback(
                future,
                new FutureCallback<>() {
                    @Override
                    public void onSuccess(final @Nullable Void result) {
                        statusFuture.set(DELIVERED);
                    }

                    @Override
                    public void onFailure(final @NotNull Throwable t) {
                        statusFuture.set(FAILED);
                    }
                },
                MoreExecutors.directExecutor());
        return statusFuture;
    }

    /**
     * The limits configured for the bridge subscription that owns this queue, or {@code null}.
     * <p>
     * Anchored rather than by substring: a forwarder queue ID is exactly
     * {@code $FORWARDER::<bridgeId>-<digest>/<topic filter>}, so the prefix up to and including the '/'
     * identifies the subscription and the rest is the topic. Matching with {@code contains} let one
     * bridge whose id merely appears inside another's queue ID answer for it, and made the resolution
     * depend on a string a client can influence (EDG-882 QA round 2).
     */
    private @Nullable CustomBridgeLimitations getBridgeConfig(final @NotNull String queueId) {
        for (final MqttBridge bridge : bridgeConfiguration.getBridges()) {
            if (!queueId.startsWith(FORWARDER_PREFIX + bridge.getId() + "-")) {
                continue;
            }
            for (final LocalSubscription localSubscription : bridge.getLocalSubscriptions()) {
                final String queueIdPrefix =
                        FORWARDER_PREFIX + bridge.getId() + "-" + localSubscription.calculateUniqueId() + "/";
                if (queueId.startsWith(queueIdPrefix)) {
                    return new CustomBridgeLimitations(bridge.isPersist(), localSubscription.getQueueLimit());
                }
            }
        }
        return null;
    }

    private static class CustomBridgeLimitations {
        private final boolean persist;
        private final @Nullable Long queueLimit;

        private CustomBridgeLimitations(final boolean persist, final @Nullable Long queueLimit) {
            this.persist = persist;
            this.queueLimit = queueLimit;
        }
    }

    private @NotNull PUBLISH createPublish(
            final @NotNull PUBLISH publish,
            final int subscriptionQos,
            final boolean retainAsPublished,
            final @Nullable ImmutableIntArray subscriptionIdentifier) {
        final ImmutableIntArray identifiers;
        if (subscriptionIdentifier == null) {
            identifiers = ImmutableIntArray.of();
        } else {
            identifiers = subscriptionIdentifier;
        }

        final PUBLISHFactory.Mqtt5Builder builder = new PUBLISHFactory.Mqtt5Builder()
                .fromPublish(publish)
                .withRetain(publish.isRetain() && retainAsPublished)
                .withSubscriptionIdentifiers(identifiers);

        final int qos = Math.min(publish.getOnwardQoS().getQosNumber(), subscriptionQos);
        final QoS resolvedQoS = QoS.valueOf(qos);
        builder.withQoS(resolvedQoS != null ? resolvedQoS : QoS.AT_MOST_ONCE);

        if (qos == 0) {
            builder.withPacketIdentifier(0);
        }

        return builder.build();
    }
}
