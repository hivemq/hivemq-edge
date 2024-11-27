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
package com.hivemq.persistence.fieldmapping;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.api.model.fieldmapping.FieldMappingMetaDataModel;
import com.hivemq.configuration.entity.adapter.FieldMappingMetaDataEntity;
import com.hivemq.extension.sdk.api.annotations.NotNull;

public class FieldMappingMetaData {

    private final @NotNull JsonNode sourceJsonSchema;
    private final @NotNull JsonNode destinationJsonSchema;


    @JsonCreator
    public FieldMappingMetaData(
            final @NotNull JsonNode sourceJsonSchema, final @NotNull JsonNode destinationJsonSchema) {
        this.sourceJsonSchema = sourceJsonSchema;
        this.destinationJsonSchema = destinationJsonSchema;
    }

    public static @NotNull FieldMappingMetaData fromModel(final @NotNull FieldMappingMetaDataModel model) {
        return new FieldMappingMetaData(model.getSourceJsonSchema(), model.getDestinationJsonSchema());
    }

    public static FieldMappingMetaData fromEntity(final @NotNull FieldMappingMetaDataEntity metaData, final @NotNull ObjectMapper objectMapper) {
        try {
            return new FieldMappingMetaData(objectMapper.readTree(metaData.getSourceJsonSchema()), objectMapper.readTree(metaData.getDestinationJsonSchema()));
        } catch (final JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public @NotNull JsonNode getDestinationJsonSchema() {
        return destinationJsonSchema;
    }

    public @NotNull JsonNode getSourceJsonSchema() {
        return sourceJsonSchema;
    }
}
