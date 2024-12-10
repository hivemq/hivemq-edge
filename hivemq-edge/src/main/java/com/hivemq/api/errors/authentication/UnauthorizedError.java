package com.hivemq.api.errors.authentication;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.Error;
import com.hivemq.http.error.ErrorWithParameter;
import com.hivemq.http.error.ErrorsWithParameter;
import com.hivemq.http.error.ErrorsWithoutParameter;

import java.util.List;

public class UnauthorizedError extends ErrorsWithoutParameter {
    public UnauthorizedError(String error) {
        super(
                "Unauthorized",
                "Unauthorized",
                "Unauthorized",
                HttpStatus.UNAUTHORIZED,
                List.of(new Error(error)));
    }
}
