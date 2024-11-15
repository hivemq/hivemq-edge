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
import com.hivemq.persistence.fieldmapping.FieldMapping;

public class FieldMappingEntity {

    @JsonProperty("source")
    private final @NotNull String sourceFieldName;
    @JsonProperty("destination")
    private final @NotNull String destinationFieldName;
    @JsonProperty("transformation")
    private final @NotNull TransformationEntity transformation;

    @JsonCreator
    public FieldMappingEntity(
            @JsonProperty("source") final @NotNull String sourceFieldName,
            @JsonProperty("destination") final @NotNull String destinationFieldName,
            @JsonProperty("transformation") final @NotNull TransformationEntity transformation) {
        this.sourceFieldName = sourceFieldName;
        this.destinationFieldName = destinationFieldName;
        this.transformation = transformation;
    }

    public static @NotNull FieldMappingEntity from(final @NotNull FieldMapping model) {
        return new FieldMappingEntity(model.getSourceFieldName(),
                model.getDestinationFieldName(),
                TransformationEntity.from(model.getTransformation()));
    }


    public @NotNull String getDestinationFieldName() {
        return destinationFieldName;
    }

    public @NotNull String getSourceFieldName() {
        return sourceFieldName;
    }

    public @NotNull TransformationEntity getTransformation() {
        return transformation;
    }
}
