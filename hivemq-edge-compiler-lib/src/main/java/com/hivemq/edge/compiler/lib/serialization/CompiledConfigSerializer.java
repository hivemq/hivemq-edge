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
package com.hivemq.edge.compiler.lib.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hivemq.edge.compiler.lib.model.CompiledConfig;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.jetbrains.annotations.NotNull;

/** Serializes and deserializes {@link CompiledConfig} to/from JSON. */
public class CompiledConfigSerializer {

    private static final ObjectMapper MAPPER = buildMapper();

    private static ObjectMapper buildMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    public @NotNull String toJson(final @NotNull CompiledConfig config) throws IOException {
        return MAPPER.writeValueAsString(config);
    }

    public void toJson(final @NotNull CompiledConfig config, final @NotNull File outputFile) throws IOException {
        MAPPER.writeValue(outputFile, config);
    }

    public @NotNull CompiledConfig fromJson(final @NotNull String json) throws IOException {
        return MAPPER.readValue(json, CompiledConfig.class);
    }

    public @NotNull CompiledConfig fromJson(final @NotNull File file) throws IOException {
        return MAPPER.readValue(file, CompiledConfig.class);
    }

    public @NotNull CompiledConfig fromJson(final @NotNull InputStream inputStream) throws IOException {
        return MAPPER.readValue(inputStream, CompiledConfig.class);
    }
}
