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

import com.hivemq.edge.compiler.lib.model.CompiledConfig;
import com.hivemq.edge.compiler.source.discovery.ProjectLoader;
import com.hivemq.edge.compiler.source.resolution.GlobalResolver;
import com.hivemq.edge.compiler.source.translation.AdapterTranslator;
import com.hivemq.edge.compiler.source.validation.DiagnosticCollector;
import java.io.IOException;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

/**
 * Orchestrates all compiler phases: discover → parse → resolve → validate → translate.
 *
 * <p>Callers check {@link Result#hasErrors()} before using the compiled config.
 */
public class EdgeCompiler {

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
     * {@code edge-project.yaml}.
     *
     * @param projectRoot directory containing {@code edge-project.yaml} (or defaults if absent)
     * @return a result with diagnostics; the compiled config is present only when there are no errors
     */
    public @NotNull Result compile(final @NotNull Path projectRoot) throws IOException {
        return compileLoaded(projectLoader.load(projectRoot));
    }

    /**
     * Compiles a single instance from a multi-instance project.
     *
     * <p>Only files under {@code projectRoot/instances/<instanceId>/} are loaded. Other instances
     * are ignored.
     *
     * @param projectRoot directory containing {@code edge-project.yaml}
     * @param instanceId  name of the instance subdirectory under {@code instances/}
     * @return a result with diagnostics; the compiled config is present only when there are no errors
     */
    public @NotNull Result compile(final @NotNull Path projectRoot, final @NotNull String instanceId)
            throws IOException {
        return compileLoaded(projectLoader.loadInstance(projectRoot, instanceId));
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

    public record Result(
            @NotNull DiagnosticCollector diagnostics,
            @org.jetbrains.annotations.Nullable CompiledConfig compiledConfig) {

        public boolean hasErrors() {
            return diagnostics.hasErrors();
        }
    }
}
