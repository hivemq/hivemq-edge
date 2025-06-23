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
import com.hivemq.adapter.sdk.api.discovery.NodeTree;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryInput;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationInput;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationOutput;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.writing.WritingContext;
import com.hivemq.adapter.sdk.api.writing.WritingInput;
import com.hivemq.adapter.sdk.api.writing.WritingOutput;
import com.hivemq.adapter.sdk.api.writing.WritingPayload;
import com.hivemq.adapter.sdk.api.writing.WritingProtocolAdapter;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.southbound.JsonSchemaGenerator;
import com.hivemq.edge.adapters.opcua.southbound.JsonToOpcUAConverter;
import com.hivemq.edge.adapters.opcua.southbound.OpcUaPayload;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.CONNECTED;
import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.DISCONNECTED;
import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.ERROR;

public class OpcUaProtocolAdapter implements WritingProtocolAdapter {
    private static final @NotNull Logger log = LoggerFactory.getLogger(OpcUaProtocolAdapter.class);

    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull String adapterId;
    private final @NotNull OpcUaClientConnection opcUaClientConnection;
    private final @NotNull Map<String, OpcuaTag> tagNameToTag;
    private final @NotNull AtomicLong requestCounter;
    private final @NotNull AtomicBoolean stopRequested;
    private final @NotNull AtomicBoolean startRequested;

