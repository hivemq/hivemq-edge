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
package com.hivemq.edge.adapters.browse.file;

import static com.hivemq.edge.adapters.browse.file.DeviceTagJsonSerializer.FileDto;
import static com.hivemq.edge.adapters.browse.file.DeviceTagJsonSerializer.fromFileDto;
import static com.hivemq.edge.adapters.browse.file.DeviceTagJsonSerializer.toFileDto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.hivemq.edge.adapters.browse.model.DeviceTagRow;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Serializes and deserializes {@link DeviceTagRow} lists to/from YAML format.
 * Uses the same nested DTO structure as the JSON serializer, differing only in the ObjectMapper factory.
 */
@Singleton
public class DeviceTagYamlSerializer {

    private final @NotNull ObjectMapper mapper;

    @Inject
    public DeviceTagYamlSerializer() {
        this(createDefaultMapper());
    }

    DeviceTagYamlSerializer(final @NotNull ObjectMapper mapper) {
        this.mapper = mapper;
    }

    static @NotNull ObjectMapper createDefaultMapper() {
        return new ObjectMapper(YAMLFactory.builder()
                        .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                        .build())
                .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
                .configure(SerializationFeature.INDENT_OUTPUT, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public byte @NotNull [] serialize(final @NotNull List<DeviceTagRow> rows) throws IOException {
        return mapper.writeValueAsBytes(toFileDto(rows));
    }

    public @NotNull List<DeviceTagRow> deserialize(final byte @NotNull [] data) throws IOException {
        return fromFileDto(mapper.readValue(data, FileDto.class));
    }
}
