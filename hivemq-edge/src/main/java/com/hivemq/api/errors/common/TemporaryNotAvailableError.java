package com.hivemq.api.errors.common;

import com.hivemq.api.errors.ErrorBase;
import com.hivemq.http.HttpStatus;
import org.jetbrains.annotations.NotNull;

public class TemporaryNotAvailableError extends ErrorBase<TemporaryNotAvailableError> {
    public TemporaryNotAvailableError() {
        super("errors/common/TemporaryNotAvailableError",
                "The endpoint is temporarily not available",
                "The endpoint is temporarily not available, please try again later",
                HttpStatus.SERVICE_UNAVAILABLE_503,
                null);
    }

    @Override
    public @NotNull String toString() {
        return "TemporaryNotAvailableError{" +
                "code='" +
                code +
                '\'' +
                ", detail='" +
                detail +
                '\'' +
                ", status=" +
                status +
                ", title='" +
                title +
                '\'' +
                ", type='" +
                type +
                '\'' +
                '}';
    }
}
