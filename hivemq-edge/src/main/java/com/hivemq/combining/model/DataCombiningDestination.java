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
