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
package com.hivemq.edge.adapters.opcua.condition;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseResultMask;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Verifies that an event query's narrowing node can propagate through the notifier it subscribes to.
 * <p>
 * A syntactically valid where-clause operand says nothing about scope. If a query subscribes to Area B but
 * compares {@code ConditionId} or {@code SourceNode} with a node below sibling Area A, the server accepts both
 * the monitored item and filter — and the tag then stays silent forever because the matching event never
 * reaches Area B. This walk rejects that configuration before subscription.
 * <p>
 * The walk goes upward from the narrow node rather than downward from the notifier. A condition/source has a
 * small ancestry; a plant-wide notifier can have an enormous subtree. It also means a selected common/root
 * notifier is accepted naturally rather than being confused with the nearest notifier.
 * <p>
 * Only a complete negative answer is {@link Result.OutsideHierarchy}. A hidden node, denied browse, transport
 * failure, unmappable continuation, or hierarchy deeper than the safety bound is {@link Result.Unverified};
 * callers retain compatibility for those servers but must make that uncertainty visible to the operator.
 */
public final class EventPropagationVerifier {

    /** The same practical hierarchy bound used by notifier resolution. */
    private static final int MAX_WALK_DEPTH = 10;

    private EventPropagationVerifier() {}

    /** Outcome of checking one narrowing node against one selected notifier. */
    public sealed interface Result {

        /** The selected notifier is the node itself or an ancestor on at least one propagation path. */
        record Reachable() implements Result {}

        /** Every browsable propagation path ended without reaching the selected notifier. */
        record OutsideHierarchy() implements Result {}

        /** The relationship could not be decided from a complete browse. */
        record Unverified(@NotNull String reason) implements Result {}
    }

    /**
     * Checks a {@code ConditionId} operand.
     * <p>
     * The specification path is condition --inverse HasCondition--&gt; ConditionSource --inverse
     * HasEventSource/HasNotifier--&gt; notifier. The compatibility path starts the event-source walk at the
     * condition itself for servers that attach it directly beneath a notifier. Either valid path is enough.
     */
    public static @NotNull CompletableFuture<Result> conditionCanEmitThrough(
            final @NotNull OpcUaClient client, final @NotNull NodeId conditionNode, final @NotNull NodeId notifier) {

        if (conditionNode.equals(notifier)) {
            return CompletableFuture.completedFuture(new Result.Reachable());
        }

        final BrowseDescription conditionSources = new BrowseDescription(
                conditionNode,
                BrowseDirection.Inverse,
                NodeIds.HasCondition,
                false,
                uint(NodeClass.Object.getValue() | NodeClass.Variable.getValue() | NodeClass.Method.getValue()),
                uint(BrowseResultMask.All.getValue()));

        final CompletableFuture<Result> specificationPath = Browsing.browseAll(client, conditionSources)
                .thenCompose(references -> walkUpwards(client, toNodeIds(client, references), notifier))
                .exceptionally(throwable -> unverified(conditionNode, throwable));

        final CompletableFuture<Result> compatibilityPath = walkUpwards(client, List.of(conditionNode), notifier);

        return specificationPath.thenCombine(compatibilityPath, EventPropagationVerifier::eitherPath);
    }

    /** Checks a {@code SourceNode} operand against its selected notifier. */
    public static @NotNull CompletableFuture<Result> sourceCanEmitThrough(
            final @NotNull OpcUaClient client, final @NotNull NodeId sourceNode, final @NotNull NodeId notifier) {
        return walkUpwards(client, List.of(sourceNode), notifier);
    }

    /** One valid alternative proves reachability; only two complete negatives prove a mismatch. */
    private static @NotNull Result eitherPath(final @NotNull Result first, final @NotNull Result second) {
        if (first instanceof Result.Reachable || second instanceof Result.Reachable) {
            return new Result.Reachable();
        }
        if (first instanceof Result.OutsideHierarchy && second instanceof Result.OutsideHierarchy) {
            return new Result.OutsideHierarchy();
        }
        return first instanceof final Result.Unverified unverified ? unverified : (Result.Unverified) second;
    }

    private static @NotNull CompletableFuture<Result> walkUpwards(
            final @NotNull OpcUaClient client, final @NotNull List<NodeId> origins, final @NotNull NodeId notifier) {
        final Set<NodeId> visited = new LinkedHashSet<>(origins);
        return walkUpwards(client, origins, notifier, visited, 0, null);
    }

