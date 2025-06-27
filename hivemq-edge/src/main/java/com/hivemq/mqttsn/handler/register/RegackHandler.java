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
import org.jetbrains.annotations.NotNull;
import com.hivemq.mqttsn.IMqttsnTopicRegistry;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slj.mqtt.sn.wire.version1_2.payload.MqttsnRegack;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Handle the REGISTER flow from MQTT-SN Clients. For each inbound REGISTER call, check
 * the registry for existing and register new if required, if not return existing.
 *
 * @author Simon L Johnson
 */
@Singleton
@ChannelHandler.Sharable
public class RegackHandler extends SimpleChannelInboundHandler<MqttsnRegack> {

    private static final Logger log = LoggerFactory.getLogger(RegackHandler.class);

    private final @NotNull IMqttsnTopicRegistry mqttsnTopicRegistry;

    @Inject
    public RegackHandler(final @NotNull IMqttsnTopicRegistry mqttsnTopicRegistry) {
        this.mqttsnTopicRegistry = mqttsnTopicRegistry;
    }

    @Override
    protected void channelRead0(final @NotNull ChannelHandlerContext ctx, final @NotNull MqttsnRegack msg) throws Exception {
        final ClientConnection clientConnection = ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
        final String clientId = clientConnection.getClientId();
        log.info("Received REGACK from client {}  {} -> {}", clientId, msg.getReturnCode(), msg.getId());
    }
}
