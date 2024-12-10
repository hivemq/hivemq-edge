package com.hivemq.api.errors.authentication;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.Error;
import com.hivemq.http.error.Errors;

import java.util.List;

public class AuthenticationValidationError extends Errors {
    public AuthenticationValidationError(List<Error> errors) {
        super(
                "AuthenticationValidationError",
                "Authentication request failed validation",
                "Parameters were missing in the authentication request",
                HttpStatus.BAD_REQUEST_400,
                errors);
    }
}
