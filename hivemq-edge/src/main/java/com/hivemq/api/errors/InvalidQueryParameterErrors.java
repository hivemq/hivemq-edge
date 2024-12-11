package com.hivemq.api.errors;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.Error;
import com.hivemq.http.error.ProblemDetails;

import java.util.List;

public class InvalidQueryParameterErrors extends ProblemDetails {
    public InvalidQueryParameterErrors(List<Error> errors) {
        super(
                "InvalidQueryParameter",
                "Query parameter is invalid",
                "Query parameter is invalid",
                HttpStatus.BAD_REQUEST_400,
                errors);
    }
}
