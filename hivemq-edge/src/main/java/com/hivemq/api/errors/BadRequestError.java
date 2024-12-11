package com.hivemq.api.errors;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.Error;
import com.hivemq.http.error.ProblemDetails;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BadRequestError extends ProblemDetails {
    public BadRequestError(
            final @Nullable String error) {
        super(
                "BadRequestError",
                "Request could not be processed",
                "Request could not be processed",
                HttpStatus.BAD_REQUEST_400,
                List.of(new Error(error)));
    }
}
