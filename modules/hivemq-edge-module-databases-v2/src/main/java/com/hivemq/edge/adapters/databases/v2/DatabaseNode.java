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
package com.hivemq.edge.adapters.databases.v2;

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
import org.jetbrains.annotations.Nullable;

/**
 * The Databases adapter's protocol {@link Node}: a SQL {@code SELECT} query to execute against the configured
 * database, together with how its result rows are shaped into northbound messages — a {@link SplitMode} choosing
 * between all rows in one array message ({@link SplitMode#ALL_IN_ONE}), one message per row
 * ({@link SplitMode#ONE_PER_ROW}), or one message per batch of {@link #batchSize()} rows
 * ({@link SplitMode#ONE_PER_BATCH}). A query is {@link NodeProperty#TYPED} (its result shape is fixed by the
 * statement), but it is not {@link NodeProperty#UNIQUE}: two tags may legitimately carry the identical query text, and
 * node correlation across the adapter boundary is by reference identity anyway, so this class deliberately does not
 * override {@code equals}/{@code hashCode}.
 * <p>
 * The fields carry a Jackson creator and property annotations so the framework's own {@code ObjectMapper}
 * deserializes this node from its {@link #nodeString()} when an Edge runtime loads a configured Databases adapter.
 */
@JsonPropertyOrder({"query", "splitMode", "batchSize"})
public final class DatabaseNode extends Node {

    static final int DEFAULT_BATCH_SIZE = 100;
    static final int MIN_BATCH_SIZE = 1;
    static final int MAX_BATCH_SIZE = 1000;

    private static final @NotNull ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @JsonProperty("query")
    private final @NotNull String query;

    @JsonProperty("splitMode")
    private final @NotNull SplitMode splitMode;

    @JsonProperty("batchSize")
    private final int batchSize;

    /**
     * @param query     the SQL query to execute on the database.
     * @param splitMode how the result rows are shaped into messages. Absent means {@link SplitMode#ALL_IN_ONE}.
     * @param batchSize the batch size — the number of rows drained per output call in {@link SplitMode#ONE_PER_ROW}
     *                  mode and the number of rows per array message in {@link SplitMode#ONE_PER_BATCH} mode (ignored
     *                  in {@link SplitMode#ALL_IN_ONE}), clamped to {@value #MIN_BATCH_SIZE}..{@value #MAX_BATCH_SIZE}.
     *                  Absent means {@value #DEFAULT_BATCH_SIZE}.
     */
    @JsonCreator
    public DatabaseNode(
            @JsonProperty(value = "query", required = true) final @NotNull String query,
            @JsonProperty("splitMode") final @Nullable SplitMode splitMode,
            @JsonProperty("batchSize") final @Nullable Integer batchSize) {
        this.query = Objects.requireNonNull(query, "query must not be null");
        this.splitMode = Objects.requireNonNullElse(splitMode, SplitMode.ALL_IN_ONE);
        final int requestedBatchSize = Objects.requireNonNullElse(batchSize, DEFAULT_BATCH_SIZE);
        this.batchSize = Math.max(MIN_BATCH_SIZE, Math.min(requestedBatchSize, MAX_BATCH_SIZE));
    }

    /**
     * @return the SQL query to execute on the database.
     */
    public @NotNull String query() {
        return query;
    }

    /**
     * @return how the result rows are shaped into northbound messages.
     */
    public @NotNull SplitMode splitMode() {
        return splitMode;
    }

    /**
     * @return the batch size — rows drained per output call in {@link SplitMode#ONE_PER_ROW} mode and rows per array
     *         message in {@link SplitMode#ONE_PER_BATCH} mode (ignored in {@link SplitMode#ALL_IN_ONE}), in
     *         {@value #MIN_BATCH_SIZE}..{@value #MAX_BATCH_SIZE}.
     */
    public int batchSize() {
        return batchSize;
    }

    @Override
    public @NotNull String nodeId() {
        return query;
    }

    @Override
    public @NotNull String nodeString() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (final JsonProcessingException e) {
            throw new IllegalStateException(
                    "The database node could not be serialized to a node-string: " + e.getMessage());
        }
    }

    @Override
    public @NotNull EnumSet<NodeProperty> properties() {
        return EnumSet.of(NodeProperty.TYPED);
    }

    @Override
    public @NotNull String toString() {
        return query;
    }
}
