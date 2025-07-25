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
package com.hivemq.mqtt.handler.subscribe;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.*;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.auth.DefaultAuthorizationBehaviour;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extensions.packets.general.ModifiableDefaultPermissionsImpl;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.handler.publish.DefaultPermissionsEvaluator;
import com.hivemq.mqtt.handler.subscribe.retained.RetainedMessagesSender;
import com.hivemq.mqtt.handler.subscribe.retained.SendRetainedMessagesListener;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5SubAckReasonCode;
import com.hivemq.mqtt.message.suback.SUBACK;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqttsn.IMqttsnTopicRegistry;
import com.hivemq.mqttsn.MqttsnProtocolException;
import com.hivemq.mqttsn.MqttsnTopicAlias;
import com.hivemq.persistence.clientsession.ClientSessionSubscriptionPersistence;
import com.hivemq.persistence.clientsession.SharedSubscriptionService;
import com.hivemq.persistence.clientsession.SharedSubscriptionServiceImpl.SharedSubscription;
import com.hivemq.persistence.clientsession.callback.SubscriptionResult;
import com.hivemq.persistence.retained.RetainedMessagePersistence;
import com.hivemq.util.Exceptions;
import com.hivemq.util.ReasonStrings;
import com.hivemq.util.Topics;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.codec.MqttsnCodecs;
import org.slj.mqtt.sn.spi.IMqttsnMessage;

import jakarta.inject.Inject;
import java.util.*;

import static com.hivemq.mqtt.message.ProtocolVersion.MQTTSNv1_2;
import static com.hivemq.mqtt.message.ProtocolVersion.MQTTSNv2_0;
import static com.hivemq.mqtt.message.reason.Mqtt5SubAckReasonCode.UNSPECIFIED_ERROR;
import static com.hivemq.mqtt.message.reason.Mqtt5SubAckReasonCode.fromCode;


/**
 * The service which is responsible for handling the subscriptions of MQTT clients
 */
public class IncomingSubscribeService {

    protected static final @NotNull Logger log = LoggerFactory.getLogger(IncomingSubscribeService.class);

    @NotNull
    private static final Comparator<Topic> TOPIC_AND_QOS_COMPARATOR = new Comparator<>() {
        @Override
        public int compare(final Topic o1, final Topic o2) {
            final int strComp = o1.getTopic().compareTo(o2.getTopic());
            if (strComp == 0) {
                return o1.getQoS().getQosNumber() - o2.getQoS().getQosNumber();
            }
            return strComp;
        }
    };

    private final @NotNull ClientSessionSubscriptionPersistence clientSessionSubscriptionPersistence;
    private final @NotNull RetainedMessagePersistence retainedMessagePersistence;
    private final @NotNull SharedSubscriptionService sharedSubscriptionService;
    private final @NotNull RetainedMessagesSender retainedMessagesSender;
    private final @NotNull MqttConfigurationService mqttConfigurationService;
    private final @NotNull RestrictionsConfigurationService restrictionsConfigurationService;
    private final @NotNull MqttServerDisconnector mqttServerDisconnector;
    private final @NotNull IMqttsnTopicRegistry mqttsnTopicRegistry;

    @Inject
    protected IncomingSubscribeService(final @NotNull ClientSessionSubscriptionPersistence clientSessionSubscriptionPersistence,
                                       final @NotNull RetainedMessagePersistence retainedMessagePersistence,
                                       final @NotNull SharedSubscriptionService sharedSubscriptionService,
                                       final @NotNull RetainedMessagesSender retainedMessagesSender,
                                       final @NotNull MqttConfigurationService mqttConfigurationService,
                                       final @NotNull RestrictionsConfigurationService restrictionsConfigurationService,
                                       final @NotNull MqttServerDisconnector mqttServerDisconnector,
                                       final @NotNull IMqttsnTopicRegistry mqttsnTopicRegistry) {

        this.clientSessionSubscriptionPersistence = clientSessionSubscriptionPersistence;
        this.retainedMessagePersistence = retainedMessagePersistence;
        this.sharedSubscriptionService = sharedSubscriptionService;
        this.retainedMessagesSender = retainedMessagesSender;
        this.mqttConfigurationService = mqttConfigurationService;
        this.restrictionsConfigurationService = restrictionsConfigurationService;
        this.mqttServerDisconnector = mqttServerDisconnector;
        this.mqttsnTopicRegistry = mqttsnTopicRegistry;
    }

