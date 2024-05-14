package com.hivemq.protocols;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.PollingPerSubscriptionProtocolAdapter;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.data.ProtocolAdapterDataSample;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterPublishService;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class PerSubscriptionSampler extends AbstractSubscriptionSampler {

    private final @NotNull PollingPerSubscriptionProtocolAdapter perSubscriptionProtocolAdapter;
    private final @NotNull PollingContext pollingContext;

    public PerSubscriptionSampler(
            final @NotNull ProtocolAdapterWrapper<PollingPerSubscriptionProtocolAdapter> protocolAdapter,
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull ObjectMapper objectMapper,
            final @NotNull ProtocolAdapterPublishService adapterPublishService,
            final @NotNull PollingContext pollingContext,
            final @NotNull EventService eventService) {
        super(protocolAdapter,
                protocolAdapter.getAdapter().getPollingIntervalMillis(),
                protocolAdapter.getAdapter().getPollingIntervalMillis(),
                metricRegistry,
                objectMapper,
                adapterPublishService,
                eventService);
        this.perSubscriptionProtocolAdapter = protocolAdapter.getAdapter();
        this.pollingContext = pollingContext;
    }


    @Override
    public @NotNull CompletableFuture<? extends ProtocolAdapterDataSample> execute() {
        if (Thread.currentThread().isInterrupted()) {
            return CompletableFuture.failedFuture(new InterruptedException());
        }
        CompletableFuture<? extends ProtocolAdapterDataSample> future =
                perSubscriptionProtocolAdapter.poll(pollingContext);
        future.thenApply(this::captureDataSample);
        return future;
    }


}
