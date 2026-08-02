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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodResult;
import org.eclipse.milo.opcua.stack.core.types.structured.CallResponse;
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
 * {@code RefreshEndEvent}. Those two are copied to <em>every</em> notifier item in the subscription and bypass
 * the where clause entirely (OPC 10000-9 §4.5, §5.5.7), so a condition tag receives them whatever its filter
 * says. They are dropped in the notification handler rather than published, because they are not transitions;
 * surfacing them to a user is a separate tag, not this one.
 * <p>
 * <b>The EventId in a refresh burst cannot be used to acknowledge.</b> Each synthesised event is a new
 * occurrence with a freshly minted {@code EventId}, not a replay of the original, and a synthesised transition
 * is not a real one. Only the {@code EventId} of the most recent genuine transition is valid for an
 * acknowledgement. Edge does not track this — it is a conduit — so a downstream consumer must.
 */
public final class ConditionRefresh {

    private ConditionRefresh() {}

    /**
     * Requests a refresh of every retained condition on one subscription.
     * <p>
     * The refresh applies to the whole subscription, not to one alarm: the server walks that subscription's
     * monitored items and re-reports every condition it currently retains.
     *
     * @param client         the connected client.
     * @param subscriptionId the subscription whose retained conditions should be re-reported.
     * @return the status of the call. Good means the server accepted the request; the burst follows
     *         asynchronously as ordinary event notifications.
     */
    public static @NotNull CompletableFuture<StatusCode> request(
            final @NotNull OpcUaClient client, final @NotNull UInteger subscriptionId) {

        // Both node ids are fixed by the specification, so nothing is browsed and no condition instance is
        // involved. OPC 10000-9 §5.5.7: "This Method is only available on the ConditionType. To invoke this
        // Method, the call shall pass the well-known MethodId of the Method on the ConditionType and the
        // ObjectId shall be the well-known NodeId of the ConditionType ObjectType." That makes it a
        // deliberate exception to the usual rule -- which the acknowledge path does follow -- that a method
        // must be a component of the instance it is called on.
        final CallMethodRequest request = new CallMethodRequest(
                NodeIds.ConditionType, NodeIds.ConditionType_ConditionRefresh, new Variant[] {Variant.of(subscriptionId)
                });
        return client.callAsync(List.of(request)).thenApply(ConditionRefresh::statusOf);
    }

    private static @NotNull StatusCode statusOf(final @NotNull CallResponse response) {
        final CallMethodResult[] results = response.getResults();
        if (results == null || results.length == 0) {
            return StatusCode.BAD;
        }
        return results[0].getStatusCode();
    }
}
