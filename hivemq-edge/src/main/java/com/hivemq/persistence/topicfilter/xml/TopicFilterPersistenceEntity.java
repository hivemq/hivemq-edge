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
package com.hivemq.persistence.topicfilter.xml;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "topic-filters-persistence")
@XmlAccessorType(XmlAccessType.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TopicFilterPersistenceEntity {

    @SuppressWarnings("unused")
    @JacksonXmlProperty(isAttribute = true, localName = "xmlns:xsi")
    private final @NotNull String nameSpace = "http://www.w3.org/2001/XMLSchema-instance";

    @SuppressWarnings("unused")
    @JacksonXmlProperty(isAttribute = true, localName = "xsi:noNamespaceSchemaLocation")
    private final @NotNull String schemaLocation = "topic-filters.xsd";

    @XmlElementWrapper(name = "topicFilters", required = true)
    @XmlElement(name = "topicFilter", required = true)
    @XmlElementRef(required = false)
    private @NotNull List<TopicFilterXmlEntity> tags;

    // JaxB needs a default constructor
    public TopicFilterPersistenceEntity() {
        this.tags = new ArrayList<>();
    }

    public TopicFilterPersistenceEntity(final @NotNull List<TopicFilterXmlEntity> tagsAsEntities) {
        this.tags = tagsAsEntities;
    }

    @JsonIgnore
    public @NotNull List<TopicFilterXmlEntity> getTopicFilters() {
        return tags;
    }

    @Override
    public @NotNull String toString() {
        return "TopicFilterPersistenceEntity{" + "tags=" + tags + '}';
    }
}
