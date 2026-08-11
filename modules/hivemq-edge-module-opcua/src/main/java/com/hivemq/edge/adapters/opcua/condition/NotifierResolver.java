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

import java.util.List;
import java.util.Optional;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 *   <li>the first notifier reachable from the condition by {@code HasCondition} to its ConditionSource, then
 *       {@code HasEventSource} / {@code HasNotifier} upward;</li>
 *   <li>nothing — the tag cannot be subscribed.</li>
 * </ol>
 * <p>
 * <b>The walk starts at the ConditionSource, not at the condition.</b> It is the source that hangs in the
 * notifier hierarchy — OPC 10000-9 §5.12: "Each ConditionSource shall be the target of a HasEventSource
 * Reference or a sub type of HasEventSource" — while the condition hangs off the <em>source</em> by
 * {@code HasCondition}. §6.3 spells the traversal out from the client's side: find ConditionSources by
 * {@code HasEventSource}, then their conditions by {@code HasCondition}. Going up from a condition therefore
 * means reversing both legs, and browsing {@code HasEventSource} from the condition itself returns nothing on
 * a server laid out as the specification prescribes.
 * There is deliberately no implicit fallback to the Server object — and the reason is a scope objection, not
 * a doubt about whether it would work. It would: OPC 10000-5 §8.3.2 is as strong a guarantee as this area
 * offers, "The Server Object serves as root notifier, that is, its EventNotifier Attribute shall be set
 * providing Events. All Events of the Server shall be accessible subscribing to the Events of the Server
 * Object." That is precisely the problem. Falling back to it would silently widen a tag from "this
 * condition's area" to "everything this server emits", leaving the filter as the only thing between an
 * operator and the whole plant's alarm traffic — a decision the tag's author should make explicitly by
 * naming {@code notifierNode}, not one Edge should make on their behalf when a walk comes up empty.
 * <p>
 * A REFRESH tag does subscribe to the Server object, and legitimately so: its purpose is the
 * subscription-wide refresh bracket, which is server-wide by definition rather than scoped to one area.
 */
public final class NotifierResolver {

    private static final @NotNull Logger log = LoggerFactory.getLogger(NotifierResolver.class);

    private NotifierResolver() {}

    /** How far to walk upward before giving up; deep enough for real hierarchies, bounded against cycles. */
    private static final int MAX_WALK_DEPTH = 10;

    /**
     * The {@code SubscribeToEvents} bit of the {@code EventNotifier} attribute (OPC 10000-3 §8.59) — the one
     * thing that makes a node a valid target for an event monitored item.
     */
    private static final int SUBSCRIBE_TO_EVENTS = 0x01;

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

