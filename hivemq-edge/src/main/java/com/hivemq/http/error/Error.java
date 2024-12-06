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

public class Error {
    public static final @NotNull String REQUIRED_FIELD_MISSING_TITLE = "Required field missing";
    public static final @NotNull String AT_LEAST_ONE_FIELD_MISSING_TITLE = "One of the fields must be present.";
    public static final @NotNull String EMPTY_STRING_TITLE = "String must not be empty";

    @JsonProperty("detail")
    @Schema(description = "Detailed contextual description of this error")
    private final @Nullable String detail;

    @JsonProperty("parameter")
    @Schema(description = "The parameter causing the issue")
    private final @Nullable String parameter;

    @JsonProperty("note")
    @Schema(description = "Additional information")
    private final @Nullable String note;

    @JsonProperty("trace")
    @Schema(description = "Trace infomration for the error")
    private final @Nullable String trace;

    @JsonCreator
    public Error(
            final @JsonProperty("detail") @Nullable String detail,
            final @JsonProperty("parameter") @Nullable String parameter,
            final @JsonProperty("note") @Nullable String note,
            final @JsonProperty("trace") @Nullable String trace) {
        this.detail = detail;
        this.parameter = parameter;
        this.note = note;
        this.trace = trace;
    }

    public static @NotNull Error missingField(final @NotNull String field) {
        return new Error(String.format("Required field '%s' is missing", field), field, null, null);
    }

    public static @NotNull Error atLeastOneMissingField(final @NotNull String... fields) {
        final String fieldsConcat = stream(fields).map(field -> "'" + field + "'").collect(joining(", "));
        return new Error(AT_LEAST_ONE_FIELD_MISSING_TITLE, fieldsConcat, null, null);
    }

    public static @NotNull Error emptyString(final @NotNull String field) {
        return new Error(EMPTY_STRING_TITLE, field, null, null);
    }

    public static @NotNull Error unknownVariables(
            final @NotNull List<String> unknownVariables, final @NotNull String field) {

        return new Error(
                String.format("Field '%s' contains unknown variables: [%s].", field, String.join(", ", unknownVariables)),
                field,
                null,
                null);
    }

    public static @NotNull Error unsupportedType(
            final @NotNull JsonNodeType expectedType,
            final @NotNull JsonNodeType actualType,
            final @NotNull String field) {
        return unsupportedType(expectedType.name(), actualType.name(), field);
    }

    public static @NotNull Error unsupportedType(
            final @NotNull String expectedType, final @NotNull String actualType, final @NotNull String field) {
        return new Error(
                String.format("Unsupported type '%s'. Expected type was '%s'.",
                        actualType,
                        expectedType),
                field,
                null,
                null);
    }

    public static @NotNull Error unsupportedValue(
            final @NotNull List<String> supportedTypes,
            final @NotNull String actualValue,
            final @NotNull String field) {
        return new Error(
                String.format("Unsupported value '%s'. Supported values are %s.",
                        actualValue,
                        supportedTypes),
                field,
                null,
                null);
    }

    public static @NotNull Error illegalValue(
            final @NotNull String field, final @NotNull String value, final @NotNull String errorMessage) {
        return new Error(
                String.format("Illegal value '%s'. %s", value, errorMessage),
                field,
                null,
                null);
    }

    public static @NotNull Error illegalValue(
            final @NotNull String field, final @NotNull String errorMessage) {
        return new Error(
                String.format("Illegal value: %s", errorMessage),
                field,
                null,
                null);
    }

    public static @NotNull Error functionsMustBePaired(
            final @NotNull String presentFunction, final @NotNull String missingFunction) {
        return new Error(
                String.format("If '%s' function is present in the pipeline, '%s' function must be present as well.",
                        presentFunction,
                        missingFunction),
                "function",
                null,
                null);
    }

    public static @NotNull Error atMostOneFunction(
            final @NotNull String function, final @NotNull List<String> fields) {
        return new Error(
                String.format("The pipeline must contain at most one '%s' function, but %d were found at %s.",
                        function,
                        fields.size(),
                        fields),
                "function",
                null,
                null);
    }

    public static @NotNull Error functionMustBeAfter(
            final @NotNull String falseField,
            final @NotNull String falseFieldFunctionId,
            final @NotNull String mustAfterField) {
        return new Error(
                String.format("The operation at '%s' with the functionId '%s' must be after a '%s' operation.",
                        falseField,
                        falseFieldFunctionId,
                        mustAfterField),
                "function",
                null,

                null);
    }

    public static @NotNull Error functionMustBeBefore(
            final @NotNull String falseField,
            final @NotNull String falseFieldFunctionId,
            final @NotNull String mustBeforeField) {
        return new Error(
                String.format("The operation at '%s' with the functionId '%s' must be before a '%s' operation.",
                        falseField,
                        falseFieldFunctionId,
                        mustBeforeField),
                "function",
                null,
                null);
    }

    public @Nullable String getDetail() {
        return detail;
    }

    public @Nullable String getParameter() {
        return parameter;
    }

    public @Nullable String getNote() {
        return note;
    }

    public @Nullable String getTrace() {
        return trace;
    }

    @Override
    public String toString() {
        return "Error{" +
                "detail='" +
                detail +
                '\'' +
                ", parameter='" +
                parameter +
                '\'' +
                ", note='" +
                note +
                '\'' +
                ", trace='" +
                trace +
                '\'' +
                '}';
    }
}
