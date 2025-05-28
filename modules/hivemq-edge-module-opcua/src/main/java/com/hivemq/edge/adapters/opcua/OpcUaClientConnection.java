package com.hivemq.edge.adapters.opcua;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.edge.adapters.opcua.client.OpcUaClientConfigurator;
import com.hivemq.edge.adapters.opcua.client.ParsedConfig;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.southbound.JsonSchemaGenerator;
import com.hivemq.edge.adapters.opcua.southbound.JsonToOpcUAConverter;
import com.hivemq.edge.adapters.opcua.southbound.OpcUaPayload;
import com.hivemq.edge.adapters.opcua.northbound.OpcUaToJsonConverter;
import com.hivemq.edge.adapters.opcua.util.Bytes;
import com.hivemq.edge.adapters.opcua.util.result.Failure;
import com.hivemq.edge.adapters.opcua.util.result.Success;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.ServiceFaultListener;
import org.eclipse.milo.opcua.sdk.client.SessionActivityListener;
import org.eclipse.milo.opcua.sdk.client.UaSession;
import org.eclipse.milo.opcua.sdk.client.subscriptions.MonitoredItemSynchronizationException;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.encoding.EncodingContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.CONNECTED;
import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.DISCONNECTED;
import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.ERROR;
import static com.hivemq.edge.adapters.opcua.Constants.PROTOCOL_ID_OPCUA;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class OpcUaClientConnection {
    private static final @NotNull Logger log = LoggerFactory.getLogger(OpcUaClientConnection.class);

    public record OpcUaClientContext(OpcUaClient client, ServiceFaultListener serviceFaultListener, SessionActivityListener sessionActivityListener) {}

    private final @NotNull String uri;
    private final @NotNull List<OpcuaTag> tags;
    private final @NotNull ProtocolAdapterTagStreamingService tagStreamingService;
    private final @NotNull DataPointFactory dataPointFactory;
    private final @NotNull EventService eventService;
    private final @NotNull String adapterId;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull ReentrantLock ioLock = new ReentrantLock();
    private final @NotNull OpcUaSpecificAdapterConfig config;
    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;

    private volatile OpcUaClientContext opcUaClientInstance;


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
    }

    public @NotNull CompletableFuture<Void> start() {
        ioLock.lock();
        try {
            final var result = ParsedConfig.fromConfig(config);
            final ParsedConfig parsedConfig;

            //this gets a lot nice with Java 21 and pattern matching.
            if (result instanceof final Failure<ParsedConfig, String> failure) {
                log.error("Failed to parse configuration for OPC UA client: {}", failure.failure());
                return CompletableFuture.failedFuture(new IllegalArgumentException(failure.failure()));
            } else if (result instanceof final Success<ParsedConfig, String> success) {
                parsedConfig = success.result();
            } else {
                throw new IllegalStateException("Unexpected result type: " + result.getClass().getName());
            }

            final var configurator = new OpcUaClientConfigurator(adapterId, parsedConfig);

            final OpcUaClient newOcpUaClient;
            try {
                newOcpUaClient = OpcUaClient.create(
                    uri,
                    endpoints -> endpoints.stream().findFirst(),
                    transportConfigBuilder -> {},
                    configurator);
            } catch (final UaException e) {
                log.error("Unable to create OpcUaClient", e);
                return CompletableFuture.failedFuture(e);
            }

            return newOcpUaClient
                    .connectAsync()
                    .thenCompose(same_as_newOcpUaClient -> {
                        log.info("Client created and connected successfully");
                        return createSubscription(newOcpUaClient);
                    })
                    .thenApply(ignored -> (Void)null)
                    .whenComplete((ignored, throwable) -> {
                        if(throwable != null) {
                            log.error("Failed to create subscription", throwable);
                            protocolAdapterState.setConnectionStatus(ERROR);
                            try {
                                newOcpUaClient.disconnect();
                            } catch (final UaException e) {
                                log.error("Failed to disconnect client", e);
                            }
                        } else {
                            log.info("Subscription created successfully");

                            final var clientContext = new OpcUaClientContext(
                                    newOcpUaClient,
                                    createServiceFaultListener(),
                                    createSessionActivityListener());
                            newOcpUaClient.addSessionActivityListener(clientContext.sessionActivityListener());
                            newOcpUaClient.addFaultListener(clientContext.serviceFaultListener());
                            opcUaClientInstance = clientContext;
                            protocolAdapterState.setConnectionStatus(CONNECTED);

                            try {
                                Thread.sleep(1_000);
                            } catch (final InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
        } finally {
            ioLock.unlock();
        }
    }

    public @NotNull CompletableFuture<Void> stop() {
        ioLock.lock();
        try {
            final var opcUaClientInstanceToBeClosed = opcUaClientInstance;
            if (opcUaClientInstance != null) {
                opcUaClientInstance = null;
                opcUaClientInstanceToBeClosed.client().removeSessionActivityListener(opcUaClientInstanceToBeClosed.sessionActivityListener());
                opcUaClientInstanceToBeClosed.client().removeFaultListener(opcUaClientInstanceToBeClosed.serviceFaultListener());
                return opcUaClientInstanceToBeClosed
                        .client()
                        .disconnectAsync()
                        .thenApply(ignored -> {
                            protocolAdapterState.setConnectionStatus(DISCONNECTED);
                            return null;
                        });
            } else {
                return CompletableFuture.completedFuture(null);
            }
        } finally {
            ioLock.unlock();
        }
    }

    public boolean isStarted() {
        return opcUaClientInstance != null;
    }

    public CompletableFuture<List<StatusCode>> write(
            final @NotNull OpcuaTag opcuaTag,
            final @NotNull OpcUaPayload opcUAWritePayload
    ) {

        final JsonToOpcUAConverter converter = new JsonToOpcUAConverter(opcUaClientInstance.client());

        log.debug("Write for opcua is invoked with payload '{}' for tag '{}' ", opcUAWritePayload, opcuaTag.getName());

        final NodeId nodeId = NodeId.parse(opcuaTag.getDefinition().getNode());
        final Object opcuaObject = converter.convertToOpcUAValue(opcUAWritePayload.value(), nodeId);

        final Variant variant = Variant.of(opcuaObject);
        final DataValue dataValue = new DataValue(variant, StatusCode.GOOD, null);

        System.out.println(dataValue);
        return opcUaClientInstance
                    .client()
                    .writeValuesAsync(List.of(nodeId), List.of(dataValue));
    }

    private @NotNull CompletionStage<Object> createSubscription(final @NotNull OpcUaClient client) {
        final var nodeIdToTag = tags.stream()
                .collect(
                        Collectors.toMap(
                                tag -> NodeId.parse(tag.getDefinition().getNode()),
                                tag -> tag));

        final Map<OpcuaTag, Boolean> tagToFirstSeen = new ConcurrentHashMap<>();

        final var subscription = new OpcUaSubscription(client);
        subscription.setPublishingInterval((double)config.getOpcuaToMqttConfig().getPublishingInterval());
        subscription.setSubscriptionListener(createSubscriptionListener(client, nodeIdToTag, tagToFirstSeen));
        //Important: Monitored items must be created after the subscription was already created,
        // otherwise the client goes into an error state
        return subscription
                .createAsync()
                .thenCompose(ignored -> createMonitoredItems(subscription));
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
            final @NotNull OpcUaClient client,
            final @NotNull Map<NodeId, @NotNull OpcuaTag> nodeIdToTag,
            final @NotNull Map<OpcuaTag, Boolean> tagToFirstSeen) {
        return new OpcUaSubscription.SubscriptionListener() {
            @Override
            public void onKeepAliveReceived(final OpcUaSubscription subscription) {
                protocolAdapterMetricsService.increment(Constants.METRIC_SUBSCRIPTION_KEEPALIVE_COUNT);
                OpcUaSubscription.SubscriptionListener.super.onKeepAliveReceived(subscription);
            }

            @Override
            public void onTransferFailed(final OpcUaSubscription subscription, final StatusCode status) {
                protocolAdapterMetricsService.increment(Constants.METRIC_SUBSCRIPTION_TRANSFER_FAILED_COUNT);
                OpcUaSubscription.SubscriptionListener.super.onTransferFailed(subscription, status);
            }

            @Override
            public void onDataReceived(
                    final OpcUaSubscription subscription,
                    final List<OpcUaMonitoredItem> items,
                    final List<DataValue> values) {
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
                        final var convertedPayload =
                                new String(convertPayload(value, client.getDynamicEncodingContext()));
                        tagStreamingService.feed(tag.getName(),
                                List.of(dataPointFactory.createJsonDataPoint(tag.getName(), convertedPayload)));
                    } catch (final UaException e) {
                        protocolAdapterMetricsService.increment(Constants.METRIC_SUBSCRIPTION_DATA_ERROR_COUNT);
                        throw new RuntimeException(e);
                    }
                }
            }
        };
    }

    public CompletableFuture<List<OpcUaNodeDiscovery.CollectedNode>> discoverValues(
            final @NotNull String rootNode,
            final int depth) {
        return OpcUaNodeDiscovery.discoverValues(opcUaClientInstance.client(), rootNode, depth);
    }

    public CompletableFuture<Optional<JsonNode>> createTagSchema(final OpcuaTag tag) {
        return new JsonSchemaGenerator(opcUaClientInstance.client(), new ObjectMapper()).createMqttPayloadJsonSchema(tag);
    }

    private static byte @NotNull [] convertPayload(
            final @NotNull DataValue dataValue,
            final @NotNull EncodingContext serializationContext) {
        //null value, empty buffer
        if (dataValue.getValue().getValue() == null) {
            return Constants.EMTPY_BYTES;
        }
        return Bytes.fromReadOnlyBuffer(OpcUaToJsonConverter.convertPayload(serializationContext, dataValue));
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
                        .withPayload(session.getSessionName() + "/" + session.getSessionId())
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
}
