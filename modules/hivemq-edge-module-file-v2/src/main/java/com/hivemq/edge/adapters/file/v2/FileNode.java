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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.adapter.sdk.api.v2.node.NodeProperty;
import java.util.EnumSet;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * The File adapter's protocol {@link Node}: a file to poll, identified by its absolute path and decoded with a
 * {@link FileContentType}. A file path pins a single source unambiguously, so a node is {@link NodeProperty#UNIQUE}
 * (and therefore {@link NodeProperty#TYPED} by the property implication the content type carries); it is not
 * {@link NodeProperty#VALID}-checked until poll time, when the file is actually read.
 * <p>
 * The fields carry a Jackson creator and property annotations so the framework's own {@code ObjectMapper}
 * deserializes this node from its {@link #nodeString()} when an Edge runtime loads a configured File adapter —
 * exactly as the v1 File tag definition round-trips. Node correlation across the adapter boundary is by reference
 * identity, so this class deliberately does not override {@code equals}/{@code hashCode}.
 */
@JsonPropertyOrder({"filePath", "contentType"})
public final class FileNode extends Node {

    private static final @NotNull ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @JsonProperty("filePath")
    private final @NotNull String filePath;

    @JsonProperty("contentType")
    private final @NotNull FileContentType contentType;

    /**
     * @param filePath    the absolute path to the file to read.
     * @param contentType the type of the content within the file.
     */
    @JsonCreator
    public FileNode(
            @JsonProperty(value = "filePath", required = true) final @NotNull String filePath,
            @JsonProperty(value = "contentType", required = true) final @NotNull FileContentType contentType) {
        this.filePath = Objects.requireNonNull(filePath, "filePath must not be null");
        this.contentType = Objects.requireNonNull(contentType, "contentType must not be null");
    }

    /**
     * @return the absolute path to the file to read.
     */
    public @NotNull String filePath() {
        return filePath;
    }

    /**
     * @return the type of the content within the file.
     */
    public @NotNull FileContentType contentType() {
        return contentType;
    }

    @Override
    public @NotNull String nodeId() {
        return filePath;
    }

    @Override
    public @NotNull String nodeString() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (final JsonProcessingException e) {
            throw new MappingException("The file node could not be serialized to a node-string: " + e.getMessage());
        }
    }

    @Override
    public @NotNull EnumSet<NodeProperty> properties() {
        return EnumSet.of(NodeProperty.UNIQUE, NodeProperty.TYPED);
    }

    @Override
    public @NotNull String toString() {
        return filePath;
    }
}
