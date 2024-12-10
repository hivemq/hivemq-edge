package com.hivemq.api.errors;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.Error;
import com.hivemq.http.error.ErrorsWithoutParameter;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AlreadyExistsError extends ErrorsWithoutParameter {
    public AlreadyExistsError(
            final @Nullable String error) {
        super(
                "ResourceAlreadyExists",
                "The resource already exists",
                "Requested to create a resource which already exists",
                HttpStatus.CONFLICT_409,
                List.of(new Error(error == null ? "An unexpected error occurred, check the logs" : error)));
    }
}
