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
package com.hivemq.mqttsn.handler.sleep;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.mqtt.services.PublishPollService;
import com.hivemq.mqttsn.MqttsnClientConnection;
import com.hivemq.mqttsn.MqttsnConnectionHelper;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slj.mqtt.sn.wire.version1_2.payload.MqttsnPingreq;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@ChannelHandler.Sharable
public class AwakeHandler extends SimpleChannelInboundHandler<MqttsnPingreq> {

    private static final Logger log = LoggerFactory.getLogger(AwakeHandler.class);

    private final @NotNull PublishPollService pollService;
    private final @NotNull MqttServerDisconnector mqttServerDisconnector;

    @Inject
    public AwakeHandler(final @NotNull MqttServerDisconnector mqttServerDisconnector,
                        final @NotNull PublishPollService pollService) {
        this.mqttServerDisconnector = mqttServerDisconnector;
        this.pollService = pollService;
    }

    @Override
    protected void channelRead0(
            final @NotNull ChannelHandlerContext ctx, final @NotNull MqttsnPingreq msg) throws Exception {

        //-- Marker on the Connection when its asleep
        final MqttsnClientConnection clientConnection = MqttsnConnectionHelper.getConnection(ctx);
        final String clientId = clientConnection.getClientId();
        if(clientId.equals(msg.getClientId())){
            clientConnection.setFlushCallback(() -> {
                log.info("Awake flush is complete, sending PING-RESP {}", clientConnection);
                clientConnection.proposeSleep();
                clientConnection.getChannel().writeAndFlush(
                        MqttsnConnectionHelper.getMessageFactoryForConnection(clientConnection).createPingresp());
            });
            clientConnection.proposeAwake();
            clientConnection.getChannel().eventLoop().submit(() ->
                    pollService.pollNewMessages(clientConnection.getClientId(), clientConnection.getChannel()));
            log.info("Waking ping-req clientId matches session clientId on the same connection, allow to wake {}", clientConnection);
        } else {
            log.warn("Waking ping-req clientId MISmatch detected, close connection {}", clientConnection);
            mqttServerDisconnector.disconnect(ctx.channel(),
                    "Client '" + clientId + "' (IP: {}) Sent Ping-Req with ClientID that Mismatched Session ClientId. Disconnecting client.",
                    "Client '" + clientId + "' (IP: {}) Sent Ping-Req with ClientID that Mismatched Session ClientId. Disconnecting client.",
                    Mqtt5DisconnectReasonCode.UNSPECIFIED_ERROR, null
            );
        }
    }
}
