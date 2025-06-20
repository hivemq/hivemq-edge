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
package com.hivemq.edge.adapters.opcua;

import com.hivemq.adapter.sdk.api.discovery.NodeType;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseResultMask;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import static java.util.Objects.requireNonNullElse;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class OpcUaNodeDiscovery {

    public static @NotNull CompletableFuture<List<CollectedNode>> discoverValues(
            final @NotNull OpcUaClient client,
            final @Nullable String rootNode,
            final int depth) {
        final NodeId browseRoot;
        if (rootNode == null || rootNode.isBlank()) {
            browseRoot = NodeIds.ObjectsFolder;
        } else {
            final Optional<NodeId> parsedNodeId = NodeId.parseSafe(rootNode);
            if (parsedNodeId.isEmpty()) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("OPC UA NodeId '" +
                        rootNode +
                        "' is not supported"));
            }
            browseRoot = parsedNodeId.get();
        }

        final var collectedNodes = new ArrayList<CollectedNode>();
        return browse(client, browseRoot, null, (ref, parent) -> {
            final String name = ref.getBrowseName() != null ? ref.getBrowseName().getName() : "";
            final String displayName = ref.getDisplayName() != null ? ref.getDisplayName().getText() : "";
            final NodeType nodeType = getNodeType(ref);
            collectedNodes.add(new CollectedNode(ref.getNodeId().toParseableString(),
                    requireNonNullElse(name, ""),
                    ref.getNodeId().toParseableString(),
                    requireNonNullElse(displayName, ""),
                    parent != null ? parent.getNodeId().toParseableString() : null,
                    nodeType != null ? nodeType : NodeType.VALUE,
                    nodeType == NodeType.VALUE));
        }, depth).thenApply(ignored -> collectedNodes);
    }

    private static @NotNull CompletableFuture<Void> browse(
            final @NotNull OpcUaClient client,
            final @NotNull NodeId browseRoot,
            final @Nullable ReferenceDescription parent,
            final @NotNull BiConsumer<ReferenceDescription, ReferenceDescription> callback,
            final int depth) {
        return client.browseAsync(new BrowseDescription(browseRoot,
                        BrowseDirection.Forward,
                        null,
                        true,
                        uint(0),
                        uint(BrowseResultMask.All.getValue())))
                .thenCompose(browseResult -> handleBrowseResult(client,
                        parent,
                        callback,
                        depth,
                        new BrowseResult[]{browseResult}));
    }

    private static @NotNull CompletableFuture<Void> handleBrowseResult(
            final @NotNull OpcUaClient client,
            final @Nullable ReferenceDescription parent,
            final @NotNull BiConsumer<ReferenceDescription, ReferenceDescription> callback,
            final int depth,
            final @Nullable BrowseResult @Nullable [] browseResults) {
        final var childFutures = new ArrayList<CompletableFuture<Void>>();
        final var references = new ArrayList<ReferenceDescription>();
        final var continuationPoints = new ArrayList<ByteString>();

        if (browseResults != null) {
            for (final BrowseResult result : browseResults) {
                if (result != null) {
                    final var continuationPoint = result.getContinuationPoint();
                    if (continuationPoint != null) {
                        continuationPoints.add(continuationPoint);
                    }
                    final var refs = result.getReferences();
                    if (refs != null) {
                        Collections.addAll(references, result.getReferences());
                    }
                }
            }
        }

        for (final ReferenceDescription rd : references) {
            callback.accept(rd, parent);
            // recursively browse to children
            if (depth > 1) {
                final Optional<NodeId> childNodeId = rd.getNodeId().toNodeId(client.getNamespaceTable());
                childNodeId.ifPresent(nodeId -> childFutures.add(browse(client, nodeId, rd, callback, depth - 1)));
            }
        }

        if (!continuationPoints.isEmpty()) {
            //TODO this looks like a bug in Milo
            final var cont = continuationPoints.stream().filter(ct -> ct.bytes() != null).toList();
            if (!cont.isEmpty()) {
                childFutures.add(Objects.requireNonNull(client)
                        .browseNextAsync(false, continuationPoints)
                        .thenCompose(nextBrowseResult -> handleBrowseResult(client,
                                parent,
                                callback,
                                depth,
                                nextBrowseResult.getResults())));
            }
        }

        return CompletableFuture.allOf(childFutures.toArray(new CompletableFuture[]{}));
    }

    private static @Nullable NodeType getNodeType(final @NotNull ReferenceDescription ref) {
        return switch (ref.getNodeClass()) {
            case Object, ObjectType, VariableType, ReferenceType, DataType -> NodeType.OBJECT;
            case Variable -> NodeType.VALUE;
            case View -> NodeType.FOLDER;
            default -> null;
        };
    }

    public record CollectedNode(@NotNull String id, @NotNull String name, @NotNull String value,
                                @NotNull String description, @Nullable String parentId, @NotNull NodeType nodeType,
                                boolean selectable) {
    }
}
