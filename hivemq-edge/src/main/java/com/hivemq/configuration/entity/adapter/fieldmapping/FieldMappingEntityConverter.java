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
import com.hivemq.edge.api.model.FieldMapping;
import org.jetbrains.annotations.NotNull;

public final class FieldMappingEntityConverter implements EntityConverter<FieldMapping, FieldMappingEntity> {
    public static final FieldMappingEntityConverter INSTANCE = new FieldMappingEntityConverter(true);
    public static final FieldMappingEntityConverter INSTANCE_WITHOUT_SOURCE_REF =
            new FieldMappingEntityConverter(false);
    private final boolean includeSourceRef;

    private FieldMappingEntityConverter(final boolean includeSourceRef) {
        this.includeSourceRef = includeSourceRef;
    }

    public boolean isIncludeSourceRef() {
        return includeSourceRef;
    }

    private @NotNull InstructionEntityConverter getInstructionEntityConverter() {
        return includeSourceRef
                ? InstructionEntityConverter.INSTANCE
                : InstructionEntityConverter.INSTANCE_WITHOUT_SOURCE_REF;
    }

    @Override
    public @NotNull FieldMappingEntity toInternalEntity(final @NotNull FieldMapping entity) {
        return new FieldMappingEntity(getInstructionEntityConverter().toInternalEntities(entity.getInstructions()));
    }

    @Override
    public @NotNull FieldMapping toRestEntity(final @NotNull FieldMappingEntity entity) {
        return FieldMapping.builder()
                .instructions(getInstructionEntityConverter().toRestEntities(entity.getInstructions()))
                .build();
    }
}
