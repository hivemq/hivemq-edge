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
    private boolean canBeRetried = true;

    public @Nullable Throwable getThrowable() {
        return throwable;
    }

    public @Nullable String getMessage() {
        return message;
    }

    public @NotNull CompletableFuture<Boolean> getFuture() {
        return future;
    }

    public boolean canBeRetried() {
        return canBeRetried;
    }

    @Override
    public void finish() {
        this.future.complete(true);
    }

    @Override
    public void fail(@NotNull final Throwable t, @Nullable final String errorMessage, boolean retry) {
        this.throwable = t;
        this.message = errorMessage;
        this.future.completeExceptionally(t);
        this.canBeRetried = retry;
    }

    @Override
    public void fail(@NotNull final String errorMessage, boolean retry) {
        this.message = errorMessage;
        this.future.completeExceptionally(new StackLessProtocolAdapterException(errorMessage));
        this.canBeRetried = retry;
    }
}
