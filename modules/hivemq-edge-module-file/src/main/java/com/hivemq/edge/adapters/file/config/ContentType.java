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
package com.hivemq.edge.adapters.file.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.edge.adapters.file.convertion.MappingException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unused")
public enum ContentType {
    BINARY(ContentType::mapBinary, "application/octet-stream"),
    TEXT_PLAIN(ContentType::mapPlainText, "text/plain"),
    TEXT_JSON(ContentType::mapJson, "application/json"),
    TEXT_XML(ContentType::mapPlainText, "application/xml"),
    TEXT_CSV(ContentType::mapPlainText, "text/csv");

    private static final @NotNull ObjectMapper OBJECT_MAPPER =
            new ObjectMapper().enable(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION);
    private static final @NotNull Logger log = LoggerFactory.getLogger(ContentType.class);

    private final @NotNull Function<byte[], Object> mapperFunction;
    private final @NotNull String mimeTypeRepresentation;

    ContentType(final @NotNull Function<byte[], Object> mapperFunction, final @NotNull String mimeTypeRepresentation) {
        this.mapperFunction = mapperFunction;
        this.mimeTypeRepresentation = mimeTypeRepresentation;
    }

    public @NotNull String getMimeTypeRepresentation() {
        return mimeTypeRepresentation;
    }

    public @Nullable Object map(byte @NotNull [] fileContent) {
        return mapperFunction.apply(fileContent);
    }

    public static @NotNull Object mapBinary(final byte @NotNull [] data) {
        return Base64.getEncoder().encode(data);
    }

    public static @NotNull Object mapPlainText(final byte @NotNull [] data) {
        return new String(data, StandardCharsets.UTF_8);
    }

    public static @NotNull Object mapJson(final byte @NotNull [] data) {
        try {
            return OBJECT_MAPPER.readTree(data);
        } catch (IOException e) {
            throw new MappingException("The content of the file could not be parsed to a JSON:" + e.getMessage());
        }
    }
}
