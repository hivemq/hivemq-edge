package com.hivemq.api.errors;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.Error;
import com.hivemq.http.error.ErrorWithParameter;
import com.hivemq.http.error.ErrorsWithParameter;
import com.hivemq.http.error.ErrorsWithoutParameter;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ValidationError extends ErrorsWithParameter {
    public ValidationError(
            final @Nullable List<ErrorWithParameter> errors) {
        super(
                "ValidationError",
                "Validatin failed",
                "JSON failed validation.",
                HttpStatus.BAD_REQUEST_400,
                errors);
    }
}
