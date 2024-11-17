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
package com.hivemq.api.model.mapping;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.fieldmapping.FieldMappingMetaData;
import io.swagger.v3.oas.annotations.media.Schema;

public class FieldMappingMetaDataModel {


    @JsonProperty("source")
    @Schema(description = "The json schema validating the incoming data for the transformation",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private final @NotNull JsonNode sourceJsonSchema;


    @JsonProperty("destination")
    @Schema(description = "The json schema validating the outgoing data for the transformation",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private final @NotNull JsonNode destinationJsonSchema;


    @JsonCreator
    public FieldMappingMetaDataModel(
            @JsonProperty("source") final @NotNull JsonNode sourceJsonSchema,
            @JsonProperty("destination") final @NotNull JsonNode destinationJsonSchema) {
        this.sourceJsonSchema = sourceJsonSchema;
        this.destinationJsonSchema = destinationJsonSchema;
    }

    public static FieldMappingMetaDataModel from(final @NotNull FieldMappingMetaData metaData) {
        return new FieldMappingMetaDataModel(metaData.getSourceJsonSchema(), metaData.getDestinationJsonSchema());
    }

    public @NotNull JsonNode getDestinationJsonSchema() {
        return destinationJsonSchema;
    }

    public @NotNull JsonNode getSourceJsonSchema() {
        return sourceJsonSchema;
    }
}
