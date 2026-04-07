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
package com.hivemq.edge.lsp.workspace;

import com.hivemq.edge.lsp.diagnostics.DiagnosticsPublisher;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitors all directories under {@code instances/} using {@link WatchService}. On file change,
 * updates the {@link WorkspaceIndex} and triggers re-validation via {@link DiagnosticsPublisher}.
 *
 * <p>Runs on a dedicated daemon thread. Call {@link #start()} once after the workspace is loaded,
 * and {@link #stop()} on server exit.
 */
public class WorkspaceWatcher implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceWatcher.class);

    private final @NotNull Path workspaceRoot;
    private final @NotNull WorkspaceIndex index;
    private final @NotNull WorkspaceLoader loader;
    private final @NotNull DiagnosticsPublisher diagnosticsPublisher;

    private volatile boolean running = false;
    private WatchService watchService;

    public WorkspaceWatcher(
            final @NotNull Path workspaceRoot,
            final @NotNull WorkspaceIndex index,
            final @NotNull WorkspaceLoader loader,
            final @NotNull DiagnosticsPublisher diagnosticsPublisher) {
        this.workspaceRoot = workspaceRoot;
        this.index = index;
        this.loader = loader;
        this.diagnosticsPublisher = diagnosticsPublisher;
    }

    /** Starts the watcher on a daemon thread. */
    public void start() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            registerAll(workspaceRoot.resolve("instances"));
            running = true;
            final Thread thread = new Thread(this, "hivemq-edge-lsp-watcher");
            thread.setDaemon(true);
            thread.start();
            log.info("Workspace watcher started for {}", workspaceRoot);
        } catch (final IOException e) {
            log.error("Failed to start workspace watcher: {}", e.getMessage());
        }
    }

    public void stop() {
        running = false;
        if (watchService != null) {
            try {
                watchService.close();
            } catch (final IOException ignored) {
            }
        }
    }

    @Override
    public void run() {
        final Map<WatchKey, Path> keyToDir = new HashMap<>();

        // Re-register all directories (WatchService needs the directory-to-key map)
        try {
            registerAll(workspaceRoot.resolve("instances"), keyToDir);
        } catch (final IOException e) {
            log.error("Watcher registration failed: {}", e.getMessage());
            return;
        }

        while (running) {
            final WatchKey key;
            try {
                key = watchService.take();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (final Exception e) {
                if (!running) return;
                log.warn("WatchService error: {}", e.getMessage());
                continue;
            }

            final Path dir = keyToDir.get(key);
            if (dir == null) {
                key.cancel();
                continue;
            }

            for (final WatchEvent<?> event : key.pollEvents()) {
                final WatchEvent.Kind<?> kind = event.kind();
                if (kind == StandardWatchEventKinds.OVERFLOW) continue;

                @SuppressWarnings("unchecked")
                final Path filename = ((WatchEvent<Path>) event).context();
                final Path fullPath = dir.resolve(filename);

                if (kind == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(fullPath)) {
                    // New directory — register it too
                    try {
                        registerAll(fullPath, keyToDir);
                    } catch (final IOException e) {
                        log.warn("Failed to watch new directory {}: {}", fullPath, e.getMessage());
                    }
                }

                final String name = fullPath.getFileName().toString();
                if (!name.endsWith(".yaml") && !name.endsWith(".yml")) continue;

                if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                    log.debug("File deleted: {}", fullPath);
                    index.removeFile(fullPath);
                } else {
                    log.debug("File changed: {}", fullPath);
                    final var parsed = loader.parseFile(fullPath);
                    index.updateFile(parsed);
                }

                diagnosticsPublisher.republishForFile(fullPath);
            }

            if (!key.reset()) {
                keyToDir.remove(key);
            }
        }
    }

    private void registerAll(final @NotNull Path root) throws IOException {
        if (!Files.isDirectory(root)) return;
        try (final Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isDirectory).forEach(dir -> {
                try {
                    dir.register(
                            watchService,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_MODIFY,
                            StandardWatchEventKinds.ENTRY_DELETE);
                } catch (final IOException e) {
                    log.warn("Failed to register directory {}: {}", dir, e.getMessage());
                }
            });
        }
    }

    private void registerAll(final @NotNull Path root, final @NotNull Map<WatchKey, Path> keyToDir) throws IOException {
        if (!Files.isDirectory(root)) return;
        try (final Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isDirectory).forEach(dir -> {
                try {
                    final WatchKey key = dir.register(
                            watchService,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_MODIFY,
                            StandardWatchEventKinds.ENTRY_DELETE);
                    keyToDir.put(key, dir);
                } catch (final IOException e) {
                    log.warn("Failed to register directory {}: {}", dir, e.getMessage());
                }
            });
        }
    }
}
