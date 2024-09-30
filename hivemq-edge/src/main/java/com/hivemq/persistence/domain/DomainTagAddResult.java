package com.hivemq.persistence.domain;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

public class DomainTagAddResult {

    private final @NotNull DomainTagPutStatus dataPolicyPutStatus;
    private final @Nullable String errorMessage;

    public DomainTagAddResult(
            final @NotNull DomainTagPutStatus dataPolicyPutStatus, final @Nullable String errorMessage) {
        this.dataPolicyPutStatus = dataPolicyPutStatus;
        this.errorMessage = errorMessage;
    }

    public static @NotNull DomainTagAddResult success() {
        return new DomainTagAddResult(DomainTagPutStatus.SUCCESS, null);
    }

    public static @NotNull DomainTagAddResult failed(final @NotNull DomainTagPutStatus putStatus) {
        return new DomainTagAddResult(putStatus, null);
    }

    public static @NotNull DomainTagAddResult failed(
            final @NotNull DomainTagPutStatus putStatus, final @Nullable String errorMessage) {
        return new DomainTagAddResult(putStatus, errorMessage);
    }

    public @NotNull DomainTagPutStatus getDomainTagPutStatus() {
        return dataPolicyPutStatus;
    }

    public @Nullable String getErrorMessage() {
        return errorMessage;
    }

    public enum DomainTagPutStatus {
        SUCCESS(),
        ALREADY_EXISTS(),
        INSUFFICIENT_STORAGE(),
        USER_ERROR(),
        INTERNAL_ERROR();
    }


}
