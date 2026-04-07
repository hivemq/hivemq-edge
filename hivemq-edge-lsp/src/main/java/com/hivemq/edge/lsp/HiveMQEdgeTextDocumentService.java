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
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles text document lifecycle events and completion requests.
 *
 * <p>We request {@code Full} text document sync, so every {@code didChange} event carries the
 * complete new document content. In-memory document state is maintained for completion.
 */
public class HiveMQEdgeTextDocumentService implements TextDocumentService {

    private static final Logger log = LoggerFactory.getLogger(HiveMQEdgeTextDocumentService.class);

    private final @NotNull WorkspaceIndex index;
    private final @NotNull WorkspaceLoader loader;
    private final @NotNull DiagnosticsPublisher diagnosticsPublisher;
    private final @NotNull CompletionProvider completionProvider;

    /** URI → current in-editor content. */
    private final Map<String, String> openDocuments = new ConcurrentHashMap<>();

    public HiveMQEdgeTextDocumentService(
            final @NotNull WorkspaceIndex index,
            final @NotNull WorkspaceLoader loader,
            final @NotNull DiagnosticsPublisher diagnosticsPublisher,
            final @NotNull CompletionProvider completionProvider) {
        this.index = index;
        this.loader = loader;
        this.diagnosticsPublisher = diagnosticsPublisher;
        this.completionProvider = completionProvider;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void didOpen(final @NotNull DidOpenTextDocumentParams params) {
        final String uri = params.getTextDocument().getUri();
        final String text = params.getTextDocument().getText();
        openDocuments.put(uri, text);
        log.debug("Opened: {}", uri);

        updateIndexFromContent(uri, text);
        diagnosticsPublisher.scheduleForFile(toPath(uri));
    }

    @Override
    public void didChange(final @NotNull DidChangeTextDocumentParams params) {
        final String uri = params.getTextDocument().getUri();
        if (params.getContentChanges().isEmpty()) return;

        // Full sync — the single change event carries the entire new content
        final String text = params.getContentChanges().get(0).getText();
        openDocuments.put(uri, text);

        updateIndexFromContent(uri, text);
        diagnosticsPublisher.scheduleForFile(toPath(uri));
    }

    @Override
    public void didSave(final @NotNull DidSaveTextDocumentParams params) {
        final String uri = params.getTextDocument().getUri();
        log.debug("Saved: {}", uri);
        diagnosticsPublisher.scheduleForFile(toPath(uri));
    }

    @Override
    public void didClose(final @NotNull DidCloseTextDocumentParams params) {
        openDocuments.remove(params.getTextDocument().getUri());
    }

    // ── Completion ────────────────────────────────────────────────────────────

    @Override
    public @NotNull CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(
            final @NotNull CompletionParams params) {

        final String uri = params.getTextDocument().getUri();
        final String text = openDocuments.getOrDefault(uri, "");
        final int line = params.getPosition().getLine();
        final int character = params.getPosition().getCharacter();
        final Path filePath = toPath(uri);

        final List<CompletionItem> items = completionProvider.complete(text, line, character, filePath);
        return CompletableFuture.completedFuture(Either.forLeft(items));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void updateIndexFromContent(final @NotNull String uri, final @NotNull String text) {
        try {
            final Path path = toPath(uri);
            // Parse the in-memory content via a temp approach: write to a temp file, parse, delete
            // For simplicity in POC, only update the file-system file when it's actually saved.
            // The index is updated from disk on save / workspace events.
            // On open/change we keep the editor version in openDocuments for completion purposes.
            // (The compiler validation still runs from disk content, which may lag by one save cycle.)
        } catch (final Exception e) {
            log.warn("Failed to update index from content for {}: {}", uri, e.getMessage());
        }
    }

    private static @NotNull Path toPath(final @NotNull String uri) {
        try {
            return Path.of(new URI(uri));
        } catch (final Exception e) {
            // Fallback: strip file:// prefix if URI parsing fails
            return Path.of(uri.replaceFirst("^file://", ""));
        }
    }
}
