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
package com.hivemq.codec.transcoder;

import com.google.common.collect.ImmutableList;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.bootstrap.ClientState;
import com.hivemq.codec.transcoder.netty.NettyPipelineTranscodingContext;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.message.Message;
import com.hivemq.mqtt.message.PINGREQ;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5PubAckReasonCode;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;
import com.hivemq.mqttsn.MqttsnClientConnection;
import com.hivemq.mqttsn.MqttsnConnectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.MqttsnSpecificationValidator;
import org.slj.mqtt.sn.spi.IMqttsnCodec;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.wire.version1_2.payload.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

/**
 * Will convert MQTT-SN version 1.2 message to MQTT Version 5 messages.
 *
 * @author Simon L Johnson
 */
@Singleton
public class MqttsnToMqtt5Transcoder
        implements ITranscoder<IMqttsnMessage, Message> {

    private static final Logger log =
            LoggerFactory.getLogger(MqttsnToMqtt5Transcoder.class);

    @Inject
    public MqttsnToMqtt5Transcoder() {
    }

    @Override
    @Nullable
    public TranscodingResult<IMqttsnMessage, Message> transcode(@NotNull ITranscodingContext context, @NotNull IMqttsnMessage messageIn) {

        NettyPipelineTranscodingContext pipelineContext = (NettyPipelineTranscodingContext) context;
        MqttsnClientConnection connection = pipelineContext.getClientConnection();
        TranscodingResult<IMqttsnMessage, Message> result = new TranscodingResult<>(context, messageIn);
        IMqttsnCodec codec = MqttsnConnectionHelper.getCodecForConnection(connection);
        try {
            Message out = null;
            switch(messageIn.getMessageType()){
                case MqttsnConstants.CONNECT:
                    preProcessSNConnect(pipelineContext, result, messageIn);
                    if (!result.isComplete()) {
                        final CONNECT.Mqtt5Builder connectBuilder = new CONNECT.Mqtt5Builder();
                        connectBuilder.withClientIdentifier(codec.getClientId(messageIn))
                                .withCleanStart(codec.isCleanSession(messageIn))
                                .withReceiveMaximum(1)
                                .withKeepAlive((int) codec.getKeepAlive(messageIn));
                        out = connectBuilder.build();
                        processConnection(connection, (CONNECT) out);
                    }
                    break;
                case MqttsnConstants.DISCONNECT:
                    long duration = codec.getDuration(messageIn);
                    if(duration > 0){
                        //the device wants to go to sleep - let the custom SN DISCONNECT handler
                        //keep the ClientConnection (Channel) open
                        //but change the ClientState and block egress PUBLISH
                    } else {
                        final DISCONNECT disconnect = new DISCONNECT(
                                messageIn.isErrorMessage() ?
                                        Mqtt5DisconnectReasonCode.UNSPECIFIED_ERROR :
                                        Mqtt5DisconnectReasonCode.NORMAL_DISCONNECTION, null,
                                Mqtt5UserProperties.NO_USER_PROPERTIES,null,0);
                        out = disconnect;
                        //The TT semantics dont call for a response here
                        pipelineContext.getChannelHandlerContext().writeAndFlush(
                                MqttsnConnectionHelper.getMessageFactoryForConnection(connection).createDisconnect());
                    }
                    break;
                case MqttsnConstants.PUBLISH:
                case MqttsnConstants.PUBLISH_M1:
                    MqttsnPublish mqttsnPublish = (MqttsnPublish) messageIn;
                    QoS qos = QoS.valueOf(Math.min(2, Math.max(mqttsnPublish.getQoS(), 0)));
                    if(mqttsnPublish.getId() > 0){
                        connection.correlatePublishToTopicAlias(mqttsnPublish.getId(),
                                mqttsnPublish.readTopicDataAsInteger());
                    }
                    final PUBLISH publish = new PUBLISHFactory.Mqtt5Builder().
                            withPacketIdentifier(mqttsnPublish.getId()).
                            withHivemqId(pipelineContext.getHiveMQId().get()).
                            //-- ensure publish -1 becomes QoS 0
                            withQoS(qos).
                            withOnwardQos(qos).
                            withRetain(mqttsnPublish.isRetainedPublish()).
                            withDuplicateDelivery(mqttsnPublish.isDupRedelivery()).
                            withPacketIdentifier(mqttsnPublish.getId()).
                            withPayload(mqttsnPublish.getData()).
                            withTopic(pipelineContext.getTopicRegistry().readTopicName(connection.getClientId(),
                                    mqttsnPublish.getTopicType(), mqttsnPublish.getTopicData(), false)).
                            build();
                    out = publish;
                    break;
                case MqttsnConstants.PUBACK:
                    MqttsnPuback mqttsnPuback = (MqttsnPuback) messageIn;
                    final PUBACK puback = new PUBACK(
                            mqttsnPuback.getId(),
                            mqttsnPuback.isErrorMessage() ?
                                    Mqtt5PubAckReasonCode.UNSPECIFIED_ERROR : Mqtt5PubAckReasonCode.SUCCESS, null,
                            Mqtt5UserProperties.NO_USER_PROPERTIES);
                    out = puback;
                    break;
                case MqttsnConstants.PUBREC:
                    MqttsnPubrec mqttsnPubrec = (MqttsnPubrec) messageIn;
                    final PUBREC pubrec = new PUBREC(mqttsnPubrec.getId());
                    out = pubrec;
                    break;
                case MqttsnConstants.PUBREL:
                    MqttsnPubrel mqttsnPubrel = (MqttsnPubrel) messageIn;
                    final PUBREL pubrel = new PUBREL(mqttsnPubrel.getId());
                    out = pubrel;
                    break;
                case MqttsnConstants.PUBCOMP:
                    MqttsnPubcomp mqttsnPubcomp = (MqttsnPubcomp) messageIn;
                    final PUBCOMP pubcomp = new PUBCOMP(mqttsnPubcomp.getId());
                    out = pubcomp;
                    break;
                case MqttsnConstants.PINGREQ:
                    MqttsnPingreq mqttsnPingreq = (MqttsnPingreq) messageIn;
                    if(connection.getClientState() == ClientState.DISCONNECTING){
                        log.trace("detected ping-req from client in sleep state, move to WakingHandler {}", connection);
                    } else {
                        final PINGREQ pingreq = new PINGREQ();
                        out = pingreq;
                    }
                    break;
                case MqttsnConstants.SUBSCRIBE:
                    MqttsnSubscribe mqttsnSubscribe = (MqttsnSubscribe) messageIn;
                    String topicName = null;
                    //-- If its short its encoded in the message
                    if(mqttsnSubscribe.getTopicType() == MqttsnConstants.TOPIC_SHORT){
                        topicName = mqttsnSubscribe.getTopicName();
                    } else {
                        topicName = pipelineContext.getTopicRegistry().readTopicName(connection.getClientId(),
                                mqttsnSubscribe.getTopicType(), mqttsnSubscribe.getTopicData(), true);
                    }
                    final SUBSCRIBE subscribe = new SUBSCRIBE(mqttsnSubscribe.getId(),
                            new Topic(topicName, QoS.valueOf(mqttsnSubscribe.getQoS())));
                    out = subscribe;
                    break;
                case MqttsnConstants.UNSUBSCRIBE:
                    MqttsnUnsubscribe mqttsnUnsubscribe = (MqttsnUnsubscribe) messageIn;
                    String topicNameFromUnsubscribe = pipelineContext.getTopicRegistry().readTopicName(connection.getClientId(),
                            mqttsnUnsubscribe.getTopicType(), mqttsnUnsubscribe.getTopicData(), true);
                    final UNSUBSCRIBE unsubscribe = new UNSUBSCRIBE(
                            ImmutableList.of(topicNameFromUnsubscribe), mqttsnUnsubscribe.getId());
                    out = unsubscribe;
                    break;
                case MqttsnConstants.ENCAPSMSG:
                    //recurse point
                    MqttsnEncapsmsg mqttsnEncapsmsg = (MqttsnEncapsmsg) messageIn;
                    return transcode(context, codec.decode(mqttsnEncapsmsg.getEncapsulatedMsg()));
                case MqttsnConstants.HELO:
                    break;
            }

            if(out != null) {
                log.trace("sn -> ttv5 message conversion {} -> {}", messageIn, out);
                result.setOutput(Optional.of(out));
            }
        } catch(Exception e){
            result.setError(e);
        }
        return result;
    }

    @Override
    public boolean canHandle(ITranscodingContext context, Class<? extends IMqttsnMessage> messageType) {
        return TranscodingUtils.instanceOf(messageType,
                MqttsnConnect.class,
                MqttsnPublish.class,
                MqttsnPingreq.class,
                MqttsnPubcomp.class,
                MqttsnPubrec.class,
                MqttsnPubrel.class,
                MqttsnPuback.class,
                MqttsnDisconnect.class,
                MqttsnSubscribe.class,
                MqttsnUnsubscribe.class,
                MqttsnHelo.class);
    }

    protected void preProcessSNConnect(NettyPipelineTranscodingContext ctx, TranscodingResult<IMqttsnMessage, Message> result, IMqttsnMessage connect){
        IMqttsnCodec codec = MqttsnConnectionHelper.getCodecForConnection(ctx.getClientConnection());
        String clientId = codec.getClientId(connect);
        int maxLength = codec.getProtocolVersion() == 1 ?
                MqttsnConstants.MAX_CLIENT_ID_LENGTH_v12 : MqttsnConstants.MAX_CLIENT_ID_LENGTH_v2;
        maxLength = Math.min(maxLength, ctx.getConfigurationService().mqttsnConfiguration().getMaxClientIdentifierLength());

        if(!MqttsnSpecificationValidator.validClientId(clientId,
                ctx.getConfigurationService().mqttsnConfiguration().isAllowEmptyClientIdentifierEnabled(),
                maxLength)){
            log.warn("SN pre-processing determined error in client-identifier format '{}'", clientId);
            result.setResult(TranscodingResult.RESULT.failure);
            result.setReasonString("ClientID Format Invalid");
            ctx.getMqttConnacker().connackError(ctx.getChannelHandlerContext().channel(),
                    result.getReasonString(),
                    result.getReasonString(),
                    Mqtt5ConnAckReasonCode.CLIENT_IDENTIFIER_NOT_VALID,
                    result.getReasonString());
        }
    }

    protected void processConnection(ClientConnection connection, CONNECT connect){
        log.trace("processing connect information onto connection {}", connect);
        connection.setConnectMessage(connect);
        connection.setClientId(connect.getClientIdentifier());
        connection.setCleanStart(connect.isCleanStart());
        connection.setMaxPacketSizeSend(connect.getMaximumPacketSize());
        connection.setClientReceiveMaximum(connect.getReceiveMaximum());
    }
}