    public void processSubscribe(final @NotNull ChannelHandlerContext ctx, final @NotNull SUBSCRIBE msg, final boolean authorizersPresent) {
        processSubscribe(ctx, msg, new Mqtt5SubAckReasonCode[msg.getTopics().size()], new String[msg.getTopics().size()], authorizersPresent);
    }

    public void processSubscribe(final @NotNull ChannelHandlerContext ctx,
                                 final @NotNull SUBSCRIBE msg,
                                 final @NotNull Mqtt5SubAckReasonCode @NotNull [] providedCodes,
                                 final @NotNull String @NotNull [] reasonStrings,
                                 final boolean authorizersPresent) {

        if (!hasOnlyValidSubscriptions(ctx, msg)) {
            return;
        }

        authorizeSubscriptions(ctx, msg, providedCodes, reasonStrings, authorizersPresent);
    }

    private void authorizeSubscriptions(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull SUBSCRIBE msg,
            final @NotNull Mqtt5SubAckReasonCode @NotNull [] providedCodes,
            final @NotNull String @NotNull [] reasonStrings,
            final boolean authorizersPresent) {

        final ModifiableDefaultPermissions permissions = ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().getAuthPermissions();
        final ModifiableDefaultPermissionsImpl defaultPermissions = (ModifiableDefaultPermissionsImpl) permissions;

        for (int i = 0; i < msg.getTopics().size(); i++) {

            if (providedCodes[i] != null) {
                continue;
            }

            //if authorizers are present and no permissions are available and the default behaviour has not been changed
            //then we deny the subscription
            if (authorizersPresent && (defaultPermissions == null || (defaultPermissions.asList().size() < 1
                    && defaultPermissions.getDefaultBehaviour() == DefaultAuthorizationBehaviour.ALLOW
                    && !defaultPermissions.isDefaultAuthorizationBehaviourOverridden()))) {
                providedCodes[i] = Mqtt5SubAckReasonCode.NOT_AUTHORIZED;
                continue;
            }

            final Topic subscription = msg.getTopics().get(i);
            if (!DefaultPermissionsEvaluator.checkSubscription(permissions, subscription)) {
                providedCodes[i] = Mqtt5SubAckReasonCode.NOT_AUTHORIZED;
                //build reason string (concat multiple reasons)

                reasonStrings[i] = "Not authorized to subscribe to topic '" + subscription.getTopic() + "' with QoS '" +
                        subscription.getQoS().getQosNumber() + "'";
            }
        }

        final StringBuilder reasonStringBuilder = new StringBuilder();

        for (final String reasonString : reasonStrings) {
            //noinspection ConstantConditions
            if (reasonString != null && !reasonString.isEmpty()) {
                reasonStringBuilder.append(reasonString).append(". ");
            }
        }

        persistSubscriptionForClient(ctx, msg, providedCodes, reasonStringBuilder.length() > 0 ? reasonStringBuilder.toString() : null);
    }

    /**
     * Checks if the SUBSCRIBE message contains only valid topic subscriptions
     *
     * @param ctx The ChannelHandlerContext
     * @param msg the SUBSCRIBE message to check
     * @return <code>true</code> if only valid subscriptions are contained, <code>false</code> otherwise
     */
    private boolean hasOnlyValidSubscriptions(final ChannelHandlerContext ctx, final SUBSCRIBE msg) {
        final ClientConnection clientConnection = ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
        log.trace("Checking SUBSCRIBE message of client '{}' if topics are valid", clientConnection.getClientId());

        final int maxTopicLength = restrictionsConfigurationService.maxTopicLength();
        for (final Topic topic : msg.getTopics()) {
            final String topicString = topic.getTopic();
            if (!Topics.isValidToSubscribe(topicString)) {
                final String logMessage = "Disconnecting client '" + clientConnection.getClientId() + "'  (IP: {}) because it sent an invalid subscription: '" + topic.getTopic() + "'";
                mqttServerDisconnector.disconnect(
                        ctx.channel(),
                        logMessage,
                        "Invalid subscription topic " + topic.getTopic(),
                        Mqtt5DisconnectReasonCode.TOPIC_FILTER_INVALID,
                        ReasonStrings.DISCONNECT_SUBSCRIBE_TOPIC_FILTER_INVALID);
                return false;
            } else if (topicString.length() > maxTopicLength) {
                final String logMessage = "Disconnecting client '" + clientConnection.getClientId() + "'  (IP: {}) because it sent a subscription to a topic exceeding the maximum topic length: '" + topic.getTopic() + "'";
                mqttServerDisconnector.disconnect(
                        ctx.channel(),
                        logMessage,
                        "Sent SUBSCRIBE for topic that exceeds maximum topic length",
                        Mqtt5DisconnectReasonCode.TOPIC_FILTER_INVALID,
                        ReasonStrings.DISCONNECT_SUBSCRIBE_TOPIC_FILTER_INVALID);
                return false;
            }
        }
        return true;
    }

