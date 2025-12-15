/*
 *  Copyright 2019-present HiveMQ GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.hivemq.configuration.entity.adapter;

import com.hivemq.configuration.entity.EntityConverter;
import com.hivemq.configuration.entity.adapter.fieldmapping.FieldMappingEntityConverter;
import com.hivemq.edge.api.model.SouthboundMapping;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;

public final class SouthboundMappingEntityConverter
        implements EntityConverter<SouthboundMapping, SouthboundMappingEntity> {
    public static final SouthboundMappingEntityConverter INSTANCE = new SouthboundMappingEntityConverter();

    private SouthboundMappingEntityConverter() {
    }

    @Override
    public @NotNull SouthboundMappingEntity toInternalEntity(final @NotNull SouthboundMapping entity) {
        throw new NotImplementedException("SouthboundMapping to SouthboundMappingEntity conversion is not implemented");
    }

    @Override
    public @NotNull SouthboundMapping toRestEntity(final @NotNull SouthboundMappingEntity entity) {
        return SouthboundMapping.builder()
                .tagName(entity.getTagName())
                .topicFilter(entity.getTopicFilter())
                .fieldMapping(entity.getFieldMapping() == null ?
                        null :
                        FieldMappingEntityConverter.INSTANCE_WITHOUT_SOURCE_REF.toRestEntity(entity.getFieldMapping()))
                .build();
    }
}
