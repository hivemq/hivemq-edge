package com.hivemq.combining;

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

