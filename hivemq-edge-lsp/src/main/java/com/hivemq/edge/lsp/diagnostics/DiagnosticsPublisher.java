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
package com.hivemq.edge.lsp.diagnostics;

import com.hivemq.edge.compiler.EdgeCompiler;
import com.hivemq.edge.compiler.source.validation.Diagnostic;
import com.hivemq.edge.lsp.workspace.WorkspaceIndex;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.services.LanguageClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs the compiler's validation pipeline and publishes LSP diagnostics to the language client.
 *
 * <p>Diagnostics are debounced: a short delay (300 ms) is applied after each request so that rapid
 * edits (e.g. while typing) don't trigger a full re-validation on every keystroke.
 */
public class DiagnosticsPublisher {

    private static final Logger log = LoggerFactory.getLogger(DiagnosticsPublisher.class);
    private static final long DEBOUNCE_MS = 300;

    private final @NotNull EdgeCompiler compiler;
    private final @NotNull WorkspaceIndex index;
    protected volatile @NotNull Path workspaceRoot;

    private volatile @Nullable LanguageClient client;

    /** Files for which diagnostics were last published — needed to send an empty list on clear. */
    private final Set<String> lastReportedUris = new HashSet<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        final Thread t = new Thread(r, "hivemq-edge-lsp-diagnostics");
        t.setDaemon(true);
        return t;
    });

    private @Nullable ScheduledFuture<?> pendingValidation;

    public DiagnosticsPublisher(
            final @NotNull EdgeCompiler compiler,
            final @NotNull WorkspaceIndex index,
            final @NotNull Path workspaceRoot) {
        this.compiler = compiler;
        this.index = index;
        this.workspaceRoot = workspaceRoot;
    }

    public DiagnosticsPublisher(final @NotNull WorkspaceIndex index, final @NotNull Path workspaceRoot) {
        this(new EdgeCompiler(), index, workspaceRoot);
    }

    public void connect(final @NotNull LanguageClient languageClient) {
        this.client = languageClient;
    }

    /**
     * Schedules a debounced re-validation for the instance that contains {@code file}.
     */
    public void scheduleForFile(final @NotNull Path file) {
        schedule(() -> validateForFile(file));
    }

    /**
     * Immediately re-validates the instance that contains {@code file} (used by the workspace watcher).
     */
    public void republishForFile(final @NotNull Path file) {
        scheduler.submit(() -> validateForFile(file));
    }

    private void schedule(final @NotNull Runnable task) {
        synchronized (this) {
            if (pendingValidation != null) {
                pendingValidation.cancel(false);
            }
            pendingValidation = scheduler.schedule(task, DEBOUNCE_MS, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Validates the instance containing {@code file} and publishes diagnostics synchronously.
     * Intended for use in tests.
     */
    void validateNow(final @NotNull Path file) {
        validateForFile(file);
    }

    private void validateForFile(final @NotNull Path file) {
        final LanguageClient lc = this.client;
        if (lc == null) return;

        // Find the instance ID for this file (the directory directly under instances/)
        final String instanceId = findInstanceId(file);
        if (instanceId == null) {
            log.debug("File {} is not under instances/ — skipping validation", file);
            return;
        }

        try {
            final EdgeCompiler.Result result = compiler.compile(workspaceRoot, instanceId);
            publishDiagnostics(lc, result.diagnostics().all());
        } catch (final IOException e) {
            log.error("Compiler error for instance {}: {}", instanceId, e.getMessage());
        }
    }

    private synchronized void publishDiagnostics(
            final @NotNull LanguageClient lc, final @NotNull List<Diagnostic> diagnostics) {

        // Group diagnostics by file URI
        final Map<String, List<org.eclipse.lsp4j.Diagnostic>> byUri = new HashMap<>();
        for (final Diagnostic d : diagnostics) {
            if (d.file() == null) continue;
            final String uri = d.file().toUri().toString();
            byUri.computeIfAbsent(uri, k -> new ArrayList<>()).add(toLsp(d));
        }

        // Publish diagnostics for files that have errors/warnings
        for (final Map.Entry<String, List<org.eclipse.lsp4j.Diagnostic>> entry : byUri.entrySet()) {
            lc.publishDiagnostics(new PublishDiagnosticsParams(entry.getKey(), entry.getValue()));
            lastReportedUris.add(entry.getKey());
        }

        // Clear diagnostics for files that are now clean
        final Set<String> nowClean = new HashSet<>(lastReportedUris);
        nowClean.removeAll(byUri.keySet());
        for (final String uri : nowClean) {
            lc.publishDiagnostics(new PublishDiagnosticsParams(uri, List.of()));
            lastReportedUris.remove(uri);
        }
    }

    // ── Conversion ────────────────────────────────────────────────────────────

    private static @NotNull org.eclipse.lsp4j.Diagnostic toLsp(final @NotNull Diagnostic d) {
        final Range range = toRange(d.range());
        final org.eclipse.lsp4j.Diagnostic lsp = new org.eclipse.lsp4j.Diagnostic(range, d.message());
        lsp.setSeverity(toSeverity(d.severity()));
        lsp.setSource("edge-compiler");
        lsp.setCode(d.code());
        return lsp;
    }

    private static @NotNull Range toRange(final @Nullable Diagnostic.DiagnosticRange range) {
        if (range == null) {
            return new Range(new Position(0, 0), new Position(0, 0));
        }
        final Position start = new Position(range.start().line(), range.start().character());
        final Position end = range.end() != null
                ? new Position(range.end().line(), range.end().character())
                : start;
        return new Range(start, end);
    }

    private static @NotNull DiagnosticSeverity toSeverity(final int severity) {
        return switch (severity) {
            case Diagnostic.SEVERITY_ERROR -> DiagnosticSeverity.Error;
            case Diagnostic.SEVERITY_WARNING -> DiagnosticSeverity.Warning;
            default -> DiagnosticSeverity.Information;
        };
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @Nullable
    private static String findInstanceId(final @NotNull Path file) {
        Path p = file.toAbsolutePath().normalize();
        while (p.getParent() != null) {
            final Path parent = p.getParent();
            if (parent.getFileName() != null
                    && "instances".equals(parent.getFileName().toString())) {
                return p.getFileName().toString();
            }
            p = parent;
        }
        return null;
    }
}
