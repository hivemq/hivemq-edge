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
package com.hivemq.bootstrap.netty.initializer;

import com.google.common.base.Preconditions;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.bootstrap.netty.ChannelDependencies;
import com.hivemq.bootstrap.netty.udp.UdpChannel;
import com.hivemq.codec.decoder.mqttsn.MqttSnDecoder;
import com.hivemq.codec.encoder.mqttsn.M2MMqttSnTranscodingEncoder;
import com.hivemq.codec.encoder.mqttsn.MqttSnEncoder;
import com.hivemq.configuration.service.entity.MqttsnUdpListener;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.connect.MessageBarrier;
import com.hivemq.mqtt.handler.publish.PublishFlushHandler;
import com.hivemq.mqttsn.MqttsnClientConnection;
import com.hivemq.mqttsn.handler.MqttsnChannelAdapter;
import com.hivemq.mqttsn.handler.register.RegackHandler;
import com.hivemq.mqttsn.handler.register.RegisterHandler;
import com.hivemq.mqttsn.handler.sleep.SleepHandler;
import com.hivemq.security.ssl.NonSslHandler;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Provider;

import static com.hivemq.bootstrap.netty.ChannelHandlerNames.CLIENT_LIFECYCLE_EVENT_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.CONNECTION_LIMITER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.EXCEPTION_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.INTERCEPTOR_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.MESSAGE_EXPIRY_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.MQTTSN_AWAKE_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.MQTTSN_MESSAGE_DECODER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.MQTTSN_MESSAGE_ENCODER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.MQTTSN_REGACK_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.MQTTSN_REGISTER_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.MQTTSN_SLEEP_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.MQTT_AUTH_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.MQTT_CONNECT_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.MQTT_DISCONNECT_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.MQTT_MESSAGE_BARRIER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.MQTT_MESSAGE_ENCODER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.MQTT_PINGREQ_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.MQTT_SUBSCRIBE_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.MQTT_UNSUBSCRIBE_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.NON_SSL_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.PLUGIN_INITIALIZER_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.PUBLISH_FLUSH_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.MQTTSN_CHANNEL_ADAPTER;


/**
 * @author Christoph Schäbel
 * @author Simon L Johnson
 */
public class UdpChannelInitializer extends AbstractChannelInitializer<UdpChannel> {

    private static final Logger logger = LoggerFactory.getLogger(UdpChannelInitializer.class);
    @NotNull
    private final Provider<NonSslHandler> nonSslHandlerProvider;

    public UdpChannelInitializer(@NotNull final ChannelDependencies channelDependencies,
                                 @NotNull final MqttsnUdpListener mqttsnUdpListener,
                                 @NotNull final Provider<NonSslHandler> nonSslHandlerProvider) {
        super(channelDependencies, mqttsnUdpListener);
        this.nonSslHandlerProvider = nonSslHandlerProvider;
    }

    @Override
    protected void initChannel(@NotNull UdpChannel ch) {

        Preconditions.checkNotNull(ch, "Channel must never be null");

        if (channelDependencies.getShutdownHooks().isShuttingDown()) {
            //during shutting down, we dont want new clients to create any pipeline,
            //and we dont want to read from their socket
            ch.config().setAutoRead(false);
            ch.close();
            return;
        }

        ClientConnection connection = createClientConnection(ch);

        //Add MQTT-SN Encoder/Decoders (cannot be shared versions for byte->message de/encoders)
        ch.pipeline().addLast(MQTTSN_MESSAGE_ENCODER, new MqttSnEncoder(channelDependencies));
        ch.pipeline().addLast(MQTTSN_MESSAGE_DECODER, new MqttSnDecoder(channelDependencies));
        ch.pipeline().addLast(MQTT_MESSAGE_ENCODER, new M2MMqttSnTranscodingEncoder(channelDependencies));

        ch.pipeline().addLast(MQTT_MESSAGE_BARRIER, new MessageBarrier(channelDependencies.getMqttServerDisconnector()));
        // before connack outbound interceptor as it initializes the client context after the connack
        ch.pipeline().addLast(PLUGIN_INITIALIZER_HANDLER, channelDependencies.getPluginInitializerHandler());

        ch.pipeline().addLast(INTERCEPTOR_HANDLER, channelDependencies.getInterceptorHandler());

//        //MQTT_PUBLISH_FLOW_HANDLER is added here after CONNECT
        ch.pipeline().addLast(MESSAGE_EXPIRY_HANDLER, channelDependencies.getPublishMessageExpiryHandler());
        ch.pipeline().addLast(MQTT_SUBSCRIBE_HANDLER, channelDependencies.getSubscribeHandler());

        ch.pipeline().addLast(MQTTSN_REGACK_HANDLER,
                new RegackHandler(channelDependencies.getMqttsnTopicRegistry()));

        ch.pipeline().addLast(MQTTSN_REGISTER_HANDLER,
                new RegisterHandler(channelDependencies.getMqttsnTopicRegistry()));

        ch.pipeline().addLast(PUBLISH_FLUSH_HANDLER, connection.getPublishFlushHandler());
        // after connect inbound interceptor as it intercepts the connect
        ch.pipeline().addLast(CLIENT_LIFECYCLE_EVENT_HANDLER,
                channelDependencies.getClientLifecycleEventHandler());

        ch.pipeline().addLast(MQTT_AUTH_HANDLER, channelDependencies.getAuthHandler());
        ch.pipeline().addLast(CONNECTION_LIMITER, channelDependencies.getConnectionLimiterHandler());
        ch.pipeline().addLast(MQTT_CONNECT_HANDLER, channelDependencies.getConnectHandler());

        ch.pipeline().addLast(MQTT_PINGREQ_HANDLER, channelDependencies.getPingRequestHandler());
        ch.pipeline().addLast(MQTT_UNSUBSCRIBE_HANDLER, channelDependencies.getUnsubscribeHandler());
        ch.pipeline().addLast(MQTT_DISCONNECT_HANDLER, channelDependencies.getDisconnectHandler());
        ch.pipeline().addLast(MQTTSN_SLEEP_HANDLER, new SleepHandler());
        ch.pipeline().addLast(MQTTSN_AWAKE_HANDLER, channelDependencies.getAwakeHandler());

        ch.pipeline().addLast(EXCEPTION_HANDLER, channelDependencies.getExceptionHandler());
        ch.pipeline().addLast(MQTTSN_CHANNEL_ADAPTER, new MqttsnChannelAdapter(channelDependencies));

    }

    @Override
    protected void addSpecialHandlers(@NotNull final Channel ch) {
        ch.pipeline().addFirst(NON_SSL_HANDLER, nonSslHandlerProvider.get());
    }

    protected ClientConnection createClientConnection(@NotNull Channel channel){
        logger.info("creating initial client connection in channel for {} -> oid {}", channel, System.identityHashCode(channel));
        final PublishFlushHandler publishFlushHandler = channelDependencies.createPublishFlushHandler();
        final ClientConnection clientConnection = new MqttsnClientConnection(channel, publishFlushHandler);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        return  clientConnection;
    }
}
