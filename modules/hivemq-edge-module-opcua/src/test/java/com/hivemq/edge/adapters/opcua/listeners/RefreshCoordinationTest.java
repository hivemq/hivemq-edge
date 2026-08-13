/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.opcua.listeners;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hivemq.adapter.sdk.api.datapoint.DataPointListBuilder;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.edge.adapters.opcua.FakeEventService;
import com.hivemq.edge.adapters.opcua.config.ConnectionOptions;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaConditionType;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagKind;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodResult;
import org.eclipse.milo.opcua.stack.core.types.structured.CallResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ResponseHeader;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Review-05 finding 2: one coordinator for every refresh the adapter owes.
 * <p>
 * A {@code ConditionRefresh} applies to a whole subscription (OPC 10000-9 §4.5), and §5.5.7 defines
 * {@code Bad_RefreshInProgress} for a second call arriving while one is running. There were two independent
 * ways to start one. The server-requested path had a pending/in-flight coordinator; the connect-time and
 * reconnect refreshes called {@code ConditionRefresh} directly and swallowed the outcome, on the reasoning
 * that they "fire once by construction".
 * <p>
 * Each does fire once. What that misses is that they fire relative to <em>another</em> reason's call, and
 * neither path knew about the other:
 * <ol>
 *   <li>a {@code RefreshRequired} starts a coordinated refresh;</li>
 *   <li>the session drops and reactivates while that call is outstanding;</li>
 *   <li>the reconnect refresh goes out directly, colliding with it;</li>
 *   <li>the server refuses one, and the automatic path only logged — no pending work, no retry;</li>
 *   <li>the reconnect completes with no successful refresh, against a requirement that says one follows
 *       every reconnect.</li>
 * </ol>
 * The retained alarm picture is then stale exactly when it matters most, and the adapter is green.
 * <p>
 * Driven through {@code client.callAsync}, which is where {@code ConditionRefresh.request} ends up, so a
 * call can be held open and the overlap made deterministic rather than raced for. The same technique as
 * {@code OpcUaSubscriptionControlEventsTest}, which pins the coordinator's behaviour within the
 * server-requested path; this class is about the paths that were outside it.
 */
class RefreshCoordinationTest {

    private static final @NotNull String ADAPTER_ID = "test-adapter";

    private @NotNull ProtocolAdapterMetricsService metrics;
    private @NotNull ProtocolAdapterTagStreamingService streaming;
    private @NotNull FakeEventService events;
    private @NotNull OpcUaClient client;

    @BeforeEach
    void setUp() {
        metrics = mock(ProtocolAdapterMetricsService.class);
        streaming = mock(ProtocolAdapterTagStreamingService.class);
        events = new FakeEventService();
        client = mock(OpcUaClient.class);
        // A control event still runs the ordinary publish path on its way past, and that path asks for the
        // publisher whether or not it ends up adding anything to it.
        when(streaming.dataPointsPublisher()).thenReturn(mock(DataPointListBuilder.class));
    }

    // ── the overlap, in both orders ──────────────────────────────────────────────────────────────────

    @Test
    void aReconnectRefreshWaitsForAServerRequestedOneRatherThanCollidingWithIt() {
        final OpcuaTag tag = conditionTag("boiler-high-temp");
        final var handler = handlerFor(tag);
        final OpcUaSubscription subscription = established(handler, 4711);
        final CompletableFuture<CallResponse> outstanding = new CompletableFuture<>();
        when(client.callAsync(any())).thenReturn(outstanding, completedRefreshCall());

        refreshRequired(handler, subscription, tag, "occurrence-A");
        assertThat(refreshCallCount())
                .as("precondition: the server's request is outstanding")
                .isOne();

        handler.onSessionReactivated();

        // The whole finding in one assertion. This used to be a second, immediate call against a
        // subscription already being refreshed -- the collision Bad_RefreshInProgress exists to report.
        verify(client, times(1)).callAsync(any());

        outstanding.complete(goodCallResponse());

        // And it is not merely suppressed: the reconnect's refresh is owed and follows once the way is clear.
        verify(client, times(2)).callAsync(any());
    }

    @Test
    void andAServerRequestedOneWaitsForAReconnectRefreshTheSameWay() {
        // The inverse ordering, which fails differently. Here the reconnect refresh was the one running, and
        // because it never took the in-flight guard, the server-requested path found the guard free and went
        // straight out on top of it.
        final OpcuaTag tag = conditionTag("boiler-high-temp");
        final var handler = handlerFor(tag);
        final OpcUaSubscription subscription = established(handler, 4711);
        final CompletableFuture<CallResponse> outstanding = new CompletableFuture<>();
        when(client.callAsync(any())).thenReturn(outstanding, completedRefreshCall());

        handler.onSessionReactivated();
        assertThat(refreshCallCount())
                .as("precondition: the reconnect refresh is outstanding")
                .isOne();

        refreshRequired(handler, subscription, tag, "occurrence-B");

        verify(client, times(1)).callAsync(any());

        outstanding.complete(goodCallResponse());

        verify(client, times(2)).callAsync(any());
    }

