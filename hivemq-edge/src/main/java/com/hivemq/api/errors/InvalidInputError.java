package com.hivemq.api.errors;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.ErrorWithParameter;
import com.hivemq.http.error.ErrorsWithParameter;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class InvalidInputError extends ErrorsWithParameter {
    public InvalidInputError(
            final @Nullable String error) {
        super(
                "InvalidInputError",
                "Invalid input",
                "JSON failed validation.",
                HttpStatus.BAD_REQUEST_400,
                List.of(new ErrorWithParameter("Unparseable JSON: " + error, null, null, null)));
    }
}
