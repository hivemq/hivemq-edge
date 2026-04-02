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
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
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
            names = {"--instance", "-i"},
            description = "Instance to compile (subdirectory name under instances/). "
                    + "Auto-detected when the project contains exactly one instance.")
    private @Nullable String instanceId;

    @Option(
            names = {"--output", "-o"},
            description = "Override output file path. Default: build/<instanceId>/compiled-config.json.")
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

        // Resolve the instance to compile
        final String resolvedInstanceId;
        try {
            resolvedInstanceId = resolveInstanceId(projectRoot);
        } catch (final IOException e) {
            System.err.println("ERROR: Failed to read project: " + e.getMessage());
            return 2;
        }
        if (resolvedInstanceId == null) {
            // resolveInstanceId already printed the error
            return 2;
        }

        final EdgeCompiler compiler = new EdgeCompiler();
        final EdgeCompiler.Result result;
        try {
            result = compiler.compile(projectRoot, resolvedInstanceId);
        } catch (final IOException e) {
            System.err.println("ERROR: Failed to read project: " + e.getMessage());
            return 2;
        }

        if (result.hasErrors() || (strict && !result.diagnostics().all().isEmpty())) {
            final List<Diagnostic> toReport =
                    strict ? result.diagnostics().all() : result.diagnostics().errors();
            reportDiagnosticsJson(toReport);
            reportDiagnosticsHuman(toReport, projectRoot);
            System.err.println(
                    "\nCompilation failed with " + result.diagnostics().errorCount() + " error(s)"
                            + (strict && result.diagnostics().warningCount() > 0
                                    ? ", " + result.diagnostics().warningCount() + " warning(s)"
                                    : "")
                            + ".");
            return 1;
        }

        // Determine output path — default is build/<instanceId>/compiled-config.json
        final Path outputPath;
        if (outputFile != null) {
            outputPath = outputFile.toPath();
        } else {
            outputPath =
                    projectRoot.resolve("build").resolve(resolvedInstanceId).resolve("compiled-config.json");
        }

        try {
            Files.createDirectories(outputPath.getParent());
            new CompiledConfigSerializer().toJson(result.compiledConfig(), outputPath.toFile());
        } catch (final IOException e) {
            System.err.println("ERROR: Failed to write output: " + e.getMessage());
            return 2;
        }

        // Report any warnings even on success
        final List<Diagnostic> warnings = result.diagnostics().warnings();
        if (!warnings.isEmpty()) {
            reportDiagnosticsJson(warnings);
            reportDiagnosticsHuman(warnings, projectRoot);
            System.err.println();
        }

        System.err.println("Compiled successfully → " + outputPath);
        return 0;
    }

    /**
     * Resolves which instance to compile.
     *
     * <ul>
     *   <li>If {@code --instance} was provided, returns it directly.
     *   <li>If the project has exactly one instance directory, auto-selects it.
     *   <li>If the project has multiple instance directories, prints an error and returns
     *       {@code null} (caller should exit with code 2).
     * </ul>
     */
    private @Nullable String resolveInstanceId(final @NotNull Path projectRoot) throws IOException {
        if (instanceId != null) {
            return instanceId;
        }

        final Path instancesDir = projectRoot.resolve("instances");
        if (!Files.isDirectory(instancesDir)) {
            System.err.println("ERROR: No 'instances/' directory found in " + projectRoot
                    + ". Use --instance to specify the instance to compile.");
            return null;
        }

        final List<String> instances;
        try (final Stream<Path> dirs = Files.list(instancesDir)) {
            instances = dirs.filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .toList();
        }

        if (instances.isEmpty()) {
            System.err.println("ERROR: No instance directories found under " + instancesDir + ".");
            return null;
        }

        if (instances.size() == 1) {
            System.err.println("Auto-selected instance: " + instances.get(0));
            return instances.get(0);
        }

        System.err.println("ERROR: Project contains multiple instances. Specify one with --instance <id>:");
        instances.forEach(id -> System.err.println("  " + id));
        return null;
    }

    /** Emits a JSON array of diagnostics to stdout — for tool/CI consumption. */
    private void reportDiagnosticsJson(final @NotNull List<Diagnostic> diagnostics) {
        try {
            System.out.println(DIAGNOSTIC_MAPPER.writeValueAsString(diagnostics));
        } catch (final IOException e) {
            // Fallback — plain text
            diagnostics.forEach(d ->
                    System.out.println("[" + (d.isError() ? "ERROR" : "WARN") + "] " + d.code() + ": " + d.message()));
        }
    }

    /**
     * Emits a human-readable summary to stderr — one block per diagnostic, with file/position info relative to the
     * project root.
     *
     * <p>Format:
     * <pre>
     * ERROR  UNRESOLVED_TAG_REFERENCE: Northbound mapping in adapter 'extruder-01' references tag 'NozzlePressure' which does not exist
     *        --> instances/berlin/adapters/extruder-01/nozzle-pressure.yaml:6:4
     * </pre>
     */
    private void reportDiagnosticsHuman(final @NotNull List<Diagnostic> diagnostics, final @NotNull Path projectRoot) {
        for (final Diagnostic d : diagnostics) {
            final String label = d.isError() ? "ERROR" : "WARN ";
            System.err.println(label + "  " + d.code() + ": " + d.message());
            if (d.file() != null) {
                final String filePart = relativize(d.file(), projectRoot);
                final String posPart = formatPosition(d.range());
                System.err.println("       --> " + filePart + posPart);
            }
        }
    }

    /** Returns a path relative to {@code projectRoot} if possible, otherwise the absolute path string. */
    private @NotNull String relativize(final @NotNull Path file, final @NotNull Path projectRoot) {
        try {
            return projectRoot.relativize(file.toAbsolutePath()).toString();
        } catch (final IllegalArgumentException e) {
            return file.toAbsolutePath().toString();
        }
    }

    /**
     * Formats the position part of a diagnostic location: {@code :line:character} (both 1-based for human display,
     * converted from 0-based LSP values), or empty string if no range.
     */
    private @NotNull String formatPosition(final @Nullable Diagnostic.DiagnosticRange range) {
        if (range == null) {
            return "";
        }
        // LSP uses 0-based; humans expect 1-based line/column numbers
        return ":" + (range.start().line() + 1) + ":" + (range.start().character() + 1);
    }
}
