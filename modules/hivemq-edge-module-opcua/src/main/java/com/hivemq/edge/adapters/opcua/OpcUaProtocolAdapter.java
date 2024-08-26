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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.discovery.NodeType;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryInput;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryOutput;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.factories.AdapterFactories;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.edge.adapters.opcua.client.OpcUaClientConfigurator;
import com.hivemq.edge.adapters.opcua.client.OpcUaEndpointFilter;
import com.hivemq.edge.adapters.opcua.client.OpcUaSubscriptionConsumer;
import com.hivemq.edge.adapters.opcua.client.OpcUaSubscriptionListener;
import com.hivemq.edge.adapters.opcua.config.OpcUaAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.OpcUaToMqttMapping;
import org.eclipse.milo.opcua.binaryschema.GenericBsdParser;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.SessionActivityListener;
import org.eclipse.milo.opcua.sdk.client.api.UaSession;
import org.eclipse.milo.opcua.sdk.client.dtd.DataTypeDictionarySessionInitializer;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseResultMask;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.CONNECTED;
import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.DISCONNECTED;
import static java.util.Objects.requireNonNullElse;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class OpcUaProtocolAdapter implements ProtocolAdapter {
    private static final @NotNull Logger log = LoggerFactory.getLogger(OpcUaProtocolAdapter.class);

    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull OpcUaAdapterConfig adapterConfig;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull ModuleServices moduleServices;
    private final @NotNull AdapterFactories adapterFactories;
    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;
    private @Nullable OpcUaClient opcUaClient;
    private final @NotNull Map<UInteger, OpcUaToMqttMapping> subscriptionMap = new ConcurrentHashMap<>();

    public OpcUaProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<OpcUaAdapterConfig> input) {
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.moduleServices = input.moduleServices();
        this.adapterFactories = input.adapterFactories();
        this.protocolAdapterMetricsService = input.getProtocolAdapterMetricsHelper();
    }

    @Override
    public @NotNull String getId() {
        return adapterConfig.getId();
    }

    @Override
    public void start(
            final @NotNull ProtocolAdapterStartInput input, final @NotNull ProtocolAdapterStartOutput output) {
        try {
            if (opcUaClient == null) {
                createClient();
            }
            opcUaClient.connect().thenAccept(uaClient -> {
                opcUaClient.getSubscriptionManager().addSubscriptionListener(createSubscriptionListener());
                createAllSubscriptions().whenComplete((unused, throwable) -> {
                    if (throwable == null) {
                        output.startedSuccessfully();
                    } else {
                        output.failStart(throwable, throwable.getMessage());
                    }
                });
            }).exceptionally(throwable -> {
                log.error("Not able to connect and subscribe to OPC UA server {}", adapterConfig.getUri(), throwable);
                stopInternal();
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

    public @NotNull CompletableFuture<Void> stopInternal() {
        try {
            if (opcUaClient == null) {
                return CompletableFuture.completedFuture(null);
            } else {
                subscriptionMap.clear();
                try {
                    return opcUaClient.disconnect()
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
            final @NotNull ProtocolAdapterDiscoveryInput input, final @NotNull ProtocolAdapterDiscoveryOutput output) {

        if (opcUaClient == null) {
            throw new IllegalStateException("OPC UA Adapter not started yet");
        }

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

        browse(opcUaClient, browseRoot, null, (ref, parent) -> {
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

    private @NotNull OpcUaSubscriptionListener createSubscriptionListener() {
        return new OpcUaSubscriptionListener(protocolAdapterMetricsService, adapterConfig.getId(), (subscription) -> {
            //re-create a subscription on failure
            final OpcUaToMqttMapping subscriptionConfig = subscriptionMap.get(subscription.getSubscriptionId());
            if (subscriptionConfig != null) {
                try {
                    subscribeToNode(subscriptionConfig).get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Not able to recreate OPC UA subscription after transfer failure", e);
                }
            }
        });
    }

    private @NotNull CompletableFuture<Void> createAllSubscriptions() {
        //noinspection ConstantValue
        if (adapterConfig.getOpcuaToMqttConfig() == null ||
                adapterConfig.getOpcuaToMqttConfig().getMappings() == null ||
                adapterConfig.getOpcuaToMqttConfig().getMappings().isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        final CompletableFuture<Void> resultFuture = new CompletableFuture<>();
        final ImmutableList.Builder<CompletableFuture<Void>> subscribeFutures = ImmutableList.builder();

        for (final OpcUaToMqttMapping subscription : adapterConfig.getOpcuaToMqttConfig().getMappings()) {
            subscribeFutures.add(subscribeToNode(subscription));
        }

        CompletableFuture.allOf(subscribeFutures.build().toArray(new CompletableFuture[]{})).thenApply(unused -> {
            resultFuture.complete(null);
            return null;
        }).exceptionally(throwable -> {
            protocolAdapterState.setErrorConnectionStatus(throwable, null);
            resultFuture.completeExceptionally(throwable);
            return null;
        });
        return resultFuture;
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
                protocolAdapterState.setConnectionStatus(DISCONNECTED);
            }

            @Override
            public void onSessionActive(final @NotNull UaSession session) {
                protocolAdapterState.setConnectionStatus(CONNECTED);
            }
        });
        opcUaClient.addFaultListener(serviceFault -> {
            moduleServices.eventService()
                    .createAdapterEvent(adapterConfig.getId(), adapterInformation.getProtocolId())
                    .withSeverity(Event.SEVERITY.ERROR)
                    .withPayload(serviceFault.getResponseHeader().getServiceResult())
                    .withMessage("A Service Fault was Detected.")
                    .fire();
        });
    }

    private @NotNull CompletableFuture<Void> subscribeToNode(final @NotNull OpcUaToMqttMapping subscription) {
        try {
            final CompletableFuture<Void> resultFuture = new CompletableFuture<>();

            final ReadValueId readValueId = new ReadValueId(NodeId.parse(subscription.getNode()),
                    AttributeId.Value.uid(),
                    null,
                    QualifiedName.NULL_VALUE);

            Objects.requireNonNull(opcUaClient)
                    .getSubscriptionManager()
                    .createSubscription(subscription.getPublishingInterval())
                    .thenAccept(new OpcUaSubscriptionConsumer(subscription,
                            readValueId,
                            moduleServices.adapterPublishService(),
                            moduleServices.eventService(),
                            resultFuture,
                            opcUaClient,
                            subscriptionMap,
                            protocolAdapterMetricsService,
                            adapterConfig.getId(),
                            this,
                            adapterFactories));
            return resultFuture;
        } catch (final Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Nullable
    private static NodeType getNodeType(ReferenceDescription ref) {
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

    private @NotNull CompletableFuture<Void> browse(
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

        try {
            final BrowseResult browseResult = client.browse(browse).get();
            return handleBrowseResult(client, parent, callback, depth, browseResult);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Browsing nodeId={} failed: {}", browseRoot, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private @NotNull CompletableFuture<Void> handleBrowseResult(
            final @NotNull OpcUaClient client,
            final @Nullable ReferenceDescription parent,
            final @NotNull BiConsumer<ReferenceDescription, ReferenceDescription> callback,
            final int depth,
            final BrowseResult browseResult) throws InterruptedException, ExecutionException {
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
            final BrowseResult nextBrowseResult =
                    Objects.requireNonNull(opcUaClient).browseNext(false, continuationPoint).get();
            handleBrowseResult(opcUaClient, parent, callback, depth, nextBrowseResult);
        }

        return CompletableFuture.allOf(childFutures.build().toArray(new CompletableFuture[]{}));
    }

    @VisibleForTesting
    public @NotNull ProtocolAdapterState getProtocolAdapterState() {
        return protocolAdapterState;
    }
}
