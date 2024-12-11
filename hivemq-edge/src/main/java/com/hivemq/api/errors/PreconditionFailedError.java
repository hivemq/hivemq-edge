package com.hivemq.api.errors;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.Error;
import com.hivemq.http.error.ProblemDetails;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PreconditionFailedError extends ProblemDetails {
    public PreconditionFailedError(
            final @NotNull String cause) {
        super(
                "PreconditionFailed",
                "Precondition Failed",
                "A precondition required for fulfilling the request was not fulfilled",
                HttpStatus.PRECONDITION_FAILED_412,
                List.of(new Error(cause)));
    }
}
