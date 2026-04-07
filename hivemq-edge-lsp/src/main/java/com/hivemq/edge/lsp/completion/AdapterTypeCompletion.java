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

import java.util.List;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.jetbrains.annotations.NotNull;

/** Provides completions for the {@code type:} field in adapter manifests. */
public class AdapterTypeCompletion {

    private static final List<String> KNOWN_TYPES = List.of("bacnetip", "opcua");

    public @NotNull List<CompletionItem> complete() {
        return KNOWN_TYPES.stream().map(AdapterTypeCompletion::item).toList();
    }

    private static @NotNull CompletionItem item(final @NotNull String type) {
        final CompletionItem item = new CompletionItem(type);
        item.setKind(CompletionItemKind.EnumMember);
        item.setDetail("Adapter type");
        return item;
    }
}
