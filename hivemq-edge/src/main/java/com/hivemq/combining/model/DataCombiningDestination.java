package com.hivemq.combining.model;

import com.hivemq.configuration.entity.combining.DataCombiningDestinationEntity;
import org.jetbrains.annotations.NotNull;

public record DataCombiningDestination(String topic, String schema) {


    public static DataCombiningDestination from(final @NotNull com.hivemq.edge.api.model.DataCombiningDestination destination) {
        return new DataCombiningDestination(destination.getTopic(), destination.getSchema());
    }

    public @NotNull com.hivemq.edge.api.model.DataCombiningDestination toModel() {
        return new com.hivemq.edge.api.model.DataCombiningDestination().topic(topic()).schema(schema());
    }

    public static DataCombiningDestination fromPersistence(final @NotNull DataCombiningDestinationEntity entity) {
        return new DataCombiningDestination(entity.getTopic(), entity.getSchema());
    }

    public @NotNull DataCombiningDestinationEntity toPersistence() {
        return new DataCombiningDestinationEntity(topic(), schema());
    }
}
