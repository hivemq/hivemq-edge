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
package com.hivemq.mqttsn.handler.register;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqttsn.IMqttsnTopicRegistry;
import com.hivemq.mqttsn.MqttsnConnectionHelper;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.wire.version1_2.payload.MqttsnRegister;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Handle the REGISTER flow from MQTT-SN Clients. For each inbound REGISTER call, check
 * the registry for existing and register new if required, if not return existing.
 *
 * @author Simon L Johnson
 */
@Singleton
@ChannelHandler.Sharable
public class RegisterHandler extends SimpleChannelInboundHandler<MqttsnRegister> {

    private static final Logger log = LoggerFactory.getLogger(RegisterHandler.class);

    private final @NotNull IMqttsnTopicRegistry mqttsnTopicRegistry;

    @Inject
    public RegisterHandler(final @NotNull IMqttsnTopicRegistry mqttsnTopicRegistry) {
        this.mqttsnTopicRegistry = mqttsnTopicRegistry;
    }

    @Override
    protected void channelRead0(@NotNull final ChannelHandlerContext ctx, @NotNull final MqttsnRegister msg) throws Exception {

        final ClientConnection clientConnection = ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
        final String clientId = clientConnection.getClientId();
        String topicName = msg.getTopicName();
        int alias = mqttsnTopicRegistry.register(clientId, topicName);
        IMqttsnMessage out = MqttsnConnectionHelper.getMessageFactoryForConnection(clientConnection).
                createRegack(0x00, alias, MqttsnConstants.RETURN_CODE_ACCEPTED);
        out.setId(msg.getId());
        ctx.writeAndFlush(out);
    }
}
