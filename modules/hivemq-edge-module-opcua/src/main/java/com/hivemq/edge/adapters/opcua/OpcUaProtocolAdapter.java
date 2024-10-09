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
import com.google.common.annotations.VisibleForTesting;
import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryInput;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.writing.WritingInput;
import com.hivemq.adapter.sdk.api.writing.WritingOutput;
import com.hivemq.adapter.sdk.api.writing.WritingPayload;
import com.hivemq.adapter.sdk.api.writing.WritingProtocolAdapter;
import com.hivemq.edge.adapters.opcua.config.BidirectionalOpcUaAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.OpcUaAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.mqtt2opcua.MqttToOpcUaMapping;
import com.hivemq.edge.adapters.opcua.mqtt2opcua.OpcUaPayload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.DISCONNECTED;

public class OpcUaProtocolAdapter implements ProtocolAdapter, WritingProtocolAdapter<MqttToOpcUaMapping> {
    private static final @NotNull Logger log = LoggerFactory.getLogger(OpcUaProtocolAdapter.class);

    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull OpcUaAdapterConfig adapterConfig;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;
    private volatile @Nullable OpcUaClientWrapper opcUaClientWrapper ;

    public OpcUaProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<OpcUaAdapterConfig> input) {
        this.adapterInformation = adapterInformation;
        this.adapterConfig = input.getConfig();
        this.protocolAdapterState = input.getProtocolAdapterState();
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
        if(opcUaClientWrapper == null) {
            synchronized (this) {
                if(opcUaClientWrapper == null) {
                    try {
                        OpcUaClientWrapper.createAndConnect(adapterConfig,
                                protocolAdapterState,
                                input.moduleServices(),
                                adapterConfig.getId(),
                                adapterInformation.getProtocolId(),
                                protocolAdapterMetricsService).thenApply(wrapper -> {
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
        if(opcUaClientWrapper != null) {
            synchronized (this) {
                if(opcUaClientWrapper != null) {
                    opcUaClientWrapper.stop().whenComplete((aVoid, t) -> {
                        if (t != null) {
                            protocolAdapterState.setErrorConnectionStatus(t, null);
                            output.failStop(t, null);
                        } else {
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
            final @NotNull ProtocolAdapterDiscoveryInput input,
            final @NotNull ProtocolAdapterDiscoveryOutput output) {
        final OpcUaClientWrapper opcUaClientWrapperTemp = opcUaClientWrapper;
        if(opcUaClientWrapperTemp != null) {
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
        if(opcUaClientWrapperTemp != null) {
            opcUaClientWrapperTemp.write(input, output);
        } else {
            log.warn("Tried executing write while client wasn't started");
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
        final OpcUaClientWrapper opcUaClientWrapperTemp = opcUaClientWrapper;
        if(opcUaClientWrapperTemp != null) {
            return opcUaClientWrapperTemp.createMqttPayloadJsonSchema(writeContext);
        } else {
            log.warn("Tried executing createMqttPayloadJsonSchema while client wasn't started");
            return CompletableFuture.failedFuture(new IllegalStateException("Tried executing createMqttPayloadJsonSchema while client wasn't started"));
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
