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

import com.hivemq.adapter.sdk.api.v2.node.Node;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;

/**
 * Selects the nodes a scripted {@link ChaosScript} behavior applies to: every node, one node by
 * {@link Node#nodeId()}, or an arbitrary predicate. The first script rule whose matcher matches a node wins, so a
 * specific {@link #byId(String)} rule declared before {@link #all()} overrides the catch-all.
 */
public final class NodeMatcher {

    private final @NotNull Predicate<Node> predicate;

    private NodeMatcher(final @NotNull Predicate<Node> predicate) {
        this.predicate = predicate;
    }

    /**
     * @return a matcher that matches every node.
     */
    public static @NotNull NodeMatcher all() {
        return new NodeMatcher(node -> true);
    }

    /**
     * @param id the {@link Node#nodeId()} to match.
     * @return a matcher that matches the single node with the given id.
     */
    public static @NotNull NodeMatcher byId(final @NotNull String id) {
        return new NodeMatcher(node -> node.nodeId().equals(id));
    }

    /**
     * @param predicate the predicate a node must satisfy.
     * @return a matcher that matches every node the predicate accepts.
     */
    public static @NotNull NodeMatcher matching(final @NotNull Predicate<Node> predicate) {
        return new NodeMatcher(predicate);
    }

    /**
     * @param node the node to test.
     * @return whether this matcher matches the given node.
     */
    public boolean matches(final @NotNull Node node) {
        return predicate.test(node);
    }
}
