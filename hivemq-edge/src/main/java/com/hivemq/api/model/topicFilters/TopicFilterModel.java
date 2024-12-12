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
import com.hivemq.persistence.topicfilter.TopicFilter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@Schema(name = "TopicFilter")
public class TopicFilterModel {

    @JsonProperty("filter")
    @Schema(format = "mqtt-topic-filter",
            description = "The topic filter according to the MQTT specification.",
            minLength = 1,
            maxLength = 65_535,
            requiredMode = Schema.RequiredMode.REQUIRED)
    private final @NotNull String topicFilter;

    @JsonProperty("description")
    @Schema(description = "The name for this topic filter.",
            maxLength = 65_535)
    private final @NotNull String description;

    @JsonProperty("schema")
    @Schema(description = "The optional json schema for this topic filter in the data uri format.",
            format = "data-url",
            nullable = true)
    private final @Nullable String schema;


    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public TopicFilterModel(
            @JsonProperty(value = "filter", required = true) final @NotNull String topicFilter,
            @JsonProperty(value = "description") final @Nullable String description,
            @JsonProperty("schema") final @Nullable String schema) {
        this.description = Objects.requireNonNullElse(description, "");
        this.topicFilter = topicFilter;
        this.schema = schema;
    }

    public @NotNull String getDescription() {
        return description;
    }

    public @NotNull String getTopicFilter() {
        return topicFilter;
    }

    public @Nullable String getSchema() {
        return schema;
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
        return topicFilter.equals(that.topicFilter) &&
                description.equals(that.description) &&
                Objects.equals(schema, that.schema);
    }

    @Override
    public int hashCode() {
        int result = topicFilter.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + Objects.hashCode(schema);
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
                ", schema='" +
                schema +
                '\'' +
                '}';
    }

    public static @NotNull TopicFilterModel fromTopicFilter(final @NotNull TopicFilter topicFilter) {
        return new TopicFilterModel(topicFilter.getTopicFilter(),
                topicFilter.getDescription(),
                topicFilter.getSchema() != null ? topicFilter.getSchema().toString() : null);
    }
}
