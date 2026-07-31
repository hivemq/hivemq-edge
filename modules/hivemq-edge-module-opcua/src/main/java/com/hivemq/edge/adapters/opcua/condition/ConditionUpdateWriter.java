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

    /** The browse name of the alarm's shelving state machine, fixed by the specification. */
    private static final @NotNull String SHELVING_STATE = "ShelvingState";

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
            return resolveMethodOn(client, objectNodeId, update.method()).thenCompose(methodNodeId -> {
                if (methodNodeId == null) {
                    return CompletableFuture.completedFuture(new StatusCode(StatusCodes.Bad_MethodInvalid));
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
     * A call names the object and the method, and the method has to be a component of that object — the
     * type-level id from {@code AcknowledgeableConditionType} identifies the method in the type hierarchy, not
     * on the instance, and calling it directly is rejected with {@code Bad_MethodInvalid}. So the instance's
     * components are browsed and matched by browse name, which is fixed by the specification.
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
