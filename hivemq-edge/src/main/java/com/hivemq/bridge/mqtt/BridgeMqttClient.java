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
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.events.model.EventBuilder;
import com.hivemq.adapter.sdk.api.events.model.Payload;
import com.hivemq.adapter.sdk.api.events.model.TypeIdentifier;
import com.hivemq.bridge.MqttForwarder;
import com.hivemq.bridge.config.BridgeTls;
import com.hivemq.bridge.config.BridgeWebsocketConfig;
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
import com.hivemq.client.mqtt.mqtt5.exceptions.Mqtt5DisconnectException;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5RetainHandling;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5Subscription;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAck;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.edge.model.TypeIdentifierImpl;
import com.hivemq.edge.modules.api.events.model.EventImpl;
import com.hivemq.security.ssl.SslUtil;
import com.hivemq.util.StoreTypeUtil;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.hivemq.edge.HiveMQEdgeConstants.CLIENT_AGENT_PROPERTY;
import static com.hivemq.edge.HiveMQEdgeConstants.CLIENT_AGENT_PROPERTY_VALUE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNullElse;

public class BridgeMqttClient {

    private static final @NotNull Logger log = LoggerFactory.getLogger(BridgeMqttClient.class);

    private static final long RECONNECT_MIN_DELAY_MS = 1_000;       // 1 second
    private static final long RECONNECT_MAX_DELAY_MS = 120_000;     // 2 minutes
    private static final double RECONNECT_JITTER_FACTOR = 0.25;     // 25% max jitter
    private static final int RECONNECT_MAX_BACKOFF_EXPONENT = 10;   // 2^10 = 1024 seconds max

