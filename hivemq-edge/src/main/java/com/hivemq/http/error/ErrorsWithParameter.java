package com.hivemq.http.error;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ErrorsWithParameter extends Errors<ErrorWithParameter> {
    @JsonCreator
    public ErrorsWithParameter(
            @JsonProperty("type")
            final @NotNull String type,
            @JsonProperty("title")
            final @NotNull String title,
            @JsonProperty("detail")
            final @Nullable String detail,
            @JsonProperty("status")
            final int status,
            @JsonProperty("errors")
            final @NotNull List<ErrorWithParameter> errors) {
        super(type, title, detail, status, errors);
    }
}