    @Test
    void twoReconnectsDuringOneCallStillCollapseIntoASingleFollowUp() {
        // §4.5 makes the refresh subscription-wide, so one call answers every reason outstanding when it
        // starts. Two reactivations while a call is running are one reason to resynchronise, not two.
        final OpcuaTag tag = conditionTag("boiler-high-temp");
        final var handler = handlerFor(tag);
        established(handler, 4711);
        final CompletableFuture<CallResponse> outstanding = new CompletableFuture<>();
        when(client.callAsync(any())).thenReturn(outstanding, completedRefreshCall());

        handler.onSessionReactivated();
        handler.onSessionReactivated();
        handler.onSessionReactivated();

        outstanding.complete(goodCallResponse());

        verify(client, times(2)).callAsync(any());
    }

    // ── the refusal that a retry answers ─────────────────────────────────────────────────────────────

    @Test
    void aRefreshRefusedAsAlreadyInProgressIsTriedAgain() {
        // The other half of the finding. Coordinating our own two paths removes the collision we cause, but
        // the southbound manual refresh is deliberately outside the coordinator -- the caller asked, so a
        // refusal is theirs to see rather than something to queue behind work they did not ask for. It can
        // still collide, and before this a refused automatic refresh was logged and forgotten: the reconnect
        // ended with no successful refresh and nothing anywhere recording that one was still owed.
        //
        // Bad_RefreshInProgress is the one refusal that says nothing about whether the request was
        // reasonable, only that it arrived while another was outstanding -- which stops being true within a
        // round trip.
        final OpcuaTag tag = conditionTag("boiler-high-temp");
        final var handler = handlerFor(tag);
        established(handler, 4711);
        when(client.callAsync(any())).thenReturn(refusedAsInProgress(), completedRefreshCall());

        handler.onSessionReactivated();

        verify(client, times(2)).callAsync(any());
    }

    @Test
    void butARefusalARetryCannotAnswerIsNotTriedAgain() {
        // The bound on the retry's reach. A server that does not implement ConditionRefresh will not
        // implement it on the second ask either, and retrying would spend calls to learn the same thing.
        final OpcuaTag tag = conditionTag("boiler-high-temp");
        final var handler = handlerFor(tag);
        established(handler, 4711);
        when(client.callAsync(any())).thenReturn(refusedWith(StatusCodes.Bad_NotSupported));

        handler.onSessionReactivated();

        verify(client, times(1)).callAsync(any());
    }

    @Test
    void aServerRefusingForeverIsNotAskedForever() {
        // The reason the retry is counted rather than unconditional. Requeueing on every refusal against a
        // server that always refuses is a hot loop: each refusal arrives and immediately produces the next
        // call, for the life of the connection.
        final OpcuaTag tag = conditionTag("boiler-high-temp");
        final var handler = handlerFor(tag);
        established(handler, 4711);
        when(client.callAsync(any())).thenReturn(refusedAsInProgress());

        handler.onSessionReactivated();

        verify(client, times(1 + OpcUaSubscriptionLifecycleHandler.MAX_REFRESH_RETRIES))
                .callAsync(any());
    }

    @Test
    void aTransportFailureIsTriedAgainToo() {
        // Not a refusal at all -- the request never reached a verdict. Same reasoning as
        // Bad_RefreshInProgress: it describes the moment rather than the request.
        final OpcuaTag tag = conditionTag("boiler-high-temp");
        final var handler = handlerFor(tag);
        established(handler, 4711);
        final CompletableFuture<CallResponse> failing = new CompletableFuture<>();
        when(client.callAsync(any())).thenReturn(failing, completedRefreshCall());

        handler.onSessionReactivated();
        failing.completeExceptionally(new IllegalStateException("the session went away"));

        verify(client, times(2)).callAsync(any());
    }

