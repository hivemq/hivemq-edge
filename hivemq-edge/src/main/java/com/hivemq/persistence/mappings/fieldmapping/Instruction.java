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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;


public record Instruction(@NotNull String sourceFieldName, @NotNull String destinationFieldName,
                          @Nullable DataIdentifierReference dataIdentifierReference) {

    public static Instruction from(final @NotNull com.hivemq.edge.api.model.Instruction model) {
        return new Instruction(model.getSource(),
                model.getDestination(),
                DataIdentifierReference.from(model.getSourceRef()));
    }

    public @NotNull com.hivemq.edge.api.model.Instruction toModel() {
        final com.hivemq.edge.api.model.Instruction instruction =
                new com.hivemq.edge.api.model.Instruction().source(sourceFieldName).destination(destinationFieldName);
        if (dataIdentifierReference() != null) {
            instruction.sourceRef(dataIdentifierReference().to());
        }
        return instruction;
    }

    public @NotNull String toSourceJsonPath() {
        // TODO Proper escaping is required.
        final Optional<DataIdentifierReference> optionalDataIdentifierReference =
                Optional.ofNullable(dataIdentifierReference());
        final String rootFieldName =
                optionalDataIdentifierReference.map(DataIdentifierReference::type).map(Enum::name).orElse("") +
                        ":" +
                        optionalDataIdentifierReference.map(DataIdentifierReference::id).orElse("");
        final String sourceFieldName = sourceFieldName().trim();
        if (sourceFieldName.startsWith("$.")) {
            return "$['" + rootFieldName + "']" + sourceFieldName.substring(1);
        } else if (sourceFieldName.startsWith("$")) {
            return "$['" + rootFieldName + "']." + sourceFieldName.substring(1);
        } else {
            return "$['" + rootFieldName + "']." + sourceFieldName;
        }
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
}
