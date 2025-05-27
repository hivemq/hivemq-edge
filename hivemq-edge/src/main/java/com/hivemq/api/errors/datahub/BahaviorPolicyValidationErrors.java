package com.hivemq.api.errors.datahub;

import com.hivemq.api.errors.validation.ValidationError;
import com.hivemq.api.errors.validation.ValidationErrors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BahaviorPolicyValidationErrors extends ValidationErrors<BahaviorPolicyValidationErrors> {
    protected BahaviorPolicyValidationErrors(
            final @NotNull String title,
            final @Nullable String detail,
            final @Nullable List<ValidationError<?>> errors,
            final int status,
            final @Nullable String code) {
        super(title, detail, errors, status, code);
    }

    public static BahaviorPolicyValidationErrors of(final @NotNull String title, final @Nullable String detail) {
        return new BahaviorPolicyValidationErrors(title, detail, null, 400, null);
    }

    public static BahaviorPolicyValidationErrors of(
            final @NotNull String title,
            final @Nullable String detail,
            final @Nullable List<ValidationError<?>> errors) {
        return new BahaviorPolicyValidationErrors(title, detail, errors, 400, null);
    }

    @Override
    public @NotNull String toString() {
        return "BahaviorPolicyValidationErrors{" +
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
}
