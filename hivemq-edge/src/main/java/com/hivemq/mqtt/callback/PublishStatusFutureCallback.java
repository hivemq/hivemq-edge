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
package com.hivemq.mqtt.callback;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.bootstrap.ClientConnection;
import org.jetbrains.annotations.NotNull;
import com.hivemq.mqtt.handler.publish.PublishStatus;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.pool.FreePacketIdRanges;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.services.PublishPollService;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.persistence.util.FutureUtils;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CancellationException;

public class PublishStatusFutureCallback implements FutureCallback<PublishStatus> {

    @NotNull
    private static final Logger log = LoggerFactory.getLogger(PublishStatusFutureCallback.class);

    @NotNull
    private final PublishPayloadPersistence payloadPersistence;
    @NotNull
    private final PublishPollService publishPollService;
    private final boolean sharedSubscription;
    @NotNull
    private final String queueId;
    @NotNull
    private final PUBLISH publish;
    @NotNull
    private final FreePacketIdRanges messageIDPool;
    private final int packetIdentifier;
    @NotNull
    private final Channel channel;
    @NotNull
    private final String client;

    public PublishStatusFutureCallback(final @NotNull PublishPayloadPersistence payloadPersistence,
                                       final @NotNull PublishPollService publishPollService,
                                       final boolean sharedSubscription,
                                       final @NotNull String queueId,
                                       final @NotNull PUBLISH publish,
                                       final @NotNull FreePacketIdRanges messageIDPool,
                                       final @NotNull Channel channel,
                                       final @NotNull String client) {
        this.payloadPersistence = payloadPersistence;
        this.publishPollService = publishPollService;
        this.sharedSubscription = sharedSubscription;
        this.queueId = queueId;
        this.publish = publish;
        this.packetIdentifier = publish.getPacketIdentifier();
        this.messageIDPool = messageIDPool;
        this.channel = channel;
        this.client = client;
    }

    @Override
    public void onSuccess(final PublishStatus status) {
        try {
            if (status == PublishStatus.IN_PROGRESS) {
                log.error("'IN_PROGRESS' is not an expected publish status in the PublishStatusFutureCallback");
                return;
            }
            // QoS 0 messages are already removed
            if (publish.getQoS().getQosNumber() > 0) {
                if (sharedSubscription) {
                    if (status == PublishStatus.DELIVERED) {
                        final ListenableFuture<Void> future = publishPollService.removeMessageFromSharedQueue(queueId, publish.getUniqueId());
                        FutureUtils.addExceptionLogger(future);

                    } else if (status == PublishStatus.NOT_CONNECTED || status == PublishStatus.FAILED) {
                        final ListenableFuture<Void> future = publishPollService.removeInflightMarker(queueId, publish.getUniqueId());
                        FutureUtils.addExceptionLogger(future);
                    }
                } else {
                    if (status == PublishStatus.DELIVERED) {
                        final ListenableFuture<Void> future = publishPollService.removeMessageFromQueue(queueId, packetIdentifier);
                        FutureUtils.addExceptionLogger(future);
                    }
                }
            }
        } catch (final Exception e) {
            log.error("Exceptions in publish status callback handling, queue ID = " + queueId, e);
        }

        if (packetIdentifier != 0) {
            messageIDPool.returnId(packetIdentifier);
        }

        if (status != PublishStatus.NOT_CONNECTED) {
            checkForNewMessages();
        }
    }

    private void checkForNewMessages() {
        final ClientConnection clientConnection = channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
        if (clientConnection.decrementInFlightCount() > 0) {
            return;
        }
        publishPollService.pollMessages(client, channel);
    }

    @Override
    public void onFailure(final @NotNull Throwable throwable) {

        if (throwable instanceof CancellationException) {
            //ignore because task was cancelled because channel became inactive and
            //response has already been sent by callback from ChannelInactiveHandler
            return;
        }

        // the payloads for QoS-0 messages are not extracted and their reference count is not incremented.
        // therefor it must not be decremented
        if (publish.getQoS() != QoS.AT_MOST_ONCE) {
            payloadPersistence.decrementReferenceCounter(publish.getPublishId());
        }
    }
}
