package com.hivemq.edge.adapters.opcua;

import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
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
import com.hivemq.edge.adapters.opcua.southbound.JsonSchemaGenerator;
import com.hivemq.edge.adapters.opcua.southbound.JsonToOpcUAConverter;
import com.hivemq.edge.adapters.opcua.southbound.OpcUaPayload;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.ServiceFaultListener;
import org.eclipse.milo.opcua.sdk.client.SessionActivityListener;
import org.eclipse.milo.opcua.sdk.client.UaSession;
import org.eclipse.milo.opcua.sdk.client.subscriptions.MonitoredItemSynchronizationException;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.CONNECTED;
import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.DISCONNECTED;
import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.ERROR;
import static com.hivemq.edge.adapters.opcua.Constants.PROTOCOL_ID_OPCUA;
import static java.util.Objects.requireNonNull;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class OpcUaClientConnection {
    private static final @NotNull Logger log = LoggerFactory.getLogger(OpcUaClientConnection.class);

    private final @NotNull String uri;
    private final @NotNull List<OpcuaTag> tags;
    private final @NotNull ProtocolAdapterTagStreamingService tagStreamingService;
    private final @NotNull DataPointFactory dataPointFactory;
    private final @NotNull EventService eventService;
    private final @NotNull String adapterId;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull AtomicBoolean started;
    private final @NotNull OpcUaSpecificAdapterConfig config;
    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;

    private volatile @Nullable OpcUaClientContext opcUaClientInstance;

    public OpcUaClientConnection(
            final @NotNull String uri,
            final @NotNull List<OpcuaTag> tags,
            final @NotNull OpcUaSpecificAdapterConfig config,
            final @NotNull ProtocolAdapterTagStreamingService tagStreamingService,
            final @NotNull DataPointFactory dataPointFactory,
            final @NotNull EventService eventService,
            final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService,
            final @NotNull String adapterId,
            final @NotNull ProtocolAdapterState protocolAdapterState) {
        this.uri = uri;
        this.tags = tags;
        this.config = config;
        this.tagStreamingService = tagStreamingService;
        this.dataPointFactory = dataPointFactory;
        this.eventService = eventService;
        this.adapterId = adapterId;
        this.protocolAdapterState = protocolAdapterState;
        this.protocolAdapterMetricsService = protocolAdapterMetricsService;
        this.started = new AtomicBoolean(false);
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

    public @NotNull CompletableFuture<Void> start() {
        if (started.compareAndSet(false, true)) {
            final ParsedConfig parsedConfig;
            final var result = ParsedConfig.fromConfig(config);
            if (result instanceof final Failure<ParsedConfig, String> failure) {
                log.error("Failed to parse configuration for OPC UA client: {}", failure.failure());
                started.set(false); // release lock
                return CompletableFuture.failedFuture(new IllegalArgumentException(failure.failure()));
            } else if (result instanceof final Success<ParsedConfig, String> success) {
                parsedConfig = success.result();
            } else {
                started.set(false); // release lock
                throw new IllegalStateException("Unexpected result type: " + result.getClass().getName());
            }

            final OpcUaClient newOcpUaClient;
            try {
                newOcpUaClient = OpcUaClient.create(uri,
                        endpoints -> endpoints.stream().findFirst(),
                        transportConfigBuilder -> {},
                        new OpcUaClientConfigurator(adapterId, parsedConfig));
            } catch (final UaException e) {
                log.error("Unable to create OpcUaClient", e);
                protocolAdapterState.setConnectionStatus(ERROR);
                started.set(false); // release lock
                return CompletableFuture.failedFuture(e);
            }

            return newOcpUaClient.connectAsync().thenCompose(same_as_newOcpUaClient -> {
                log.info("Client created and connected successfully");
                return createSubscription(newOcpUaClient);
            }).thenApply(ignored -> (Void) null).whenComplete((ignored, throwable) -> {
                if (throwable != null) {
                    log.error("Failed to create subscription", throwable);
                    protocolAdapterState.setConnectionStatus(ERROR);
                    try {
                        newOcpUaClient.disconnect();
                    } catch (final UaException e) {
                        log.error("Failed to disconnect client", e);
                    } finally {
                        started.set(false); // release lock
                    }
                } else {
                    log.info("Subscription created successfully");
                    final ServiceFaultListener faultListener = createServiceFaultListener();
                    final SessionActivityListener activityListener = createSessionActivityListener();
                    newOcpUaClient.addSessionActivityListener(activityListener);
                    newOcpUaClient.addFaultListener(faultListener);
                    opcUaClientInstance = new OpcUaClientContext(newOcpUaClient, faultListener, activityListener);
                    protocolAdapterState.setConnectionStatus(CONNECTED);
                }
            });
        } else {
            log.warn("Start operation is already in progress or has been completed.");
            return CompletableFuture.completedFuture(null);
        }
    }

    public @NotNull CompletableFuture<Void> stop() {
        if (started.compareAndSet(true, false)) {
            final var toBeClosed = opcUaClientInstance;
            if (toBeClosed != null) {
                opcUaClientInstance = null;
                toBeClosed.client().removeSessionActivityListener(toBeClosed.sessionActivityListener());
                toBeClosed.client().removeFaultListener(toBeClosed.serviceFaultListener());
                return toBeClosed.client().disconnectAsync().thenApply(ignored -> {
                    protocolAdapterState.setConnectionStatus(DISCONNECTED);
                    return null;
                });
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    public boolean isStarted() {
        return started.get();
    }

    public @NotNull CompletableFuture<List<StatusCode>> write(
            final @NotNull OpcuaTag opcuaTag,
            final @NotNull OpcUaPayload opcUAWritePayload) {
        final OpcUaClientContext instance = opcUaClientInstance;
        if (started.get() && instance != null) {
            final JsonToOpcUAConverter converter = new JsonToOpcUAConverter(instance.client());
            if (log.isDebugEnabled()) {
                log.debug("Write for opcua is invoked with payload '{}' for tag '{}' ",
                        opcUAWritePayload,
                        opcuaTag.getName());
            }
            final NodeId nodeId = NodeId.parse(opcuaTag.getDefinition().getNode());
            final Object opcuaObject = converter.convertToOpcUAValue(opcUAWritePayload.value(), nodeId);
            return instance.client()
                    .writeValuesAsync(List.of(nodeId),
                            List.of(new DataValue(Variant.of(opcuaObject), StatusCode.GOOD, null)));
        }
        log.error("OPC UA client instance is not initialized. Call start() first.");
        return CompletableFuture.failedFuture(new IllegalStateException("OPC UA client instance is not initialized."));
    }

    private @NotNull CompletionStage<Object> createSubscription(final @NotNull OpcUaClient client) {
        final var subscription = new OpcUaSubscription(client);
        subscription.setPublishingInterval((double) config.getOpcuaToMqttConfig().getPublishingInterval());
        subscription.setSubscriptionListener(createSubscriptionListener(client));
        // Important: Monitored items must be created after the subscription was already created,
        // otherwise the client goes into an error state
        return subscription.createAsync().thenCompose(ignored -> createMonitoredItems(subscription));
    }

    private @NotNull CompletableFuture<Object> createMonitoredItems(final @NotNull OpcUaSubscription subscription) {
        tags.forEach(opcuaTag -> {
            final String nodeId = opcuaTag.getDefinition().getNode();
            final var monitoredItem = OpcUaMonitoredItem.newDataItem(NodeId.parse(nodeId));
            monitoredItem.setQueueSize(uint(config.getOpcuaToMqttConfig().getServerQueueSize()));
            monitoredItem.setSamplingInterval(config.getOpcuaToMqttConfig().getPublishingInterval());
            subscription.addMonitoredItem(monitoredItem);
        });

        try {
            subscription.synchronizeMonitoredItems();
            return CompletableFuture.completedFuture(null);
        } catch (final MonitoredItemSynchronizationException ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }

    private OpcUaSubscription.@NotNull SubscriptionListener createSubscriptionListener(
            final @NotNull OpcUaClient client) {
        final var nodeIdToTag =
                tags.stream().collect(Collectors.toMap(tag -> NodeId.parse(tag.getDefinition().getNode()), tag -> tag));
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
                    if (null == tagToFirstSeen.putIfAbsent(tag, true)) {
                        eventService.createAdapterEvent(adapterId, PROTOCOL_ID_OPCUA)
                                .withSeverity(Event.SEVERITY.INFO)
                                .withMessage(String.format("Adapter '%s' took first sample for tag '%s'",
                                        adapterId,
                                        tag.getName()))
                                .fire();
                    }
                    final var value = values.get(i);
                    try {
                        protocolAdapterMetricsService.increment(Constants.METRIC_SUBSCRIPTION_DATA_RECEIVED_COUNT);
                        tagStreamingService.feed(tag.getName(),
                                List.of(dataPointFactory.createJsonDataPoint(tag.getName(),
                                        extractPayload(client, value))));
                    } catch (final UaException e) {
                        protocolAdapterMetricsService.increment(Constants.METRIC_SUBSCRIPTION_DATA_ERROR_COUNT);
                        throw new RuntimeException(e);
                    }
                }
            }
        };
    }

    public @NotNull CompletableFuture<List<OpcUaNodeDiscovery.CollectedNode>> discoverValues(
            final @NotNull String rootNode,
            final int depth) {
        return OpcUaNodeDiscovery.discoverValues(requireNonNull(opcUaClientInstance).client(), rootNode, depth);
    }

    public @NotNull CompletableFuture<Optional<JsonNode>> createTagSchema(final @NotNull OpcuaTag tag) {
        return new JsonSchemaGenerator(requireNonNull(opcUaClientInstance).client()).createMqttPayloadJsonSchema(tag);
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
                log.info("OPC UA client of protocol adapter '{}' disconnected: {}", adapterId, session);
                eventService.createAdapterEvent(adapterId, PROTOCOL_ID_OPCUA)
                        .withSeverity(Event.SEVERITY.WARN)
                        .withPayload(session.getSessionName() + '/' + session.getSessionId())
                        .withMessage("Adapter '" + adapterId + "' session has been disconnected.")
                        .fire();
                protocolAdapterState.setConnectionStatus(DISCONNECTED);
            }

            @Override
            public void onSessionActive(final @NotNull UaSession session) {
                protocolAdapterMetricsService.increment(Constants.METRIC_SESSION_ACTIVE_COUNT);
                log.info("OPC UA client of protocol adapter '{}' connected: {}", adapterId, session);
                protocolAdapterState.setConnectionStatus(CONNECTED);
            }
        };
    }

    public record OpcUaClientContext(OpcUaClient client, ServiceFaultListener serviceFaultListener,
                                     SessionActivityListener sessionActivityListener) {
    }
}
