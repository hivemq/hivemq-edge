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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.discovery.NodeType;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryInput;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryOutput;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationOutput;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.adapter.sdk.api.writing.WritingContext;
import com.hivemq.adapter.sdk.api.writing.WritingInput;
import com.hivemq.adapter.sdk.api.writing.WritingOutput;
import com.hivemq.edge.adapters.opcua.client.OpcUaClientConfigurator;
import com.hivemq.edge.adapters.opcua.client.OpcUaEndpointFilter;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.mqtt2opcua.JsonSchemaGenerator;
import com.hivemq.edge.adapters.opcua.mqtt2opcua.JsonToOpcUAConverter;
import com.hivemq.edge.adapters.opcua.mqtt2opcua.OpcUaPayload;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.SessionActivityListener;
import org.eclipse.milo.opcua.sdk.client.UaSession;
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

import java.util.ArrayList;
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

public class OpcUaClientWrapper {
    private static final Logger log = LoggerFactory.getLogger(OpcUaClientWrapper.class);

    public final @NotNull OpcUaClient client;
    public final @NotNull JsonToOpcUAConverter jsonToOpcUAConverter;
    public final @NotNull JsonSchemaGenerator jsonSchemaGenerator;
    public final @NotNull OpcUaSubscriptionLifecycle opcUaSubscriptionLifecycle;
    public final @NotNull DataPointFactory dataPointFactory;

    public OpcUaClientWrapper(
            final @NotNull OpcUaClient client,
            final @NotNull OpcUaSubscriptionLifecycle opcUaSubscriptionLifecycle,
            final @NotNull JsonToOpcUAConverter jsonToOpcUAConverter,
            final @NotNull JsonSchemaGenerator jsonSchemaGenerator,
            final @NotNull DataPointFactory dataPointFactory) {
        this.client = client;
        this.jsonToOpcUAConverter = jsonToOpcUAConverter;
        this.jsonSchemaGenerator = jsonSchemaGenerator;
        this.opcUaSubscriptionLifecycle = opcUaSubscriptionLifecycle;
        this.dataPointFactory = dataPointFactory;
    }

    public @NotNull CompletableFuture<Void> stop() {
        return opcUaSubscriptionLifecycle.stop()
                .thenCompose(ignored -> client.disconnectAsync().thenApply(ignored2 -> null));
    }

    public void createMqttPayloadJsonSchema(
            final @NotNull OpcuaTag tag, final @NotNull TagSchemaCreationOutput output) {

        final String nodeId = tag.getDefinition().getNode();
        jsonSchemaGenerator.createJsonSchema(NodeId.parse(nodeId), output);
    }

