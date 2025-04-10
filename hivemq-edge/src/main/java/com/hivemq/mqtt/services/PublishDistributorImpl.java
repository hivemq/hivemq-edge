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

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.ImmutableIntArray;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.bridge.MessageForwarderImpl;
import com.hivemq.bridge.config.LocalSubscription;
import com.hivemq.bridge.config.MqttBridge;
import com.hivemq.configuration.reader.BridgeExtractor;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.configuration.service.MqttConfigurationService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.hivemq.mqtt.handler.publish.PublishStatus;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.topic.SubscriberWithIdentifiers;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.persistence.clientsession.ClientSession;
import com.hivemq.persistence.clientsession.ClientSessionPersistence;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.persistence.util.FutureUtils;
import dagger.Lazy;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static com.hivemq.mqtt.handler.publish.PublishStatus.DELIVERED;
import static com.hivemq.mqtt.handler.publish.PublishStatus.FAILED;
import static com.hivemq.mqtt.handler.publish.PublishStatus.NOT_CONNECTED;
import static com.hivemq.sampling.SamplingService.SAMPLER_PREFIX;
import static com.hivemq.sampling.SamplingService.SAMPLER_QUEUE_LIMIT;

/**
 * @author Christoph Schäbel
 */
@Singleton
public class PublishDistributorImpl implements PublishDistributor {


    @NotNull
    private final PublishPayloadPersistence payloadPersistence;
    @NotNull
    private final ClientQueuePersistence clientQueuePersistence;
    @NotNull
    private final Lazy<ClientSessionPersistence> clientSessionPersistence;
    @NotNull
    private final SingleWriterService singleWriterService;
    @NotNull
    private final MqttConfigurationService mqttConfigurationService;
    @NotNull
    private final BridgeExtractor bridgeConfiguration;

