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
import com.hivemq.api.model.fieldmapping.FieldMappingModel;
import com.hivemq.configuration.entity.adapter.FieldMappingEntity;
import com.hivemq.extension.sdk.api.annotations.NotNull;

public class FieldMapping {


    private final @NotNull String sourceFieldName;
    private final @NotNull String destinationFieldName;
    private final @NotNull Transformation transformation;

    @JsonCreator
    public FieldMapping(
            final @NotNull String sourceFieldName,
            final @NotNull String destinationFieldName,
            final @NotNull Transformation transformation) {
        this.sourceFieldName = sourceFieldName;
        this.destinationFieldName = destinationFieldName;
        this.transformation = transformation;
    }

    public static @NotNull FieldMapping fromModel(final @NotNull FieldMappingModel model) {
        return new FieldMapping(model.getSourceFieldName(),
                model.getDestinationFieldName(),
                Transformation.fromModel(model.getTransformation()));
    }


    public @NotNull String getDestinationFieldName() {
        return destinationFieldName;
    }

    public @NotNull String getSourceFieldName() {
        return sourceFieldName;
    }

    public @NotNull Transformation getTransformation() {
        return transformation;
    }

    public static FieldMapping from(final @NotNull FieldMappingEntity fieldMappingEntity) {
        return new FieldMapping(fieldMappingEntity.getSourceFieldName(),
                fieldMappingEntity.getDestinationFieldName(),
                Transformation.from(fieldMappingEntity.getTransformation()));
    }
}
