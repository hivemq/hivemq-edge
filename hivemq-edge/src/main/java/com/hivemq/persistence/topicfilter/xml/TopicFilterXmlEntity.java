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

import com.hivemq.extension.sdk.api.annotations.NotNull;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.NONE)
public class TopicFilterXmlEntity {

    @XmlElement(name = "description", required = true)
    private final @NotNull String description;

    @XmlElement(name = "filter", required = true)
    private final @NotNull String topicFilter;


    //no-arg for JaxB
    @SuppressWarnings("unused")
    public TopicFilterXmlEntity() {
        this.description = "";
        this.topicFilter = "";
    }

    public TopicFilterXmlEntity(
            final @NotNull String topicFilter, final @NotNull String description) {
        this.description = description;
        this.topicFilter = topicFilter;
    }


    public @NotNull String getDescription() {
        return description;
    }

    public @NotNull String getTopicFilter() {
        return topicFilter;
    }

    @Override
    public @NotNull String toString() {
        return "TopicFilterXmlEntity{" + "description='" + description + '\'' + ", topicFilter='" + topicFilter + '\'' + '}';
    }
}
