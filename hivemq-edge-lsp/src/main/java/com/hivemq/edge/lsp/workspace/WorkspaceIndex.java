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

import com.hivemq.edge.compiler.source.model.SourceDeviceTag;
import com.hivemq.edge.compiler.source.model.SourceFile;
import com.hivemq.edge.compiler.source.model.SourceNorthboundMapping;
import com.hivemq.edge.compiler.source.model.SourceTag;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Cross-file model of the config workspace. Maintained by {@link WorkspaceLoader} (initial scan) and
 * {@link WorkspaceWatcher} (incremental updates).
 *
 * <p>All public methods are thread-safe: reads acquire no lock (the map is replaced atomically on update);
 * writes use synchronized on {@code this}.
 */
public class WorkspaceIndex {

    /** file path → most recently parsed SourceFile */
    private volatile @NotNull Map<Path, SourceFile> files = Map.of();

    /** adapterDirectory → adapterId from the manifest in that directory */
    private volatile @NotNull Map<Path, String> adapterIdByDir = Map.of();

    /** adapterDirectory → tag names (from all files in that directory) */
    private volatile @NotNull Map<Path, List<String>> tagNamesByDir = Map.of();

    /** adapterDirectory → device tag IDs (from all files in that directory) */
    private volatile @NotNull Map<Path, List<String>> deviceTagIdsByDir = Map.of();

    /** instanceRoot → sorted list of adapter IDs found under that instance */
    private volatile @NotNull Map<Path, List<String>> adapterIdsByInstance = Map.of();

    // ── Update ────────────────────────────────────────────────────────────────

    /**
     * Replaces the full index content. Called by {@link WorkspaceLoader} after initial or full scan.
     *
     * @param allFiles all parsed SourceFiles in the workspace
     */
    public synchronized void rebuild(final @NotNull List<SourceFile> allFiles) {
        final Map<Path, SourceFile> newFiles = new HashMap<>();
        final Map<Path, List<SourceFile>> byDir = new HashMap<>();

        for (final SourceFile file : allFiles) {
            if (file.path == null) continue;
            newFiles.put(file.path, file);
            byDir.computeIfAbsent(file.path.getParent(), k -> new ArrayList<>()).add(file);
        }

        final Map<Path, String> newAdapterIdByDir = new HashMap<>();
        final Map<Path, List<String>> newTagNames = new HashMap<>();
        final Map<Path, List<String>> newDeviceTagIds = new HashMap<>();

        for (final Map.Entry<Path, List<SourceFile>> entry : byDir.entrySet()) {
            final Path dir = entry.getKey();
            final List<SourceFile> dirFiles = entry.getValue();
            indexDirectory(dir, dirFiles, newAdapterIdByDir, newTagNames, newDeviceTagIds);
        }

        // Group adapter directories by instance root
        final Map<Path, List<String>> newAdaptersByInstance = new HashMap<>();
        for (final Map.Entry<Path, String> e : newAdapterIdByDir.entrySet()) {
            final Path instanceRoot = findInstanceRoot(e.getKey());
            if (instanceRoot != null) {
                newAdaptersByInstance
                        .computeIfAbsent(instanceRoot, k -> new ArrayList<>())
                        .add(e.getValue());
            }
        }
        newAdaptersByInstance.values().forEach(Collections::sort);

        // Atomic replace
        this.files = Collections.unmodifiableMap(newFiles);
        this.adapterIdByDir = Collections.unmodifiableMap(newAdapterIdByDir);
        this.tagNamesByDir = Collections.unmodifiableMap(newTagNames);
        this.deviceTagIdsByDir = Collections.unmodifiableMap(newDeviceTagIds);
        this.adapterIdsByInstance = Collections.unmodifiableMap(newAdaptersByInstance);
    }

    /**
     * Updates a single file in the index, then rebuilds derived maps for affected directories.
     * Called by {@link WorkspaceWatcher} on incremental file changes.
     */
    public synchronized void updateFile(final @NotNull SourceFile updatedFile) {
        if (updatedFile.path == null) return;

        final Map<Path, SourceFile> newFiles = new ConcurrentHashMap<>(this.files);
        newFiles.put(updatedFile.path, updatedFile);

        rebuild(new ArrayList<>(newFiles.values()));
    }

