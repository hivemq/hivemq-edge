package com.hivemq.api.errors.validation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.api.errors.ApiError;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class ValidationErrors<E> extends ApiError<ValidationErrors<E>> {
    @JsonProperty("errors")
    protected final @NotNull List<ValidationError<?>> errors;

    protected ValidationErrors(
            final @NotNull String title,
            final @Nullable String detail,
            final @Nullable List<ValidationError<?>> errors,
            final int status,
            final @Nullable String code) {
        super(title, detail, status, code);
        this.errors = new ArrayList<>();
        if (errors != null && !errors.isEmpty()) {
            this.errors.addAll(errors);
        }
    }

    @Override
    public @NotNull String toString() {
        return "ValidationErrors{" +
                "errors=" +
                errors +
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
    public boolean equals(final @Nullable Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final ValidationErrors<?> that = (ValidationErrors<?>) o;
        return Objects.equals(errors, that.errors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), errors);
    }

    public @NotNull List<ValidationError<?>> getErrors() {
        return errors;
    }
}
