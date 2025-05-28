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

package com.hivemq.api.errors.validation;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class UnsupportedFieldValidationError extends ValidationError<UnsupportedFieldValidationError> {
    @JsonProperty("actualValue")
    private @NotNull String actualValue;
    @JsonProperty("expectedValue")
    private @NotNull String expectedValue;
    @JsonProperty("field")
    private @NotNull String field;

    private UnsupportedFieldValidationError(
            final @NotNull String detail,
            final @NotNull String field,
            final @NotNull String actualValue,
            final @NotNull String expectedValue) {
        super(detail);
        setActualValue(actualValue);
        setExpectedValue(expectedValue);
        setField(field);
    }

    public static @NotNull UnsupportedFieldValidationError of(
            final @NotNull String detail,
            final @NotNull String field,
            final @NotNull String actualValue,
            final @NotNull String expectedValue) {
        return new UnsupportedFieldValidationError(detail, field, actualValue, expectedValue);
    }

    public static @NotNull UnsupportedFieldValidationError ofType(
            final @NotNull String field,
            final @NotNull String actualType,
            final @NotNull String expectedType) {
        return new UnsupportedFieldValidationError("Unsupported type '" +
                actualType +
                " for field '" +
                field +
                "'. Expected type is '" +
                expectedType +
                "'.", field, actualType, expectedType);
    }

    public static @NotNull UnsupportedFieldValidationError ofValue(
            final @NotNull String field,
            final @NotNull String actualValue,
            final @NotNull String expectedValue) {
        return new UnsupportedFieldValidationError("Unsupported value '" +
                actualValue +
                " for field '" +
                field +
                "'. Expected value is '" +
                expectedValue +
                "'.", field, actualValue, expectedValue);
    }

    public @NotNull String getExpectedValue() {
        return expectedValue;
    }

    public @NotNull UnsupportedFieldValidationError setExpectedValue(@NotNull final String expectedValue) {
        this.expectedValue = Objects.requireNonNull(expectedValue);
        return this;
    }

    public @NotNull String getActualValue() {
        return actualValue;
    }

    public @NotNull UnsupportedFieldValidationError setActualValue(final @NotNull String actualValue) {
        this.actualValue = Objects.requireNonNull(actualValue);
        return this;
    }

    public @NotNull String getField() {
        return field;
    }

    public @NotNull UnsupportedFieldValidationError setField(final @NotNull String field) {
        this.field = Objects.requireNonNull(field);
        return this;
    }
}