    /**
     * Removes a deleted file from the index, then rebuilds.
     */
    public synchronized void removeFile(final @NotNull Path deletedPath) {
        final Map<Path, SourceFile> newFiles = new ConcurrentHashMap<>(this.files);
        newFiles.remove(deletedPath);
        rebuild(new ArrayList<>(newFiles.values()));
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    /** Returns all tag names defined in the adapter directory that contains {@code file}. */
    public @NotNull List<String> tagNamesForFile(final @NotNull Path file) {
        final Path dir = file.getParent();
        return tagNamesByDir.getOrDefault(dir, List.of());
    }

    /** Returns all device tag IDs defined in the adapter directory that contains {@code file}. */
    public @NotNull List<String> deviceTagIdsForFile(final @NotNull Path file) {
        final Path dir = file.getParent();
        return deviceTagIdsByDir.getOrDefault(dir, List.of());
    }

    /**
     * Returns all {@code adapterId::tagName} cross-adapter references available in the instance
     * that contains {@code file}.
     */
    public @NotNull List<String> crossAdapterRefsForFile(final @NotNull Path file) {
        final Path instanceRoot = findInstanceRoot(file);
        if (instanceRoot == null) return List.of();

        final List<String> refs = new ArrayList<>();
        for (final Map.Entry<Path, String> e : adapterIdByDir.entrySet()) {
            final Path adapterDir = e.getKey();
            if (!adapterDir.startsWith(instanceRoot)) continue;
            final String adapterId = e.getValue();
            for (final String tagName : tagNamesByDir.getOrDefault(adapterDir, List.of())) {
                refs.add(adapterId + "::" + tagName);
            }
        }
        refs.sort(String::compareTo);
        return refs;
    }

    /**
     * Returns all tag names for the given adapter ID within the instance that contains {@code file}.
     * Used for cross-adapter completion after {@code adapterId::}.
     */
    public @NotNull List<String> tagNamesForAdapter(final @NotNull Path file, final @NotNull String adapterId) {
        final Path instanceRoot = findInstanceRoot(file);
        if (instanceRoot == null) return List.of();

        for (final Map.Entry<Path, String> e : adapterIdByDir.entrySet()) {
            if (!e.getValue().equals(adapterId)) continue;
            if (!e.getKey().startsWith(instanceRoot)) continue;
            return tagNamesByDir.getOrDefault(e.getKey(), List.of());
        }
        return List.of();
    }

    /** Returns the directory name (last path component) as a single suggestion for adapter {@code id:} completion. */
    public @NotNull List<String> adapterIdSuggestionForFile(final @NotNull Path file) {
        final Path dir = file.getParent();
        if (dir == null) return List.of();
        return List.of(dir.getFileName().toString());
    }

    /**
     * Returns all parsed source files in the workspace.
     */
    public @NotNull Map<Path, SourceFile> allFiles() {
        return files;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void indexDirectory(
            final @NotNull Path dir,
            final @NotNull List<SourceFile> dirFiles,
            final @NotNull Map<Path, String> adapterIdByDir,
            final @NotNull Map<Path, List<String>> tagNamesByDir,
            final @NotNull Map<Path, List<String>> deviceTagIdsByDir) {

        // Collect tag names and device tag IDs from all files in this directory
        final List<String> tagNames = new ArrayList<>();
        final List<String> deviceTagIds = new ArrayList<>();
        String adapterId = null;

        for (final SourceFile file : dirFiles) {
            if (file.type != null && file.id != null) {
                adapterId = file.id;
            }
            for (final SourceTag tag : file.tags) {
                if (tag.name != null && !tag.name.isBlank()) {
                    tagNames.add(tag.name);
                }
            }
            // Inline tags defined inside northbound mappings (tag: { name: ... } form)
            for (final SourceNorthboundMapping mapping : file.northbound) {
                if (mapping.tag != null && mapping.tag.name != null && !mapping.tag.name.isBlank()) {
                    tagNames.add(mapping.tag.name);
                }
            }
            for (final SourceDeviceTag dt : file.deviceTags) {
                if (dt.id != null && !dt.id.isBlank()) {
                    deviceTagIds.add(dt.id);
                }
            }
        }

        if (adapterId != null) {
            adapterIdByDir.put(dir, adapterId);
        }
        if (!tagNames.isEmpty()) {
            Collections.sort(tagNames);
            tagNamesByDir.put(dir, Collections.unmodifiableList(tagNames));
        }
        if (!deviceTagIds.isEmpty()) {
            Collections.sort(deviceTagIds);
            deviceTagIdsByDir.put(dir, Collections.unmodifiableList(deviceTagIds));
        }
    }

    /**
     * Finds the instance root directory for a path. The instance root is the directory immediately
     * under the {@code instances/} directory in the path hierarchy.
     *
     * <p>E.g. for {@code /workspace/instances/wolery/adapters/pump-01/tags.yaml}, returns
     * {@code /workspace/instances/wolery}.
     */
    @Nullable
    static Path findInstanceRoot(final @NotNull Path path) {
        Path p = path.toAbsolutePath().normalize();
        // Walk up: look for a parent named "instances" and return its child
        while (p.getParent() != null) {
            if ("instances"
                    .equals(
                            p.getParent().getFileName() != null
                                    ? p.getParent().getFileName().toString()
                                    : null)) {
                return p;
            }
            p = p.getParent();
        }
        return null;
    }
}
