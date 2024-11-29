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
package com.hivemq.api.model.mappings.fieldmapping;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.persistence.mappings.fieldmapping.Metadata;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;

@Schema(name = "Metadata")
public class MetadataModel {

    @JsonProperty(value = "destination", required = true)
    @Schema(name = "destination", description = "The schema used for the write target")
    private final @NotNull JsonNode destinationJsonSchema;

    @JsonProperty(value = "source", required = true)
    @Schema(name = "source", description = "The schema used for the incoming data")
    private final @NotNull JsonNode sourceJsonSchema;

    @JsonCreator
    public MetadataModel(
            @JsonProperty(value = "destination", required = true) @NotNull final JsonNode destinationJsonSchema,
            @JsonProperty(value = "source", required = true)@NotNull final JsonNode sourceJsonSchema) {
        this.destinationJsonSchema = destinationJsonSchema;
        this.sourceJsonSchema = sourceJsonSchema;
    }

    public @NotNull JsonNode getDestinationJsonSchema() {
        return destinationJsonSchema;
    }

    public @NotNull JsonNode getSourceJsonSchema() {
        return sourceJsonSchema;
    }

    public static MetadataModel from(Metadata metadata) {
        return new MetadataModel(
                metadata.getDestinationJsonSchema(),
                metadata.getSourceJsonSchema());
    }
}
