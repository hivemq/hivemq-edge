package com.hivemq.persistence.domain;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

//TODO is it worth the inheritence?
public class PersistenceResult<S> {

    private final @NotNull S status;
    private final @Nullable String errorMessage;

    public PersistenceResult(
            final @NotNull S status, final @Nullable String errorMessage) {
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public static @NotNull <S>PersistenceResult<S> failed(final @NotNull S putStatus) {
        return new PersistenceResult(putStatus, null);
    }

    public static @NotNull <S>PersistenceResult<S> failed(
            final @NotNull S putStatus, final @Nullable String errorMessage) {
        return new PersistenceResult(putStatus, errorMessage);
    }

    public @NotNull S getS() {
        return status;
    }

    public @Nullable String getErrorMessage() {
        return errorMessage;
    }

}
