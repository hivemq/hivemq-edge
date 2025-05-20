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

import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryInput;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryOutput;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationInput;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationOutput;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.writing.WritingInput;
import com.hivemq.adapter.sdk.api.writing.WritingOutput;
import com.hivemq.adapter.sdk.api.writing.WritingPayload;
import com.hivemq.adapter.sdk.api.writing.WritingProtocolAdapter;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.CONNECTED;
import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.ERROR;
import static com.hivemq.edge.adapters.opcua.OpcUaProtocolAdapterInformation.PROTOCOL_ID;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class OpcUaProtocolAdapter implements WritingProtocolAdapter {
    private static final @NotNull Logger log = LoggerFactory.getLogger(OpcUaProtocolAdapter.class);

    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull OpcUaSpecificAdapterConfig adapterConfig;
    private final @NotNull List<OpcuaTag> tags;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;
    private final @NotNull ModuleServices moduleServices;
    private final @NotNull String adapterId;
    private final @NotNull DataPointFactory dataPointFactory;
    private volatile @Nullable OpcUaClient opcUaClient;

    public OpcUaProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<OpcUaSpecificAdapterConfig> input) {
        this.adapterId = input.getAdapterId();
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.tags = input.getTags().stream().map(tag -> (OpcuaTag)tag).toList();
        this.protocolAdapterMetricsService = input.getProtocolAdapterMetricsHelper();
        this.moduleServices = input.moduleServices();
        this.dataPointFactory = input.adapterFactories().dataPointFactory();
    }

    @Override
    public @NotNull String getId() {
        return adapterId;
    }

    @Override
    public void start(
            final @NotNull ProtocolAdapterStartInput input, final @NotNull ProtocolAdapterStartOutput output) {
        if (opcUaClient == null) {
            synchronized (this) {
                if (opcUaClient == null) {
                    try {
                        final var newOcpUaClient = createOpcUaClient(output);

                        opcUaClient = newOcpUaClient;

                    } catch (UaException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        } else {
            throw new IllegalStateException("Already started");
        }
    }

    private @NotNull OpcUaClient createOpcUaClient(final @NotNull ProtocolAdapterStartOutput output) throws UaException {
        final var newOcpUaClient = OpcUaClient.create(
                adapterConfig.getUri(),
                endpoints -> endpoints.stream().findFirst(),
                transportConfigBuilder -> {},
                clientConfigBuilder ->
                        //TODO MUST BE CONFIGURABLE!!!!!!!!!
                        clientConfigBuilder
                                .setApplicationName(LocalizedText.english("eclipse milo opc-ua client"))
                                .setApplicationUri("urn:eclipse:milo:examples:client"));
        newOcpUaClient
                .connectAsync()
                .thenCompose(client -> {
                    final var nodeIdToTag = tags.stream()
                            .collect(
                                    Collectors.toMap(
                                            tag -> NodeId.parse(tag.getDefinition().getNode()),
                                            tag -> tag));

                    final Map<OpcuaTag, Boolean> tagToFirstSeen = new ConcurrentHashMap<>();

                    var subscription = new OpcUaSubscription(client);
                    subscription.setPublishingInterval((double)adapterConfig.getOpcuaToMqttConfig().getPublishingInterval());
                    subscription.setSubscriptionListener(new OpcUaSubscription.SubscriptionListener() {
                        @Override
                        public void onDataReceived(
                                final OpcUaSubscription subscription,
                                final List<OpcUaMonitoredItem> items,
                                final List<DataValue> values) {
                            for (int i = 0; i < items.size(); i++) {
                                final var tag = nodeIdToTag.get(items.get(i).getReadValueId().getNodeId());
                                if (null == tagToFirstSeen.putIfAbsent(tag, true)) {
                                    moduleServices.eventService().createAdapterEvent(adapterId, PROTOCOL_ID)
                                            .withSeverity(Event.SEVERITY.INFO)
                                            .withMessage(String.format("Adapter '%s' took first sample for tag '%s'",
                                                    adapterId,
                                                    tag.getName()))
                                            .fire();

                                }
                                final var value = values.get(i);
                                try {
                                    final var convertedPayload = new String(convertPayload(value, client.getDynamicEncodingContext()));
                                    moduleServices.protocolAdapterTagStreamingService().feed(tag.getName(), List.of(dataPointFactory.createJsonDataPoint(tag.getName(), convertedPayload)));
                                } catch (UaException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    });

                    return subscription
                            .createAsync()
                            .thenCompose(ignored -> {
                                tags.forEach(opcuaTag -> {
                                    final String nodeId = opcuaTag.getDefinition().getNode();
                                    var monitoredItem = OpcUaMonitoredItem.newDataItem(NodeId.parse(nodeId));
                                    monitoredItem.setQueueSize(uint(adapterConfig.getOpcuaToMqttConfig().getServerQueueSize()));
                                    monitoredItem.setSamplingInterval(adapterConfig.getOpcuaToMqttConfig().getPublishingInterval());
                                    subscription.addMonitoredItem(monitoredItem);
                                });

                                try {
                                    subscription.synchronizeMonitoredItems();
                                    return CompletableFuture.completedFuture(null);
                                } catch (MonitoredItemSynchronizationException ex) {
                                    return CompletableFuture.failedFuture(ex);
                                }
                            });
                })
                .whenComplete((ignored, throwable) -> {
                        if (throwable != null) {
                            log.error("Failed to connect", throwable);
                            output.failStart(throwable, "Failed to connect");
                            protocolAdapterState.setConnectionStatus(ERROR);
                            newOcpUaClient.disconnectAsync();
                        } else {
                            log.info("Subscription created successfully");
                            protocolAdapterState.setConnectionStatus(CONNECTED);
                            output.startedSuccessfully();
                        }
                    });
        return newOcpUaClient;
    }

    @Override
    public void stop(final @NotNull ProtocolAdapterStopInput input, final @NotNull ProtocolAdapterStopOutput output) {
        if (opcUaClient != null) {
            synchronized (this) {
                if (opcUaClient != null) {
                    opcUaClient
                            .disconnectAsync()
                            .whenComplete((client, throwable) -> {
                                opcUaClient = null;
                                if (throwable != null) {
                                    output.failStop(throwable, "Failed to stop");
                                } else {
                                    output.stoppedSuccessfully();
                                }
                            });
                }
            }
        }
    }

    @Override
    public void discoverValues(
            final @NotNull ProtocolAdapterDiscoveryInput input, final @NotNull ProtocolAdapterDiscoveryOutput output) {
        //TODO implement discovery
        output.finish();
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

    @Override
    public void write(final @NotNull WritingInput input, final @NotNull WritingOutput output) {
        //TODO implement write
        output.finish();
    }

    @Override
    public void createTagSchema(
            final @NotNull TagSchemaCreationInput input, final @NotNull TagSchemaCreationOutput output) {
        //TODO implement createTagSchema
    }

    @Override
    public @NotNull Class<? extends WritingPayload> getMqttPayloadClass() {
        return OpcUaPayload.class;
    }

    @VisibleForTesting
    public @NotNull ProtocolAdapterState getProtocolAdapterState() {
        return protocolAdapterState;
    }



    public static final byte[] EMTPY_BYTES = new byte[]{};

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
