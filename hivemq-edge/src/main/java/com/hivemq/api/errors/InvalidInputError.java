package com.hivemq.api.errors;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.Error;
import com.hivemq.http.error.Errors;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class InvalidInputError extends Errors {
    public InvalidInputError(
            final @Nullable String error) {
        super(
                "InvalidInputError",
                "Invalid input",
                "JSON failed validation.",
                HttpStatus.BAD_REQUEST_400,
                List.of(new Error("Unparseable JSON: " + error, null, null, null)));
    }
}
