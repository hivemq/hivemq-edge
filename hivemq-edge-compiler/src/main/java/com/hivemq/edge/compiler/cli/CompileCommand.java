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
package com.hivemq.edge.compiler.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hivemq.edge.compiler.EdgeCompiler;
import com.hivemq.edge.compiler.lib.serialization.CompiledConfigSerializer;
import com.hivemq.edge.compiler.source.validation.Diagnostic;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "compile",
        description = "Compile a config directory into a deployable JSON artifact.",
        mixinStandardHelpOptions = true)
public class CompileCommand implements Callable<Integer> {

    private static final ObjectMapper DIAGNOSTIC_MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Option(
            names = {"--project", "-p"},
            description = "Path to the project directory (containing edge-project.yaml). Default: current directory.",
            defaultValue = ".")
    private @Nullable File projectDir;

    @Option(
            names = {"--output", "-o"},
            description = "Override output file path. Default: build/compiled-config.json.")
    private @Nullable File outputFile;

    @Option(
            names = {"--strict"},
            description = "Treat warnings as errors.")
    private boolean strict;

    @Override
    public Integer call() {
        final Path projectRoot = (projectDir != null ? projectDir : new File("."))
                .toPath()
                .toAbsolutePath()
                .normalize();

        final EdgeCompiler compiler = new EdgeCompiler();
        final EdgeCompiler.Result result;
        try {
            result = compiler.compile(projectRoot);
        } catch (final IOException e) {
            System.err.println("ERROR: Failed to read project: " + e.getMessage());
            return 2;
        }

        if (result.hasErrors() || (strict && !result.diagnostics().all().isEmpty())) {
            final List<Diagnostic> toReport =
                    strict ? result.diagnostics().all() : result.diagnostics().errors();
            reportDiagnostics(toReport);
            System.err.println(
                    "\nCompilation failed with " + result.diagnostics().errorCount() + " error(s).");
            return 1;
        }

        // Determine output path
        final Path outputPath;
        if (outputFile != null) {
            outputPath = outputFile.toPath();
        } else {
            outputPath = projectRoot.resolve("build").resolve("compiled-config.json");
        }

        try {
            Files.createDirectories(outputPath.getParent());
            new CompiledConfigSerializer().toJson(result.compiledConfig(), outputPath.toFile());
        } catch (final IOException e) {
            System.err.println("ERROR: Failed to write output: " + e.getMessage());
            return 2;
        }

        if (!result.diagnostics().all().isEmpty()) {
            reportDiagnostics(result.diagnostics().all());
        }
        System.err.println("Compiled successfully → " + outputPath);
        return 0;
    }

    private void reportDiagnostics(final List<Diagnostic> diagnostics) {
        try {
            // Machine-readable LSP Diagnostic JSON array to stdout
            System.out.println(DIAGNOSTIC_MAPPER.writeValueAsString(diagnostics));
        } catch (final IOException e) {
            // Fallback — plain text
            diagnostics.forEach(d -> System.out.println("[" + (d.isError() ? "ERROR" : "WARN") + "] "
                    + d.code() + ": " + d.message()
                    + (d.file() != null ? " (" + d.file() + ")" : "")));
        }
    }
}
