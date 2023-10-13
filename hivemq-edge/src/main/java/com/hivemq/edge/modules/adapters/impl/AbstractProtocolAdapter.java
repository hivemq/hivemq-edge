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
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterDataSample;
import com.hivemq.edge.modules.adapters.metrics.ProtocolAdapterMetricsHelper;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterDiscoveryInput;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterDiscoveryOutput;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterStartInput;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterStartOutput;
import com.hivemq.edge.modules.api.adapters.ModuleServices;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapter;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterCapability;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterInformation;
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
    protected @Nullable String errorMessage;
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

    /**
     * Returns the static information relating to this adapter implementation, include plugin names etc.
     * @return
     */
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
        if(adapterPublishService == null){
            adapterPublishService = moduleServices.adapterPublishService();
        }
    }

    /**
     * Invoked by the start method when the adapter is called to start.
     */
    protected void initStartAttempt(){
        lastStartAttemptTime = System.currentTimeMillis();
    }

    /**
     * Sets the last error message associated with the adapter runtime. This is can be sent through the API to
     * give an indication of the status of an adapter runtime.
     * @param errorMessage
     */
    protected void reportErrorMessage(@Nullable final Throwable throwable, @NotNull final String errorMessage){
        Preconditions.checkNotNull(throwable);
        this.errorMessage = errorMessage == null ? throwable.getMessage() : errorMessage;
    }

    @Override
    public Long getTimeOfLastStartAttempt() {
        return lastStartAttemptTime;
    }

    @Override
    public CompletableFuture<Void> discoverValues(final @NotNull ProtocolAdapterDiscoveryInput input,
                                                  final @NotNull ProtocolAdapterDiscoveryOutput output) {
        if(!ProtocolAdapterCapability.supportsCapability(
                getProtocolAdapterInformation(), ProtocolAdapterCapability.DISCOVER)){
            return CompletableFuture.failedFuture(new UnsupportedOperationException("Adapter type does not support discovery"));
        } else {
            return discoverValuesInternal(input, output);
        }
    }


    /**
     * Satisfies the external interface requirements, binding and initialising state that the broader system needs.
     * Will eventually call into the abstraction (startInternal) whose sole focus will be to start the encapsulated
     * connections/resources required by the adapter runtime.
     *
     * @param input - the state associated with runtime. Allows the adapter to bind to required services in a decoupled manner
     * @param output - the output resulting from the start operation. The adapter will
     * @return A future that marks the starting of the adapter.
     */
    @Override
    public CompletableFuture<ProtocolAdapterStartOutput> start(
            final ProtocolAdapterStartInput input,
            final ProtocolAdapterStartOutput output) {
        CompletableFuture<ProtocolAdapterStartOutput> future = null;
        if(running()){
            return CompletableFuture.completedFuture(output);
        }
        try {
            bindServices(input.moduleServices());
            initStartAttempt();
            future = startInternal(output);
            return future;
        } finally {
            if(future != null){
                future.thenRun(() -> onStartSuccess(output)).exceptionally(
                            (t) ->  {
                                onStartFail(output, t);
                                return null;
                            });
            }
        }
    }

    /**
     * Satisfies the external interface requirements, releasing state and setting progress flags accordingly.
     * Will eventually call into the abstraction (stopInternal) whose sole focus will be to stop the encapsulated
     * connections/resources required by the adapter runtime.
     *
     * @return A future that marks the stopping of the adapter.
     */
    @Override
    public CompletableFuture<Void> stop() {
        if(!running()){
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Void> future = null;
        try {
            future = stopInternal();
            return future;
        } finally {
            if(future != null){
                future.thenRun(() -> onStop());
            }
        }
    }

    /**
     * Set the internal status indication, used by the frameworks and APIs to monitor the adapter status and offer controls around
     * state transitions.
     * @param connectionStatus - set the new status of the adapter to that supplied
     */
    protected void setConnectionStatus(@NotNull final ConnectionStatus connectionStatus){
        Preconditions.checkNotNull(connectionStatus);
        synchronized (lock){
            this.connectionStatus = connectionStatus;
        }
    }

    /**
     * A convenience method that sets the ConnectionStatus to Error
     * and the errorMessage to that supplied.
     */
    protected void setErrorConnectionStatus(@NotNull final String errorMessage){
        Preconditions.checkNotNull(errorMessage);
        synchronized (lock){
            this.connectionStatus = ConnectionStatus.ERROR;
            reportErrorMessage(null, errorMessage);
        }
    }

    protected void setErrorConnectionStatus(@NotNull final Throwable t){
        Preconditions.checkNotNull(t);
        synchronized (lock){
            this.connectionStatus = ConnectionStatus.ERROR;
            reportErrorMessage(t, t.getMessage());
        }
    }

    protected void setRuntimeStatus(@NotNull final RuntimeStatus runtimeStatus){
        Preconditions.checkNotNull(runtimeStatus);
        synchronized (lock){
            this.runtimeStatus = runtimeStatus;
        }
    }

    protected boolean running(){
        synchronized (lock){
            return runtimeStatus == RuntimeStatus.STARTED;
        }
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
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

    protected void onStartFail(@NotNull final ProtocolAdapterStartOutput output, @NotNull final Throwable throwable){
        setErrorConnectionStatus(throwable);
        output.failStart(throwable, throwable.getMessage());
        //-- TODO add system event
    }

    protected void onStartSuccess(@NotNull final ProtocolAdapterStartOutput output){
        setRuntimeStatus(RuntimeStatus.STARTED);
        output.startedSuccessfully("adapter start OK");
        //-- TODO add system event
    }

    protected void onStop(){
        setRuntimeStatus(RuntimeStatus.STOPPED);
        //-- TODO add system event
    }


    /**
     * Provide a method to lazily traverse tag-data on your external device.
     * @param input
     * @param output
     * @return
     */
    protected CompletableFuture<Void> discoverValuesInternal(final @NotNull ProtocolAdapterDiscoveryInput input,
                                                             final @NotNull ProtocolAdapterDiscoveryOutput output){
        return CompletableFuture.completedFuture(null);
    }

    protected abstract CompletableFuture<ProtocolAdapterStartOutput> startInternal(final ProtocolAdapterStartOutput output);

    protected abstract CompletableFuture<Void> stopInternal();
}
