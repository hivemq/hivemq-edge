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
package com.hivemq.configuration.entity.adapter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.fieldmapping.FieldMappings;

import javax.xml.bind.annotation.XmlElement;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class FieldMappingsEntity {

    @XmlElement(name = "topic-filter")
    private final @NotNull String topicFilter;

    @XmlElement(name = "tag-name")
    private final @NotNull String tagName;

    @XmlElement(name = "field-mappings")
    private final @NotNull List<FieldMappingEntity> fieldMappingEntities;

    @XmlElement(name = "metadata")
    private final @NotNull FieldMappingMetaDataEntity metaData;

    public FieldMappingsEntity(
            final @NotNull String topicFilter,
            final @NotNull String tagName,
            final @NotNull List<FieldMappingEntity> fieldMappingEntities,
            final @NotNull FieldMappingMetaDataEntity metaData) {
        this.topicFilter = topicFilter;
        this.tagName = tagName;
        this.fieldMappingEntities = fieldMappingEntities;
        this.metaData = metaData;
    }

    public static @NotNull FieldMappingsEntity from(final @NotNull FieldMappings model) {
        final List<FieldMappingEntity> fieldMappingEntityList =
                model.getFieldMappings().stream().map(FieldMappingEntity::from).collect(Collectors.toList());
        return new FieldMappingsEntity(model.getTopicFilter(),
                model.getTagName(),
                fieldMappingEntityList,
                FieldMappingMetaDataEntity.from(model.getMetaData()));
    }

    public @NotNull List<FieldMappingEntity> getFieldMappingModels() {
        return fieldMappingEntities;
    }

    public @NotNull FieldMappingMetaDataEntity getMetaData() {
        return metaData;
    }

    public @NotNull String getTagName() {
        return tagName;
    }

    public @NotNull String getTopicFilter() {
        return topicFilter;
    }
}
