package com.hivemq.api.errors.datahub;

import com.hivemq.api.errors.validation.ValidationError;
import com.hivemq.api.errors.validation.ValidationErrors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BehaviorPolicyValidationErrors extends ValidationErrors<BehaviorPolicyValidationErrors> {
    protected BehaviorPolicyValidationErrors(
            final @NotNull String title,
            final @Nullable String detail,
            final @Nullable List<ValidationError<?>> errors,
            final int status,
            final @Nullable String code) {
        super(title, detail, errors, status, code);
    }

    public static BehaviorPolicyValidationErrors of(
            final @Nullable List<ValidationError<?>> errors) {
        return new BehaviorPolicyValidationErrors("Behavior policy is invalid",
                "Behavior policy is invalid",
                errors,
                400,
                null);
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
