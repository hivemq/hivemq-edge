/*
 *  Copyright 2019-present HiveMQ GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.hivemq.api.errors.datahub;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.api.errors.validation.ValidationError;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class UnknownVariableValidationError extends ValidationError<UnknownVariableValidationError> {
    @JsonProperty("variables")
    private final @NotNull List<String> variables;

    @JsonProperty("field")
    private @NotNull String field;

    private UnknownVariableValidationError(
            final @NotNull String detail,
            final @NotNull String field,
            final @Nullable List<String> variables) {
        super(detail);
        setField(field);
        this.variables = new ArrayList<>();
        if (variables != null && !variables.isEmpty()) {
            this.variables.addAll(variables);
        }
    }

    public static @NotNull UnknownVariableValidationError of(
            final @NotNull String field,
            final @Nullable List<String> variables) {
        return new UnknownVariableValidationError("Field '" +
                field +
                "' contains unknown variables: [" +
                variables +
                "].", field, variables);
    }

    public static @NotNull UnknownVariableValidationError of(
            final @NotNull String detail,
            final @NotNull String field,
            final @Nullable List<String> variables) {
        return new UnknownVariableValidationError(detail, field, variables);
    }

    public @NotNull String getField() {
        return field;
    }

    public @NotNull UnknownVariableValidationError setField(@NotNull final String field) {
        this.field = Objects.requireNonNull(field);
        return this;
    }

    @Override
    public @NotNull String toString() {
        return "UnknownVariableValidationError{" +
                "variables=" +
                variables +
                ", field='" +
                field +
                '\'' +
                ", detail='" +
                detail +
                '\'' +
                ", type='" +
                type +
                '\'' +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final UnknownVariableValidationError that = (UnknownVariableValidationError) o;
        return Objects.equals(variables, that.variables) && Objects.equals(field, that.field);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), variables, field);
    }

    public @NotNull List<String> getVariables() {
        return variables;
    }
}
