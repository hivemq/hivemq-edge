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
package com.hivemq.edge.compiler.source.validation;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.nio.file.Path;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A single validation diagnostic in LSP Diagnostic format.
 *
 * <pre>{@code
 * {
 *   "severity": 1,
 *   "message": "Tag 'NozzlePressure' referenced in northbound mapping does not exist",
 *   "source": "edge-compiler",
 *   "code": "UNRESOLVED_TAG_REFERENCE",
 *   "data": { "adapterId": "extruder-01", "tagName": "NozzlePressure" }
 * }
 * }</pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Diagnostic(
        int severity,
        @NotNull String message,
        @NotNull String source,
        @NotNull String code,
        @Nullable Path file,
        @Nullable Map<String, Object> data) {

    /** LSP severity: Error */
    public static final int SEVERITY_ERROR = 1;

    /** LSP severity: Warning */
    public static final int SEVERITY_WARNING = 2;

    private static final String SOURCE = "edge-compiler";

    public static @NotNull Diagnostic error(
            final @NotNull String code,
            final @NotNull String message,
            final @Nullable Path file,
            final @Nullable Map<String, Object> data) {
        return new Diagnostic(SEVERITY_ERROR, message, SOURCE, code, file, data);
    }

    public static @NotNull Diagnostic error(
            final @NotNull String code, final @NotNull String message, final @Nullable Path file) {
        return error(code, message, file, null);
    }

    public static @NotNull Diagnostic error(final @NotNull String code, final @NotNull String message) {
        return error(code, message, null, null);
    }

    public boolean isError() {
        return severity == SEVERITY_ERROR;
    }
}
