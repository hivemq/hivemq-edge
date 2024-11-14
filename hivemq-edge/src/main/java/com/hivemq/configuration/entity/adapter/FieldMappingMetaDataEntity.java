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
package com.hivemq.configuration.entity.adapter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.fieldmapping.FieldMappingMetaData;

public class FieldMappingMetaDataEntity {

    @JsonProperty("source-schema")
    private final @NotNull String sourceJsonSchema;

    @JsonProperty("destination-schema")
    private final @NotNull String destinationJsonSchema;

    @JsonCreator
    public FieldMappingMetaDataEntity(
            @JsonProperty("source-schema") final @NotNull String sourceJsonSchema,
            @JsonProperty("destination-schema") final @NotNull String destinationJsonSchema) {
        this.sourceJsonSchema = sourceJsonSchema;
        this.destinationJsonSchema = destinationJsonSchema;
    }

    public static @NotNull FieldMappingMetaDataEntity from(
            final @NotNull FieldMappingMetaData model) {
        return new FieldMappingMetaDataEntity(model.getSourceJsonSchema().toString(),
                model.getDestinationJsonSchema().toString());
    }

    public @NotNull String getDestinationJsonSchema() {
        return destinationJsonSchema;
    }

    public @NotNull String getSourceJsonSchema() {
        return sourceJsonSchema;
    }
}