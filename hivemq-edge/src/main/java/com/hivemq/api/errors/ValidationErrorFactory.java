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

package com.hivemq.api.errors;

import com.hivemq.edge.api.model.EmptyFieldValidationError;
import com.hivemq.edge.api.model.InvalidFieldLengthValidationError;
import com.hivemq.edge.api.model.InvalidFieldValueValidationError;
import com.hivemq.edge.api.model.InvalidIdentifierValidationError;
import com.hivemq.edge.api.model.MissingFieldValidationError;
import com.hivemq.edge.api.model.UnsupportedFieldValidationError;
import org.jetbrains.annotations.NotNull;

public final class ValidationErrorFactory extends ErrorFactory {
    private ValidationErrorFactory() {
        super();
    }

    public static @NotNull EmptyFieldValidationError emptyFieldValidationError(
            final @NotNull String field) {
        return emptyFieldValidationError("Required field '" + field + "' is empty", field);
    }

    public static @NotNull EmptyFieldValidationError emptyFieldValidationError(
            final @NotNull String detail,
            final @NotNull String field) {
        return EmptyFieldValidationError.builder()
                .type(type(EmptyFieldValidationError.class))
                .detail(detail)
                .field(field)
                .build();
    }

    public static @NotNull InvalidFieldLengthValidationError invalidFieldLengthValidationError(
            final @NotNull String detail,
            final @NotNull String field,
            final @NotNull String value,
            final int actualLength,
            final int expectedMinimumLength,
            final int expectedMaximumLength) {
        return InvalidFieldLengthValidationError.builder()
                .type(type(InvalidFieldLengthValidationError.class))
                .detail(detail)
                .field(field)
                .value(value)
                .actualLength(actualLength)
                .expectedMinimumLength(expectedMinimumLength)
                .expectedMaximumLength(expectedMaximumLength)
                .build();
    }

    public static @NotNull InvalidFieldValueValidationError invalidFieldValueValidationError(
            final @NotNull String detail,
            final @NotNull String field,
            final @NotNull String value) {
        return InvalidFieldValueValidationError.builder()
                .type(type(InvalidFieldValueValidationError.class))
                .detail(detail)
                .field(field)
                .value(value)
                .build();
    }

    public static @NotNull InvalidIdentifierValidationError invalidIdentifierValidationError(
            final @NotNull String field,
            final @NotNull String value) {
        return invalidIdentifierValidationError("Identifier " +
                field +
                " must begin with a letter and may only consist of lowercase letters," +
                " uppercase letters, numbers, periods, hyphens, and underscores", field, value);
    }

    public static @NotNull InvalidIdentifierValidationError invalidIdentifierValidationError(
            final @NotNull String detail,
            final @NotNull String field,
            final @NotNull String value) {
        return InvalidIdentifierValidationError.builder()
                .type(type(InvalidIdentifierValidationError.class))
                .detail(detail)
                .field(field)
                .value(value)
                .build();
    }

    public static @NotNull MissingFieldValidationError missingFieldValidationError(
            final @NotNull String field) {
        return missingFieldValidationError("Required field '" + field + "' is missing.", field);
    }

    public static @NotNull MissingFieldValidationError missingFieldValidationError(
            final @NotNull String detail,
            final @NotNull String field) {
        return MissingFieldValidationError.builder()
                .type(type(MissingFieldValidationError.class))
                .detail(detail)
                .field(field)
                .build();
    }

    public static @NotNull UnsupportedFieldValidationError unsupportedFieldValidationError(
            final @NotNull String detail,
            final @NotNull String field,
            final @NotNull String actualValue,
            final @NotNull String expectedValue) {
        return UnsupportedFieldValidationError.builder()
                .type(type(UnsupportedFieldValidationError.class))
                .detail(detail)
                .field(field)
                .actualValue(actualValue)
                .expectedValue(expectedValue)
                .build();
    }

    public static @NotNull UnsupportedFieldValidationError unsupportedFieldValidationErrorByType(
            final @NotNull String field,
            final @NotNull String actualType,
            final @NotNull String expectedType) {
        return unsupportedFieldValidationError("Unsupported type '" +
                actualType +
                " for field '" +
                field +
                "'. Expected type is '" +
                expectedType +
                "'.", field, actualType, expectedType);
    }

    public static @NotNull UnsupportedFieldValidationError unsupportedFieldValidationErrorByValue(
            final @NotNull String field,
            final @NotNull String actualValue,
            final @NotNull String expectedValue) {
        return unsupportedFieldValidationError("Unsupported value '" +
                actualValue +
                " for field '" +
                field +
                "'. Expected value is '" +
                expectedValue +
                "'.", field, actualValue, expectedValue);
    }
}
