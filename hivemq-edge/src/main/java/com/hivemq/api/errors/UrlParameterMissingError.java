package com.hivemq.api.errors;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.ErrorWithParameter;
import com.hivemq.http.error.ErrorsWithParameter;

import java.util.List;

public class UrlParameterMissingError extends ErrorsWithParameter {
    public UrlParameterMissingError(String parameterName) {
        super(
                "UrlParameterMissing",
                "Required url parameter missing",
                "Required url parameter missing",
                HttpStatus.BAD_REQUEST_400,
                List.of(new ErrorWithParameter("URL parameter missing: " + parameterName, parameterName, null, null)));
    }
}
