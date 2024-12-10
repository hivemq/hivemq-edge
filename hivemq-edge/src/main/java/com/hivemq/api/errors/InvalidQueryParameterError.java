package com.hivemq.api.errors;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.ErrorWithParameter;
import com.hivemq.http.error.ErrorsWithParameter;

import java.util.List;

public class InvalidQueryParameterError extends ErrorsWithParameter {
    public InvalidQueryParameterError(String parameterName, String reason) {
        super(
                "InvalidQueryParameter",
                "Query parameter is invalid",
                "Query parameter is invalid",
                HttpStatus.BAD_REQUEST_400,
                List.of(new ErrorWithParameter(String.format("Query parameter %s is invalid: %s",  parameterName, reason), parameterName, null, null)));
    }
}
