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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/** Accumulates {@link Diagnostic} instances during compilation. All errors are collected before failing. */
public class DiagnosticCollector {

    private final @NotNull List<Diagnostic> diagnostics = new ArrayList<>();

    public void add(final @NotNull Diagnostic diagnostic) {
        diagnostics.add(diagnostic);
    }

    public @NotNull List<Diagnostic> all() {
        return Collections.unmodifiableList(diagnostics);
    }

    public @NotNull List<Diagnostic> errors() {
        return diagnostics.stream().filter(Diagnostic::isError).toList();
    }

    public boolean hasErrors() {
        return diagnostics.stream().anyMatch(Diagnostic::isError);
    }

    public int errorCount() {
        return (int) diagnostics.stream().filter(Diagnostic::isError).count();
    }
}
