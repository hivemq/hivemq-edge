package com.hivemq.api.errors;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.Error;
import com.hivemq.http.error.ErrorsWithoutParameter;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class InternalServerError extends ErrorsWithoutParameter {
    public InternalServerError(
            final @Nullable String error) {
        super(
                "InternalServerError",
                "InternalError",
                "Internal error",
                HttpStatus.INTERNAL_SERVER_ERROR_500,
                List.of(new Error(error == null ? "An unexpected error occurred, check the logs" : error)));
    }
}
