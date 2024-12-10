package com.hivemq.http.error;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ErrorsWithoutParameter extends Errors<Error> {
    @JsonCreator
    public ErrorsWithoutParameter(
            final @NotNull String type,
            final @NotNull String title,
            final @Nullable String detail,
            final int status,
            final @NotNull List<Error> errors) {
        super(type, title, detail, status, errors);
    }
}
