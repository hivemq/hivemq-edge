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
import com.hivemq.combining.model.PrimaryType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.List;
import java.util.Objects;


public class DataCombiningSourcesEntity {

    @JsonProperty("primaryName")
    @XmlElement(name = "primaryName")
    private final @NotNull String primaryName;

    @JsonProperty("primaryType")
    @XmlElement(name = "primaryType")
    private final @NotNull PrimaryType primaryType;

    @JsonProperty("tags")
    @XmlElementWrapper(name = "tags")
    @XmlElement(name = "tag")
    private final @NotNull List<String> tags;

    @JsonProperty("topicFilters")
    @XmlElementWrapper(name = "topicFilters")
    @XmlElement(name = "topicFilter")
    private final @NotNull List<String> topicFilters;

    // no-arg for jaxb
    public DataCombiningSourcesEntity() {
        tags = List.of();
        topicFilters = List.of();
        primaryName = "";
        primaryType = PrimaryType.TAG;
    }

    public DataCombiningSourcesEntity(
            final @NotNull String primaryName,
            final @NotNull PrimaryType primaryType,
            final @NotNull List<String> tags,
            final @NotNull List<String> topicFilters) {
        this.primaryName = primaryName;
        this.primaryType = primaryType;
        this.tags = tags;
        this.topicFilters = topicFilters;
    }

    public @NotNull List<String> getTags() {
        return tags;
    }

    public @NotNull List<String> getTopicFilters() {
        return topicFilters;
    }

    public @NotNull String getPrimaryName() {
        return primaryName;
    }

    public @NotNull PrimaryType getPrimaryType() {
        return primaryType;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final DataCombiningSourcesEntity that = (DataCombiningSourcesEntity) o;
        return Objects.equals(getPrimaryName(), that.getPrimaryName()) &&
                getPrimaryType() == that.getPrimaryType() &&
                Objects.equals(getTags(), that.getTags()) &&
                Objects.equals(getTopicFilters(), that.getTopicFilters());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPrimaryName(), getPrimaryType(), getTags(), getTopicFilters());
    }
}

