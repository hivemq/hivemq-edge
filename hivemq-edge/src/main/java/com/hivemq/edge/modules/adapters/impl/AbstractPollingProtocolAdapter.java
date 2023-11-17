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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.hivemq.edge.modules.adapters.data.AbstractProtocolAdapterJsonPayload;
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterDataSample;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterPollingSampler;
import com.hivemq.edge.modules.adapters.model.impl.ProtocolAdapterPollingSamplerImpl;
import com.hivemq.edge.modules.api.adapters.ModuleServices;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterInformation;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPollingService;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPublishBuilder;
import com.hivemq.edge.modules.api.events.EventUtils;
import com.hivemq.edge.modules.api.events.model.Event;
import com.hivemq.edge.modules.config.impl.AbstractPollingProtocolAdapterConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.handler.publish.PublishReturnCode;
import net.javacrumbs.futureconverter.java8guava.FutureConverter;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Simon L Johnson
 */
public abstract class AbstractPollingProtocolAdapter <T extends AbstractPollingProtocolAdapterConfig, U extends ProtocolAdapterDataSample>
        extends AbstractProtocolAdapter<T> {

    protected @Nullable ProtocolAdapterPollingService protocolAdapterPollingService;
    protected @NotNull AtomicLong publishCount = new AtomicLong();

    public AbstractPollingProtocolAdapter(
            final ProtocolAdapterInformation adapterInformation,
            final T adapterConfig,
            final MetricRegistry metricRegistry) {
        super(adapterInformation, adapterConfig, metricRegistry);
    }

    @VisibleForTesting
    public void bindServices(final @NotNull ModuleServices moduleServices){
        Preconditions.checkNotNull(moduleServices);
        super.bindServices(moduleServices);
        if(protocolAdapterPollingService == null){
            protocolAdapterPollingService = moduleServices.protocolAdapterPollingService();
        }
    }

    @Override
    public CompletableFuture<Void> stop() {
        publishCount.set(0);
        return super.stop().whenComplete((v, t) ->
                protocolAdapterPollingService.getPollingJobsForAdapter(getId()).stream().forEach(
                    protocolAdapterPollingService::stopPolling));
    }

    protected CompletableFuture<?> captureDataSample(final @NotNull U sample){
        Preconditions.checkNotNull(sample);
        Preconditions.checkNotNull(sample.getTopic());
        Preconditions.checkArgument(sample.getQos() <= 2 && sample.getQos() >= 0, "QoS needs to be a valid Quality-Of-Service value (0,1,2)");
        try {
            final ImmutableList.Builder<CompletableFuture<?>> publishFutures = ImmutableList.builder();
            List<AbstractProtocolAdapterJsonPayload> payloads = convertAdapterSampleToPublishes(sample);
            for (AbstractProtocolAdapterJsonPayload payload : payloads){
                byte[] json = convertToJson(payload);
                final ProtocolAdapterPublishBuilder publishBuilder = adapterPublishService.publish()
                        .withTopic(sample.getTopic())
                        .withQoS(sample.getQos())
                        .withPayload(json);
                final CompletableFuture<PublishReturnCode> publishFuture = publishBuilder.send();
                publishFuture.thenAccept(publishReturnCode -> {
                    protocolAdapterMetricsHelper.incrementReadPublishSuccess();
                    if(publishCount.incrementAndGet() == 1){
                        eventService.fireEvent(eventBuilder(Event.SEVERITY.INFO).
                                withMessage(String.format("Adapter took first sample to be published to '%s'", sample.getTopic())).
                                withPayload(EventUtils.generateJsonPayload(json)).build());
                    }
                })
                .exceptionally(throwable -> {
                    protocolAdapterMetricsHelper.incrementReadPublishFailure();
                    log.warn("Error Publishing Adapter Payload", throwable); return null;
                });
                publishFutures.add(publishFuture);
            }
            return CompletableFuture.allOf(publishFutures.build().toArray(new CompletableFuture[0]));
        } catch(Exception e){
            return CompletableFuture.failedFuture(e);
        }
    }

    protected void startPolling(final @NotNull Sampler sampler) {
        Preconditions.checkNotNull(sampler);
        protocolAdapterPollingService.schedulePolling(this, sampler);
    }

    /**
     * Method is invoked by the sampling engine on the schedule determined by the configuration
     * supplied.
     */
    protected abstract CompletableFuture<U> onSamplerInvoked(T config) ;

    /**
     * Hook Method is invoked by the sampling engine when the scheduling engine is removing the
     * sampler from being managed
     */
    protected void onSamplerClosed(final @NotNull ProtocolAdapterPollingSampler sampler) {
        //-- Override me
    }

    /**
     * Hook Method is invoked by the sampling engine when the sampler throws an exception. It contains
     * details of whether the sampler will continue or be removed from the scheduler along with
     * the cause of the error.
     */
    protected void onSamplerError(final @NotNull ProtocolAdapterPollingSampler sampler, final @NotNull Throwable exception, boolean continuing) {
        setErrorConnectionStatus(exception, null);
        if(!continuing){
            stop();
        }
    }

    protected class Sampler extends ProtocolAdapterPollingSamplerImpl<U> {

        protected final T config;

        public Sampler(final @NotNull T config) {
            super(AbstractPollingProtocolAdapter.this.getId(), config.getPollingIntervalMillis(),
                    config.getPollingIntervalMillis(),
                    TimeUnit.MILLISECONDS,
                    config.getMaxPollingErrorsBeforeRemoval());
            this.config = config;
        }

        @Override
        public CompletableFuture<U> execute() {
            if(Thread.currentThread().isInterrupted()){
                return CompletableFuture.failedFuture(new InterruptedException());
            }
            CompletableFuture<U> data = onSamplerInvoked(config);
            data.thenApply(d -> captureDataSample(d));
            return data;
        }

        @Override
        public void close() {
            super.close();
            onSamplerClosed(this);
        }

        @Override
        public void error(@NotNull final Throwable t, final boolean continuing) {
            onSamplerError(this,t,continuing);
        }
    }
}
