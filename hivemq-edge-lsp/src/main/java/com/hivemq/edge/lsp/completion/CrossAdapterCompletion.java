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
import org.jetbrains.annotations.Nullable;

/**
 * Provides cross-adapter completions for {@code tag:} fields in combiner files.
 *
 * <ul>
 *   <li>Without prefix: all {@code adapterId::tagName} pairs for the current instance.
 *   <li>With prefix (after {@code adapterId::}): just the tag names for that adapter.
 * </ul>
 */
public class CrossAdapterCompletion {

    private final @NotNull WorkspaceIndex index;

    public CrossAdapterCompletion(final @NotNull WorkspaceIndex index) {
        this.index = index;
    }

    /** Returns all {@code adapterId::tagName} pairs visible from the given file's instance. */
    public @NotNull List<CompletionItem> completeAll(final @NotNull Path currentFile) {
        return index.crossAdapterRefsForFile(currentFile).stream()
                .map(CrossAdapterCompletion::refItem)
                .toList();
    }

    /**
     * Returns only tag names for a specific adapter (used after the user has typed {@code adapterId::}).
     *
     * @param currentFile the file being edited (used to determine the instance)
     * @param adapterId   the adapter ID the user typed before {@code ::}
     */
    public @NotNull List<CompletionItem> completeTagNames(
            final @NotNull Path currentFile, final @Nullable String adapterId) {
        if (adapterId == null || adapterId.isBlank()) {
            return List.of();
        }
        return index.tagNamesForAdapter(currentFile, adapterId).stream()
                .map(CrossAdapterCompletion::tagNameItem)
                .toList();
    }

    private static @NotNull CompletionItem refItem(final @NotNull String ref) {
        final CompletionItem item = new CompletionItem(ref);
        item.setKind(CompletionItemKind.Reference);
        item.setDetail("Cross-adapter tag reference");
        return item;
    }

    private static @NotNull CompletionItem tagNameItem(final @NotNull String tagName) {
        final CompletionItem item = new CompletionItem(tagName);
        item.setKind(CompletionItemKind.Value);
        item.setDetail("Tag name");
        return item;
    }
}
