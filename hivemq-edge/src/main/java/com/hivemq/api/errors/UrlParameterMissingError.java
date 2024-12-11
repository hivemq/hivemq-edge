package com.hivemq.api.errors;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.Error;
import com.hivemq.http.error.ProblemDetails;

import java.util.List;

public class UrlParameterMissingError extends ProblemDetails {
    public UrlParameterMissingError(String parameterName) {
        super(
                "UrlParameterMissing",
                "Required url parameter missing",
                "Required url parameter missing",
                HttpStatus.BAD_REQUEST_400,
                List.of(new Error("URL parameter missing: " + parameterName, parameterName)));
    }
}