    @Test
    void theRetryBudgetIsPerRunOfFailuresRatherThanPerConnection() {
        // Why the counter resets on success. Bounded per connection, a long-lived adapter that collided a
        // few times early would spend the rest of its life unable to retry a refresh -- and the reconnect
        // contract applies to every reconnect, not to the first few.
        final OpcuaTag tag = conditionTag("boiler-high-temp");
        final var handler = handlerFor(tag);
        established(handler, 4711);
        when(client.callAsync(any()))
                .thenReturn(
                        refusedAsInProgress(),
                        refusedAsInProgress(),
                        refusedAsInProgress(),
                        completedRefreshCall(),
                        refusedAsInProgress(),
                        completedRefreshCall());

        handler.onSessionReactivated();
        assertThat(refreshCallCount())
                .as("three refusals then a success, all within one run")
                .isEqualTo(4);

        handler.onSessionReactivated();

        assertThat(refreshCallCount())
                .as("the budget must have been handed back, so this run can retry as well")
                .isEqualTo(6);
    }

    // ── helpers ─────────────────────────────────────────────────────────────────────────────────────

    private static void refreshRequired(
            final @NotNull OpcUaSubscriptionLifecycleHandler handler,
            final @NotNull OpcUaSubscription subscription,
            final @NotNull OpcuaTag tag,
            final @NotNull String eventId) {

        handler.onEventReceived(
                subscription,
                List.of(itemFor(tag)),
                List.<Variant[]>of(controlEvent(NodeIds.RefreshRequiredEventType, eventId)));
    }

    private @NotNull OpcUaSubscriptionLifecycleHandler handlerFor(final @NotNull OpcuaTag... tags) {
        return new OpcUaSubscriptionLifecycleHandler(
                metrics,
                streaming,
                events,
                ADAPTER_ID,
                List.of(tags),
                client,
                new OpcUaSpecificAdapterConfig(
                        "opc.tcp://localhost:4840",
                        false,
                        null,
                        null,
                        null,
                        OpcUaToMqttConfig.defaultOpcUaToMqttConfig(),
                        null,
                        ConnectionOptions.defaultConnectionOptions()));
    }

    private static @NotNull OpcUaSubscription established(
            final @NotNull OpcUaSubscriptionLifecycleHandler handler, final int id) {

        final OpcUaSubscription subscription = mock(OpcUaSubscription.class);
        when(subscription.getSubscriptionId()).thenReturn(Optional.of(uint(id)));
        handler.installSubscriptionForTesting(subscription);
        return subscription;
    }

    private int refreshCallCount() {
        @SuppressWarnings("unchecked")
        final ArgumentCaptor<List<CallMethodRequest>> captor = ArgumentCaptor.forClass(List.class);
        verify(client, org.mockito.Mockito.atLeast(0)).callAsync(captor.capture());
        return captor.getAllValues().size();
    }

    private static @NotNull CompletableFuture<CallResponse> completedRefreshCall() {
        return CompletableFuture.completedFuture(goodCallResponse());
    }

    private static @NotNull CompletableFuture<CallResponse> refusedAsInProgress() {
        return CompletableFuture.completedFuture(responseWith(StatusCodes.Bad_RefreshInProgress));
    }

    private static @NotNull CompletableFuture<CallResponse> refusedWith(final long statusCode) {
        return CompletableFuture.completedFuture(responseWith(statusCode));
    }

    private static @NotNull CallResponse goodCallResponse() {
        return responseWith(StatusCode.GOOD.getValue());
    }

    /**
     * A call response whose <em>method result</em> carries the status.
     * <p>
     * That is where {@code ConditionRefresh} reads it from, not from the response header — the service call
     * succeeded, and the method it invoked is what was refused.
     */
    private static @NotNull CallResponse responseWith(final long statusCode) {
        return new CallResponse(
                new ResponseHeader(null, uint(0), StatusCode.GOOD, null, null, null),
                new CallMethodResult[] {new CallMethodResult(new StatusCode(statusCode), null, null, null)},
                null);
    }

    private static @NotNull OpcuaTag conditionTag(final @NotNull String name) {
        return new OpcuaTag(
                name,
                "a condition tag",
                new OpcuaTagDefinition("ns=2;s=" + name, OpcuaTagKind.CONDITION, OpcuaConditionType.ALARM_CONDITION));
    }

    private static @NotNull OpcUaMonitoredItem itemFor(final @NotNull OpcuaTag tag) {
        final OpcUaMonitoredItem item = mock(OpcUaMonitoredItem.class);
        when(item.getUserObject()).thenReturn(Optional.of(tag));
        return item;
    }

    /** An event carrying only the two fields the control-event routing reads, at their select-clause positions. */
    private static @NotNull Variant[] controlEvent(final @NotNull NodeId eventType, final @NotNull String eventId) {

        return new Variant[] {
            new Variant(ByteString.of(eventId.getBytes(StandardCharsets.UTF_8))), new Variant(eventType)
        };
    }
}
