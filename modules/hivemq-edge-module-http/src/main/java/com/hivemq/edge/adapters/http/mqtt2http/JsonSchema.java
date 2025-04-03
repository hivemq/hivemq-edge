/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.http.mqtt2http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;

public class JsonSchema {

    private static final @NotNull ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static @NotNull JsonNode createJsonSchema() {
        try {
            return OBJECT_MAPPER.readTree("{\n" +
                    "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" +
                    "  \"type\": \"object\",\n" +
                    "  \"properties\": {\n" +
                    "    \"value\": {\n" +
                    "      \"type\": \"object\"\n" +
                    "    }\n" +
                    "  },\n" +
                    "  \"required\": [\n" +
                    "    \"value\"\n" +
                    "  ]\n" +
                    "}");
        } catch (final JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
