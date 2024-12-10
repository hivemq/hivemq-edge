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
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

public class ErrorWithParameter extends Error{
    public static final @NotNull String REQUIRED_FIELD_MISSING_TITLE = "Required field missing";
    public static final @NotNull String AT_LEAST_ONE_FIELD_MISSING_TITLE = "One of the fields must be present.";
    public static final @NotNull String EMPTY_STRING_TITLE = "String must not be empty";

    @JsonProperty("parameter")
    @Schema(description = "The parameter causing the issue")
    private final @Nullable String parameter;

    @JsonCreator
    public ErrorWithParameter(
            final @JsonProperty("detail") @NotNull String detail,
            final @JsonProperty("parameter") @Nullable String parameter,
            final @JsonProperty("note") @Nullable String note,
            final @JsonProperty("trace") @Nullable String trace) {
        super(detail, note, trace);
        this.parameter = parameter;
    }

    public static @NotNull ErrorWithParameter missingField(final @NotNull String field) {
        return new ErrorWithParameter(String.format("Required field '%s' is missing", field), field, null, null);
    }

    public static @NotNull ErrorWithParameter atLeastOneMissingField(final @NotNull String... fields) {
        final String fieldsConcat = stream(fields).map(field -> "'" + field + "'").collect(joining(", "));
        return new ErrorWithParameter(AT_LEAST_ONE_FIELD_MISSING_TITLE, fieldsConcat, null, null);
    }

    public static @NotNull ErrorWithParameter emptyString(final @NotNull String field) {
        return new ErrorWithParameter(EMPTY_STRING_TITLE, field, null, null);
    }

    public static @NotNull ErrorWithParameter unknownVariables(
            final @NotNull List<String> unknownVariables, final @NotNull String field) {

        return new ErrorWithParameter(
                String.format("Field '%s' contains unknown variables: [%s].", field, String.join(", ", unknownVariables)),
                field,
                null,
                null);
    }

    public static @NotNull ErrorWithParameter unsupportedType(
            final @NotNull JsonNodeType expectedType,
            final @NotNull JsonNodeType actualType,
            final @NotNull String field) {
        return unsupportedType(expectedType.name(), actualType.name(), field);
    }

    public static @NotNull ErrorWithParameter unsupportedType(
            final @NotNull String expectedType, final @NotNull String actualType, final @NotNull String field) {
        return new ErrorWithParameter(
                String.format("Unsupported type '%s'. Expected type was '%s'.",
                        actualType,
                        expectedType),
                field,
                null,
                null);
    }

    public static @NotNull ErrorWithParameter unsupportedValue(
            final @NotNull List<String> supportedTypes,
            final @NotNull String actualValue,
            final @NotNull String field) {
        return new ErrorWithParameter(
                String.format("Unsupported value '%s'. Supported values are %s.",
                        actualValue,
                        supportedTypes),
                field,
                null,
                null);
    }

    public static @NotNull ErrorWithParameter illegalValue(
            final @NotNull String field, final @NotNull String value, final @NotNull String errorMessage) {
        return new ErrorWithParameter(
                String.format("Illegal value '%s'. %s", value, errorMessage),
                field,
                null,
                null);
    }

    public static @NotNull ErrorWithParameter illegalValue(
            final @NotNull String field, final @NotNull String errorMessage) {
        return new ErrorWithParameter(
                String.format("Illegal value: %s", errorMessage),
                field,
                null,
                null);
    }

    public static @NotNull ErrorWithParameter functionsMustBePaired(
            final @NotNull String presentFunction, final @NotNull String missingFunction) {
        return new ErrorWithParameter(
                String.format("If '%s' function is present in the pipeline, '%s' function must be present as well.",
                        presentFunction,
                        missingFunction),
                "function",
                null,
                null);
    }

    public static @NotNull ErrorWithParameter atMostOneFunction(
            final @NotNull String function, final @NotNull List<String> fields) {
        return new ErrorWithParameter(
                String.format("The pipeline must contain at most one '%s' function, but %d were found at %s.",
                        function,
                        fields.size(),
                        fields),
                "function",
                null,
                null);
    }

    public static @NotNull ErrorWithParameter functionMustBeAfter(
            final @NotNull String falseField,
            final @NotNull String falseFieldFunctionId,
            final @NotNull String mustAfterField) {
        return new ErrorWithParameter(
                String.format("The operation at '%s' with the functionId '%s' must be after a '%s' operation.",
                        falseField,
                        falseFieldFunctionId,
                        mustAfterField),
                "function",
                null,

                null);
    }

    public static @NotNull ErrorWithParameter functionMustBeBefore(
            final @NotNull String falseField,
            final @NotNull String falseFieldFunctionId,
            final @NotNull String mustBeforeField) {
        return new ErrorWithParameter(
                String.format("The operation at '%s' with the functionId '%s' must be before a '%s' operation.",
                        falseField,
                        falseFieldFunctionId,
                        mustBeforeField),
                "function",
                null,
                null);
    }

    public @Nullable String getParameter() {
        return parameter;
    }

    @Override
    public String toString() {
        return "Error{" +
                "detail='" +
                getDetail() +
                '\'' +
                ", parameter='" +
                parameter +
                '\'' +
                ", note='" +
                getParameter() +
                '\'' +
                ", trace='" +
                getTrace() +
                '\'' +
                '}';
    }
}
