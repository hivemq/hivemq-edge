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
package com.hivemq.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public final class JsonUtils {
    public static final @NotNull ObjectMapper NO_PRETTY_PRINT_WITH_JAVA_TIME =
            new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.INDENT_OUTPUT);

    public static final @NotNull ObjectMapper PRETTY_PRINT_WITH_JAVA_TIME =
            new ObjectMapper().registerModule(new JavaTimeModule()).enable(SerializationFeature.INDENT_OUTPUT);

    private JsonUtils() {}

    public static @NotNull Optional<DocumentContext> toDocumentContext(final byte @NotNull [] bytes) {
        try {
            return Optional.of(JsonPath.parse(new String(bytes, StandardCharsets.UTF_8)));
        } catch (final Exception e) {
            return Optional.empty();
        }
    }

    public static @NotNull Optional<JsonNode> toJsonNode(final byte @NotNull [] bytes) {
        return toJsonNode(bytes, NO_PRETTY_PRINT_WITH_JAVA_TIME);
    }

    public static @NotNull Optional<JsonNode> toJsonNode(
            final byte @NotNull [] bytes, final @NotNull ObjectMapper objectMapper) {
        try {
            return Optional.of(objectMapper.readTree(bytes));
        } catch (final Exception e) {
            return Optional.empty();
        }
    }
}
