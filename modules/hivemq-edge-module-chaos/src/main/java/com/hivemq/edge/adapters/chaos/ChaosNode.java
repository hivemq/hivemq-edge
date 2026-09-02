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
package com.hivemq.edge.adapters.chaos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.adapter.sdk.api.v2.node.NodeProperty;
import java.util.EnumSet;
import org.jetbrains.annotations.NotNull;

/**
 * The {@link ChaosProtocolAdapter}'s protocol node — a minimal {@link Node} identified by a single id. It is {@link NodeProperty#UNIQUE} (and therefore {@link NodeProperty#TYPED}, by the property implication
 * the protocol carries), so a scripted {@link NodeMatcher#byId(String)} can correlate behaviors to nodes. The
 * wrapper never inspects more than the {@link Node} reference, so a single id is all the simulator needs.
 * <p>
 * The {@code identifier} carries a Jackson creator so the framework's own {@code ObjectMapper} deserializes this node
 * from its {@link #nodeString()} when an Edge runtime loads a configured chaos adapter — exactly as a real adapter's
 * node class would.
 */
public final class ChaosNode extends Node {

    private final @NotNull String identifier;

    /**
     * @param identifier the node's stable identity, used by {@link NodeMatcher#byId(String)} and the
     *                   {@code nodeString} serialization.
     */
    @JsonCreator
    public ChaosNode(final @JsonProperty("identifier") @NotNull String identifier) {
        this.identifier = identifier;
    }

    @Override
    public @NotNull String nodeId() {
        return identifier;
    }

    @Override
    public @NotNull String nodeString() {
        return "{\"identifier\":\"" + identifier + "\"}";
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
