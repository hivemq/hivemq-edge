package com.hivemq.api.errors.common;

import com.hivemq.api.errors.Error;
import com.hivemq.http.HttpStatus;

public class TemporaryNotAvailableError extends Error<TemporaryNotAvailableError> {
    public TemporaryNotAvailableError() {
        super("errors/common/TemporaryNotAvailableError",
                "The endpoint is temporarily not available",
                "The endpoint is temporarily not available, please try again later",
                HttpStatus.SERVICE_UNAVAILABLE_503,
                null);
    }
}
