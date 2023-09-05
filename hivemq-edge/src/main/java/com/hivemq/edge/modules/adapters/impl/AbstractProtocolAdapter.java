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
import com.hivemq.edge.modules.adapters.ProtocolAdapterException;
import com.hivemq.edge.modules.adapters.metrics.ProtocolAdapterMetricsHelper;
import com.hivemq.edge.modules.api.adapters.ModuleServices;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapter;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterInformation;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPollingService;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPublishService;
import com.hivemq.edge.modules.config.impl.AbstractProtocolAdapterConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

/**
 * The abstract adapter contains the baseline coupling and basic lifecycle operations of
 * a given protocol adapter in the system.
 *
 * It will provide services to manage polling, publishing and standardised metrics which
 * will provide a cohesive runtime lifecycle of all the running instances
 *
 * @author Simon L Johnson
 */
public abstract class AbstractProtocolAdapter<T extends AbstractProtocolAdapterConfig> implements ProtocolAdapter {

    protected final @NotNull ProtocolAdapterInformation adapterInformation;

    protected final @NotNull ObjectMapper objectMapper;

    protected @Nullable ProtocolAdapterPublishService adapterPublishService;
    protected @Nullable ProtocolAdapterPollingService protocolAdapterPollingService;
    protected @NotNull ProtocolAdapterMetricsHelper protocolAdapterMetricsHelper;
    protected @NotNull T adapterConfig;
    protected @Nullable Long lastStartAttemptTime;
    protected @Nullable String lastErrorMessage;

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

    public byte[] convertToJson(final @NotNull Object data) throws ProtocolAdapterException {
        try {
            Preconditions.checkNotNull(data);
            ProtocolAdapterPublisherJsonPayload payload = new ProtocolAdapterPublisherJsonPayload();
            payload.setValue(data);
            payload.setTimestamp(System.currentTimeMillis());
            return objectMapper.writeValueAsBytes(payload);
        } catch(JsonProcessingException e){
            throw new ProtocolAdapterException("Error Wrapping Adapter Data", e);
        }
    }

    protected void bindServices(final @NotNull ModuleServices moduleServices){
        Preconditions.checkNotNull(moduleServices);
        protocolAdapterPollingService = moduleServices.protocolAdapterPollingService();
        adapterPublishService = moduleServices.adapterPublishService();
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

    public Integer getNumberOfDaemonProcessed(){
        return protocolAdapterPollingService.getPollingJobsForAdapter(getId()).size();
    }

    @Override
    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public T getAdapterConfig() {
        return adapterConfig;
    }

    @Override
    public @NotNull String getId() {
        return adapterConfig.getId();
    }

}
