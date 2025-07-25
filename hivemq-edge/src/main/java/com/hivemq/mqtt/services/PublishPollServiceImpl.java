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
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.ImmutableIntArray;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.bootstrap.ClientState;
import com.hivemq.configuration.service.InternalConfigurations;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.hivemq.mqtt.callback.PublishStatusFutureCallback;
import com.hivemq.mqtt.handler.publish.PublishStatus;
import com.hivemq.mqtt.message.MessageWithID;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.pool.FreePacketIdRanges;
import com.hivemq.mqtt.message.pool.exception.MessageIdUnavailableException;
import com.hivemq.mqtt.message.pool.exception.NoMessageIdAvailableException;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.message.publish.PublishWithFuture;
import com.hivemq.mqtt.message.publish.PubrelWithFuture;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.SubscriberWithQoS;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.persistence.clientsession.SharedSubscriptionService;
import com.hivemq.persistence.connection.ConnectionPersistence;
import com.hivemq.persistence.payload.PayloadPersistenceException;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.persistence.util.FutureUtils;
import com.hivemq.util.Exceptions;
import dagger.Lazy;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.configuration.service.InternalConfigurations.PUBLISH_POLL_BATCH_SIZE_BYTES;

@Singleton
public class PublishPollServiceImpl implements PublishPollService {


    private static final @NotNull Logger log = LoggerFactory.getLogger(PublishPollService.class);

    private final @NotNull ClientQueuePersistence clientQueuePersistence;
    private final @NotNull ConnectionPersistence connectionPersistence;
    private final @NotNull PublishPayloadPersistence payloadPersistence;
    private final @NotNull MessageDroppedService messageDroppedService;
    private final @NotNull Lazy<SharedSubscriptionService> sharedSubscriptionService;
    private final @NotNull SingleWriterService singleWriterService;

    @Inject
    public PublishPollServiceImpl(
            final @NotNull ClientQueuePersistence clientQueuePersistence,
            final @NotNull ConnectionPersistence connectionPersistence,
            final @NotNull PublishPayloadPersistence payloadPersistence,
            final @NotNull MessageDroppedService messageDroppedService,
            final @NotNull Lazy<SharedSubscriptionService> sharedSubscriptionService,
            final @NotNull SingleWriterService singleWriterService) {
        this.clientQueuePersistence = clientQueuePersistence;
        this.connectionPersistence = connectionPersistence;
        this.payloadPersistence = payloadPersistence;
        this.messageDroppedService = messageDroppedService;
        this.sharedSubscriptionService = sharedSubscriptionService;
        this.singleWriterService = singleWriterService;
    }

    @Override
    public void pollMessages(final @NotNull String client, final @NotNull Channel channel) {
        checkNotNull(client, "Client must not be null");
        checkNotNull(channel, "Channel must not be null");
        // Null equal false, true will never be set
        final ClientConnection clientConnection = channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
        final boolean inflightMessagesSent = clientConnection.isInFlightMessagesSent();
        if (inflightMessagesSent) {
            pollNewMessages(client, channel);
            final boolean noSharedSubscriptions = clientConnection.getNoSharedSubscription();
            if (noSharedSubscriptions) {
                return;
            }
            try {
                final ImmutableSet<Topic> topics = sharedSubscriptionService.get().getSharedSubscriptions(client);
                if (topics.isEmpty()) {
                    clientConnection.setNoSharedSubscription(true);
                    return;
                }
                for (final Topic topic : topics) {
                    final String sharedSubscriptions = sharedSubscriptionService.get().removePrefix(topic.getTopic());
                    pollSharedPublishesForClient(client,
                            sharedSubscriptions,
                            topic.getQoS().getQosNumber(),
                            topic.isRetainAsPublished(),
                            topic.getSubscriptionIdentifier(),
                            channel);
                }
            } catch (final ExecutionException e) {
                log.error("Exception while reading shared subscriptions for client {}", client, e);
            }

        } else {
            pollInflightMessages(client, channel);
        }
    }

