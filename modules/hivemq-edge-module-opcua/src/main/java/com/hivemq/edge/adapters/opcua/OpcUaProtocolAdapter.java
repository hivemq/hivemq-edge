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

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.hivemq.api.model.core.Payload;
import com.hivemq.edge.adapters.opcua.client.OpcUaClientConfigurator;
import com.hivemq.edge.adapters.opcua.client.OpcUaEndpointFilter;
import com.hivemq.edge.adapters.opcua.client.OpcUaSubscriptionConsumer;
import com.hivemq.edge.adapters.opcua.client.OpcUaSubscriptionListener;
import com.hivemq.edge.modules.adapters.impl.AbstractProtocolAdapter;
import com.hivemq.edge.modules.adapters.model.NodeType;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterDiscoveryInput;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterDiscoveryOutput;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterStartOutput;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterInformation;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPublishService;
import com.hivemq.edge.modules.api.events.EventUtils;
import com.hivemq.edge.modules.api.events.model.Event;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.eclipse.milo.opcua.binaryschema.GenericBsdParser;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.SessionActivityListener;
import org.eclipse.milo.opcua.sdk.client.api.ServiceFaultListener;
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
import org.eclipse.milo.opcua.stack.core.types.structured.ServiceFault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

import static java.util.Objects.requireNonNullElse;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class OpcUaProtocolAdapter extends AbstractProtocolAdapter<OpcUaAdapterConfig> {
    private static final Logger log = LoggerFactory.getLogger(OpcUaProtocolAdapter.class);
    private @Nullable OpcUaClient opcUaClient;
    private final @NotNull Map<UInteger, OpcUaAdapterConfig.Subscription> subscriptionMap = new ConcurrentHashMap<>();

    public OpcUaProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull OpcUaAdapterConfig adapterConfig,
            final @NotNull MetricRegistry metricRegistry) {
        super(adapterInformation, adapterConfig, metricRegistry);
    }

    @Override
    protected @NotNull CompletableFuture<ProtocolAdapterStartOutput> startInternal(final @NotNull ProtocolAdapterStartOutput output) {
        try {
            if (opcUaClient == null) {
                createClient();
            }
            final CompletableFuture<ProtocolAdapterStartOutput> resultFuture = new CompletableFuture<>();

            opcUaClient.connect().thenAccept(uaClient -> {
                opcUaClient.getSubscriptionManager().addSubscriptionListener(createSubscriptionListener());
                createAllSubscriptions().whenComplete((unused, throwable) -> {
                    if (throwable == null) {
                        resultFuture.complete(output);
                    } else {
                        output.failStart(throwable, throwable.getMessage());
                        resultFuture.completeExceptionally(throwable);
                    }
                });

            }).exceptionally(throwable -> {
                log.error("Not able to connect and subscribe to OPC-UA server {}", adapterConfig.getUri(), throwable);
                stop();
                output.failStart(throwable, throwable.getMessage());
                resultFuture.completeExceptionally(throwable);
                return null;
            });

            return resultFuture;

        } catch (Exception e) {
            log.error("Not able to start OPC-UA client for server {}", adapterConfig.getUri(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    protected @NotNull CompletableFuture<Void> stopInternal() {
        try {
            if (opcUaClient == null) {
                return CompletableFuture.completedFuture(null);
            } else {
                subscriptionMap.clear();
                try {
                    return opcUaClient.disconnect().thenAccept(client -> {
                        setConnectionStatus(ConnectionStatus.DISCONNECTED);
                    });
                } finally {
                    opcUaClient = null;
                }
            }
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public @NotNull CompletableFuture<Void> discoverValues(
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

        return browse(0, opcUaClient, browseRoot, null, (ref, parent) -> {
            final String name = ref.getBrowseName() != null ? ref.getBrowseName().getName() : "";
            final String displayName = ref.getDisplayName() != null ? ref.getDisplayName().getText() : "";
            final NodeType nodeType = getNodeType(ref);
            output.getNodeTree()
                    .addNode(ref.getNodeId().toParseableString(),
                            requireNonNullElse(name, ""),
                            requireNonNullElse(displayName, ""),
                            parent != null ? parent.getNodeId().toParseableString() : null,
                            nodeType != null ? nodeType : NodeType.VALUE,
                            nodeType != null && nodeType == NodeType.VALUE);
        }, input.getDepth());
    }

    @NotNull
    private OpcUaSubscriptionListener createSubscriptionListener() {
        return new OpcUaSubscriptionListener(protocolAdapterMetricsHelper, adapterConfig.getId(), (subscription) -> {
            //re-create a subscription on failure
            final OpcUaAdapterConfig.Subscription subscriptionConfig =
                    subscriptionMap.get(subscription.getSubscriptionId());
            if (subscriptionConfig != null) {
                try {
                    subscribeToNode(subscriptionConfig).get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Not able to recreate OPC-UA subscription after transfer failure", e);
                }
            }
        });
    }

    private CompletableFuture<Void> createAllSubscriptions() {
        //noinspection ConstantValue
        if (adapterConfig.getSubscriptions() == null || adapterConfig.getSubscriptions().isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        final CompletableFuture<Void> resultFuture = new CompletableFuture<>();
        final ImmutableList.Builder<CompletableFuture<Void>> subscribeFutures = ImmutableList.builder();

        for (OpcUaAdapterConfig.Subscription subscription : adapterConfig.getSubscriptions()) {
            subscribeFutures.add(subscribeToNode(subscription));
        }

        CompletableFuture.allOf(subscribeFutures.build().toArray(new CompletableFuture[]{})).thenApply(unused -> {
            resultFuture.complete(null);
            return null;
        }).exceptionally(throwable -> {
            setErrorConnectionStatus(throwable);
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
            public void onSessionInactive(final UaSession session) {
                setConnectionStatus(ConnectionStatus.DISCONNECTED);
            }
            @Override
            public void onSessionActive(final UaSession session) {
                setConnectionStatus(ConnectionStatus.CONNECTED);
            }
        });
        opcUaClient.addFaultListener(serviceFault -> {
            eventService.fireEvent(
                    eventBuilder(Event.SEVERITY.ERROR).
                            withPayload(Payload.fromObject(objectMapper, serviceFault.getResponseHeader().getServiceResult())).
                            withMessage("A Service Fault was Detected.").build());
        });
        setRuntimeStatus(RuntimeStatus.STARTED);
    }

    private @NotNull CompletableFuture<Void> subscribeToNode(final @NotNull OpcUaAdapterConfig.Subscription subscription) {
        try {

            final CompletableFuture<Void> resultFuture = new CompletableFuture<>();

            ReadValueId readValueId = new ReadValueId(NodeId.parse(subscription.getNode()),
                    AttributeId.Value.uid(),
                    null,
                    QualifiedName.NULL_VALUE);

            Objects.requireNonNull(opcUaClient)
                    .getSubscriptionManager()
                    .createSubscription(subscription.getPublishingInterval())
                    .thenAccept(new OpcUaSubscriptionConsumer(subscription,
                            readValueId,
                            adapterPublishService,
                            eventService,
                            resultFuture,
                            opcUaClient,
                            subscriptionMap,
                            protocolAdapterMetricsHelper,
                            adapterConfig.getId()));


            return resultFuture;
        } catch (Exception e) {
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
            int indent,
            final @NotNull OpcUaClient client,
            final @NotNull NodeId browseRoot,
            final @Nullable ReferenceDescription parent,
            final @NotNull BiConsumer<ReferenceDescription, ReferenceDescription> callback,
            final int depth) {
        BrowseDescription browse = new BrowseDescription(browseRoot,
                BrowseDirection.Forward,
                null,
                true,
                uint(0),
                uint(BrowseResultMask.All.getValue()));

        try {
            BrowseResult browseResult = client.browse(browse).get();
            return handleBrowseResult(indent, client, parent, callback, depth, browseResult);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Browsing nodeId={} failed: {}", browseRoot, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @NotNull
    private CompletableFuture<Void> handleBrowseResult(
            int indent,
            @NotNull OpcUaClient client,
            @Nullable ReferenceDescription parent,
            @NotNull BiConsumer<ReferenceDescription, ReferenceDescription> callback,
            int depth,
            BrowseResult browseResult) throws InterruptedException, ExecutionException {
        ReferenceDescription[] references = browseResult.getReferences();

        final ImmutableList.Builder<CompletableFuture<Void>> childFutures = ImmutableList.builder();

        if (references == null) {
            return CompletableFuture.completedFuture(null);
        }
        for (ReferenceDescription rd : references) {
            callback.accept(rd, parent);
            // recursively browse to children
            if (depth > 1) {
                final Optional<NodeId> childNodeId = rd.getNodeId().toNodeId(client.getNamespaceTable());
                childNodeId.ifPresent(nodeId -> childFutures.add(browse(indent + 1,
                        client,
                        nodeId,
                        rd,
                        callback,
                        depth - 1)));
            }
        }

        final ByteString continuationPoint = browseResult.getContinuationPoint();
        if (continuationPoint != null && !continuationPoint.isNull()) {
            final BrowseResult nextBrowseResult =
                    Objects.requireNonNull(opcUaClient).browseNext(false, continuationPoint).get();
            handleBrowseResult(indent, opcUaClient, parent, callback, depth, nextBrowseResult);
        }

        return CompletableFuture.allOf(childFutures.build().toArray(new CompletableFuture[]{}));
    }

}
