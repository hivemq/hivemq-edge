/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.opcua;

import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.edge.adapters.opcua.client.Failure;
import com.hivemq.edge.adapters.opcua.client.OpcUaClientConfigurator;
import com.hivemq.edge.adapters.opcua.client.ParsedConfig;
import com.hivemq.edge.adapters.opcua.client.Success;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.northbound.OpcUaToJsonConverter;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.ServiceFaultListener;
import org.eclipse.milo.opcua.sdk.client.SessionActivityListener;
import org.eclipse.milo.opcua.sdk.client.UaSession;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.transport.client.tcp.OpcTcpClientTransportConfigBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.CONNECTED;
import static com.hivemq.edge.adapters.opcua.Constants.PROTOCOL_ID_OPCUA;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

class OpcUaClientConnection {
    private static final @NotNull Logger log = LoggerFactory.getLogger(OpcUaClientConnection.class);
    private static final long STOP_WAIT_MILLIS = 30 * 1000;
    private static final long STOP_POLL_MILLIS = 100;
    private static final @NotNull Consumer<OpcTcpClientTransportConfigBuilder> TRANSPORT_CONFIG = ignore -> {
    };

    private final @NotNull OpcUaSpecificAdapterConfig config;
    private final @NotNull List<OpcuaTag> tags;
    private final @NotNull ProtocolAdapterTagStreamingService tagStreamingService;
    private final @NotNull DataPointFactory dataPointFactory;
    private final @NotNull EventService eventService;
    private final @NotNull String adapterId;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;
    private final @NotNull AtomicReference<ConnectionContext> context;

    OpcUaClientConnection(
            final @NotNull String adapterId,
            final @NotNull ProtocolAdapterInput<OpcUaSpecificAdapterConfig> input,
            final @NotNull List<OpcuaTag> tags,
            final @NotNull ProtocolAdapterState protocolAdapterState) {
        this.config = input.getConfig();
        this.tagStreamingService = input.moduleServices().protocolAdapterTagStreamingService();
        this.dataPointFactory = input.adapterFactories().dataPointFactory();
        this.eventService = input.moduleServices().eventService();
        this.protocolAdapterMetricsService = input.getProtocolAdapterMetricsHelper();
        this.adapterId = adapterId;
        this.protocolAdapterState = protocolAdapterState;
        this.tags = tags;
        this.context = new AtomicReference<>();
    }

    private static void quietlyDeleteSubscription(
            final @NotNull OpcUaClient client,
            final @Nullable OpcUaSubscription subscription) {
        if (subscription != null) {
            try {
                subscription.delete();
            } catch (final Exception e) {
                log.warn("Failed to delete subscription {}: {}", subscription, e.getMessage());
            }
            try {
                client.removeSubscription(subscription);
            } catch (final Exception e) {
                log.warn("Failed to remove subscription {}: {}", subscription, e.getMessage());
            }
        }
    }

    private static void quietlyCloseClient(
            final @NotNull OpcUaClient client,
            final @Nullable OpcUaSubscription subscription,
            final @Nullable ServiceFaultListener faultListener,
            final @Nullable SessionActivityListener activityListener) {

        quietlyDeleteSubscription(client, subscription);
        if (faultListener != null) {
            try {
                client.removeFaultListener(faultListener);
            } catch (final Throwable e) {
                log.warn("Failed to remove fault listener {}: {}", faultListener, e.getMessage());
            }
        }
        if (activityListener != null) {
            try {
                client.removeSessionActivityListener(activityListener);
            } catch (final Throwable e) {
                log.warn("Failed to remove session activity listener {}: {}", activityListener, e.getMessage());
            }
        }

        client.disconnectAsync().exceptionally(throwable -> {
            log.warn("Failed to disconnect: {}", throwable.getMessage());
            return null;
        });
    }

