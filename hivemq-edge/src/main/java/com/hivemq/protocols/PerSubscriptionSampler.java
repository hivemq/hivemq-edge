package com.hivemq.protocols;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.polling.PollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterPublishService;
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterDataSampleImpl;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class PerSubscriptionSampler extends AbstractSubscriptionSampler {

    private final @NotNull PollingProtocolAdapter perSubscriptionProtocolAdapter;
    private final @NotNull PollingContext pollingContext;

    public PerSubscriptionSampler(
            final @NotNull ProtocolAdapterWrapper<PollingProtocolAdapter> protocolAdapter,
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
    public @NotNull CompletableFuture<PollingOutputImpl.PollingResult> execute() {
        if (Thread.currentThread().isInterrupted()) {
            return CompletableFuture.failedFuture(new InterruptedException());
        }
        final PollingOutputImpl pollingOutput = new PollingOutputImpl(new ProtocolAdapterDataSampleImpl());
        try {
            perSubscriptionProtocolAdapter.poll(new PollingInputImpl(pollingContext), pollingOutput);
        }catch (Throwable t){
            pollingOutput.fail(t);
            throw t;
        }
        final CompletableFuture<PollingOutputImpl.PollingResult> outputFuture = pollingOutput.getOutputFuture();
        outputFuture.thenAccept(pollingResult -> {
            if (pollingResult == PollingOutputImpl.PollingResult.SUCCESS) {
                this.captureDataSample(pollingOutput.getDataSample(), pollingContext);
            } else if (pollingResult == PollingOutputImpl.PollingResult.FAILURE) {
                // TODO LOG
            }
        });
        return outputFuture;
    }


}
