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

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.edge.compiler.source.model.SourceFile;
import com.hivemq.edge.compiler.source.model.SourceTag;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorkspaceIndexTest {

    // ── Rebuild ────────────────────────────────────────────────────────────────

    @Test
    void tagNamesAreIndexedAfterRebuild() {
        final Path dir = Path.of("/workspace/instances/factory/adapters/pump-01");
        final WorkspaceIndex index = new WorkspaceIndex();
        index.rebuild(List.of(
                manifest(dir.resolve("adapter.yaml"), "pump-01", "opcua"),
                tagFile(dir.resolve("tags.yaml"), "Pressure", "Temperature")));

        assertThat(index.tagNamesForFile(dir.resolve("tags.yaml")))
                .containsExactlyInAnyOrder("Pressure", "Temperature");
    }

    @Test
    void adapterIdSuggestionReturnsDirectoryName() {
        final Path dir = Path.of("/workspace/instances/factory/adapters/pump-01");
        final WorkspaceIndex index = new WorkspaceIndex();
        index.rebuild(List.of(manifest(dir.resolve("adapter.yaml"), "pump-01", "opcua")));

        assertThat(index.adapterIdSuggestionForFile(dir.resolve("adapter.yaml")))
                .containsExactly("pump-01");
    }

    // ── Incremental update ─────────────────────────────────────────────────────

    @Test
    void updateFileAddsNewTagsToIndex() {
        final Path dir = Path.of("/workspace/instances/factory/adapters/pump-01");
        final Path tagsPath = dir.resolve("tags.yaml");
        final WorkspaceIndex index = new WorkspaceIndex();
        index.rebuild(List.of(tagFile(tagsPath, "Alpha")));

        index.updateFile(tagFile(tagsPath, "Alpha", "Beta"));

        assertThat(index.tagNamesForFile(tagsPath)).containsExactlyInAnyOrder("Alpha", "Beta");
    }

    @Test
    void removeFileDropsTagsFromIndex() {
        final Path dir = Path.of("/workspace/instances/factory/adapters/pump-01");
        final Path tagsPath = dir.resolve("tags.yaml");
        final WorkspaceIndex index = new WorkspaceIndex();
        index.rebuild(List.of(tagFile(tagsPath, "Alpha")));

        index.removeFile(tagsPath);

        assertThat(index.tagNamesForFile(tagsPath)).isEmpty();
    }

    // ── Cross-adapter refs ─────────────────────────────────────────────────────

    @Test
    void crossAdapterRefsIncludeTagsFromBothAdapters() {
        final Path instanceRoot = Path.of("/workspace/instances/factory");
        final Path dir1 = instanceRoot.resolve("adapters/pump-01");
        final Path dir2 = instanceRoot.resolve("adapters/hvac-01");

        final WorkspaceIndex index = new WorkspaceIndex();
        index.rebuild(List.of(
                manifest(dir1.resolve("adapter.yaml"), "pump-01", "opcua"),
                tagFile(dir1.resolve("tags.yaml"), "Pressure"),
                manifest(dir2.resolve("adapter.yaml"), "hvac-01", "bacnetip"),
                tagFile(dir2.resolve("tags.yaml"), "ZoneTemp")));

        final Path combinerFile = instanceRoot.resolve("data-combiners/status.yaml");
        assertThat(index.crossAdapterRefsForFile(combinerFile))
                .containsExactlyInAnyOrder("pump-01::Pressure", "hvac-01::ZoneTemp");
    }

    @Test
    void tagNamesForAdapterFiltersToSpecificAdapter() {
        final Path instanceRoot = Path.of("/workspace/instances/factory");
        final Path dir1 = instanceRoot.resolve("adapters/pump-01");
        final Path dir2 = instanceRoot.resolve("adapters/hvac-01");

        final WorkspaceIndex index = new WorkspaceIndex();
        index.rebuild(List.of(
                manifest(dir1.resolve("adapter.yaml"), "pump-01", "opcua"),
                tagFile(dir1.resolve("tags.yaml"), "Pressure"),
                manifest(dir2.resolve("adapter.yaml"), "hvac-01", "bacnetip"),
                tagFile(dir2.resolve("tags.yaml"), "ZoneTemp")));

        final Path combinerFile = instanceRoot.resolve("data-combiners/status.yaml");
        assertThat(index.tagNamesForAdapter(combinerFile, "pump-01")).containsExactly("Pressure");
        assertThat(index.tagNamesForAdapter(combinerFile, "hvac-01")).containsExactly("ZoneTemp");
    }

    @Test
    void crossAdapterRefsAreEmptyForFileOutsideInstances() {
        final Path file = Path.of("/workspace/some-other-dir/file.yaml");
        assertThat(new WorkspaceIndex().crossAdapterRefsForFile(file)).isEmpty();
    }

    // ── findInstanceRoot ───────────────────────────────────────────────────────

    @Test
    void findInstanceRootReturnsDirectoryUnderInstances() {
        final Path file = Path.of("/workspace/instances/factory/adapters/pump-01/tags.yaml");
        final Path instanceRoot = WorkspaceIndex.findInstanceRoot(file);
        assertThat(instanceRoot).isNotNull();
        assertThat(instanceRoot.getFileName().toString()).isEqualTo("factory");
    }

    @Test
    void findInstanceRootReturnsNullForPathNotUnderInstances() {
        assertThat(WorkspaceIndex.findInstanceRoot(Path.of("/workspace/other/file.yaml")))
                .isNull();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static SourceFile manifest(final Path path, final String id, final String type) {
        final SourceFile f = new SourceFile();
        f.path = path;
        f.id = id;
        f.type = type;
        return f;
    }

    private static SourceFile tagFile(final Path path, final String... tagNames) {
        final SourceFile f = new SourceFile();
        f.path = path;
        final List<SourceTag> tags = new ArrayList<>();
        for (final String name : tagNames) {
            final SourceTag t = new SourceTag();
            t.name = name;
            tags.add(t);
        }
        f.tags = tags;
        return f;
    }
}
