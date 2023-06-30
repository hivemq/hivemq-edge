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

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.bootstrap.ClientState;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.handler.publish.PublishFlushHandler;
import io.netty.channel.Channel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an MQTTSN connection in the system
 * @author Simon L Johnson
 */
public class MqttsnClientConnection extends ClientConnection {

    private volatile @NotNull MqttsnClientState mqttsnClientState
            = MqttsnClientState.DISCONNECTED;

    private @Nullable Map<Integer, Integer> publishMsgIdToTopicAliasId;

    private @Nullable MqttsnProtocolVersion mqttsnProtocolVersion;

    private @Nullable IAwakeFlushCompleteCallback awakeFlushCompleteCallback;
    private final @NotNull Object flushCallbackMutex = new Object();


    public MqttsnClientConnection(@NotNull final Channel channel, @NotNull final PublishFlushHandler publishFlushHandler) {
        super(channel, publishFlushHandler);
        publishMsgIdToTopicAliasId = Collections.synchronizedMap(new HashMap<>());
    }

    public MqttsnClientState getMqttsnClientState() {
        return mqttsnClientState;
    }

    public void setMqttsnClientState(MqttsnClientState mqttsnClientState) {
        this.mqttsnClientState = mqttsnClientState;
    }

    public MqttsnProtocolVersion getMqttsnProtocolVersion() {
        return mqttsnProtocolVersion;
    }

    public void setMqttsnProtocolVersion(MqttsnProtocolVersion mqttsnProtocolVersion) {
        this.mqttsnProtocolVersion = mqttsnProtocolVersion;
    }

    public void proposeClientState(final @NotNull ClientState clientState) {
        super.proposeClientState(clientState);
        //only make the change if the super state changed
        if(getClientState() == ClientState.AUTHENTICATED){
            mqttsnClientState = MqttsnClientState.ACTIVE;
        }
    }

    public void proposeSleep(){
        proposeClientState(ClientState.DISCONNECTING);
        mqttsnClientState = MqttsnClientState.ASLEEP;
    }

    public void proposeAwake(){
        proposeClientState(ClientState.AUTHENTICATED);
        mqttsnClientState = MqttsnClientState.AWAKE;
    }

    public void proposeActive(){
        proposeClientState(ClientState.AUTHENTICATED);
        mqttsnClientState = MqttsnClientState.ACTIVE;
    }

    public void correlatePublishToTopicAlias(@NotNull final Integer msgId, @NotNull final Integer topicAliasId){
        publishMsgIdToTopicAliasId.put(msgId, topicAliasId);
    }

    public Integer getOriginatingTopicAliasForMessageId(@NotNull final Integer msgId){
        return publishMsgIdToTopicAliasId.get(msgId);
    }


    @Override
    public int decrementInFlightCount() {
        final int returnCount = super.decrementInFlightCount();
        try {
            return returnCount;
        } finally {
            if(returnCount == 0 && awakeFlushCompleteCallback != null){
                synchronized (flushCallbackMutex){
                    if(awakeFlushCompleteCallback != null){
                        synchronized (flushCallbackMutex){
                            //-- This happens ONCE only
                            getChannel().eventLoop().submit(() -> {
                                awakeFlushCompleteCallback.flushComplete();
                                awakeFlushCompleteCallback = null;
                            });
                        }
                    }
                }
            }
        }
    }

    public void setFlushCallback(final @NotNull IAwakeFlushCompleteCallback awakeFlushCompleteCallback) {
        this.awakeFlushCompleteCallback = awakeFlushCompleteCallback;
    }

    @Override
    public String toString() {
        return "MqttsnClientConnection{" +
                "clientId=" + super.getClientId() +
                ", state=" + mqttsnClientState +
                ", parentState=" + getClientState() +
                ", version=" + mqttsnProtocolVersion +
                '}';
    }
}
