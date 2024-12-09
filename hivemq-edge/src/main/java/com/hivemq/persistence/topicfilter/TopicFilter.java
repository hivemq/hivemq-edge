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
package com.hivemq.persistence.topicfilter;

import com.hivemq.api.format.DataUrl;
import com.hivemq.api.model.topicFilters.TopicFilterModel;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Immutable
public class TopicFilter {

    private final @NotNull String description;
    private final @NotNull String topicFilter;
    private final @Nullable DataUrl schema;


    public TopicFilter(
            final @NotNull String topicFilter, final @NotNull String description, final @Nullable DataUrl schema) {
        this.description = description;
        this.topicFilter = topicFilter;
        this.schema = schema;
    }

    public static @NotNull TopicFilter fromTopicFilterModel(
            final @NotNull TopicFilterModel topicFilter) {
        return new TopicFilter(topicFilter.getTopicFilter(),
                topicFilter.getDescription(),
                topicFilter.getSchema() != null ? DataUrl.create(topicFilter.getSchema()) : null);
    }

    public @NotNull String getDescription() {
        return description;
    }

    public @NotNull String getTopicFilter() {
        return topicFilter;
    }

    public @Nullable DataUrl getSchema() {
        return schema;
    }

    // IMPORTANT: Only use filter for equals and hashcode as we do not care about the description and these methods are used for removal
    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final TopicFilter that = (TopicFilter) o;
        return topicFilter.equals(that.topicFilter);
    }

    @Override
    public int hashCode() {
        return topicFilter.hashCode();
    }

    @Override
    public @NotNull String toString() {
        return "TopicFilter{" + "name='" + description + '\'' + ", topicFilter='" + topicFilter + '\'' + '}';
    }
}
