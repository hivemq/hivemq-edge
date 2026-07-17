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
package com.hivemq.protocols.v2.southbound;

import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.adapter.sdk.api.v2.node.NodeProperty;
import java.util.EnumSet;
import org.jetbrains.annotations.NotNull;

/** A unique {@link Node} identified by a plain string, for the southbound write tests. */
final class TestNode extends Node {

    private final @NotNull String identifier;

    TestNode(final @NotNull String identifier) {
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
        return EnumSet.of(NodeProperty.UNIQUE);
    }
}
