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
package com.hivemq.persistence.mappings.fieldmapping;

import com.hivemq.combining.model.DataIdentifierReference;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record Instruction(
        @NotNull String sourceFieldName,
        @NotNull String destinationFieldName,
        @Nullable DataIdentifierReference dataIdentifierReference) {

    public static Instruction from(final @NotNull com.hivemq.edge.api.model.Instruction model) {
        return new Instruction(
                model.getSource(), model.getDestination(), DataIdentifierReference.from(model.getSourceRef()));
    }

    public @NotNull com.hivemq.edge.api.model.Instruction toModel() {
        final com.hivemq.edge.api.model.Instruction instruction = new com.hivemq.edge.api.model.Instruction()
                .source(sourceFieldName)
                .destination(destinationFieldName);
        if (dataIdentifierReference() != null) {
            instruction.sourceRef(dataIdentifierReference().to());
        }
        return instruction;
    }

    /**
     * To source json path string.
     * <p>
     * It supports both dot and bracket notation, e.g. $.store.book[0].title and $.store['book'][0]['title']
     *
     * @return the json path
     */
    public @NotNull String toSourceJsonPath() {
        final String sourceFieldName = sourceFieldName().trim();
        return Optional.ofNullable(dataIdentifierReference())
                .filter(r -> Objects.nonNull(r.type()))
                .filter(r -> Objects.nonNull(r.id()))
                .map(DataIdentifierReference::toFullyQualifiedName)
                // We need to escape the root field name, because it can contain single quotes.
                .map(Instruction::escapeSingleQuote)
                .map(fieldName -> "$['" + fieldName + "'].")
                .map(prefix -> {
                    if (sourceFieldName.startsWith("$.")) {
                        return prefix + sourceFieldName.substring(2);
                    } else if (sourceFieldName.startsWith("$")) {
                        return prefix + sourceFieldName.substring(1);
                    } else {
                        return prefix + sourceFieldName;
                    }
                })
                .map(path -> {
                    if (path.endsWith(".")) {
                        return path.substring(0, path.length() - 1);
                    }
                    return path;
                })
                .orElse(sourceFieldName);
    }

    public @NotNull String toDestinationJsonPath() {
        final String destinationJsonPath = destinationFieldName();
        if (destinationJsonPath.startsWith("$.")) {
            return destinationJsonPath.substring(2);
        } else if (destinationJsonPath.startsWith("$")) {
            return destinationJsonPath.substring(1);
        }
        return destinationJsonPath;
    }

    /**
     * Escapes single quotes in a string. Already escaped single quotes (\') are ignored.
     *
     * @param input the input string
     * @return the string with unescaped single quotes escaped
     */
    static @NotNull String escapeSingleQuote(final @NotNull String input) {
        final StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            if (c == '\'') {
                // Check if this single quote is already escaped
                if (i > 0 && input.charAt(i - 1) == '\\') {
                    // Already escaped, just append the quote (backslash was already added)
                    result.append(c);
                } else {
                    // Not escaped, escape it
                    result.append("\\'");
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
