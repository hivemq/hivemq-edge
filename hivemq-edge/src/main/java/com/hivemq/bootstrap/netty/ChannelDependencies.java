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
package com.hivemq.bootstrap.netty;

import com.hivemq.bootstrap.factories.HandlerProvider;
import com.hivemq.codec.decoder.mqtt.MqttConnectDecoder;
import com.hivemq.codec.decoder.mqtt.MqttDecoders;
import com.hivemq.codec.encoder.EncoderFactory;
import com.hivemq.codec.encoder.MQTTMessageEncoder;
import com.hivemq.codec.transcoder.Mqtt5ToMqttsnTranscoder;
import com.hivemq.codec.transcoder.MqttsnToMqtt5Transcoder;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import org.jetbrains.annotations.NotNull;
import com.hivemq.extensions.handler.ClientLifecycleEventHandler;
import com.hivemq.extensions.handler.IncomingPublishHandler;
import com.hivemq.extensions.handler.IncomingSubscribeHandler;
import com.hivemq.extensions.handler.PluginInitializerHandler;
import com.hivemq.logging.EventLog;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.metrics.handler.GlobalMQTTMessageCounter;
import com.hivemq.mqtt.handler.InterceptorHandler;
import com.hivemq.mqtt.handler.auth.AuthHandler;
import com.hivemq.mqtt.handler.auth.AuthInProgressMessageHandler;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.handler.connect.ConnectHandler;
import com.hivemq.mqtt.handler.connect.ConnectionLimiterHandler;
import com.hivemq.mqtt.handler.connect.NoConnectIdleHandler;
import com.hivemq.mqtt.handler.disconnect.DisconnectHandler;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.handler.ping.PingRequestHandler;
import com.hivemq.mqtt.handler.publish.MessageExpiryHandler;
import com.hivemq.mqtt.handler.publish.PublishFlushHandler;
import com.hivemq.mqtt.handler.subscribe.SubscribeHandler;
import com.hivemq.mqtt.handler.unsubscribe.UnsubscribeHandler;
import com.hivemq.mqttsn.IMqttsnTopicRegistry;
import com.hivemq.mqttsn.handler.sleep.AwakeHandler;
import com.hivemq.mqttsn.handler.sleep.SleepHandler;
import com.hivemq.mqttsn.services.IGatewayBroadcastService;
import com.hivemq.security.ssl.SslParameterHandler;
import dagger.Lazy;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

public class ChannelDependencies {

    private final @NotNull NoConnectIdleHandler noConnectIdleHandler;
    private final @NotNull Lazy<ConnectHandler> connectHandlerProvider;
    private final @NotNull ConnectionLimiterHandler connectionLimiterHandler;
    private final @NotNull DisconnectHandler disconnectHandler;
    private final @NotNull Provider<SubscribeHandler> subscribeHandlerProvider;
    private final @NotNull UnsubscribeHandler unsubscribeHandler;
    private final @NotNull ChannelGroup channelGroup;
    private final @NotNull ConfigurationService fullConfigurationService;
    private final @NotNull GlobalTrafficShapingHandler globalTrafficShapingHandler;
    private final @NotNull MetricsHolder metricsHolder;
    private final @NotNull ExceptionHandler exceptionHandler;
    private final @NotNull PingRequestHandler pingRequestHandler;
    private final @NotNull RestrictionsConfigurationService restrictionsConfigurationService;
    private final @NotNull MqttConnectDecoder mqttConnectDecoder;
    private final @NotNull MQTTMessageEncoder mqttMessageEncoder;
    private final @NotNull EventLog eventLog;
    private final @NotNull SslParameterHandler sslParameterHandler;
    private final @NotNull MqttDecoders mqttDecoders;
    private final @NotNull Provider<AuthHandler> authHandlerProvider;
    private final @NotNull Provider<PluginInitializerHandler> pluginInitializerHandlerProvider;
    private final @NotNull Provider<ClientLifecycleEventHandler> clientLifecycleEventHandlerProvider;
    private final @NotNull AuthInProgressMessageHandler authInProgressMessageHandler;
    private final @NotNull Provider<IncomingPublishHandler> incomingPublishHandlerProvider;
    private final @NotNull Provider<IncomingSubscribeHandler> incomingSubscribeHandlerProvider;
    private final @NotNull Provider<MessageExpiryHandler> publishMessageExpiryHandlerProvider;
    private final @NotNull MqttServerDisconnector mqttServerDisconnector;
    private final @NotNull MqttConnacker mqttConnacker;
    private final @NotNull InterceptorHandler interceptorHandler;
    private final @NotNull GlobalMQTTMessageCounter globalMQTTMessageCounter;
    private final @NotNull ShutdownHooks shutdownHooks;
    private final @NotNull IMqttsnTopicRegistry mqttsnTopicRegistry;
    private final @NotNull MqttsnToMqtt5Transcoder mqttsnToMqttTranscoder;
    private final @NotNull Mqtt5ToMqttsnTranscoder mqttToMqttsnTranscoder;
    private final @NotNull HivemqId hiveMqId;
    private final @NotNull AwakeHandler awakeHandler;
    private final @NotNull SleepHandler sleepHandler;
    private final @NotNull Provider<IGatewayBroadcastService> gatewayDiscoveryService;
    private final @NotNull HandlerProvider handlerProvider;

