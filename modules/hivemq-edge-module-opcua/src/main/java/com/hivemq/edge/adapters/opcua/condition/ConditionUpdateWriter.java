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
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
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

        final Variant[] arguments = argumentsFor(update);

        // Shelving methods hang off the condition's ShelvingState object rather than the condition itself, so
        // the object a call is made on is not always the condition.
        return resolveTargetObject(client, conditionNodeId, update.method()).thenCompose(objectNodeId -> {
            if (objectNodeId == null) {
                return CompletableFuture.completedFuture(new StatusCode(StatusCodes.Bad_NodeIdUnknown));
            }
            return resolveMethodOn(client, objectNodeId, update.method()).thenCompose(browsed -> {
                final NodeId methodNodeId =
                        browsed != null ? browsed : SPECIFIED_TYPE_LEVEL_METHODS.get(update.method());
                if (methodNodeId == null) {
                    // Nothing on the instance and no id the specification prescribes. Logged rather than
                    // returned silently: this is a southbound command a user sent, and Bad_MethodInvalid on
                    // its own does not say whether the method is unsupported by the device or unreachable
                    // because the server keeps its conditions out of the AddressSpace.
                    log.error(
                            "Cannot invoke {} on condition {}: the server exposes no such method on the "
                                    + "instance, and OPC 10000-9 prescribes no type-level MethodId for it. "
                                    + "If this server does not expose Condition instances, this method cannot "
                                    + "be called through Edge.",
                            update.method().browseName(),
                            conditionNodeId);
                    return CompletableFuture.completedFuture(new StatusCode(StatusCodes.Bad_MethodInvalid));
                }
                if (browsed == null) {
                    log.debug(
                            "Condition {} exposes no {} method; calling the ConditionType's method id as OPC "
                                    + "10000-9 §5.5.4/§5.5.5 prescribe.",
                            conditionNodeId,
                            update.method().browseName());
                }
                final CallMethodRequest request = new CallMethodRequest(objectNodeId, methodNodeId, arguments);
                return client.callAsync(List.of(request)).thenApply(ConditionUpdateWriter::statusOf);
            });
        });
    }

    /**
     * Builds the input arguments, in the order the specification defines them.
     * <p>
     * Ten of the fourteen methods take none at all, so an empty array is the normal case rather than a
     * degenerate one. The command's optional fields exist for the minority that do take arguments.
     */
    private static @NotNull Variant[] argumentsFor(final @NotNull ConditionUpdate update) {
        return switch (update.method().arguments()) {
            case EVENT_AND_COMMENT ->
                new Variant[] {Variant.of(update.eventId()), Variant.of(LocalizedText.english(update.comment()))};
            case DURATION -> new Variant[] {Variant.of(update.duration())};
            case NONE -> new Variant[0];
        };
    }

    /**
     * Finds the object the method is invoked on — the condition itself, or its {@code ShelvingState}.
     *
     * @return the object node, or {@code null} when the condition has no {@code ShelvingState} (it is not an
     *         alarm, or the server does not expose shelving).
     */
    private static @NotNull CompletableFuture<NodeId> resolveTargetObject(
            final @NotNull OpcUaClient client,
            final @NotNull NodeId conditionNodeId,
            final @NotNull ConditionUpdate.Method method) {

        if (method.location() == ConditionUpdate.Method.Location.CONDITION) {
            return CompletableFuture.completedFuture(conditionNodeId);
        }
        return browseComponent(client, conditionNodeId, NodeClass.Object, SHELVING_STATE);
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

        return client.browseAsync(browse).thenApply(result -> {
            final ReferenceDescription[] references = result.getReferences();
            if (references == null) {
                return null;
            }
            for (final ReferenceDescription reference : references) {
                final QualifiedName referenceBrowseName = reference.getBrowseName();
                if (referenceBrowseName != null && browseName.equals(referenceBrowseName.getName())) {
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
