package com.hivemq.api.errors.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.api.errors.Error;
import com.hivemq.http.HttpStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class RequestBodyMissingError extends Error<RequestBodyMissingError> {
    @JsonProperty("parameterName")
    @Schema(description = "Correlation id")
    protected @NotNull String parameterName;

    public RequestBodyMissingError(final @NotNull String parameterName) {
        super("errors/common/RequestBodyMissingError",
                "Required request body parameter " + parameterName + " missing",
                "Required request body parameter " + parameterName + " missing",
                HttpStatus.BAD_REQUEST_400,
                null);
        setParameterName(parameterName);
    }

    public @NotNull String getParameterName() {
        return parameterName;
    }

    public @NotNull RequestBodyMissingError setParameterName(final @NotNull String parameterName) {
        this.parameterName = Objects.requireNonNull(parameterName);
        return this;
    }
}
