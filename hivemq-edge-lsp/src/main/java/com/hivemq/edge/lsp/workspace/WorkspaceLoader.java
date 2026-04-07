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

import com.hivemq.edge.compiler.source.discovery.YamlFileParser;
import com.hivemq.edge.compiler.source.model.SourceFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performs an initial scan of the workspace root, discovering and parsing all YAML files under
 * {@code instances/}, and populating a {@link WorkspaceIndex}.
 */
public class WorkspaceLoader {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceLoader.class);

    private final @NotNull YamlFileParser parser;

    public WorkspaceLoader(final @NotNull YamlFileParser parser) {
        this.parser = parser;
    }

    public WorkspaceLoader() {
        this(new YamlFileParser());
    }

    /**
     * Scans all YAML files under {@code workspaceRoot/instances/}, parses them, and rebuilds the index.
     */
    public void load(final @NotNull Path workspaceRoot, final @NotNull WorkspaceIndex index) {
        final Path instancesDir = workspaceRoot.resolve("instances");
        if (!Files.isDirectory(instancesDir)) {
            log.debug("No instances/ directory found at {}", workspaceRoot);
            index.rebuild(List.of());
            return;
        }

        final List<SourceFile> allFiles = new ArrayList<>();
        discoverAndParse(instancesDir, allFiles);
        log.info("Workspace scan: found {} YAML files under {}", allFiles.size(), instancesDir);
        index.rebuild(allFiles);
    }

    /**
     * Parses a single YAML file and returns it (or an empty placeholder on error).
     */
    public @NotNull SourceFile parseFile(final @NotNull Path path) {
        try {
            return parser.parseSourceFile(path);
        } catch (final IOException e) {
            log.warn("Failed to parse {}: {}", path, e.getMessage());
            final SourceFile placeholder = new SourceFile();
            placeholder.path = path;
            return placeholder;
        }
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private void discoverAndParse(final @NotNull Path dir, final @NotNull List<SourceFile> result) {
        try (final Stream<Path> walk = Files.walk(dir)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> {
                        final String name = p.getFileName().toString();
                        return name.endsWith(".yaml") || name.endsWith(".yml");
                    })
                    .forEach(p -> result.add(parseFile(p)));
        } catch (final IOException e) {
            log.error("Error scanning directory {}: {}", dir, e.getMessage());
        }
    }
}
