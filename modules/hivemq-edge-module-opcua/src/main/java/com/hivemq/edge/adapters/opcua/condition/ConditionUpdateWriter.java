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
import java.util.Set;
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
import org.jetbrains.annotations.Nullable;
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
     * The MethodId to pass when the condition instance exposes no method node of its own — keyed by browse
     * name, so a method and its {@code "2"} variant are two entries rather than two tables.
     * <p>
     * A server need not expose condition instances in the AddressSpace at all, and OPC 10000-9 says so
     * repeatedly: §5.7.3, §5.5.6, §5.8.17.2 and their neighbours all note that "some Servers do not expose
     * Condition instances in the AddressSpace" and require every server to accept the ConditionId as the
     * <em>ObjectId</em> instead. What none of those clauses says is which <em>MethodId</em> accompanies it —
     * only §5.5.4 and §5.5.5, for Enable and Disable, name one outright.
     * <p>
     * That silence used to be read here as a prohibition, and this table held those two methods alone. It is
     * not one. The rule the other twelve inherit is stated once, generally, in OPC 10000-4 §5.12.2.2 Table 59:
     * "If the objectId is the NodeId of an Object, the methodId is either the NodeId of the Method that is a
     * component of the Object instance <em>or the NodeId of the Method in the ObjectType that defines the
     * Method</em>." Part 9 does not repeat it per method because Part 4 already covers every method there is.
     * So the id is not guessed from a vendor library's constant table — it is the one the standard nodeset
     * fixes for the type that declares the operation, and Part 4 says a Call may name it.
     * <p>
     * The ObjectId stays the ConditionId. Part 9 is explicit that it must: "The Method cannot be called with
     * an ObjectId of the ConditionType Node". Only the MethodId becomes type-level.
     * <p>
     * Three types declare these fourteen operations, and each entry names the one that declares it:
     * {@code ConditionType} for enable/disable/comment, {@code AcknowledgeableConditionType} for the two
     * acknowledgement methods, {@code AlarmConditionType} for the alarm methods. Shelving is the exception —
     * see {@link #SHELVED_STATE_MACHINE_METHODS}.
     */
    private static final @NotNull Map<String, NodeId> CONDITION_TYPE_METHODS = Map.ofEntries(
            // ConditionType (§5.5): the operations every condition has.
            Map.entry("Enable", NodeIds.ConditionType_Enable),
            Map.entry("Disable", NodeIds.ConditionType_Disable),
            Map.entry("AddComment", NodeIds.ConditionType_AddComment),
            // AcknowledgeableConditionType (§5.7).
            Map.entry("Acknowledge", NodeIds.AcknowledgeableConditionType_Acknowledge),
            Map.entry("Confirm", NodeIds.AcknowledgeableConditionType_Confirm),
            // AlarmConditionType (§5.8). Silence has no "2" variant; the specification defines none.
            Map.entry("Silence", NodeIds.AlarmConditionType_Silence),
            Map.entry("Suppress", NodeIds.AlarmConditionType_Suppress),
            Map.entry("Suppress2", NodeIds.AlarmConditionType_Suppress2),
            Map.entry("Unsuppress", NodeIds.AlarmConditionType_Unsuppress),
            Map.entry("Unsuppress2", NodeIds.AlarmConditionType_Unsuppress2),
            Map.entry("RemoveFromService", NodeIds.AlarmConditionType_RemoveFromService),
            Map.entry("RemoveFromService2", NodeIds.AlarmConditionType_RemoveFromService2),
            Map.entry("PlaceInService", NodeIds.AlarmConditionType_PlaceInService),
            Map.entry("PlaceInService2", NodeIds.AlarmConditionType_PlaceInService2),
            Map.entry("Reset", NodeIds.AlarmConditionType_Reset),
            Map.entry("Reset2", NodeIds.AlarmConditionType_Reset2),
            // DialogConditionType (§5.6). A dialog is a condition in its own right rather than an alarm, so
            // its methods hang off a sibling of AlarmConditionType rather than beneath it.
            Map.entry("Respond", NodeIds.DialogConditionType_Respond),
            Map.entry("Respond2", NodeIds.DialogConditionType_Respond2),
            // Shelving reached through the condition, because this server exposes no ShelvingState object
            // either. AlarmConditionType declares the machine at ShelvingState, so its instance-declaration
            // method node is the one the condition's own type defines.
            Map.entry("Unshelve", NodeIds.AlarmConditionType_ShelvingState_Unshelve),
            Map.entry("Unshelve2", NodeIds.AlarmConditionType_ShelvingState_Unshelve2),
            Map.entry("OneShotShelve", NodeIds.AlarmConditionType_ShelvingState_OneShotShelve),
            Map.entry("OneShotShelve2", NodeIds.AlarmConditionType_ShelvingState_OneShotShelve2),
            Map.entry("TimedShelve", NodeIds.AlarmConditionType_ShelvingState_TimedShelve),
            Map.entry("TimedShelve2", NodeIds.AlarmConditionType_ShelvingState_TimedShelve2));

    /**
     * The MethodId for a call whose ObjectId is the condition's {@code ShelvingState} object rather than the
     * condition — the ordinary shelving case, where the server does expose the state machine but not the
     * method node beneath it.
     * <p>
     * Separate from {@link #CONDITION_TYPE_METHODS} because the defining type differs with the object: the
     * ObjectId decides which ObjectType Part 4's rule points at. A {@code ShelvingState} object is a
     * {@code ShelvedStateMachineType}, so the method it defines is {@code ShelvedStateMachineType_Unshelve};
     * the condition is an {@code AlarmConditionType}, so the same operation reached through it is
     * {@code AlarmConditionType_ShelvingState_Unshelve}. Both are the standard nodeset's, and which applies
     * is decided by {@link #typeLevelMethod}.
     */
    private static final @NotNull Map<String, NodeId> SHELVED_STATE_MACHINE_METHODS = Map.ofEntries(
            Map.entry("Unshelve", NodeIds.ShelvedStateMachineType_Unshelve),
            Map.entry("Unshelve2", NodeIds.ShelvedStateMachineType_Unshelve2),
            Map.entry("OneShotShelve", NodeIds.ShelvedStateMachineType_OneShotShelve),
            Map.entry("OneShotShelve2", NodeIds.ShelvedStateMachineType_OneShotShelve2),
            Map.entry("TimedShelve", NodeIds.ShelvedStateMachineType_TimedShelve),
            Map.entry("TimedShelve2", NodeIds.ShelvedStateMachineType_TimedShelve2));

    /**
     * The statuses that mean "not this method node, on this object" as opposed to "the operation failed".
     * <p>
     * Only these justify a second attempt with the other form of the method. A server that answers
     * {@code Bad_UserAccessDenied} or {@code Bad_ConditionAlreadyEnabled} has found the method and declined
     * the operation, and retrying would either fail identically or — worse — perform it twice.
     */
    private static final @NotNull Set<Long> METHOD_NOT_HERE = Set.of(
            StatusCodes.Bad_NotSupported,
            StatusCodes.Bad_MethodInvalid,
            StatusCodes.Bad_NodeIdUnknown,
            StatusCodes.Bad_NodeIdInvalid);

    private ConditionUpdateWriter() {}

    /**
     * A method node to call, and which of its two forms it is — the {@code "2"} variant takes a comment the
     * base form has nowhere to put, so the form decides the arguments.
     */
    private record ResolvedMethod(@NotNull NodeId nodeId, boolean commentedVariant) {}

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

        warnIfNoServerCanCarryTheComment(update, conditionNodeId);
        // Shelving methods hang off the condition's ShelvingState object rather than the condition itself, so
        // the object a call is made on is not always the condition.
        return resolveTargetObject(client, conditionNodeId, update.method())
                .thenCompose(objectNodeId -> resolveOnInstance(client, objectNodeId, conditionNodeId, update)
                        .thenCompose(resolved -> resolved != null
                                ? call(client, objectNodeId, resolved, update)
                                : callTypeLevel(client, objectNodeId, conditionNodeId, update)));
    }

    /**
     * The commented form to reach for first, or null when the base form is the one to call.
     * <p>
     * Three conditions, stated once and consulted by both the instance and the type-level path: there is a
     * comment to carry, the base form does not already take one, and the specification defines a variant that
     * does.
     */
    private static @Nullable String preferredCommentedName(final @NotNull ConditionUpdate update) {
        if (update.comment() == null
                || update.method().arguments() == ConditionUpdate.Method.Arguments.EVENT_AND_COMMENT) {
            return null;
        }
        return update.method().commentedBrowseName();
    }

    /**
     * Says plainly that a comment cannot reach any server, rather than letting the user infer it from silence.
     * <p>
     * Enable, Disable and Silence only: the specification defines no {@code Enable2}, {@code Disable2} or
     * {@code Silence2}, so this is a property of the method rather than of the device, and it is known before
     * a single browse.
     */
    private static void warnIfNoServerCanCarryTheComment(
            final @NotNull ConditionUpdate update, final @NotNull NodeId conditionNodeId) {

        if (update.comment() == null
                || update.method().arguments() == ConditionUpdate.Method.Arguments.EVENT_AND_COMMENT
                || update.method().commentedBrowseName() != null) {
            return;
        }
        log.warn(
                "Comment ignored for {} on condition {}: OPC 10000-9 defines no commented variant of this "
                        + "method, so no server can record one. The {} itself is being performed.",
                update.method().name(),
                conditionNodeId,
                update.method().name());
    }

    /**
     * Finds the method node <em>on the object instance</em>, preferring the form that carries the comment.
     * <p>
     * The two forms are Optional and <em>independent</em> — Table 40 lists {@code Suppress} and
     * {@code Suppress2} as separate members — so a server may expose either, and both directions have to be
     * tried. Resolution used to be one-directional: it preferred {@code Suppress2} when the user sent a
     * comment and otherwise looked only for {@code Suppress}, so a server exposing only the newer form
     * rejected a command purely because the user had left an optional field out.
     * <p>
     * At most two browses, and never the same name twice: the second form is asked for only when the first
     * missed and it has not already been asked for.
     *
     * @return the method to call, or {@code null} when the instance exposes neither form — which is not the
     *         end of the road, only the end of what browsing can answer. See {@link #callTypeLevel}.
     */
    private static @NotNull CompletableFuture<ResolvedMethod> resolveOnInstance(
            final @NotNull OpcUaClient client,
            final @NotNull NodeId objectNodeId,
            final @NotNull NodeId conditionNodeId,
            final @NotNull ConditionUpdate update) {

        final ConditionUpdate.Method method = update.method();
        final String preferCommented = preferredCommentedName(update);
        final CompletableFuture<NodeId> commentedFirst = preferCommented == null
                ? CompletableFuture.completedFuture(null)
                : browseComponent(client, objectNodeId, NodeClass.Method, preferCommented);

        return commentedFirst.thenCompose(commented -> {
            if (commented != null) {
                return CompletableFuture.completedFuture(new ResolvedMethod(commented, true));
            }
            return browseComponent(client, objectNodeId, NodeClass.Method, method.browseName())
                    .thenCompose(base -> {
                        if (base != null) {
                            if (preferCommented != null) {
                                // The deliberate trade: someone writing {"method":"SUPPRESS","comment":"..."}
                                // wants the alarm suppressed first and foremost, so the comment is dropped
                                // with a warning rather than the write failing over a note.
                                log.warn(
                                        "Comment ignored for {} on condition {}: this server exposes {} but "
                                                + "not {}, and only the latter takes a comment. The {} itself "
                                                + "is being performed.",
                                        method.name(),
                                        conditionNodeId,
                                        method.browseName(),
                                        preferCommented,
                                        method.name());
                            }
                            return CompletableFuture.completedFuture(new ResolvedMethod(base, false));
                        }
                        final String commentedName = method.commentedBrowseName();
                        if (commentedName == null || preferCommented != null) {
                            // Either there is no second form, or it has already been browsed for and missed.
                            return CompletableFuture.completedFuture(null);
                        }
                        // No comment was supplied, so the commented form was not tried first -- but it is the
                        // only form this server has. commentOf renders the absent comment as a null
                        // LocalizedText, which OPC 10000-9 §5.7.3 defines as "ignored and any existing
                        // comments will remain unchanged", so calling it without one is not a substitution
                        // but the encoding the specification provides.
                        return browseComponent(client, objectNodeId, NodeClass.Method, commentedName)
                                .thenApply(late -> late == null ? null : new ResolvedMethod(late, true));
                    });
        });
    }

    /** Sends the Call, with the arguments the resolved form takes. */
    private static @NotNull CompletableFuture<StatusCode> call(
            final @NotNull OpcUaClient client,
            final @NotNull NodeId objectNodeId,
            final @NotNull ResolvedMethod method,
            final @NotNull ConditionUpdate update) {

        final Variant[] arguments = method.commentedVariant() ? commentedArgumentsFor(update) : argumentsFor(update);
        final CallMethodRequest request = new CallMethodRequest(objectNodeId, method.nodeId(), arguments);
        return client.callAsync(List.of(request)).thenApply(ConditionUpdateWriter::statusOf);
    }

    /**
     * Calls the MethodId the standard ObjectType declares, for a server that exposes no condition instance.
     * <p>
     * This used to be reachable by Enable and Disable alone, and every other command was answered with a
     * client-minted {@code Bad_NotSupported} without a Call ever being sent — so a conformant server that
     * keeps its conditions out of the AddressSpace, which OPC 10000-9 explicitly permits and repeatedly
     * accommodates, could not be acknowledged. See {@link #CONDITION_TYPE_METHODS} for why Part 4's general
     * rule, not Part 9's per-method silence, governs here.
     * <p>
     * <b>The server decides, not Edge.</b> Whether a given method is supported for a given object is the
     * server's answer to give: it may not implement an Optional method, and no amount of local reasoning can
     * establish that from here. A rejection now arrives as the status the server chose, which names the real
     * reason, rather than as a locally invented one that only ever meant "we did not try".
     * <p>
     * One retry, and only across the two forms of the same method. Browsing cannot narrow the choice when
     * there is no instance to browse, so the form the user's intent asks for is tried first and the other
     * follows if the server says that node is not there — the same both-directions rule
     * {@link #resolveOnInstance} applies, with the server's status standing in for the browse result.
     */
    private static @NotNull CompletableFuture<StatusCode> callTypeLevel(
            final @NotNull OpcUaClient client,
            final @NotNull NodeId objectNodeId,
            final @NotNull NodeId conditionNodeId,
            final @NotNull ConditionUpdate update) {

        final ConditionUpdate.Method method = update.method();
        final String preferCommented = preferredCommentedName(update);
        final String firstName = preferCommented != null ? preferCommented : method.browseName();
        final String secondName = preferCommented != null ? method.browseName() : method.commentedBrowseName();

        final NodeId first = typeLevelMethod(objectNodeId, conditionNodeId, firstName);
        if (first == null) {
            return notSupported(conditionNodeId, update);
        }
        log.debug(
                "Condition {} exposes no {} method node; calling the {} declared by the standard type, which "
                        + "OPC 10000-4 §5.12.2.2 Table 59 permits for any method.",
                conditionNodeId,
                firstName,
                first);
        return call(client, objectNodeId, new ResolvedMethod(first, preferCommented != null), update)
                .thenCompose(status -> {
                    if (secondName == null || !METHOD_NOT_HERE.contains(status.value())) {
                        return CompletableFuture.completedFuture(status);
                    }
                    final NodeId second = typeLevelMethod(objectNodeId, conditionNodeId, secondName);
                    if (second == null) {
                        return CompletableFuture.completedFuture(status);
                    }
                    if (preferCommented != null) {
                        log.warn(
                                "Comment ignored for {} on condition {}: this server answered {} with {}, so "
                                        + "it does not implement the form that takes a comment. The {} itself "
                                        + "is being performed.",
                                method.name(),
                                conditionNodeId,
                                preferCommented,
                                status,
                                method.name());
                    } else {
                        log.debug(
                                "Condition {} answered {} with {}; trying its {} form instead.",
                                conditionNodeId,
                                firstName,
                                status,
                                secondName);
                    }
                    return call(client, objectNodeId, new ResolvedMethod(second, preferCommented == null), update);
                });
    }

    /**
     * The standard type's MethodId for a browse name, chosen by which object the Call names.
     * <p>
     * Part 4's rule points at "the Method in the ObjectType that defines the Method", and which ObjectType
     * that is depends on the ObjectId: a {@code ShelvingState} object is a {@code ShelvedStateMachineType},
     * while the condition it hangs off is an {@code AlarmConditionType}.
     */
    private static @Nullable NodeId typeLevelMethod(
            final @NotNull NodeId objectNodeId,
            final @NotNull NodeId conditionNodeId,
            final @NotNull String browseName) {

        return objectNodeId.equals(conditionNodeId)
                ? CONDITION_TYPE_METHODS.get(browseName)
                : SHELVED_STATE_MACHINE_METHODS.get(browseName);
    }

    /**
     * Reports a method Edge holds no id for at all — neither on the instance nor in the standard nodeset.
     * <p>
     * Unreachable for the fourteen methods as they stand, since every one of them is declared by a standard
     * type and named in {@link #CONDITION_TYPE_METHODS}. It is the honest answer if a method is ever added
     * without its type-level entry, which is exactly when a silent failure would be hardest to trace.
     */
    private static @NotNull CompletableFuture<StatusCode> notSupported(
            final @NotNull NodeId conditionNodeId, final @NotNull ConditionUpdate update) {

        log.error(
                "Cannot invoke {} on condition {}: the server exposes no {} method node on the instance, and "
                        + "Edge holds no type-level MethodId for it either.",
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
     * The arguments of a {@code "2"} variant: the base method's arguments with the comment appended.
     * <p>
     * {@code TimedShelve2(ShelvingTime, Comment)} and {@code Respond2(SelectedResponse, Comment)} are the two
     * with a second argument — the rest take the comment alone.
     */
    private static @NotNull Variant[] commentedArgumentsFor(final @NotNull ConditionUpdate update) {
        return switch (update.method().arguments()) {
            case DURATION -> new Variant[] {Variant.of(update.duration()), Variant.of(commentOf(update))};
            case SELECTED_RESPONSE ->
                new Variant[] {Variant.of(update.selectedResponse()), Variant.of(commentOf(update))};
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
            // Int32: OPC 10000-9 §5.6.3 declares SelectedResponse an Int32, and a Variant carrying anything
            // else is a type mismatch the server rejects rather than coerces.
            case SELECTED_RESPONSE -> new Variant[] {Variant.of(update.selectedResponse())};
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
        // every server to accept. Returning Bad_NodeIdUnknown here instead, as this used to, made shelving
        // impossible on that whole class of server and blamed the operator's tag configuration for it.
        //
        // Which ObjectId this settles on also decides which type declares the method, so it is consulted
        // again by typeLevelMethod when the method node has to come from the standard nodeset.
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
