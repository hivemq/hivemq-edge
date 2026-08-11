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
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseResultMask;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodResult;
import org.eclipse.milo.opcua.stack.core.types.structured.CallResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Requests a condition state transition on the server.
 * <p>
 * Acknowledging is a <em>method call</em>, not a write. A condition's acked state is not a variable Edge can
 * assign to — the server owns the state machine, and moving it is an operation with arguments. That is the one
 * structural difference between a southbound condition write and an ordinary tag write, and it is why this
 * path calls rather than writes.
 * <p>
 * Nothing is stored here. The {@code EventId} arrives in the command, is passed to the server, and is
 * forgotten: Edge relays the transition request and does not track which transitions are outstanding.
 */
public final class ConditionUpdateWriter {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ConditionUpdateWriter.class);

    /** The browse name of the alarm's shelving state machine, fixed by the specification. */
    private static final @NotNull String SHELVING_STATE = "ShelvingState";

    /**
     * The two methods whose type-level MethodId the specification names outright.
     * <p>
     * A server need not expose condition instances in the AddressSpace at all, in which case there is no
     * instance to browse for a method node. OPC 10000-9 §5.5.4 and §5.5.5 resolve that for Enable and Disable
     * by fixing the id to use: "Since Condition instances are not required to be defined in the AddressSpace,
     * the MethodId that is passed in the Call Service shall be the NodeId of the Disable Method on the
     * ConditionType."
     * <p>
     * <b>Only these two.</b> An exhaustive sweep of Part 9 finds three statements about which MethodId to
     * pass, and the third describes the ordinary instance case for shelving. The other twelve methods say a
     * client may use the ConditionId as the <em>ObjectId</em>, but never say which MethodId accompanies it —
     * so there is no spec-blessed id to fall back to, and guessing one from a vendor library's constant table
     * would be inventing a contract the specification does not state. Those keep browsing the instance.
     * <p>
     * The ObjectId is <em>not</em> changed to the type: both clauses say "The Method cannot be called with an
     * ObjectId of the ConditionType Node". It stays the ConditionId; only the MethodId becomes type-level.
     */
    private static final @NotNull Map<ConditionUpdate.Method, NodeId> SPECIFIED_TYPE_LEVEL_METHODS = Map.of(
            ConditionUpdate.Method.ENABLE, NodeIds.ConditionType_Enable,
            ConditionUpdate.Method.DISABLE, NodeIds.ConditionType_Disable);

    private ConditionUpdateWriter() {}

    /**
     * Invokes the OPC UA method for the requested transition.
     *
     * @param client          the connected client.
     * @param conditionNodeId the condition the method is invoked on — the object id of the call.
     * @param update          the requested transition.
     * @return the status of the call: good when the server accepted the transition. Completes exceptionally
     *         only if the call itself could not be made.
     */
    public static @NotNull CompletableFuture<StatusCode> requestTransition(
            final @NotNull OpcUaClient client,
            final @NotNull NodeId conditionNodeId,
            final @NotNull ConditionUpdate update) {

        // Shelving methods hang off the condition's ShelvingState object rather than the condition itself, so
        // the object a call is made on is not always the condition.
        return resolveTargetObject(client, conditionNodeId, update.method())
                .thenCompose(objectNodeId -> resolveCommentedMethodOn(client, objectNodeId, update, conditionNodeId)
                        .thenCompose(commented -> commented != null
                                ? callCommented(client, objectNodeId, commented, update)
                                : callBase(
                                        client,
                                        objectNodeId,
                                        conditionNodeId,
                                        update,
                                        commentedVariantAlreadyBrowsed(update))));
    }

    /**
     * Whether {@link #resolveCommentedMethodOn} has already browsed for the {@code "2"} variant and not
     * found it, so {@link #callBase}'s own fallback must not browse for it a second time.
     * <p>
     * It browses exactly when there is a comment to carry, the base form does not already take one, and the
     * specification defines a variant at all — the same three conditions, stated once.
     */
    private static boolean commentedVariantAlreadyBrowsed(final @NotNull ConditionUpdate update) {
        return update.comment() != null
                && update.method().arguments() != ConditionUpdate.Method.Arguments.EVENT_AND_COMMENT
                && update.method().commentedBrowseName() != null;
    }

    /**
     * Calls the {@code "2"} variant, which carries the user's comment.
     */
    private static @NotNull CompletableFuture<StatusCode> callCommented(
            final @NotNull OpcUaClient client,
            final @NotNull NodeId objectNodeId,
            final @NotNull NodeId methodNodeId,
            final @NotNull ConditionUpdate update) {

        final CallMethodRequest request =
                new CallMethodRequest(objectNodeId, methodNodeId, commentedArgumentsFor(update));
        return client.callAsync(List.of(request)).thenApply(ConditionUpdateWriter::statusOf);
    }

    /** Calls the base method — the original form, which for most methods cannot carry a comment. */
    private static @NotNull CompletableFuture<StatusCode> callBase(
            final @NotNull OpcUaClient client,
            final @NotNull NodeId objectNodeId,
            final @NotNull NodeId conditionNodeId,
            final @NotNull ConditionUpdate update,
            final boolean commentedVariantKnownAbsent) {

        final Variant[] arguments = argumentsFor(update);
        return resolveMethodOn(client, objectNodeId, update.method()).thenCompose(browsed -> {
            if (browsed != null) {
                final CallMethodRequest request = new CallMethodRequest(objectNodeId, browsed, arguments);
                return client.callAsync(List.of(request)).thenApply(ConditionUpdateWriter::statusOf);
            }
            final NodeId specified = SPECIFIED_TYPE_LEVEL_METHODS.get(update.method());
            if (specified != null) {
                log.debug(
                        "Condition {} exposes no {} method; calling the ConditionType's method id as OPC "
                                + "10000-9 §5.5.4/§5.5.5 prescribe.",
                        conditionNodeId,
                        update.method().browseName());
                final CallMethodRequest request = new CallMethodRequest(objectNodeId, specified, arguments);
                return client.callAsync(List.of(request)).thenApply(ConditionUpdateWriter::statusOf);
            }
            return callCommentedAsFallback(client, objectNodeId, conditionNodeId, update, commentedVariantKnownAbsent);
        });
    }

    /**
     * Tries the {@code "2"} variant when the base method is not on the instance and no type-level id exists.
     * <p>
     * The two forms are Optional and <em>independent</em> — Table 40 lists {@code Suppress} and
     * {@code Suppress2} as separate members — so a server may expose either. The resolution used to be
     * one-directional: it preferred {@code Suppress2} when the user sent a comment, and otherwise looked
     * only for {@code Suppress}. A server exposing only the newer form therefore rejected a command with
     * {@code Bad_NotSupported} purely because the user had left an optional field out, which made whether an
     * alarm could be suppressed depend on whether anyone wrote a note about it.
     * <p>
     * Nothing has to be invented to close that. {@code commentOf} already renders an absent comment as
     * {@link LocalizedText#NULL_VALUE}, and OPC 10000-9 §5.7.3 defines exactly that as "ignored and any
     * existing comments will remain unchanged" — so the commented form carries "no comment" natively, and
     * calling it without one is not a substitution but the encoding the specification provides.
     */
    private static @NotNull CompletableFuture<StatusCode> callCommentedAsFallback(
            final @NotNull OpcUaClient client,
            final @NotNull NodeId objectNodeId,
            final @NotNull NodeId conditionNodeId,
            final @NotNull ConditionUpdate update,
            final boolean commentedVariantKnownAbsent) {

        final String commentedName = update.method().commentedBrowseName();
        if (commentedName == null || commentedVariantKnownAbsent) {
            return notSupported(conditionNodeId, update);
        }
        return browseComponent(client, objectNodeId, NodeClass.Method, commentedName)
                .thenCompose(commented -> {
                    if (commented == null) {
                        return notSupported(conditionNodeId, update);
                    }
                    log.debug(
                            "Condition {} exposes {} but not {}; calling the commented variant with a null "
                                    + "LocalizedText, which OPC 10000-9 §5.7.3 defines as leaving any existing "
                                    + "comment unchanged.",
                            conditionNodeId,
                            commentedName,
                            update.method().browseName());
                    return callCommented(client, objectNodeId, commented, update);
                });
    }

    /**
     * Reports that neither form of the method can be reached on this server.
     * <p>
     * Logged rather than returned silently: the status code alone does not say whether the method is
     * unsupported by the device or unreachable because the server keeps its conditions out of the
     * AddressSpace, and southbound is one-directional — this log is the only thing an operator sees, since a
     * failed write is dropped rather than answered.
     */
    private static @NotNull CompletableFuture<StatusCode> notSupported(
            final @NotNull NodeId conditionNodeId, final @NotNull ConditionUpdate update) {

        log.error(
                "Cannot invoke {} on condition {}: the server exposes neither {} nor its commented variant "
                        + "on the instance, and OPC 10000-9 prescribes no type-level MethodId for it. If this "
                        + "server does not expose Condition instances, this method cannot be called through Edge.",
                update.method().name(),
                conditionNodeId,
                update.method().browseName());
        // Bad_NotSupported, not Bad_MethodInvalid. OPC 10000-4 Table 61 distinguishes them:
        // Bad_MethodInvalid is "the method id does not refer to a Method for the specified Object",
        // which claims we hold an id that points at a non-method. We hold no id at all. The code
        // for that is Bad_NotSupported -- "the Method is not supported for the Object instance".
        // It reaches nobody but the log lines that interpolate it, which is reason enough for it
        // to name the right thing.
        return CompletableFuture.completedFuture(new StatusCode(StatusCodes.Bad_NotSupported));
    }

    /**
     * Finds the {@code "2"} variant of the method, when the user supplied a comment and the server has one.
     * <p>
     * The original methods take no arguments — {@code Suppress()} has nowhere to put a note — so the
     * specification added {@code Suppress2(Comment)} alongside. Both are Optional and independent, so which
     * exists is a per-device question and has to be browsed.
     * <p>
     * <b>The action is what the user asked for; the comment is best effort.</b> Someone writing
     * {@code {"method": "SUPPRESS", "comment": "..."}} wants the alarm suppressed first and foremost, so a
     * server without {@code Suppress2} gets the plain {@code Suppress} and the comment is dropped — with a
     * warning, because silently discarding it is what made this a defect. Failing the write instead would
     * leave an alarm unsuppressed over a note, which is the worse trade.
     *
     * @return the node of the commented variant, or null when there is no comment to carry, no such variant
     *         in the specification, or none on this server.
     */
    private static @NotNull CompletableFuture<NodeId> resolveCommentedMethodOn(
            final @NotNull OpcUaClient client,
            final @NotNull NodeId objectNodeId,
            final @NotNull ConditionUpdate update,
            final @NotNull NodeId conditionNodeId) {

        final String comment = update.comment();
        if (comment == null || update.method().arguments() == ConditionUpdate.Method.Arguments.EVENT_AND_COMMENT) {
            // Nothing to carry, or the base method already carries it.
            return CompletableFuture.completedFuture(null);
        }
        final String commentedName = update.method().commentedBrowseName();
        if (commentedName == null) {
            // Enable, Disable and Silence: the specification defines no "2" form, so no server can record a
            // comment for these. Worth saying plainly rather than letting the user infer it from silence.
            log.warn(
                    "Comment ignored for {} on condition {}: OPC 10000-9 defines no commented variant of this "
                            + "method, so no server can record one. The {} itself is being performed.",
                    update.method().name(),
                    conditionNodeId,
                    update.method().name());
            return CompletableFuture.completedFuture(null);
        }
        return browseComponent(client, objectNodeId, NodeClass.Method, commentedName)
                .thenApply(found -> {
                    if (found == null) {
                        log.warn(
                                "Comment ignored for {} on condition {}: this server exposes {} but not {}, "
                                        + "and only the latter takes a comment. The {} itself is being performed.",
                                update.method().name(),
                                conditionNodeId,
                                update.method().browseName(),
                                commentedName,
                                update.method().name());
                    }
                    return found;
                });
    }

    /**
     * The arguments of a {@code "2"} variant: the base method's arguments with the comment appended.
     * <p>
     * {@code TimedShelve2(ShelvingTime, Comment)} is the only one with two — the rest take the comment alone.
     */
    private static @NotNull Variant[] commentedArgumentsFor(final @NotNull ConditionUpdate update) {
        return switch (update.method().arguments()) {
            case DURATION -> new Variant[] {Variant.of(update.duration()), Variant.of(commentOf(update))};
            case NONE -> new Variant[] {Variant.of(commentOf(update))};
            // The base form already takes the comment, so this variant is never resolved for these.
            case EVENT_AND_COMMENT -> new Variant[] {Variant.of(update.eventId()), Variant.of(commentOf(update))};
        };
    }

    /**
     * Builds the input arguments, in the order the specification defines them.
     * <p>
     * Ten of the fourteen methods take none at all, so an empty array is the normal case rather than a
     * degenerate one. The command's optional fields exist for the minority that do take arguments.
     */
    private static @NotNull Variant[] argumentsFor(final @NotNull ConditionUpdate update) {
        return switch (update.method().arguments()) {
            case EVENT_AND_COMMENT -> new Variant[] {Variant.of(update.eventId()), Variant.of(commentOf(update))};
            case DURATION -> new Variant[] {Variant.of(update.duration())};
            case NONE -> new Variant[0];
        };
    }

    /**
     * The comment argument, in the form the specification gives the caller's intent.
     * <p>
     * OPC 10000-9 §5.7.3: "If the comment field is NULL (both locale and text are empty) it will be ignored
     * and any existing comments will remain unchanged. To reset the comment, an empty text with a locale
     * shall be provided." Three intents, three encodings — and the middle one is easy to send by accident,
     * because {@code comment} is optional in the write schema.
     * <p>
     * {@code LocalizedText.NULL_VALUE} rather than {@code english(null)}: Milo's single-argument constructor
     * hardcodes the locale to {@code "en"}, and its {@code isNull()} requires <em>both</em> fields to be
     * null. So an English-locale text of null is not the specification's NULL — it is the reset form with a
     * different spelling.
     */
    private static @NotNull LocalizedText commentOf(final @NotNull ConditionUpdate update) {
        final String comment = update.comment();
        return comment == null ? LocalizedText.NULL_VALUE : LocalizedText.english(comment);
    }

    /**
     * Finds the object the method is invoked on — the condition itself, or its {@code ShelvingState}.
     *
     * @return the object node. Never null: a shelving method whose {@code ShelvingState} cannot be found
     *         falls back to the condition, which §5.8.17 requires every server to accept.
     */
    private static @NotNull CompletableFuture<NodeId> resolveTargetObject(
            final @NotNull OpcUaClient client,
            final @NotNull NodeId conditionNodeId,
            final @NotNull ConditionUpdate.Method method) {

        if (method.location() == ConditionUpdate.Method.Location.CONDITION) {
            return CompletableFuture.completedFuture(conditionNodeId);
        }
        // No ShelvingState to be found is not the end of the road. OPC 10000-9 §5.8.17.2 (and §5.8.17.4,
        // §5.8.17.6 for the other two): "some Servers do not expose Condition instances in the AddressSpace.
        // Therefore, all Servers shall also allow Clients to call the Unshelve Method by specifying
        // ConditionId as the ObjectId where the ConditionId is the Condition that has Shelving child."
        //
        // So the condition itself is the fallback ObjectId, and it is a fallback the specification requires
        // every server to accept -- unlike the MethodId question of finding 1, where only Enable and Disable
        // have a prescribed answer. Returning Bad_NodeIdUnknown here instead, as this used to, made shelving
        // impossible on that whole class of server and blamed the operator's tag configuration for it.
        return browseComponent(client, conditionNodeId, NodeClass.Object, SHELVING_STATE)
                .thenApply(shelvingState -> {
                    if (shelvingState == null) {
                        log.debug(
                                "Condition {} exposes no {} object; calling {} on the condition itself, which "
                                        + "OPC 10000-9 §5.8.17 requires every server to accept.",
                                conditionNodeId,
                                SHELVING_STATE,
                                method.browseName());
                        return conditionNodeId;
                    }
                    return shelvingState;
                });
    }

    /**
     * Finds the node of the requested method <em>on this condition instance</em>.
     * <p>
     * Preferred over the type-level id whenever the instance exposes it, because it is the form every server
     * accepts. It is not the only legal form, though: OPC 10000-4 §5.12.2.2 Table 59 says the methodId "is
     * either the NodeId of the Method that is a component of the Object instance <em>or</em> the NodeId of
     * the Method in the ObjectType that defines the Method". A miss here is therefore not proof that the
     * method cannot be called — see {@link #SPECIFIED_TYPE_LEVEL_METHODS}.
     *
     * @return the instance's method node, or {@code null} when the condition does not expose that method.
     */
    private static @NotNull CompletableFuture<NodeId> resolveMethodOn(
            final @NotNull OpcUaClient client,
            final @NotNull NodeId objectNodeId,
            final @NotNull ConditionUpdate.Method method) {
        return browseComponent(client, objectNodeId, NodeClass.Method, method.browseName());
    }

    /**
     * Finds a component of a node by browse name.
     * <p>
     * Used for both lookups this class needs — the method on an object, and the {@code ShelvingState} object
     * on a condition — because they are the same operation over a different node class.
     *
     * @return the matching node, or {@code null} when the node has no such component.
     */
    private static @NotNull CompletableFuture<NodeId> browseComponent(
            final @NotNull OpcUaClient client,
            final @NotNull NodeId parentNodeId,
            final @NotNull NodeClass nodeClass,
            final @NotNull String browseName) {

        final BrowseDescription browse = new BrowseDescription(
                parentNodeId,
                BrowseDirection.Forward,
                NodeIds.HasComponent,
                true,
                uint(nodeClass.getValue()),
                uint(BrowseResultMask.All.getValue()));

        // browseAll, not browseAsync: a condition can carry a great many components -- both forms of every
        // method, the state machines, vendor extensions -- and a server that pages its answer would
        // otherwise make a present method look absent.
        return Browsing.browseAll(client, browse).thenApply(references -> {
            for (final ReferenceDescription reference : references) {
                if (Browsing.isStandardName(reference.getBrowseName(), browseName)) {
                    return reference
                            .getNodeId()
                            .toNodeId(client.getNamespaceTable())
                            .orElse(null);
                }
            }
            return null;
        });
    }

    /**
     * Extracts the per-method status from a call response. One request was sent, so one result is expected;
     * an empty result array means the server answered the call without answering the method.
     */
    private static @NotNull StatusCode statusOf(final @NotNull CallResponse response) {
        final CallMethodResult[] results = response.getResults();
        if (results == null || results.length == 0) {
            return StatusCode.BAD;
        }
        return results[0].getStatusCode();
    }
}
