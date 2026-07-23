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
package com.hivemq.edge.adapters.file.v2;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The type of the content within a polled file, and how its raw bytes decode to a value. The five types and their
 * decode rules are carried over verbatim from the v1 File adapter:
 * <ul>
 * <li>{@link #BINARY} — Base64-encoded bytes;</li>
 * <li>{@link #TEXT_JSON} — parsed to a {@code JsonNode} (a non-JSON file raises a {@link MappingException});</li>
 * <li>{@link #TEXT_PLAIN}, {@link #TEXT_XML}, {@link #TEXT_CSV} — decoded as a UTF-8 string.</li>
 * </ul>
 * The framework deserializes a tag's node-string into a {@link FileNode} whose {@code contentType} is one of these
 * constants (by name), so the constant names are part of the node-string contract.
 */
public enum FileContentType {
    BINARY(FileContentType::mapBinary),
    TEXT_PLAIN(FileContentType::mapPlainText),
    TEXT_JSON(FileContentType::mapJson),
    TEXT_XML(FileContentType::mapPlainText),
    TEXT_CSV(FileContentType::mapPlainText);

    private static final @NotNull ObjectMapper OBJECT_MAPPER =
            new ObjectMapper().enable(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION);

    private final @NotNull Function<byte[], Object> mapperFunction;

    FileContentType(final @NotNull Function<byte[], Object> mapperFunction) {
        this.mapperFunction = mapperFunction;
    }

    /**
     * Decode the file's raw bytes to a value of this content type.
     *
     * @param fileContent the file's raw bytes.
     * @return the decoded value — a {@code JsonNode} for {@link #TEXT_JSON}, a Base64-encoded {@code byte[]} for
     *         {@link #BINARY}, a UTF-8 {@code String} otherwise; or {@code null} if nothing could be decoded.
     * @throws MappingException if the bytes cannot be decoded to this content type.
     */
    public @Nullable Object map(final byte @NotNull [] fileContent) {
        return mapperFunction.apply(fileContent);
    }

    private static @NotNull Object mapBinary(final byte @NotNull [] data) {
        return Base64.getEncoder().encode(data);
    }

    private static @NotNull Object mapPlainText(final byte @NotNull [] data) {
        return new String(data, StandardCharsets.UTF_8);
    }

    private static @Nullable Object mapJson(final byte @NotNull [] data) {
        try {
            return OBJECT_MAPPER.readTree(data);
        } catch (final IOException e) {
            throw new MappingException("The content of the file could not be parsed to a JSON:" + e.getMessage());
        }
    }
}
