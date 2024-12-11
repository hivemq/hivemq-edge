package com.hivemq.api.errors;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.ProblemDetails;

import java.util.List;

public class TemporaryNotAvailableError extends ProblemDetails {
    public TemporaryNotAvailableError() {
        super(
                "TemporaryUnavailable",
                "The endpoint is temporarily not available",
                "The endpoint is temporarily not available, please try again later",
                HttpStatus.SERVICE_UNAVAILABLE_503,
                List.of());
    }
}
