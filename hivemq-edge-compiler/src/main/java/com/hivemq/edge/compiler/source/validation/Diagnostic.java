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
 * <p>Machine-readable JSON output (stdout):
 *
 * <pre>{@code
 * {
 *   "severity": 1,
 *   "message": "Tag 'NozzlePressure' referenced in northbound mapping does not exist",
 *   "source": "edge-compiler",
 *   "code": "UNRESOLVED_TAG_REFERENCE",
 *   "file": "/abs/path/to/nozzle-pressure.yaml",
 *   "range": { "start": { "line": 5, "character": 2 } },
 *   "data": { "adapterId": "extruder-01", "tagName": "NozzlePressure" }
 * }
 * }</pre>
 *
 * <p>Line and character numbers are 0-based (LSP convention).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Diagnostic(
        int severity,
        @NotNull String message,
        @NotNull String source,
        @NotNull String code,
        @Nullable Path file,
        @Nullable DiagnosticRange range,
        @Nullable Map<String, Object> data) {

    /** LSP severity: Error */
    public static final int SEVERITY_ERROR = 1;

    /** LSP severity: Warning */
    public static final int SEVERITY_WARNING = 2;

    private static final String SOURCE = "edge-compiler";

    // ── Inner types ───────────────────────────────────────────────────────────

    /** LSP Position — line and character are 0-based. */
    public record DiagnosticPosition(int line, int character) {}

    /**
     * LSP Range — {@code start} is required; {@code end} is optional (omitted when only a point is known).
     *
     * <p>Use {@link #of(int, int)} to create a point range from source model position fields.
     * If the source model has no position ({@code line == -1}), use {@link #ofNullable(int, int)} which returns null.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DiagnosticRange(
            @NotNull DiagnosticPosition start, @Nullable DiagnosticPosition end) {

        /** Creates a point range (no end position). */
        public static @NotNull DiagnosticRange of(final int line, final int character) {
            return new DiagnosticRange(new DiagnosticPosition(line, character), null);
        }

        /**
         * Creates a point range if {@code line >= 0}, or returns {@code null} if the position is unknown
         * ({@code line == -1}).
         */
        public static @Nullable DiagnosticRange ofNullable(final int line, final int character) {
            return line >= 0 ? of(line, character) : null;
        }
    }

    // ── Error factories ───────────────────────────────────────────────────────

    public static @NotNull Diagnostic error(final @NotNull String code, final @NotNull String message) {
        return new Diagnostic(SEVERITY_ERROR, message, SOURCE, code, null, null, null);
    }

    public static @NotNull Diagnostic error(
            final @NotNull String code, final @NotNull String message, final @Nullable Path file) {
        return new Diagnostic(SEVERITY_ERROR, message, SOURCE, code, file, null, null);
    }

    public static @NotNull Diagnostic error(
            final @NotNull String code,
            final @NotNull String message,
            final @Nullable Path file,
            final @Nullable Map<String, Object> data) {
        return new Diagnostic(SEVERITY_ERROR, message, SOURCE, code, file, null, data);
    }

    public static @NotNull Diagnostic error(
            final @NotNull String code,
            final @NotNull String message,
            final @Nullable Path file,
            final @Nullable DiagnosticRange range) {
        return new Diagnostic(SEVERITY_ERROR, message, SOURCE, code, file, range, null);
    }

    public static @NotNull Diagnostic error(
            final @NotNull String code,
            final @NotNull String message,
            final @Nullable Path file,
            final @Nullable DiagnosticRange range,
            final @Nullable Map<String, Object> data) {
        return new Diagnostic(SEVERITY_ERROR, message, SOURCE, code, file, range, data);
    }

    // ── Warning factories ─────────────────────────────────────────────────────

    public static @NotNull Diagnostic warning(
            final @NotNull String code,
            final @NotNull String message,
            final @Nullable Path file,
            final @Nullable DiagnosticRange range) {
        return new Diagnostic(SEVERITY_WARNING, message, SOURCE, code, file, range, null);
    }

    public static @NotNull Diagnostic warning(
            final @NotNull String code,
            final @NotNull String message,
            final @Nullable Path file,
            final @Nullable DiagnosticRange range,
            final @Nullable Map<String, Object> data) {
        return new Diagnostic(SEVERITY_WARNING, message, SOURCE, code, file, range, data);
    }

    // ── Predicates ────────────────────────────────────────────────────────────

    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isError() {
        return severity == SEVERITY_ERROR;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isWarning() {
        return severity == SEVERITY_WARNING;
    }
}