    protected boolean canReceive(final @NotNull ClientConnection clientConnection) {
        if (clientConnection == null ||
                clientConnection.getClientState() == ClientState.DISCONNECTING ||
                clientConnection.getClientState().disconnected()) {
            return false; // client is disconnecting or disconnected
        }
        return true;
    }

    @Override
    public void pollNewMessages(final @NotNull String client) {
        final ClientConnection clientConnection = connectionPersistence.get(client);
        pollNewMessages(client, clientConnection.getChannel());
    }

    @Override
    public void pollNewMessages(final @NotNull String client, final @NotNull Channel channel) {

        //SLJ Edit - moved this check down to cover both poll methods
        final ClientConnection clientConnection = connectionPersistence.get(client);
        if (!canReceive(clientConnection)) {
            return;
        }

        final FreePacketIdRanges messageIDPool =
                channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().getMessageIDPool();
        final ImmutableIntArray messageIds;
        try {
            messageIds = createMessageIds(messageIDPool, pollMessageLimit(channel));
        } catch (final NoMessageIdAvailableException e) {
            // This should never happen if the limit for the poll message limit is set correctly
            log.error("No message id available for client " + client, e);
            return;
        }

        final ListenableFuture<ImmutableList<PUBLISH>> future =
                clientQueuePersistence.readNew(client, false, messageIds, PUBLISH_POLL_BATCH_SIZE_BYTES);

        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(final ImmutableList<PUBLISH> publishes) {
                // Return unused ID's
                int usedIds = 0;
                for (int i = 0; i < publishes.size(); i++) {
                    if (publishes.get(i).getQoS() != QoS.AT_MOST_ONCE) {
                        usedIds++;
                    }
                }
                for (int i = usedIds; i < messageIds.length(); i++) {
                    messageIDPool.returnId(messageIds.get(i));
                }
                final List<PublishWithFuture> publishesToSend = new ArrayList<>(publishes.size());
                final ClientConnection clientConnection = channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
                // Add all messages to the in-flight count before sending them out.
                // Avoids polling messages again if the counter falls to 0 due to in-flight messages finishing
                // concurrently while we're still sending others here.
                clientConnection.incrementInFlightCount(publishes.size());
                for (int i = 0, publishesSize = publishes.size(); i < publishesSize; i++) {
                    final PUBLISH publish = publishes.get(i);
                    try {
                        final SettableFuture<PublishStatus> publishFuture = SettableFuture.create();
                        Futures.addCallback(publishFuture,
                                new PublishStatusFutureCallback(payloadPersistence,
                                        PublishPollServiceImpl.this,
                                        false,
                                        client,
                                        publish,
                                        messageIDPool,
                                        channel,
                                        client),
                                MoreExecutors.directExecutor());
                        final PublishWithFuture publishWithFuture =
                                new PublishWithFuture(publish, publishFuture, false, payloadPersistence);
                        publishesToSend.add(publishWithFuture);
                    } catch (final PayloadPersistenceException e) {
                        // We don't prevent other messages form being published in case the reference is missing
                        log.error("Payload reference error for publish on topic: " + publish.getTopic(), e);
                        if (publish.getQoS().getQosNumber() > 0) {
                            removeMessageFromQueue(client, publish.getPacketIdentifier());
                        }
                        clientConnection.decrementInFlightCount();
                        messageDroppedService.failed(client, publish.getTopic(), publish.getQoS().getQosNumber());
                    }
                }
                channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME)
                        .get()
                        .getPublishFlushHandler()
                        .sendPublishes(publishesToSend);
            }

