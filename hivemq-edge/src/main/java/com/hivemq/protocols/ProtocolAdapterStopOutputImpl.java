package com.hivemq.protocols;

import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ProtocolAdapterStopOutputImpl implements ProtocolAdapterStopOutput {


    final @NotNull CompletableFuture<Void> outputFuture = new CompletableFuture<>();
    private @Nullable String errorMessage = null;

    public ProtocolAdapterStopOutputImpl() {
    }

    public @NotNull CompletableFuture<Void> getOutputFuture() {
        return outputFuture;
    }

    public @Nullable String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public void stoppedSuccessfully() {
        outputFuture.complete(null);
    }

    @Override
    public void failStop(@NotNull final Throwable throwable, @Nullable final String errorMessage) {
        this.errorMessage = errorMessage;
        this.outputFuture.completeExceptionally(throwable);
    }
}
