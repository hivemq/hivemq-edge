package com.hivemq.api.errors.common;

import com.hivemq.api.errors.ApiError;
import com.hivemq.http.HttpStatus;
import org.jetbrains.annotations.NotNull;

public class TemporaryNotAvailableError extends ApiError<TemporaryNotAvailableError> {
    protected TemporaryNotAvailableError() {
        super("errors/common/TemporaryNotAvailableError",
                "The endpoint is temporarily not available",
                "The endpoint is temporarily not available, please try again later",
                HttpStatus.SERVICE_UNAVAILABLE_503,
                null);
    }

    public static TemporaryNotAvailableError of() {
        return new TemporaryNotAvailableError();
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
