package com.hivemq.protocols;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.edge.modules.adapters.PollingProtocolAdapter;
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterDataSample;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPublishService;
import com.hivemq.edge.modules.api.events.EventService;
import com.hivemq.edge.modules.config.ProtocolAdapterConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class SubscriptionSampler extends AbstractSubscriptionSampler {

    protected @NotNull AtomicBoolean closed = new AtomicBoolean(false);

    protected final @NotNull PollingProtocolAdapter protocolAdapter;

    public SubscriptionSampler(
            final @NotNull ProtocolAdapterWrapper<PollingProtocolAdapter> adapterProtocolAdapterWrapper,
            final @NotNull ProtocolAdapterConfig protocolAdapterConfig,
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull ObjectMapper objectMapper,
            final @NotNull ProtocolAdapterPublishService adapterPublishService,
            final @NotNull EventService eventService) {
        super(adapterProtocolAdapterWrapper, protocolAdapterConfig,
                metricRegistry,
                objectMapper,
                adapterPublishService,
                eventService);
        this.protocolAdapter = adapterProtocolAdapterWrapper.getAdapter();
    }

    @Override
    public @NotNull CompletableFuture<? extends ProtocolAdapterDataSample> execute() {
        if (Thread.currentThread().isInterrupted()) {
            return CompletableFuture.failedFuture(new InterruptedException());
        }
        final CompletableFuture<? extends ProtocolAdapterDataSample> pollFuture = protocolAdapter.poll();
        pollFuture.thenApply(this::captureDataSample);
        return pollFuture;
    }
}
