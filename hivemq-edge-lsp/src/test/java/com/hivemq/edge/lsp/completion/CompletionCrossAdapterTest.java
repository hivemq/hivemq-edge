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

import com.hivemq.edge.compiler.source.model.SourceFile;
import com.hivemq.edge.compiler.source.model.SourceTag;
import com.hivemq.edge.lsp.workspace.WorkspaceIndex;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.lsp4j.CompletionItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests cross-adapter completion (both the {@code adapterId::tagName} list and the per-adapter
 * tag-name list after {@code ::}).
 */
class CompletionCrossAdapterTest {

    private static final Path INSTANCE_ROOT = Path.of("/workspace/instances/factory");
    private static final Path COMBINER_FILE = INSTANCE_ROOT.resolve("data-combiners/status.yaml");

    private CompletionProvider provider;

    @BeforeEach
    void setUp() {
        final Path dir1 = INSTANCE_ROOT.resolve("adapters/pump-01");
        final Path dir2 = INSTANCE_ROOT.resolve("adapters/hvac-01");

        final WorkspaceIndex index = new WorkspaceIndex();
        index.rebuild(List.of(
                manifest(dir1.resolve("adapter.yaml"), "pump-01", "opcua"),
                tagFile(dir1.resolve("tags.yaml"), "Pressure"),
                manifest(dir2.resolve("adapter.yaml"), "hvac-01", "bacnetip"),
                tagFile(dir2.resolve("tags.yaml"), "ZoneTemp")));

        provider = new CompletionProvider(index);
    }

    @Test
    void tagCompletionReturnsAllCrossAdapterRefs() {
        // tag: appears as a mapping key inside a combiner — nested, not directly after "- "
        final String doc = "dataCombiners:\n  - name: Status\n    tag: ";
        final List<CompletionItem> items = complete(doc, 2, COMBINER_FILE);
        assertThat(labels(items)).containsExactlyInAnyOrder("pump-01::Pressure", "hvac-01::ZoneTemp");
    }

    @Test
    void tagCompletionAfterDoubleColonFiltersToAdapter() {
        final String doc = "dataCombiners:\n  - name: Status\n    tag: pump-01::";
        final List<CompletionItem> items = complete(doc, 2, COMBINER_FILE);
        assertThat(labels(items)).containsExactly("Pressure");
    }

    @Test
    void tagCompletionAfterDoubleColonForOtherAdapter() {
        final String doc = "dataCombiners:\n  - name: Status\n    tag: hvac-01::";
        final List<CompletionItem> items = complete(doc, 2, COMBINER_FILE);
        assertThat(labels(items)).containsExactly("ZoneTemp");
    }

    @Test
    void tagCompletionReturnsEmptyForUnknownAdapter() {
        final String doc = "dataCombiners:\n  - name: Status\n    tag: no-such-adapter::";
        final List<CompletionItem> items = complete(doc, 2, COMBINER_FILE);
        assertThat(items).isEmpty();
    }

    @Test
    void crossAdapterRefsAreEmptyForFileNotInInstances() {
        final Path outsideFile = Path.of("/workspace/other/file.yaml");
        final String doc = "dataCombiners:\n  - name: Status\n    tag: ";
        final List<CompletionItem> items = complete(doc, 2, outsideFile);
        assertThat(items).isEmpty();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<CompletionItem> complete(final String doc, final int line, final Path file) {
        return provider.complete(doc, line, doc.split("\n")[line].length(), file);
    }

    private static List<String> labels(final List<CompletionItem> items) {
        return items.stream().map(CompletionItem::getLabel).toList();
    }

    private static SourceFile manifest(final Path path, final String id, final String type) {
        final SourceFile f = new SourceFile();
        f.path = path;
        f.id = id;
        f.type = type;
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
}
