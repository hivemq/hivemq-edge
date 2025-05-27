package com.hivemq.api.errors.validation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.api.errors.ApiError;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public abstract class ValidationError<E extends ValidationError<E>> {
    @JsonProperty("detail")
    protected @NotNull String detail;
    @JsonProperty("type")
    protected @NotNull String type;

    protected ValidationError(@NotNull final String detail) {
        this(detail, null);
    }

    protected ValidationError(@NotNull final String detail, final @Nullable String type) {
        setDetail(detail);
        setType(type);
    }

    public @NotNull String getDetail() {
        return detail;
    }

    public @NotNull ValidationError<E> setDetail(@NotNull final String detail) {
        this.detail = Objects.requireNonNull(detail);
        return this;
    }

    public @NotNull String getType() {
        return type;
    }

    public @NotNull ValidationError<E> setType(final @Nullable String type) {
        this.type = type == null ? ApiError.getTypeFromClassName(getClass()) : type;
        return this;
    }

    @Override
    public @NotNull String toString() {
        return "ValidationError{" + "detail='" + detail + '\'' + ", type='" + type + '\'' + '}';
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ValidationError<?> that = (ValidationError<?>) o;
        return Objects.equals(detail, that.detail) && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(detail, type);
    }
}
