package com.hivemq.protocols;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.edge.modules.adapters.PollingPerSubscriptionProtocolAdapter;
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterDataSample;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPublishService;
import com.hivemq.edge.modules.api.events.EventService;
import com.hivemq.edge.modules.api.events.model.EventBuilder;
import com.hivemq.edge.modules.config.AdapterSubscription;
import com.hivemq.edge.modules.config.CustomConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class PerSubscriptionSampler extends AbstractSubscriptionSampler {

    private final @NotNull PollingPerSubscriptionProtocolAdapter perSubscriptionProtocolAdapter;
    private final @NotNull AdapterSubscription adapterSubscription;

    public PerSubscriptionSampler(
            final @NotNull ProtocolAdapterWrapper<PollingPerSubscriptionProtocolAdapter> protocolAdapter,
            final @NotNull CustomConfig customConfig,
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull ObjectMapper objectMapper,
            final @NotNull ProtocolAdapterPublishService adapterPublishService,
            final @NotNull AdapterSubscription adapterSubscription,
            final @NotNull EventService eventService,
            final @NotNull Supplier<EventBuilder> eventBuilderSupplier) {
        super(protocolAdapter,
                customConfig,
                metricRegistry,
                objectMapper,
                adapterPublishService,
                eventService,
                eventBuilderSupplier);
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
