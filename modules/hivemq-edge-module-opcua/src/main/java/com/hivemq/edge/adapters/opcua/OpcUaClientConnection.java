package com.hivemq.edge.adapters.opcua;

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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.CONNECTED;
import static com.hivemq.edge.adapters.opcua.Constants.PROTOCOL_ID_OPCUA;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

class OpcUaClientConnection {
    private static final @NotNull Logger log = LoggerFactory.getLogger(OpcUaClientConnection.class);

    private final @NotNull String uri;
    private final @NotNull List<OpcuaTag> tags;
    private final @NotNull ProtocolAdapterTagStreamingService tagStreamingService;
    private final @NotNull DataPointFactory dataPointFactory;
    private final @NotNull EventService eventService;
    private final @NotNull String adapterId;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull OpcUaSpecificAdapterConfig config;
    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;
    private volatile @Nullable OpcUaClientContext context;

    OpcUaClientConnection(
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

    void start() throws Throwable {
        if (context != null){
            return;
        }

        final ParsedConfig parsedConfig;
        final var result = ParsedConfig.fromConfig(config);
        if (result instanceof final Failure<ParsedConfig, String> failure) {
            log.error("Failed to parse configuration for OPC UA client: {}", failure.failure());
            throw new IllegalStateException(failure.failure());
        } else if (result instanceof final Success<ParsedConfig, String> success) {
            parsedConfig = success.result();
        } else {
            throw new IllegalStateException("Unexpected result type: " + result.getClass().getName());
        }

        final OpcUaClient client = OpcUaClient.create(uri,
                endpoints -> endpoints.stream().findFirst(),
                ignore -> {},
                new OpcUaClientConfigurator(adapterId, parsedConfig)).connect();
        log.info("Client created and connected successfully");

        final ServiceFaultListener faultListener = createServiceFaultListener();
        final SessionActivityListener activityListener = createSessionActivityListener();
        client.addSessionActivityListener(activityListener);
        client.addFaultListener(faultListener);

        final OpcUaSubscription subscription = new OpcUaSubscription(client);
        subscription.setPublishingInterval((double) config.getOpcuaToMqttConfig().getPublishingInterval());
        subscription.setSubscriptionListener(createSubscriptionListener(client));
        try {
            subscription.create();
            tags.forEach(opcuaTag -> {
                final String nodeId = opcuaTag.getDefinition().getNode();
                final var monitoredItem = OpcUaMonitoredItem.newDataItem(NodeId.parse(nodeId));
                monitoredItem.setQueueSize(uint(config.getOpcuaToMqttConfig().getServerQueueSize()));
                monitoredItem.setSamplingInterval(config.getOpcuaToMqttConfig().getPublishingInterval());
                subscription.addMonitoredItem(monitoredItem);
                log.info("Added monitored item: {}", nodeId);
            });
            subscription.synchronizeMonitoredItems();
            log.info("Subscription created successfully");
        } catch (final Throwable error) {
            try {
                log.error("Failed to start OPC UA client", error);
                subscription.delete();
                client.removeSessionActivityListener(activityListener);
                client.removeFaultListener(faultListener);
                client.removeSubscription(subscription);
                client.disconnect();
            } catch (final Throwable ignore) {
                // polite attempt to close, throw the existing exception
            }
            throw error;
        }

        context = new OpcUaClientContext(client, faultListener, activityListener, subscription);
    }

    void stop() throws Throwable {
        try {
            final OpcUaClientContext ctx = context;
            if (ctx != null) {
                ctx.disconnect();
            }
        } finally {
            context = null;
        }
    }

    @NotNull OpcUaClient client() {
        return Objects.requireNonNull(context).client();
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
                protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
            }

            @Override
            public void onSessionActive(final @NotNull UaSession session) {
                protocolAdapterMetricsService.increment(Constants.METRIC_SESSION_ACTIVE_COUNT);
                log.info("OPC UA client of protocol adapter '{}' connected: {}", adapterId, session);
                protocolAdapterState.setConnectionStatus(CONNECTED);
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
                        final var value = values.get(i);
                        protocolAdapterMetricsService.increment(Constants.METRIC_SUBSCRIPTION_DATA_RECEIVED_COUNT);

                        final String payload;
                        if (value.getValue().getValue() == null) {
                            payload = "";
                        } else {
                            final ByteBuffer byteBuffer =
                                    OpcUaToJsonConverter.convertPayload(client.getDynamicEncodingContext(), value);
                            final byte[] buffer = new byte[byteBuffer.remaining()];
                            byteBuffer.get(buffer);
                            payload = new String(buffer, StandardCharsets.UTF_8);
                        }

                        tagStreamingService.feed(tn, List.of(dataPointFactory.createJsonDataPoint(tn, payload)));
                    } catch (final UaException e) {
                        protocolAdapterMetricsService.increment(Constants.METRIC_SUBSCRIPTION_DATA_ERROR_COUNT);
                        throw new RuntimeException(e);
                    }
                }
            }
        };
    }


    private record OpcUaClientContext(OpcUaClient client, ServiceFaultListener faultListener,
                                      SessionActivityListener activityListener, OpcUaSubscription subscription) {
        private void disconnect() throws Throwable {
            client.removeSubscription(subscription);
            client.removeSessionActivityListener(activityListener);
            client.removeFaultListener(faultListener);
            client.disconnect();
        }
    }
}
