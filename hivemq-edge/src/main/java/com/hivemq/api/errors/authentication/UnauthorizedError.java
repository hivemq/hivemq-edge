package com.hivemq.api.errors.authentication;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.Error;
import com.hivemq.http.error.ProblemDetails;

import java.util.List;

public class UnauthorizedError extends ProblemDetails {
    public UnauthorizedError(String error) {
        super(
                "Unauthorized",
                "Unauthorized",
                "Unauthorized",
                HttpStatus.UNAUTHORIZED,
                List.of(new Error(error)));
    }
}
