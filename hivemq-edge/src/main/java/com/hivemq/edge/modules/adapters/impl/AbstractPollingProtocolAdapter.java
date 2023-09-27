package com.hivemq.edge.modules.adapters.impl;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Preconditions;
import com.hivemq.edge.modules.adapters.ProtocolAdapterException;
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterDataSample;
import com.hivemq.edge.modules.adapters.params.impl.ProtocolAdapterPollingInputImpl;
import com.hivemq.edge.modules.api.adapters.ModuleServices;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterInformation;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPollingService;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPublishBuilder;
import com.hivemq.edge.modules.config.impl.AbstractPollingProtocolAdapterConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.handler.publish.PublishReturnCode;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Simon L Johnson
 */
public abstract class AbstractPollingProtocolAdapter <T extends AbstractPollingProtocolAdapterConfig, U extends ProtocolAdapterDataSample>
        extends AbstractProtocolAdapter<T> {

    protected @Nullable ProtocolAdapterPollingService protocolAdapterPollingService;

    public AbstractPollingProtocolAdapter(
            final ProtocolAdapterInformation adapterInformation,
            final T adapterConfig,
            final MetricRegistry metricRegistry) {
        super(adapterInformation, adapterConfig, metricRegistry);
    }

    protected void bindServices(final @NotNull ModuleServices moduleServices){
        Preconditions.checkNotNull(moduleServices);
        super.bindServices(moduleServices);
        if(protocolAdapterPollingService == null){
            protocolAdapterPollingService = moduleServices.protocolAdapterPollingService();
        }
    }

    @Override
    public CompletableFuture<Void> stop() {
        return super.stop().whenComplete((v, t) ->
                protocolAdapterPollingService.getPollingJobsForAdapter(getId()).stream().forEach(
                    protocolAdapterPollingService::stopPolling));
    }

    protected void captureDataSample(final @NotNull U sample) throws ProtocolAdapterException {
        Preconditions.checkNotNull(sample);
        Preconditions.checkNotNull(sample.getData());
        Preconditions.checkNotNull(sample.getTopic());
        Preconditions.checkArgument(sample.getQos() <= 2 && sample.getQos() >= 0, "QoS needs to be a valid Quality-Of-Service value (0,1,2)");
        final ProtocolAdapterPublishBuilder publishBuilder = adapterPublishService.publish()
                .withTopic(sample.getTopic())
                .withPayload(convertToJson(sample))
                .withQoS(sample.getQos());
        final CompletableFuture<PublishReturnCode> publishFuture = publishBuilder.send();
        publishFuture.thenAccept(publishReturnCode -> {
            protocolAdapterMetricsHelper.incrementReadPublishSuccess();
        }).exceptionally(throwable -> {
            protocolAdapterMetricsHelper.incrementReadPublishFailure();
            log.warn("Error Publishing Adapter Payload", throwable);
            return null;
        });
    }

    protected void startPolling(final @NotNull Sampler sampler) {
        Preconditions.checkNotNull(sampler);
        protocolAdapterPollingService.schedulePolling(this, sampler);
    }

    protected abstract U doSample(T config) throws Exception ;

    protected class Sampler extends ProtocolAdapterPollingInputImpl {

        protected final T config;

        public Sampler(final @NotNull T config) {
            super(config.getPublishingInterval(),
                    config.getPublishingInterval(),
                    TimeUnit.MILLISECONDS,
                    config.getMaxPollingErrorsBeforeRemoval());
            this.config = config;
        }

        @Override
        public void execute() throws Exception {
            U data = doSample(config);
            if (data != null) {
                captureDataSample(data);
            }
        }
    }
}
