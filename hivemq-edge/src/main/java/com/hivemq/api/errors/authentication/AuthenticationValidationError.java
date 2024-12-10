package com.hivemq.api.errors.authentication;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.ErrorWithParameter;
import com.hivemq.http.error.ErrorsWithParameter;

import java.util.List;

public class AuthenticationValidationError extends ErrorsWithParameter {
    public AuthenticationValidationError(List<ErrorWithParameter> errors) {
        super(
                "AuthenticationValidationError",
                "Authentication request failed validation",
                "Parameters were missing in the authentication request",
                HttpStatus.BAD_REQUEST_400,
                errors);
    }
}
