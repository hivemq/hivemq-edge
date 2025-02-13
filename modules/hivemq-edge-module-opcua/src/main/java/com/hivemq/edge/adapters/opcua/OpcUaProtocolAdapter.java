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
import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryInput;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryOutput;
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
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.adapter.sdk.api.writing.WritingContext;
import com.hivemq.adapter.sdk.api.writing.WritingInput;
import com.hivemq.adapter.sdk.api.writing.WritingOutput;
import com.hivemq.adapter.sdk.api.writing.WritingPayload;
import com.hivemq.adapter.sdk.api.writing.WritingProtocolAdapter;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.mqtt2opcua.OpcUaPayload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.CONNECTED;
import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.DISCONNECTED;

public class OpcUaProtocolAdapter implements WritingProtocolAdapter {
    private static final @NotNull Logger log = LoggerFactory.getLogger(OpcUaProtocolAdapter.class);

    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull OpcUaSpecificAdapterConfig adapterConfig;
    private final @NotNull List<Tag> tags;
    private final @NotNull List<PollingContext> northboundMappings;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;
    private final @NotNull ModuleServices moduleServices;
    private final @NotNull String adapterId;
    private volatile @Nullable OpcUaClientWrapper opcUaClientWrapper;

    public OpcUaProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<OpcUaSpecificAdapterConfig> input) {
        this.adapterId = input.getAdapterId();
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.tags = input.getTags();
        this.protocolAdapterMetricsService = input.getProtocolAdapterMetricsHelper();
        this.moduleServices = input.moduleServices();
        this.northboundMappings = input.getPollingContexts();
    }

    @Override
    public @NotNull String getId() {
        return adapterId;
    }

    @Override
    public void start(
            final @NotNull ProtocolAdapterStartInput input, final @NotNull ProtocolAdapterStartOutput output) {
        if (opcUaClientWrapper == null) {
            synchronized (this) {
                if (opcUaClientWrapper == null) {
                    try {
                        OpcUaClientWrapper.createAndConnect(adapterId,
                                adapterConfig,
                                tags,
                                northboundMappings,
                                protocolAdapterState,
                                moduleServices.eventService(),
                                moduleServices.adapterPublishService(),
                                adapterInformation.getProtocolId(),
                                protocolAdapterMetricsService,
                                output).thenApply(wrapper -> {
                            protocolAdapterState.setConnectionStatus(CONNECTED);
                            output.startedSuccessfully();
                            opcUaClientWrapper = wrapper;
                            return wrapper;
                        }).exceptionally(throwable -> {
                            log.error("Not able to connect and subscribe to OPC UA server {}",
                                    adapterConfig.getUri(),
                                    throwable);
                            protocolAdapterState.setErrorConnectionStatus(throwable, null);
                            output.failStart(throwable, throwable.getMessage());
                            return null;
                        });
                    } catch (final Exception e) {
                        log.error("Not able to start OPC UA client for server {}", adapterConfig.getUri(), e);
                        protocolAdapterState.setConnectionStatus(DISCONNECTED);
                        output.failStart(e, "Not able to start OPC UA client for server " + adapterConfig.getUri());
                    }
                }
            }
        } else {
            throw new IllegalStateException("Already started");
        }
    }

    @Override
    public void stop(final @NotNull ProtocolAdapterStopInput input, final @NotNull ProtocolAdapterStopOutput output) {
        if (opcUaClientWrapper != null) {
            synchronized (this) {
                if (opcUaClientWrapper != null) {
                    opcUaClientWrapper.stop().whenComplete((aVoid, t) -> {
                        if (t != null) {
                            protocolAdapterState.setErrorConnectionStatus(t, null);
                            output.failStop(t, null);
                        } else {
                            opcUaClientWrapper = null;
                            protocolAdapterState.setConnectionStatus(DISCONNECTED);
                            output.stoppedSuccessfully();
                        }
                    });
                }
            }
        } else {
            log.error("Tried to stop OPC UA client for server {} which wasn't started", adapterConfig.getUri());
        }
    }

    @Override
    public void discoverValues(
            final @NotNull ProtocolAdapterDiscoveryInput input, final @NotNull ProtocolAdapterDiscoveryOutput output) {
        final OpcUaClientWrapper opcUaClientWrapperTemp = opcUaClientWrapper;
        if (opcUaClientWrapperTemp != null) {
            opcUaClientWrapperTemp.discoverValues(input, output);
        } else {
            log.warn("Tried executing discoverValues while client wasn't started");
        }
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

    @Override
    public void write(final @NotNull WritingInput input, final @NotNull WritingOutput output) {
        final OpcUaClientWrapper opcUaClientWrapperTemp = opcUaClientWrapper;
        final WritingContext writeContext = input.getWritingContext();
        if (opcUaClientWrapperTemp != null) {
            tags.stream()
                    .filter(tag -> tag.getName().equals(writeContext.getTagName()))
                    .findFirst()
                    .ifPresentOrElse(def -> opcUaClientWrapperTemp.write(input, output, (OpcuaTag) def),
                            () -> output.fail("Subscription for protocol adapter failed because the used tag '" +
                                    writeContext.getTagName() +
                                    "' was not found. For the subscription to work the tag must be created via REST API or the UI."));
        } else {
            log.warn("Tried executing write while client wasn't started");
        }
    }

    @Override
    public void createTagSchema(
            final @NotNull TagSchemaCreationInput input, final @NotNull TagSchemaCreationOutput output) {
        final OpcUaClientWrapper opcUaClientWrapperTemp = opcUaClientWrapper;
        if (opcUaClientWrapperTemp != null) {
            tags.stream()
                    .filter(tag -> tag.getName().equals(input.getTagName()))
                    .findFirst()
                    .ifPresentOrElse(def -> opcUaClientWrapperTemp.createMqttPayloadJsonSchema((OpcuaTag) def, output),
                            () -> {
                                log.warn(
                                        "The tag '{}' was not found during creation of schema for tags on remote plc. Available tags: {}",
                                        input.getTagName(),
                                        tags);
                                output.tagNotFound(String.format(
                                        "The tag '%s' was not found during creation of schema for tags on remote plc. Available tags: '%s'",
                                        input.getTagName(),
                                        tags));
                            });
        } else {
            output.adapterNotStarted();
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
