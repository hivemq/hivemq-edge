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
import com.hivemq.adapter.sdk.api.config.ProtocolAdapterConfig;
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
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.List;

public class ProtocolAdapterWrapper<T extends ProtocolAdapter> {

    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;
    private final @NotNull T adapter;
    private final @NotNull ProtocolAdapterFactory<?> adapterFactory;
    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull ProtocolAdapterConfig configObject;
    private final @NotNull List<Tag> tags;
    protected @Nullable Long lastStartAttemptTime;

    public ProtocolAdapterWrapper(
            final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService,
            final @NotNull T adapter,
            final @NotNull ProtocolAdapterFactory<?> adapterFactory,
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterState protocolAdapterState,
            final @NotNull ProtocolAdapterConfig configObject,
            final @NotNull List<Tag> tags) {
        this.protocolAdapterMetricsService = protocolAdapterMetricsService;
        this.adapter = adapter;
        this.adapterFactory = adapterFactory;
        this.adapterInformation = adapterInformation;
        this.protocolAdapterState = protocolAdapterState;
        this.configObject = configObject;
        this.tags = tags;
    }

    public void start(
            @NotNull final ProtocolAdapterStartInput input, @NotNull final ProtocolAdapterStartOutput output) {
        initStartAttempt();
        adapter.start(input, output);
    }

    public void stop(@NotNull final ProtocolAdapterStopInput input, @NotNull final ProtocolAdapterStopOutput output) {
        adapter.stop(input, output);
    }

    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapter.getProtocolAdapterInformation();
    }

    public void discoverValues(
            @NotNull final ProtocolAdapterDiscoveryInput input, @NotNull final ProtocolAdapterDiscoveryOutput output) {
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

    public @NotNull ProtocolAdapterConfig getConfigObject() {
        return configObject;
    }

    public @NotNull List<Tag> getTags() {
        return tags;
    }

    public @NotNull Long getTimeOfLastStartAttempt() {
        return lastStartAttemptTime;
    }

    public @NotNull String getId() {
        return adapter.getId();
    }

    public @NotNull T getAdapter() {
        return adapter;
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
