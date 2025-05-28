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

public final class AtMostOneFunctionValidationError extends ValidationError<AtMostOneFunctionValidationError> {
    @JsonProperty("paths")
    private final @NotNull List<String> paths;
    @JsonProperty("function")
    private @NotNull String function;
    @JsonProperty("occurrences")
    private int occurrences;

    private AtMostOneFunctionValidationError(
            final @NotNull String detail,
            final @NotNull String function,
            final int occurrences,
            final @Nullable List<String> paths) {
        super(detail);
        setFunction(function);
        setOccurrences(occurrences);
        this.paths = new ArrayList<>();
        if (paths != null && !paths.isEmpty()) {
            this.paths.addAll(paths);
        }
    }

    public static @NotNull AtMostOneFunctionValidationError of(
            final @NotNull String function,
            final int occurrences,
            final @Nullable List<String> paths) {
        return new AtMostOneFunctionValidationError("The pipeline must contain at most one '" +
                function +
                "' function, but " +
                occurrences +
                " were found at " +
                paths +
                ".", function, occurrences, paths);
    }

    public static @NotNull AtMostOneFunctionValidationError of(
            final @NotNull String detail,
            final @NotNull String function,
            final int occurrences,
            final @Nullable List<String> paths) {
        return new AtMostOneFunctionValidationError(detail, function, occurrences, paths);
    }

    public int getOccurrences() {
        return occurrences;
    }

    public @NotNull AtMostOneFunctionValidationError setOccurrences(final int occurrences) {
        this.occurrences = occurrences;
        return this;
    }

    public @NotNull List<String> getPaths() {
        return paths;
    }

    public @NotNull String getFunction() {
        return function;
    }

    public @NotNull AtMostOneFunctionValidationError setFunction(@NotNull final String function) {
        this.function = Objects.requireNonNull(function);
        return this;
    }
}
