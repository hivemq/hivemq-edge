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

import java.nio.file.Path;
import java.util.List;
import org.eclipse.lsp4j.CompletionItem;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link DocumentContext} line analysis and {@link AdapterTypeCompletion}.
 */
class CompletionTypeTest {

    // ── DocumentContext.analyze() — target detection ──────────────────────────

    @Test
    void typeKeyIsRecognised() {
        final DocumentContext ctx = DocumentContext.analyze("type: ", 0, 6);
        assertThat(ctx.target()).isEqualTo(DocumentContext.CompletionTarget.TYPE);
    }

    @Test
    void tagNameKeyIsRecognised() {
        final String doc = "northbound:\n  - tagName: ";
        final DocumentContext ctx = DocumentContext.analyze(doc, 1, doc.split("\n")[1].length());
        assertThat(ctx.target()).isEqualTo(DocumentContext.CompletionTarget.TAG_NAME);
    }

    @Test
    void tagNameKeyIsRecognisedWhenIndentedWithoutListMarker() {
        final String doc = "tags:\n  - name: Pressure\n    tagName: ";
        final DocumentContext ctx = DocumentContext.analyze(doc, 2, doc.split("\n")[2].length());
        assertThat(ctx.target()).isEqualTo(DocumentContext.CompletionTarget.TAG_NAME);
    }

    @Test
    void deviceTagIdKeyIsRecognised() {
        final String doc = "tags:\n  - deviceTagId: ";
        final DocumentContext ctx = DocumentContext.analyze(doc, 1, doc.split("\n")[1].length());
        assertThat(ctx.target()).isEqualTo(DocumentContext.CompletionTarget.DEVICE_TAG_ID);
    }

    @Test
    void tagKeyIsRecognisedAsCrossAdapterTag() {
        final String doc = "dataCombiners:\n  - tag: ";
        final DocumentContext ctx = DocumentContext.analyze(doc, 1, doc.split("\n")[1].length());
        assertThat(ctx.target()).isEqualTo(DocumentContext.CompletionTarget.CROSS_ADAPTER_TAG);
    }

    @Test
    void tagKeyWithAdapterPrefixIsRecognisedAsCrossAdapterTagName() {
        final String doc = "dataCombiners:\n  - tag: pump-01::";
        final String line1 = doc.split("\n")[1];
        final DocumentContext ctx = DocumentContext.analyze(doc, 1, line1.length());
        assertThat(ctx.target()).isEqualTo(DocumentContext.CompletionTarget.CROSS_ADAPTER_TAG_NAME);
        assertThat(ctx.adapterIdPrefix()).isEqualTo("pump-01");
    }

    @Test
    void idKeyIsRecognisedAsAdapterId() {
        final DocumentContext ctx = DocumentContext.analyze("id: ", 0, 4);
        assertThat(ctx.target()).isEqualTo(DocumentContext.CompletionTarget.ADAPTER_ID);
    }

    @Test
    void unrecognisedKeyProducesUnknownTarget() {
        final DocumentContext ctx = DocumentContext.analyze("name: my-adapter\n", 0, 5);
        assertThat(ctx.target()).isEqualTo(DocumentContext.CompletionTarget.UNKNOWN);
    }

    @Test
    void lineOutOfRangeProducesUnknownTarget() {
        final DocumentContext ctx = DocumentContext.analyze("type: opcua\n", 99, 0);
        assertThat(ctx.target()).isEqualTo(DocumentContext.CompletionTarget.UNKNOWN);
    }

    // ── DocumentContext.analyze() — file type detection ───────────────────────

    @Test
    void adapterFileTypeIsDetectedFromTopLevelTypeKey() {
        final String doc = "type: opcua\nid: pump-01\n";
        assertThat(DocumentContext.analyze(doc, 0, 6).fileType()).isEqualTo(DocumentContext.FileType.ADAPTER);
    }

    @Test
    void combinerFileTypeIsDetectedFromDataCombinersKey() {
        final String doc = "dataCombiners:\n  - name: Status\n";
        assertThat(DocumentContext.analyze(doc, 0, 5).fileType()).isEqualTo(DocumentContext.FileType.COMBINER);
    }

    @Test
    void otherFileTypeIsUsedWhenNeitherMarkerPresent() {
        final String doc = "tags:\n  - name: Pressure\n";
        assertThat(DocumentContext.analyze(doc, 0, 5).fileType()).isEqualTo(DocumentContext.FileType.OTHER);
    }

    @Test
    void indentedTypeKeyDoesNotTriggerAdapterDetection() {
        // An indented "type:" is not a top-level adapter discriminator
        final String doc = "connection:\n  type: opc\n";
        assertThat(DocumentContext.analyze(doc, 0, 5).fileType()).isEqualTo(DocumentContext.FileType.OTHER);
    }

    // ── AdapterTypeCompletion ─────────────────────────────────────────────────

    @Test
    void adapterTypeCompletionReturnsKnownTypes() {
        final List<CompletionItem> items = new AdapterTypeCompletion().complete();
        final List<String> labels = items.stream().map(CompletionItem::getLabel).toList();
        assertThat(labels).contains("opcua", "bacnetip");
    }

    @Test
    void adapterTypeCompletionViaProvider() {
        final com.hivemq.edge.lsp.workspace.WorkspaceIndex index = new com.hivemq.edge.lsp.workspace.WorkspaceIndex();
        final CompletionProvider provider = new CompletionProvider(index);
        final Path anyFile = Path.of("/workspace/instances/x/adapters/pump-01/adapter.yaml");

        final List<CompletionItem> items = provider.complete("type: ", 0, 6, anyFile);
        final List<String> labels = items.stream().map(CompletionItem::getLabel).toList();
        assertThat(labels).contains("opcua", "bacnetip");
    }
}
