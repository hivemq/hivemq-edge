package com.hivemq.persistence.domain;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

public class DomainTagUpdateResult {

    private final @NotNull DomainTagUpdateStatus dataPolicyUpdateStatus;
    private final @Nullable String errorMessage;

    public DomainTagUpdateResult(
            final @NotNull DomainTagUpdateStatus dataPolicyUpdateStatus, final @Nullable String errorMessage) {
        this.dataPolicyUpdateStatus = dataPolicyUpdateStatus;
        this.errorMessage = errorMessage;
    }

    public static @NotNull DomainTagUpdateResult success() {
        return new DomainTagUpdateResult(DomainTagUpdateStatus.SUCCESS, null);
    }

    public static @NotNull DomainTagUpdateResult failed(final @NotNull DomainTagUpdateStatus putStatus) {
        return new DomainTagUpdateResult(putStatus,  null);
    }

    public static @NotNull DomainTagUpdateResult failed(
            final @NotNull DomainTagUpdateStatus putStatus, final @Nullable String errorMessage) {
        return new DomainTagUpdateResult(putStatus, errorMessage);
    }

    public @NotNull DomainTagUpdateStatus getDomainTagUpdateStatus() {
        return dataPolicyUpdateStatus;
    }


    public @Nullable String getErrorMessage() {
        return errorMessage;
    }


    public enum DomainTagUpdateStatus {
        SUCCESS(),
        NOT_FOUND(),
        INTERNAL_ERROR();
    }
}
