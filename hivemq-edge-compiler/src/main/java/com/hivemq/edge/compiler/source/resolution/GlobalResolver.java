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
package com.hivemq.edge.compiler.source.resolution;

import com.hivemq.edge.compiler.source.model.SourceDataCombiner;
import com.hivemq.edge.compiler.source.model.SourceFile;
import com.hivemq.edge.compiler.source.validation.Diagnostic;
import com.hivemq.edge.compiler.source.validation.DiagnosticCollector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Groups files by adapter directory, drives per-adapter resolution, then validates cross-adapter references in data
 * combiners.
 */
public class GlobalResolver {

    private static final String CROSS_ADAPTER_SEPARATOR = "::";

    private final @NotNull AdapterScopeResolver adapterScopeResolver;

    public GlobalResolver(final @NotNull AdapterScopeResolver adapterScopeResolver) {
        this.adapterScopeResolver = adapterScopeResolver;
    }

    public GlobalResolver() {
        this(new AdapterScopeResolver());
    }

    public @NotNull ResolvedProject resolve(
            final @NotNull List<SourceFile> allFiles, final @NotNull DiagnosticCollector errors) {

        // Group files by their parent directory
        final Map<java.nio.file.Path, List<SourceFile>> byDirectory =
                allFiles.stream().filter(f -> f.path != null).collect(Collectors.groupingBy(f -> f.path.getParent()));

        // Identify adapter directories: directories that contain exactly one adapter manifest
        final List<ResolvedAdapter> resolvedAdapters = new ArrayList<>();
        final List<SourceDataCombiner> allCombiners = new ArrayList<>();

        for (final Map.Entry<java.nio.file.Path, List<SourceFile>> entry : byDirectory.entrySet()) {
            final java.nio.file.Path dir = entry.getKey();
            final List<SourceFile> filesInDir = entry.getValue();

            final List<SourceFile> manifests =
                    filesInDir.stream().filter(SourceFile::isAdapterManifest).toList();

            if (manifests.size() > 1) {
                errors.add(Diagnostic.error(
                        "MULTIPLE_ADAPTER_CONFIGS",
                        "Directory '" + dir + "' contains " + manifests.size()
                                + " adapter config files — exactly one is allowed",
                        dir.resolve(".")));
                continue;
            }

            if (manifests.size() == 1) {
                final SourceFile manifest = manifests.get(0);
                validateAdapterId(manifest, errors);
                final ResolvedAdapter resolved = adapterScopeResolver.resolve(manifest, filesInDir, errors);
                resolvedAdapters.add(resolved);
            }

            // Collect data combiners from all files in this directory (even non-adapter dirs)
            filesInDir.forEach(f -> allCombiners.addAll(f.dataCombiners));
        }

        // Build global tag pool for cross-adapter reference resolution
        final Map<String, Map<String, ResolvedTag>> globalTagPool = new HashMap<>();
        for (final ResolvedAdapter adapter : resolvedAdapters) {
            globalTagPool.put(adapter.adapterId(), adapter.tags());
        }

        // Validate data combiner cross-adapter references
        for (final SourceDataCombiner combiner : allCombiners) {
            validateCombinerReferences(combiner, globalTagPool, errors);
        }

        return new ResolvedProject(resolvedAdapters, allCombiners);
    }

    private void validateAdapterId(final @NotNull SourceFile manifest, final @NotNull DiagnosticCollector errors) {
        if (manifest.id == null || manifest.id.isBlank()) {
            errors.add(Diagnostic.error(
                    "ADAPTER_MISSING_ID",
                    "Adapter manifest at '" + manifest.path + "' has no 'id' field",
                    manifest.path));
            return;
        }
        if (manifest.path != null) {
            final String dirName = manifest.path.getParent().getFileName().toString();
            if (!dirName.equals(manifest.id)) {
                errors.add(Diagnostic.error(
                        "ADAPTER_ID_MISMATCH",
                        "Adapter id '" + manifest.id + "' does not match directory name '" + dirName + "'",
                        manifest.path,
                        Map.of("adapterId", manifest.id, "directoryName", dirName)));
            }
        }
    }

    private void validateCombinerReferences(
            final @NotNull SourceDataCombiner combiner,
            final @NotNull Map<String, Map<String, ResolvedTag>> globalTagPool,
            final @NotNull DiagnosticCollector errors) {

        if (combiner.name == null || combiner.name.isBlank()) {
            errors.add(Diagnostic.error("COMBINER_MISSING_NAME", "A data combiner has no 'name' field"));
            return;
        }

        combiner.mappings.forEach(mapping -> {
            if (mapping.trigger != null && mapping.trigger.tag != null) {
                validateCrossAdapterRef(mapping.trigger.tag, combiner.name, "trigger", globalTagPool, errors);
            }
            mapping.instructions.forEach(instruction -> {
                if (instruction.source != null && instruction.source.tag != null) {
                    validateCrossAdapterRef(
                            instruction.source.tag, combiner.name, "instruction source", globalTagPool, errors);
                }
            });
        });
    }

    private void validateCrossAdapterRef(
            final @NotNull String ref,
            final @NotNull String combinerName,
            final @NotNull String context,
            final @NotNull Map<String, Map<String, ResolvedTag>> globalTagPool,
            final @NotNull DiagnosticCollector errors) {

        final int sepIdx = ref.indexOf(CROSS_ADAPTER_SEPARATOR);
        if (sepIdx < 0) {
            errors.add(Diagnostic.error(
                    "INVALID_CROSS_ADAPTER_REFERENCE",
                    "Data combiner '" + combinerName + "' " + context + " reference '" + ref
                            + "' must use the format 'adapterId::tagName'",
                    null,
                    Map.of("combinerName", combinerName, "reference", ref)));
            return;
        }

        final String adapterId = ref.substring(0, sepIdx);
        final String tagName = ref.substring(sepIdx + CROSS_ADAPTER_SEPARATOR.length());

        final @Nullable Map<String, ResolvedTag> adapterTags = globalTagPool.get(adapterId);
        if (adapterTags == null) {
            errors.add(Diagnostic.error(
                    "UNKNOWN_ADAPTER_REFERENCE",
                    "Data combiner '" + combinerName + "' " + context + " references unknown adapter '" + adapterId
                            + "'",
                    null,
                    Map.of("combinerName", combinerName, "adapterId", adapterId)));
            return;
        }

        if (!adapterTags.containsKey(tagName)) {
            errors.add(Diagnostic.error(
                    "UNRESOLVED_CROSS_ADAPTER_REFERENCE",
                    "Data combiner '" + combinerName + "' " + context + " references tag '" + tagName
                            + "' which does not exist in adapter '" + adapterId + "'",
                    null,
                    Map.of("combinerName", combinerName, "adapterId", adapterId, "tagName", tagName)));
        }
    }

    public record ResolvedProject(
            @NotNull List<ResolvedAdapter> adapters,
            @NotNull List<SourceDataCombiner> dataCombiners) {}
}
