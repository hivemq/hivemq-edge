package com.hivemq.api.errors;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.ErrorWithParameter;
import com.hivemq.http.error.ErrorsWithParameter;
import com.hivemq.http.error.ErrorsWithoutParameter;

import java.util.List;

public class BodyParameterMissingError extends ErrorsWithParameter {
    public BodyParameterMissingError(String parameterName) {
        super(
                "BodyParameterMissing",
                "Required request body parameter missing",
                "Required request body parameter missing",
                HttpStatus.BAD_REQUEST_400,
                List.of(new ErrorWithParameter("Request body parameter missing: " + parameterName, parameterName, null, null)));
    }
}
