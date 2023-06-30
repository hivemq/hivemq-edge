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
import com.hivemq.codec.transcoder.ITranscoder;
import com.hivemq.codec.transcoder.TranscodingResult;
import com.hivemq.codec.transcoder.netty.NettyPipelineTranscodingContext;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.Message;
import com.hivemq.mqttsn.MqttsnProtocolException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slj.mqtt.sn.spi.IMqttsnMessage;

import java.util.List;
import java.util.Optional;

public class M2MMqttSnTranscodingEncoder extends MessageToMessageEncoder<Message> {

    private static final Logger logger = LoggerFactory.getLogger(M2MMqttSnTranscodingEncoder.class);

    private final @NotNull ChannelDependencies channelDependencies;

    public M2MMqttSnTranscodingEncoder(final @NotNull ChannelDependencies channelDependencies){
        this.channelDependencies = channelDependencies;
    }

    @Override
    protected void encode(final ChannelHandlerContext ctx, final Message msg, final List<Object> out) throws Exception {
        NettyPipelineTranscodingContext transcodingContext =
                new NettyPipelineTranscodingContext(ctx, channelDependencies);
        ITranscoder<Message, List<IMqttsnMessage>> transcoder = channelDependencies.getMqttToMqttsnTranscoder();
        if(transcoder.canHandle(transcodingContext, msg.getClass())){
            TranscodingResult<Message, List<IMqttsnMessage>> result = transcoder.transcode(transcodingContext, msg);
            if(result.isComplete()){
                Optional<List<IMqttsnMessage>> optional = result.getOutput();
                if(optional.isPresent()){
                    List<IMqttsnMessage> list = optional.get();
                    out.addAll(list);
                }
                //check transcoder for errors
                if(result.isError()){
                    logger.warn("error encountered during transcoding",
                            result.getError());
                }
            }
        } else {
            logger.error("unable to send MQTT traffic to SN device {}", msg);
            throw new MqttsnProtocolException("unable to send MQTT traffic to SN device");
        }
    }
}
