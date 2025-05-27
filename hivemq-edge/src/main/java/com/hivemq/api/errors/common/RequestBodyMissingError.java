package com.hivemq.api.errors.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.api.errors.ApiError;
import com.hivemq.http.HttpStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class RequestBodyMissingError extends ApiError<RequestBodyMissingError> {
    @JsonProperty("parameter")
    @Schema(description = "Parameter")
    protected @Nullable String parameter;

    protected RequestBodyMissingError(
            final @NotNull String title,
            final @Nullable String detail,
            final @Nullable String parameter) {
        super(title, detail, HttpStatus.BAD_REQUEST_400, null);
        setParameter(parameter);
    }

    public static RequestBodyMissingError of(final @NotNull String parameter) {
        return new RequestBodyMissingError("Required request body parameter " + parameter + " missing",
                "Required request body parameter " + parameter + " missing",
                parameter);
    }

    public static RequestBodyMissingError of() {
        return new RequestBodyMissingError("Required request body missing", "Required request body missing", null);
    }

    public @Nullable String getParameter() {
        return parameter;
    }

    public @NotNull RequestBodyMissingError setParameter(final @Nullable String parameter) {
        this.parameter = parameter;
        return this;
    }

    @Override
    public @NotNull String toString() {
        return "RequestBodyMissingError{" +
                "parameter='" +
                parameter +
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
        return Objects.equals(parameter, that.parameter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), parameter);
    }
}
