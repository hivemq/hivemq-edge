package com.hivemq.api.errors;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.Error;
import com.hivemq.http.error.ProblemDetails;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ValidationError extends ProblemDetails {
    public ValidationError(
            final @Nullable List<Error> errors) {
        super(
                "ValidationError",
                "Validatin failed",
                "JSON failed validation.",
                HttpStatus.BAD_REQUEST_400,
                errors);
    }
}
