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
package com.hivemq.mqtt.handler.publish;

import static com.hivemq.configuration.service.InternalConfigurations.DROP_MESSAGES_QOS_0_ENABLED;
import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.SESSION_EXPIRE_ON_DISCONNECT;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extensions.handler.IncomingPublishHandler;
import com.hivemq.mqtt.event.PublishDroppedEvent;
import com.hivemq.mqtt.event.PubrelDroppedEvent;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.MessageWithID;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5PubRecReasonCode;
import com.hivemq.mqtt.services.PublishPollService;
import com.hivemq.persistence.qos.IncomingMessageFlowPersistence;
import com.hivemq.persistence.util.FutureUtils;
import com.hivemq.util.ReasonStrings;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Florian Limpöck
 */
@SuppressWarnings("FutureReturnValueIgnored")
public class PublishFlowHandler extends ChannelDuplexHandler {

    @NotNull
    private static final Logger log = LoggerFactory.getLogger(PublishFlowHandler.class);

    // must be static as a new instance of qos receiver handler is in every channel
    private static final @NotNull AtomicLong UNACKNOWLEDGED_PUBLISHES_COUNTER = new AtomicLong();
    private static final long QOS2_LIMIT_WARNING_INTERVAL_NANOS = TimeUnit.MINUTES.toNanos(1);
    // static: one warning a minute for the node, not per channel
    private static final @NotNull AtomicLong LAST_QOS2_LIMIT_WARNING =
            new AtomicLong(System.nanoTime() - QOS2_LIMIT_WARNING_INTERVAL_NANOS - 1);

    private final @NotNull IncomingMessageFlowPersistence persistence;
    private final @NotNull OrderedTopicService orderedTopicService;
    private final @NotNull PublishPollService publishPollService;
    private final @NotNull IncomingPublishHandler incomingPublishHandler;
    private final @NotNull DropOutgoingPublishesHandler dropOutgoingPublishesHandler;
    private final @NotNull MqttServerDisconnector serverDisconnector;

    private final @NotNull Map<Integer, Boolean> qos1AlreadySentMap;

    @VisibleForTesting
    @Inject
    public PublishFlowHandler(
            final @NotNull PublishPollService publishPollService,
            final @NotNull IncomingMessageFlowPersistence persistence,
            final @NotNull OrderedTopicService orderedTopicService,
            final @NotNull IncomingPublishHandler incomingPublishHandler,
            final @NotNull DropOutgoingPublishesHandler dropOutgoingPublishesHandler,
            final @NotNull MqttServerDisconnector serverDisconnector) {
        this.publishPollService = publishPollService;
        this.persistence = persistence;
        this.orderedTopicService = orderedTopicService;
        this.qos1AlreadySentMap = new HashMap<>();
        this.incomingPublishHandler = incomingPublishHandler;
        this.dropOutgoingPublishesHandler = dropOutgoingPublishesHandler;
        this.serverDisconnector = serverDisconnector;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final @NotNull Object msg) throws Exception {
        final String client = ctx.channel()
                .attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME)
                .get()
                .getClientId();

