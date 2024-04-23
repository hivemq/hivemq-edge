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
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterDataSample;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterInformation;
import com.hivemq.edge.modules.config.AdapterSubscription;
import com.hivemq.edge.modules.config.impl.AbstractPollingProtocolAdapterConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Will create a sampler for each subscription presented based on the global sampling window.
 * @author Simon L Johnson
 */
public abstract class AbstractPollingPerSubscriptionAdapter<T extends AbstractPollingProtocolAdapterConfig, U extends ProtocolAdapterDataSample>
        extends AbstractPollingProtocolAdapter <T, U>{

    public AbstractPollingPerSubscriptionAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull T adapterConfig,
            final @NotNull MetricRegistry metricRegistry) {
        super(adapterInformation, adapterConfig, metricRegistry);
    }

    @Override
    protected CompletableFuture<U> onSamplerInvoked(final @NotNull T config) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Subscription sampler should be used."));

    }

    protected abstract @NotNull CompletableFuture<U> onSamplerInvoked(@NotNull T config, @NotNull AdapterSubscription adapterSubscription) ;

    protected class SubscriptionSampler extends Sampler {

        protected final @NotNull AdapterSubscription adapterSubscription;

        public SubscriptionSampler(final @NotNull T config,
                      final @NotNull AdapterSubscription adapterSubscription) {
            super(config);
            this.adapterSubscription = adapterSubscription;
        }

        @Override
        public CompletableFuture<U> execute() {
            if(Thread.currentThread().isInterrupted()){
                return CompletableFuture.failedFuture(new InterruptedException());
            }
            CompletableFuture<U> future = onSamplerInvoked(config, adapterSubscription);
            future.thenApply(d -> captureDataSample(d));
            return future;
        }
    }
}