            @Override
            public void onFailure(final Throwable t) {
                Exceptions.rethrowError("Exception in new messages handling", t);
                channel.disconnect();
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    public void pollInflightMessages(final @NotNull String client, final @NotNull Channel channel) {
        final ListenableFuture<ImmutableList<MessageWithID>> future =
                clientQueuePersistence.readInflight(client, PUBLISH_POLL_BATCH_SIZE_BYTES, pollMessageLimit(channel));
        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(final ImmutableList<MessageWithID> messages) {

                final ClientConnection clientConnection = channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();

                if (messages.isEmpty()) {
                    clientConnection.setInFlightMessagesSent(true);
                    channel.eventLoop().submit(() -> pollMessages(client, channel)); // No more inflight messages
                    return;
                }

//                final AtomicInteger inFlightMessageCount = inFlightMessageCount(channel);
                final List<PublishWithFuture> publishesToSend = new ArrayList<>(messages.size());
                // Add all messages to the in-flight count before sending them out.
                // Avoids polling messages again if the counter falls to 0 due to in-flight messages finishing
                // concurrently while we're still sending others here.
                clientConnection.incrementInFlightCount(messages.size());
                for (int i = 0, messagesSize = messages.size(); i < messagesSize; i++) {
                    final MessageWithID message = messages.get(i);
                    final FreePacketIdRanges messageIDPool = clientConnection.getMessageIDPool();
                    try {
                        messageIDPool.takeSpecificId(message.getPacketIdentifier());

                    } catch (final MessageIdUnavailableException e) {
                        log.warn("The desired packet ID was not available when polling inflight messages: {}",
                                e.getMessage());
                    }
                    if (message instanceof PUBLISH) {
                        final PUBLISH publish = (PUBLISH) message;
                        try {
                            final SettableFuture<PublishStatus> publishFuture = SettableFuture.create();
                            Futures.addCallback(publishFuture,
                                    new PublishStatusFutureCallback(payloadPersistence,
                                            PublishPollServiceImpl.this,
                                            false,
                                            client,
                                            publish,
                                            messageIDPool,
                                            channel,
                                            client),
                                    MoreExecutors.directExecutor());
                            final PublishWithFuture publishWithFuture =
                                    new PublishWithFuture(publish, publishFuture, false, payloadPersistence);
                            publishesToSend.add(publishWithFuture);
                        } catch (final PayloadPersistenceException e) {
                            // We don't prevent other messages from being published in case of a missing reference
                            log.error("Payload reference error for publish on topic: " + publish.getTopic(), e);
                            if (publish.getQoS().getQosNumber() > 0) {
                                removeMessageFromQueue(client, publish.getPacketIdentifier());
                            }
                            clientConnection.decrementInFlightCount();
                            messageDroppedService.failed(client, publish.getTopic(), publish.getQoS().getQosNumber());
                        }
                    } else if (message instanceof PUBREL) {
                        // We don't care if the message is delivered successfully here.
                        // If the client disconnects before we receive a PUBCOMP we will retry anyways.
                        final SettableFuture<PublishStatus> settableFuture = SettableFuture.create();
                        channel.writeAndFlush(new PubrelWithFuture((PUBREL) message, settableFuture));
                        Futures.addCallback(settableFuture,
                                new PubrelResendCallback(client, message, messageIDPool, channel),
                                MoreExecutors.directExecutor());
                    }
                }
                clientConnection.getPublishFlushHandler().sendPublishes(publishesToSend);
            }

            @Override
            public void onFailure(final Throwable t) {
                Exceptions.rethrowError("Exception in inflight messages handling", t);
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    public void pollSharedPublishes(final @NotNull String sharedSubscription) {
        final List<SubscriberWithQoS> subscribers =
                new ArrayList<>(sharedSubscriptionService.get().getSharedSubscriber(sharedSubscription));
        // Don't shuffle the whole Collection at once here, as this is CPU-intensive for many subscribers.
        // Instead, use an approach similar to what Collections.shuffle does: Iterate backwards, randomly choose
        // an element below the back index, and swap it with the element at the back index if it can't be used.
        for (int backIndex = subscribers.size(); backIndex > 0; backIndex--) {
            final int chosenIndex = ThreadLocalRandom.current().nextInt(backIndex);
            final SubscriberWithQoS subscriber = subscribers.get(chosenIndex);
            final ClientConnection clientConnection = connectionPersistence.get(subscriber.getSubscriber());
            if (clientConnection == null || !clientConnection.getChannel().isActive()) {
                // The subscriber is disconnected.
                // Swap it with the back index so that any other subscriber could be chosen next.
                final SubscriberWithQoS backSubscriber = subscribers.get(backIndex - 1);
                subscribers.set(chosenIndex, backSubscriber);
                continue;
            }
            pollSharedPublishesForClient(subscriber.getSubscriber(),
                    sharedSubscription,
                    subscriber.getQos(),
                    subscriber.isRetainAsPublished(),
                    subscriber.getSubscriptionIdentifier(),
                    clientConnection.getChannel());
        }
    }

    @Override
    public void pollSharedPublishesForClient(
            final @NotNull String client,
            final @NotNull String sharedSubscription,
            final int qos,
            final boolean retainAsPublished,
            final @Nullable Integer subscriptionIdentifier,
            final @NotNull Channel channel) {
        final ClientConnection clientConnection = channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
        if (clientConnection.isMessagesInFlight()) {
            return;
        }

        final ListenableFuture<ImmutableList<PUBLISH>> future = clientQueuePersistence.readShared(sharedSubscription,
                pollMessageLimit(channel),
                PUBLISH_POLL_BATCH_SIZE_BYTES);

        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(final @NotNull ImmutableList<PUBLISH> publishes) {
                if (publishes.isEmpty()) {
                    return;
                }
                final FreePacketIdRanges freePacketIdRanges = clientConnection.getMessageIDPool();
                final List<PublishWithFuture> publishesToSend = new ArrayList<>(publishes.size());
                // Add all messages to the in-flight count before sending them out.
                // Avoids polling messages again if the counter falls to 0 due to in-flight messages finishing
                // concurrently while we're still sending others here.
                clientConnection.incrementInFlightCount(publishes.size());
                for (int i = 0, publishesSize = publishes.size(); i < publishesSize; i++) {
                    PUBLISH publish = publishes.get(i);
                    try {
                        if (publish.getOnwardQoS().getQosNumber() > 0 && qos == 0) {
                            // In case the messages gets downgraded to qos 0, it can be removed.
                            removeMessageFromSharedQueue(sharedSubscription, publish.getUniqueId());
                        }
                        // We can't sent the qos when the message is queue, because we don't know the which client is will be sent
                        final QoS minQos = QoS.valueOf(Math.min(qos, publish.getOnwardQoS().getQosNumber()));
                        // There can only be one subscription ID for this message, because there are no overlapping shared subscriptions
                        final ImmutableIntArray subscriptionIdentifiers = subscriptionIdentifier != null ?
                                ImmutableIntArray.of(subscriptionIdentifier) :
                                ImmutableIntArray.of();
                        int packetId = 0;
                        if (minQos.getQosNumber() > 0) {
                            packetId = freePacketIdRanges.takeNextId();
                        }
                        publish = new PUBLISHFactory.Mqtt5Builder().fromPublish(publish)
                                .withPacketIdentifier(packetId)
                                .withQoS(minQos)
                                .withOnwardQos(minQos)
                                .withRetain(publish.isRetain() && retainAsPublished)
                                .withSubscriptionIdentifiers(subscriptionIdentifiers)
                                .build();
                    } catch (final NoMessageIdAvailableException e) {
                        // This should never happen if the limit for the poll message limit is set correctly
                        log.error("No message id available for client: " +
                                client +
                                ", shared subscription " +
                                sharedSubscription, e);
                        messageDroppedService.queueFullShared(sharedSubscription,
                                publish.getTopic(),
                                publish.getQoS().getQosNumber());
                        clientConnection.decrementInFlightCount();
                        return;
                    }
                    try {
                        final SettableFuture<PublishStatus> publishFuture = SettableFuture.create();
                        Futures.addCallback(publishFuture,
                                new PublishStatusFutureCallback(payloadPersistence,
                                        PublishPollServiceImpl.this,
                                        true,
                                        sharedSubscription,
                                        publish,
                                        freePacketIdRanges,
                                        channel,
                                        client),
                                MoreExecutors.directExecutor());
                        final PublishWithFuture publishWithFuture =
                                new PublishWithFuture(publish, publishFuture, false, payloadPersistence);
                        publishesToSend.add(publishWithFuture);
                    } catch (final PayloadPersistenceException e) {
                        // We don't prevent other messages form being published in case on reference is missing
                        log.error("Payload reference error for publish on topic: " + publish.getTopic(), e);
                        if (publish.getQoS().getQosNumber() > 0) {
                            removeMessageFromSharedQueue(sharedSubscription, publish.getUniqueId());
                        }
                        clientConnection.decrementInFlightCount();
                        messageDroppedService.failed(client, publish.getTopic(), publish.getQoS().getQosNumber());
                    }
                }
                clientConnection.getPublishFlushHandler().sendPublishes(publishesToSend);
            }

            @Override
            public void onFailure(final @NotNull Throwable t) {
                Exceptions.rethrowError("Exception in shared publishes poll handling for client " +
                        client +
                        "for shared subscription " +
                        sharedSubscription, t);
            }
        }, MoreExecutors.directExecutor());
    }

    @NotNull
    @Override
    public ListenableFuture<Void> removeMessageFromQueue(final @NotNull String client, final int packetId) {
        return clientQueuePersistence.remove(client, packetId);
    }

    @NotNull
    @Override
    public ListenableFuture<Void> removeMessageFromSharedQueue(
            final @NotNull String sharedSubscription,
            final @NotNull String uniqueId) {
        return clientQueuePersistence.removeShared(sharedSubscription, uniqueId);
    }

    @NotNull
    @Override
    public ListenableFuture<Void> putPubrelInQueue(final @NotNull String client, final int packetId) {
        return clientQueuePersistence.putPubrel(client, packetId);
    }

    @NotNull
    @Override
    public ListenableFuture<Void> removeInflightMarker(
            final @NotNull String sharedSubscription,
            final @NotNull String uniqueId) {
        return clientQueuePersistence.removeInFlightMarker(sharedSubscription, uniqueId);
    }

    @NotNull
    private ImmutableIntArray createMessageIds(
            final @NotNull FreePacketIdRanges messageIDPool,
            final int pollMessageLimit) throws NoMessageIdAvailableException {
        final ImmutableIntArray.Builder builder = ImmutableIntArray.builder(pollMessageLimit);
        for (int i = 0; i < pollMessageLimit; i++) {
            final int nextId = messageIDPool.takeNextId();
            builder.add(nextId);
        }
        return builder.build();
    }

    private int pollMessageLimit(final @NotNull Channel channel) {
        final ClientConnection clientConnection = channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
        final int maxInflightWindow = (clientConnection == null) ?
                InternalConfigurations.MAX_INFLIGHT_WINDOW_SIZE_MESSAGES :
                clientConnection.getMaxInflightWindow(InternalConfigurations.MAX_INFLIGHT_WINDOW_SIZE_MESSAGES);
        return Math.max(InternalConfigurations.PUBLISH_POLL_BATCH_SIZE, maxInflightWindow);
    }

    private class PubrelResendCallback implements FutureCallback<PublishStatus> {
        @NotNull
        private final String client;
        @NotNull
        private final MessageWithID message;
        @NotNull
        private final FreePacketIdRanges messageIDPool;
        @NotNull
        private final Channel channel;

        PubrelResendCallback(
                final @NotNull String client,
                final @NotNull MessageWithID message,
                final @NotNull FreePacketIdRanges messageIDPool,
                final @NotNull Channel channel) {
            this.client = client;
            this.message = message;
            this.messageIDPool = messageIDPool;
            this.channel = channel;
        }

        @Override
        public void onSuccess(final @NotNull PublishStatus result) {
            messageIDPool.returnId(message.getPacketIdentifier());
            if (result != PublishStatus.NOT_CONNECTED) {
                final ListenableFuture<Void> future = removeMessageFromQueue(client, message.getPacketIdentifier());
                FutureUtils.addExceptionLogger(future);
            }
            if (channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().decrementInFlightCount() > 0) {
                return;
            }
            pollMessages(client, channel);
        }

        @Override
        public void onFailure(final Throwable t) {
            Exceptions.rethrowError("Pubrel delivery failed", t);
            messageIDPool.returnId(message.getPacketIdentifier());
            channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().decrementInFlightCount();
        }
    }
}
