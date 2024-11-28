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
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.adapter.sdk.api.mappings.toedge.ToEdgeMapping;

import javax.xml.bind.ValidationEvent;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.helpers.ValidationEventImpl;
import java.util.List;

@SuppressWarnings("unused")
public class ToEdgeMappingEntity {

    @XmlElement(name = "topicFilter", required = true)
    private final @NotNull String topicFilter;

    @XmlElement(name = "tagName", required = true)
    private final @NotNull String tagName;

    @XmlElement(name = "maxQos", required = true)
    private final int qos;

    // no-arg constructor for JaxB
    public ToEdgeMappingEntity() {
        topicFilter = "";
        tagName = "";
        qos = 1;
    }

    public ToEdgeMappingEntity(
            final @NotNull String tagName,
            final @NotNull String topicFilter, final int maxQoS) {
        this.tagName = tagName;
        this.topicFilter = topicFilter;
        this.qos = maxQoS;
    }

    public @NotNull String getTagName() {
        return tagName;
    }

    public @NotNull String getTopicFilter() {
        return topicFilter;
    }

    public int getMaxQos() {
        return qos;
    }

    public void validate(final @NotNull List<ValidationEvent> validationEvents) {
        if (topicFilter == null || topicFilter.isEmpty()) {
            validationEvents.add(new ValidationEventImpl(ValidationEvent.FATAL_ERROR, "topicFilter is missing", null));
        }
        if (tagName == null || tagName.isEmpty()) {
            validationEvents.add(new ValidationEventImpl(ValidationEvent.FATAL_ERROR, "tagName is missing", null));
        }
    }


    public @NotNull ToEdgeMapping toToEdgeMapping() {
        return new ToEdgeMapping(
                this.getTagName(),
                this.getTopicFilter(),
                this.getMaxQos());
    }

    public static @NotNull ToEdgeMappingEntity from(final @NotNull ToEdgeMapping toEdgeMapping) {
        return new ToEdgeMappingEntity(
                toEdgeMapping.getTagName(),
                toEdgeMapping.getTopicFilter(),
                toEdgeMapping.getMaxQoS());
    }

}
