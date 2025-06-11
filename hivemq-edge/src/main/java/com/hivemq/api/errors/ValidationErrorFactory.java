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

import com.hivemq.edge.api.model.AtLeastOneFieldMissingValidationError;
import com.hivemq.edge.api.model.EmptyFieldValidationError;
import com.hivemq.edge.api.model.InvalidFieldLengthValidationError;
import com.hivemq.edge.api.model.InvalidFieldValueValidationError;
import com.hivemq.edge.api.model.InvalidIdentifierValidationError;
import com.hivemq.edge.api.model.MissingFieldValidationError;
import com.hivemq.edge.api.model.UnsupportedFieldValidationError;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ValidationErrorFactory extends ErrorFactory {
    private ValidationErrorFactory() {
        super();
    }

    public static @NotNull AtLeastOneFieldMissingValidationError atLeastOneFieldMissingValidationError(final @NotNull String... paths) {
        return AtLeastOneFieldMissingValidationError.builder()
                .type(type(AtLeastOneFieldMissingValidationError.class))
                .detail("At least one of the fields must be present: " +
                        Stream.of(paths).map(path -> "'" + path + "'").collect(Collectors.joining(", ")) +
                        ".")
                .paths(List.of(paths))
                .build();
    }

    public static @NotNull EmptyFieldValidationError emptyFieldValidationError(
            final @NotNull String path) {
        return emptyFieldValidationError("Required field '" + path + "' is empty", path);
    }

    public static @NotNull EmptyFieldValidationError emptyFieldValidationError(
            final @NotNull String detail,
            final @NotNull String path) {
        return EmptyFieldValidationError.builder()
                .type(type(EmptyFieldValidationError.class))
                .detail(detail)
                .path(path)
                .build();
    }

    public static @NotNull InvalidFieldLengthValidationError invalidFieldLengthValidationError(
            final @NotNull String detail,
            final @NotNull String path,
            final @NotNull String value,
            final int actualLength,
            final int expectedMinimumLength,
            final int expectedMaximumLength) {
        return InvalidFieldLengthValidationError.builder()
                .type(type(InvalidFieldLengthValidationError.class))
                .detail(detail)
                .path(path)
                .value(value)
                .actualLength(actualLength)
                .expectedMinimumLength(expectedMinimumLength)
                .expectedMaximumLength(expectedMaximumLength)
                .build();
    }

    public static @NotNull InvalidFieldValueValidationError invalidFieldValueValidationError(
            final @NotNull String detail,
            final @NotNull String path,
            final @NotNull String value) {
        return InvalidFieldValueValidationError.builder()
                .type(type(InvalidFieldValueValidationError.class))
                .detail(detail)
                .path(path)
                .value(value)
                .build();
    }

    public static @NotNull InvalidIdentifierValidationError invalidIdentifierValidationError(
            final @NotNull String path,
            final @NotNull String value) {
        return invalidIdentifierValidationError("Identifier " +
                path +
                " must begin with a letter and may only consist of lowercase letters," +
                " uppercase letters, numbers, periods, hyphens, and underscores", path, value);
    }

    public static @NotNull InvalidIdentifierValidationError invalidIdentifierValidationError(
            final @NotNull String detail,
            final @NotNull String path,
            final @NotNull String value) {
        return InvalidIdentifierValidationError.builder()
                .type(type(InvalidIdentifierValidationError.class))
                .detail(detail)
                .path(path)
                .value(value)
                .build();
    }

    public static @NotNull MissingFieldValidationError missingFieldValidationError(
            final @NotNull String path) {
        return missingFieldValidationError("Required field '" + path + "' is missing.", path);
    }

    public static @NotNull MissingFieldValidationError missingFieldValidationError(
            final @NotNull String detail,
            final @NotNull String path) {
        return MissingFieldValidationError.builder()
                .type(type(MissingFieldValidationError.class))
                .detail(detail)
                .path(path)
                .build();
    }

    public static @NotNull UnsupportedFieldValidationError unsupportedFieldValidationError(
            final @NotNull String detail,
            final @NotNull String path,
            final @NotNull String actualValue,
            final @NotNull String expectedValue) {
        return UnsupportedFieldValidationError.builder()
                .type(type(UnsupportedFieldValidationError.class))
                .detail(detail)
                .path(path)
                .actualValue(actualValue)
                .expectedValue(expectedValue)
                .build();
    }

    public static @NotNull UnsupportedFieldValidationError unsupportedFieldValidationErrorByType(
            final @NotNull String path,
            final @NotNull String actualType,
            final @NotNull String expectedType) {
        return unsupportedFieldValidationError("Unsupported type '" +
                actualType +
                " for field '" +
                path +
                "'. Expected type is '" +
                expectedType +
                "'.", path, actualType, expectedType);
    }

    public static @NotNull UnsupportedFieldValidationError unsupportedFieldValidationErrorByValue(
            final @NotNull String path,
            final @NotNull String actualValue,
            final @NotNull String expectedValue) {
        return unsupportedFieldValidationError("Unsupported value '" +
                actualValue +
                " for field '" +
                path +
                "'. Expected value is '" +
                expectedValue +
                "'.", path, actualValue, expectedValue);
    }
}