    private static void awaitOnCounter(final @NotNull AtomicLong requestCounter) {
        final long startTime = System.currentTimeMillis();
        while (requestCounter.get() > 0 && (System.currentTimeMillis() - startTime) < STOP_WAIT_MILLIS) {
            try {
                TimeUnit.MILLISECONDS.sleep(STOP_POLL_MILLIS);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private static @NotNull String extractPayload(final @NotNull OpcUaClient client, final @NotNull DataValue value)
            throws UaException {
        if (value.getValue().getValue() == null) {
            return "";
        }

        final ByteBuffer byteBuffer = OpcUaToJsonConverter.convertPayload(client.getDynamicEncodingContext(), value);
        final byte[] buffer = new byte[byteBuffer.remaining()];
        byteBuffer.get(buffer);
        return new String(buffer, StandardCharsets.UTF_8);
    }

    @NotNull CompletableFuture<Void> start() {
        if (context.get() != null) {
            return CompletableFuture.completedFuture(null);
        }

        final ParsedConfig parsedConfig;
        final var result = ParsedConfig.fromConfig(config);
        if (result instanceof final Failure<ParsedConfig, String> failure) {
            log.error("Failed to parse configuration for OPC UA client: {}", failure.failure());
            return CompletableFuture.failedFuture(new IllegalStateException(failure.failure()));
        } else if (result instanceof final Success<ParsedConfig, String> success) {
            parsedConfig = success.result();
        } else {
            return CompletableFuture.failedFuture(new IllegalStateException("Unexpected result type: " +
                    result.getClass().getName()));
        }

        final OpcUaClient client;
        try {
            client = OpcUaClient.create(config.getUri(),
                    endpoints -> endpoints.stream()
                                .filter(e -> {
                                    final var requiredPolicy = config.getSecurity().policy().getSecurityPolicy().getUri();
                                    return requiredPolicy.equals(e.getSecurityPolicyUri());
                                })
                                .findFirst(),
                    TRANSPORT_CONFIG,
                    new OpcUaClientConfigurator(adapterId, parsedConfig));
        } catch (final Throwable error) {
            return CompletableFuture.failedFuture(error);
        }

        return client.connectAsync().thenCompose(this::subscribe).whenComplete((subscription, throwable) -> {
            if (throwable == null) {
                final ServiceFaultListener faultListener = createServiceFaultListener();
                final SessionActivityListener activityListener = createSessionActivityListener();
                client.addSessionActivityListener(activityListener);
                client.addFaultListener(faultListener);
                final ConnectionContext newContext =
                        new ConnectionContext(client, subscription, faultListener, activityListener);
                if (context.compareAndSet(null, newContext)) {
                    log.info("Client created and connected successfully");
                } else {
                    log.warn("Concurrent start detected for adapter '{}'. Closing redundant OPC UA client connection.",
                            adapterId);
                    quietlyCloseClient(client,
                            subscription,
                            faultListener,
                            activityListener); // <-- THIS IS THE MISSING CLEANUP
                }
            } else {
                log.error("Failed to start OPC UA client", throwable);
                quietlyCloseClient(client, subscription, null, null);
            }
        }).thenApply(subscription -> null);
    }

    @NotNull CompletableFuture<Void> stop(final @NotNull AtomicLong requestCounter) {
        final ConnectionContext ctx = context.getAndSet(null);
        if (ctx == null) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(() -> awaitOnCounter(requestCounter)).thenCompose(v -> {
            quietlyCloseClient(ctx.client(), ctx.subscription(), ctx.faultListener(), ctx.activityListener());
            return CompletableFuture.completedFuture(null);
        });
    }

    @Nullable OpcUaClient client() {
        final ConnectionContext ctx = context.get();
        return ctx != null ? ctx.client() : null;
    }

    private @NotNull CompletionStage<OpcUaSubscription> subscribe(final @NotNull OpcUaClient client) {
        final OpcUaSubscription subscription = new OpcUaSubscription(client);
        subscription.setPublishingInterval((double) config.getOpcuaToMqttConfig().publishingInterval());
        subscription.setSubscriptionListener(createSubscriptionListener(client));
        return subscription.createAsync()
                .thenCompose(unit -> addAndSynchronizeMonitoredItems(subscription))
                .thenApply(sub -> subscription)
                .exceptionally(error -> {
                    log.error("Failed to create/synchronize OPC UA subscription/monitored items: {}",
                            error.getMessage(),
                            error);
                    quietlyDeleteSubscription(client, subscription);
                    throw new CompletionException(error);
                });
    }

    private @NotNull CompletableFuture<Void> addAndSynchronizeMonitoredItems(final @NotNull OpcUaSubscription subscription) {
        tags.forEach(opcuaTag -> {
            final String nodeId = opcuaTag.getDefinition().getNode();
            final var monitoredItem = OpcUaMonitoredItem.newDataItem(NodeId.parse(nodeId));
            monitoredItem.setQueueSize(uint(config.getOpcuaToMqttConfig().serverQueueSize()));
            monitoredItem.setSamplingInterval(config.getOpcuaToMqttConfig().publishingInterval());
            subscription.addMonitoredItem(monitoredItem);
            log.debug("Added monitored item: {}", nodeId);
        });

        return CompletableFuture.supplyAsync(() -> {
            try {
                subscription.synchronizeMonitoredItems();
                log.info("All monitored items synchronized successfully for adapter '{}'.", adapterId);
                return null;
            } catch (final Throwable e) {
                log.error("Failed to synchronize monitored items: {}", e.getMessage(), e);
                throw new CompletionException("Failed to synchronize monitored items", e);
            }
        });
    }

    private @NotNull ServiceFaultListener createServiceFaultListener() {
        return serviceFault -> {
            protocolAdapterMetricsService.increment(Constants.METRIC_SUBSCRIPTION_SERVICE_FAULT_COUNT);
            log.info("OPC UA client of protocol adapter '{}' detected a service fault: {}", adapterId, serviceFault);
            eventService.createAdapterEvent(adapterId, PROTOCOL_ID_OPCUA)
                    .withSeverity(Event.SEVERITY.ERROR)
                    .withPayload(serviceFault.getResponseHeader().getServiceResult())
                    .withMessage("A Service Fault was Detected.")
                    .fire();
        };
    }

    private @NotNull SessionActivityListener createSessionActivityListener() {
        return new SessionActivityListener() {
            @Override
            public void onSessionInactive(final @NotNull UaSession session) {
                protocolAdapterMetricsService.increment(Constants.METRIC_SESSION_INACTIVE_COUNT);
                eventService.createAdapterEvent(adapterId, PROTOCOL_ID_OPCUA)
                        .withSeverity(Event.SEVERITY.WARN)
                        .withPayload(session.getSessionName() + '/' + session.getSessionId())
                        .withMessage("Adapter '" + adapterId + "' session has been disconnected.")
                        .fire();
                protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
                log.info("OPC UA client of protocol adapter '{}' disconnected: {}", adapterId, session);
            }

            @Override
            public void onSessionActive(final @NotNull UaSession session) {
                protocolAdapterMetricsService.increment(Constants.METRIC_SESSION_ACTIVE_COUNT);
                protocolAdapterState.setConnectionStatus(CONNECTED);
                log.info("OPC UA client of protocol adapter '{}' connected: {}", adapterId, session);
            }
        };
    }

    private OpcUaSubscription.@NotNull SubscriptionListener createSubscriptionListener(
            final @NotNull OpcUaClient client) {
        final var nodeIdToTag = tags.stream()
                .collect(Collectors.toMap(tag -> NodeId.parse(tag.getDefinition().getNode()), Function.identity()));
        final Map<OpcuaTag, Boolean> tagToFirstSeen = new ConcurrentHashMap<>();
        return new OpcUaSubscription.SubscriptionListener() {
            @Override
            public void onKeepAliveReceived(final @NotNull OpcUaSubscription subscription) {
                protocolAdapterMetricsService.increment(Constants.METRIC_SUBSCRIPTION_KEEPALIVE_COUNT);
                OpcUaSubscription.SubscriptionListener.super.onKeepAliveReceived(subscription);
            }

            @Override
            public void onTransferFailed(
                    final @NotNull OpcUaSubscription subscription,
                    final @NotNull StatusCode status) {
                protocolAdapterMetricsService.increment(Constants.METRIC_SUBSCRIPTION_TRANSFER_FAILED_COUNT);
                OpcUaSubscription.SubscriptionListener.super.onTransferFailed(subscription, status);
            }

            @Override
            public void onDataReceived(
                    final @NotNull OpcUaSubscription subscription,
                    final @NotNull List<OpcUaMonitoredItem> items,
                    final @NotNull List<DataValue> values) {
                for (int i = 0; i < items.size(); i++) {
                    final var tag = nodeIdToTag.get(items.get(i).getReadValueId().getNodeId());
                    final String tn = tag.getName();
                    if (null == tagToFirstSeen.putIfAbsent(tag, true)) {
                        eventService.createAdapterEvent(adapterId, PROTOCOL_ID_OPCUA)
                                .withSeverity(Event.SEVERITY.INFO)
                                .withMessage(String.format("Adapter '%s' took first sample for tag '%s'",
                                        adapterId,
                                        tn))
                                .fire();
                    }
                    try {
                        protocolAdapterMetricsService.increment(Constants.METRIC_SUBSCRIPTION_DATA_RECEIVED_COUNT);
                        final String payload = extractPayload(client, values.get(i));
                        tagStreamingService.feed(tn, List.of(dataPointFactory.createJsonDataPoint(tn, payload)));
                    } catch (final Throwable e) {
                        protocolAdapterMetricsService.increment(Constants.METRIC_SUBSCRIPTION_DATA_ERROR_COUNT);
                        throw new RuntimeException(e);
                    }
                }
            }
        };
    }

    private record ConnectionContext(OpcUaClient client, OpcUaSubscription subscription,
                                     ServiceFaultListener faultListener, SessionActivityListener activityListener) {
    }
}
