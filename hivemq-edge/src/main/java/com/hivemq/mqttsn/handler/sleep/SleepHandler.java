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
import com.hivemq.mqttsn.MqttsnClientConnection;
import com.hivemq.mqttsn.MqttsnConnectionHelper;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slj.mqtt.sn.wire.version1_2.payload.MqttsnDisconnect;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@ChannelHandler.Sharable
public class SleepHandler extends SimpleChannelInboundHandler<MqttsnDisconnect> {

    private static final Logger log = LoggerFactory.getLogger(SleepHandler.class);

    @Inject
    public SleepHandler() {
    }

    @Override
    protected void channelRead0(
            final @NotNull ChannelHandlerContext ctx, final @NotNull MqttsnDisconnect msg) throws Exception {

        final MqttsnClientConnection clientConnection = MqttsnConnectionHelper.getConnection(ctx);
        clientConnection.proposeSleep();
        log.trace("Device {} has requested to go to sleep for {}", clientConnection.getClientId(), msg.getDuration());
        clientConnection.setClientSessionExpiryInterval(Long.valueOf(msg.getDuration()));
        clientConnection.getChannel().writeAndFlush(
                MqttsnConnectionHelper.getMessageFactoryForConnection(clientConnection).createDisconnect());
    }
}
