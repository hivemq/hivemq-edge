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
package com.hivemq.edge.lsp.completion;

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.edge.compiler.source.model.SourceDeviceTag;
import com.hivemq.edge.compiler.source.model.SourceFile;
import com.hivemq.edge.compiler.source.model.SourceTag;
import com.hivemq.edge.lsp.workspace.WorkspaceIndex;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.lsp4j.CompletionItem;
import org.junit.jupiter.api.Test;

/**
 * Tests tag-name and device-tag-id completion using a programmatically populated {@link WorkspaceIndex}.
 */
class CompletionTagNameTest {

    private static final Path ADAPTER_DIR = Path.of("/workspace/instances/factory/adapters/pump-01");

    // ── tagName completion ────────────────────────────────────────────────────

    @Test
    void tagNameCompletionReturnsTagsFromSameDirectory() {
        final WorkspaceIndex index = indexWithTags(ADAPTER_DIR, "Pressure", "Temperature");
        final List<CompletionItem> items = completeOnLine(index, "tagName: ", ADAPTER_DIR.resolve("tags.yaml"));
        assertThat(labels(items)).containsExactlyInAnyOrder("Pressure", "Temperature");
    }

    @Test
    void tagNameCompletionIsEmptyForUnindexedDirectory() {
        final WorkspaceIndex index = indexWithTags(ADAPTER_DIR, "Pressure");
        final Path otherFile = Path.of("/workspace/instances/factory/adapters/other/tags.yaml");
        final List<CompletionItem> items = completeOnLine(index, "tagName: ", otherFile);
        assertThat(items).isEmpty();
    }

    // ── deviceTagId completion ────────────────────────────────────────────────

    @Test
    void deviceTagIdCompletionReturnsDeviceTagIds() {
        final WorkspaceIndex index = indexWithDeviceTags(ADAPTER_DIR, "ns=2;i=1001", "ns=2;i=1002");
        final List<CompletionItem> items = completeOnLine(index, "deviceTagId: ", ADAPTER_DIR.resolve("tags.yaml"));
        assertThat(labels(items)).containsExactlyInAnyOrder("ns=2;i=1001", "ns=2;i=1002");
    }

    @Test
    void deviceTagIdCompletionIsEmptyWhenNoDeviceTagsIndexed() {
        final WorkspaceIndex index = new WorkspaceIndex();
        final List<CompletionItem> items = completeOnLine(index, "deviceTagId: ", ADAPTER_DIR.resolve("tags.yaml"));
        assertThat(items).isEmpty();
    }

    // ── adapterId completion ──────────────────────────────────────────────────

    @Test
    void adapterIdCompletionSuggestsDirectoryName() {
        final WorkspaceIndex index = new WorkspaceIndex();
        final List<CompletionItem> items = completeOnLine(index, "id: ", ADAPTER_DIR.resolve("adapter.yaml"));
        assertThat(labels(items)).containsExactly("pump-01");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static WorkspaceIndex indexWithTags(final Path adapterDir, final String... tagNames) {
        final SourceFile manifest = manifest(
                adapterDir.resolve("adapter.yaml"), adapterDir.getFileName().toString());
        final SourceFile tagsFile = tagFile(adapterDir.resolve("tags.yaml"), tagNames);
        final WorkspaceIndex index = new WorkspaceIndex();
        index.rebuild(List.of(manifest, tagsFile));
        return index;
    }

    private static WorkspaceIndex indexWithDeviceTags(final Path adapterDir, final String... ids) {
        final SourceFile manifest = manifest(
                adapterDir.resolve("adapter.yaml"), adapterDir.getFileName().toString());
        final SourceFile dtFile = deviceTagFile(adapterDir.resolve("discovered.yaml"), ids);
        final WorkspaceIndex index = new WorkspaceIndex();
        index.rebuild(List.of(manifest, dtFile));
        return index;
    }

    private static List<CompletionItem> completeOnLine(
            final WorkspaceIndex index, final String lineText, final Path file) {
        final CompletionProvider provider = new CompletionProvider(index);
        return provider.complete(lineText, 0, lineText.length(), file);
    }

    private static List<String> labels(final List<CompletionItem> items) {
        return items.stream().map(CompletionItem::getLabel).toList();
    }

    private static SourceFile manifest(final Path path, final String id) {
        final SourceFile f = new SourceFile();
        f.path = path;
        f.id = id;
        f.type = "opcua";
        return f;
    }

    private static SourceFile tagFile(final Path path, final String... names) {
        final SourceFile f = new SourceFile();
        f.path = path;
        final List<SourceTag> tags = new ArrayList<>();
        for (final String name : names) {
            final SourceTag t = new SourceTag();
            t.name = name;
            tags.add(t);
        }
        f.tags = tags;
        return f;
    }

    private static SourceFile deviceTagFile(final Path path, final String... ids) {
        final SourceFile f = new SourceFile();
        f.path = path;
        final List<SourceDeviceTag> deviceTags = new ArrayList<>();
        for (final String id : ids) {
            final SourceDeviceTag dt = new SourceDeviceTag();
            dt.id = id;
            deviceTags.add(dt);
        }
        f.deviceTags = deviceTags;
        return f;
    }
}
