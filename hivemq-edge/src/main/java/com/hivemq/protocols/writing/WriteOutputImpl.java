package com.hivemq.protocols.writing;

import com.hivemq.adapter.sdk.api.writing.WriteOutput;
import com.hivemq.exceptions.StackLessProtocolAdapterException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class WriteOutputImpl implements WriteOutput {

    private volatile @Nullable String message = null;
    private volatile @Nullable Throwable throwable = null;
    private final @NotNull CompletableFuture<Boolean> future = new CompletableFuture<>();

    public @Nullable Throwable getThrowable() {
        return throwable;
    }

    public @Nullable String getMessage() {
        return message;
    }

    public @NotNull CompletableFuture<Boolean> getFuture() {
        return future;
    }

    @Override
    public void finish() {
        this.future.complete(true);
    }

    @Override
    public void fail(@NotNull final Throwable t, @Nullable final String errorMessage) {
        this.throwable = t;
        this.message = errorMessage;
        this.future.complete(false);
    }

    @Override
    public void fail(@NotNull final String errorMessage) {
        this.message = errorMessage;
        future.completeExceptionally(new StackLessProtocolAdapterException(errorMessage));
    }
}
