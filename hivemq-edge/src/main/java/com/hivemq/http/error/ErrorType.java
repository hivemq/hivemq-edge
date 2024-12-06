package com.hivemq.http.error;

import org.jetbrains.annotations.NotNull;

public class ErrorType {
    private final @NotNull String type;
    private final @NotNull String title;
    private final @NotNull String detail;

    public ErrorType(@NotNull final String type, @NotNull final String title, @NotNull final String detail) {
        this.type = type;
        this.title = title;
        this.detail = detail;
    }

    public @NotNull String getType() {
        return type;
    }

    public @NotNull String getTitle() {
        return title;
    }

    public @NotNull String getDetail() {
        return detail;
    }
}
