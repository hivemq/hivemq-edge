package com.hivemq.protocols;

import com.hivemq.edge.modules.adapters.model.ProtocolAdapterStartOutput;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

public class ProtocolAdapterStartOutputImpl implements ProtocolAdapterStartOutput {

    //default: all good
    boolean startedSuccessfully = true;
    @Nullable String message = null;
    @Nullable Throwable throwable;

    @Override
    public void startedSuccessfully(@NotNull final String message) {
        startedSuccessfully = true;
        this.message = message;
    }

    @Override
    public void failStart(@NotNull Throwable t, @Nullable final String errorMessage) {
        startedSuccessfully = false;
        this.throwable = t;
        this.message = errorMessage;
    }

    public @Nullable Throwable getThrowable() {
        return throwable;
    }

    public boolean isStartedSuccessfully() {
        return startedSuccessfully;
    }

    public @Nullable String getMessage() {
        return message;
    }
}
