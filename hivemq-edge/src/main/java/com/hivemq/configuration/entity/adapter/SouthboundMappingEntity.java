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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.configuration.entity.adapter.fieldmapping.FieldMappingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.hivemq.persistence.mappings.SouthboundMapping;

import javax.xml.bind.ValidationEvent;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.helpers.ValidationEventImpl;
import java.util.List;

@SuppressWarnings("unused")
public class SouthboundMappingEntity {

    @XmlElement(name = "topicFilter", required = true)
    private final @NotNull String topicFilter;

    @XmlElement(name = "tagName", required = true)
    private final @NotNull String tagName;

    @XmlElement(name = "fieldMapping")
    private final @Nullable FieldMappingEntity fieldMapping;

    // no-arg constructor for JaxB
    public SouthboundMappingEntity() {
        topicFilter = "";
        tagName = "";
        fieldMapping = null;
    }

    public SouthboundMappingEntity(
            final @NotNull String tagName,
            final @NotNull String topicFilter,
            final @Nullable FieldMappingEntity fieldMapping) {
        this.tagName = tagName;
        this.topicFilter = topicFilter;
        this.fieldMapping = fieldMapping;
    }

    public @NotNull String getTagName() {
        return tagName;
    }

    public @NotNull String getTopicFilter() {
        return topicFilter;
    }

    public void validate(final @NotNull List<ValidationEvent> validationEvents) {
        if (topicFilter == null || topicFilter.isEmpty()) {
            validationEvents.add(new ValidationEventImpl(ValidationEvent.FATAL_ERROR, "topicFilter is missing", null));
        }
        if (tagName == null || tagName.isEmpty()) {
            validationEvents.add(new ValidationEventImpl(ValidationEvent.FATAL_ERROR, "tagName is missing", null));
        }
    }


    public @NotNull SouthboundMapping to(final @NotNull ObjectMapper mapper) {
        return new SouthboundMapping(
                this.getTagName(),
                this.getTopicFilter(),
                this.fieldMapping != null ? this.fieldMapping.to(mapper) : null);
    }

    public static @NotNull SouthboundMappingEntity from(final @NotNull SouthboundMapping southboundMapping) {
        return new SouthboundMappingEntity(
                southboundMapping.getTagName(),
                southboundMapping.getTopicFilter(),
                FieldMappingEntity.from(southboundMapping.getFieldMapping())
        );
    }

}
