package com.hivemq.persistence.domain;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

public class DomainTagDeleteResult {

    private final @NotNull DomainTagDeleteStatus dataPolicyUpdateStatus;
    private final @Nullable String errorMessage;

    public DomainTagDeleteResult(
            final @NotNull DomainTagDeleteStatus dataPolicyUpdateStatus, final @Nullable String errorMessage) {
        this.dataPolicyUpdateStatus = dataPolicyUpdateStatus;
        this.errorMessage = errorMessage;
    }

    public static @NotNull DomainTagDeleteResult success() {
        return new DomainTagDeleteResult(DomainTagDeleteStatus.SUCCESS, null);
    }

    public static @NotNull DomainTagDeleteResult failed(final @NotNull DomainTagDeleteStatus putStatus) {
        return new DomainTagDeleteResult(putStatus,  null);
    }

    public static @NotNull DomainTagDeleteResult failed(
            final @NotNull DomainTagDeleteStatus deleteResult, final @Nullable String errorMessage) {
        return new DomainTagDeleteResult(deleteResult, errorMessage);
    }

    public @NotNull DomainTagDeleteStatus getDomainTagUpdateStatus() {
        return dataPolicyUpdateStatus;
    }


    public @Nullable String getErrorMessage() {
        return errorMessage;
    }


    public enum DomainTagDeleteStatus {
        SUCCESS(),
        NOT_FOUND(),
        INTERNAL_ERROR();
    }
}