    private final @NotNull MqttBridge bridge;
    private final @NotNull BridgeInterceptorHandler bridgeInterceptorHandler;
    private final @NotNull SystemInformation systemInformation;
    private final @NotNull HivemqId hivemqId;
    private final @NotNull Mqtt5AsyncClient mqtt5Client;
    private final @NotNull ListeningExecutorService executorService;
    private final @NotNull PerBridgeMetrics perBridgeMetrics;
    private final @NotNull EventService eventService;
    private final @NotNull MetricRegistry metricRegistry;
    private final @NotNull AtomicBoolean connected;
    private final @NotNull AtomicReference<OperationState> operationState;
    private final @NotNull AtomicReference<SettableFuture<Void>> startFutureRef;
    private final @NotNull AtomicReference<SettableFuture<Void>> stopFutureRef;
    private final @NotNull AtomicBoolean stopped;
    private final @NotNull List<MqttForwarder> forwarders;

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
        this.metricRegistry = metricRegistry;
        this.mqtt5Client = createClient();
        this.perBridgeMetrics = new PerBridgeMetrics(bridge.getId(), metricRegistry);
        this.connected = new AtomicBoolean();
        this.stopped = new AtomicBoolean();
        this.forwarders = Collections.synchronizedList(new ArrayList<>());
        this.executorService = MoreExecutors.newDirectExecutorService();
        this.operationState = new AtomicReference<>(OperationState.IDLE);
        this.startFutureRef = new AtomicReference<>();
        this.stopFutureRef = new AtomicReference<>();
    }

    public static @NotNull String createForwarderId(
            final @NotNull String bridgeId,
            final @NotNull LocalSubscription sub) {
        return bridgeId + '-' + sub.calculateUniqueId();
    }

    public synchronized @NotNull ListenableFuture<Void> start() {
        if (operationState.compareAndSet(OperationState.IDLE, OperationState.STARTING)) {
            log.info("Starting bridge '{}' connecting to {}:{}", bridge.getId(), bridge.getHost(), bridge.getPort());
            stopped.set(false);
            final SettableFuture<Void> startFuture = SettableFuture.create();
            startFutureRef.set(startFuture);
            final long connectStartTime = log.isDebugEnabled() ? System.nanoTime() : 0;
            mqtt5Client.connectWith()
                    .cleanStart(bridge.isCleanStart())
                    .keepAlive(bridge.getKeepAlive())
                    .userProperties(Mqtt5UserProperties.builder()
                            .add(CLIENT_AGENT_PROPERTY,
                                    String.format(CLIENT_AGENT_PROPERTY_VALUE, systemInformation.getHiveMQVersion()))
                            .build())
                    .sessionExpiryInterval(bridge.getSessionExpiry())
                    .send()
                    .handleAsync((mqtt5ConnAck, throwable) -> {
                        if (stopped.get()) {
                            if (log.isDebugEnabled()) {
                                log.debug("Bridge '{}' stopped during connection attempt", bridge.getId());
                            }
                            return null;
                        }
                        if (throwable != null) {
                            log.error("Failed to connect bridge '{}' to {}:{}: {}",
                                    bridge.getId(), bridge.getHost(), bridge.getPort(), throwable.getMessage());
                            log.debug("Connection exception details", throwable);
                            return null;
                        }
                        if (mqtt5ConnAck.getReasonCode().isError()) {
                            log.error(
                                    "Failed to connect bridge '{}', CONNACK returned reason code {}, reason string: '{}'",
                                    bridge.getId(),
                                    mqtt5ConnAck.getReasonCode(),
                                    mqtt5ConnAck.getReasonString().map(Objects::toString).orElse(""));
                            return null;
                        }

                        if (log.isDebugEnabled()) {
                            final long connectMicros = (System.nanoTime() - connectStartTime) / 1000;
                            log.debug("Bridge '{}' MQTT connection established in {} μs", bridge.getId(), connectMicros);
                        }

                        final int forwarderCount = forwarders.size();
                        if (forwarderCount > 0 && log.isDebugEnabled()) {
                            log.debug("Draining queues for {} forwarder(s) on bridge '{}'", forwarderCount, bridge.getId());
                        }
                        forwarders.forEach(MqttForwarder::drainQueue);

                        final ImmutableList.Builder<@NotNull CompletableFuture<Mqtt5SubAck>> subFutures =
                                new ImmutableList.Builder<>();
                        final int remoteSubCount = bridge.getRemoteSubscriptions().size();
                        if (remoteSubCount > 0 && log.isDebugEnabled()) {
                            log.debug("Setting up {} remote subscription(s) for bridge '{}'", remoteSubCount, bridge.getId());
                        }

                        for (final RemoteSubscription sub : bridge.getRemoteSubscriptions()) {
                            if (log.isTraceEnabled()) {
                                log.trace("Subscribing to remote topics {} for bridge '{}'", sub.getFilters(), bridge.getId());
                            }
                            subFutures.add(mqtt5Client.subscribeWith()
                                    .addSubscriptions(sub.getFilters()
                                            .stream()
                                            .map(filter -> Mqtt5Subscription.builder()
                                                    .topicFilter(MqttTopicFilter.of(filter))
                                                    .qos(requireNonNullElse(MqttQos.fromCode(sub.getMaxQoS()),
                                                            MqttQos.AT_MOST_ONCE))
                                                    .retainAsPublished(sub.isPreserveRetain())
                                                    .retainHandling(Mqtt5RetainHandling.DO_NOT_SEND)
                                                    .build())
                                            .toList())
                                    .callback(new RemotePublishConsumer(sub,
                                            bridgeInterceptorHandler,
                                            bridge,
                                            executorService,
                                            hivemqId,
                                            perBridgeMetrics))
                                    .send());
                        }
                        CompletableFuture.allOf(subFutures.build().toArray(new CompletableFuture[0]))
                                .handle((result, exception) -> {
                                    if (log.isInfoEnabled()) {
                                        if (exception == null) {
                                            log.info("Bridge '{}' started successfully", bridge.getId());
                                        } else {
                                            log.error("Bridge '{}' started with an internal error {}",
                                                    bridge.getId(),
                                                    exception.getMessage(),
                                                    exception);
                                        }
                                    }
                                    startFutureRef.getAndSet(null).set(null);
                                    operationState.set(OperationState.IDLE);
                                    return null;
                                });
                        return null;
                    });
            return startFuture;
        }
        if (log.isDebugEnabled()) {
            log.debug("Bridge '{}' start requested but operation state is {}, returning ongoing operation",
                    bridge.getId(), operationState.get());
        }
        return getOngoingOperation(operationState.get(), OperationState.STARTING);
    }

    public synchronized @NotNull ListenableFuture<Void> stop() {
        final ListenableFuture<Void> startFuture = startFutureRef.get();
        if (startFuture != null) {
            try {
                log.debug("Waiting for bridge '{}' is being started before it can be stopped", bridge.getId());
                startFuture.get();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                log.debug("Interrupted while waiting for bridge '{}' start", bridge.getId());
            } catch (final ExecutionException e) {
                log.debug("While waiting for bridge '{}' start, there is an error {}", bridge.getId(), e.getMessage());
            }
        }
        if (operationState.compareAndSet(OperationState.IDLE, OperationState.STOPPING) ||
                operationState.compareAndSet(OperationState.STARTING, OperationState.STOPPING)) {
            log.info("Stopping bridge '{}'", bridge.getId());
            final SettableFuture<Void> stopFuture = SettableFuture.create();
            stopFutureRef.set(stopFuture);
            final long stopStartTime = log.isDebugEnabled() ? System.nanoTime() : 0;
            stopped.set(true);
            mqtt5Client.disconnect().handle((result, exception) -> {
                if (log.isDebugEnabled()) {
                    final long stopMicros = (System.nanoTime() - stopStartTime) / 1000;
                    log.debug("Bridge '{}' disconnected in {} μs", bridge.getId(), stopMicros);
                    if (exception != null) {
                        log.debug("Bridge '{}' disconnected with an internal error {}", bridge.getId(), exception.getMessage(), exception);
                    }
                }
                stopFutureRef.getAndSet(null).set(null);
                operationState.set(OperationState.IDLE);
                perBridgeMetrics.clearAll(metricRegistry);
                if (log.isInfoEnabled()) {
                    log.info("Bridge '{}' stopped successfully", bridge.getId());
                }
                return null;
            });
            return stopFuture;
        }
        if (log.isDebugEnabled()) {
            log.debug("Bridge '{}' stop requested but operation state is {}, returning ongoing operation",
                    bridge.getId(), operationState.get());
        }
        return getOngoingOperation(operationState.get(), OperationState.STOPPING);
    }

    public @NotNull Mqtt5AsyncClient getMqtt5Client() {
        return mqtt5Client;
    }

    public @NotNull List<MqttForwarder> createForwarders() {
        final ImmutableList.Builder<@NotNull MqttForwarder> builder = ImmutableList.builder();
        final int localSubCount = bridge.getLocalSubscriptions().size();
        if (log.isDebugEnabled()) {
            log.debug("Creating {} forwarder(s) for local subscriptions on bridge '{}'", localSubCount, bridge.getId());
        }
        for (final var sub : bridge.getLocalSubscriptions()) {
            if (log.isTraceEnabled()) {
                log.trace("Creating forwarder for local topics {} on bridge '{}'", sub.getFilters(), bridge.getId());
            }
            builder.add(new RemoteMqttForwarder(createForwarderId(bridge.getId(), sub),
                    bridge,
                    sub,
                    this,
                    perBridgeMetrics,
                    bridgeInterceptorHandler));
        }
        forwarders.addAll(builder.build());
        if (log.isDebugEnabled()) {
            log.debug("Created {} forwarder(s) for bridge '{}'", forwarders.size(), bridge.getId());
        }
        return Collections.unmodifiableList(forwarders);
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

    protected @NotNull EventBuilder eventBuilder(final @NotNull EventImpl.SEVERITY severity) {
        final EventBuilder builder = eventService.bridgeEvent();
        builder.withTimestamp(System.currentTimeMillis());
        builder.withSource(TypeIdentifierImpl.create(TypeIdentifier.Type.BRIDGE, bridge.getId()));
        builder.withSeverity(severity);
        return builder;
    }

    private @NotNull ListenableFuture<Void> getOngoingOperation(
            final @NotNull OperationState current,
            final @NotNull OperationState target) {
        return switch (current) {
            case STARTING -> {
                if (target == OperationState.STARTING) {
                    final var existing = startFutureRef.get();
                    if (existing != null) {
                        log.info(
                                "Start operation already in progress for adapter with id '{}', returning existing future",
                                bridge.getId());
                        yield existing;
                    }
                }
                final SettableFuture<Void> future = SettableFuture.create();
                future.set(null);
                yield future;
            }
            case STOPPING -> {
                if (target == OperationState.STOPPING) {
                    final var existing = stopFutureRef.get();
                    if (existing != null) {
                        log.info("Stop operation already in progress for adapter with id '{}', returning existing future",
                                bridge.getId());
                        yield existing;
                    }
                    // If no existing future, return completed future instead of null
                    if (log.isDebugEnabled()) {
                        log.debug("Stop operation for bridge '{}' already completed, returning completed future",
                                bridge.getId());
                    }
                }
                final SettableFuture<Void> future = SettableFuture.create();
                future.set(null);
                yield future;
            }
            case IDLE -> {
                final SettableFuture<Void> future = SettableFuture.create();
                future.set(null);
                yield future;
            }
        };
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private @NotNull Mqtt5AsyncClient createClient() {
        final Mqtt5ClientBuilder builder = Mqtt5Client.builder();
        builder.identifier(bridge.getClientId());
        builder.serverHost(bridge.getHost());
        builder.serverPort(bridge.getPort());

        final BridgeWebsocketConfig websocketConfig = bridge.getBridgeWebsocketConfig();
        if (websocketConfig != null) {
            builder.webSocketConfig()
                    .subprotocol(websocketConfig.getSubProtocol())
                    .serverPath(websocketConfig.getPath())
                    .applyWebSocketConfig();
        }

        //-- Bind connection listeners to maintain status
        builder.addConnectedListener(context -> {
            // Check if this is a dangling client that reconnected after being stopped
            if (stopped.get()) {
                log.warn("Bridge '{}' connected but is marked as stopped - disconnecting immediately to prevent dangling client",
                        bridge.getId());
                mqtt5Client.disconnect();
                return;
            }
            log.info("Bridge '{}' connected to {}:{}", bridge.getId(), bridge.getHost(), bridge.getPort());
            connected.set(true);
            final int forwarderCount = forwarders.size();
            if (forwarderCount > 0) {
                if (log.isDebugEnabled()) {
                    log.debug("Draining queues for {} forwarder(s) after reconnection on bridge '{}'",
                            forwarderCount, bridge.getId());
                }
                forwarders.forEach(MqttForwarder::drainQueue);
            }
            eventBuilder(Event.SEVERITY.INFO).withMessage("Bridge '" + bridge.getId() + "' connected").fire();
        });

        //-- Fire a system event for the various logging layers
        builder.addDisconnectedListener(context -> {
            final Throwable cause = context.getCause();
            String message = cause.getMessage();
            if (cause instanceof final Mqtt5DisconnectException disconnectException) {
                message += " Code: " + disconnectException.getMqttMessage().getReasonCode();
                final Optional<MqttUtf8String> reasonString = disconnectException.getMqttMessage().getReasonString();
                if (reasonString.isPresent()) {
                    message += " Reason: " + reasonString.get();
                }
            }
            log.warn("Bridge '{}' disconnected from {}:{}: {}", bridge.getId(), bridge.getHost(), bridge.getPort(), message);
            log.debug("Disconnection cause details", cause);
            connected.set(false);
            eventBuilder(EventImpl.SEVERITY.ERROR).withMessage("Bridge '" + bridge.getId() + "' disconnected")
                    .withPayload(Payload.ContentType.PLAIN_TEXT, ExceptionUtils.getStackTrace(cause))
                    .fire();

            //auto-reconnect
            if (stopped.get()) {
                if (log.isDebugEnabled()) {
                    log.debug("Bridge '{}' is stopped, canceling any pending reconnections", bridge.getId());
                }
                // Explicitly cancel reconnection to prevent dangling clients
                context.getReconnector().reconnect(false);
                return;
            }
            if (context.getSource() != MqttDisconnectSource.USER) {
                // exponential backoff with 1s-2min range, 25% additive jitter for thundering herd prevention
                final MqttClientReconnector reconnector = context.getReconnector();
                final int attempts = reconnector.getAttempts();
                final int exponent = Math.min(attempts, RECONNECT_MAX_BACKOFF_EXPONENT);
                final long calculatedDelay = RECONNECT_MIN_DELAY_MS << exponent;
                final long delayMs = Math.min(calculatedDelay, RECONNECT_MAX_DELAY_MS);
                // Full jitter is often better for reducing load spikes
                // This gives random delay between 0 and delayMs * JITTER_FACTOR
                final long jitterMs =
                        (long) (delayMs * RECONNECT_JITTER_FACTOR * ThreadLocalRandom.current().nextDouble());
                final long totalDelayMs = delayMs + jitterMs;
                if (log.isInfoEnabled()) {
                    log.info("Bridge '{}' will attempt reconnection #{} in {} ms (delay: {} ms, jitter: {} ms)",
                            bridge.getId(), attempts + 1, totalDelayMs, delayMs, jitterMs);
                }
                reconnector.reconnect(true).delay(totalDelayMs, TimeUnit.MILLISECONDS);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Bridge '{}' disconnected by user, not attempting auto-reconnection", bridge.getId());
                }
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
                final KeyManagerFactory keyManagerFactory =
                        SslUtil.createKeyManagerFactory(requireNonNullElse(bridgeTls.getKeystoreType(),
                                        StoreTypeUtil.deductType(bridgeTls.getKeystorePath())),
                                bridgeTls.getKeystorePath(),
                                bridgeTls.getKeystorePassword(),
                                bridgeTls.getPrivateKeyPassword());
                sslConfigBuilder.keyManagerFactory(keyManagerFactory);
            }
            if (bridgeTls.getTruststorePath() != null) {
                final TrustManagerFactory trustManagerFactory = SslUtil.createTrustManagerFactory(requireNonNullElse(
                                bridgeTls.getTruststoreType(),
                                StoreTypeUtil.deductType(bridgeTls.getTruststorePath())),
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

    /**
     * Represents the current operation state of the bridge to handle concurrent start/stop operations.
     */
    private enum OperationState {
        IDLE,
        STARTING,
        STOPPING,
    }
}
