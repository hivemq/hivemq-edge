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
import com.hivemq.bootstrap.factories.HandlerPackage;
import com.hivemq.bootstrap.netty.ChannelDependencies;
import com.hivemq.codec.decoder.MQTTMessageDecoder;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import com.hivemq.configuration.service.entity.Listener;
import org.jetbrains.annotations.NotNull;
import com.hivemq.mqtt.handler.connect.MessageBarrier;
import com.hivemq.mqtt.handler.publish.PublishFlushHandler;
import com.hivemq.security.exception.SslException;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static com.hivemq.bootstrap.netty.ChannelHandlerNames.ALL_CHANNELS_GROUP_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.CLIENT_LIFECYCLE_EVENT_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.CONNECTION_LIMITER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.EXCEPTION_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.GLOBAL_THROTTLING_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.INTERCEPTOR_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.MESSAGE_EXPIRY_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.MQTT_AUTH_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.MQTT_CONNECT_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.MQTT_DISCONNECT_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.MQTT_MESSAGE_BARRIER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.MQTT_MESSAGE_DECODER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.MQTT_MESSAGE_ENCODER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.MQTT_PINGREQ_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.MQTT_SUBSCRIBE_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.MQTT_UNSUBSCRIBE_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.NEW_CONNECTION_IDLE_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.NO_CONNECT_IDLE_EVENT_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.PLUGIN_INITIALIZER_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.PUBLISH_FLUSH_HANDLER;
import static com.hivemq.logging.LoggingUtils.appendListenerToMessage;

public abstract class AbstractChannelInitializer<T extends Channel> extends ChannelInitializer<T> {

    private static final Logger log = LoggerFactory.getLogger(AbstractChannelInitializer.class);

    private final @NotNull Listener listener;
    private final boolean throttlingEnabled;
    private final boolean legacyNettyShutdown;
    protected final @NotNull ChannelDependencies channelDependencies;

    public AbstractChannelInitializer(
            final @NotNull ChannelDependencies channelDependencies, final @NotNull Listener listener) {
        this.channelDependencies = channelDependencies;
        this.listener = listener;
        final boolean incomingEnabled = channelDependencies.getRestrictionsConfigurationService().incomingLimit() > 0;
        final boolean outgoingEnabled = InternalConfigurations.OUTGOING_BANDWIDTH_THROTTLING_DEFAULT_BYTES_PER_SEC > 0;
        legacyNettyShutdown = InternalConfigurations.NETTY_SHUTDOWN_LEGACY;
        throttlingEnabled = incomingEnabled || outgoingEnabled;
    }

    @Override
    protected void initChannel(final @NotNull T ch) throws Exception {

        Preconditions.checkNotNull(ch, "Channel must never be null");

        if (!legacyNettyShutdown && channelDependencies.getShutdownHooks().isShuttingDown()) {
            //during shutting down, we dont want new clients to create any pipeline,
            //and we dont want to read from their socket
            ch.config().setAutoRead(false);
            ch.close();
            return;
        }

        final PublishFlushHandler publishFlushHandler = channelDependencies.createPublishFlushHandler();
        final ClientConnection clientConnection = new ClientConnection(ch, publishFlushHandler);
        ch.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);

        clientConnection.setConnectedListener(listener);

        ch.pipeline()
                .addLast(ALL_CHANNELS_GROUP_HANDLER, new ChannelGroupHandler(channelDependencies.getChannelGroup()));
        if (throttlingEnabled) {
            ch.pipeline().addLast(GLOBAL_THROTTLING_HANDLER, channelDependencies.getGlobalTrafficShapingHandler());
        }
        ch.pipeline().addLast(MQTT_MESSAGE_DECODER, new MQTTMessageDecoder(channelDependencies));
        ch.pipeline().addLast(MQTT_MESSAGE_ENCODER, channelDependencies.getMqttMessageEncoder());
        addNoConnectIdleHandler(ch);
        //MQTT_5_FLOW_CONTROL_HANDLER is added here after CONNECT
        ch.pipeline()
                .addLast(MQTT_MESSAGE_BARRIER, new MessageBarrier(channelDependencies.getMqttServerDisconnector()));

