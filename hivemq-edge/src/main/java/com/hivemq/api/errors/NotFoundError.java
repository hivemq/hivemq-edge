package com.hivemq.api.errors;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.ProblemDetails;

import java.util.List;

public class NotFoundError extends ProblemDetails {
    public NotFoundError() {
        super(
                "NotFoundError",
                "The resource could not be found",
                "The resource could not be found",
                HttpStatus.NOT_FOUND_404,
                List.of());
    }
}
