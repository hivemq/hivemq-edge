package com.hivemq.edge.modules.api.adapters.model;

import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

/**
 * @author Simon L Johnson
 */
public class ProtocolAdapterValidationFailure {

    private @NotNull final String message;
    private @Nullable Class origin;
    private @Nullable Throwable cause;
    private @Nullable String fieldName;

    public ProtocolAdapterValidationFailure(final @NotNull String message) {
        Preconditions.checkNotNull(message);
        this.message = message;
    }

    public ProtocolAdapterValidationFailure(
            @NotNull final String message,
            @Nullable final String fieldName,
            @Nullable final Class origin,
            @Nullable final Throwable cause) {
        Preconditions.checkNotNull(message);
        this.message = message;
        this.origin = origin;
        this.cause = cause;
        this.fieldName = fieldName;
    }

    public ProtocolAdapterValidationFailure(
            @NotNull final String message,
            @Nullable final String fieldName,
            @Nullable final Class origin) {
        Preconditions.checkNotNull(message);
        this.message = message;
        this.origin = origin;
        this.fieldName = fieldName;
    }

    public @NotNull String getMessage() {
        return message;
    }

    public @Nullable Class getOrigin() {
        return origin;
    }

    public @Nullable Throwable getCause() {
        return cause;
    }

    public @Nullable String getFieldName() {
        return fieldName;
    }
}
