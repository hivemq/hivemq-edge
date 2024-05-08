package com.hivemq.protocols;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.edge.modules.adapters.PollingPerSubscriptionProtocolAdapter;
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterDataSample;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPublishService;
import com.hivemq.edge.modules.api.events.EventService;
import com.hivemq.edge.modules.config.AdapterSubscription;
import com.hivemq.edge.modules.config.ProtocolAdapterConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class PerSubscriptionSampler extends AbstractSubscriptionSampler {

    private final @NotNull PollingPerSubscriptionProtocolAdapter perSubscriptionProtocolAdapter;
    private final @NotNull AdapterSubscription adapterSubscription;

    public PerSubscriptionSampler(
            final @NotNull ProtocolAdapterWrapper<PollingPerSubscriptionProtocolAdapter> protocolAdapter,
            final @NotNull ProtocolAdapterConfig protocolAdapterConfig,
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull ObjectMapper objectMapper,
            final @NotNull ProtocolAdapterPublishService adapterPublishService,
            final @NotNull AdapterSubscription adapterSubscription,
            final @NotNull EventService eventService) {
        super(protocolAdapter, protocolAdapterConfig,
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
