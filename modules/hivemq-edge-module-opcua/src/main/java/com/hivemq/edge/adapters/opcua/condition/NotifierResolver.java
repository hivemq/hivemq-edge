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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
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
 *   <li>the <em>nearest</em> notifier reachable from the condition by {@code HasCondition} to its
 *       ConditionSource, then {@code HasEventSource} / {@code HasNotifier} upward — nearest rather than
 *       first-found, which is why the search is breadth-first; see {@link #nearestNotifier};</li>
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

    /**
     * How many levels of the hierarchy to examine before giving up — deep enough for real address spaces, and
     * a backstop against one that is pathologically deep. Cycles are handled by the visited set rather than
     * by this bound; see {@link #nearestNotifier}.
     */
    private static final int MAX_WALK_DEPTH = 10;

    /**
     * The {@code SubscribeToEvents} bit of the {@code EventNotifier} attribute (OPC 10000-3 §8.59) — the one
     * thing that makes a node a valid target for an event monitored item.
     */
    private static final int SUBSCRIBE_TO_EVENTS = 0x01;

    /**
     * The statuses that settle the question rather than dodging it.
     * <p>
     * {@code Bad_NodeIdUnknown} and {@code Bad_NodeIdInvalid} are the server saying the configured target does
     * not identify a node — the first that no such node exists, the second that the id is not well formed for
     * this server. Neither is a matter of permission or timing, and neither will read differently on a
     * restart.
     * <p>
     * {@code Bad_AttributeIdInvalid} is the same answer arrived at from the other side. It means the node does
     * not have the attribute being read, and {@code EventNotifier} is defined for exactly the node classes
     * that can deliver events — OPC 10000-3 §7.17 gives it to Objects and Views, and it is mandatory on both.
     * So a node that has no {@code EventNotifier} is not a notifier, whatever else it is. This is also the
     * one entry that only applies to the second read: a NodeClass read that answers
     * {@code Bad_AttributeIdInvalid} would mean something has gone very strangely wrong, since every node has
     * a NodeClass.
     */
    private static final @NotNull Set<Long> DEFINITELY_NOT_A_NOTIFIER =
            Set.of(StatusCodes.Bad_NodeIdUnknown, StatusCodes.Bad_NodeIdInvalid, StatusCodes.Bad_AttributeIdInvalid);

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
                // Taken at its word *here*, and only here: resolution does not browse to check a node the
                // operator named, because a declaration exists precisely for the server whose references
                // could not be walked. That is the whole of this step's claim, and the qualification matters
                // -- the tag is not subscribed unverified. verifyCondition() preflights the same node with
                // checkSubscribable() straight afterwards, and drops the tag when the server says definitely
                // not a notifier, so a typo no longer subscribes cleanly and then stays silent forever.
                // Read without that qualification, this comment says what the user guide wrongly says.
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
                        : walkUpwardsFrom(client, conditionNode))
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
     * <b>Only a definite answer rejects.</b> A server that <em>declines</em> to say — the read fails, or comes
     * back {@code Bad_UserAccessDenied} — leaves the tag alone with a warning, because refusing on silence
     * would break servers that restrict attribute reads while the node is perfectly good.
     * <p>
     * Not every bad status is silence, though, and treating them alike was the defect here.
     * {@code Bad_NodeIdUnknown} and {@code Bad_NodeIdInvalid} are not the server withholding an attribute;
     * they are the server stating that the configured target does not identify a node at all. Admitting those
     * let a typo through the one preflight that exists to catch it, and the tag then either failed the
     * monitored-item batch — taking healthy tags with it, on some servers — or subscribed cleanly and stayed
     * silent forever, which is indistinguishable from an alarm that has not fired. See
     * {@link #DEFINITELY_NOT_A_NOTIFIER}.
     *
     * @param field the tag definition field this node came from, named in the rejection so an operator knows
     *              which line to correct.
     * @return the reason the node cannot be subscribed, or empty when it can be (or when the server would
     *         not say).
     */
    public static @NotNull CompletableFuture<Optional<String>> checkSubscribable(
            final @NotNull OpcUaClient client,
            final @NotNull NodeId nodeId,
            final @NotNull String tagName,
            final @NotNull String field) {

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
                    final Optional<String> wrongClass = rejectByNodeClass(results[0], nodeId, tagName, field);
                    if (wrongClass.isPresent()) {
                        return wrongClass;
                    }
                    return rejectByEventNotifier(results[1], nodeId, tagName, field);
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
            final @NotNull DataValue result,
            final @NotNull NodeId nodeId,
            final @NotNull String tagName,
            final @NotNull String field) {

        final Optional<String> definite = rejectByStatus(result, nodeId, tagName, field, "NodeClass");
        if (definite.isPresent()) {
            return definite;
        }
        if (result.statusCode() != null && result.statusCode().isBad()) {
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
        return Optional.of("its event target " + nodeId + " (from '" + field + "') is a " + nodeClass
                + ", and only an Object or a View can deliver events (OPC 10000-3 §7.17). For a CONDITION tag "
                + "name the alarm in 'node' and let Edge find the notifier, or name a real notifier in "
                + "'notifierNode'; for an EVENT_SUBSCRIPTION tag 'node' must be the notifier itself");
    }

    /** Rejects a node whose {@code EventNotifier} attribute says it does not accept event subscriptions. */
    private static @NotNull Optional<String> rejectByEventNotifier(
            final @NotNull DataValue result,
            final @NotNull NodeId nodeId,
            final @NotNull String tagName,
            final @NotNull String field) {

        final Optional<String> definite = rejectByStatus(result, nodeId, tagName, field, "EventNotifier attribute");
        if (definite.isPresent()) {
            return definite;
        }
        if (result.statusCode() != null && result.statusCode().isBad()) {
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
        return Optional.of("its event target " + nodeId + " (from '" + field + "') has the SubscribeToEvents "
                + "bit clear in its EventNotifier attribute, so the server will not deliver events from it. "
                + "Name a node that is an event notifier, or leave 'notifierNode' empty to have Edge walk to one");
    }

    /**
     * Separates a server that <em>cannot</em> answer from one that <em>will not</em>.
     * <p>
     * Every bad status used to be read as the latter and waved through, which is defensible for exactly one
     * of them. {@code Bad_UserAccessDenied} says the session may not read the attribute and says nothing
     * about the node; so does a timeout, or a transport failure. Admitting those is the right trade, because
     * refusing a tag because a server restricts attribute reads would break perfectly good configurations.
     * <p>
     * {@link #DEFINITELY_NOT_A_NOTIFIER} is the other kind, and the distinction is not a nuance: those
     * statuses are the server stating that the configured target does not identify a usable node. Waving one
     * through defeats the entire purpose of this preflight — a typo reaches monitored-item synchronization,
     * where it either fails the whole batch or produces a tag that subscribes cleanly and never publishes.
     *
     * @param attribute the attribute being read, for the message.
     * @return the rejection when the status is a definite answer, empty when it is silence or success.
     */
    private static @NotNull Optional<String> rejectByStatus(
            final @NotNull DataValue result,
            final @NotNull NodeId nodeId,
            final @NotNull String tagName,
            final @NotNull String field,
            final @NotNull String attribute) {

        final StatusCode status = result.statusCode();
        if (status == null || !status.isBad()) {
            return Optional.empty();
        }
        if (DEFINITELY_NOT_A_NOTIFIER.contains(status.value())) {
            return Optional.of("its event target " + nodeId + " (from '" + field + "') was refused by the server "
                    + "with " + status + " when reading its " + attribute + ". That is not the server declining "
                    + "to answer: it is the server saying this is not a node it can deliver events from. Check "
                    + "'" + field + "' for a typo, a stale node id, or a namespace index that has changed");
        }
        log.warn(
                "Tag '{}': the server would not report the {} of {} ({}); not treating that as a reason to "
                        + "refuse the tag.",
                tagName,
                attribute,
                nodeId,
                status);
        return Optional.empty();
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
     * Steps from the condition to its ConditionSource(s) by inverse {@code HasCondition}, then searches
     * upward from all of them together.
     * <p>
     * Inverse because {@code HasCondition} points from the source <em>to</em> the condition (§5.12, Table 136:
     * its inverse name is {@code IsConditionOf}). A condition may be referenced by more than one source, and
     * all of them are the first level of the search rather than one being exhausted before the next is looked
     * at — see {@link #nearestNotifier}.
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

        return Browsing.browseAll(client, browse).thenCompose(references -> {
            // The sources are the first level, tested before anything above them: HasNotifier is a subtype of
            // HasEventSource, so a node can be both a ConditionSource and an event notifier (§6.2), and one
            // that is needs no walking past.
            final List<NodeId> sources = toNodeIds(client, references);
            final Set<NodeId> visited = new LinkedHashSet<>(sources);
            visited.add(conditionNode);
            return nearestNotifier(client, sources, visited, 0);
        });
    }

    /**
     * Searches upward from one node, which is <em>not</em> itself a candidate.
     * <p>
     * The compatibility path, for servers that attach {@code HasEventSource} to the condition directly. The
     * condition is excluded from the search because {@code ConditionType} defines no {@code EventNotifier}
     * attribute at all, so it can never be the answer — only its parents can.
     */
    private static @NotNull CompletableFuture<NodeId> walkUpwardsFrom(
            final @NotNull OpcUaClient client, final @NotNull NodeId start) {

        final Set<NodeId> visited = new LinkedHashSet<>();
        visited.add(start);
        // Seeded at one, not zero: the parentsOf below has already taken the first upward hop, so the
        // frontier handed on is a level above the start rather than the start itself. Seeding zero would
        // give this path an eleventh hop that the ConditionSource path does not have.
        return parentsOf(client, List.of(start), 0, new ArrayList<>(), visited)
                .thenCompose(frontier -> nearestNotifier(client, frontier, visited, 1));
    }

    /**
     * The nearest notifier at or above a level of the hierarchy, searched one whole level at a time.
     * <p>
     * <b>Breadth-first, because the resolver's promise is the</b> <em>nearest</em> <b>notifier and a server's
     * reference order is not a distance ordering.</b> Depth-first — following one candidate's entire ancestry
     * before looking at the candidate beside it — returns whichever branch the server happened to list first,
     * so a topology like
     *
     * <pre>
     *   condition --IsConditionOf--&gt; source A --up--&gt; area A (notifier)
     *             \-IsConditionOf--&gt; source B (notifier)
     * </pre>
     *
     * answers "area A" when the server lists A first and "source B" when it lists B first. Both are notifiers
     * that can see the condition, so nothing fails and nothing is logged; the tag simply subscribes one level
     * broader than it needed to, at the server's whim. That costs server-side filter work, and worse, it can
     * pick a wide notifier the session is not permitted to subscribe to while a narrow permitted one was
     * available — turning a valid topology into a failed tag on reference order alone.
     * <p>
     * {@code visited} is what makes the bound below a safeguard rather than the only thing standing between
     * this and a cycle. Without it a diamond re-browses a shared ancestor once per path into it, and a genuine
     * cycle costs a full branching-factor-to-the-tenth-power sweep before {@link #MAX_WALK_DEPTH} stops it.
     * It also has to be a set of nodes already <em>enqueued</em> rather than already tested, or the two paths
     * into a diamond would both add the ancestor to the same level.
     * <p>
     * A plain {@link LinkedHashSet} is safe despite the stages running on Milo's threads: every step here is
     * chained with {@code thenCompose}, so no two touch it at once and each sees the last one's writes.
     * <p>
     * <b>{@code depth} counts upward hops already taken to reach this frontier, and the bound is tested
     * against the frontier rather than applied before it.</b> The distinction is the whole of review-05
     * finding 8. Written {@code depth >= MAX_WALK_DEPTH}, with the ConditionSources seeded at zero, a frontier
     * arriving at depth ten was discarded <em>without its {@code EventNotifier} attribute ever being read</em>
     * — so a notifier exactly ten hops above a ConditionSource was rejected, and the resolver told the
     * operator to configure {@code notifierNode} for a topology the documented bound admits. The compatibility
     * path did not have the defect, because its first level is reached by a {@code parentsOf} call made
     * before this method is entered; the two therefore disagreed about their own limit by one, which is worse
     * than either answer.
     * <p>
     * So each caller states how far its frontier already is: {@link #walkFromConditionSources} seeds zero,
     * since the sources are the search's origin rather than a step above it, and {@link #walkUpwardsFrom}
     * seeds one, having already taken a step to build its frontier. Both then test ten upward hops, which is
     * what {@link #MAX_WALK_DEPTH} has always been documented to mean.
     */
    private static @NotNull CompletableFuture<NodeId> nearestNotifier(
            final @NotNull OpcUaClient client,
            final @NotNull List<NodeId> frontier,
            final @NotNull Set<NodeId> visited,
            final int depth) {

        if (frontier.isEmpty() || depth > MAX_WALK_DEPTH) {
            return CompletableFuture.completedFuture(null);
        }
        return firstNotifierIn(client, frontier, 0)
                .thenCompose(found -> found != null
                        ? CompletableFuture.completedFuture(found)
                        : parentsOf(client, frontier, 0, new ArrayList<>(), visited)
                                .thenCompose(next -> nearestNotifier(client, next, visited, depth + 1)));
    }

    /**
     * The first node in a level that is itself a notifier, or null when none of them is.
     * <p>
     * Within one level the server's order is the only tiebreak there is, and any of them is equally near — so
     * unlike the across-level case it carries no preference worth overriding.
     */
    private static @NotNull CompletableFuture<NodeId> firstNotifierIn(
            final @NotNull OpcUaClient client, final @NotNull List<NodeId> frontier, final int index) {

        if (index >= frontier.size()) {
            return CompletableFuture.completedFuture(null);
        }
        final NodeId candidate = frontier.get(index);
        return isNotifier(client, candidate)
                .thenCompose(notifier -> notifier
                        ? CompletableFuture.completedFuture(candidate)
                        : firstNotifierIn(client, frontier, index + 1));
    }

    /**
     * Every not-yet-seen node one {@code HasEventSource} step above a level, in server order.
     * <p>
     * Inverse because the references point downward — a notifier <em>has</em> event sources beneath it — so
     * getting from a condition to its notifier means following them backwards.
     */
    private static @NotNull CompletableFuture<List<NodeId>> parentsOf(
            final @NotNull OpcUaClient client,
            final @NotNull List<NodeId> frontier,
            final int index,
            final @NotNull List<NodeId> collected,
            final @NotNull Set<NodeId> visited) {

        if (index >= frontier.size()) {
            return CompletableFuture.completedFuture(collected);
        }

        // Node class mask 0 means "all classes". A View can be an event notifier just as an Object can (OPC
        // 10000-3 §7.17: the source of a HasEventSource "shall be an Object or View"), so masking to Objects
        // would drop a View-organised area before isNotifier() could test it. The EventNotifier read is the
        // authoritative test anyway, which makes filtering by class here redundant as well as wrong.
        final BrowseDescription browse = new BrowseDescription(
                frontier.get(index),
                BrowseDirection.Inverse,
                NodeIds.HasEventSource,
                true, // include HasNotifier, which is a subtype of HasEventSource
                uint(0),
                uint(BrowseResultMask.All.getValue()));

        return Browsing.browseAll(client, browse).thenCompose(references -> {
            for (final NodeId parent : toNodeIds(client, references)) {
                if (visited.add(parent)) {
                    collected.add(parent);
                }
            }
            return parentsOf(client, frontier, index + 1, collected, visited);
        });
    }

    /**
     * The references that name a node this client can address, in server order.
     * <p>
     * A reference whose {@code ExpandedNodeId} names a namespace absent from this session's table is dropped
     * rather than reported: it identifies a node on another server, which cannot be subscribed to here.
     */
    private static @NotNull List<NodeId> toNodeIds(
            final @NotNull OpcUaClient client, final @NotNull List<ReferenceDescription> references) {

        final List<NodeId> nodeIds = new ArrayList<>(references.size());
        for (final ReferenceDescription reference : references) {
            reference.getNodeId().toNodeId(client.getNamespaceTable()).ifPresent(nodeIds::add);
        }
        return nodeIds;
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
