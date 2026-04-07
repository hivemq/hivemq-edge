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

/** Provides completions for {@code tagName:} fields — all tag names in the same adapter directory. */
public class TagNameCompletion {

    private final @NotNull WorkspaceIndex index;

    public TagNameCompletion(final @NotNull WorkspaceIndex index) {
        this.index = index;
    }

    public @NotNull List<CompletionItem> complete(final @NotNull Path currentFile) {
        return index.tagNamesForFile(currentFile).stream()
                .map(TagNameCompletion::item)
                .toList();
    }

    private static @NotNull CompletionItem item(final @NotNull String tagName) {
        final CompletionItem item = new CompletionItem(tagName);
        item.setKind(CompletionItemKind.Value);
        item.setDetail("Tag name");
        return item;
    }
}
