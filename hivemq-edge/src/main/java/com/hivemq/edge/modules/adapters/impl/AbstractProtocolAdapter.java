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
package com.hivemq.edge.modules.adapters.impl;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.hivemq.api.model.status.Status;
import com.hivemq.edge.modules.adapters.ProtocolAdapterException;
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterDataSample;
import com.hivemq.edge.modules.adapters.metrics.ProtocolAdapterMetricsHelper;
import com.hivemq.edge.modules.adapters.params.NodeTree;
import com.hivemq.edge.modules.adapters.params.ProtocolAdapterDiscoveryInput;
import com.hivemq.edge.modules.adapters.params.ProtocolAdapterDiscoveryOutput;
import com.hivemq.edge.modules.adapters.params.ProtocolAdapterStartInput;
import com.hivemq.edge.modules.adapters.params.ProtocolAdapterStartOutput;
import com.hivemq.edge.modules.api.adapters.ModuleServices;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapter;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterCapability;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterInformation;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPollingService;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPublishService;
import com.hivemq.edge.modules.config.impl.AbstractProtocolAdapterConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * The abstract adapter contains the baseline coupling and basic lifecycle operations of
 * a given protocol adapter in the system.
 *
 * It will provide services to manage polling, publishing and standardised metrics which
 * will provide a cohesive runtime lifecycle of all the running instances
 *
 * @author Simon L Johnson
 */
public abstract class AbstractProtocolAdapter<T extends AbstractProtocolAdapterConfig>
        implements ProtocolAdapter {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final @NotNull ProtocolAdapterInformation adapterInformation;
    protected final @NotNull ObjectMapper objectMapper;

    protected @Nullable ProtocolAdapterPublishService adapterPublishService;
    protected @NotNull ProtocolAdapterMetricsHelper protocolAdapterMetricsHelper;

    protected @NotNull T adapterConfig;
    protected @Nullable Long lastStartAttemptTime;
    protected @Nullable String lastErrorMessage;
    protected @Nullable volatile Object lock = new Object();
    protected @Nullable volatile RuntimeStatus runtimeStatus = RuntimeStatus.STOPPED;
    protected @Nullable volatile ConnectionStatus connectionStatus = ConnectionStatus.DISCONNECTED;

    public AbstractProtocolAdapter(final @NotNull ProtocolAdapterInformation adapterInformation,
                                   final @NotNull T adapterConfig,
                                   final @NotNull MetricRegistry metricRegistry) {
        Preconditions.checkNotNull(adapterInformation);
        Preconditions.checkNotNull(adapterConfig);
        Preconditions.checkNotNull(metricRegistry);
        this.adapterInformation = adapterInformation;
        this.adapterConfig = adapterConfig;
        this.protocolAdapterMetricsHelper = new ProtocolAdapterMetricsHelper(adapterInformation.getProtocolId(),
                adapterConfig.getId(), metricRegistry);
        this.objectMapper = new ObjectMapper();
    }

    public ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

    /**
     * Converts the supplied object into a valid JSON document which wraps a new timestamp and the
     * supplied object as the value
     * @param data - The data you wish to wrap into the standard JSONB envelope
     * @return a valid JSON document encoded to UTF-8 with the supplied value wrapped as an attribute on the envelope
     */
    public byte[] convertToJson(final @NotNull ProtocolAdapterDataSample data) throws ProtocolAdapterException {
        try {
            Preconditions.checkNotNull(data);
            ProtocolAdapterPublisherJsonPayload payload = new ProtocolAdapterPublisherJsonPayload();
            payload.setValue(data);
            if(data.getTimestamp() > 0){
                payload.setTimestamp(data.getTimestamp());
            } else {
                payload.setTimestamp(System.currentTimeMillis());
            }
            return objectMapper.writeValueAsBytes(payload);
        } catch(JsonProcessingException e){
            throw new ProtocolAdapterException("Error Wrapping Adapter Data", e);
        }
    }

    protected void bindServices(final @NotNull ModuleServices moduleServices){
        Preconditions.checkNotNull(moduleServices);
        if(adapterPublishService != null){
            adapterPublishService = moduleServices.adapterPublishService();
        }
    }

    protected void initStartAttempt(){
        lastStartAttemptTime = System.currentTimeMillis();
    }

    protected void setLastErrorMessage(String lastErrorMessage){
        this.lastErrorMessage = lastErrorMessage;
    }

    @Override
    public Long getTimeOfLastStartAttempt() {
        return lastStartAttemptTime;
    }

    @Override
    public CompletableFuture<Void> discoverValues(final @NotNull ProtocolAdapterDiscoveryInput input,
                                                  final @NotNull ProtocolAdapterDiscoveryOutput output) {
        if(ProtocolAdapterCapability.supportsCapability(
                getProtocolAdapterInformation(), ProtocolAdapterCapability.DISCOVER)){
            return CompletableFuture.failedFuture(new UnsupportedOperationException("Adapter type does not support discovery"));
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    @Override
    public CompletableFuture<Void> start(
            final ProtocolAdapterStartInput input,
            final ProtocolAdapterStartOutput output) {
        CompletableFuture<Void> future = null;
        synchronized (lock){
            try {
                bindServices(input.moduleServices());
                initStartAttempt();
                future = startInternal(input, output);
                return future;
            } finally {
                if(future != null){
                    future.thenRun(() -> setRuntimeStatus(RuntimeStatus.STARTED));
                }
            }
        }
    }

    @Override
    public CompletableFuture<Void> stop() {
        CompletableFuture<Void> future = null;
        synchronized (lock){
            try {
                future = stopInternal();
                return future;
            } finally {
                if(future != null){
                    future.thenRun(() -> setRuntimeStatus(RuntimeStatus.STOPPED));
                }
            }
        }
    }

    protected void setConnectionStatus(@NotNull final ConnectionStatus connectionStatus){
        Preconditions.checkNotNull(connectionStatus);
        synchronized (lock){
            this.connectionStatus = connectionStatus;
        }
    }

    protected void setErrorConnectionStatus(@NotNull final String errorMessage){
        Preconditions.checkNotNull(errorMessage);
        synchronized (lock){
            this.connectionStatus = ConnectionStatus.ERROR;
            setErrorConnectionStatus(errorMessage);
        }
    }

    protected void setRuntimeStatus(@NotNull final RuntimeStatus runtimeStatus){
        Preconditions.checkNotNull(runtimeStatus);
        synchronized (lock){
            this.runtimeStatus = runtimeStatus;
            if(runtimeStatus == RuntimeStatus.STARTED){
                initStartAttempt();
            }
        }
    }

    @Override
    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public @NotNull T getAdapterConfig() {
        return adapterConfig;
    }
    @Override
    public @NotNull String getId() {
        return adapterConfig.getId();
    }

    @Override
    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    @Override
    public RuntimeStatus getRuntimeStatus() {
        return runtimeStatus;
    }

    protected abstract CompletableFuture<Void> startInternal(final ProtocolAdapterStartInput input, final ProtocolAdapterStartOutput output);

    protected abstract CompletableFuture<Void> stopInternal();
}
