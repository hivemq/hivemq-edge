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
package com.hivemq.mqtt.message.dropping;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5PubAckReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5PubRecReasonCode;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import static com.hivemq.bootstrap.ClientConnection.CHANNEL_ATTRIBUTE_NAME;

@Singleton
public class IncomingPublishDropper {

    private final @NotNull MqttServerDisconnector mqttDisconnector;
    private final @NotNull MessageDroppedService messageDroppedService;


    @Inject
    public IncomingPublishDropper(
            final @NotNull MqttServerDisconnector mqttDisconnector,
            final @NotNull MessageDroppedService messageDroppedService) {
        this.mqttDisconnector = mqttDisconnector;
        this.messageDroppedService = messageDroppedService;
    }


    public void dropMessage(
            final @NotNull AckReasonCode ackReasonCode,
            final @Nullable String reasonString,
            final @NotNull PUBLISH publish,
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull String clientId,
            final @NotNull String reason,
            final @NotNull String logMessage,
            final @NotNull String eventLogMessage) {
        final Channel channel = ctx.channel();

        final ProtocolVersion protocolVersion = channel.attr(CHANNEL_ATTRIBUTE_NAME).get().getProtocolVersion();
        //MQTT 3
        if (protocolVersion != ProtocolVersion.MQTTv5) {
            if (ackReasonCode != AckReasonCode.SUCCESS) {
                disconnectMqtt3Client(channel, logMessage, eventLogMessage);
            } else {
                dropWithoutDisconnectingMqtt3(publish, ctx);
            }
        } else {
            //MQTT 5
            dropPublishMqtt5(ackReasonCode, reasonString, publish, ctx);
        }

        messageDroppedService.withReason(clientId, publish.getTopic(), reason, publish.getQoS().getQosNumber());
    }

    private void dropWithoutDisconnectingMqtt3(
            final @NotNull PUBLISH publish, final @NotNull ChannelHandlerContext ctx) {
        switch (publish.getQoS()) {
            case AT_MOST_ONCE:
                //no ack for qos 0
                break;
            case AT_LEAST_ONCE:
                final PUBACK puback = new PUBACK(publish.getPacketIdentifier());
                ctx.writeAndFlush(puback);
                break;
            case EXACTLY_ONCE:
                final PUBREC pubrec = new PUBREC(publish.getPacketIdentifier());
                ctx.writeAndFlush(pubrec);
                break;
        }
    }

    private void dropPublishMqtt5(
            final @NotNull AckReasonCode ackReasonCode,
            final @Nullable String reasonString,
            final @NotNull PUBLISH publish,
            final @NotNull ChannelHandlerContext ctx) {

        switch (publish.getQoS()) {
            case AT_MOST_ONCE:
                //no ack for qos 0
                break;
            case AT_LEAST_ONCE:
                final Mqtt5PubAckReasonCode pubackReasonCode = Mqtt5PubAckReasonCode.from(ackReasonCode);
                final PUBACK puback = new PUBACK(publish.getPacketIdentifier(),
                        pubackReasonCode,
                        reasonString,
                        Mqtt5UserProperties.NO_USER_PROPERTIES);
                ctx.writeAndFlush(puback);
                break;
            case EXACTLY_ONCE:
                final Mqtt5PubRecReasonCode pubrecReasonCode = Mqtt5PubRecReasonCode.from(ackReasonCode);
                final PUBREC pubrec = new PUBREC(publish.getPacketIdentifier(),
                        pubrecReasonCode,
                        reasonString,
                        Mqtt5UserProperties.NO_USER_PROPERTIES);
                ctx.writeAndFlush(pubrec);
                break;
        }
    }

    private void disconnectMqtt3Client(
            final @NotNull Channel channel, final @NotNull String logMessage, final @NotNull String evetLogMessage) {
        mqttDisconnector.disconnect(channel,
                logMessage,
                evetLogMessage,
                Mqtt5DisconnectReasonCode.ADMINISTRATIVE_ACTION,
                null);
    }

}
