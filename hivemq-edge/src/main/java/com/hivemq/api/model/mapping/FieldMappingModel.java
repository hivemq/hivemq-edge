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
import com.hivemq.persistence.fieldmapping.FieldMapping;
import io.swagger.v3.oas.annotations.media.Schema;

public class FieldMappingModel {

    @JsonProperty("source")
    @Schema(description = "The field name in the incoming data.")
    private final @NotNull String sourceFieldName;


    @JsonProperty("destination")
    @Schema(description = "The field name in the outgoing data")
    private final @NotNull String destinationFieldName;


    @JsonProperty("transformation")
    @Schema(description = "The kind of transformation that is performed on the data")
    private final @NotNull TransformationModel transformation;

    @JsonCreator
    public FieldMappingModel(
            @JsonProperty("source") final @NotNull String sourceFieldName,
            @JsonProperty("destination") final @NotNull String destinationFieldName,
            @JsonProperty("transformation") final @NotNull TransformationModel transformation) {
        this.sourceFieldName = sourceFieldName;
        this.destinationFieldName = destinationFieldName;
        this.transformation = transformation;
    }

    public @NotNull String getDestinationFieldName() {
        return destinationFieldName;
    }

    public @NotNull String getSourceFieldName() {
        return sourceFieldName;
    }

    public @NotNull TransformationModel getTransformation() {
        return transformation;
    }

    public static @NotNull FieldMappingModel from(final @NotNull FieldMapping fieldMapping) {
        return new FieldMappingModel(fieldMapping.getSourceFieldName(), fieldMapping.getDestinationFieldName(), TransformationModel.from(fieldMapping.getTransformation()));
    }
}
