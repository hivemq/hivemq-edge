/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.http.error;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.google.common.base.MoreObjects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Objects;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

public class Error {
    public static final @NotNull String REQUIRED_FIELD_MISSING_TITLE = "Required field missing";
    public static final @NotNull String EMPTY_STRING_TITLE = "String must not be empty";
    public static final @NotNull String UNSUPPORTED_TYPE_TITLE = "Unsupported type";
    public static final @NotNull String UNSUPPORTED_VALUE_TITLE = "Unsupported value";
    public static final @NotNull String ILLEGAL_VALUE_TITLE = "Illegal value";
    public static final @NotNull String UNKNOWN_VARIABLES_TITLE = "Unknown variables";
    public static final @NotNull String ILLEGAL_COMBINATION_TITLE = "Illegal combination";

    @JsonProperty("title")
    @Schema(description = "The type of this error")
    private final @NotNull String title;

    @JsonProperty("detail")
    @Schema(description = "Detailed contextual description of this error")
    private final @Nullable String detail;

    @JsonCreator
    public Error(
            final @JsonProperty("title") @NotNull String title, final @JsonProperty("detail") @Nullable String detail) {
        this.title = title;
        this.detail = detail;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Error error = (Error) o;
        return Objects.equals(title, error.title) && Objects.equals(detail, error.detail);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, detail);
    }

    @Override
    public @NotNull String toString() {
        return MoreObjects.toStringHelper("Error").add("title", title).add("detail", detail).toString();
    }

    public static @NotNull Error missingField(final @NotNull String field) {
        return new Error(REQUIRED_FIELD_MISSING_TITLE, "Required field '" + field + "' is missing.");
    }

    public static @NotNull Error atLeastOneMissingField(final @NotNull String... fields) {
        final String fieldsConcat = stream(fields).map(field -> "'" + field + "'").collect(joining(", "));
        return new Error(REQUIRED_FIELD_MISSING_TITLE,
                "At least one of the following fields is required: " + fieldsConcat + ".");
    }

    public static @NotNull Error emptyString(final @NotNull String field) {
        return new Error(EMPTY_STRING_TITLE, String.format("String value of field '%s' must not be empty.", field));
    }

    public static @NotNull Error unknownVariables(
            final @NotNull List<String> unknownVariables, final @NotNull String field) {
        return new Error(UNKNOWN_VARIABLES_TITLE,
                String.format("Field '%s' contains unknown variables: [%s].",
                        field,
                        String.join(", ", unknownVariables)));
    }

    public static @NotNull Error unsupportedType(
            final @NotNull JsonNodeType expectedType,
            final @NotNull JsonNodeType actualType,
            final @NotNull String field) {
        return unsupportedType(expectedType.name(), actualType.name(), field);
    }

    public static @NotNull Error unsupportedType(
            final @NotNull String expectedType, final @NotNull String actualType, final @NotNull String field) {
        return new Error(UNSUPPORTED_TYPE_TITLE,
                String.format("Field '%s' has unsupported type '%s'. Expected type was '%s'.",
                        field,
                        actualType,
                        expectedType));
    }

    public static @NotNull Error unsupportedValue(
            final @NotNull List<String> supportedTypes,
            final @NotNull String actualValue,
            final @NotNull String field) {
        return new Error(UNSUPPORTED_VALUE_TITLE,
                String.format("Field '%s' has unsupported value '%s'. Supported values are %s.",
                        field,
                        actualValue,
                        supportedTypes));
    }

    public static @NotNull Error illegalValue(
            final @NotNull String field, final @NotNull String value, final @NotNull String errorMessage) {
        return new Error(ILLEGAL_VALUE_TITLE,
                String.format("Field '%s' has illegal value '%s'. %s", field, value, errorMessage));
    }

    public static @NotNull Error illegalValue(
            final @NotNull String field, final @NotNull String errorMessage) {
        return new Error(ILLEGAL_VALUE_TITLE, String.format("Field '%s' has illegal value. %s", field, errorMessage));
    }

    public static @NotNull Error functionsMustBePaired(
            final @NotNull String presentFunction, final @NotNull String missingFunction) {
        return new Error(ILLEGAL_COMBINATION_TITLE,
                String.format("If '%s' function is present in the pipeline, '%s' function must be present as well.",
                        presentFunction,
                        missingFunction));
    }

    public static @NotNull Error atMostOneFunction(
            final @NotNull String function, final @NotNull List<String> fields) {
        return new Error(ILLEGAL_COMBINATION_TITLE,
                String.format("The pipeline must contain at most one '%s' function, but %d were found at %s.",
                        function,
                        fields.size(),
                        fields));
    }

    public static @NotNull Error functionMustBeAfter(
            final @NotNull String falseField,
            final @NotNull String falseFieldFunctionId,
            final @NotNull String mustAfterField) {
        return new Error(ILLEGAL_COMBINATION_TITLE,
                String.format("The operation at '%s' with the functionId '%s' must be after a '%s' operation.",
                        falseField,
                        falseFieldFunctionId,
                        mustAfterField));
    }

    public static @NotNull Error functionMustBeBefore(
            final @NotNull String falseField,
            final @NotNull String falseFieldFunctionId,
            final @NotNull String mustBeforeField) {
        return new Error(ILLEGAL_COMBINATION_TITLE,
                String.format("The operation at '%s' with the functionId '%s' must be before a '%s' operation.",
                        falseField,
                        falseFieldFunctionId,
                        mustBeforeField));
    }

    public @NotNull String getTitle() {
        return title;
    }

    public @Nullable String getDetail() {
        return detail;
    }
}