        return walkFromConditionSources(client, conditionNode)
                .thenCompose(found -> found != null
                        ? CompletableFuture.completedFuture(found)
                        // No ConditionSource, or none of them led anywhere. Servers do attach HasEventSource
                        // to the condition directly, which the specification does not describe but which
                        // costs one browse to accommodate -- and the alternative is refusing to subscribe.
                        : walkUpwards(client, conditionNode, 0))
                .thenApply(found -> found == null
                        ? new Result.NotFound("no notifier could be found by walking up from tag '" + tagName
                                + "'. Set 'notifierNode' on the tag to name it explicitly")
                        : (Result) new Result.Found(found, "found by walking up from the condition"))
                // The per-tag boundary for a browse failure. Browsing propagates one now instead of
                // returning an empty list, so this handler says the walk could not be performed rather than
                // reporting that it found nothing -- two answers that pointed an operator at opposite
                // problems, one at their tag's node id and the other at the connection.
                .exceptionally(throwable -> new Result.NotFound("could not look for a notifier for tag '" + tagName
                        + "': " + Browsing.describeException(throwable)));
    }

    /**
     * Whether a node Edge was told to subscribe to can actually deliver events.
     * <p>
     * Two kinds of tag name their event target directly rather than having it walked to: an
     * {@code EVENT_SUBSCRIPTION} tag, whose {@code node} <em>is</em> the notifier, and a {@code CONDITION}
     * tag carrying an explicit {@code notifierNode}. Neither was checked. A variable, a plain object with no
     * {@code SubscribeToEvents} bit, or a typo went straight to monitored-item synchronization — where,
     * depending on the server, it fails the whole batch or is accepted into a tag that subscribes cleanly and
     * then stays silent forever. The second is the bad one: nothing distinguishes it from an alarm that
     * simply has not fired.
     * <p>
     * <b>Only a definite answer rejects.</b> A server that declines to say — the read fails, or answers with
     * a bad status — leaves the tag alone with a warning, because refusing on silence would break servers
     * that restrict attribute reads while the node is perfectly good. What rejects is the server saying
     * plainly that this node is a Variable, or that its {@code EventNotifier} has the bit clear.
     *
     * @return the reason the node cannot be subscribed, or empty when it can be (or when the server would
     *         not say).
     */
    public static @NotNull CompletableFuture<Optional<String>> checkSubscribable(
            final @NotNull OpcUaClient client, final @NotNull NodeId nodeId, final @NotNull String tagName) {

        final List<ReadValueId> reads = List.of(
                new ReadValueId(nodeId, AttributeId.NodeClass.uid(), null, null),
                new ReadValueId(nodeId, AttributeId.EventNotifier.uid(), null, null));

        return client.readAsync(0.0, TimestampsToReturn.Neither, reads)
                .thenApply(response -> {
                    final DataValue[] results = response.getResults();
                    if (results == null || results.length < 2) {
                        log.warn(
                                "Tag '{}': the server did not answer the NodeClass/EventNotifier read for {}, so "
                                        + "whether it can deliver events was not checked. Subscribing anyway.",
                                tagName,
                                nodeId);
                        return Optional.<String>empty();
                    }
                    final Optional<String> wrongClass = rejectByNodeClass(results[0], nodeId, tagName);
                    if (wrongClass.isPresent()) {
                        return wrongClass;
                    }
                    return rejectByEventNotifier(results[1], nodeId, tagName);
                })
                .exceptionally(throwable -> {
                    log.warn(
                            "Tag '{}': could not read NodeClass/EventNotifier of {} ({}), so whether it can "
                                    + "deliver events was not checked. Subscribing anyway.",
                            tagName,
                            nodeId,
                            Browsing.describeException(throwable));
                    return Optional.empty();
                });
    }

    /**
     * Rejects a node whose class cannot carry the {@code EventNotifier} attribute at all.
     * <p>
     * OPC 10000-3 §7.17: the source of a {@code HasEventSource} "shall be an Object or View", and those are
     * the two classes the specification gives an {@code EventNotifier} attribute. A Variable named as a
     * notifier is the commonest form of this mistake — a value tag's node id pasted into an event tag.
     */
    private static @NotNull Optional<String> rejectByNodeClass(
            final @NotNull DataValue result, final @NotNull NodeId nodeId, final @NotNull String tagName) {

        if (result.statusCode() != null && result.statusCode().isBad()) {
            log.warn(
                    "Tag '{}': the server would not report the NodeClass of {} ({}); not treating that as a "
                            + "reason to refuse the tag.",
                    tagName,
                    nodeId,
                    result.statusCode());
            return Optional.empty();
        }
        final Object value = result.value().value();
        final NodeClass nodeClass = asNodeClass(value);
        if (nodeClass == null) {
            return Optional.empty();
        }
        if (nodeClass == NodeClass.Object || nodeClass == NodeClass.View) {
            return Optional.empty();
        }
        return Optional.of("its event target " + nodeId + " is a " + nodeClass
                + ", and only an Object or a View can deliver events (OPC 10000-3 §7.17). For a CONDITION tag "
                + "name the alarm in 'node' and let Edge find the notifier, or name a real notifier in "
                + "'notifierNode'; for an EVENT_SUBSCRIPTION tag 'node' must be the notifier itself");
    }

    /** Rejects a node whose {@code EventNotifier} attribute says it does not accept event subscriptions. */
    private static @NotNull Optional<String> rejectByEventNotifier(
            final @NotNull DataValue result, final @NotNull NodeId nodeId, final @NotNull String tagName) {

        if (result.statusCode() != null && result.statusCode().isBad()) {
            log.warn(
                    "Tag '{}': the server would not report the EventNotifier attribute of {} ({}); not treating "
                            + "that as a reason to refuse the tag.",
                    tagName,
                    nodeId,
                    result.statusCode());
            return Optional.empty();
        }
        final Object value = result.value().value();
        if (!(value instanceof final Number bits)) {
            // Present but not a number, or absent altogether. Not a statement that events are unavailable.
            return Optional.empty();
        }
        if ((bits.intValue() & SUBSCRIBE_TO_EVENTS) != 0) {
            return Optional.empty();
        }
        return Optional.of("its event target " + nodeId + " has the SubscribeToEvents bit clear in its "
                + "EventNotifier attribute, so the server will not deliver events from it. Name a node that "
                + "is an event notifier, or leave 'notifierNode' empty to have Edge walk to one");
    }

    /** The {@code NodeClass} a read returned, or null when the server sent something unrecognisable. */
    private static @Nullable NodeClass asNodeClass(final @Nullable Object value) {
        if (value instanceof final NodeClass nodeClass) {
            return nodeClass;
        }
        if (value instanceof final Number encoded) {
            return NodeClass.from(encoded.intValue());
        }
        return null;
    }

    /**
     * Steps from the condition to its ConditionSource(s) by inverse {@code HasCondition}, then walks up from
     * each until one reaches a notifier.
     * <p>
     * Inverse because {@code HasCondition} points from the source <em>to</em> the condition (§5.12, Table 136:
     * its inverse name is {@code IsConditionOf}). A condition may be referenced by more than one source, so
     * each is tried in turn rather than only the first.
     * <p>
     * The node class mask admits Variables as well as Objects: §6.3's own example hangs conditions off a
     * Variable, {@code LevelMeasurement}, and §5.12 allows "an Object, Variable or Method Node" as the source
     * of the reference.
     */
    private static @NotNull CompletableFuture<NodeId> walkFromConditionSources(
            final @NotNull OpcUaClient client, final @NotNull NodeId conditionNode) {

        final BrowseDescription browse = new BrowseDescription(
                conditionNode,
                BrowseDirection.Inverse,
                NodeIds.HasCondition,
                false, // HasCondition is concrete and has no subtypes to include
                uint(NodeClass.Object.getValue() | NodeClass.Variable.getValue() | NodeClass.Method.getValue()),
                uint(BrowseResultMask.All.getValue()));

        return Browsing.browseAll(client, browse)
                .thenCompose(references -> references.isEmpty()
                        ? CompletableFuture.completedFuture(null)
                        : firstNotifierAboveAny(client, references, 0));
    }

    /** Walks up from each ConditionSource in turn, taking the first notifier any of them reaches. */
    private static @NotNull CompletableFuture<NodeId> firstNotifierAboveAny(
            final @NotNull OpcUaClient client, final @NotNull List<ReferenceDescription> sources, final int index) {

        if (index >= sources.size()) {
            return CompletableFuture.completedFuture(null);
        }
        final NodeId source = sources.get(index)
                .getNodeId()
                .toNodeId(client.getNamespaceTable())
                .orElse(null);
        if (source == null) {
            return firstNotifierAboveAny(client, sources, index + 1);
        }

        // The source may itself be the notifier: HasNotifier is a subtype of HasEventSource, so a node can be
        // both a ConditionSource and an event notifier (§6.2). Testing it before walking past it keeps the
        // result the nearest notifier rather than the next one up.
        return isNotifier(client, source)
                .thenCompose(notifier -> notifier
                        ? CompletableFuture.completedFuture(source)
                        : walkUpwards(client, source, 0)
                                .thenCompose(found -> found != null
                                        ? CompletableFuture.completedFuture(found)
                                        : firstNotifierAboveAny(client, sources, index + 1)));
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

        // Node class mask 0 means "all classes". A View can be an event notifier just as an Object can (OPC
        // 10000-3 §7.17: the source of a HasEventSource "shall be an Object or View"), so masking to Objects
        // would drop a View-organised area before isNotifier() could test it. The EventNotifier read below is
        // the authoritative test anyway, which makes filtering by class here redundant as well as wrong.
        final BrowseDescription browse = new BrowseDescription(
                from,
                BrowseDirection.Inverse,
                NodeIds.HasEventSource,
                true, // include HasNotifier, which is a subtype of HasEventSource
                uint(0),
                uint(BrowseResultMask.All.getValue()));

        return Browsing.browseAll(client, browse)
                .thenCompose(references -> references.isEmpty()
                        ? CompletableFuture.completedFuture(null)
                        : firstNotifierAmong(client, references, 0, depth));
    }

    /**
     * Takes the first candidate that is a notifier; otherwise keeps walking up from it.
     * <p>
     * "First" is the nearest one, which is what a condition tag wants: the narrowest notifier that can see it,
     * rather than the broadest.
     */
    private static @NotNull CompletableFuture<NodeId> firstNotifierAmong(
            final @NotNull OpcUaClient client,
            final @NotNull List<ReferenceDescription> references,
            final int index,
            final int depth) {

        if (index >= references.size()) {
            return CompletableFuture.completedFuture(null);
        }
        final NodeId candidate = references
                .get(index)
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
