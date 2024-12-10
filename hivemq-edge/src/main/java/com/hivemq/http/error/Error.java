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

    @JsonProperty("detail")
    @Schema(description = "Detailed contextual description of this error")
    private final @NotNull String detail;

    @JsonProperty("note")
    @Schema(description = "Additional information")
    private final @Nullable String note;

    @JsonProperty("trace")
    @Schema(description = "Trace infomration for the error")
    private final @Nullable String trace;

    @JsonCreator
    public Error(
            final @JsonProperty("detail") @NotNull String detail,
            final @JsonProperty("note") @Nullable String note,
            final @JsonProperty("trace") @Nullable String trace) {
        this.detail = detail;
        this.note = note;
        this.trace = trace;
    }

    public Error(
            final @JsonProperty("detail") @NotNull String detail) {
        this.detail = detail;
        this.note = null;
        this.trace = null;
    }

    public static @NotNull Error functionsMustBePaired(
            final @NotNull String presentFunction, final @NotNull String missingFunction) {
        return new Error(
                String.format("If '%s' function is present in the pipeline, '%s' function must be present as well.",
                        presentFunction,
                        missingFunction),
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
                null,
                null);
    }

    public @Nullable String getDetail() {
        return detail;
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
                ", note='" +
                note +
                '\'' +
                ", trace='" +
                trace +
                '\'' +
                '}';
    }
}