    private void persistSubscriptionForClient(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull SUBSCRIBE msg,
            final @Nullable Mqtt5SubAckReasonCode @Nullable [] providedCodes,
            final @Nullable String reasonString) {

        final ClientConnection clientConnection = ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
        final String clientId = clientConnection.getClientId();
        downgradeSharedSubscriptions(msg);

        final ProtocolVersion mqttVersion = clientConnection.getProtocolVersion();
        final Mqtt5SubAckReasonCode[] answerCodes = providedCodes != null ? providedCodes : new Mqtt5SubAckReasonCode[msg.getTopics().size()];

        final ImmutableList.Builder<ListenableFuture<SubscriptionResult>> singleAddFutures = ImmutableList.builder();
        int futureCount = 0;

        final Set<Topic> ignoredTopics = new HashSet<>();

        for (int i = 0; i < msg.getTopics().size(); i++) {
            final Topic topic = msg.getTopics().get(i);

            if (answerCodes[i] == null || answerCodes[i].getCode() < 128) {
                if (!mqttConfigurationService.wildcardSubscriptionsEnabled() && Topics.containsWildcard(topic.getTopic())) {
                    final String logMessage = "Client '" + clientId + "' (IP: {}) sent a SUBSCRIBE with a wildcard character in the topic filter '" + topic.getTopic() + "', although wildcard subscriptions are disabled. Disconnecting client.";
                    mqttServerDisconnector.disconnect(
                            ctx.channel(),
                            logMessage,
                            "Sent a SUBSCRIBE with a wildcard character in the topic filter '" + topic.getTopic() + "', although wildcard subscriptions are disabled",
                            Mqtt5DisconnectReasonCode.WILDCARD_SUBSCRIPTION_NOT_SUPPORTED,
                            ReasonStrings.DISCONNECT_WILDCARD_SUBSCRIPTIONS_NOT_SUPPORTED);
                    return;
                } else if (!mqttConfigurationService.sharedSubscriptionsEnabled() && Topics.isSharedSubscriptionTopic(topic.getTopic())) {
                    final String logMessage = "Client '" + clientId + "' (IP: {}) sent a SUBSCRIBE, which matches a shared subscription '" + topic.getTopic() + "', although shared subscriptions are disabled. Disconnecting client.";
                    mqttServerDisconnector.disconnect(
                            ctx.channel(),
                            logMessage,
                            "Sent a SUBSCRIBE, which matches a shared subscription '" + topic.getTopic() + "', although shared subscriptions are disabled",
                            Mqtt5DisconnectReasonCode.SHARED_SUBSCRIPTION_NOT_SUPPORTED,
                            ReasonStrings.DISCONNECT_SHARED_SUBSCRIPTIONS_NOT_SUPPORTED);
                    return;
                } else {
                    final Mqtt5SubAckReasonCode reasonCode = fromCode(topic.getQoS().getQosNumber());
                    answerCodes[i] = reasonCode != null ? reasonCode : UNSPECIFIED_ERROR;
                }
            }

            if (answerCodes[i].getCode() >= 128) { // every code >= 128 is an error code
                if (mqttVersion == ProtocolVersion.MQTTv3_1) {
                    handleInsufficientPermissionsV31(ctx, topic);
                    return;
                } else {
                    ignoredTopics.add(topic);
                    log.trace("Ignoring subscription for client [{}] and topic [{}] with qos [{}] because the client is not permitted", clientId, topic.getTopic(), topic.getQoS());
                }
            }
        }

        final Set<Topic> cleanedSubscriptions = getCleanedSubscriptions(msg);
        final Set<Topic> cleanedSubscriptionsWithQoS = Sets.newTreeSet(TOPIC_AND_QOS_COMPARATOR);
        cleanedSubscriptionsWithQoS.addAll(cleanedSubscriptions);

        ListenableFuture<ImmutableList<SubscriptionResult>> batchedFuture = null;
        if (batch(cleanedSubscriptions)) {
            //this should never happen for MQTT-SN
            cleanedSubscriptions.removeAll(ignoredTopics);
            batchedFuture = persistBatchedSubscriptions(clientId, msg, cleanedSubscriptions, mqttVersion, answerCodes);
            futureCount++;
        } else {
            for (int i = 0; i < msg.getTopics().size(); i++) {
                final Topic topic = msg.getTopics().get(i);
                if (ignoredTopics.contains(topic) || !cleanedSubscriptionsWithQoS.contains(topic)) {
                    continue;
                }
                answerCodes[i] = fromCode(topic.getQoS().getQosNumber());

                final SettableFuture<SubscriptionResult> settableFuture = SettableFuture.create();
                singleAddFutures.add(settableFuture);
                futureCount++;

                //allow this to be hooked
                ListenableFuture<SubscriptionResult> addSubscriptionFuture;
                if (mqttVersion == MQTTSNv1_2 || mqttVersion == MQTTSNv2_0) {
                    try {
                        final Optional<MqttsnTopicAlias> alias = mqttsnTopicRegistry.readTopicAlias(clientId, topic.getTopic());
                        if (alias.isEmpty()) {
                            mqttsnTopicRegistry.register(clientId, topic.getTopic());
                        }
                    } catch (final MqttsnProtocolException e) {
                        log.error("error registering alias", e);
                        addSubscriptionFuture = Futures.immediateFailedFuture(e);
                    }
                }
                addSubscriptionFuture = clientSessionSubscriptionPersistence.addSubscription(clientId, topic);

                Futures.addCallback(addSubscriptionFuture, new SubscribePersistenceCallback(settableFuture, clientId, topic, mqttVersion, answerCodes, i),
                        MoreExecutors.directExecutor());
            }
        }

        log.trace("Applied all subscriptions for client [{}]", clientId);
        if (futureCount == 0) {
            //we don't need to check for retained messages here, because we did not persist any of the subscriptions
            final Object out = createSuback(clientId, msg.getTopics(), msg.getPacketIdentifier(), ImmutableList.copyOf(answerCodes), reasonString, mqttVersion);
            if (out != null) {
                ctx.channel().writeAndFlush(out);
            }
            return;
        }

        final SettableFuture<List<SubscriptionResult>> addResultsFuture = SettableFuture.create();

        if (batchedFuture != null) {
            addResultsFuture.setFuture(batchedFuture);
        } else {
            addResultsFuture.setFuture(Futures.allAsList(singleAddFutures.build()));
        }

        sendSubackAndRetainedMessages(clientId, ctx, msg, answerCodes, addResultsFuture, ignoredTopics, reasonString, mqttVersion);
    }

