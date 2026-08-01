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
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
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
 * Asks the server to re-send the current alarm picture after a (re)connect.
 * <p>
 * Transitions are events: fired and forgotten. A condition that went active while Edge was disconnected has
 * already emitted its transition, and the server cannot re-send it — so after a reconnect the current alarm
 * state is simply unknown to us. {@code ConditionRefresh} closes that gap by asking the server to
 * <em>synthesise</em> a fresh transition report for every condition it currently retains, as though each had
 * just transitioned. It is the bridge from state back to transitions, which is all the event path can carry.
 * <p>
 * The burst arrives as ordinary events on the existing subscription, bracketed by {@code RefreshStartEvent} and
 * {@code RefreshEndEvent}. Nothing special is needed to receive them: the same handler processes them, because
 * they are structurally identical to live events.
 * <p>
 * <b>The EventId in a refresh burst cannot be used to acknowledge.</b> Each synthesised event is a new
 * occurrence with a freshly minted {@code EventId}, not a replay of the original, and a synthesised transition
 * is not a real one. Only the {@code EventId} of the most recent genuine transition is valid for an
 * acknowledgement. Edge does not track this — it is a conduit — so a downstream consumer must.
 */
public final class ConditionRefresh {

    private ConditionRefresh() {}

    /** The browse name of the refresh method, fixed by the specification. */
    private static final @NotNull String CONDITION_REFRESH = "ConditionRefresh";

    /**
     * Requests a refresh of every retained condition on one subscription.
     * <p>
     * The refresh applies to the whole subscription, not to one alarm — but it still has to be <em>called</em>
     * on an object that offers the method. {@code ConditionRefresh} is defined on {@code ConditionType}, so a
     * server exposes it on its condition instances; the type-level node id is not itself callable, and the
     * Server object does not carry the method (verified against an embedded server, which offers only
     * {@code GetMonitoredItems}, {@code ResendData}, {@code SetSubscriptionDurable} and
     * {@code RequestServerStateChange}). Any subscribed condition therefore serves as the entry point, and the
     * burst it triggers covers every retained condition on the subscription.
     *
     * @param client         the connected client.
     * @param conditionNode  any condition on the subscription — the object the call is made on.
     * @param subscriptionId the subscription whose retained conditions should be re-reported.
     * @return the status of the call. Good means the server accepted the request; the burst follows
     *         asynchronously as ordinary event notifications.
     */
    public static @NotNull CompletableFuture<StatusCode> request(
            final @NotNull OpcUaClient client,
            final @NotNull NodeId conditionNode,
            final @NotNull UInteger subscriptionId) {

        return resolveRefreshMethod(client, conditionNode).thenCompose(methodNodeId -> {
            if (methodNodeId == null) {
                return CompletableFuture.completedFuture(new StatusCode(StatusCodes.Bad_MethodInvalid));
            }
            final CallMethodRequest request =
                    new CallMethodRequest(conditionNode, methodNodeId, new Variant[] {Variant.of(subscriptionId)});
            return client.callAsync(List.of(request)).thenApply(ConditionRefresh::statusOf);
        });
    }

    /**
     * Finds {@code ConditionRefresh} on the condition instance, by browse name — the same resolution the
     * acknowledge path uses, and for the same reason: a method must be a component of the object it is called
     * on.
     */
    private static @NotNull CompletableFuture<NodeId> resolveRefreshMethod(
            final @NotNull OpcUaClient client, final @NotNull NodeId conditionNode) {

        final BrowseDescription browse = new BrowseDescription(
                conditionNode,
                BrowseDirection.Forward,
                NodeIds.HasComponent,
                true,
                uint(NodeClass.Method.getValue()),
                uint(BrowseResultMask.All.getValue()));

        return client.browseAsync(browse).thenApply(result -> {
            final ReferenceDescription[] references = result.getReferences();
            if (references == null) {
                return null;
            }
            for (final ReferenceDescription reference : references) {
                final QualifiedName browseName = reference.getBrowseName();
                if (browseName != null && CONDITION_REFRESH.equals(browseName.getName())) {
                    return reference
                            .getNodeId()
                            .toNodeId(client.getNamespaceTable())
                            .orElse(null);
                }
            }
            return null;
        });
    }

    private static @NotNull StatusCode statusOf(final @NotNull CallResponse response) {
        final CallMethodResult[] results = response.getResults();
        if (results == null || results.length == 0) {
            return StatusCode.BAD;
        }
        return results[0].getStatusCode();
    }
}