    @Inject
    public PublishDistributorImpl(
            final @NotNull PublishPayloadPersistence payloadPersistence,
            final @NotNull ClientQueuePersistence clientQueuePersistence,
            final @NotNull Lazy<ClientSessionPersistence> clientSessionPersistence,
            final @NotNull SingleWriterService singleWriterService,
            final @NotNull ConfigurationService configurationService) {
        this.payloadPersistence = payloadPersistence;
        this.clientQueuePersistence = clientQueuePersistence;
        this.clientSessionPersistence = clientSessionPersistence;
        this.singleWriterService = singleWriterService;
        this.mqttConfigurationService = configurationService.mqttConfiguration();
        this.bridgeConfiguration = configurationService.bridgeExtractor();
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

            final ListenableFuture<PublishStatus> publishFuture = sendMessageToSubscriber(publish,
                    entry.getKey(),
                    subscriber.getQos(),
                    false,
                    subscriber.isRetainAsPublished(),
                    subscriber.getSubscriptionIdentifier());

            final SettableFuture<Void> publishFinishedFuture = SettableFuture.create();
            publishResultFutureBuilder.add(publishFinishedFuture);
            Futures.addCallback(publishFuture,
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
            final ListenableFuture<PublishStatus> future = sendMessageToSubscriber(publish,
                    sharedSubscriber,
                    publish.getQoS().getQosNumber(),
                    true,
                    true,
                    null);
            publishResultFutureBuilder.add(publishFinishedFuture);
            Futures.addCallback(future,
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

        return handlePublish(publish,
                clientId,
                subscriptionQos,
                sharedSubscription,
                retainAsPublished,
                subscriptionIdentifier);
    }

    private @NotNull ListenableFuture<PublishStatus> handlePublish(
            final @NotNull PUBLISH publish,
            final @NotNull String client,
            final int subscriptionQos,
            final boolean sharedSubscription,
            final boolean retainAsPublished,
            final @Nullable ImmutableIntArray subscriptionIdentifier) {

        if (sharedSubscription) {
            // only do the bridge iterations for client ids that can even be bridge clients
            if (client.startsWith(MessageForwarderImpl.FORWARDER_PREFIX)) {
                return handlePublishForBridgeForwarder(publish,
                        client,
                        retainAsPublished,
                        subscriptionIdentifier,
                        mqttConfigurationService.maxQueuedMessages(),
                        subscriptionQos);
            } else if (client.startsWith(SAMPLER_PREFIX)) {
                return queuePublish(client,
                        publish,
                        subscriptionQos,
                        true,
                        retainAsPublished,
                        subscriptionIdentifier,
                        SAMPLER_QUEUE_LIMIT);
            } else {
                return queuePublish(client,
                        publish,
                        subscriptionQos,
                        true,
                        retainAsPublished,
                        subscriptionIdentifier,
                        mqttConfigurationService.maxQueuedMessages());
            }
        }

        final boolean qos0Message = Math.min(subscriptionQos, publish.getQoS().getQosNumber()) == 0;
        final ClientSession clientSession = clientSessionPersistence.get().getSession(client, false);
        final boolean clientConnected = clientSession != null && clientSession.isConnected();

        if ((qos0Message && !clientConnected)) {
            return Futures.immediateFuture(NOT_CONNECTED);
        }

        //no session present or session already expired
        if (clientSession == null) {
            return Futures.immediateFuture(NOT_CONNECTED);
        }

        return queuePublish(client,
                publish,
                subscriptionQos,
                false,
                retainAsPublished,
                subscriptionIdentifier,
                clientSession.getQueueLimit());
    }

    private @NotNull SettableFuture<PublishStatus> handlePublishForBridgeForwarder(
            final @NotNull PUBLISH publish,
            final @NotNull String client,
            final boolean retainAsPublished,
            final @NotNull ImmutableIntArray subscriptionIdentifier,
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
                // if the bridge has the persist flag disabled, we reduce the QoS of the messages 0, so they are not stored in the file persistence in case.
                appliedQoS = 0;
            }
        }
        return queuePublish(client,
                publish,
                appliedQoS,
                true,
                retainAsPublished,
                subscriptionIdentifier,
                appliedQueueLimit);
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

        final Long appliedQueueLimit =
                Objects.requireNonNullElseGet(queueLimit, mqttConfigurationService::maxQueuedMessages);
        final ListenableFuture<Void> future = clientQueuePersistence.add(client,
                shared,
                createPublish(publish, subscriptionQos, retainAsPublished, subscriptionIdentifier),
                false,
                appliedQueueLimit);

        final SettableFuture<PublishStatus> statusFuture = SettableFuture.create();

        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(final @Nullable Void result) {
                statusFuture.set(DELIVERED);
            }

            @Override
            public void onFailure(final @NotNull Throwable t) {
                statusFuture.set(FAILED);
            }
        }, MoreExecutors.directExecutor());
        return statusFuture;
    }

    private @Nullable CustomBridgeLimitations getBridgeConfig(final @NotNull String clientId) {
        for (final MqttBridge bridge : bridgeConfiguration.getBridges()) {
            final String bridgeClientId = MessageForwarderImpl.FORWARDER_PREFIX + bridge.getId();
            if (clientId.contains(bridgeClientId)) {
                for (final LocalSubscription localSubscription : bridge.getLocalSubscriptions()) {
                    final String detailedBridgeClientId =
                            MessageForwarderImpl.FORWARDER_PREFIX + bridge.getId() + "-" + localSubscription.calculateUniqueId();
                    // contains as it ends with the topic filter, which we dont know
                    if (clientId.contains(detailedBridgeClientId)) {
                        return new CustomBridgeLimitations(bridge.isPersist(), localSubscription.getQueueLimit());
                    }
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

        final PUBLISHFactory.Mqtt5Builder builder = new PUBLISHFactory.Mqtt5Builder().fromPublish(publish)
                .withRetain(publish.isRetain() && retainAsPublished)
                .withSubscriptionIdentifiers(identifiers);

        final int qos = Math.min(publish.getOnwardQoS().getQosNumber(), subscriptionQos);
        builder.withQoS(QoS.valueOf(qos));

        if (qos == 0) {
            builder.withPacketIdentifier(0);
        }

        return builder.build();
    }
}
