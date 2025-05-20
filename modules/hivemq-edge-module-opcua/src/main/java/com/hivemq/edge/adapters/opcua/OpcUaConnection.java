package com.hivemq.edge.adapters.opcua;

import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.edge.adapters.opcua.client.OpcUaClientConfigurator;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.mqtt2opcua.JsonSchemaGenerator;
import com.hivemq.edge.adapters.opcua.mqtt2opcua.JsonToOpcUAConverter;
import com.hivemq.edge.adapters.opcua.mqtt2opcua.OpcUaPayload;
import com.hivemq.edge.adapters.opcua.opcua2mqtt.OpcUaJsonPayloadConverter;
import com.hivemq.edge.adapters.opcua.util.Bytes;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.ServiceFaultListener;
import org.eclipse.milo.opcua.sdk.client.SessionActivityListener;
import org.eclipse.milo.opcua.sdk.client.UaSession;
import org.eclipse.milo.opcua.sdk.client.dtd.LegacyDataTypeManagerInitializer;
import org.eclipse.milo.opcua.sdk.client.subscriptions.MonitoredItemSynchronizationException;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.encoding.EncodingContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.WriteValue;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
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
import static com.hivemq.edge.adapters.opcua.OpcUaProtocolAdapterInformation.PROTOCOL_ID;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class OpcUaConnection {
    private static final Logger log = LoggerFactory.getLogger(OpcUaConnection.class);

    public static final byte[] EMTPY_BYTES = new byte[]{};

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

    private volatile OpcUaClientContext opcUaClientInstance;


    public OpcUaConnection(
            final @NotNull String uri,
            final @NotNull List<OpcuaTag> tags,
            final @NotNull OpcUaSpecificAdapterConfig config,
            final @NotNull ProtocolAdapterTagStreamingService tagStreamingService,
            final @NotNull DataPointFactory dataPointFactory,
            final @NotNull EventService eventService,
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
    }

    public @NotNull CompletableFuture<Void> start() {
        ioLock.lock();
        try {
            final OpcUaClient newOcpUaClient;
            try {
                newOcpUaClient = OpcUaClient.create(
                uri,
                endpoints -> endpoints.stream().findFirst(),
                transportConfigBuilder -> {},
                        new OpcUaClientConfigurator(config, adapterId));
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
                            newOcpUaClient.setDataTypeManagerInitializer(new LegacyDataTypeManagerInitializer(newOcpUaClient));
                            newOcpUaClient.addSessionActivityListener(clientContext.sessionActivityListener());
                            newOcpUaClient.addFaultListener(clientContext.serviceFaultListener());
                            opcUaClientInstance = clientContext;
                            protocolAdapterState.setConnectionStatus(CONNECTED);

                            try {
                                Thread.sleep(1_000);
                            } catch (InterruptedException e) {
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
            if (opcUaClientInstance != null) {
                final var opcUaClientInstanceToBeClosed = opcUaClientInstance;
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

        final JsonToOpcUAConverter converter;
        try {
            converter = new JsonToOpcUAConverter(opcUaClientInstance.client());
        } catch (UaException e) {
            log.error("Failed creating JsonToOpcUAConverter", e);
            return CompletableFuture.failedFuture(e);
        }

        log.debug("Write for opcua is invoked with payload '{}' for tag '{}' ", opcUAWritePayload, opcuaTag.getName());

        final NodeId nodeId = NodeId.parse(opcuaTag.getDefinition().getNode());
        final Object opcuaObject = converter.convertToOpcUAValue(opcUAWritePayload.getValue(), nodeId);
        final Variant variant = Variant.of(opcuaObject);
        final DataValue dataValue = new DataValue(variant);

        return opcUaClientInstance
                    .client()
                    .writeValuesAsync(List.of(nodeId), List.of(dataValue))
                    .thenApply(res -> {
                        res.forEach(result->{
                            //TODO handle status isBad!!
                            System.out.println(result);
                        });
                        return  res;
                    });
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
                //TODO add metrics: protocolAdapterMetricsService.increment("subscription.keepalive.count");
                OpcUaSubscription.SubscriptionListener.super.onKeepAliveReceived(subscription);
            }

            @Override
            public void onTransferFailed(final OpcUaSubscription subscription, final StatusCode status) {
                //TODO add metrics protocolAdapterMetricsService.increment("subscription.transfer.failed.count");
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
                        eventService.createAdapterEvent(adapterId, PROTOCOL_ID)
                                .withSeverity(Event.SEVERITY.INFO)
                                .withMessage(String.format("Adapter '%s' took first sample for tag '%s'",
                                        adapterId,
                                        tag.getName()))
                                .fire();
                    }
                    final var value = values.get(i);
                    try {
                        final var convertedPayload =
                                new String(convertPayload(value, client.getDynamicEncodingContext()));
                        tagStreamingService.feed(tag.getName(),
                                List.of(dataPointFactory.createJsonDataPoint(tag.getName(), convertedPayload)));
                    } catch (UaException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
    }

    public CompletableFuture<List<OpcUaBrowser.CollectedNode>> discoverValues(
            final @NotNull String rootNode,
            final int depth) {
        return OpcUaBrowser.discoverValues(opcUaClientInstance.client(), rootNode, depth);
    }

    public CompletableFuture<Optional<JsonNode>> createTagScheam(final OpcuaTag tag) {
        return JsonSchemaGenerator.createMqttPayloadJsonSchema(opcUaClientInstance.client(), tag);
    }

    private static byte @NotNull [] convertPayload(
            final @NotNull DataValue dataValue,
            final @NotNull EncodingContext serializationContext) {
        //null value, emtpy buffer
        if (dataValue.getValue().getValue() == null) {
            return EMTPY_BYTES;
        }
        return Bytes.fromReadOnlyBuffer(OpcUaJsonPayloadConverter.convertPayload(serializationContext, dataValue));
    }

    private @NotNull ServiceFaultListener createServiceFaultListener() {
        return serviceFault -> {
            //Fault listeners previously also triggered on dosconnects!
            log.info("OPC UA client of protocol adapter '{}' detected a service fault: {}", adapterId, serviceFault);
            eventService.createAdapterEvent(adapterId, PROTOCOL_ID)
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
                log.info("OPC UA client of protocol adapter '{}' disconnected: {}", adapterId, session);
                eventService.createAdapterEvent(adapterId, PROTOCOL_ID)
                        .withSeverity(Event.SEVERITY.WARN)
                        .withPayload(session.getSessionName() + "/" + session.getSessionId())
                        .withMessage("Adapter '" + adapterId + "' session has been disconnected.")
                        .fire();
                protocolAdapterState.setConnectionStatus(DISCONNECTED);
            }

            @Override
            public void onSessionActive(final @NotNull UaSession session) {
                log.info("OPC UA client of protocol adapter '{}' connected: {}", adapterId, session);
                protocolAdapterState.setConnectionStatus(CONNECTED);
            }
        };
    }
}
