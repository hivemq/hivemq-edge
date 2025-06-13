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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.CONNECTED;
import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.DISCONNECTED;
import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.ERROR;

public class OpcUaProtocolAdapter implements WritingProtocolAdapter {
    private static final @NotNull Logger log = LoggerFactory.getLogger(OpcUaProtocolAdapter.class);

    private static final long STOP_WAIT_MILLIS = 30 * 1000;
    private static final long STOP_POLL_MILLIS = 100;

    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull String adapterId;
    private final @NotNull OpcUaClientConnection opcUaClientConnection;
    private final @NotNull Map<String, OpcuaTag> tagNameToTag;

    private final @NotNull ReentrantLock lock;
    private final @NotNull AtomicLong requestCounter;
    private final @NotNull AtomicBoolean stopRequested;

    public OpcUaProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<OpcUaSpecificAdapterConfig> input) {
        this.adapterId = input.getAdapterId();
        this.adapterInformation = adapterInformation;
        this.protocolAdapterState = input.getProtocolAdapterState();
        final List<OpcuaTag> tagList = input.getTags().stream().map(tag -> (OpcuaTag) tag).toList();
        this.tagNameToTag = tagList.stream().collect(Collectors.toMap(OpcuaTag::getName, tag -> tag));
        this.opcUaClientConnection = new OpcUaClientConnection(input.getConfig().getUri(),
                tagList,
                input.getConfig(),
                input.moduleServices().protocolAdapterTagStreamingService(),
                input.adapterFactories().dataPointFactory(),
                input.moduleServices().eventService(),
                input.getProtocolAdapterMetricsHelper(),
                adapterId,
                protocolAdapterState);

        this.lock = new ReentrantLock();
        this.requestCounter = new AtomicLong();
        this.stopRequested = new AtomicBoolean();
    }

    @Override
    public @NotNull String getId() {
        return adapterId;
    }

    @Override
    public void start(
            final @NotNull ProtocolAdapterStartInput input,
            final @NotNull ProtocolAdapterStartOutput output) {
        lock.lock();
        try {
            log.info("Starting OpcUa protocol adapter {}", adapterId);
            try {
                opcUaClientConnection.start();
                protocolAdapterState.setConnectionStatus(CONNECTED);
                output.startedSuccessfully();
                log.info("Successfully started OpcUa protocol adapter {}", adapterId);
            } catch (final Throwable t) {
                protocolAdapterState.setConnectionStatus(ERROR);
                log.error("Unable to connect and subscribe to the OPC UA server", t);
                output.failStart(t, "Unable to connect and subscribe to the OPC UA server");
            }
        } finally {
            requestCounter.set(0);
            lock.unlock();
            stopRequested.set(false);
        }
    }

    @Override
    public void stop(final @NotNull ProtocolAdapterStopInput input, final @NotNull ProtocolAdapterStopOutput output) {
        if (stopRequested.compareAndSet(false, true)) {
            lock.lock();
            try {
                log.info("Stopping OpcUa protocol adapter {}", adapterId);
                final long startTime = System.currentTimeMillis();
                while (requestCounter.get() > 0 && (System.currentTimeMillis() - startTime) < STOP_WAIT_MILLIS) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(STOP_POLL_MILLIS);
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                opcUaClientConnection.stop();
                protocolAdapterState.setConnectionStatus(DISCONNECTED);
                output.stoppedSuccessfully();
                log.info("Successfully stopped OpcUa protocol adapter {}", adapterId);
            } catch (final Throwable t) {
                protocolAdapterState.setConnectionStatus(ERROR);
                output.failStop(t, "Unable to stop the connection to the OPC UA server");
                log.error("Unable to stop the connection to the OPC UA server", t);
            } finally {
                requestCounter.set(0);
                lock.unlock();
                stopRequested.set(false);
            }
        }
    }

    @Override
    public void discoverValues(
            final @NotNull ProtocolAdapterDiscoveryInput input,
            final @NotNull ProtocolAdapterDiscoveryOutput output) {
        if (stopRequested.get()) {
            return;
        }

        if (input.getRootNode() == null) {
            log.error("Discovery failed: Root node is null");
            output.fail("Root node is null");
            return;
        }

        lock.lock();
        try {
            requestCounter.incrementAndGet();
            OpcUaNodeDiscovery.discoverValues(opcUaClientConnection.client(), input.getRootNode(), input.getDepth())
                    .whenComplete((collectedNodes, throwable) -> {
                        if (throwable != null) {
                            log.error("Unable to discover the OPC UA server", throwable);
                            output.fail(throwable, "Unable to discover values");
                        } else {
                            final NodeTree nodeTree = output.getNodeTree();
                            collectedNodes.forEach(node -> nodeTree.addNode(node.id(),
                                    node.name(),
                                    node.value(),
                                    node.description(),
                                    node.parentId(),
                                    node.nodeType(),
                                    node.selectable()));
                            output.finish();
                        }
                        requestCounter.decrementAndGet();
                    });
        } finally {
            lock.unlock();
        }
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

    @Override
    public void write(final @NotNull WritingInput input, final @NotNull WritingOutput output) {
        if (stopRequested.get()) {
            return;
        }

        final WritingContext writeContext = input.getWritingContext();
        final OpcUaPayload opcUAWritePayload = (OpcUaPayload) input.getWritingPayload();
        final String tn = writeContext.getTagName();
        final var opcuaTag = tagNameToTag.get(writeContext.getTagName());
        if (opcuaTag == null) {
            log.error("Tried executing write with a non existent tag '{}'", tn);
            return;
        }

        lock.lock();
        try {
            final OpcUaClient client = opcUaClientConnection.client();

            final JsonToOpcUAConverter converter = new JsonToOpcUAConverter(client);
            if (log.isDebugEnabled()) {
                log.debug("Write for OPC UA is invoked with payload '{}' for tag '{}' ",
                        opcUAWritePayload,
                        opcuaTag.getName());
            }
            final NodeId nodeId = NodeId.parse(opcuaTag.getDefinition().getNode());
            final Object opcuaObject = converter.convertToOpcUAValue(opcUAWritePayload.value(), nodeId);

            requestCounter.incrementAndGet();
            client.writeValuesAsync(List.of(nodeId),
                            List.of(new DataValue(Variant.of(opcuaObject), StatusCode.GOOD, null)))
                    .whenComplete((statusCode, throwable) -> {
                        final var badStatus = statusCode.stream().filter(StatusCode::isBad).findFirst();
                        badStatus.ifPresentOrElse(bad -> {
                            log.error("Failed to write tag '{}': {}", tn, bad);
                            output.fail("Failed to write tag '" + tn + "': " + bad);
                        }, () -> {
                            if (throwable != null) {
                                log.error("Exception while writing tag '{}'", tn, throwable);
                                output.fail(throwable, null);
                            } else {
                                log.debug("Wrote tag='{}'", opcuaTag.getName());
                                output.finish();
                            }
                        });
                        requestCounter.decrementAndGet();
                    });
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void createTagSchema(
            final @NotNull TagSchemaCreationInput input,
            final @NotNull TagSchemaCreationOutput output) {
        if (stopRequested.get()) {
            return;
        }

        final String tn = input.getTagName();
        final var tag = tagNameToTag.get(tn);

        lock.lock();
        try {
            requestCounter.incrementAndGet();
            new JsonSchemaGenerator(opcUaClientConnection.client()).createMqttPayloadJsonSchema(tag)
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            log.error("Exception while creating tag schema '{}'", tn, throwable);
                            output.fail(throwable, null);
                        } else {
                            log.debug("Created tag schema='{}'", input.getTagName());
                            result.ifPresentOrElse(schema -> {
                                log.debug("Schema inferred for tag='{}'", input.getTagName());
                                output.finish(schema);
                            }, () -> {
                                log.error("No schema inferred for tag='{}'", input.getTagName());
                                output.fail("No schema inferred for tag='{}'");
                            });
                        }
                        requestCounter.decrementAndGet();
                    });
        } finally {
            lock.unlock();
        }
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
