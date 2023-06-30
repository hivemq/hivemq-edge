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
package com.hivemq.mqttsn.handler;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.bootstrap.netty.ChannelDependencies;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;

/**
 * MQTT-SN specific adapter to handle some non standard lifecycle things.
 * @author Simon L Johnson
 */
public class MqttsnChannelAdapter extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(MqttsnChannelAdapter.class);

    private @NotNull ChannelDependencies channelDependencies;

    public MqttsnChannelAdapter(final @NotNull ChannelDependencies channelDependencies) {
        this.channelDependencies = channelDependencies;
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        final ClientConnection clientConnection = ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
        if(clientConnection != null){
            if(log.isTraceEnabled()){
                log.trace("Clearing session alias registry with clientId {}", clientConnection.getClientId());
            }
            channelDependencies.getMqttsnTopicRegistry().clearSessionAliases(clientConnection.getClientId());
        }
    }
}
