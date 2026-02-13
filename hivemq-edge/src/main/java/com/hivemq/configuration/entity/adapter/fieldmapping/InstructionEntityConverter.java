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
package com.hivemq.configuration.entity.adapter.fieldmapping;

import com.hivemq.configuration.entity.EntityConverter;
import com.hivemq.configuration.entity.combining.DataIdentifierReferenceEntityConverter;
import com.hivemq.edge.api.model.Instruction;
import org.jetbrains.annotations.NotNull;

public final class InstructionEntityConverter implements EntityConverter<Instruction, InstructionEntity> {
    public static final InstructionEntityConverter INSTANCE = new InstructionEntityConverter(true);
    public static final InstructionEntityConverter INSTANCE_WITHOUT_SOURCE_REF = new InstructionEntityConverter(false);

    private final boolean includeSourceRef;

    private InstructionEntityConverter(final boolean includeSourceRef) {
        this.includeSourceRef = includeSourceRef;
    }

    public boolean isIncludeSourceRef() {
        return includeSourceRef;
    }

    @Override
    public @NotNull InstructionEntity toInternalEntity(final @NotNull Instruction entity) {
        return new InstructionEntity(
                entity.getSource(),
                entity.getDestination(),
                includeSourceRef && entity.getSourceRef() != null
                        ? DataIdentifierReferenceEntityConverter.INSTANCE.toInternalEntity(entity.getSourceRef())
                        : null);
    }

    @Override
    public @NotNull Instruction toRestEntity(final @NotNull InstructionEntity entity) {
        return Instruction.builder()
                .destination(entity.getDestinationFieldName())
                .source(entity.getSourceFieldName())
                .sourceRef(
                        includeSourceRef && entity.getOrigin() != null
                                ? DataIdentifierReferenceEntityConverter.INSTANCE.toRestEntity(entity.getOrigin())
                                : null)
                .build();
    }
}