    private void sendSubackAndRetainedMessages(final String clientId, final ChannelHandlerContext ctx, final @NotNull SUBSCRIBE msg, final @NotNull Mqtt5SubAckReasonCode[] answerCodes,
                                               final @NotNull SettableFuture<List<SubscriptionResult>> addResultsFuture, final @NotNull Set<Topic> ignoredTopics,
                                               final @Nullable String reasonString, final @NotNull ProtocolVersion mqttVersion) {

        Futures.addCallback(addResultsFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(final @Nullable List<SubscriptionResult> subscriptionResults) {

                final Object out = createSuback(clientId, msg.getTopics(), msg.getPacketIdentifier(), ImmutableList.copyOf(answerCodes), reasonString, mqttVersion);
                if (out != null) {
                    final ChannelFuture future = ctx.channel().writeAndFlush(out);
                    // actually the ignoredTopics are unnecessary in this case, as the batching logic already applies the filtering
                    if (subscriptionResults != null) {
                        future.addListener(new SendRetainedMessagesListener(subscriptionResults, ignoredTopics, retainedMessagePersistence, retainedMessagesSender));
                    }
                }
            }

            @Override
            public void onFailure(final @NotNull Throwable throwable) {
                //Already logged
                ctx.channel().disconnect();
            }
        }, ctx.executor());
    }

    /**
     * If a Server receives a SUBSCRIBE packet that contains multiple Topic Filters it MUST handle that packet as if it
     * had received a sequence of multiple SUBSCRIBE packets.
     * <p>
     * This means we can delete subscribes to a topic, that are in sequence before another subscribe to the same topic.
     *
     * @param msg a SUBSCRIBE message
     * @return the cleaned subscriptions
     */
    @NotNull
    private Set<Topic> getCleanedSubscriptions(final SUBSCRIBE msg) {
        final List<Topic> topics = msg.getTopics();
        final int size = topics.size();
        if (size < 2) {
            return Sets.newHashSet(topics);
        }
        final Set<Topic> cleanedTopics = Sets.newHashSetWithExpectedSize(size);
        // we expect the topics to be mostly different
        for (final Topic topic : topics) {
            if (!cleanedTopics.add(topic)) {
                cleanedTopics.remove(topic);
                cleanedTopics.add(topic);
            }
        }
        return cleanedTopics;
    }

    @VisibleForTesting
    static boolean batch(final @NotNull Set<Topic> topics) {
        return topics.size() >= 2;
    }

    @NotNull
    private ListenableFuture<ImmutableList<SubscriptionResult>> persistBatchedSubscriptions(final @NotNull String clientId, final @NotNull SUBSCRIBE msg, final @NotNull Set<Topic> cleanedSubscriptions, final @NotNull ProtocolVersion mqttVersion, final @NotNull Mqtt5SubAckReasonCode[] answerCodes) {
        final SettableFuture<ImmutableList<SubscriptionResult>> settableFuture = SettableFuture.create();
        final ImmutableSet<Topic> topics = ImmutableSet.copyOf(cleanedSubscriptions);
        final ListenableFuture<ImmutableList<SubscriptionResult>> addSubscriptionFuture = clientSessionSubscriptionPersistence.addSubscriptions(clientId, topics);
        Futures.addCallback(addSubscriptionFuture, new SubscribePersistenceBatchedCallback(settableFuture, clientId, msg, mqttVersion, answerCodes),
                MoreExecutors.directExecutor());
        return settableFuture;
    }

    private void handleInsufficientPermissionsV31(final @NotNull ChannelHandlerContext ctx, final @NotNull Topic topic) {
        final ClientConnection clientConnection = ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
        log.debug("MQTT v3.1 Client '{}' (IP: {}) is not authorized to subscribe to topic '{}' with QoS '{}'. Disconnecting client.",
                clientConnection.getClientId(), clientConnection.getChannelIP().orElse("UNKNOWN"), topic.getTopic(), topic.getQoS().getQosNumber());
        mqttServerDisconnector.disconnect(
                ctx.channel(),
                null, //already logged
                "Not authorized to subscribe to topic '" + topic.getTopic() + "', QoS '" + topic.getQoS() + "'",
                Mqtt5DisconnectReasonCode.NOT_AUTHORIZED, //same as mqtt 5 just for the events
                null);
    }

    /**
     * Hook method to allow the subscription handler to be used in non MQTT contexts
     */
    protected @Nullable Object createSuback(final @NotNull String clientId,
                                            final @NotNull List<Topic> topics,
                                            final @NotNull Integer packetIdentifier,
                                            final @NotNull List<Mqtt5SubAckReasonCode> codes,
                                            final @Nullable String reasonString,
                                            final @NotNull ProtocolVersion protocolVersion) {

        //MQTT-SN
        if (protocolVersion == MQTTSNv1_2 || protocolVersion == MQTTSNv2_0) {
            return createMqttsnSuback(clientId, topics, packetIdentifier, codes, protocolVersion);
        }

        //MQTT
        return new SUBACK(packetIdentifier, codes, reasonString);
    }

    @Nullable
    private IMqttsnMessage createMqttsnSuback(final @NotNull String clientId,
                                              final @NotNull List<Topic> topics,
                                              final @NotNull Integer packetIdentifier,
                                              final @NotNull List<Mqtt5SubAckReasonCode> codes,
                                              final @NotNull ProtocolVersion protocolVersion) {
        try {
            int returnCode = MqttsnConstants.RETURN_CODE_ACCEPTED;

            final Optional<MqttsnTopicAlias> alias = mqttsnTopicRegistry.readTopicAlias(clientId, topics.get(0).getTopic());
            final int topicId = alias.get().getAlias();
            int grantedQos = 0;

            if (!codes.isEmpty()) {
                final Mqtt5SubAckReasonCode code = codes.get(0);
                if (!code.isError()) {
                    grantedQos = code.getCode();
                } else {
                    returnCode = MqttsnConstants.RETURN_CODE_INVALID_TOPIC_ID;
                }
            }

            final IMqttsnMessage msg;
            if (protocolVersion == MQTTSNv1_2) {
                msg = MqttsnCodecs.MQTTSN_CODEC_VERSION_1_2.createMessageFactory().createSuback(grantedQos, topicId, returnCode);
            } else {
                msg = MqttsnCodecs.MQTTSN_CODEC_VERSION_2_0.createMessageFactory().createSuback(grantedQos, topicId, returnCode);
            }
            msg.setId(packetIdentifier);
            return msg;
        } catch (final MqttsnProtocolException e) {
            log.error("error reading from the topic registry", e);
            return null;
        }
    }

    private static class SubscribePersistenceBatchedCallback implements FutureCallback<ImmutableList<SubscriptionResult>> {
        @NotNull
        private final SettableFuture<ImmutableList<SubscriptionResult>> settableFuture;
        @NotNull
        private final String clientId;
        @NotNull
        private final SUBSCRIBE msg;
        @NotNull
        private final ProtocolVersion mqttVersion;
        @NotNull
        private final Mqtt5SubAckReasonCode[] answerCodes;

        public SubscribePersistenceBatchedCallback(final @NotNull SettableFuture<ImmutableList<SubscriptionResult>> settableFuture, final @NotNull String clientId, final @NotNull SUBSCRIBE msg, final @NotNull ProtocolVersion mqttVersion, final @NotNull Mqtt5SubAckReasonCode[] answerCodes) {
            this.settableFuture = settableFuture;
            this.clientId = clientId;
            this.msg = msg;
            this.mqttVersion = mqttVersion;
            this.answerCodes = answerCodes;
        }

        @Override
        public void onSuccess(final @Nullable ImmutableList<SubscriptionResult> subscriptionResult) {
            settableFuture.set(subscriptionResult);
            log.trace("Adding subscriptions for client [{}] and topics [{}]", clientId, msg.getTopics());
        }

        @Override
        public void onFailure(final @NotNull Throwable throwable) {
            if (mqttVersion == ProtocolVersion.MQTTv3_1_1) {
                Exceptions.rethrowError("Unable to persist subscription to topics " + msg.getTopics() + " for client " + clientId + ".", throwable);
                for (int i = 0; i < answerCodes.length; i++) {
                    answerCodes[i] = UNSPECIFIED_ERROR;
                }
                settableFuture.set(null);
            } else {
                settableFuture.setException(throwable);
            }
        }
    }

    private void downgradeSharedSubscriptions(final @NotNull SUBSCRIBE subscribe) {
        for (final Topic topic : subscribe.getTopics()) {
            final SharedSubscription sharedSubscription = sharedSubscriptionService.checkForSharedSubscription(topic.getTopic());
            if (sharedSubscription == null) {
                continue;
            }
            if (topic.getQoS().getQosNumber() > 1) {
                // QoS 2 is not supported for shared subscriptions
                topic.setQoS(QoS.AT_LEAST_ONCE);
            }
        }
    }

    private static class SubscribePersistenceCallback implements FutureCallback<SubscriptionResult> {
        @NotNull
        private final SettableFuture<SubscriptionResult> settableFuture;
        @NotNull
        private final String clientId;
        @NotNull
        private final Topic topic;
        @NotNull
        private final ProtocolVersion mqttVersion;
        @NotNull
        private final Mqtt5SubAckReasonCode[] answerCodes;
        private final int index;

        public SubscribePersistenceCallback(final @NotNull SettableFuture<SubscriptionResult> settableFuture, final @NotNull String clientId, final @NotNull Topic topic, final @NotNull ProtocolVersion mqttVersion, final @NotNull Mqtt5SubAckReasonCode[] answerCodes, final int index) {
            this.settableFuture = settableFuture;
            this.clientId = clientId;
            this.topic = topic;
            this.mqttVersion = mqttVersion;
            this.answerCodes = answerCodes;
            this.index = index;
        }

        @Override
        public void onSuccess(final @Nullable SubscriptionResult subscriptionResult) {
            settableFuture.set(subscriptionResult);
            log.trace("Adding subscriptions for client [{}] and topic [{}] with qos [{}]", clientId, topic.getTopic(), topic.getQoS());
        }

        @Override
        public void onFailure(final @NotNull Throwable throwable) {
            if (mqttVersion == ProtocolVersion.MQTTv3_1_1) {
                Exceptions.rethrowError("Unable to persist subscription to topic " + topic + " for client " + clientId + ".", throwable);
                answerCodes[index] = UNSPECIFIED_ERROR;
                settableFuture.set(null);
            } else {
                settableFuture.setException(throwable);
            }
        }
    }


}