        final HandlerPackage handlerPackage = channelDependencies.getHandlerProvider().get();
        if (handlerPackage != null) {
            ch.pipeline().addLast(handlerPackage.getHandlerName(), handlerPackage.getHandler());
        }

        // before connack outbound interceptor as it initializes the client context after the connack
        ch.pipeline().addLast(PLUGIN_INITIALIZER_HANDLER, channelDependencies.getPluginInitializerHandler());

        ch.pipeline().addLast(INTERCEPTOR_HANDLER, channelDependencies.getInterceptorHandler());

        //MQTT_PUBLISH_FLOW_HANDLER is added here after CONNECT
        ch.pipeline().addLast(MESSAGE_EXPIRY_HANDLER, channelDependencies.getPublishMessageExpiryHandler());

        ch.pipeline().addLast(MQTT_SUBSCRIBE_HANDLER, channelDependencies.getSubscribeHandler());

        ch.pipeline().addLast(PUBLISH_FLUSH_HANDLER, publishFlushHandler);
        // after connect inbound interceptor as it intercepts the connect
        ch.pipeline().addLast(CLIENT_LIFECYCLE_EVENT_HANDLER, channelDependencies.getClientLifecycleEventHandler());

        ch.pipeline().addLast(MQTT_AUTH_HANDLER, channelDependencies.getAuthHandler());
        ch.pipeline().addLast(CONNECTION_LIMITER, channelDependencies.getConnectionLimiterHandler());
        ch.pipeline().addLast(MQTT_CONNECT_HANDLER, channelDependencies.getConnectHandler());


        ch.pipeline().addLast(MQTT_PINGREQ_HANDLER, channelDependencies.getPingRequestHandler());
        ch.pipeline().addLast(MQTT_UNSUBSCRIBE_HANDLER, channelDependencies.getUnsubscribeHandler());
        ch.pipeline().addLast(MQTT_DISCONNECT_HANDLER, channelDependencies.getDisconnectHandler());

        addSpecialHandlers(ch);

        ch.pipeline().addLast(EXCEPTION_HANDLER, channelDependencies.getExceptionHandler());
    }

    protected void addNoConnectIdleHandler(final @NotNull Channel ch) {

        //get timeout value from internal config
        final RestrictionsConfigurationService restrictionsConfig =
                channelDependencies.getRestrictionsConfigurationService();

        final long timeoutMillis = restrictionsConfig.noConnectIdleTimeout();

        if (timeoutMillis > 0) {
            final IdleStateHandler idleStateHandler = new IdleStateHandler(timeoutMillis, 0, 0, TimeUnit.MILLISECONDS);

            ch.pipeline().addAfter(MQTT_MESSAGE_ENCODER, NEW_CONNECTION_IDLE_HANDLER, idleStateHandler);
            ch.pipeline()
                    .addAfter(NEW_CONNECTION_IDLE_HANDLER,
                            NO_CONNECT_IDLE_EVENT_HANDLER,
                            channelDependencies.getNoConnectIdleHandler());
        }
    }

    protected abstract void addSpecialHandlers(final @NotNull Channel ch) throws Exception;

    @Override
    public void exceptionCaught(final @NotNull ChannelHandlerContext ctx, final @NotNull Throwable cause)
            throws Exception {
        if (cause instanceof SslException) {
            final ClientConnection clientConnection = ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
            log.error("{}. Disconnecting client {} ",
                    cause.getMessage(),
                    clientConnection.getChannelIP().orElse("UNKNOWN"));
            log.debug("Original exception:", cause);
            final String eventLogMessage = appendListenerToMessage(ctx.channel(),
                    cause.getMessage() != null ? cause.getMessage() : "TLS connection initialization failed");
            //We need to close the channel because the initialization wasn't successful
            channelDependencies.getMqttServerDisconnector().logAndClose(ctx.channel(), null,  //already logged
                    eventLogMessage);
        } else {

            //Just use the default handler
            super.exceptionCaught(ctx, cause);
        }
    }

}
