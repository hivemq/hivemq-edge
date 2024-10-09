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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.discovery.NodeType;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryInput;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryOutput;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.writing.WritingInput;
import com.hivemq.adapter.sdk.api.writing.WritingOutput;
import com.hivemq.adapter.sdk.api.writing.WritingPayload;
import com.hivemq.adapter.sdk.api.writing.WritingProtocolAdapter;
import com.hivemq.edge.adapters.opcua.client.OpcUaClientConfigurator;
import com.hivemq.edge.adapters.opcua.client.OpcUaEndpointFilter;
import com.hivemq.edge.adapters.opcua.config.BidirectionalOpcUaAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.OpcUaAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.mqtt2opcua.MqttToOpcUaMapping;
import com.hivemq.edge.adapters.opcua.mqtt2opcua.JsonSchemaGenerator;
import com.hivemq.edge.adapters.opcua.mqtt2opcua.JsonToOpcUAConverter;
import com.hivemq.edge.adapters.opcua.mqtt2opcua.OpcUaPayload;
import org.eclipse.milo.opcua.binaryschema.GenericBsdParser;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.SessionActivityListener;
import org.eclipse.milo.opcua.sdk.client.api.UaSession;
import org.eclipse.milo.opcua.sdk.client.dtd.DataTypeDictionarySessionInitializer;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseResultMask;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.CONNECTED;
import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.DISCONNECTED;
import static java.util.Objects.requireNonNullElse;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class OpcUaProtocolAdapter implements ProtocolAdapter, WritingProtocolAdapter<MqttToOpcUaMapping> {
    private static final @NotNull Logger log = LoggerFactory.getLogger(OpcUaProtocolAdapter.class);

    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull OpcUaAdapterConfig adapterConfig;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull ModuleServices moduleServices;
    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;
    private volatile @Nullable OpcUaClient opcUaClient;
    private volatile @Nullable JsonToOpcUAConverter jsonToOpcUAConverter;
    private volatile @Nullable JsonSchemaGenerator jsonSchemaGenerator;
    private volatile @Nullable OpcUaSubscriptionLifecycle opcUaSubscriptionLifecycle;

    public OpcUaProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<OpcUaAdapterConfig> input) {
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.moduleServices = input.moduleServices();
        this.protocolAdapterMetricsService = input.getProtocolAdapterMetricsHelper();
    }

    @Override
    public @NotNull String getId() {
        return adapterConfig.getId();
    }

    @Override
    public void start(
            final @NotNull ProtocolAdapterStartInput input,
            final @NotNull ProtocolAdapterStartOutput output) {
        try {
            if (opcUaClient == null) {
                createClient();
            }
            opcUaClient.connect().thenAccept(uaClient -> {
                try {
                    jsonToOpcUAConverter = new JsonToOpcUAConverter(opcUaClient);
                    jsonSchemaGenerator = new JsonSchemaGenerator(opcUaClient, new ObjectMapper());
                } catch (final UaException e) {
                    log.error("Unable to create the converter for writing.", e);
                }
                this.opcUaSubscriptionLifecycle = new OpcUaSubscriptionLifecycle(
                        opcUaClient,
                        adapterConfig.getId(),
                        adapterInformation.getProtocolId(),
                        protocolAdapterMetricsService,
                        moduleServices.eventService(),
                        moduleServices.adapterPublishService());

                opcUaClient.getSubscriptionManager()
                        .addSubscriptionListener(opcUaSubscriptionLifecycle);

                opcUaSubscriptionLifecycle
                        .subscribeAll(adapterConfig.getOpcuaToMqttConfig().getOpcuaToMqttMappings())
                        .whenComplete((unused, throwable) -> {
                    if (throwable == null) {
                        output.startedSuccessfully();
                    } else {
                        protocolAdapterState.setErrorConnectionStatus(throwable, null);
                        output.failStart(throwable, throwable.getMessage());
                    }
                });
            }).exceptionally(throwable -> {
                log.error("Not able to connect and subscribe to OPC UA server {}", adapterConfig.getUri(), throwable);
                stopInternal();
                protocolAdapterState.setErrorConnectionStatus(throwable, null);
                output.failStart(throwable, throwable.getMessage());
                return null;
            });
        } catch (final Exception e) {
            log.error("Not able to start OPC UA client for server {}", adapterConfig.getUri(), e);
            output.failStart(e, "Not able to start OPC UA client for server " + adapterConfig.getUri());
        }
    }

    @Override
    public void stop(final @NotNull ProtocolAdapterStopInput input, final @NotNull ProtocolAdapterStopOutput output) {
        stopInternal().whenComplete((aVoid, t) -> {
            if (t != null) {
                output.failStop(t, null);
            } else {
                output.stoppedSuccessfully();
            }
        });
    }

    private @NotNull CompletableFuture<Void> stopInternal() {
        try {
            if (opcUaClient == null) {
                return CompletableFuture.completedFuture(null);
            } else {
                OpcUaSubscriptionLifecycle temp = opcUaSubscriptionLifecycle;
                opcUaSubscriptionLifecycle = null;
                if (temp != null) {
                    temp.stop();
                }
                try {
                    return opcUaClient
                            .disconnect()
                            .thenAccept(client -> protocolAdapterState.setConnectionStatus(DISCONNECTED));
                } finally {
                    opcUaClient = null;
                }
            }
        } catch (final Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public void discoverValues(
            final @NotNull ProtocolAdapterDiscoveryInput input,
            final @NotNull ProtocolAdapterDiscoveryOutput output) {
        OpcUaClient client = opcUaClient;
        Objects.requireNonNull(client, "OPC UA Adapter not started yet");

        final NodeId browseRoot;
        if (input.getRootNode() == null || input.getRootNode().isBlank()) {
            browseRoot = Identifiers.ObjectsFolder;
        } else {
            final Optional<NodeId> parsedNodeId = NodeId.parseSafe(input.getRootNode());
            if (parsedNodeId.isEmpty()) {
                throw new IllegalArgumentException("OPC UA NodeId '" + input.getRootNode() + "' is not supported");
            }
            browseRoot = parsedNodeId.get();
        }

        browse(client, browseRoot, null, (ref, parent) -> {
            final String name = ref.getBrowseName() != null ? ref.getBrowseName().getName() : "";
            final String displayName = ref.getDisplayName() != null ? ref.getDisplayName().getText() : "";
            final NodeType nodeType = getNodeType(ref);
            output.getNodeTree()
                    .addNode(ref.getNodeId().toParseableString(),
                            requireNonNullElse(name, ""),
                            ref.getNodeId().toParseableString(),
                            requireNonNullElse(displayName, ""),
                            parent != null ? parent.getNodeId().toParseableString() : null,
                            nodeType != null ? nodeType : NodeType.VALUE,
                            nodeType == NodeType.VALUE);
        }, input.getDepth()).whenComplete((aVoid, t) -> {
            if (t != null) {
                output.fail(t, null);
            } else {
                output.finish();
            }
        });
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

    @Override
    public void write(final @NotNull WritingInput writingInput, final @NotNull WritingOutput writingOutput) {
        Objects.requireNonNull(opcUaClient, "OPC UA Adapter not started yet");
        final OpcUaPayload opcUAWritePayload = (OpcUaPayload) writingInput.getWritingPayload();
        final MqttToOpcUaMapping writeContext = (MqttToOpcUaMapping) writingInput.getWritingContext();
        log.debug("Write for opcua is invoked with payload '{}' and context '{}' ", opcUAWritePayload, writeContext);
        try {
            Objects.requireNonNull(jsonToOpcUAConverter, "Converter is null.");

            final NodeId nodeId = NodeId.parse(writeContext.getNode());
            final Object opcUaObject;
            try {
                opcUaObject = Objects.requireNonNull(jsonToOpcUAConverter)
                        .convertToOpcUAValue(opcUAWritePayload.getValue(), nodeId);
            } catch (final Exception e) {
                writingOutput.fail(e.getMessage());
                return;
            }

            final Variant variant = new Variant(opcUaObject);
            final DataValue dataValue = new DataValue(variant, null, null);
            final CompletableFuture<StatusCode> writeFuture = opcUaClient.writeValue(nodeId, dataValue);
            writeFuture.whenComplete((statusCode, throwable) -> {
                if (throwable != null) {
                    log.error("Exception while writing to opcua node '{}'", writeContext.getNode(), throwable);
                    writingOutput.fail(throwable, null);
                } else {
                    log.info("Wrote '{}' to nodeId={}", variant, nodeId);
                    writingOutput.finish();
                }
            });
        } catch (final IllegalArgumentException illegalArgumentException) {
            writingOutput.fail(illegalArgumentException, null);
        }
    }

    @Override
    public @NotNull List<MqttToOpcUaMapping> getWritingContexts() {
        if(adapterConfig instanceof BidirectionalOpcUaAdapterConfig) {
            return ((BidirectionalOpcUaAdapterConfig) adapterConfig).getMqttToOpcUaConfig().getMappings();
        }
        return Collections.emptyList();
    }

    @Override
    public @NotNull CompletableFuture<@NotNull JsonNode> createMqttPayloadJsonSchema(final @NotNull MqttToOpcUaMapping writeContext) {
        try {
            return Objects.requireNonNull(jsonSchemaGenerator).createJsonSchema(NodeId.parse(writeContext.getNode()));
        } catch (final Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public @NotNull Class<? extends WritingPayload> getMqttPayloadClass() {
        return OpcUaPayload.class;
    }

    private void createClient() throws UaException {
        final String configPolicyUri = adapterConfig.getSecurity().getPolicy().getSecurityPolicy().getUri();

        opcUaClient = OpcUaClient.create(adapterConfig.getUri(),
                new OpcUaEndpointFilter(configPolicyUri, adapterConfig),
                new OpcUaClientConfigurator(adapterConfig));
        //Decoding a struct with custom DataType requires a DataTypeManager, so we register one that updates each time a session is activated.
        opcUaClient.addSessionInitializer(new DataTypeDictionarySessionInitializer(new GenericBsdParser()));

        //-- Seems to be not connection monitoring hook, use the session activity listener
        opcUaClient.addSessionActivityListener(new SessionActivityListener() {
            @Override
            public void onSessionInactive(final @NotNull UaSession session) {
                log.info("OPC UA client of protocol adapter '{}' disconnected: {}", getId(), session);
                protocolAdapterState.setConnectionStatus(DISCONNECTED);
            }

            @Override
            public void onSessionActive(final @NotNull UaSession session) {
                log.info("OPC UA client of protocol adapter '{}' connected: {}", getId(), session);
                protocolAdapterState.setConnectionStatus(CONNECTED);
            }
        });
        opcUaClient.addFaultListener(serviceFault -> {
            log.info("OPC UA client of protocol adapter '{}' detected a service fault: {}", getId(), serviceFault);
            moduleServices.eventService()
                    .createAdapterEvent(adapterConfig.getId(), adapterInformation.getProtocolId())
                    .withSeverity(Event.SEVERITY.ERROR)
                    .withPayload(serviceFault.getResponseHeader().getServiceResult())
                    .withMessage("A Service Fault was Detected.")
                    .fire();
        });
    }

    private static @Nullable NodeType getNodeType(final @NotNull ReferenceDescription ref) {
        switch (ref.getNodeClass()) {
            case Object:
            case ObjectType:
            case VariableType:
            case ReferenceType:
            case DataType:
                return NodeType.OBJECT;
            case Variable:
                return NodeType.VALUE;
            case View:
                return NodeType.FOLDER;
            default:
                return null;
        }
    }

    private static @NotNull CompletableFuture<Void> browse(
            final @NotNull OpcUaClient client,
            final @NotNull NodeId browseRoot,
            final @Nullable ReferenceDescription parent,
            final @NotNull BiConsumer<ReferenceDescription, ReferenceDescription> callback,
            final int depth) {
        final BrowseDescription browse = new BrowseDescription(browseRoot,
                BrowseDirection.Forward,
                null,
                true,
                uint(0),
                uint(BrowseResultMask.All.getValue()));

        return client
                .browse(browse)
                .thenCompose(browseResult -> handleBrowseResult(client, parent, callback, depth, browseResult));
    }

    private static @NotNull CompletableFuture<Void> handleBrowseResult(
            final @NotNull OpcUaClient client,
            final @Nullable ReferenceDescription parent,
            final @NotNull BiConsumer<ReferenceDescription, ReferenceDescription> callback,
            final int depth,
            final BrowseResult browseResult) {
        final ReferenceDescription[] references = browseResult.getReferences();

        final ImmutableList.Builder<CompletableFuture<Void>> childFutures = ImmutableList.builder();

        if (references == null) {
            return CompletableFuture.completedFuture(null);
        }

        for (final ReferenceDescription rd : references) {
            callback.accept(rd, parent);
            // recursively browse to children
            if (depth > 1) {
                final Optional<NodeId> childNodeId = rd.getNodeId().toNodeId(client.getNamespaceTable());
                childNodeId.ifPresent(nodeId -> childFutures.add(browse(client, nodeId, rd, callback, depth - 1)));
            }
        }

        final ByteString continuationPoint = browseResult.getContinuationPoint();
        if (continuationPoint != null && !continuationPoint.isNull()) {
                childFutures.add(
                        Objects.requireNonNull(client)
                                .browseNext(false, continuationPoint)
                                .thenCompose(nextBrowseResult ->
                                        handleBrowseResult(client, parent, callback, depth, nextBrowseResult)));
        }

        return CompletableFuture.allOf(childFutures.build().toArray(new CompletableFuture[]{}));
    }

    @VisibleForTesting
    public @NotNull ProtocolAdapterState getProtocolAdapterState() {
        return protocolAdapterState;
    }
}