        if (msg instanceof PUBLISH publish) {

            handlePublish(ctx, publish, client);

        } else if (msg instanceof PUBACK puback) {

            handlePuback(ctx, puback, client);

        } else if (msg instanceof PUBREC pubrec) {

            handlePubrec(ctx, pubrec, client);

        } else if (msg instanceof PUBREL pubrel) {

            handlePubrel(ctx, pubrel);

        } else if (msg instanceof PUBCOMP pubcomp) {

            handlePubcomp(ctx, pubcomp, client);

        } else {
            super.channelRead(ctx, msg);
        }
    }

    @Override
    public void write(
            final @NotNull ChannelHandlerContext ctx, final @NotNull Object msg, final @NotNull ChannelPromise promise)
            throws Exception {

        if (!(msg instanceof PUBLISH || msg instanceof PUBACK || msg instanceof PUBREL)) {
            if (msg instanceof PUBREC pubrec && pubrec.getReasonCode().getCode() >= 0x80) {
                // A PUBREC with an error code ends the flow: the client may reuse the identifier at once (MQTT 5,
                // 4.3.3), and kept, it would drop that next PUBLISH as a retransmission and count against the limits.
                // Released once the write completes, not when it starts: an outbound interceptor may hold the PUBREC,
                // and until it is sent a retransmission must still be recognised as one.
                final ChannelPromise written = promise.unvoid();
                written.addListener(future -> releaseIncomingQos2(ctx, pubrec.getPacketIdentifier()));
                super.write(ctx, msg, written);
                return;
            }
            super.write(ctx, msg, promise);
            return;
        }

        if (msg instanceof PUBACK puback) {
            final int messageId = puback.getPacketIdentifier();
            promise.addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    qos1AlreadySentMap.remove(messageId);
                    if (log.isTraceEnabled()) {
                        log.trace(
                                "Client '{}' completed a PUBLISH flow with QoS 1 for packet identifier '{}'",
                                ctx,
                                messageId);
                    }
                }
            });
        }

        final boolean flowComplete = orderedTopicService.handlePublish(ctx.channel(), msg, promise);
        if (flowComplete) {
            return;
        }

        if (DROP_MESSAGES_QOS_0_ENABLED) {
            final boolean messageDropped = dropOutgoingPublishesHandler.checkChannelNotWritable(ctx, msg, promise);
            if (messageDropped) {
                return;
            }
        }
        super.write(ctx, msg, promise);
    }

    @Override
    public void userEventTriggered(final @NotNull ChannelHandlerContext ctx, final @NotNull Object evt)
            throws Exception {
        if (evt instanceof PublishDroppedEvent publishDroppedEvent) {
            // Already logged, just proceeded with with the next message
            orderedTopicService.messageFlowComplete(
                    ctx, publishDroppedEvent.getMessage().getPacketIdentifier());
            return;
        }
        if (evt instanceof PubrelDroppedEvent pubrelDroppedEvent) {
            orderedTopicService.messageFlowComplete(
                    ctx, pubrelDroppedEvent.getMessage().getPacketIdentifier());
            return;
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {

        orderedTopicService.handleInactive();

        final ClientConnection clientConnection =
                ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
        final Long sessionExpiryInterval = clientConnection.getClientSessionExpiryInterval();

        // remove incoming message flow for not persisted client
        if (sessionExpiryInterval != null && sessionExpiryInterval == SESSION_EXPIRE_ON_DISCONNECT) {
            final String clientId = clientConnection.getClientId();
            if (clientId
                    != null) { // Just to be save. The client id should never be null, if the persistent session is not
                // null.
                persistence.delete(clientId);
            }
        }
        super.channelInactive(ctx);
    }

    @SuppressWarnings("NullAway") // client is always set when PUBLISH messages are flowing
    private void handlePublish(
            final @NotNull ChannelHandlerContext ctx, final @NotNull PUBLISH publish, final @Nullable String client)
            throws Exception {
        if (publish.getQoS() == QoS.AT_MOST_ONCE) { // do nothing
            incomingPublishHandler.interceptOrDelegate(ctx, publish, client);
            // QoS 1 or 2 duplicate delivery handling
        } else if (publish.getQoS() == QoS.AT_LEAST_ONCE) {
            UNACKNOWLEDGED_PUBLISHES_COUNTER.incrementAndGet();
            if (publish.isDuplicateDelivery() && qos1AlreadySentMap.get(publish.getPacketIdentifier()) != null) {
                log.debug(
                        "Client {} sent a duplicate publish message with id {}. This message is ignored",
                        client,
                        publish.getPacketIdentifier());
            } else {
                final int packetId = publish.getPacketIdentifier();
                qos1AlreadySentMap.put(publish.getPacketIdentifier(), true);
                firstPublishForMessageIdReceived(ctx, publish, client, packetId);
            }
            // QoS 2 duplicate delivery handling
        } else {
            final int messageId = publish.getPacketIdentifier();
            switch (persistence.add(client, messageId)) {
                case ADDED -> firstPublishForMessageIdReceived(ctx, publish, client, messageId);
                case ALREADY_PRESENT -> ctx.writeAndFlush(new PUBREC(messageId));
                case LIMIT_REACHED -> refuseQos2Publish(ctx, client, messageId);
            }
        }
    }

    /**
     * The client holds {@code max-incoming-qos2-flows-per-client} unreleased QoS 2 messages, or the node holds
     * {@code max-incoming-qos2-flows}. MQTT 5 can refuse the one message with PUBREC 0x97, which ends its flow and keeps
     * the connection; MQTT 3 has no negative PUBREC, so it is disconnected. The message is not forwarded either way.
     */
    private void refuseQos2Publish(
            final @NotNull ChannelHandlerContext ctx, final @NotNull String client, final int messageId) {
        final long now = System.nanoTime();
        final long last = LAST_QOS2_LIMIT_WARNING.get();
        if (now - last > QOS2_LIMIT_WARNING_INTERVAL_NANOS && LAST_QOS2_LIMIT_WARNING.compareAndSet(last, now)) {
            log.warn(
                    "A limit of unreleased inbound QoS 2 messages (max-incoming-qos2-flows-per-client or "
                            + "max-incoming-qos2-flows) is reached; QoS 2 PUBLISHes are refused. Last refused client: "
                            + "'{}'. Logged at most once a minute.",
                    client);
        }
        final ProtocolVersion protocolVersion = ctx.channel()
                .attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME)
                .get()
                .getProtocolVersion();
        if (protocolVersion == ProtocolVersion.MQTTv5) {
            ctx.writeAndFlush(new PUBREC(
                    messageId,
                    Mqtt5PubRecReasonCode.QUOTA_EXCEEDED,
                    ReasonStrings.QUOTA_EXCEEDED_INCOMING_QOS2_FLOWS,
                    Mqtt5UserProperties.NO_USER_PROPERTIES));
        } else {
            serverDisconnector.disconnect(
                    ctx.channel(),
                    "A client (IP: {}) sent a QoS 2 PUBLISH over the limit of QoS 2 messages awaiting PUBREL. "
                            + "Disconnecting client.",
                    "Sent a QoS 2 PUBLISH over the limit of QoS 2 messages awaiting PUBREL",
                    Mqtt5DisconnectReasonCode.QUOTA_EXCEEDED,
                    ReasonStrings.QUOTA_EXCEEDED_INCOMING_QOS2_FLOWS);
        }
    }

    @SuppressWarnings("NullAway") // the client id is set before any PUBREC is written
    private void releaseIncomingQos2(final @NotNull ChannelHandlerContext ctx, final int messageId) {
        persistence.remove(
                ctx.channel()
                        .attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME)
                        .get()
                        .getClientId(),
                messageId);
    }

    private void firstPublishForMessageIdReceived(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull PUBLISH publish,
            final @NotNull String client,
            final int messageId)
            throws Exception {
        incomingPublishHandler.interceptOrDelegate(ctx, publish, client);
        log.trace(
                "Client {} sent a publish message with id {} which was not forwarded before. This message is processed normally",
                client,
                messageId);
    }

    private void handlePuback(
            final @NotNull ChannelHandlerContext ctx, final PUBACK msg, final @Nullable String client) {

        log.trace("Client {}: Received PUBACK", client);
        final int messageId = msg.getPacketIdentifier();
        orderedTopicService.messageFlowComplete(ctx, messageId);
        if (log.isTraceEnabled()) {
            log.trace("Client {}: Received PUBACK remove message id:[{}] ", client, messageId);
        }
    }

    @SuppressWarnings("NullAway") // client is always set when PUBREC messages are flowing
    private void handlePubrec(
            final @NotNull ChannelHandlerContext ctx, final @NotNull PUBREC msg, final @Nullable String client) {
        log.trace("Client {}: Received pubrec", client);

        if (msg.getReasonCode() != Mqtt5PubRecReasonCode.SUCCESS
                && msg.getReasonCode() != Mqtt5PubRecReasonCode.NO_MATCHING_SUBSCRIBERS) {
            orderedTopicService.messageFlowComplete(ctx, ((MessageWithID) msg).getPacketIdentifier());
        }

        final ListenableFuture<Void> future = publishPollService.putPubrelInQueue(client, msg.getPacketIdentifier());
        FutureUtils.addExceptionLogger(future);
        if (log.isTraceEnabled()) {
            log.trace("Client {}: Received PUBREC remove message id:[{}]", client, msg.getPacketIdentifier());
        }
        // We send it with channel instead of context because otherwise we can't intercept the write in this handler
        ctx.channel().writeAndFlush(new PUBREL(msg.getPacketIdentifier()));
    }

    @SuppressWarnings("NullAway") // clientId is always set when messages are flowing
    private void handlePubrel(final ChannelHandlerContext ctx, final PUBREL pubrel) {
        final String client = ctx.channel()
                .attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME)
                .get()
                .getClientId();

        final int messageId = pubrel.getPacketIdentifier();

        // Released on PUBREL, not on PUBCOMP: the client cannot reuse the identifier before it receives PUBCOMP, and a
        // PUBCOMP that never flushes must not leave the identifier held
        persistence.remove(client, messageId);
        ctx.writeAndFlush(new PUBCOMP(messageId)).addListener(new PubcompSentListener(messageId, client));
    }

    private void handlePubcomp(
            final @NotNull ChannelHandlerContext ctx, final @NotNull PUBCOMP msg, final @Nullable String client) {
        log.trace("Client {}: Received PUBCOMP", client);

        orderedTopicService.messageFlowComplete(ctx, msg.getPacketIdentifier());
        if (log.isTraceEnabled()) {
            log.trace("Client {}: Received PUBCOMP remove message id:[{}]", client, msg.getPacketIdentifier());
        }
    }

    @Immutable
    private static class PubcompSentListener implements ChannelFutureListener {

        private final int messageId;
        private final @Nullable String client;

        PubcompSentListener(final int messageId, final @Nullable String client) {
            this.messageId = messageId;
            this.client = client;
        }

        @Override
        public void operationComplete(final ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
                UNACKNOWLEDGED_PUBLISHES_COUNTER.decrementAndGet();
                log.trace(
                        "Client '{}' completed a PUBLISH flow with QoS 1 or 2 for packet identifier '{}'",
                        client,
                        messageId);
            }
        }
    }
}
