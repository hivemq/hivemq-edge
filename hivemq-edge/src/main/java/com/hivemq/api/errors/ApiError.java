package com.hivemq.api.errors;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public abstract class ApiError<E extends ApiError<E>> {
    private static final String CLASS_PREFIX = "com.hivemq";
    private static final String TYPE_PREFIX = "https://hivemq.com";

    @JsonProperty("code")
    @Schema(description = "Correlation id")
    protected @Nullable String code;

    @JsonProperty("detail")
    protected @Nullable String detail;

    @JsonProperty("status")
    protected int status;

    @JsonProperty(value = "title", required = true)
    protected @NotNull String title;

    @JsonProperty(value = "type")
    protected @NotNull String type;

    protected ApiError(
            final @NotNull String title,
            final @Nullable String detail,
            final int status,
            final @Nullable String code) {
        this(null, title, detail, status, code);
    }

    protected ApiError(
            final @Nullable String type,
            final @NotNull String title,
            final @Nullable String detail,
            final int status,
            final @Nullable String code) {
        setCode(code);
        setDetail(detail);
        setStatus(status);
        setTitle(title);
        setType(type);
    }

    public static String getTypeFromClassName(final @NotNull Class<?> clazz) {
        return TYPE_PREFIX + clazz.getName().substring(CLASS_PREFIX.length()).replace(".", "/");
    }

    public @Nullable String getCode() {
        return code;
    }

    public @NotNull ApiError<E> setCode(@Nullable final String code) {
        this.code = code;
        return this;
    }

    public @Nullable String getDetail() {
        return detail;
    }

    public @NotNull ApiError<E> setDetail(@Nullable final String detail) {
        this.detail = detail;
        return this;
    }

    public int getStatus() {
        return status;
    }

    public @NotNull ApiError<E> setStatus(final int status) {
        this.status = status;
        return this;
    }

    public @NotNull String getTitle() {
        return title;
    }

    public @NotNull ApiError<E> setTitle(@NotNull final String title) {
        this.title = Objects.requireNonNull(title);
        return this;
    }

    public @NotNull String getType() {
        return type;
    }

    public @NotNull ApiError<E> setType(@Nullable final String type) {
        this.type = type == null ? getTypeFromClassName(getClass()) : type;
        return this;
    }

    @Override
    public @NotNull String toString() {
        return "ErrorBase{" +
                "code='" +
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
        final ApiError<?> errorBase = (ApiError<?>) o;
        return status == errorBase.status &&
                Objects.equals(code, errorBase.code) &&
                Objects.equals(detail, errorBase.detail) &&
                Objects.equals(title, errorBase.title) &&
                Objects.equals(type, errorBase.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, detail, status, title, type);
    }
}
