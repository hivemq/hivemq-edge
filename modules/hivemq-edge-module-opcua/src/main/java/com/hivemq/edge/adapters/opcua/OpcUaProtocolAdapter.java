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
import com.hivemq.edge.adapters.opcua.southbound.OpcUaPayload;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OpcUaProtocolAdapter implements WritingProtocolAdapter {
    private static final @NotNull Logger log = LoggerFactory.getLogger(OpcUaProtocolAdapter.class);

    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull String adapterId;
    private final @NotNull OpcUaClientConnection opcUaClientConnection;
    private final @NotNull Map<String, OpcuaTag> tagNameToTag;

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
    }

    @Override
    public @NotNull String getId() {
        return adapterId;
    }

    @Override
    public void start(
            final @NotNull ProtocolAdapterStartInput input,
            final @NotNull ProtocolAdapterStartOutput output) {
        log.info("Starting OpcUa protocol adapter {}", adapterId);
        opcUaClientConnection.start().whenComplete((ignored, throwable) -> {
            if (throwable != null) {
                log.error("Unable to connect and subscribe to the OPC UA server", throwable);
                output.failStart(throwable, "Unable to connect and subscribe to the OPC UA server");
            } else {
                log.info("Successfully started OpcUa protocol adapter {}", adapterId);
                output.startedSuccessfully();
            }
        });
    }

    @Override
    public void stop(final @NotNull ProtocolAdapterStopInput input, final @NotNull ProtocolAdapterStopOutput output) {
        log.info("Stopping OpcUa protocol adapter {}", adapterId);
        opcUaClientConnection.stop().whenComplete((ignored, throwable) -> {
            if (throwable != null) {
                log.error("Unable to stop the connection to the OPC UA server", throwable);
                output.failStop(throwable, "Unable to stop the connection to the OPC UA server");
            } else {
                log.info("Successfully stopped OpcUa protocol adapter {}", adapterId);
                output.stoppedSuccessfully();
            }
        });
    }

    @Override
    public void discoverValues(
            final @NotNull ProtocolAdapterDiscoveryInput input,
            final @NotNull ProtocolAdapterDiscoveryOutput output) {
        if (input.getRootNode() != null) {
            opcUaClientConnection.discoverValues(input.getRootNode(), input.getDepth())
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
                    });
        } else {
            log.error("Discovery failed: Root node is null");
            output.fail("Root node is null");
        }
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

    @Override
    public void write(final @NotNull WritingInput input, final @NotNull WritingOutput output) {
        final WritingContext writeContext = input.getWritingContext();
        final var opcuaTag = tagNameToTag.get(writeContext.getTagName());
        if (opcUaClientConnection.isStarted()) {
            if (opcuaTag != null) {
                opcUaClientConnection.write(opcuaTag, (OpcUaPayload) input.getWritingPayload())
                        .whenComplete((statusCode, throwable) -> {
                            final var badStatus = statusCode.stream().filter(StatusCode::isBad).findFirst();
                            badStatus.ifPresentOrElse(bad -> {
                                log.error("Failed to write tag '{}': {}", writeContext.getTagName(), bad);
                                output.fail("Failed to write tag '" + writeContext.getTagName() + "': " + bad);
                            }, () -> {
                                if (throwable != null) {
                                    log.error("Exception while writing tag '{}'", writeContext.getTagName(), throwable);
                                    output.fail(throwable, null);
                                } else {
                                    log.debug("Wrote tag='{}'", opcuaTag.getName());
                                    output.finish();
                                }
                            });
                        });
            } else {
                log.error("Tried executing write with a non existent tag '{}'", writeContext.getTagName());
            }
        } else {
            log.warn("Tried executing write while client wasn't started");
        }
    }

    @Override
    public void createTagSchema(
            final @NotNull TagSchemaCreationInput input,
            final @NotNull TagSchemaCreationOutput output) {
        final var tag = tagNameToTag.get(input.getTagName());
        opcUaClientConnection.createTagSchema(tag).whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error("Exception while creating tag schema '{}'", input.getTagName(), throwable);
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
        });
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
