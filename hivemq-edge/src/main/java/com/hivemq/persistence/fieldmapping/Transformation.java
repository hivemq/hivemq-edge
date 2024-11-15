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

import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.api.model.mapping.TransformationModel;
import com.hivemq.configuration.entity.adapter.TransformationEntity;
import com.hivemq.extension.sdk.api.annotations.NotNull;

@SuppressWarnings("InstantiationOfUtilityClass")
public class Transformation {

    // currently there is no transformation present at all

    public static Transformation from(final @NotNull TransformationEntity transformation) {
        return new Transformation();
    }

    public static Transformation fromModel(final @NotNull TransformationModel transformation) {
        return new Transformation();
    }
}
