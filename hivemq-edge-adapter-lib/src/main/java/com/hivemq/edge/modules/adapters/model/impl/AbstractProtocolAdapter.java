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
package com.hivemq.edge.modules.adapters.model.impl;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.hivemq.edge.model.TypeIdentifierImpl;
import com.hivemq.edge.modules.adapters.ProtocolAdapterException;
import com.hivemq.edge.modules.adapters.data.AbstractProtocolAdapterJsonPayload;
import com.hivemq.edge.modules.adapters.data.DataPoint;
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterDataSample;
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterMultiPublishJsonPayload;
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterPublisherJsonPayload;
import com.hivemq.edge.modules.adapters.data.TagSample;
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
import com.hivemq.edge.modules.api.events.EventService;
import com.hivemq.edge.modules.api.events.EventUtils;
import com.hivemq.edge.modules.api.events.model.EventBuilder;
import com.hivemq.edge.modules.api.events.model.EventBuilderImpl;
import com.hivemq.edge.modules.api.events.model.EventImpl;
import com.hivemq.edge.modules.config.AdapterSubscription.MessageHandlingOptions;
import com.hivemq.edge.modules.config.impl.AbstractProtocolAdapterConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * The abstract adapter contains the baseline coupling and basic lifecycle operations of
 * a given protocol adapter in the system.
 * <p>
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

    protected @Nullable EventService eventService;

    protected @Nullable ProtocolAdapterPublishService adapterPublishService;
    protected @NotNull ProtocolAdapterMetricsHelper protocolAdapterMetricsHelper;

    protected @NotNull T adapterConfig;
    protected @Nullable Long lastStartAttemptTime;
    protected @Nullable String errorMessage;
    protected @NotNull final Object lock = new Object();
    protected @NotNull AtomicReference<RuntimeStatus> runtimeStatus =
            new AtomicReference<>(RuntimeStatus.STOPPED);
    protected @NotNull AtomicReference<ConnectionStatus> connectionStatus =
            new AtomicReference<>(ConnectionStatus.DISCONNECTED);

    public AbstractProtocolAdapter(final @NotNull ProtocolAdapterInformation adapterInformation,
                                   final @NotNull T adapterConfig,
                                   final @NotNull MetricRegistry metricRegistry) {
        Preconditions.checkNotNull(adapterInformation);
        Preconditions.checkNotNull(adapterConfig);
        Preconditions.checkNotNull(metricRegistry);
        this.adapterInformation = adapterInformation;
        this.adapterConfig = adapterConfig;
        this.protocolAdapterMetricsHelper = null;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Returns the static information relating to this adapter implementation, include plugin names etc.
     * @return
     */
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

    /**
     * Converts the supplied object into a valid JSON document which wraps a new timestamp and the
     * supplied object as the value
     * @param data - The data you wish to wrap into the standard JSONB envelope
     * @return a valid JSON document encoded to UTF-8 with the supplied value wrapped as an attribute on the envelope
     */
    public @NotNull List<AbstractProtocolAdapterJsonPayload> convertAdapterSampleToPublishes(final @NotNull ProtocolAdapterDataSample data) {
        Preconditions.checkNotNull(data);
        List<AbstractProtocolAdapterJsonPayload> list = new ArrayList<>();
        //-- Only include the timestamp if the settings say so
        Long timestamp = data.getSubscription().getIncludeTimestamp() ? data.getTimestamp() : null;
        if(data.getDataPoints().size() > 1 &&
                data.getSubscription().getMessageHandlingOptions() ==
                        MessageHandlingOptions.MQTTMessagePerSubscription){
            //-- Put all derived samples into a single MQTT message
            AbstractProtocolAdapterJsonPayload payload = createMultiPublishPayload(timestamp, data.getDataPoints(), data.getSubscription().getIncludeTagNames());
            decoratePayloadMessage(data, payload);
            list.add(payload);
        } else {
            //-- Put all derived samples into individual publish messages
            data.getDataPoints().stream().
                    map(dp ->
                        createPublishPayload(timestamp, dp, data.getSubscription().getIncludeTagNames())).
                    map(pp -> decoratePayloadMessage(data, pp)).
            forEach(list::add);
        }
        return list;
    }

    protected @NotNull ProtocolAdapterPublisherJsonPayload createPublishPayload(final @Nullable Long timestamp, @NotNull DataPoint dataPoint, boolean includeTagName){
        return new ProtocolAdapterPublisherJsonPayload(timestamp, createTagSample(dataPoint, includeTagName));
    }

    protected @NotNull AbstractProtocolAdapterJsonPayload createMultiPublishPayload(final @Nullable Long timestamp, List<DataPoint> dataPoint, boolean includeTagName){
        return new ProtocolAdapterMultiPublishJsonPayload(timestamp, dataPoint.stream().map(dp -> createTagSample(dp, includeTagName)).collect(Collectors.toList()));
    }

    protected static TagSample createTagSample(final @NotNull DataPoint dataPoint, boolean includeTagName){
        return new TagSample(includeTagName ? dataPoint.getTagName() : null, dataPoint.getTagValue());
    }

    protected @NotNull AbstractProtocolAdapterJsonPayload decoratePayloadMessage(ProtocolAdapterDataSample sample, @NotNull AbstractProtocolAdapterJsonPayload payload){
        if(sample.getUserProperties() != null && !sample.getUserProperties().isEmpty()){
            payload.setUserProperties(sample.getUserProperties());
        }
        return payload;
    }

    /**
     * Converts the supplied object into a valid JSON document which wraps a new timestamp and the
     * supplied object as the value
     * @param data - The data you wish to wrap into the standard JSONB envelope
     * @return a valid JSON document encoded to UTF-8 with the supplied value wrapped as an attribute on the envelope
     */
    public byte @NotNull [] convertToJson(final @NotNull AbstractProtocolAdapterJsonPayload data) throws ProtocolAdapterException {
        try {
            Preconditions.checkNotNull(data);
            return objectMapper.writeValueAsBytes(data);
        } catch(JsonProcessingException e){
            throw new ProtocolAdapterException("Error Wrapping Adapter Data", e);
        }
    }

    @VisibleForTesting
    public void bindServices(final @NotNull ModuleServices moduleServices){
        Preconditions.checkNotNull(moduleServices);
        if(adapterPublishService == null){
            adapterPublishService = moduleServices.adapterPublishService();
        }
        if(eventService == null){
            eventService = moduleServices.eventService();
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
    protected void reportErrorMessage(@Nullable final Throwable throwable, @Nullable final String errorMessage, final boolean sendEvent){
        this.errorMessage = errorMessage == null ? throwable == null ? null : throwable.getMessage() : errorMessage;
        if(sendEvent){
            eventService.fireEvent(
                    eventBuilder(EventImpl.SEVERITY.ERROR).
                            withMessage(String.format("Adapter '%s' encountered an error.",
                            adapterConfig.getId())).
                            withPayload(EventUtils.generateErrorPayload(throwable)).
                            build());
        }
    }

    @Override
    public @NotNull CompletableFuture<Void> discoverValues(final @NotNull ProtocolAdapterDiscoveryInput input,
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
    public @NotNull CompletableFuture<ProtocolAdapterStartOutput> start(
            final @NotNull ProtocolAdapterStartInput input,
            final @NotNull ProtocolAdapterStartOutput output) {
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
    public @NotNull CompletableFuture<Void> stop() {
        if(!running()){
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Void> future = null;
        try {
            future = stopInternal();
            return future;
        } finally {
            if(future != null){
                future.thenRun(this::onStop);
            }
        }
    }

    /**
     * Set the internal status indication, used by the frameworks and APIs to monitor the adapter status and offer controls around
     * state transitions.
     * @param connectionStatus - set the new status of the adapter to that supplied
     * @return was the value updated by the operation
     */
    protected boolean setConnectionStatus(@NotNull final ConnectionStatus connectionStatus){
        Preconditions.checkNotNull(connectionStatus);
        return this.connectionStatus.getAndSet(connectionStatus) != connectionStatus;
    }

    /**
     * A convenience method that sets the ConnectionStatus to Error
     * and the errorMessage to that supplied.
     */
    protected void setErrorConnectionStatus(@Nullable final Throwable t, @Nullable final String errorMessage){
        boolean changed = setConnectionStatus(ConnectionStatus.ERROR);
        reportErrorMessage(t, errorMessage, changed);
    }


    protected void setErrorConnectionStatus(@NotNull final Throwable t){
        Preconditions.checkNotNull(t);
        setErrorConnectionStatus(t, null);
    }

    protected void setRuntimeStatus(@NotNull final RuntimeStatus runtimeStatus){
        Preconditions.checkNotNull(runtimeStatus);
        this.runtimeStatus.set(runtimeStatus);
    }

    @Override
    public void destroy() {
        protocolAdapterMetricsHelper.clearAll();
    }

    protected boolean running(){
        return runtimeStatus.get() == RuntimeStatus.STARTED;
    }

    @Override
    public @Nullable String getErrorMessage() {
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
    public @NotNull ConnectionStatus getConnectionStatus() {
        return connectionStatus.get();
    }

    @Override
    public @NotNull RuntimeStatus getRuntimeStatus() {
        return runtimeStatus.get();
    }

    protected void onStartFail(@NotNull final ProtocolAdapterStartOutput output, @NotNull final Throwable throwable){
        setErrorConnectionStatus(throwable, null);
        output.failStart(throwable, throwable.getMessage());
        eventService.fireEvent(
                eventBuilder(EventImpl.SEVERITY.CRITICAL).
                        withPayload(EventUtils.generateErrorPayload(throwable)).
                        withMessage("Error starting adapter").build());
    }

    protected void onStartSuccess(@NotNull final ProtocolAdapterStartOutput output){
        setRuntimeStatus(RuntimeStatus.STARTED);
        output.startedSuccessfully("adapter start OK");
        eventService.fireEvent(
                eventBuilder(EventImpl.SEVERITY.INFO).
                        withMessage(String.format("Adapter '%s' started OK.",
                        adapterConfig.getId())).build());
    }

    protected void onStop(){
        setRuntimeStatus(RuntimeStatus.STOPPED);
        eventService.fireEvent(
                eventBuilder(EventImpl.SEVERITY.INFO).
                        withMessage(String.format("Adapter '%s' stopped OK.",
                        adapterConfig.getId())).build());
    }

    /**
     * Provide a method to lazily traverse tag-data on your external device.
     * @param input
     * @param output
     * @return
     */
    protected @NotNull CompletableFuture<Void> discoverValuesInternal(final @NotNull ProtocolAdapterDiscoveryInput input,
                                                                      final @NotNull ProtocolAdapterDiscoveryOutput output){
        return CompletableFuture.completedFuture(null);
    }

    protected abstract @NotNull CompletableFuture<ProtocolAdapterStartOutput> startInternal(final @NotNull ProtocolAdapterStartOutput output);

    protected abstract @NotNull CompletableFuture<Void> stopInternal();

    protected @NotNull EventBuilder eventBuilder(final @NotNull EventImpl.SEVERITY severity){
        EventBuilder builder = new EventBuilderImpl();
        builder.withTimestamp(System.currentTimeMillis());
        builder.withSource(TypeIdentifierImpl.create(TypeIdentifierImpl.TYPE.ADAPTER, adapterConfig.getId()));
        builder.withAssociatedObject(TypeIdentifierImpl.create(TypeIdentifierImpl.TYPE.ADAPTER_TYPE,
                adapterInformation.getProtocolId()));
        builder.withSeverity(severity);
        return builder;
    }
}
