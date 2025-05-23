package com.hivemq.http.error;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ErrorDataPolicyValidation extends Error {
    public ErrorDataPolicyValidation(final @NotNull String detail, final @Nullable String parameter) {
        super(detail, parameter);
    }

    public ErrorDataPolicyValidation(final @NotNull String detail) {
        super(detail);
    }
}
