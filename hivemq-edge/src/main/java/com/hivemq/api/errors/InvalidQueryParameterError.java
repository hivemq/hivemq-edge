package com.hivemq.api.errors;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.Error;
import com.hivemq.http.error.Errors;

import java.util.List;

public class InvalidQueryParameterError extends Errors {
    public InvalidQueryParameterError(String parameterName, String reason) {
        super(
                "InvalidQueryParameter",
                "Query parameter is invalid",
                "Query parameter is invalid",
                HttpStatus.BAD_REQUEST_400,
                List.of(new Error(String.format("Query parameter %s is invalid: %s",  parameterName, reason), parameterName, null, null)));
    }
}
