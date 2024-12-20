/*
 * Copyright 2019-present HiveMQ GmbH
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
package com.hivemq.protocols;

import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryInput;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryOutput;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.hivemq.persistence.mappings.NorthboundMapping;
import com.hivemq.persistence.mappings.SouthboundMapping;

import java.util.List;

public class ProtocolAdapterWrapper {

    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;
    private final @NotNull ProtocolAdapter adapter;
    private final @NotNull ProtocolAdapterFactory<?> adapterFactory;
    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull ProtocolSpecificAdapterConfig configObject;
    private final @NotNull List<? extends Tag> tags;
    private final @NotNull List<SouthboundMapping> southboundMappings;
    private final @NotNull List<NorthboundMapping> northboundMappings;
    protected @Nullable Long lastStartAttemptTime;

    public ProtocolAdapterWrapper(
            final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService,
            final @NotNull ProtocolAdapter adapter,
            final @NotNull ProtocolAdapterFactory<?> adapterFactory,
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterState protocolAdapterState,
            final @NotNull ProtocolSpecificAdapterConfig configObject,
            final @NotNull List<? extends Tag> tags,
            final @NotNull List<SouthboundMapping> southboundMappings,
            final @NotNull List<NorthboundMapping> northboundMappings) {
        this.protocolAdapterMetricsService = protocolAdapterMetricsService;
        this.adapter = adapter;
        this.adapterFactory = adapterFactory;
        this.adapterInformation = adapterInformation;
        this.protocolAdapterState = protocolAdapterState;
        this.configObject = configObject;
        this.tags = tags;
        this.southboundMappings = southboundMappings;
        this.northboundMappings = northboundMappings;
    }

    public void start(
            final @NotNull ProtocolAdapterStartInput input, final @NotNull ProtocolAdapterStartOutput output) {
        initStartAttempt();
        adapter.start(input, output);
    }

    public void stop(final @NotNull ProtocolAdapterStopInput input, final @NotNull ProtocolAdapterStopOutput output) {
        adapter.stop(input, output);
    }

    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapter.getProtocolAdapterInformation();
    }

    public void discoverValues(
            final @NotNull ProtocolAdapterDiscoveryInput input, final @NotNull ProtocolAdapterDiscoveryOutput output) {
        adapter.discoverValues(input, output);
    }

    public @NotNull ProtocolAdapterState.ConnectionStatus getConnectionStatus() {
        return protocolAdapterState.getConnectionStatus();
    }

    public @NotNull ProtocolAdapterState.RuntimeStatus getRuntimeStatus() {
        return protocolAdapterState.getRuntimeStatus();
    }

    public @Nullable String getErrorMessage() {
        return protocolAdapterState.getLastErrorMessage();
    }

    protected void initStartAttempt() {
        lastStartAttemptTime = System.currentTimeMillis();
    }

    public @NotNull ProtocolAdapterFactory<?> getAdapterFactory() {
        return adapterFactory;
    }

    public @NotNull ProtocolAdapterInformation getAdapterInformation() {
        return adapterInformation;
    }

    public @NotNull ProtocolSpecificAdapterConfig getConfigObject() {
        return configObject;
    }

    public @NotNull List<? extends Tag> getTags() {
        return tags;
    }

    public @NotNull Long getTimeOfLastStartAttempt() {
        return lastStartAttemptTime;
    }

    public @NotNull String getId() {
        return adapter.getId();
    }

    public @NotNull ProtocolAdapter getAdapter() {
        return adapter;
    }

    public @NotNull List<NorthboundMapping> getFromEdgeMappings() {
        return northboundMappings;
    }

    public @NotNull List<SouthboundMapping> getToEdgeMappings() {
        return southboundMappings;
    }

    public @NotNull ProtocolAdapterMetricsService getProtocolAdapterMetricsService() {
        return protocolAdapterMetricsService;
    }

    public void setErrorConnectionStatus(final @NotNull Throwable exception, final @Nullable String errorMessage) {
        protocolAdapterState.setErrorConnectionStatus(exception, errorMessage);
    }

    public void setRuntimeStatus(final @NotNull ProtocolAdapterState.RuntimeStatus runtimeStatus) {
        protocolAdapterState.setRuntimeStatus(runtimeStatus);
    }
}
