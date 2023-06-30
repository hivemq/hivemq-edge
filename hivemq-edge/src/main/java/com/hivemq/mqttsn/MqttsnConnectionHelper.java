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
package com.hivemq.mqttsn;

import com.google.common.base.Preconditions;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.publish.PUBLISH;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import org.slj.mqtt.sn.codec.MqttsnCodecs;
import org.slj.mqtt.sn.spi.IMqttsnCodec;
import org.slj.mqtt.sn.spi.IMqttsnMessageFactory;

/**
 * Utilities to derive the correct message factories and codecs for the version
 * of the Connection resident on the system.
 *
 * @author Simon L Johnson
 */
public class MqttsnConnectionHelper {

    public static final AttributeKey<PUBLISH> PUBLISH_TOPIC_ID_ATTRIBUTE_NAME = AttributeKey.valueOf("Sn.PublishTopicMapContext");

    public static IMqttsnCodec getCodecForConnection(@NotNull final ClientConnection connection){
        IMqttsnCodec codec = MqttsnCodecs.MQTTSN_CODEC_VERSION_1_2;
        if(connection instanceof MqttsnClientConnection){
            MqttsnProtocolVersion version =
                    ((MqttsnClientConnection) connection).getMqttsnProtocolVersion();
            if(version == MqttsnProtocolVersion.MQTTSNv20){
                codec = MqttsnCodecs.MQTTSN_CODEC_VERSION_2_0;
            }
        }
        return codec;
    }

    public static IMqttsnMessageFactory getMessageFactoryForConnection(@NotNull final ClientConnection connection){
        IMqttsnCodec codec = getCodecForConnection(connection);
        return codec.createMessageFactory();
    }

    public static MqttsnClientConnection getConnection(@NotNull final ChannelHandlerContext ctx){
        final ClientConnection clientConnection =
                ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
        Preconditions.checkState(clientConnection instanceof MqttsnClientConnection, "Client Connection should be of SN type");
        return (MqttsnClientConnection) clientConnection;
    }
}