    public void discoverValues(
            final @NotNull ProtocolAdapterDiscoveryInput input, final @NotNull ProtocolAdapterDiscoveryOutput output) {
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

    public void write(
            final @NotNull WritingInput writingInput,
            final @NotNull WritingOutput writingOutput,
            final @NotNull OpcuaTag opcuaTag) {
        final OpcUaPayload opcUAWritePayload = (OpcUaPayload) writingInput.getWritingPayload();
        final WritingContext writeContext = writingInput.getWritingContext();
        log.debug("Write for opcua is invoked with payload '{}' and context '{}' ", opcUAWritePayload, writeContext);

        final NodeId nodeId = NodeId.parse(opcuaTag.getDefinition().getNode());

        try {
            final Object opcuaObject = jsonToOpcUAConverter.convertToOpcUAValue(opcUAWritePayload.getValue(), nodeId);
            final Variant variant = new Variant(opcuaObject);
            final DataValue dataValue = new DataValue(variant, null, null);
            final CompletableFuture<List<StatusCode>> writeFuture = client.writeValuesAsync(List.of(nodeId), List.of(dataValue));

            //TODO statusCode wasn't a list before
            writeFuture.whenComplete((statusCode, throwable) -> {
                if (throwable != null) {
                    log.error("Exception while writing to opcua node '{}'", writeContext.getTagName(), throwable);
                    writingOutput.fail(throwable, null);
                } else {
                    log.debug("Wrote '{}' to nodeId={}", variant, nodeId);
                    writingOutput.finish();
                }
            });
        } catch (final Exception e) {
            writingOutput.fail(e, null);
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

        return client.browseAsync(browse)
                .thenCompose(browseResult -> handleBrowseResult(client, parent, callback, depth, new BrowseResult[]{browseResult}));
    }

    private static @NotNull CompletableFuture<Void> handleBrowseResult(
            final @NotNull OpcUaClient client,
            final @Nullable ReferenceDescription parent,
            final @NotNull BiConsumer<ReferenceDescription, ReferenceDescription> callback,
            final int depth,
            final BrowseResult[] browseResults) {
        final List<CompletableFuture<Void>> childFutures = new ArrayList<>();
        final var references = new ArrayList<ReferenceDescription>();
        final var continuationPoints = new ArrayList<ByteString>();

        for (final BrowseResult result : browseResults) {
            final var continuationPoint = result.getContinuationPoint();
            if(continuationPoint != null) {
                continuationPoints.add(continuationPoint);
            }
            Collections.addAll(references, result.getReferences());
        }

        for (final ReferenceDescription rd : references) {
            callback.accept(rd, parent);
            // recursively browse to children
            if (depth > 1) {
                final Optional<NodeId> childNodeId = rd.getNodeId().toNodeId(client.getNamespaceTable());
                childNodeId.ifPresent(nodeId -> childFutures.add(browse(client, nodeId, rd, callback, depth - 1)));
            }
        }

        if (!continuationPoints.isEmpty()) {
            childFutures.add(Objects.requireNonNull(client)
                    .browseNextAsync(false,continuationPoints)
                    .thenCompose(nextBrowseResult ->
                            handleBrowseResult(
                                    client,
                                    parent,
                                    callback,
                                    depth,
                                    nextBrowseResult.getResults())));
        }

        return CompletableFuture.allOf(childFutures.toArray(new CompletableFuture[]{}));
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

    public static @NotNull CompletableFuture<OpcUaClientWrapper> createAndConnect(
            final @NotNull String adapterId,
            final @NotNull OpcUaSpecificAdapterConfig adapterConfig,
            final @NotNull List<OpcuaTag> tags,
            final @NotNull ProtocolAdapterState protocolAdapterState,
            final @NotNull EventService eventService,
            final @NotNull ProtocolAdapterTagStreamingService protocolAdapterTagStreamingService,
            final @NotNull String protocolId,
            final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService,
            final @NotNull ProtocolAdapterStartOutput output,
            final @NotNull DataPointFactory dataPointFactory) throws UaException {
        final String configPolicyUri = adapterConfig.getSecurity().getPolicy().getSecurityPolicy().getUri();

        final OpcUaClient opcUaClient = OpcUaClient.create(
                adapterConfig.getUri().toString(),
                new OpcUaEndpointFilter(adapterId, configPolicyUri, adapterConfig),
                null,
                new OpcUaClientConfigurator(adapterConfig, adapterId));
        //Decoding a struct with custom DataType requires a DataTypeManager, so we register one that updates each time a session is activated.
        //TODO deactivated, check if it still works
        //opcUaClient.addSessionInitializer(new DataTypeDictionarySessionInitializer(new GenericBsdParser()));

        //-- Seems to be not connection monitoring hook, use the session activity listener
        opcUaClient.addSessionActivityListener(new SessionActivityListener() {

            @Override
            public void onSessionInactive(final @NotNull UaSession session) {
                log.info("OPC UA client of protocol adapter '{}' disconnected: {}", adapterId, session);
                protocolAdapterState.setConnectionStatus(DISCONNECTED);
            }

            @Override
            public void onSessionActive(final @NotNull UaSession session) {
                log.info("OPC UA client of protocol adapter '{}' connected: {}", adapterId, session);
                protocolAdapterState.setConnectionStatus(CONNECTED);
            }
        });
        opcUaClient.addFaultListener(serviceFault -> {
            log.info("OPC UA client of protocol adapter '{}' detected a service fault: {}", adapterId, serviceFault);
            eventService.createAdapterEvent(adapterId, protocolId)
                    .withSeverity(Event.SEVERITY.ERROR)
                    .withPayload(serviceFault.getResponseHeader().getServiceResult())
                    .withMessage("A Service Fault was Detected.")
                    .fire();
        });

        return opcUaClient.connectAsync().thenCompose(uaClient -> {
            final OpcUaSubscriptionLifecycle opcUaSubscriptionLifecycle = new OpcUaSubscriptionLifecycle(
                    opcUaClient,
                    adapterId,
                    protocolId,
                    protocolAdapterMetricsService,
                    eventService,
                    protocolAdapterTagStreamingService,
                    adapterConfig.getOpcuaToMqttConfig(),
                    dataPointFactory);

            opcUaClient.getSubscriptionManager().addSubscriptionListener(opcUaSubscriptionLifecycle);

            try {
                final JsonToOpcUAConverter jsonToOpcUAConverter = new JsonToOpcUAConverter(opcUaClient);
                final JsonSchemaGenerator jsonSchemaGenerator =
                        new JsonSchemaGenerator(opcUaClient, new ObjectMapper());
                if (adapterConfig.getOpcuaToMqttConfig() != null) {
                    return opcUaSubscriptionLifecycle.subscribeAll(tags)
                            .thenApply(ignored -> new OpcUaClientWrapper(
                                    opcUaClient,
                                    opcUaSubscriptionLifecycle,
                                    jsonToOpcUAConverter,
                                    jsonSchemaGenerator,
                                    dataPointFactory));
                } else {
                    return CompletableFuture.completedFuture(null);
                }
            } catch (final UaException e) {
                log.error("Unable to create the converters for writing.", e);
                output.failStart(e, "Unable to create the converters for writing.");
                return CompletableFuture.failedFuture(e);
            }
        });
    }


}
