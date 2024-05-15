package com.hivemq.protocols;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.data.ProtocolAdapterDataSample;
import com.hivemq.adapter.sdk.api.polling.PollingOutput;
import com.hivemq.edge.modules.adapters.data.DataPointImpl;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class PollingOutputImpl implements PollingOutput {

    public enum PollingResult{
        SUCCESS,
        NO_DATA,
        FAILURE;
    }


    private final @NotNull ProtocolAdapterDataSample dataSample;
    final @NotNull CompletableFuture<PollingResult> outputFuture = new CompletableFuture<>();

    public PollingOutputImpl(final @NotNull ProtocolAdapterDataSample dataSample) {
        this.dataSample = dataSample;
    }


    @Override
    public void addDataPoint(final @NotNull String tagName, final @NotNull Object tagValue) {
        dataSample.addDataPoint(new DataPointImpl(tagName, tagValue));
    }

    @Override
    public void addDataPoint(final @NotNull DataPoint dataPoint) {
        dataSample.addDataPoint(dataPoint);
    }

    @Override
    public void finish() {
        if(dataSample.getDataPoints().isEmpty()){
            outputFuture.complete(PollingResult.NO_DATA);
        } else {
            outputFuture.complete(PollingResult.SUCCESS);
        }
    }

    @Override
    public void fail(final @NotNull Throwable t) {
        outputFuture.completeExceptionally(t);
    }

    public @NotNull CompletableFuture<PollingResult> getOutputFuture() {
        return outputFuture;
    }

    public @NotNull ProtocolAdapterDataSample getDataSample() {
        return dataSample;
    }
}
