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
package com.hivemq.bridge.mqtt;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.bridge.MqttForwarder;
import com.hivemq.bridge.config.BridgeTls;
import com.hivemq.bridge.config.LocalSubscription;
import com.hivemq.bridge.config.MqttBridge;
import com.hivemq.bridge.config.RemoteSubscription;
import com.hivemq.bridge.metrics.PerBridgeMetrics;
import com.hivemq.client.mqtt.MqttClientSslConfig;
import com.hivemq.client.mqtt.MqttClientSslConfigBuilder;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.datatypes.MqttTopicFilter;
import com.hivemq.client.mqtt.datatypes.MqttUtf8String;
import com.hivemq.client.mqtt.lifecycle.MqttClientReconnector;
import com.hivemq.client.mqtt.lifecycle.MqttDisconnectSource;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.Mqtt5ClientBuilder;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperties;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserPropertiesBuilder;
import com.hivemq.client.mqtt.mqtt5.exceptions.Mqtt5DisconnectException;
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5RetainHandling;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5Subscription;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAck;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.edge.HiveMQEdgeConstants;
import com.hivemq.edge.model.TypeIdentifier;
import com.hivemq.edge.modules.api.events.EventService;
import com.hivemq.edge.modules.api.events.EventUtils;
import com.hivemq.edge.modules.api.events.model.Event;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.security.ssl.SslUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class BridgeMqttClient {

    private static final Logger log = LoggerFactory.getLogger(BridgeMqttClient.class);

    private final @NotNull MqttBridge bridge;
    private final @NotNull BridgeInterceptorHandler bridgeInterceptorHandler;
    private final @NotNull SystemInformation systemInformation;
    private final @NotNull HivemqId hivemqId;
    private final @NotNull Mqtt5AsyncClient mqtt5Client;
    private final @NotNull ListeningExecutorService executorService;
    private final @NotNull PerBridgeMetrics perBridgeMetrics;
    private final @NotNull EventService eventService;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final @NotNull List<MqttForwarder> forwarders = Collections.synchronizedList(new ArrayList<>());

    public BridgeMqttClient(
            final @NotNull SystemInformation systemInformation,
            final @NotNull MqttBridge bridge,
            final @NotNull BridgeInterceptorHandler bridgeInterceptorHandler,
            final @NotNull HivemqId hivemqId,
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull EventService eventService) {

        this.hivemqId = hivemqId;
        this.systemInformation = systemInformation;
        this.bridge = bridge;
        this.bridgeInterceptorHandler = bridgeInterceptorHandler;
        this.eventService = eventService;
        this.mqtt5Client = createClient();
        executorService = MoreExecutors.newDirectExecutorService();
        perBridgeMetrics = new PerBridgeMetrics(bridge.getId(), metricRegistry);
    }

    @NotNull
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private Mqtt5AsyncClient createClient() {
        final Mqtt5ClientBuilder builder = Mqtt5Client.builder();
        builder.identifier(bridge.getClientId());
        builder.serverHost(bridge.getHost());
        builder.serverPort(bridge.getPort());

        //-- Bind connection listeners to maintain status
        builder.addConnectedListener(context -> {
            log.debug("Bridge {} connected", bridge.getId());
            connected.set(true);
            forwarders.forEach(MqttForwarder::drainQueue);
        });

        builder.addConnectedListener(context -> eventService.fireEvent(eventBuilder(Event.SEVERITY.INFO).withMessage(
                String.format("Bridge '%s' connected", getBridge().getId())).build()));

        //-- Fire a system event for the various logging layers
        builder.addDisconnectedListener(context -> eventService.fireEvent(eventBuilder(context.getCause() == null ?
                Event.SEVERITY.INFO :
                Event.SEVERITY.ERROR).withMessage(String.format("Bridge '%s' disconnected", getBridge().getId()))
                .withPayload(EventUtils.generateErrorPayload(context.getCause()))
                .build()));

        builder.addDisconnectedListener(context -> {
            final Throwable cause = context.getCause();
            String message = cause.getMessage();
            if (cause instanceof Mqtt5DisconnectException) {
                final Mqtt5DisconnectException disconnectException = (Mqtt5DisconnectException) cause;
                message += " Code: " + disconnectException.getMqttMessage().getReasonCode();
                final Optional<MqttUtf8String> reasonString = disconnectException.getMqttMessage().getReasonString();
                if (reasonString.isPresent()) {
                    message += " Reason: " + reasonString.get();
                }
            }
            log.debug("Bridge {} disconnected: {}", bridge.getId(), message);
            connected.set(false);
        });

        //auto-reconnect
        builder.addDisconnectedListener(context -> {
            if (stopped.get()) {
                return;
            }
            if (context.getSource() != MqttDisconnectSource.USER) {
                final MqttClientReconnector reconnector = context.getReconnector();
                final long delay = (long) Math.min(1_000_000_000L * Math.pow(2, reconnector.getAttempts()),
                        120_000_000_000L); //exponential backoff with min 1s, max 120s (+ random delay)
                final long randomDelay =
                        (long) (delay / 4d / Integer.MAX_VALUE * ThreadLocalRandom.current().nextInt());
                reconnector.reconnect(true).delay(delay + randomDelay, TimeUnit.NANOSECONDS);
            }
        });

        final BridgeTls bridgeTls = bridge.getBridgeTls();
        if (bridgeTls != null) {
            final MqttClientSslConfigBuilder sslConfigBuilder = MqttClientSslConfig.builder();
            if (!bridgeTls.getCipherSuites().isEmpty()) {
                sslConfigBuilder.cipherSuites(bridgeTls.getCipherSuites());
            }
            if (!bridgeTls.getProtocols().isEmpty()) {
                sslConfigBuilder.protocols(bridgeTls.getProtocols());
            }
            if (bridgeTls.getKeystorePath() != null) {
                final KeyManagerFactory keyManagerFactory = SslUtil.createKeyManagerFactory(bridgeTls.getKeystoreType(),
                        bridgeTls.getKeystorePath(),
                        bridgeTls.getKeystorePassword(),
                        bridgeTls.getPrivateKeyPassword());
                sslConfigBuilder.keyManagerFactory(keyManagerFactory);
            }

            if (bridgeTls.getTruststorePath() != null) {
                final TrustManagerFactory trustManagerFactory =
                        SslUtil.createTrustManagerFactory(bridgeTls.getTruststoreType(),
                                bridgeTls.getTruststorePath(),
                                bridgeTls.getTruststorePassword());
                sslConfigBuilder.trustManagerFactory(trustManagerFactory);
            }

            if (!bridgeTls.isVerifyHostname()) {
                sslConfigBuilder.hostnameVerifier((hostname, session) -> true);
            }

            sslConfigBuilder.handshakeTimeout(bridgeTls.getHandshakeTimeout(), TimeUnit.SECONDS);
            builder.sslConfig(sslConfigBuilder.build());
        }

        if (bridge.getUsername() != null && bridge.getPassword() != null) {
            builder.simpleAuth()
                    .username(bridge.getUsername())
                    .password(bridge.getPassword().getBytes(UTF_8))
                    .applySimpleAuth();
        }
        return builder.buildAsync();
    }

    public @NotNull Mqtt5AsyncClient getMqtt5Client() {
        return mqtt5Client;
    }

    public @NotNull ListenableFuture<Void> start() {

        final SettableFuture<Void> resultFuture = SettableFuture.create();
        stopped.set(false);

        final Mqtt5UserPropertiesBuilder mqtt5UserPropertiesBuilder = Mqtt5UserProperties.builder();
        mqtt5UserPropertiesBuilder.add(HiveMQEdgeConstants.CLIENT_AGENT_PROPERTY,
                String.format(HiveMQEdgeConstants.CLIENT_AGENT_PROPERTY_VALUE, systemInformation.getHiveMQVersion()));
        final CompletableFuture<Mqtt5ConnAck> connectFuture = mqtt5Client.connectWith()
                .cleanStart(bridge.isCleanStart())
                .keepAlive(bridge.getKeepAlive())
                .userProperties(mqtt5UserPropertiesBuilder.build())
                .sessionExpiryInterval(bridge.getSessionExpiry())
                .send();

        connectFuture.handleAsync((mqtt5ConnAck, throwable) -> {

            if (throwable != null) {
                log.error("Not able to connect bridge {}", bridge.getId(), throwable);
                return null;
            }

            if (mqtt5ConnAck.getReasonCode().isError()) {
                log.error("Not able to connect bridge '{}', CONNACK returned reason code {}, reason string: '{}'",
                        bridge.getId(),
                        mqtt5ConnAck.getReasonCode(),
                        mqtt5ConnAck.getReasonString().map(Objects::toString).orElse(""));
            }

            for (MqttForwarder forwarder : forwarders) {
                forwarder.drainQueue();
            }

            ImmutableList.Builder<CompletableFuture<Mqtt5SubAck>> subscribeFutures = new ImmutableList.Builder<>();
            for (RemoteSubscription remoteSubscription : bridge.getRemoteSubscriptions()) {
                final List<Mqtt5Subscription> subscriptions = convertSubscriptions(remoteSubscription);
                final Consumer<Mqtt5Publish> mqtt5PublishConsumer = new RemotePublishConsumer(remoteSubscription,
                        bridgeInterceptorHandler,
                        bridge,
                        executorService,
                        hivemqId,
                        perBridgeMetrics);
                final CompletableFuture<Mqtt5SubAck> send = mqtt5Client.subscribeWith()
                        .addSubscriptions(subscriptions)
                        .callback(mqtt5PublishConsumer)
                        .send();

                subscribeFutures.add(send);
            }

            CompletableFuture.allOf(subscribeFutures.build().toArray(new CompletableFuture[0])).thenRun(() -> {
                resultFuture.set(null);
            });

            return null;
        });


        return resultFuture;
    }


    @NotNull
    private static List<Mqtt5Subscription> convertSubscriptions(
            final @NotNull RemoteSubscription remoteSubscription) {
        return remoteSubscription.getFilters().stream().map(originalFilter -> {
            MqttTopicFilter topicFilter = MqttTopicFilter.of(originalFilter);
            return Mqtt5Subscription.builder()
                    .topicFilter(topicFilter)
                    .qos(Objects.requireNonNullElse(MqttQos.fromCode(remoteSubscription.getMaxQoS()),
                            MqttQos.AT_MOST_ONCE))
                    .retainAsPublished(remoteSubscription.isPreserveRetain())
                    .retainHandling(Mqtt5RetainHandling.DO_NOT_SEND)
                    .build();
        }).collect(Collectors.toUnmodifiableList());
    }

    public void stop() {
        stopped.set(true);
        mqtt5Client.toAsync().disconnect();
    }

    public @NotNull List<MqttForwarder> createForwarders() {
        final ImmutableList.Builder<MqttForwarder> builder = ImmutableList.builder();
        for (LocalSubscription localSubscription : bridge.getLocalSubscriptions()) {
            builder.add(new RemoteMqttForwarder(createForwarderId(bridge.getId(), localSubscription),
                    bridge,
                    localSubscription,
                    this,
                    perBridgeMetrics,
                    bridgeInterceptorHandler));
        }
        forwarders.addAll(builder.build());
        return Collections.unmodifiableList(forwarders);
    }

    @NotNull
    public static String createForwarderId(final @NotNull String bridgeId, LocalSubscription localSubscription) {
        return bridgeId + "-" + localSubscription.calculateUniqueId();
    }

    public @NotNull List<MqttForwarder> getActiveForwarders() {
        return forwarders;
    }

    public @NotNull MqttBridge getBridge() {
        return bridge;
    }

    public boolean isConnected() {
        return connected.get();
    }

    protected @NotNull Event.Builder eventBuilder(final @NotNull Event.SEVERITY severity) {
        Event.Builder builder = new Event.Builder();
        builder.withTimestamp(System.currentTimeMillis());
        builder.withSource(TypeIdentifier.create(TypeIdentifier.TYPE.BRIDGE, bridge.getId()));
        builder.withSeverity(severity);
        return builder;
    }
}
