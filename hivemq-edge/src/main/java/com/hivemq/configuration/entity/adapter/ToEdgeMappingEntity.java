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
import com.hivemq.protocols.ToEdgeMapping;

import javax.xml.bind.annotation.XmlElement;

@SuppressWarnings("unused")
public class ToEdgeMappingEntity {

    @XmlElement(name = "topic-filter", required = true)
    private final @NotNull String topicFilter;

    @XmlElement(name = "tag-name", required = true)
    private final @NotNull String tagName;

    @XmlElement(name = "max-qos", required = true)
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

    public static @NotNull ToEdgeMappingEntity from(final @NotNull ToEdgeMapping toEdgeMapping) {
        return new ToEdgeMappingEntity(toEdgeMapping.getTagName(),
                toEdgeMapping.getTopicFilter(), toEdgeMapping.getMaxQoS());
    }
}
