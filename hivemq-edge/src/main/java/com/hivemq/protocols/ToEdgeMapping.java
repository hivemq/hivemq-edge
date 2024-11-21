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
package com.hivemq.protocols;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.configuration.entity.adapter.ToEdgeMappingEntity;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.fieldmapping.FieldMappings;

public class ToEdgeMapping {

    private final @NotNull String topicFilter;
    private final @NotNull String tagName;
    private final int maxQoS;
    private final @NotNull FieldMappings fieldMappings;


    public ToEdgeMapping(
            final @NotNull String tagName,
            final @NotNull String topicFilter,
            final int maxQoS,
            final @NotNull FieldMappings fieldMappings) {
        this.tagName = tagName;
        this.topicFilter = topicFilter;
        this.maxQoS = maxQoS;
        this.fieldMappings = fieldMappings;

    }

    public @NotNull String getTopicFilter() {
        return topicFilter;
    }

    public @NotNull String getTagName() {
        return tagName;
    }

    public @NotNull FieldMappings getFieldMappings() {
        return fieldMappings;
    }

    public static @NotNull ToEdgeMapping fromEntity(
            final @NotNull ToEdgeMappingEntity toEdgeMappingEntity, final @NotNull ObjectMapper objectMapper) {
        return new ToEdgeMapping(toEdgeMappingEntity.getTagName(),
                toEdgeMappingEntity.getTopicFilter(),
                toEdgeMappingEntity.getMaxQos(),
                FieldMappings.fromEntity(toEdgeMappingEntity.getFieldMappingsEntity(), objectMapper));
    }

    public int getMaxQoS() {
        return maxQoS;
    }
}