    public OpcUaProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<OpcUaSpecificAdapterConfig> input) {
        this.adapterId = input.getAdapterId();
        this.adapterInformation = adapterInformation;
        this.protocolAdapterState = input.getProtocolAdapterState();
        final List<OpcuaTag> tagList = input.getTags().stream().map(tag -> (OpcuaTag) tag).toList();
        this.tagNameToTag = tagList.stream().collect(Collectors.toMap(OpcuaTag::getName, Function.identity()));
        this.requestCounter = new AtomicLong();
        this.stopRequested = new AtomicBoolean(false);
        this.startRequested = new AtomicBoolean(false);
        this.opcUaClientConnection = new OpcUaClientConnection(adapterId, input, tagList, protocolAdapterState);
    }

    @Override
    public @NotNull String getId() {
        return adapterId;
    }

    @Override
    public void start(
            final @NotNull ProtocolAdapterStartInput input,
            final @NotNull ProtocolAdapterStartOutput output) {
        if (!stopRequested.get() && startRequested.compareAndSet(false, true)) {
            log.info("Starting OPC UA protocol adapter {}", adapterId);
            opcUaClientConnection.start().whenComplete((ignore, throwable) -> {
                if (throwable == null) {
                    protocolAdapterState.setConnectionStatus(CONNECTED);
                    output.startedSuccessfully();
                    log.info("Successfully started OPC UA protocol adapter {}", adapterId);
                } else {
                    protocolAdapterState.setConnectionStatus(ERROR);
                    startRequested.set(false);
                    output.failStart(throwable, "Unable to connect and subscribe to the OPC UA server");
                    log.error("Unable to connect and subscribe to the OPC UA server", throwable);
                }
            });
        }
    }

    @Override
    public void stop(final @NotNull ProtocolAdapterStopInput input, final @NotNull ProtocolAdapterStopOutput output) {
        if (startRequested.get() && stopRequested.compareAndSet(false, true)) {
            log.info("Stopping OPC UA protocol adapter {}", adapterId);
            opcUaClientConnection.stop(requestCounter).whenComplete((v, throwable) -> {
                try {
                    if (throwable == null) {
                        protocolAdapterState.setConnectionStatus(DISCONNECTED);
                        output.stoppedSuccessfully();
                        log.info("Successfully stopped OPC UA protocol adapter {}", adapterId);
                    } else {
                        protocolAdapterState.setConnectionStatus(ERROR);
                        output.failStop(throwable, "Unable to stop the connection to the OPC UA server");
                        log.error("Unable to stop the connection to the OPC UA server", throwable);
                    }
                } finally {
                    startRequested.set(false);
                    stopRequested.set(false);
                }
            });
        }
    }

    @Override
    public void discoverValues(
            final @NotNull ProtocolAdapterDiscoveryInput input,
            final @NotNull ProtocolAdapterDiscoveryOutput output) {
        if (input.getRootNode() == null) {
            log.error("Discovery failed: Root node is null");
            output.fail("Root node is null");
            return;
        }

        final OpcUaClient client = opcUaClientConnection.client();
        if (client != null && startRequested.get() && !stopRequested.get()) {
            requestCounter.incrementAndGet();
            OpcUaNodeDiscovery.discoverValues(client, input.getRootNode(), input.getDepth())
                    .whenComplete((collectedNodes, throwable) -> {
                        try {
                            if (throwable == null) {
                                final NodeTree nodeTree = output.getNodeTree();
                                collectedNodes.forEach(node -> nodeTree.addNode(node.id(),
                                        node.name(),
                                        node.value(),
                                        node.description(),
                                        node.parentId(),
                                        node.nodeType(),
                                        node.selectable()));
                                output.finish();
                            } else {
                                log.error("Unable to discover the OPC UA server", throwable);
                                output.fail(throwable, "Unable to discover values");
                            }
                        } finally {
                            requestCounter.decrementAndGet();
                        }
                    });
        } else {
            log.warn("Cannot perform discovery: OPC UA client is not connected.");
        }
    }

    @Override
    public void write(final @NotNull WritingInput input, final @NotNull WritingOutput output) {
        final WritingContext writeContext = input.getWritingContext();
        final OpcUaPayload opcUAWritePayload = (OpcUaPayload) input.getWritingPayload();
        final String tagName = writeContext.getTagName();
        final OpcuaTag opcuaTag = tagNameToTag.get(tagName);
        if (opcuaTag == null) {
            log.error("Attempted to write to non-existent tag '{}'", tagName);
            output.fail("Tag '" + tagName + "' not found.");
            return;
        }

        final OpcUaClient client = opcUaClientConnection.client();
        if (client != null && startRequested.get() && !stopRequested.get()) {
            final JsonToOpcUAConverter converter = new JsonToOpcUAConverter(client);
            if (log.isDebugEnabled()) {
                log.debug("Write invoked with payload '{}' for tag '{}'", opcUAWritePayload, opcuaTag.getName());
            }

            final NodeId nodeId = NodeId.parse(opcuaTag.getDefinition().getNode());
            final Object opcuaObject = converter.convertToOpcUAValue(opcUAWritePayload.value(), nodeId);

            requestCounter.incrementAndGet();
            client.writeValuesAsync(List.of(nodeId),
                            List.of(new DataValue(Variant.of(opcuaObject), StatusCode.GOOD, null)))
                    .whenComplete((statusCodes, throwable) -> {
                        try {
                            final var badStatus = statusCodes.stream().filter(StatusCode::isBad).findFirst();
                            if (badStatus.isPresent()) {
                                log.error("Failed to write tag '{}': {}", tagName, badStatus.get());
                                output.fail("Failed to write tag '" + tagName + "': " + badStatus.get());
                            } else if (throwable == null) {
                                log.debug("Successfully wrote tag '{}'", opcuaTag.getName());
                                output.finish();
                            } else {
                                log.error("Exception while writing tag '{}'", tagName, throwable);
                                output.fail(throwable, null);
                            }
                        } finally {
                            requestCounter.decrementAndGet();
                        }
                    });
        } else {
            log.warn("Cannot perform write: OPC UA client is not connected.");
        }
    }

    @Override
    public void createTagSchema(
            final @NotNull TagSchemaCreationInput input,
            final @NotNull TagSchemaCreationOutput output) {
        final String tagName = input.getTagName();
        final OpcuaTag tag = tagNameToTag.get(tagName);
        if (tag == null) {
            log.error("Cannot create schema for non-existent tag '{}'", tagName);
            output.fail("Tag '" + tagName + "' not found.");
            return;
        }

        final OpcUaClient client = opcUaClientConnection.client();
        if (client != null) {
            requestCounter.incrementAndGet();
            new JsonSchemaGenerator(client).createMqttPayloadJsonSchema(tag).whenComplete((result, throwable) -> {
                try {
                    if (throwable == null) {
                        result.ifPresentOrElse(schema -> {
                            log.debug("Schema inferred for tag='{}'", tagName);
                            output.finish(schema);
                        }, () -> {
                            log.error("No schema inferred for tag='{}'", tagName);
                            output.fail("No schema inferred for tag='" + tagName + "'");
                        });
                    } else {
                        log.error("Exception while creating tag schema for '{}'", tagName, throwable);
                        output.fail(throwable, null);
                    }
                } finally {
                    requestCounter.decrementAndGet();
                }
            });
        } else {
            log.warn("Cannot create tag schema: OPC UA client is not connected.");
        }
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

    @Override
    public @NotNull Class<? extends WritingPayload> getMqttPayloadClass() {
        return OpcUaPayload.class;
    }

    @VisibleForTesting
    public @NotNull ProtocolAdapterState getProtocolAdapterState() {
        return protocolAdapterState;
    }
}