    @Inject
    public ChannelDependencies(
            final @NotNull NoConnectIdleHandler noConnectIdleHandler,
            final @NotNull Lazy<ConnectHandler> connectHandlerProvider,
            final @NotNull ConnectionLimiterHandler connectionLimiterHandler,
            final @NotNull DisconnectHandler disconnectHandler,
            final @NotNull Provider<SubscribeHandler> subscribeHandlerProvider,
            final @NotNull UnsubscribeHandler unsubscribeHandler,
            final @NotNull ChannelGroup channelGroup,
            final @NotNull ConfigurationService fullConfigurationService,
            final @NotNull GlobalTrafficShapingHandler globalTrafficShapingHandler,
            final @NotNull MetricsHolder metricsHolder,
            final @NotNull ExceptionHandler exceptionHandler,
            final @NotNull PingRequestHandler pingRequestHandler,
            final @NotNull RestrictionsConfigurationService restrictionsConfigurationService,
            final @NotNull MqttConnectDecoder mqttConnectDecoder,
            final @NotNull EventLog eventLog,
            final @NotNull SslParameterHandler sslParameterHandler,
            final @NotNull MqttDecoders mqttDecoders,
            final @NotNull EncoderFactory encoderFactory,
            final @NotNull Provider<AuthHandler> authHandlerProvider,
            final @NotNull AuthInProgressMessageHandler authInProgressMessageHandler,
            final @NotNull Provider<PluginInitializerHandler> pluginInitializerHandlerProvider,
            final @NotNull Provider<ClientLifecycleEventHandler> clientLifecycleEventHandlerProvider,
            final @NotNull Provider<IncomingPublishHandler> incomingPublishHandlerProvider,
            final @NotNull Provider<IncomingSubscribeHandler> incomingSubscribeHandlerProvider,
            final @NotNull Provider<MessageExpiryHandler> publishMessageExpiryHandlerProvider,
            final @NotNull MqttServerDisconnector mqttServerDisconnector,
            final @NotNull InterceptorHandler interceptorHandler,
            final @NotNull GlobalMQTTMessageCounter globalMQTTMessageCounter,
            final @NotNull ShutdownHooks shutdownHooks,
            final @NotNull IMqttsnTopicRegistry mqttsnTopicRegistry,
            final @NotNull HivemqId hiveMqId,
            final @NotNull MqttsnToMqtt5Transcoder mqttsnToMqttTranscoder,
            final @NotNull Mqtt5ToMqttsnTranscoder mqttToMqttsnTranscoder,
            final @NotNull AwakeHandler awakeHandler,
            final @NotNull SleepHandler sleepHandler,
            final @NotNull MqttConnacker mqttConnacker,
            final @NotNull Provider<IGatewayBroadcastService> gatewayDiscoveryService, @NotNull HandlerProvider handlerProvider) {

        this.noConnectIdleHandler = noConnectIdleHandler;
        this.connectHandlerProvider = connectHandlerProvider;
        this.connectionLimiterHandler = connectionLimiterHandler;
        this.disconnectHandler = disconnectHandler;
        this.subscribeHandlerProvider = subscribeHandlerProvider;
        this.unsubscribeHandler = unsubscribeHandler;
        this.channelGroup = channelGroup;
        this.fullConfigurationService = fullConfigurationService;
        this.globalTrafficShapingHandler = globalTrafficShapingHandler;
        this.metricsHolder = metricsHolder;
        this.exceptionHandler = exceptionHandler;
        this.pingRequestHandler = pingRequestHandler;
        this.restrictionsConfigurationService = restrictionsConfigurationService;
        this.mqttConnectDecoder = mqttConnectDecoder;
        this.shutdownHooks = shutdownHooks;
        this.mqttsnTopicRegistry = mqttsnTopicRegistry;
        this.handlerProvider = handlerProvider;
        this.mqttMessageEncoder = new MQTTMessageEncoder(encoderFactory, globalMQTTMessageCounter);
        this.eventLog = eventLog;
        this.sslParameterHandler = sslParameterHandler;
        this.mqttDecoders = mqttDecoders;
        this.authHandlerProvider = authHandlerProvider;
        this.authInProgressMessageHandler = authInProgressMessageHandler;
        this.pluginInitializerHandlerProvider = pluginInitializerHandlerProvider;
        this.clientLifecycleEventHandlerProvider = clientLifecycleEventHandlerProvider;
        this.incomingPublishHandlerProvider = incomingPublishHandlerProvider;
        this.incomingSubscribeHandlerProvider = incomingSubscribeHandlerProvider;
        this.publishMessageExpiryHandlerProvider = publishMessageExpiryHandlerProvider;
        this.mqttServerDisconnector = mqttServerDisconnector;
        this.interceptorHandler = interceptorHandler;
        this.globalMQTTMessageCounter = globalMQTTMessageCounter;
        this.hiveMqId = hiveMqId;
        this.mqttsnToMqttTranscoder = mqttsnToMqttTranscoder;
        this.mqttToMqttsnTranscoder = mqttToMqttsnTranscoder;
        this.awakeHandler = awakeHandler;
        this.sleepHandler = sleepHandler;
        this.mqttConnacker = mqttConnacker;
        this.gatewayDiscoveryService = gatewayDiscoveryService;
    }

