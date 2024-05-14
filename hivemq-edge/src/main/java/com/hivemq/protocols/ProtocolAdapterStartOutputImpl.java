package com.hivemq.protocols;

import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ProtocolAdapterStartOutputImpl implements ProtocolAdapterStartOutput {

    private @Nullable volatile String message = null;
    private @Nullable volatile Throwable throwable = null;
    private final @NotNull CompletableFuture<Boolean> startFuture = new CompletableFuture<>();

    @Override
    public void startedSuccessfully() {
        this.startFuture.complete(true);
    }

    @Override
    public void failStart(final @NotNull Throwable t, final @Nullable String errorMessage) {
        this.throwable = t;
        this.message = errorMessage;
        this.startFuture.complete(false);
    }

    public @Nullable Throwable getThrowable() {
        return throwable;
    }

    public @Nullable String getMessage() {
        return message;
    }

    public @NotNull CompletableFuture<Boolean> getStartFuture() {
        return startFuture;
    }
}
