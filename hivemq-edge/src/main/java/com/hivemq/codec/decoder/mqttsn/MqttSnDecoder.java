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
package com.hivemq.codec.decoder.mqttsn;

import com.hivemq.bootstrap.netty.ChannelDependencies;
import com.hivemq.codec.transcoder.ITranscoder;
import com.hivemq.codec.transcoder.TranscodingResult;
import com.hivemq.codec.transcoder.netty.NettyPipelineTranscodingContext;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.Message;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqttsn.MqttsnProtocolVersion;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.codec.MqttsnCodecException;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;

import java.util.List;
import java.util.Optional;

/**
 * ByteToMessage decoders cannot be shared instances, so we need to create a new instance per pipeline init
 */
public class MqttSnDecoder extends ByteToMessageDecoder {

    public static final int MESSAGE_TYPE_CONNECT = 0x04;

    final @NotNull ChannelDependencies channelDependencies;

    private static final Logger logger = LoggerFactory.getLogger(MqttSnDecoder.class);

    public MqttSnDecoder(final @NotNull ChannelDependencies channelDependencies){
        this.channelDependencies = channelDependencies;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf inBuf, List<Object> out) throws Exception {

        final int readableBytes = inBuf.readableBytes();
        if (readableBytes < 2) { //minimum packet size
            return;
        }

        try {

            NettyPipelineTranscodingContext transcodingContext =
                    new NettyPipelineTranscodingContext(ctx, channelDependencies);

            while (inBuf.readableBytes() >= 2) { // 2 is min packet size
                inBuf.markReaderIndex();
                final ByteBuf headerBuffer = inBuf.readSlice(2);
                final byte[] header = new byte[headerBuffer.readableBytes()];
                for (int i = 0; i < headerBuffer.readableBytes(); i++) {
                    header[i] = headerBuffer.getByte(i);
                }
                inBuf.resetReaderIndex();

                //packet size same for 1.2 as for 2.0
                final int packetSize = MqttsnWireUtils.readMessageLength(header);
                if (inBuf.readableBytes() < packetSize) {
                    inBuf.resetReaderIndex();
                    inBuf.clear();
                    throw new MqttsnCodecException("insufficient data available for parser");
                }

                final ByteBuf messageBuffer = inBuf.readSlice(packetSize);
                inBuf.markReaderIndex();
                final byte[] bytesMesg = new byte[messageBuffer.readableBytes()];
                for (int i = 0; i < messageBuffer.readableBytes(); i++) {
                    bytesMesg[i] = messageBuffer.getByte(i);
                }
                if (transcodingContext.getClientConnection().getMqttsnProtocolVersion() == null) {
                    detectProtocolVersion(transcodingContext, bytesMesg);
                }

                if(logger.isTraceEnabled()){
                    logger.trace("processing inbound {} bytes on connection {}", bytesMesg.length,
                            transcodingContext.getClientConnection());
                }

                ITranscoder<IMqttsnMessage, Message> transcoder = channelDependencies.getMqttsnToMqttTranscoder();
                //either transcode the message if the transcoder supports it, or hand it to the pipeline unchanged
                //and let the pipeline handle it
                IMqttsnMessage mqttsnMessage = transcodingContext.getCodec().decode(bytesMesg);
                boolean handled = false;
                if (transcoder.canHandle(transcodingContext, mqttsnMessage.getClass())) {
                    TranscodingResult<IMqttsnMessage, Message> result
                            = transcoder.transcode(transcodingContext, mqttsnMessage);
                    if ((handled = result.isComplete())) {
                        Optional<Message> optional = result.getOutput();
                        if(optional.isPresent()){
                            //Add the output of the transcoder to the pipeline
                            out.add(optional.get());
                        }

                        //check transcoder for errors
                        if(result.isError()){
                            logger.warn("Error encountered during transcoding - {}",
                                    result.getReasonString(), result.getError());
                        }
                    }
                }

                //the transcoder was unable to handle the message
                if (!handled) {
                    out.add(mqttsnMessage);
                }
            }
        } catch (MqttsnCodecException e) {
            String log = String.format("MqttSN Protocol Violation [%s]", e.getMessage());
            channelDependencies.getMqttServerDisconnector().logAndClose(ctx.channel(), log, log);
        }
    }

    /**
     * Use the CONNECT packet to determine the version of the protocol and set this on the ClientConnection
     */
    protected void detectProtocolVersion(@NotNull final NettyPipelineTranscodingContext context, @NotNull final byte[] header) {

        final int messageType = MqttsnWireUtils.readMessageType(header);
        int protocolVersion = MqttsnConstants.PROTOCOL_VERSION_UNKNOWN;
        if (messageType == MESSAGE_TYPE_CONNECT) {
            //get protocol version
            if (header[0] == 0x01) {
                protocolVersion = header[5];
            } else {
                protocolVersion = header[3];
            }
        }

        MqttsnProtocolVersion v = null;
        if (protocolVersion <= 1) {
            v = MqttsnProtocolVersion.MQTTSNv12;
            context.getClientConnection().setProtocolVersion(ProtocolVersion.MQTTSNv1_2);
        } else {
            v = MqttsnProtocolVersion.MQTTSNv20;
            context.getClientConnection().setProtocolVersion(ProtocolVersion.MQTTSNv2_0);
        }
        logger.trace("determine client connecting with version [{}], setting on session", v);
        context.getClientConnection().setMqttsnProtocolVersion(v);
    }

}
