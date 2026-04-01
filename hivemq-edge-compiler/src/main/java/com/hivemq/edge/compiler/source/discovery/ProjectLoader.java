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
package com.hivemq.edge.compiler.source.discovery;

import com.hivemq.edge.compiler.source.model.EdgeProjectDescriptor;
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
 * Reads {@code edge-project.yaml} and discovers all compilable YAML files in the declared source directories.
 *
 * <p>Files excluded from discovery:
 * <ul>
 *   <li>The declared output directory (default: {@code build/})
 *   <li>{@code edge-project.yaml} itself
 *   <li>{@code .csv} files (catalogues — not compiled)
 * </ul>
 */
public class ProjectLoader {

    private static final Logger log = LoggerFactory.getLogger(ProjectLoader.class);
    private static final String PROJECT_FILE = "edge-project.yaml";

    private final @NotNull YamlFileParser parser;

    public ProjectLoader(final @NotNull YamlFileParser parser) {
        this.parser = parser;
    }

    public ProjectLoader() {
        this(new YamlFileParser());
    }

    /**
     * Loads the project from the given root directory.
     *
     * @param projectRoot directory containing {@code edge-project.yaml}
     * @return a {@link LoadedProject} with the descriptor and all discovered source files
     */
    public @NotNull LoadedProject load(final @NotNull Path projectRoot) throws IOException {
        final Path descriptorPath = projectRoot.resolve(PROJECT_FILE);
        final EdgeProjectDescriptor descriptor;
        if (Files.exists(descriptorPath)) {
            descriptor = parser.parseProjectDescriptor(descriptorPath);
            log.debug("Loaded project descriptor from {}", descriptorPath);
        } else {
            descriptor = new EdgeProjectDescriptor();
            log.debug("No {} found in {}; using defaults", PROJECT_FILE, projectRoot);
        }

        final Path outputDir =
                projectRoot.resolve(descriptor.output).toAbsolutePath().normalize();
        final List<SourceFile> sourceFiles = new ArrayList<>();

        for (final String sourcePath : descriptor.sources) {
            final Path sourceDir = projectRoot.resolve(sourcePath);
            if (!Files.isDirectory(sourceDir)) {
                log.warn("Source directory does not exist: {}", sourceDir);
                continue;
            }
            discoverYamlFiles(sourceDir, outputDir, projectRoot, sourceFiles);
        }

        log.debug("Discovered {} source files", sourceFiles.size());
        return new LoadedProject(projectRoot, descriptor, sourceFiles);
    }

    private void discoverYamlFiles(
            final @NotNull Path sourceDir,
            final @NotNull Path outputDir,
            final @NotNull Path projectRoot,
            final @NotNull List<SourceFile> result)
            throws IOException {
        try (final Stream<Path> walk = Files.walk(sourceDir)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".yaml")
                            || p.getFileName().toString().endsWith(".yml"))
                    .filter(p -> !p.toAbsolutePath().normalize().startsWith(outputDir))
                    .filter(p -> !p.getFileName().toString().equals(PROJECT_FILE))
                    .forEach(p -> {
                        try {
                            final SourceFile file = parser.parseSourceFile(p);
                            result.add(file);
                            log.trace("Parsed {}", projectRoot.relativize(p));
                        } catch (final IOException e) {
                            log.warn("Failed to parse {}: {}", p, e.getMessage());
                            // Non-fatal — collected as a parse error in validation
                            final SourceFile errorFile = new SourceFile();
                            errorFile.path = p;
                            result.add(errorFile);
                        }
                    });
        }
    }

    public record LoadedProject(
            @NotNull Path projectRoot,
            @NotNull EdgeProjectDescriptor descriptor,
            @NotNull List<SourceFile> sourceFiles) {}
}
