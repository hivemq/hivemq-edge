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
package com.hivemq.api.model.topicFilters;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.persistence.topicfilter.TopicFilter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

@Schema(name = "TopicFilter")
public class TopicFilterModel {

    @JsonProperty("topicFilter")
    @Schema(format = "mqtt-topic-filter",
            description = "The topic filter according to the MQTT specification.",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private final @NotNull String topicFilter;

    @JsonProperty("description")
    @Schema(description = "The name for this topic filter.")
    private final @NotNull String description;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public TopicFilterModel(
            @JsonProperty("topicFilter") final @NotNull String topicFilter,
            @JsonProperty("description") final @Nullable String description) {
        this.description = Objects.requireNonNullElse(description, "");
        this.topicFilter = topicFilter;
    }

    public @NotNull String getDescription() {
        return description;
    }

    public @NotNull String getTopicFilter() {
        return topicFilter;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final TopicFilterModel that = (TopicFilterModel) o;
        return description.equals(that.description) && topicFilter.equals(that.topicFilter);
    }

    @Override
    public int hashCode() {
        int result = description.hashCode();
        result = 31 * result + topicFilter.hashCode();
        return result;
    }

    @Override
    public @NotNull String toString() {
        return "TopicFilterModel{" +
                "description='" +
                description +
                '\'' +
                ", topicFilter='" +
                topicFilter +
                '\'' +
                '}';
    }

    public static @NotNull TopicFilterModel fromTopicFilter(final @NotNull TopicFilter topicFilter) {
        return new TopicFilterModel(topicFilter.getTopicFilter(), topicFilter.getDescription());
    }
}
