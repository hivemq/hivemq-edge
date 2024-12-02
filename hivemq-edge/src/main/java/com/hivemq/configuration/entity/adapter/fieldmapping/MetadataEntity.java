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
package com.hivemq.configuration.entity.adapter.fieldmapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import com.hivemq.persistence.mappings.fieldmapping.Metadata;

import javax.xml.bind.annotation.XmlElement;

public class MetadataEntity {

    @XmlElement(name = "sourceSchema")
    private final @NotNull String sourceJsonSchema;

    @XmlElement(name = "destinationSchema")
    private final @NotNull String destinationJsonSchema;

    // no-arg for JaxB
    MetadataEntity() {
        sourceJsonSchema = "{}";
        destinationJsonSchema = "{}";
    }

    public MetadataEntity(
            final @NotNull String sourceJsonSchema,
            final @NotNull String destinationJsonSchema) {
        this.sourceJsonSchema = sourceJsonSchema;
        this.destinationJsonSchema = destinationJsonSchema;
    }

    public @NotNull String getDestinationJsonSchema() {
        return destinationJsonSchema;
    }

    public @NotNull String getSourceJsonSchema() {
        return sourceJsonSchema;
    }

    public static @NotNull MetadataEntity from(
            final @NotNull Metadata model) {
        return new MetadataEntity(model.getSourceJsonSchema().toString(),
                model.getDestinationJsonSchema().toString());
    }

    public @NotNull Metadata to(ObjectMapper mapper) {
        final Metadata metadata;
        try {
            metadata =
                    new Metadata(mapper.readTree(getSourceJsonSchema()), mapper.readTree(getDestinationJsonSchema()));
            return metadata;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e); //TODO nicer!
        }
    }
}
