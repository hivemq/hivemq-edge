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
package com.hivemq.persistence.mappings.fieldmapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.api.model.mappings.fieldmapping.MetadataModel;
import org.jetbrains.annotations.NotNull;

public class Metadata {
    private final @NotNull JsonNode destinationJsonSchema;

    private final @NotNull JsonNode sourceJsonSchema;

    public Metadata(final @NotNull JsonNode destinationJsonSchema, final @NotNull JsonNode sourceJsonSchema) {
        this.destinationJsonSchema = destinationJsonSchema;
        this.sourceJsonSchema = sourceJsonSchema;
    }

    public @NotNull JsonNode getDestinationJsonSchema() {
        return destinationJsonSchema;
    }

    public @NotNull JsonNode getSourceJsonSchema() {
        return sourceJsonSchema;
    }

    public static Metadata from(MetadataModel model) {
        return new Metadata(model.getDestinationJsonSchema(), model.getSourceJsonSchema());
    }
}
