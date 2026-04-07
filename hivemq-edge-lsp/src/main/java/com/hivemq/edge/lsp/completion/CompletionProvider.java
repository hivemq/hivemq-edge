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

import com.hivemq.edge.lsp.workspace.WorkspaceIndex;
import java.nio.file.Path;
import java.util.List;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.jetbrains.annotations.NotNull;

/**
 * Dispatches completion requests to the appropriate provider based on {@link DocumentContext}.
 */
public class CompletionProvider {

    private final @NotNull AdapterTypeCompletion adapterTypeCompletion = new AdapterTypeCompletion();
    private final @NotNull TagNameCompletion tagNameCompletion;
    private final @NotNull DeviceTagIdCompletion deviceTagIdCompletion;
    private final @NotNull CrossAdapterCompletion crossAdapterCompletion;

    public CompletionProvider(final @NotNull WorkspaceIndex index) {
        this.tagNameCompletion = new TagNameCompletion(index);
        this.deviceTagIdCompletion = new DeviceTagIdCompletion(index);
        this.crossAdapterCompletion = new CrossAdapterCompletion(index);
    }

    /**
     * Returns completion items for the given document context and file.
     *
     * @param documentText full text of the open document
     * @param line         0-based cursor line
     * @param character    0-based cursor character
     * @param currentFile  absolute path to the file being edited
     * @return list of completion items (may be empty)
     */
    public @NotNull List<CompletionItem> complete(
            final @NotNull String documentText, final int line, final int character, final @NotNull Path currentFile) {

        final DocumentContext ctx = DocumentContext.analyze(documentText, line, character);

        return switch (ctx.target()) {
            case TYPE -> adapterTypeCompletion.complete();
            case TAG_NAME -> tagNameCompletion.complete(currentFile);
            case DEVICE_TAG_ID -> deviceTagIdCompletion.complete(currentFile);
            case CROSS_ADAPTER_TAG -> crossAdapterCompletion.completeAll(currentFile);
            case CROSS_ADAPTER_TAG_NAME -> crossAdapterCompletion.completeTagNames(currentFile, ctx.adapterIdPrefix());
            case ADAPTER_ID -> adapterIdItems(currentFile);
            case UNKNOWN -> List.of();
        };
    }

    private static @NotNull List<CompletionItem> adapterIdItems(final @NotNull Path currentFile) {
        final Path dir = currentFile.getParent();
        if (dir == null) return List.of();
        final CompletionItem item = new CompletionItem(dir.getFileName().toString());
        item.setKind(CompletionItemKind.Value);
        item.setDetail("Adapter ID (directory name)");
        return List.of(item);
    }
}
