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

import com.hivemq.codec.transcoder.netty.NettyPipelineTranscodingContext;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.message.Message;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.PINGRESP;
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.mqtt.message.suback.SUBACK;
import com.hivemq.mqtt.message.unsuback.UNSUBACK;
import com.hivemq.mqttsn.MqttsnClientConnection;
import com.hivemq.mqttsn.MqttsnConnectionHelper;
import com.hivemq.mqttsn.MqttsnProtocolException;
import com.hivemq.mqttsn.MqttsnTopicAlias;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.spi.IMqttsnMessageFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Will convert messages from MQTTv5 messages to MQTT-SN messages created by the factory supplied.
 *
 * @author Simon L Johnson
 */
@Singleton
public class Mqtt5ToMqttsnTranscoder
        implements ITranscoder<Message, List<IMqttsnMessage>> {

    private static final Logger log =
            LoggerFactory.getLogger(Mqtt5ToMqttsnTranscoder.class);

    @Inject
    public Mqtt5ToMqttsnTranscoder() {

    }

    @Override
    @Nullable
    public TranscodingResult<Message, List<IMqttsnMessage>> transcode(@NotNull ITranscodingContext context, @NotNull Message messageIn) {

        NettyPipelineTranscodingContext pipelineContext = (NettyPipelineTranscodingContext) context;
        MqttsnClientConnection connection = pipelineContext.getClientConnection();
        IMqttsnMessageFactory factory = MqttsnConnectionHelper.getMessageFactoryForConnection(connection);
        TranscodingResult<Message, List<IMqttsnMessage>> result = new TranscodingResult<>(context, messageIn);
        try {
            List list = new ArrayList();
            MessageType type = messageIn.getType();
            switch (type){
                case CONNACK:
                    CONNACK connack = (CONNACK) messageIn;
                    IMqttsnMessage out = factory.createConnack(connack.getReasonCode().isError() ? MqttsnConstants.RETURN_CODE_SERVER_UNAVAILABLE :
                                    MqttsnConstants.RETURN_CODE_ACCEPTED,
                            connack.isSessionPresent(), null, connack.getSessionExpiryInterval());
                    list.add(out);
                    break;
                case UNSUBACK:
                    UNSUBACK unsuback = (UNSUBACK) messageIn;
                    out = factory.createUnsuback(MqttsnConstants.RETURN_CODE_ACCEPTED);
                    out.setId(unsuback.getPacketIdentifier());
                    list.add(out);
                    break;
                case PUBLISH:
                    PUBLISH publish = (PUBLISH) messageIn;
                    Optional<MqttsnTopicAlias> aliasOptional =
                            pipelineContext.getTopicRegistry().readTopicAlias(connection.getClientId(), publish.getTopic());
                    MqttsnTopicAlias topicAlias = null;

                    if(!aliasOptional.isPresent()){
                        log.info("alias not found for outbound PUBLISH, adding registration procedure");
                        int topicAliasVal = pipelineContext.getTopicRegistry().register(connection.getClientId(), publish.getTopic());
                        out = factory.createRegister(topicAliasVal, publish.getTopic());
                        list.add(out);
                    } else {
                        topicAlias = aliasOptional.get();
                    }

                    //-- we will always have a means to deliver by this point
                    if(topicAlias == null){
                        topicAlias =
                                pipelineContext.getTopicRegistry().readTopicAlias(connection.getClientId(), publish.getTopic()).get();
                    }

                    out = factory.createPublish(
                            publish.getQoS().getQosNumber(),
                            publish.isDuplicateDelivery(),
                            publish.isRetain(),
                            mapTopicType(topicAlias.getType()),
                            topicAlias.getAlias(),
                            publish.getPayload());
                    if(publish.getQoS().getQosNumber() > 0){
                        out.setId(publish.getPacketIdentifier());
                    }
                    list.add(out);
                    break;
                case PUBACK:
                    PUBACK puback = (PUBACK) messageIn;
                    Integer topicId = connection.getOriginatingTopicAliasForMessageId(puback.getPacketIdentifier());
                    out = factory.createPuback(topicId == null ? 0 : topicId, puback.getReasonCode().isError() ?
                            MqttsnConstants.RETURN_CODE_INVALID_TOPIC_ID  : MqttsnConstants.RETURN_CODE_ACCEPTED);
                    out.setId(puback.getPacketIdentifier());
                    list.add(out);
                    break;
                case PUBREC:
                    PUBREC pubrec = (PUBREC) messageIn;
                    out = factory.createPubrec();
                    out.setId(pubrec.getPacketIdentifier());
                    list.add(out);
                    break;
                case PUBREL:
                    PUBREL pubrel = (PUBREL) messageIn;
                    out = factory.createPubrel();
                    out.setId(pubrel.getPacketIdentifier());
                    list.add(out);
                    break;
                case PUBCOMP:
                    PUBCOMP pubcomp = (PUBCOMP) messageIn;
                    out = factory.createPubcomp();
                    out.setId(pubcomp.getPacketIdentifier());
                    list.add(out);
                    break;
                case PINGRESP:
                    PINGRESP pingresp = (PINGRESP) messageIn;
                    out = factory.createPingresp();
                    list.add(out);
                    break;
                case DISCONNECT:
                    DISCONNECT disconnect = (DISCONNECT) messageIn;
                    out = factory.createDisconnect();
                    list.add(out);
                    break;

            }

            if(!list.isEmpty()){
                log.trace("ttv5 -> sn message conversion {} -> {} sn messages", messageIn, list.size());
                result.setOutput(Optional.of(list));
            }

        } catch(MqttsnProtocolException e){
            log.error("error transcoding message", e);
            result.setError(e);
        }

        return result;
    }

    @Override
    public boolean canHandle(ITranscodingContext context, Class<? extends Message> messageType) {
        return TranscodingUtils.instanceOf(messageType,
                CONNACK.class,
                DISCONNECT.class,
                PUBLISH.class,
                PUBACK.class,
                PUBREC.class,
                PUBREL.class,
                PUBCOMP.class,
                UNSUBACK.class,
                SUBACK.class,
                PINGRESP.class);
    }

    private static MqttsnConstants.TOPIC_TYPE mapTopicType(MqttsnTopicAlias.TYPE type){
        switch(type){
            case FULL: return MqttsnConstants.TOPIC_TYPE.FULL;
            case SHORT: return MqttsnConstants.TOPIC_TYPE.SHORT;
            case NORMAL: return MqttsnConstants.TOPIC_TYPE.NORMAL;
            case PREDEFINED: return MqttsnConstants.TOPIC_TYPE.PREDEFINED;
        }
        throw new IllegalArgumentException("unknown alias type");
    }

}
