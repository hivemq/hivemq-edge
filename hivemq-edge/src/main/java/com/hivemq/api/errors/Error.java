package com.hivemq.api.errors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class Error<E extends Error<E>> {

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

    @JsonCreator
    public Error(
            @JsonProperty(value = "type") final @NotNull String type,
            @JsonProperty(value = "title") final @NotNull String title,
            @JsonProperty(value = "detail") final @Nullable String detail,
            @JsonProperty(value = "status") final int status,
            @JsonProperty(value = "code") final @Nullable String code) {
        setCode(code);
        setDetail(detail);
        setStatus(status);
        setTitle(title);
        setType(type);
    }

    public @Nullable String getCode() {
        return code;
    }

    public @NotNull Error<E> setCode(@Nullable final String code) {
        this.code = code;
        return this;
    }

    public @Nullable String getDetail() {
        return detail;
    }

    public @NotNull Error<E> setDetail(@Nullable final String detail) {
        this.detail = detail;
        return this;
    }

    public int getStatus() {
        return status;
    }

    public @NotNull Error<E> setStatus(final int status) {
        this.status = status;
        return this;
    }

    public @NotNull String getTitle() {
        return title;
    }

    public @NotNull Error<E> setTitle(@NotNull final String title) {
        this.title = Objects.requireNonNull(title);
        return this;
    }

    public @NotNull String getType() {
        return type;
    }

    public @NotNull Error<E> setType(@NotNull final String type) {
        this.type = Objects.requireNonNull(type);
        return this;
    }
}
