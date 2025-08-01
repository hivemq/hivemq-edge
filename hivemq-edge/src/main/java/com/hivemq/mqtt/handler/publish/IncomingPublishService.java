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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import com.hivemq.datagov.DataGovernanceContext;
import com.hivemq.datagov.DataGovernanceService;
import com.hivemq.datagov.impl.DataGovernanceContextImpl;
import com.hivemq.datagov.model.DataGovernanceData;
import com.hivemq.datagov.model.impl.DataGovernanceDataImpl;
import com.hivemq.extension.sdk.api.packets.auth.DefaultAuthorizationBehaviour;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.extensions.handler.tasks.PublishAuthorizerResult;
import com.hivemq.extensions.packets.general.ModifiableDefaultPermissionsImpl;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5PubAckReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5PubRecReasonCode;
import com.hivemq.util.ReasonStrings;
import io.netty.channel.ChannelHandlerContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * This Service is responsible for PUBLISH message processing after interception and authorisation.
 *
 * @author Dominik Obermaier
 * @author Christoph Schäbel
 * @author Florian Limpöck
 */
@Singleton
public class IncomingPublishService {

    private final @NotNull MqttConfigurationService mqttConfigurationService;
    private final @NotNull RestrictionsConfigurationService restrictionsConfigurationService;
    private final @NotNull MqttServerDisconnector mqttServerDisconnector;

    private final @NotNull DataGovernanceService dataGovernanceService;

    @Inject
    IncomingPublishService(
            final @NotNull MqttConfigurationService mqttConfigurationService,
            final @NotNull RestrictionsConfigurationService restrictionsConfigurationService,
            final @NotNull MqttServerDisconnector mqttServerDisconnector,
            final @NotNull DataGovernanceService dataGovernanceService) {

        this.mqttConfigurationService = mqttConfigurationService;
        this.restrictionsConfigurationService = restrictionsConfigurationService;
        this.mqttServerDisconnector = mqttServerDisconnector;
        this.dataGovernanceService = dataGovernanceService;
    }

    public void processPublish(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull PUBLISH publish,
            final @Nullable PublishAuthorizerResult authorizerResult) {

        final ClientConnection clientConnection = ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
        final ProtocolVersion protocolVersion = clientConnection.getProtocolVersion();

        final int maxQos = mqttConfigurationService.maximumQos().getQosNumber();
        final int qos = publish.getQoS().getQosNumber();
        if (qos > maxQos) {
            final String clientId = clientConnection.getClientId();
            mqttServerDisconnector.disconnect(ctx.channel(),
                    "Client '" +
                            clientId +
                            "' (IP: {}) sent a PUBLISH with QoS exceeding the maximum configured QoS." +
                            " Got QoS " +
                            publish.getQoS() +
                            ", maximum: " +
                            mqttConfigurationService.maximumQos() +
                            ". Disconnecting client.",
                    "Sent PUBLISH with QoS (" + qos + ") higher than the allowed maximum (" + maxQos + ")",
                    Mqtt5DisconnectReasonCode.QOS_NOT_SUPPORTED,
                    String.format(ReasonStrings.CONNACK_QOS_NOT_SUPPORTED_PUBLISH, qos, maxQos));
            return;
        }

        final String topic = publish.getTopic();
        final int maxTopicLength = restrictionsConfigurationService.maxTopicLength();
        if (topic.length() > maxTopicLength) {
            final String clientId = clientConnection.getClientId();
            final String logMessage = "Client '" +
                    clientId +
                    "' (IP: {}) sent a PUBLISH with a topic that exceeds the maximum configured length of '" +
                    maxTopicLength +
                    "' . Disconnecting client.";
            mqttServerDisconnector.disconnect(ctx.channel(),
                    logMessage,
                    "Sent PUBLISH for a topic that exceeds maximum topic length",
                    Mqtt5DisconnectReasonCode.TOPIC_NAME_INVALID,
                    ReasonStrings.DISCONNECT_MAXIMUM_TOPIC_LENGTH_EXCEEDED);
            return;
        }

        if (ProtocolVersion.MQTTv3_1 == protocolVersion ||
                ProtocolVersion.MQTTv3_1_1 == protocolVersion) { //Version 2.0 ChangePoint: will need this for MQTT-SN
            final Long maxPublishSize = clientConnection.getMaxPacketSizeSend();
            if (!isMessageSizeAllowed(maxPublishSize, publish)) {
                final String clientId = clientConnection.getClientId();
                final String logMessage = "Client '" +
                        clientId +
                        "' (IP: {}) sent a PUBLISH with " +
                        publish.getPayload().length +
                        " bytes payload its max allowed size is " +
                        maxPublishSize +
                        " bytes. Disconnecting client.";
                final String reason = "Sent PUBLISH with a payload that is bigger than the allowed message size";
                mqttServerDisconnector.disconnect(ctx.channel(),
                        logMessage,
                        reason,
                        Mqtt5DisconnectReasonCode.PACKET_TOO_LARGE,
                        reason);
                return;
            }
        }

        authorizePublish(ctx, publish, authorizerResult);
    }

