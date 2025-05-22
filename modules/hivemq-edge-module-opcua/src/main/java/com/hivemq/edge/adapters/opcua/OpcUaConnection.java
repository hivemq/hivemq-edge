package com.hivemq.edge.adapters.opcua;

import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.mqtt2opcua.JsonSchemaGenerator;
import com.hivemq.edge.adapters.opcua.mqtt2opcua.JsonToOpcUAConverter;
import com.hivemq.edge.adapters.opcua.mqtt2opcua.OpcUaPayload;
import com.hivemq.edge.adapters.opcua.opcua2mqtt.OpcUaJsonPayloadConverter;
import com.hivemq.edge.adapters.opcua.util.Bytes;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.subscriptions.MonitoredItemSynchronizationException;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.encoding.EncodingContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.CONNECTED;
import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.ERROR;
import static com.hivemq.edge.adapters.opcua.OpcUaProtocolAdapterInformation.PROTOCOL_ID;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class OpcUaConnection {
    private static final Logger log = LoggerFactory.getLogger(OpcUaConnection.class);

    public static final byte[] EMTPY_BYTES = new byte[]{};

    private final @NotNull String uri;
    private final @NotNull List<OpcuaTag> tags;
    private final @NotNull OpcUaToMqttConfig toMqttConfig;
    private final @NotNull ProtocolAdapterTagStreamingService tagStreamingService;
    private final @NotNull DataPointFactory dataPointFactory;
    private final @NotNull EventService eventService;
    private final @NotNull String adapterId;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull ReentrantLock ioLock = new ReentrantLock();

    private volatile OpcUaClient opcUaClientInstance;

    public OpcUaConnection(
            final @NotNull String uri,
            final @NotNull List<OpcuaTag> tags,
            final @NotNull OpcUaToMqttConfig toMqttConfig,
            final @NotNull ProtocolAdapterTagStreamingService tagStreamingService,
            final @NotNull DataPointFactory dataPointFactory,
            final @NotNull EventService eventService,
            final @NotNull String adapterId,
            final @NotNull ProtocolAdapterState protocolAdapterState) {
        this.uri = uri;
        this.tags = tags;
        this.toMqttConfig = toMqttConfig;
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
                clientConfigBuilder ->
                        //TODO MUST BE CONFIGURABLE!!!!!!!!!
                        clientConfigBuilder
                                .setApplicationName(LocalizedText.english("eclipse milo opc-ua client"))
                                .setApplicationUri("urn:eclipse:milo:examples:client"));
            } catch (final UaException e) {
                log.error("Unable to create OpcUaClient", e);
                return CompletableFuture.failedFuture(e);
            }

            return newOcpUaClient
                    .connectAsync()
                    .thenCompose(client -> {
                        log.info("Client created and connected successfully");
                        return createSubscription(client);
                    })
                    .thenCompose(ignored -> {
                        log.info("Subscription created successfully");
                        return CompletableFuture.completedFuture((Void)null);
                    })
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
                            opcUaClientInstance = newOcpUaClient;
                            protocolAdapterState.setConnectionStatus(CONNECTED);
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
                return opcUaClientInstanceToBeClosed
                        .disconnectAsync()
                        .thenApply(ignored -> null);
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
            converter = new JsonToOpcUAConverter(opcUaClientInstance);
        } catch (UaException e) {
            log.error("Failed creating JsonToOpcUAConverter", e);
            return CompletableFuture.failedFuture(e);
        }

        log.debug("Write for opcua is invoked with payload '{}' for tag '{}' ", opcUAWritePayload, opcuaTag.getName());

        final NodeId nodeId = NodeId.parse(opcuaTag.getDefinition().getNode());
        final Object opcuaObject = converter.convertToOpcUAValue(opcUAWritePayload.getValue(), nodeId);
        final Variant variant = new Variant(opcuaObject);
        final DataValue dataValue = new DataValue(variant, null, null);
        return opcUaClientInstance
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
        subscription.setPublishingInterval((double)toMqttConfig.getPublishingInterval());
        subscription.setSubscriptionListener(createSubscriptionListener(client, nodeIdToTag, tagToFirstSeen));
        return subscription.createAsync().thenCompose(ignored -> createMonitoredItems(subscription));
    }

    private @NotNull CompletableFuture<Object> createMonitoredItems(final @NotNull OpcUaSubscription subscription) {
        tags.forEach(opcuaTag -> {
            final String nodeId = opcuaTag.getDefinition().getNode();
            var monitoredItem = OpcUaMonitoredItem.newDataItem(NodeId.parse(nodeId));
            monitoredItem.setQueueSize(uint(toMqttConfig.getServerQueueSize()));
            monitoredItem.setSamplingInterval(toMqttConfig.getPublishingInterval());
            subscription.addMonitoredItem(monitoredItem);
        });

        try {
            subscription.synchronizeMonitoredItems();
            return CompletableFuture.completedFuture(null);
        } catch (MonitoredItemSynchronizationException ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }

    private OpcUaSubscription.@NotNull SubscriptionListener createSubscriptionListener(
            final @NotNull OpcUaClient client,
            final @NotNull Map<NodeId, @NotNull OpcuaTag> nodeIdToTag,
            final @NotNull Map<OpcuaTag, Boolean> tagToFirstSeen) {
        return new OpcUaSubscription.SubscriptionListener() {
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
        return OpcUaBrowser.discoverValues(opcUaClientInstance, rootNode, depth);
    }

    public CompletableFuture<JsonSchemaGenerator.Result> createTagScheam(OpcuaTag tag) {
        return JsonSchemaGenerator.createMqttPayloadJsonSchema(opcUaClientInstance, tag);
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
}
