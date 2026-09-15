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
 * <p>
 * The Server object is the one notifier the <em>walk</em> is not asked about: the specification guarantees
 * every event of the server is accessible there, so no answer the walk could give about the hierarchy would
 * be worth having. It is still asked whether the narrowing node exists — see {@link #isRootNotifier} and
 * {@link #presentInAddressSpace}.
 */
public final class EventPropagationVerifier {

    /** The same practical hierarchy bound used by notifier resolution. */
    private static final int MAX_WALK_DEPTH = 10;

    private EventPropagationVerifier() {}

    /**
     * Whether the selected notifier is one nothing can be outside of.
     * <p>
     * The Server object is not merely a notifier that happens to sit high up; it is the one node the
     * specification guarantees can see everything. OPC 10000-5 §8.3.2: "The Server Object serves as root
     * notifier, that is, its EventNotifier Attribute shall be set providing Events. <em>All Events of the
     * Server shall be accessible subscribing to the Events of the Server Object.</em>" A {@code shall} that
     * broad makes {@link Result.OutsideHierarchy} unreachable here by definition — there is no node in the
     * address space whose events are not accessible at the Server object.
     * <p>
     * So the walk must not be allowed to answer <em>that</em> question. It infers from modelled references,
     * and a server is free to deliver an event to the Server object while modelling none of the inverse
     * {@code HasNotifier} chain that would let the walk find its way back up — sparse address spaces and
     * browse views filtered by user permission both produce exactly that. The walk would then browse a clean
     * zero-parent answer, conclude {@code OutsideHierarchy}, and drop a tag that would have published,
     * telling the operator their source is outside a hierarchy that by construction contains everything.
     * <p>
     * Distinct from {@code NotifierResolver}'s deliberate refusal to <em>fall back</em> to the Server object,
     * which is a scope argument: choosing Server on an operator's behalf silently widens a tag from one area
     * to the whole plant. Nothing is being chosen here. The operator named this notifier, and the only
     * question is whether to believe the specification about what it can carry.
     */
    private static boolean isRootNotifier(final @NotNull NodeId notifier) {
        return NodeIds.Server.equals(notifier);
    }

    /**
     * The only question left to ask about a narrowing node once the notifier is the root: is it there?
     * <p>
     * <b>Why anything is asked at all.</b> {@link #isRootNotifier} settles the <em>hierarchy</em> question and
     * nothing else, but the walk it replaces was answering two questions rather than one. Alongside "does this
     * node's event path reach the selected notifier" it was incidentally answering "is this node something the
     * server has", because a node the server does not hold produces a browse failure rather than a clean
     * negative. Short-circuiting the walk dropped both, and §8.3.2 licenses dropping only the first: it
     * promises that every event of the server is accessible at the Server object, which says nothing whatever
     * about whether some node id the operator typed is an event source, or a node, or anything at all.
     * <p>
     * The gap that left is the one QA reported as EDG-894 P6. A query tag rooted at the Server object — the
     * natural choice for a plant-wide subscription — narrowed by a {@code sourceNode} or {@code conditionNode}
     * that is a typo, a stale id from a migrated configuration, or a plain variable was accepted without
     * anything being looked at. It subscribes cleanly, its where clause matches nothing, and it stays silent
     * for the life of the adapter with no event, log line or status naming it. That is indistinguishable from
     * a quiet plant, which is the worst thing a tag can be.
     * <p>
     * <b>Why the answer is {@link Result.Unverified} and never {@link Result.OutsideHierarchy}.</b> Absence is
     * not proof of a mistake: OPC 10000-9 §4.3 permits a server to keep its condition instances out of the
     * address space entirely and deliver them through events alone, and §5.7.3 requires the ConditionId to be
     * accepted regardless. So a missing node may be a working configuration on such a server. {@code Unverified}
     * keeps the tag subscribed — which is the whole of review-09 finding 4, and must not be undone — while
     * routing through the caller's {@code reportUnverifiedTag} so the operator is told which tag, which field
     * and which node id, and what to check first if it never publishes.
     */
    private static @NotNull CompletableFuture<Result> presentInAddressSpace(
            final @NotNull OpcUaClient client, final @NotNull NodeId node) {

        // The same browse the walk's first hop makes, so a server that permits one permits the other and this
        // check cannot fail for a reason the walk would not also have hit. What is read from it is different:
        // not who the parents are -- under the root notifier that is settled -- but whether the server
        // answered about this node at all. Good with zero references is a node that exists and simply has no
        // modelled event source, which is the ordinary case here and is fine.
        final BrowseDescription browse = new BrowseDescription(
                node,
                BrowseDirection.Inverse,
                NodeIds.HasEventSource,
                true,
                uint(0),
                uint(BrowseResultMask.All.getValue()));

        return Browsing.browseAll(client, browse)
                .<Result>thenApply(references -> new Result.Reachable())
                .exceptionally(throwable -> {
                    if (Browsing.nodeNotInAddressSpace(throwable)) {
                        return new Result.Unverified("the server does not expose " + node.toParseableString()
                                + " in its address space, so nothing can be confirmed to match this predicate");
                    }
                    return unverified(node, throwable);
                });
    }

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
        if (isRootNotifier(notifier)) {
            return presentInAddressSpace(client, conditionNode);
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
        // Named ahead of the root check rather than left to walkUpwards' own frontier test, which would
        // otherwise browse the Server object to establish that the Server object exists.
        if (sourceNode.equals(notifier)) {
            return CompletableFuture.completedFuture(new Result.Reachable());
        }
        if (isRootNotifier(notifier)) {
            return presentInAddressSpace(client, sourceNode);
        }
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