    private void authorizePublish(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull PUBLISH publish,
            final @Nullable PublishAuthorizerResult authorizerResult) {

        final ClientConnection clientConnection = ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();

        if (authorizerResult != null && authorizerResult.getAckReasonCode() != null) {
            //decision has been made in PublishAuthorizer
            if (clientConnection.isIncomingPublishesDefaultFailedSkipRest()) {
                //reason string and reason code null, because client disconnected previously
                finishUnauthorizedPublish(ctx, publish, null, null);
            } else if (authorizerResult.getAckReasonCode() == AckReasonCode.SUCCESS) {
                publishMessage(ctx, publish);
            } else {
                finishUnauthorizedPublish(ctx,
                        publish,
                        authorizerResult.getAckReasonCode(),
                        authorizerResult.getReasonString());
            }
            return;
        }

        final ModifiableDefaultPermissions permissions = clientConnection.getAuthPermissions();
        final ModifiableDefaultPermissionsImpl defaultPermissions = (ModifiableDefaultPermissionsImpl) permissions;

        //if authorizers are present and no permissions are available and the default behaviour has not been changed
        //then we deny the publish
        if (authorizerResult != null &&
                authorizerResult.isAuthorizerPresent() &&
                (defaultPermissions == null ||
                        (defaultPermissions.asList().isEmpty() &&
                                defaultPermissions.getDefaultBehaviour() == DefaultAuthorizationBehaviour.ALLOW &&
                                !defaultPermissions.isDefaultAuthorizationBehaviourOverridden()))) {
            finishUnauthorizedPublish(ctx, publish, null, null);
            return;
        }

        if (DefaultPermissionsEvaluator.checkPublish(permissions, publish)) {
            publishMessage(ctx, publish);
        } else {
            finishUnauthorizedPublish(ctx, publish, null, null);
        }
    }

    private void finishUnauthorizedPublish(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull PUBLISH publish,
            final @Nullable AckReasonCode reasonCode,
            final @Nullable String reasonString) {

        final ClientConnection clientConnection = ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();

        clientConnection.setIncomingPublishesDefaultFailedSkipRest(true);

        if (!ctx.channel().isActive()) {
            //no more processing needed.
            return;
        }

        final String reason = "Not authorized to publish on topic '" +
                publish.getTopic() +
                "' with QoS '" +
                publish.getQoS().getQosNumber() +
                "' and retain '" +
                publish.isRetain() +
                "'";

        //MQTT 3.x.x -> disconnect (without publish answer packet)
        if (clientConnection.getProtocolVersion() != ProtocolVersion.MQTTv5) {

            final String clientId = clientConnection.getClientId();
            mqttServerDisconnector.disconnect(ctx.channel(),
                    "Client '" +
                            clientId +
                            "' (IP: {}) is not authorized to publish on topic '" +
                            publish.getTopic() +
                            "' with QoS '" +
                            publish.getQoS().getQosNumber() +
                            "' and retain '" +
                            publish.isRetain() +
                            "'. Disconnecting client.",
                    reason,
                    Mqtt5DisconnectReasonCode.NOT_AUTHORIZED,
                    reason);
            return;
        }

        //MQTT 5 -> send ACK with error code and then disconnect
        switch (publish.getQoS()) {
            case AT_MOST_ONCE:
                //just drop the message, no back channel to the client
                break;
            case AT_LEAST_ONCE:
                final PUBACK puback = new PUBACK(publish.getPacketIdentifier(),
                        reasonCode != null ?
                                Mqtt5PubAckReasonCode.from(reasonCode) :
                                Mqtt5PubAckReasonCode.NOT_AUTHORIZED,
                        reasonString != null ? reasonString : reason,
                        Mqtt5UserProperties.NO_USER_PROPERTIES);
                ctx.pipeline().writeAndFlush(puback);
                break;
            case EXACTLY_ONCE:
                final PUBREC pubrec = new PUBREC(publish.getPacketIdentifier(),
                        reasonCode != null ?
                                Mqtt5PubRecReasonCode.from(reasonCode) :
                                Mqtt5PubRecReasonCode.NOT_AUTHORIZED,
                        reasonString != null ? reasonString : reason,
                        Mqtt5UserProperties.NO_USER_PROPERTIES);
                ctx.pipeline().writeAndFlush(pubrec);
                break;
        }

        final String clientId = clientConnection.getClientId();
        mqttServerDisconnector.disconnect(ctx.channel(),
                "Client '" +
                        clientId +
                        "' (IP: {}) is not authorized to publish on topic '" +
                        publish.getTopic() +
                        "' with QoS '" +
                        publish.getQoS().getQosNumber() +
                        "' and retain '" +
                        publish.isRetain() +
                        "'. Disconnecting client.",
                reason,
                Mqtt5DisconnectReasonCode.NOT_AUTHORIZED,
                reason);
    }

