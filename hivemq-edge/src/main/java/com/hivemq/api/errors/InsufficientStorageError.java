package com.hivemq.api.errors;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.Error;
import com.hivemq.http.error.ProblemDetails;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class InsufficientStorageError extends ProblemDetails {
    public InsufficientStorageError(
            final @NotNull String cause) {
        super(
                "InsufficientStorage",
                "Insufficient Storage",
                "Insufficient Storage",
                HttpStatus.INSUFFICIENT_STORAGE_507,
                List.of(new Error(cause)));
    }
}
