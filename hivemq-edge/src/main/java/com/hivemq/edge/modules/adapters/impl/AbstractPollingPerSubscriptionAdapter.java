package com.hivemq.edge.modules.adapters.impl;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.api.model.metrics.DataPoint;
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterDataSample;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterInformation;
import com.hivemq.edge.modules.config.impl.AbstractPollingProtocolAdapterConfig;
import com.hivemq.edge.modules.config.impl.AbstractProtocolAdapterConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Will create a sampler for each subscription presented based on the global sampling window.
 * @author Simon L Johnson
 */
public abstract class AbstractPollingPerSubscriptionAdapter<T extends AbstractPollingProtocolAdapterConfig, U extends ProtocolAdapterDataSample>
        extends AbstractPollingProtocolAdapter <T, U>{

    public AbstractPollingPerSubscriptionAdapter(
            final ProtocolAdapterInformation adapterInformation,
            final T adapterConfig,
            final MetricRegistry metricRegistry) {
        super(adapterInformation, adapterConfig, metricRegistry);
    }

    @Override
    protected CompletableFuture<U> onSamplerInvoked(final T config) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Subscription sampler should be used."));

    }

    protected abstract CompletableFuture<U> onSamplerInvoked(@NotNull T config, @NotNull T.Subscription subscription) ;

    protected class SubscriptionSampler extends Sampler {

        protected final AbstractProtocolAdapterConfig.Subscription subscription;

        public SubscriptionSampler(final @NotNull T config,
                      final @NotNull AbstractProtocolAdapterConfig.Subscription subscription) {
            super(config);
            this.subscription = subscription;
        }

        @Override
        public CompletableFuture<U> execute() {
            CompletableFuture<U> future = onSamplerInvoked(config, subscription);
            future.thenApply(d -> captureDataSample(d));
            return future;
        }
    }
}