    /**
     * Breadth-first ancestry walk. {@code depth} is the number of upward event-source hops taken to reach the
     * current frontier, so a target exactly at the bound is still tested.
     */
    private static @NotNull CompletableFuture<Result> walkUpwards(
            final @NotNull OpcUaClient client,
            final @NotNull List<NodeId> frontier,
            final @NotNull NodeId notifier,
            final @NotNull Set<NodeId> visited,
            final int depth,
            final @Nullable String uncertainty) {

        if (frontier.contains(notifier)) {
            return CompletableFuture.completedFuture(new Result.Reachable());
        }
        if (frontier.isEmpty()) {
            return CompletableFuture.completedFuture(finished(uncertainty));
        }

        return parentsOf(client, frontier, 0, new ArrayList<>(), visited, uncertainty)
                .thenCompose(parents -> {
                    if (parents.nodes().isEmpty()) {
                        return CompletableFuture.completedFuture(finished(parents.uncertainty()));
                    }
                    if (depth >= MAX_WALK_DEPTH) {
                        return CompletableFuture.completedFuture(new Result.Unverified("the event hierarchy above "
                                + frontier.stream()
                                        .map(NodeId::toParseableString)
                                        .toList()
                                + " extends beyond the "
                                + MAX_WALK_DEPTH
                                + "-hop verification bound"));
                    }
                    if (parents.nodes().contains(notifier)) {
                        return CompletableFuture.completedFuture(new Result.Reachable());
                    }
                    return walkUpwards(client, parents.nodes(), notifier, visited, depth + 1, parents.uncertainty());
                });
    }

    /** Browses one complete frontier while retaining uncertainty from failed branches. */
    private static @NotNull CompletableFuture<Parents> parentsOf(
            final @NotNull OpcUaClient client,
            final @NotNull List<NodeId> frontier,
            final int index,
            final @NotNull List<NodeId> collected,
            final @NotNull Set<NodeId> visited,
            final @Nullable String uncertainty) {

        if (index >= frontier.size()) {
            return CompletableFuture.completedFuture(new Parents(collected, uncertainty));
        }

        final NodeId node = frontier.get(index);
        final BrowseDescription browse = new BrowseDescription(
                node,
                BrowseDirection.Inverse,
                NodeIds.HasEventSource,
                true,
                uint(0),
                uint(BrowseResultMask.All.getValue()));

        return Browsing.browseAll(client, browse)
                .handle((references, throwable) -> new BrowseAnswer(references, throwable))
                .thenCompose(answer -> {
                    String nextUncertainty = uncertainty;
                    if (answer.throwable() != null) {
                        if (nextUncertainty == null) {
                            nextUncertainty = "could not completely browse the event hierarchy from "
                                    + node.toParseableString() + ": "
                                    + Browsing.describeException(answer.throwable());
                        }
                    } else {
                        for (final NodeId parent : toNodeIds(client, Objects.requireNonNull(answer.references()))) {
                            if (visited.add(parent)) {
                                collected.add(parent);
                            }
                        }
                    }
                    return parentsOf(client, frontier, index + 1, collected, visited, nextUncertainty);
                });
    }

    private static @NotNull Result finished(final @Nullable String uncertainty) {
        return uncertainty == null ? new Result.OutsideHierarchy() : new Result.Unverified(uncertainty);
    }

    private static @NotNull Result.Unverified unverified(
            final @NotNull NodeId node, final @NotNull Throwable throwable) {
        return new Result.Unverified("could not browse the event hierarchy from " + node.toParseableString() + ": "
                + Browsing.describeException(throwable));
    }

    /** Converts only references addressable in this session. */
    private static @NotNull List<NodeId> toNodeIds(
            final @NotNull OpcUaClient client, final @NotNull List<ReferenceDescription> references) {
        final List<NodeId> nodeIds = new ArrayList<>(references.size());
        for (final ReferenceDescription reference : references) {
            reference.getNodeId().toNodeId(client.getNamespaceTable()).ifPresent(nodeIds::add);
        }
        return nodeIds;
    }

    private record Parents(
            @NotNull List<NodeId> nodes, @Nullable String uncertainty) {}

    private record BrowseAnswer(
            @Nullable List<ReferenceDescription> references,
            @Nullable Throwable throwable) {}
}
