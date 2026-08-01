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

import java.util.concurrent.CompletableFuture;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseResultMask;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Finds the node a condition tag's events are received from.
 * <p>
 * A condition is not itself an event notifier — {@code ConditionType} defines no {@code EventNotifier}
 * attribute, and events are only available from nodes whose {@code SubscribeToEvents} bit is set. So the
 * MonitoredItem goes on a notifier <em>above</em> the condition, and the condition is picked out of that
 * notifier's traffic by the filter's {@code ConditionId} predicate.
 * <p>
 * Resolution order, in decreasing preference:
 * <ol>
 *   <li>the {@code notifierNode} declared on the tag — the escape hatch for servers whose references cannot
 *       be walked;</li>
 *   <li>the first notifier reachable from the condition by {@code HasEventSource} / {@code HasNotifier};</li>
 *   <li>nothing — the tag cannot be subscribed.</li>
 * </ol>
 * There is deliberately no implicit fallback to the Server object. It is a notifier by convention and would
 * almost always work, which is exactly the problem: it would silently widen a tag from "this condition's
 * area" to "everything this server emits", leaving the filter as the only thing between an operator and the
 * whole plant's alarm traffic.
 */
public final class NotifierResolver {

    private NotifierResolver() {}

    /** How far to walk upward before giving up; deep enough for real hierarchies, bounded against cycles. */
    private static final int MAX_WALK_DEPTH = 10;

    /** The outcome of looking for a notifier. */
    public sealed interface Result {

        /** The node to place the MonitoredItem on, and how it was arrived at. */
        record Found(@NotNull NodeId notifier, @NotNull String how) implements Result {}

        /** No notifier could be determined; the tag must not be subscribed. */
        record NotFound(@NotNull String reason) implements Result {}
    }

    /**
     * Resolves the notifier for one condition tag.
     * <p>
     * Never completes exceptionally: a browse or read that fails is reported as {@link Result.NotFound}. This
     * runs during adapter start, where a thrown exception would abort the whole tag sequence.
     */
    public static @NotNull CompletableFuture<Result> resolve(
            final @NotNull OpcUaClient client,
            final @NotNull NodeId conditionNode,
            final @Nullable String declaredNotifier,
            final @NotNull String tagName) {

        if (declaredNotifier != null) {
            try {
                // Taken at its word, not verified against the device: a declaration exists precisely because
                // the device could not be relied on to answer. A wrong one surfaces as a failed subscription.
                return CompletableFuture.completedFuture(
                        new Result.Found(NodeId.parse(declaredNotifier), "declared on the tag"));
            } catch (final Exception e) {
                return CompletableFuture.completedFuture(new Result.NotFound("tag '" + tagName
                        + "' declares notifier '"
                        + declaredNotifier
                        + "', which is not a valid node id: "
                        + e.getMessage()));
            }
        }

        return walkUpwards(client, conditionNode, 0)
                .thenApply(found -> found == null
                        ? new Result.NotFound("no notifier could be found by walking up from tag '" + tagName
                                + "'. Set 'notifierNode' on the tag to name it explicitly")
                        : (Result) new Result.Found(found, "found by walking up from the condition"))
                .exceptionally(throwable -> new Result.NotFound(
                        "could not look for a notifier for tag '" + tagName + "': " + throwable.getMessage()));
    }

    /**
     * Walks {@code HasEventSource} / {@code HasNotifier} inverse references upward, returning the first node
     * that is genuinely a notifier.
     * <p>
     * Inverse because the references point downward — a notifier <em>has</em> event sources beneath it — so
     * getting from a condition to its notifier means following them backwards.
     */
    private static @NotNull CompletableFuture<NodeId> walkUpwards(
            final @NotNull OpcUaClient client, final @NotNull NodeId from, final int depth) {

        if (depth >= MAX_WALK_DEPTH) {
            return CompletableFuture.completedFuture(null);
        }

        final BrowseDescription browse = new BrowseDescription(
                from,
                BrowseDirection.Inverse,
                NodeIds.HasEventSource,
                true, // include HasNotifier, which is a subtype of HasEventSource
                uint(NodeClass.Object.getValue()),
                uint(BrowseResultMask.All.getValue()));

        return client.browseAsync(browse).thenCompose(result -> {
            final ReferenceDescription[] references = result.getReferences();
            if (references == null || references.length == 0) {
                return CompletableFuture.completedFuture(null);
            }
            return firstNotifierAmong(client, references, 0, depth);
        });
    }

    /**
     * Takes the first candidate that is a notifier; otherwise keeps walking up from it.
     * <p>
     * "First" is the nearest one, which is what a condition tag wants: the narrowest notifier that can see it,
     * rather than the broadest.
     */
    private static @NotNull CompletableFuture<NodeId> firstNotifierAmong(
            final @NotNull OpcUaClient client,
            final @NotNull ReferenceDescription @NotNull [] references,
            final int index,
            final int depth) {

        if (index >= references.length) {
            return CompletableFuture.completedFuture(null);
        }
        final NodeId candidate = references[index]
                .getNodeId()
                .toNodeId(client.getNamespaceTable())
                .orElse(null);
        if (candidate == null) {
            return firstNotifierAmong(client, references, index + 1, depth);
        }

        return isNotifier(client, candidate).thenCompose(notifier -> {
            if (notifier) {
                return CompletableFuture.completedFuture(candidate);
            }
            // Not a notifier itself: it may still sit beneath one, so keep going up through it.
            return walkUpwards(client, candidate, depth + 1)
                    .thenCompose(fromAbove -> fromAbove != null
                            ? CompletableFuture.completedFuture(fromAbove)
                            : firstNotifierAmong(client, references, index + 1, depth));
        });
    }

    /**
     * Whether a node's {@code EventNotifier} attribute has the {@code SubscribeToEvents} bit set — the only
     * thing that makes a node a valid target for an event MonitoredItem.
     */
    private static @NotNull CompletableFuture<Boolean> isNotifier(
            final @NotNull OpcUaClient client, final @NotNull NodeId nodeId) {

        final ReadValueId read = new ReadValueId(nodeId, AttributeId.EventNotifier.uid(), null, null);
        return client.readAsync(0.0, TimestampsToReturn.Neither, java.util.List.of(read))
                .thenApply(response -> {
                    final DataValue[] results = response.getResults();
                    if (results == null || results.length == 0) {
                        return false;
                    }
                    final Object value = results[0].value().value();
                    if (!(value instanceof final Number bits)) {
                        return false;
                    }
                    return (bits.intValue() & 0x01) != 0;
                })
                .exceptionally(throwable -> false);
    }
}
