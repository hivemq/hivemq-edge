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
package com.hivemq.protocols.v2.wrapper;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.schema.ScalarSchema;
import com.hivemq.adapter.sdk.api.schema.ScalarType;
import com.hivemq.adapter.sdk.api.schema.Schema;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.adapter.sdk.api.v2.node.NodeProperty;
import com.hivemq.adapter.sdk.api.v2.node.NodeTagPair;
import java.util.EnumSet;
import org.jetbrains.annotations.NotNull;

/**
 * Shared fixtures for the adapter-machine tests: a minimal {@link Node}, a {@link NodeTagPair} factory carrying a
 * reused v1 {@link Schema}, and a minimal {@link DataPoint}. The wrapper never inspects the schema or value — it
 * only correlates by {@link Node} reference and stamps tag names — so the simplest concrete instances suffice.
 */
final class WrapperTestSupport {

    private WrapperTestSupport() {}

    static @NotNull Node node(final @NotNull String identifier) {
        return new TestNode(identifier);
    }

    static @NotNull NodeTagPair pair(final @NotNull String tagName) {
        final Schema schema = new ScalarSchema(ScalarType.STRING, null, null, null, null, false, true, false);
        return NodeTagPair.create(new TestNode(tagName), tagName, schema, true, false);
    }

    static @NotNull DataPoint dataPoint(final @NotNull String tagName, final @NotNull Object value) {
        return new TestDataPoint(tagName, value);
    }

    static final class TestNode extends Node {

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

        @Override
        public @NotNull String toString() {
            return identifier;
        }
    }

    record TestDataPoint(@NotNull String tagName, @NotNull Object value) implements DataPoint {

        @Override
        public @NotNull Object getTagValue() {
            return value;
        }

        @Override
        public boolean treatTagValueAsJson() {
            return false;
        }

        @Override
        public @NotNull String getTagName() {
            return tagName;
        }
    }
}
