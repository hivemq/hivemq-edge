package com.hivemq.api.errors.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.api.errors.ErrorBase;
import com.hivemq.http.HttpStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class RequestBodyMissingError extends ErrorBase<RequestBodyMissingError> {
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

    @Override
    public @NotNull String toString() {
        return "RequestBodyMissingError{" +
                "parameterName='" +
                parameterName +
                '\'' +
                ", code='" +
                code +
                '\'' +
                ", detail='" +
                detail +
                '\'' +
                ", status=" +
                status +
                ", title='" +
                title +
                '\'' +
                ", type='" +
                type +
                '\'' +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final RequestBodyMissingError that = (RequestBodyMissingError) o;
        return Objects.equals(parameterName, that.parameterName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), parameterName);
    }
}
