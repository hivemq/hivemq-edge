package com.hivemq.protocols;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.extension.sdk.api.adapters.PollingPerSubscriptionProtocolAdapter;
import com.hivemq.extension.sdk.api.adapters.config.AdapterSubscription;
import com.hivemq.extension.sdk.api.adapters.data.ProtocolAdapterDataSample;
import com.hivemq.extension.sdk.api.adapters.services.ProtocolAdapterPublishService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.events.EventService;

import java.util.concurrent.CompletableFuture;

public class PerSubscriptionSampler extends AbstractSubscriptionSampler {

    private final @NotNull PollingPerSubscriptionProtocolAdapter perSubscriptionProtocolAdapter;
    private final @NotNull AdapterSubscription adapterSubscription;

    public PerSubscriptionSampler(
            final @NotNull ProtocolAdapterWrapper<PollingPerSubscriptionProtocolAdapter> protocolAdapter,
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull ObjectMapper objectMapper,
            final @NotNull ProtocolAdapterPublishService adapterPublishService,
            final @NotNull AdapterSubscription adapterSubscription,
            final @NotNull EventService eventService) {
        super(protocolAdapter,
                protocolAdapter.getAdapter().getPollingIntervalMillis(),
                protocolAdapter.getAdapter().getPollingIntervalMillis(),
                metricRegistry,
                objectMapper,
                adapterPublishService,
                eventService);
        this.perSubscriptionProtocolAdapter = protocolAdapter.getAdapter();
        this.adapterSubscription = adapterSubscription;
    }


    @Override
    public @NotNull CompletableFuture<? extends ProtocolAdapterDataSample> execute() {
        if (Thread.currentThread().isInterrupted()) {
            return CompletableFuture.failedFuture(new InterruptedException());
        }
        CompletableFuture<? extends ProtocolAdapterDataSample> future =
                perSubscriptionProtocolAdapter.poll(adapterSubscription);
        future.thenApply(this::captureDataSample);
        return future;
    }


}
