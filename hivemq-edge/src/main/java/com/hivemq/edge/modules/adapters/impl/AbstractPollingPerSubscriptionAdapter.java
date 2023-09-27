package com.hivemq.edge.modules.adapters.impl;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterDataSample;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterInformation;
import com.hivemq.edge.modules.config.impl.AbstractPollingProtocolAdapterConfig;
import com.hivemq.edge.modules.config.impl.AbstractProtocolAdapterConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;

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
    protected U onSamplerInvoked(final T config) throws Exception {
        throw new UnsupportedOperationException("Subscription sampler should be used.");
    }

    protected abstract U onSamplerInvoked(@NotNull T config, @NotNull T.Subscription subscription) throws Exception ;

    protected class SubscriptionSampler extends Sampler {

        protected final AbstractProtocolAdapterConfig.Subscription subscription;

        public SubscriptionSampler(final @NotNull T config,
                      final @NotNull AbstractProtocolAdapterConfig.Subscription subscription) {
            super(config);
            this.subscription = subscription;
        }

        @Override
        public void execute() throws Exception {
            U data = onSamplerInvoked(config, subscription);
            if (data != null) {
                captureDataSample(data);
            }
        }
    }
}
