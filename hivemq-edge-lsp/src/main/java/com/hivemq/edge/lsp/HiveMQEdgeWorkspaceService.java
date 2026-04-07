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

import com.hivemq.edge.lsp.diagnostics.DiagnosticsPublisher;
import com.hivemq.edge.lsp.workspace.WorkspaceIndex;
import com.hivemq.edge.lsp.workspace.WorkspaceLoader;
import java.net.URI;
import java.nio.file.Path;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.FileChangeType;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles {@code workspace/didChangeWatchedFiles} — notified by the editor when YAML files change
 * on disk (including files not currently open in the editor).
 */
public class HiveMQEdgeWorkspaceService implements WorkspaceService {

    private static final Logger log = LoggerFactory.getLogger(HiveMQEdgeWorkspaceService.class);

    private final @NotNull WorkspaceIndex index;
    private final @NotNull WorkspaceLoader loader;
    private final @NotNull DiagnosticsPublisher diagnosticsPublisher;

    public HiveMQEdgeWorkspaceService(
            final @NotNull WorkspaceIndex index,
            final @NotNull WorkspaceLoader loader,
            final @NotNull DiagnosticsPublisher diagnosticsPublisher) {
        this.index = index;
        this.loader = loader;
        this.diagnosticsPublisher = diagnosticsPublisher;
    }

    @Override
    public void didChangeWatchedFiles(final @NotNull DidChangeWatchedFilesParams params) {
        for (final var event : params.getChanges()) {
            try {
                final Path filePath = Path.of(new URI(event.getUri()));
                if (event.getType() == FileChangeType.Deleted) {
                    log.debug("Editor reported file deleted: {}", filePath);
                    index.removeFile(filePath);
                } else {
                    log.debug("Editor reported file changed: {}", filePath);
                    index.updateFile(loader.parseFile(filePath));
                }
                diagnosticsPublisher.republishForFile(filePath);
            } catch (final Exception e) {
                log.warn("Failed to process file change event for {}: {}", event.getUri(), e.getMessage());
            }
        }
    }

    @Override
    public void didChangeConfiguration(final @NotNull DidChangeConfigurationParams params) {
        // Not used in this POC
    }
}
