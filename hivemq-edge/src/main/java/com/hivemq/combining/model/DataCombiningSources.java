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
package com.hivemq.combining.model;

import com.hivemq.configuration.entity.combining.DataCombiningSourcesEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public record DataCombiningSources(List<String> tags, List<String> topicFilters) {

    public static @NotNull DataCombiningSources fromModel(final @NotNull com.hivemq.edge.api.model.DataCombiningSources model) {
        return new DataCombiningSources(model.getTags(), model.getTopicFilters());
    }

    public @NotNull com.hivemq.edge.api.model.DataCombiningSources toModel() {
        return new com.hivemq.edge.api.model.DataCombiningSources().tags(tags).topicFilters(topicFilters);
    }


    public static @NotNull DataCombiningSources fromPersistence(final @NotNull DataCombiningSourcesEntity persistenceModel) {
        return new DataCombiningSources(persistenceModel.getTags(), persistenceModel.getTopicFilters());
    }

    public @NotNull DataCombiningSourcesEntity toPersistence() {
        return new DataCombiningSourcesEntity(tags, topicFilters);
    }

}

