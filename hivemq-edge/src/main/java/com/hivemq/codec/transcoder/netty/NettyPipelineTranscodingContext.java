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
package com.hivemq.codec.transcoder.netty;

import com.google.common.base.Preconditions;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.bootstrap.netty.ChannelDependencies;
import com.hivemq.codec.transcoder.ITranscodingContext;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqttsn.IMqttsnTopicRegistry;
import com.hivemq.mqttsn.MqttsnClientConnection;
import com.hivemq.mqttsn.MqttsnConnectionHelper;
import io.netty.channel.ChannelHandlerContext;
import org.slj.mqtt.sn.spi.IMqttsnCodec;
import org.slj.mqtt.sn.spi.IMqttsnMessageFactory;

/**
 * @author Simon L Johnson
 */
public class NettyPipelineTranscodingContext implements ITranscodingContext {

    private final @NotNull ChannelHandlerContext channelHandlerContext;
    private final @NotNull ChannelDependencies channelDependencies;

    public NettyPipelineTranscodingContext(@NotNull final ChannelHandlerContext channelHandlerContext,
                                           @NotNull final ChannelDependencies channelDependencies) {
        this.channelHandlerContext = channelHandlerContext;
        this.channelDependencies = channelDependencies;
    }

    public ChannelHandlerContext getChannelHandlerContext() {
        return channelHandlerContext;
    }

    public MqttsnClientConnection getClientConnection() {
        final ClientConnection clientConnection = channelHandlerContext.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
        Preconditions.checkNotNull(clientConnection);
        return (MqttsnClientConnection) clientConnection;
    }

    public HivemqId getHiveMQId() {
        return channelDependencies.getHiveMqId();
    }

    public IMqttsnTopicRegistry getTopicRegistry() {
        return channelDependencies.getMqttsnTopicRegistry();
    }

    public ConfigurationService getConfigurationService() {
        return channelDependencies.getConfigurationService();
    }

    public MqttConnacker getMqttConnacker() {
        return channelDependencies.getMqttConnacker();
    }

    public IMqttsnCodec getCodec(){
        return MqttsnConnectionHelper.getCodecForConnection(getClientConnection());
    }

    public IMqttsnMessageFactory getMessageFactory(){
        return MqttsnConnectionHelper.getMessageFactoryForConnection(getClientConnection());
    }
}
