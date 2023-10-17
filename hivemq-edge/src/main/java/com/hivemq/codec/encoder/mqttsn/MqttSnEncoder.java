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
package com.hivemq.codec.encoder.mqttsn;

import com.hivemq.bootstrap.netty.ChannelDependencies;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqttsn.MqttsnConnectionHelper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slj.mqtt.sn.spi.IMqttsnCodec;
import org.slj.mqtt.sn.spi.IMqttsnMessage;

public class MqttSnEncoder extends MessageToByteEncoder<IMqttsnMessage> {
    private final @NotNull ChannelDependencies channelDependencies;

    public MqttSnEncoder(final @NotNull ChannelDependencies channelDependencies) {
        this.channelDependencies = channelDependencies;
    }

    @Override
    protected void encode(final @NotNull ChannelHandlerContext ctx, final @NotNull IMqttsnMessage msg, final @NotNull ByteBuf out) throws Exception {
        IMqttsnCodec mqttsnCodec =
                MqttsnConnectionHelper.getCodecForConnection(MqttsnConnectionHelper.getConnection(ctx));
        out.writeBytes(mqttsnCodec.encode(msg));
    }
}
