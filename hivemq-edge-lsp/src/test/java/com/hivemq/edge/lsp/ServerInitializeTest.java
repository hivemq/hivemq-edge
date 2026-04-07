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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.ApplyWorkspaceEditParams;
import org.eclipse.lsp4j.ApplyWorkspaceEditResponse;
import org.eclipse.lsp4j.ConfigurationParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.RegistrationParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.UnregistrationParams;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests that {@link HiveMQEdgeLspServer#initialize(InitializeParams)} returns the correct
 * server capabilities.
 */
class ServerInitializeTest {

    @TempDir
    Path tempDir;

    @Test
    void initializeReturnsFullTextDocumentSync() throws Exception {
        final InitializeResult result = initialize(tempDir.toUri().toString());
        assertThat(result.getCapabilities().getTextDocumentSync().getRight().getChange())
                .isEqualTo(TextDocumentSyncKind.Full);
    }

    @Test
    void initializeHasSaveNotificationsEnabled() throws Exception {
        final InitializeResult result = initialize(tempDir.toUri().toString());
        assertThat(result.getCapabilities().getTextDocumentSync().getRight().getSave())
                .isNotNull();
    }

    @Test
    void initializeReturnsCompletionProviderWithTriggerCharacters() throws Exception {
        final InitializeResult result = initialize(tempDir.toUri().toString());
        final var completionOptions = result.getCapabilities().getCompletionProvider();
        assertThat(completionOptions).isNotNull();
        assertThat(completionOptions.getTriggerCharacters()).contains(":", " ");
    }

    @Test
    void initializeWorksWithoutRootUri() throws Exception {
        final InitializeResult result = initialize(null);
        assertThat(result).isNotNull();
        assertThat(result.getCapabilities()).isNotNull();
    }

    @Test
    void initializeWithRootPathFallback() throws Exception {
        final HiveMQEdgeLspServer server = new HiveMQEdgeLspServer();
        server.connect(noOpClient());

        final InitializeParams params = new InitializeParams();
        params.setRootUri(null);
        params.setRootPath(tempDir.toString());

        final InitializeResult result = server.initialize(params).get();
        assertThat(result.getCapabilities().getTextDocumentSync().getRight().getChange())
                .isEqualTo(TextDocumentSyncKind.Full);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private InitializeResult initialize(final String rootUri) throws Exception {
        final HiveMQEdgeLspServer server = new HiveMQEdgeLspServer();
        server.connect(noOpClient());

        final InitializeParams params = new InitializeParams();
        params.setRootUri(rootUri);

        return server.initialize(params).get();
    }

    private static LanguageClient noOpClient() {
        return new LanguageClient() {
            @Override
            public void publishDiagnostics(final PublishDiagnosticsParams diagnostics) {}

            @Override
            public void telemetryEvent(final Object object) {}

            @Override
            public void logMessage(final MessageParams message) {}

            @Override
            public void showMessage(final MessageParams messageParams) {}

            @Override
            public CompletableFuture<MessageActionItem> showMessageRequest(
                    final ShowMessageRequestParams requestParams) {
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<ApplyWorkspaceEditResponse> applyEdit(final ApplyWorkspaceEditParams params) {
                return CompletableFuture.completedFuture(new ApplyWorkspaceEditResponse(false));
            }

            @Override
            public CompletableFuture<Void> registerCapability(final RegistrationParams params) {
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<Void> unregisterCapability(final UnregistrationParams params) {
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<List<Object>> configuration(final ConfigurationParams configurationParams) {
                return CompletableFuture.completedFuture(List.of());
            }
        };
    }
}
