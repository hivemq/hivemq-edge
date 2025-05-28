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

public final class FunctionMustBePairedValidationError extends ValidationError<FunctionMustBePairedValidationError> {
    @JsonProperty("existingFunction")
    private @NotNull String existingFunction;

    @JsonProperty("missingFunction")
    private @NotNull String missingFunction;

    private FunctionMustBePairedValidationError(
            final @NotNull String detail,
            final @NotNull String existingFunction,
            final @NotNull String missingFunction) {
        super(detail);
        setExistingFunction(existingFunction);
        setMissingFunction(missingFunction);
    }

    public static @NotNull FunctionMustBePairedValidationError of(
            final @NotNull String existingFunction,
            final @NotNull String missingFunction) {
        return new FunctionMustBePairedValidationError("If '" +
                existingFunction +
                "' function is present in the pipeline, '" +
                missingFunction +
                "' function must be present as well.", existingFunction, missingFunction);
    }

    public static @NotNull FunctionMustBePairedValidationError of(
            final @NotNull String detail,
            final @NotNull String existingFunction,
            final @NotNull String missingFunction) {
        return new FunctionMustBePairedValidationError(detail, existingFunction, missingFunction);
    }

    public @NotNull String getMissingFunction() {
        return missingFunction;
    }

    public @NotNull FunctionMustBePairedValidationError setMissingFunction(@NotNull final String missingFunction) {
        this.missingFunction = Objects.requireNonNull(missingFunction);
        return this;
    }

    public @NotNull String getExistingFunction() {
        return existingFunction;
    }

    public @NotNull FunctionMustBePairedValidationError setExistingFunction(@NotNull final String existingFunction) {
        this.existingFunction = Objects.requireNonNull(existingFunction);
        return this;
    }
}