    private void publishMessage(final ChannelHandlerContext ctx, final @NotNull PUBLISH publish) {

        final ClientConnection clientConnection = ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
        final String clientId = clientConnection.getClientId();

        final DataGovernanceData data =
                new DataGovernanceDataImpl.Builder().withPublish(publish).withClientId(clientId).build();

        final DataGovernanceContext governanceContext = new DataGovernanceContextImpl(data);
        governanceContext.setExecutorService(ctx.channel().eventLoop());

        final ListenableFuture<PublishingResult> publishFinishedFuture =
                dataGovernanceService.applyAndPublish(governanceContext);
//        final ListenableFuture<PublishReturnCode> publishFinishedFuture = publishService.publish(publish, ctx.channel().eventLoop(), clientId);
        Futures.addCallback(publishFinishedFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(final @Nullable PublishingResult result) {
                sendAck(ctx, publish, result);
            }

            @Override
            public void onFailure(final @NotNull Throwable t) {
                // TODO think if we wanna set a reason string here.
                sendAck(ctx, publish, PublishingResult.failed(null, AckReasonCode.UNSPECIFIED_ERROR));
            }
        }, ctx.channel().eventLoop());
    }

    private void sendAck(
            final @NotNull ChannelHandlerContext ctx,
            final PUBLISH publish,
            final @Nullable PublishingResult publishingResult) {


        switch (publish.getQoS()) {
            case AT_MOST_ONCE:
                // do nothing
                break;
            case AT_LEAST_ONCE:
                if (publishingResult == null) {
                    ctx.pipeline().writeAndFlush(new PUBACK(publish.getPacketIdentifier()));
                    break;
                }
                switch (publishingResult.getPublishReturnCode()) {
                    case DELIVERED -> {
                        ctx.pipeline().writeAndFlush(new PUBACK(publish.getPacketIdentifier()));
                    }
                    case NO_MATCHING_SUBSCRIBERS -> {
                        ctx.pipeline()
                                .writeAndFlush(new PUBACK(publish.getPacketIdentifier(),
                                        Mqtt5PubAckReasonCode.NO_MATCHING_SUBSCRIBERS,
                                        null,
                                        Mqtt5UserProperties.NO_USER_PROPERTIES));
                    }
                    case FAILED -> {
                        ctx.pipeline()
                                .writeAndFlush(new PUBACK(publish.getPacketIdentifier(),
                                        resolveMqtt5PubAckReasonCode(publishingResult),
                                        publishingResult.getReasonString(),
                                        Mqtt5UserProperties.NO_USER_PROPERTIES));
                    }
                }
                break;
            case EXACTLY_ONCE:
                if (publishingResult == null) {
                    ctx.pipeline().writeAndFlush(new PUBREC(publish.getPacketIdentifier()));
                    break;
                }
                switch (publishingResult.getPublishReturnCode()) {
                    case DELIVERED -> {
                        ctx.pipeline().writeAndFlush(new PUBREC(publish.getPacketIdentifier()));
                    }
                    case NO_MATCHING_SUBSCRIBERS -> {
                        ctx.pipeline()
                                .writeAndFlush(new PUBREC(publish.getPacketIdentifier(),
                                        Mqtt5PubRecReasonCode.NO_MATCHING_SUBSCRIBERS,
                                        null,
                                        Mqtt5UserProperties.NO_USER_PROPERTIES));
                    }
                    case FAILED -> {
                        ctx.pipeline()
                                .writeAndFlush(new PUBREC(publish.getPacketIdentifier(),
                                        resolveMqtt5PubRecReasonCode(publishingResult),
                                        publishingResult.getReasonString(),
                                        Mqtt5UserProperties.NO_USER_PROPERTIES));
                    }
                }
                break;
        }
    }

    private @NotNull Mqtt5PubRecReasonCode resolveMqtt5PubRecReasonCode(final @NotNull PublishingResult publishingResult) {
        return Mqtt5PubRecReasonCode.from(publishingResult.getAckReasonCode());
    }

    private @NotNull Mqtt5PubAckReasonCode resolveMqtt5PubAckReasonCode(final @NotNull PublishingResult publishingResult) {
        return Mqtt5PubAckReasonCode.from(publishingResult.getAckReasonCode());
    }

    private boolean isMessageSizeAllowed(final @Nullable Long maxPublishSize, final @NotNull PUBLISH publish) {
        return maxPublishSize == null || publish.getPayload() == null || maxPublishSize >= publish.getPayload().length;
    }
}
