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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Result of line-based cursor position analysis. Tells the {@link CompletionProvider} which key is being
 * completed and what the file type is.
 *
 * <p>No full YAML parse at cursor position — just string inspection of the current line and a
 * coarse scan of the document for type-detection keys.
 */
public record DocumentContext(
        @NotNull FileType fileType,
        @NotNull CompletionTarget target,
        /** Adapter ID prefix for cross-adapter completion after {@code "adapterId::"}. */
        @Nullable String adapterIdPrefix) {

    /** Coarse file type inferred from top-level YAML keys present in the document. */
    public enum FileType {
        /** File contains a top-level {@code type:} key — adapter manifest. */
        ADAPTER,
        /** File contains a top-level {@code dataCombiners:} key — combiner file. */
        COMBINER,
        /** Everything else (tag files, device-tag files, northbound mappings). */
        OTHER,
    }

    /** Which YAML key value is being completed. */
    public enum CompletionTarget {
        /** {@code type: |} — adapter type value (opcua / bacnetip). */
        TYPE,
        /** {@code tagName: |} — a tag name within the same adapter directory. */
        TAG_NAME,
        /** {@code deviceTagId: |} — a device tag ID within the same adapter directory. */
        DEVICE_TAG_ID,
        /** {@code tag: |} — cross-adapter reference in the form {@code adapterId::tagName}. */
        CROSS_ADAPTER_TAG,
        /**
         * {@code tag: adapterId::|} — tag name within a specific adapter (after the {@code ::}
         * separator). The adapter ID is available via {@link DocumentContext#adapterIdPrefix()}.
         */
        CROSS_ADAPTER_TAG_NAME,
        /** {@code id: |} — adapter ID (suggested from the directory name). */
        ADAPTER_ID,
        /** Cursor is not in a context we support. */
        UNKNOWN,
    }

    /**
     * Analyses the current cursor position (0-based line/character) against the full document text and
     * returns the inferred context.
     *
     * @param documentText full text of the open document
     * @param line         0-based line number of the cursor
     * @param character    0-based character offset within that line
     */
    public static @NotNull DocumentContext analyze(
            final @NotNull String documentText, final int line, final int character) {

        final FileType fileType = detectFileType(documentText);

        final String[] lines = documentText.split("\n", -1);
        if (line >= lines.length) {
            return new DocumentContext(fileType, CompletionTarget.UNKNOWN, null);
        }

        // Text up to cursor on the current line
        final String rawLine = lines[line];
        final String cursorLine = rawLine.substring(0, Math.min(character, rawLine.length()));
        // Strip leading whitespace, then strip the YAML sequence marker ("- ") if present,
        // so that keys appearing as the first key in a list item are still matched.
        String trimmed = cursorLine.stripLeading();
        if (trimmed.startsWith("- ")) {
            trimmed = trimmed.substring(2).stripLeading();
        }

        if (trimmed.startsWith("type:")) {
            return new DocumentContext(fileType, CompletionTarget.TYPE, null);
        }
        if (trimmed.startsWith("tagName:")) {
            return new DocumentContext(fileType, CompletionTarget.TAG_NAME, null);
        }
        if (trimmed.startsWith("deviceTagId:")) {
            return new DocumentContext(fileType, CompletionTarget.DEVICE_TAG_ID, null);
        }
        if (trimmed.startsWith("tag:")) {
            // Check for adapterId:: prefix after the colon
            final String afterColon = trimmed.substring("tag:".length()).stripLeading();
            final int sepIdx = afterColon.indexOf("::");
            if (sepIdx >= 0) {
                final String adapterPrefix = afterColon.substring(0, sepIdx);
                return new DocumentContext(fileType, CompletionTarget.CROSS_ADAPTER_TAG_NAME, adapterPrefix);
            }
            return new DocumentContext(fileType, CompletionTarget.CROSS_ADAPTER_TAG, null);
        }
        if (trimmed.startsWith("id:")) {
            return new DocumentContext(fileType, CompletionTarget.ADAPTER_ID, null);
        }

        return new DocumentContext(fileType, CompletionTarget.UNKNOWN, null);
    }

    private static @NotNull FileType detectFileType(final @NotNull String text) {
        // Check for top-level dataCombiners key — this is a combiner file
        if (text.contains("dataCombiners:")) {
            return FileType.COMBINER;
        }
        // Check for top-level type: key at start of line — adapter manifest
        for (final String line : text.split("\n", -1)) {
            final String stripped = line.stripLeading();
            if (stripped.startsWith("type:") && !line.startsWith(" ") && !line.startsWith("\t")) {
                return FileType.ADAPTER;
            }
        }
        return FileType.OTHER;
    }
}
