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

import java.util.Objects;

public final class InvalidFunctionOrderValidationError extends ValidationError<InvalidFunctionOrderValidationError> {
    @JsonProperty("field")
    private @NotNull String field;

    @JsonProperty("function")
    private @NotNull String function;

    @JsonProperty("previousFunction")
    private @NotNull String previousFunction;

    private InvalidFunctionOrderValidationError(
            final @NotNull String detail,
            final @NotNull String field,
            final @NotNull String function,
            final @NotNull String previousFunction) {
        super(detail);
        setField(field);
        setFunction(function);
        setPreviousFunction(previousFunction);
    }

    public static @NotNull InvalidFunctionOrderValidationError of(
            final @NotNull String field,
            final @NotNull String function,
            final @NotNull String previousFunction) {
        return new InvalidFunctionOrderValidationError("The operation at '" +
                field +
                "' with the functionId '" +
                function +
                "' must be after a '" +
                previousFunction +
                "' operation.", field, function, previousFunction);
    }

    public static @NotNull InvalidFunctionOrderValidationError of(
            final @NotNull String detail,
            final @NotNull String field,
            final @NotNull String function,
            final @NotNull String previousFunction) {
        return new InvalidFunctionOrderValidationError(detail, field, function, previousFunction);
    }

    public @NotNull String getField() {
        return field;
    }

    public @NotNull InvalidFunctionOrderValidationError setField(@NotNull final String field) {
        this.field = Objects.requireNonNull(field);
        return this;
    }

    public @NotNull String getPreviousFunction() {
        return previousFunction;
    }

    public @NotNull InvalidFunctionOrderValidationError setPreviousFunction(@NotNull final String previousFunction) {
        this.previousFunction = Objects.requireNonNull(previousFunction);
        return this;
    }

    public @NotNull String getFunction() {
        return function;
    }

    public @NotNull InvalidFunctionOrderValidationError setFunction(@NotNull final String function) {
        this.function = Objects.requireNonNull(function);
        return this;
    }
}
