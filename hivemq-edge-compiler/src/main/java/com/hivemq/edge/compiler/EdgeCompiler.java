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
package com.hivemq.edge.compiler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.edge.compiler.lib.model.CompiledConfig;
import com.hivemq.edge.compiler.source.discovery.ProjectLoader;
import com.hivemq.edge.compiler.source.resolution.GlobalResolver;
import com.hivemq.edge.compiler.source.translation.AdapterTranslator;
import com.hivemq.edge.compiler.source.validation.DiagnosticCollector;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

/**
 * Orchestrates all compiler phases: discover → parse → resolve → validate → translate.
 *
 * <p>Callers check {@link Result#hasErrors()} before using the compiled config.
 */
public class EdgeCompiler {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final @NotNull ProjectLoader projectLoader;
    private final @NotNull GlobalResolver globalResolver;
    private final @NotNull AdapterTranslator translator;

    public EdgeCompiler(
            final @NotNull ProjectLoader projectLoader,
            final @NotNull GlobalResolver globalResolver,
            final @NotNull AdapterTranslator translator) {
        this.projectLoader = projectLoader;
        this.globalResolver = globalResolver;
        this.translator = translator;
    }

    public EdgeCompiler() {
        this(new ProjectLoader(), new GlobalResolver(), new AdapterTranslator());
    }

    /**
     * Compiles the project at {@code projectRoot}, loading all sources declared in
     * {@code edge-project.yaml}. Embeds {@code workspace.json} from the project root if present.
     *
     * @param projectRoot directory containing {@code edge-project.yaml} (or defaults if absent)
     * @return a result with diagnostics; the compiled config is present only when there are no errors
     */
    public @NotNull Result compile(final @NotNull Path projectRoot) throws IOException {
        final Result result = compileLoaded(projectLoader.load(projectRoot));
        return withWorkspace(result, projectRoot.resolve("workspace.json"));
    }

    /**
     * Compiles a single instance from a multi-instance project. Embeds
     * {@code instances/<instanceId>/workspace.json} if present.
     *
     * @param projectRoot directory containing {@code edge-project.yaml}
     * @param instanceId  name of the instance subdirectory under {@code instances/}
     * @return a result with diagnostics; the compiled config is present only when there are no errors
     */
    public @NotNull Result compile(final @NotNull Path projectRoot, final @NotNull String instanceId)
            throws IOException {
        final Result result = compileLoaded(projectLoader.loadInstance(projectRoot, instanceId));
        return withWorkspace(
                result, projectRoot.resolve("instances").resolve(instanceId).resolve("workspace.json"));
    }

    private @NotNull Result compileLoaded(final @NotNull ProjectLoader.LoadedProject loaded) {
        final DiagnosticCollector errors = new DiagnosticCollector();

        final GlobalResolver.ResolvedProject resolved = globalResolver.resolve(loaded.sourceFiles(), errors);

        if (errors.hasErrors()) {
            return new Result(errors, null);
        }

        final String edgeVersion =
                loaded.descriptor().edgeVersion != null ? loaded.descriptor().edgeVersion : "unknown";
        final CompiledConfig compiled = translator.translate(resolved, edgeVersion, errors);

        if (errors.hasErrors()) {
            return new Result(errors, null);
        }

        return new Result(errors, compiled);
    }

    private @NotNull Result withWorkspace(final @NotNull Result result, final @NotNull Path workspaceFile) {
        if (result.compiledConfig() == null || !Files.exists(workspaceFile)) {
            return result;
        }
        try {
            final JsonNode workspace = OBJECT_MAPPER.readTree(workspaceFile.toFile());
            final CompiledConfig base = result.compiledConfig();
            final CompiledConfig withWorkspace = new CompiledConfig(
                    base.notice(),
                    base.signature(),
                    base.formatVersion(),
                    base.edgeVersion(),
                    base.protocolAdapters(),
                    base.dataCombiners(),
                    workspace);
            return new Result(result.diagnostics(), withWorkspace);
        } catch (final IOException e) {
            // workspace.json is non-functional — skip silently if unreadable
            return result;
        }
    }

    public record Result(
            @NotNull DiagnosticCollector diagnostics,
            @org.jetbrains.annotations.Nullable CompiledConfig compiledConfig) {

        public boolean hasErrors() {
            return diagnostics.hasErrors();
        }
    }
}
