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
package com.hivemq.edge.adapters.workload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.adapter.sdk.api.v2.node.NodeProperty;
import java.util.EnumSet;
import org.jetbrains.annotations.NotNull;

/**
 * The Workload adapter's protocol node — a minimal {@link Node} identified by a single id, which the scenario keys
 * its per-tag waveform and fault timeline on. Carries a Jackson creator so the framework's own {@code ObjectMapper}
 * deserializes it from its {@link #nodeString()} when Edge loads a configured workload adapter.
 */
public final class WorkloadNode extends Node {

    private final @NotNull String identifier;

    @JsonCreator
    public WorkloadNode(final @JsonProperty("identifier") @NotNull String identifier) {
        this.identifier = identifier;
    }

    @Override
    public @NotNull String nodeId() {
        return identifier;
    }

    @Override
    public @NotNull String nodeString() {
        // Escape the identifier so a value containing " \ or a control character still produces valid JSON that the
        // framework ObjectMapper can round-trip when Edge reloads the configuration.
        final StringBuilder sb = new StringBuilder(identifier.length() + 16).append("{\"identifier\":\"");
        for (int i = 0; i < identifier.length(); i++) {
            final char c = identifier.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append("\"}").toString();
    }

    @Override
    public @NotNull EnumSet<NodeProperty> properties() {
        return EnumSet.of(NodeProperty.UNIQUE, NodeProperty.TYPED);
    }

    @Override
    public @NotNull String toString() {
        return identifier;
    }
}