    @NotNull
    public NoConnectIdleHandler getNoConnectIdleHandler() {
        return noConnectIdleHandler;
    }

    @NotNull
    public ConnectHandler getConnectHandler() {
        return connectHandlerProvider.get();
    }

    @NotNull
    public ConnectionLimiterHandler getConnectionLimiterHandler() {
        return connectionLimiterHandler;
    }

    @NotNull
    public DisconnectHandler getDisconnectHandler() {
        return disconnectHandler;
    }

    @NotNull
    public SubscribeHandler getSubscribeHandler() {
        return subscribeHandlerProvider.get();
    }

    @NotNull
    public UnsubscribeHandler getUnsubscribeHandler() {
        return unsubscribeHandler;
    }

    @NotNull
    public ChannelGroup getChannelGroup() {
        return channelGroup;
    }

    @NotNull
    public ConfigurationService getConfigurationService() {
        return fullConfigurationService;
    }

    @NotNull
    public GlobalTrafficShapingHandler getGlobalTrafficShapingHandler() {
        return globalTrafficShapingHandler;
    }

    @NotNull
    public MetricsHolder getMetricsHolder() {
        return metricsHolder;
    }

    @NotNull
    public ExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

    @NotNull
    public PingRequestHandler getPingRequestHandler() {
        return pingRequestHandler;
    }

    @NotNull
    public RestrictionsConfigurationService getRestrictionsConfigurationService() {
        return restrictionsConfigurationService;
    }

    @NotNull
    public MqttConnectDecoder getMqttConnectDecoder() {
        return mqttConnectDecoder;
    }

    @NotNull
    public MQTTMessageEncoder getMqttMessageEncoder() {
        return mqttMessageEncoder;
    }

    @NotNull
    public MessageExpiryHandler getPublishMessageExpiryHandler() {
        return publishMessageExpiryHandlerProvider.get();
    }

    @NotNull
    public EventLog getEventLog() {
        return eventLog;
    }

    @NotNull
    public SslParameterHandler getSslParameterHandler() {
        return sslParameterHandler;
    }

    @NotNull
    public MqttDecoders getMqttDecoders() {
        return mqttDecoders;
    }

    @NotNull
    public AuthHandler getAuthHandler() {
        return authHandlerProvider.get();
    }

    @NotNull
    public AuthInProgressMessageHandler getAuthInProgressMessageHandler() {
        return authInProgressMessageHandler;
    }

    @NotNull
    public PluginInitializerHandler getPluginInitializerHandler() {
        return pluginInitializerHandlerProvider.get();
    }

    @NotNull
    public ClientLifecycleEventHandler getClientLifecycleEventHandler() {
        return clientLifecycleEventHandlerProvider.get();
    }

    @NotNull
    public IncomingPublishHandler getIncomingPublishHandler() {
        return incomingPublishHandlerProvider.get();
    }

    @NotNull
    public IncomingSubscribeHandler getIncomingSubscribeHandler() {
        return incomingSubscribeHandlerProvider.get();
    }

    @NotNull
    public MqttServerDisconnector getMqttServerDisconnector() {
        return mqttServerDisconnector;
    }

    @NotNull
    public InterceptorHandler getInterceptorHandler() {
        return interceptorHandler;
    }

    @NotNull
    public GlobalMQTTMessageCounter getGlobalMQTTMessageCounter() {
        return globalMQTTMessageCounter;
    }

    @NotNull
    public PublishFlushHandler createPublishFlushHandler() {
        return new PublishFlushHandler(metricsHolder);
    }

    @NotNull
    public ShutdownHooks getShutdownHooks() {
        return shutdownHooks;
    }

    public @NotNull IMqttsnTopicRegistry getMqttsnTopicRegistry() {
        return mqttsnTopicRegistry;
    }

    public @NotNull MqttsnToMqtt5Transcoder getMqttsnToMqttTranscoder() {
        return mqttsnToMqttTranscoder;
    }

    public @NotNull Mqtt5ToMqttsnTranscoder getMqttToMqttsnTranscoder() {
        return mqttToMqttsnTranscoder;
    }

    public @NotNull HivemqId getHiveMqId() {
        return hiveMqId;
    }

    public @NotNull AwakeHandler getAwakeHandler() {
        return awakeHandler;
    }

    public @NotNull SleepHandler getSleepHandler() {
        return sleepHandler;
    }

    public @NotNull MqttConnacker getMqttConnacker() {
        return mqttConnacker;
    }

    public @NotNull Provider<IGatewayBroadcastService> getGatewayDiscoveryService() {
        return gatewayDiscoveryService;
    }

    public @NotNull HandlerProvider getHandlerProvider() {
        return handlerProvider;
    }
}
