package com.hivemq.api.errors;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.Error;
import com.hivemq.http.error.ProblemDetails;

import java.util.List;

public class BodyParameterMissingError extends ProblemDetails {
    public BodyParameterMissingError(String parameterName) {
        super(
                "BodyParameterMissing",
                "Required request body parameter missing",
                "Required request body parameter missing",
                HttpStatus.BAD_REQUEST_400,
                List.of(new Error("Request body parameter missing: " + parameterName, parameterName)));
    }
}
