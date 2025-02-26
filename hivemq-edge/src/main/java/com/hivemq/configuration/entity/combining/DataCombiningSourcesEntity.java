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
package com.hivemq.configuration.entity.combining;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.ArrayList;
import java.util.List;


public class DataCombiningSourcesEntity {

    @JsonProperty("primaryReference")
    @XmlElement(name = "primary-reference")
    private final @NotNull DataIdentifierReferenceEntity primaryIdentifier;

    @JsonProperty("tags")
    @XmlElementWrapper(name = "tags")
    @XmlElement(name = "tag")
    private final @NotNull List<String> tags;

    @JsonProperty("topicFilters")
    @XmlElementWrapper(name = "topic-filters")
    @XmlElement(name = "topic-filter")
    private final @NotNull List<String> topicFilters;

    // no-arg for jaxb
    public DataCombiningSourcesEntity() {
        tags = new ArrayList<>();
        topicFilters = new ArrayList<>();
        primaryIdentifier = null;
    }

    public DataCombiningSourcesEntity(
            final @NotNull DataIdentifierReferenceEntity primaryIdentifier,
            final @NotNull List<String> tags,
            final @NotNull List<String> topicFilters) {
        this.primaryIdentifier = primaryIdentifier;
        this.tags = tags;
        this.topicFilters = topicFilters;
    }

    public @NotNull List<String> getTags() {
        return tags;
    }

    public @NotNull List<String> getTopicFilters() {
        return topicFilters;
    }

    public @NotNull DataIdentifierReferenceEntity getPrimaryIdentifier() {
        return primaryIdentifier;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DataCombiningSourcesEntity that = (DataCombiningSourcesEntity) o;
        return primaryIdentifier.equals(that.primaryIdentifier) &&
                tags.equals(that.tags) &&
                topicFilters.equals(that.topicFilters);
    }

    @Override
    public int hashCode() {
        int result = primaryIdentifier.hashCode();
        result = 31 * result + tags.hashCode();
        result = 31 * result + topicFilters.hashCode();
        return result;
    }
}

