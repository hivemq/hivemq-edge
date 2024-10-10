package com.hivemq.edge.adapters.opcua;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.hivemq.adapter.sdk.api.discovery.NodeType;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryInput;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryOutput;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.writing.WritingInput;
import com.hivemq.adapter.sdk.api.writing.WritingOutput;
import com.hivemq.edge.adapters.opcua.client.OpcUaClientConfigurator;
import com.hivemq.edge.adapters.opcua.client.OpcUaEndpointFilter;
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
    public final @NotNull Optional<JsonToOpcUAConverter> jsonToOpcUAConverter;
    public final @NotNull Optional<JsonSchemaGenerator> jsonSchemaGenerator;
    public final @NotNull OpcUaSubscriptionLifecycle opcUaSubscriptionLifecycle;

    public OpcUaClientWrapper(
            @NotNull final OpcUaClient client,
            @NotNull final OpcUaSubscriptionLifecycle opcUaSubscriptionLifecycle,
            @NotNull final Optional<JsonToOpcUAConverter> jsonToOpcUAConverter,
            @NotNull final Optional<JsonSchemaGenerator> jsonSchemaGenerator) {
        this.client = client;
        this.jsonToOpcUAConverter = jsonToOpcUAConverter;
        this.jsonSchemaGenerator = jsonSchemaGenerator;
        this.opcUaSubscriptionLifecycle = opcUaSubscriptionLifecycle;
    }

    public CompletableFuture<Void> stop() {
        return opcUaSubscriptionLifecycle
                .stop()
                .thenCompose(ignored ->
                    client
                        .disconnect()
                        .thenApply(ignored2 -> null)
                );
    }

    public @NotNull CompletableFuture<@NotNull JsonNode> createMqttPayloadJsonSchema(final @NotNull MqttToOpcUaMapping writeContext) {
        return jsonSchemaGenerator
                .map(gen -> gen.createJsonSchema(NodeId.parse(writeContext.getNode())))
                .orElseGet(() -> CompletableFuture.failedFuture(new NullPointerException()));
    }

    public void discoverValues(
            final @NotNull ProtocolAdapterDiscoveryInput input,
            final @NotNull ProtocolAdapterDiscoveryOutput output) {
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

    public void write(final @NotNull WritingInput writingInput, final @NotNull WritingOutput writingOutput) {
        final OpcUaPayload opcUAWritePayload = (OpcUaPayload) writingInput.getWritingPayload();
        final MqttToOpcUaMapping writeContext = (MqttToOpcUaMapping) writingInput.getWritingContext();
        log.debug("Write for opcua is invoked with payload '{}' and context '{}' ", opcUAWritePayload, writeContext);
        final NodeId nodeId = NodeId.parse(writeContext.getNode());

        jsonToOpcUAConverter
                .map(conv -> conv.convertToOpcUAValue(opcUAWritePayload.getValue(), nodeId))
                .ifPresentOrElse(
                        opcUaObject -> {
                                final Variant variant = new Variant(opcUaObject);
                                final DataValue dataValue = new DataValue(variant, null, null);
                                final CompletableFuture<StatusCode> writeFuture = client.writeValue(nodeId, dataValue);
                                writeFuture.whenComplete((statusCode, throwable) -> {
                                    if (throwable != null) {
                                        log.error("Exception while writing to opcua node '{}'", writeContext.getNode(), throwable);
                                        writingOutput.fail(throwable, null);
                                    } else {
                                        log.info("Wrote '{}' to nodeId={}", variant, nodeId);
                                        writingOutput.finish();
                                    }
                                });
                            },
                        () -> writingOutput.fail("JsonToOpcUaConverter not available"));

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

    public static CompletableFuture<OpcUaClientWrapper> createAndConnect(
            final @NotNull OpcUaAdapterConfig adapterConfig,
            final @NotNull ProtocolAdapterState protocolAdapterState,
            final @NotNull ModuleServices moduleServices,
            final @NotNull String id,
            final @NotNull String protocolId,
            final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService
    ) throws UaException {
        final String configPolicyUri = adapterConfig.getSecurity().getPolicy().getSecurityPolicy().getUri();

        OpcUaClient opcUaClient = OpcUaClient.create(adapterConfig.getUri(),
                new OpcUaEndpointFilter(configPolicyUri, adapterConfig),
                new OpcUaClientConfigurator(adapterConfig));
        //Decoding a struct with custom DataType requires a DataTypeManager, so we register one that updates each time a session is activated.
        opcUaClient.addSessionInitializer(new DataTypeDictionarySessionInitializer(new GenericBsdParser()));

        //-- Seems to be not connection monitoring hook, use the session activity listener
        opcUaClient.addSessionActivityListener(new SessionActivityListener() {
            @Override
            public void onSessionInactive(final @NotNull UaSession session) {
                log.info("OPC UA client of protocol adapter '{}' disconnected: {}", id, session);
                protocolAdapterState.setConnectionStatus(DISCONNECTED);
            }

            @Override
            public void onSessionActive(final @NotNull UaSession session) {
                log.info("OPC UA client of protocol adapter '{}' connected: {}", id, session);
                protocolAdapterState.setConnectionStatus(CONNECTED);
            }
        });
        opcUaClient.addFaultListener(serviceFault -> {
            log.info("OPC UA client of protocol adapter '{}' detected a service fault: {}", id, serviceFault);
            moduleServices.eventService()
                    .createAdapterEvent(adapterConfig.getId(), protocolId)
                    .withSeverity(Event.SEVERITY.ERROR)
                    .withPayload(serviceFault.getResponseHeader().getServiceResult())
                    .withMessage("A Service Fault was Detected.")
                    .fire();
        });

        return opcUaClient.connect().thenCompose(uaClient -> {
            OpcUaSubscriptionLifecycle opcUaSubscriptionLifecycle = new OpcUaSubscriptionLifecycle(
                    opcUaClient,
                    adapterConfig.getId(),
                    protocolId,
                    protocolAdapterMetricsService,
                    moduleServices.eventService(),
                    moduleServices.adapterPublishService());

            opcUaClient.getSubscriptionManager()
                    .addSubscriptionListener(opcUaSubscriptionLifecycle);

            try {
                Optional<JsonToOpcUAConverter> jsonToOpcUAConverterOpt = Optional.of(new JsonToOpcUAConverter(opcUaClient));
                Optional<JsonSchemaGenerator> jsonSchemaGeneratorOpt = Optional.of(new JsonSchemaGenerator(opcUaClient    , new ObjectMapper()));
                return opcUaSubscriptionLifecycle
                        .subscribeAll(adapterConfig.getOpcuaToMqttConfig().getOpcuaToMqttMappings())
                        .thenApply(ignored -> new OpcUaClientWrapper(opcUaClient, opcUaSubscriptionLifecycle, jsonToOpcUAConverterOpt, jsonSchemaGeneratorOpt));
            } catch (final UaException e) {
                log.error("Unable to create the converters for writing.", e);
                Optional<JsonToOpcUAConverter> jsonToOpcUAConverterOpt = Optional.empty();
                Optional<JsonSchemaGenerator> jsonSchemaGeneratorOpt = Optional.empty();
                return opcUaSubscriptionLifecycle
                        .subscribeAll(adapterConfig.getOpcuaToMqttConfig().getOpcuaToMqttMappings())
                        .thenApply(ignored -> new OpcUaClientWrapper(opcUaClient, opcUaSubscriptionLifecycle, jsonToOpcUAConverterOpt, jsonSchemaGeneratorOpt));
            }
        });
    }

}
