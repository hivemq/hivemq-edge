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
 * database, together with the message-shaping choice — one message carrying all result rows as an array, or one
 * message per row. A query is {@link NodeProperty#TYPED} (its result shape is fixed by the statement), but it is not
 * {@link NodeProperty#UNIQUE}: two tags may legitimately carry the identical query text, and node correlation across
 * the adapter boundary is by reference identity anyway, so this class deliberately does not override
 * {@code equals}/{@code hashCode}.
 * <p>
 * The fields carry a Jackson creator and property annotations so the framework's own {@code ObjectMapper}
 * deserializes this node from its {@link #nodeString()} when an Edge runtime loads a configured Databases adapter.
 * The {@code spiltLinesInIndividualMessages} key preserves the historical (misspelled) v1 configuration key exactly,
 * so existing tag definitions carry over unchanged.
 */
@JsonPropertyOrder({"query", "spiltLinesInIndividualMessages"})
public final class DatabaseNode extends Node {

    private static final @NotNull ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @JsonProperty("query")
    private final @NotNull String query;

    @JsonProperty("spiltLinesInIndividualMessages")
    private final boolean spiltLinesInIndividualMessages;

    /**
     * @param query                          the SQL query to execute on the database.
     * @param spiltLinesInIndividualMessages whether each result row is published as its own message instead of all
     *                                       rows in one array message. Absent means {@code false}.
     */
    @JsonCreator
    public DatabaseNode(
            @JsonProperty(value = "query", required = true) final @NotNull String query,
            @JsonProperty(value = "spiltLinesInIndividualMessages")
                    final @Nullable Boolean spiltLinesInIndividualMessages) {
        this.query = Objects.requireNonNull(query, "query must not be null");
        this.spiltLinesInIndividualMessages = Objects.requireNonNullElse(spiltLinesInIndividualMessages, false);
    }

    /**
     * @return the SQL query to execute on the database.
     */
    public @NotNull String query() {
        return query;
    }

    /**
     * @return whether each result row is published as its own message instead of all rows in one array message.
     */
    public boolean spiltLinesInIndividualMessages() {
        return spiltLinesInIndividualMessages;
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
