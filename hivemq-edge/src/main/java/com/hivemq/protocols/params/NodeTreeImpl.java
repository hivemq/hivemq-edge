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
package com.hivemq.protocols.params;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.edge.modules.adapters.model.NodeTree;
import com.hivemq.edge.modules.adapters.model.NodeType;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NodeTreeImpl implements NodeTree {

    private final Map<String, ObjectNode> nodes = new HashMap<>();
    private final ObjectNode root = new ObjectNode("ROOT", "ROOT", "ROOT", NodeType.FOLDER, false);

    @Override
    public void addNode(
            @NotNull final String id,
            @NotNull final String name,
            @NotNull final String description,
            @Nullable final String parentId,
            @NotNull NodeType nodeType,
            boolean selectable) {
        final ObjectNode node = new ObjectNode(name, description, id, nodeType, selectable);
        if (parentId != null) {
            final ObjectNode parentNode = nodes.get(parentId);
            if (parentNode != null) {
                parentNode.addChild(node);
            }
        } else {
            root.addChild(node);
        }
        nodes.put(node.getId(), node);
    }

    public @NotNull ObjectNode getRootNode() {
        return root;
    }

    @Override
    public @NotNull String toString() {
        StringBuilder result = new StringBuilder();
        for (ObjectNode child : root.children) {
            result.append(child.toString(0));
        }
        return result.toString();
    }

    public static class ObjectNode {

        @JsonProperty("id")
        private final @NotNull String id;


        @JsonProperty("name")
        private final @NotNull String name;

        @JsonProperty("description")
        private final @NotNull String description;

        @JsonProperty("nodeType")
        private final @NotNull NodeType nodeType;

        @JsonProperty("selectable")
        private final boolean selectable;

        @JsonProperty("children")
        private final @NotNull List<ObjectNode> children = new LinkedList<>();

        public ObjectNode(
                final @NotNull String name,
                final @NotNull String description,
                final @NotNull String id,
                final @NotNull NodeType nodeType,
                final boolean selectable) {
            this.name = name;
            this.description = description;
            this.id = id;
            this.nodeType = nodeType;
            this.selectable = selectable;
        }

        public void addChild(final @NotNull ObjectNode node) {
            children.add(node);
        }

        public @NotNull List<ObjectNode> getChildren() {
            return children;
        }

        public @NotNull String getName() {
            return name;
        }

        public @NotNull String getId() {
            return id;
        }

        public @NotNull String getDescription() {
            return description;
        }

        public @NotNull NodeType getNodeType() {
            return nodeType;
        }

        public boolean isSelectable() {
            return selectable;
        }

        public String toString(int indent) {
            StringBuilder result = new StringBuilder(String.format("%s | %s | %s \n", id, name, description));

            for (ObjectNode child : children) {
                result.append(child.toString(++indent));
            }

            return indent(indent, result.toString());
        }


    }

    public static @NotNull String indent(int n, String src) {
        if (src.isEmpty()) {
            return "";
        }
        Stream<String> stream = src.lines();
        if (n > 0) {
            final String spaces = " ".repeat(n);
            stream = stream.map(s -> spaces + s);
        } else if (n == Integer.MIN_VALUE) {
            stream = stream.map(String::stripLeading);
        }
        return stream.collect(Collectors.joining("\n", "", "\n"));
    }
}
