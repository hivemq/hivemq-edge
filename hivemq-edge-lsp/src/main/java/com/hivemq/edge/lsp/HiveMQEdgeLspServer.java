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
package com.hivemq.edge.lsp;

import com.hivemq.edge.lsp.completion.CompletionProvider;
import com.hivemq.edge.lsp.diagnostics.DiagnosticsPublisher;
import com.hivemq.edge.lsp.workspace.WorkspaceIndex;
import com.hivemq.edge.lsp.workspace.WorkspaceLoader;
import com.hivemq.edge.lsp.workspace.WorkspaceWatcher;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.SaveOptions;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.TextDocumentSyncOptions;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Root language server implementation. Wires together the workspace model, completion providers,
 * and diagnostics publisher.
 */
public class HiveMQEdgeLspServer implements LanguageServer, LanguageClientAware {

    private static final Logger log = LoggerFactory.getLogger(HiveMQEdgeLspServer.class);

    private final @NotNull WorkspaceIndex index = new WorkspaceIndex();
    private final @NotNull WorkspaceLoader loader = new WorkspaceLoader();
    private final @NotNull DiagnosticsPublisher diagnosticsPublisher;
    private final @NotNull HiveMQEdgeTextDocumentService textDocumentService;
    private final @NotNull HiveMQEdgeWorkspaceService workspaceService;

    private @Nullable WorkspaceWatcher watcher;
    private @Nullable Path workspaceRoot;

    public HiveMQEdgeLspServer() {
        // workspaceRoot is not yet known; DiagnosticsPublisher needs it — set a placeholder
        // that gets replaced in initialize().  We use a late-init approach via a wrapper.
        this.diagnosticsPublisher = new LazyDiagnosticsPublisher(index);
        final CompletionProvider completionProvider = new CompletionProvider(index);
        this.textDocumentService =
                new HiveMQEdgeTextDocumentService(index, loader, diagnosticsPublisher, completionProvider);
        this.workspaceService = new HiveMQEdgeWorkspaceService(index, loader, diagnosticsPublisher);
    }

    // ── LanguageClientAware ───────────────────────────────────────────────────

    @Override
    public void connect(final @NotNull LanguageClient client) {
        ((LazyDiagnosticsPublisher) diagnosticsPublisher).connect(client);
        log.info("Language client connected");
    }

    // ── LanguageServer ────────────────────────────────────────────────────────

    @Override
    public @NotNull CompletableFuture<InitializeResult> initialize(final @NotNull InitializeParams params) {
        // Determine workspace root from rootUri or rootPath
        if (params.getRootUri() != null) {
            try {
                workspaceRoot = Path.of(new URI(params.getRootUri()));
            } catch (final Exception e) {
                log.warn("Could not parse rootUri {}: {}", params.getRootUri(), e.getMessage());
            }
        }
        if (workspaceRoot == null && params.getRootPath() != null) {
            workspaceRoot = Path.of(params.getRootPath());
        }

        if (workspaceRoot != null) {
            ((LazyDiagnosticsPublisher) diagnosticsPublisher).setWorkspaceRoot(workspaceRoot);
            log.info("Workspace root: {}", workspaceRoot);
        }

        final ServerCapabilities capabilities = new ServerCapabilities();
        final TextDocumentSyncOptions syncOptions = new TextDocumentSyncOptions();
        syncOptions.setChange(TextDocumentSyncKind.Full);
        syncOptions.setOpenClose(true);
        syncOptions.setSave(new SaveOptions(false)); // notify on save; content read from disk
        capabilities.setTextDocumentSync(syncOptions);
        final CompletionOptions completionOptions = new CompletionOptions();
        completionOptions.setTriggerCharacters(List.of(":", " "));
        capabilities.setCompletionProvider(completionOptions);

        return CompletableFuture.completedFuture(new InitializeResult(capabilities));
    }

    @Override
    public void initialized(final @NotNull InitializedParams params) {
        // Load workspace and start watcher after the client has acknowledged initialization
        if (workspaceRoot != null) {
            loader.load(workspaceRoot, index);
            watcher = new WorkspaceWatcher(workspaceRoot, index, loader, diagnosticsPublisher);
            watcher.start();
            log.info("Workspace loaded and watcher started");
        }
    }

    @Override
    public @NotNull CompletableFuture<Object> shutdown() {
        if (watcher != null) {
            watcher.stop();
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void exit() {
        System.exit(0);
    }

    @Override
    public @NotNull TextDocumentService getTextDocumentService() {
        return textDocumentService;
    }

    @Override
    public @NotNull WorkspaceService getWorkspaceService() {
        return workspaceService;
    }

    // ── Inner type: late-init wrapper ─────────────────────────────────────────

    /**
     * Wraps {@link DiagnosticsPublisher} with deferred initialization of the workspace root. The root
     * is not known until {@code initialize()} is called, but the publisher is wired during construction.
     */
    private static final class LazyDiagnosticsPublisher extends DiagnosticsPublisher {

        private volatile boolean rootSet = false;

        LazyDiagnosticsPublisher(final @NotNull WorkspaceIndex index) {
            // Provide a placeholder root; overridden before any validation runs
            super(index, Path.of("."));
        }

        void setWorkspaceRoot(final @NotNull Path root) {
            this.workspaceRoot = root;
            this.rootSet = true;
        }

        @Override
        public void scheduleForFile(final @NotNull Path file) {
            if (!rootSet) return;
            super.scheduleForFile(file);
        }

        @Override
        public void republishForFile(final @NotNull Path file) {
            if (!rootSet) return;
            super.republishForFile(file);
        }
    }
}
