package com.hivemq.api.errors;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.ErrorWithParameter;
import com.hivemq.http.error.ErrorsWithParameter;

import java.util.List;

public class InvalidQueryParameterErrors extends ErrorsWithParameter {
    public InvalidQueryParameterErrors(List<ErrorWithParameter> errors) {
        super(
                "InvalidQueryParameter",
                "Query parameter is invalid",
                "Query parameter is invalid",
                HttpStatus.BAD_REQUEST_400,
                errors);
    }
}
