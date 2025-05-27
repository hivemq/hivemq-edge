package com.hivemq.api.errors.datahub;

import com.hivemq.api.errors.validation.ValidationError;
import com.hivemq.api.errors.validation.ValidationErrors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DataPolicyValidationErrors extends ValidationErrors<DataPolicyValidationErrors> {
    protected DataPolicyValidationErrors(
            final @NotNull String title,
            final @Nullable String detail,
            final @Nullable List<ValidationError<?>> errors,
            final int status,
            final @Nullable String code) {
        super(title, detail, errors, status, code);
    }

    public static DataPolicyValidationErrors of(
            final @Nullable List<ValidationError<?>> errors) {
        return new DataPolicyValidationErrors("Data policy is invalid", "Data policy is invalid", errors, 400, null);
    }

    @Override
    public @NotNull String toString() {
        return "DataPolicyValidationErrors{" +
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
