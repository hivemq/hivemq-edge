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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.hivemq.adapter.sdk.api.datapoint.DataPointBuilder;
import com.hivemq.adapter.sdk.api.datapoint.DataPointListBuilder;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.edge.adapters.opcua.Constants;
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
import org.eclipse.milo.opcua.stack.core.encoding.DefaultEncodingContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodResult;
import org.eclipse.milo.opcua.stack.core.types.structured.CallResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ResponseHeader;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * The four control events, which reach a monitored item whether or not its filter admits them.
 * <p>
 * Driven through {@code onEventReceived} directly rather than against the embedded server, and that is not a
 * shortcut. Milo's server does not implement the OPC 10000-9 §4.5 bypass at all — its {@code
 * MonitoredEventItem} evaluates the where clause against every event, with no exemption for the refresh types
 * or for queue overflow — so no integration test in this module can deliver one. Overflow is further out of
 * reach: producing it needs a server whose event queue actually fills.
 */
class OpcUaSubscriptionControlEventsTest {

    private static final @NotNull String ADAPTER_ID = "test-adapter";

    private @NotNull ProtocolAdapterMetricsService metrics;
    private @NotNull ProtocolAdapterTagStreamingService streaming;
    private @NotNull DataPointListBuilder publisher;
    private @NotNull FakeEventService events;
    private @NotNull OpcUaClient client;

    @BeforeEach
    void setUp() {
        metrics = mock(ProtocolAdapterMetricsService.class);
        streaming = mock(ProtocolAdapterTagStreamingService.class);
        publisher = mock(DataPointListBuilder.class);
        events = new FakeEventService();
        client = mock(OpcUaClient.class);
        when(streaming.dataPointsPublisher()).thenReturn(publisher);
    }

    // ── finding 1: queue overflow ────────────────────────────────────────────────────────────────────

    @Test
    void anOverflowOnAConditionTagIsReportedRatherThanDiscarded() {
        // The defect: overflow is delivered ONLY to the item whose queue filled (OPC 10000-4 §7.22.3), and
        // the routing dropped it from every non-REFRESH tag while expecting a REFRESH tag to receive it. A
        // condition tag could therefore lose transitions with nothing anywhere saying so -- no MQTT message,
        // no adapter event, no metric -- and an event is never re-sent, so the history has a hole a refresh
        // cannot fill.
        final OpcuaTag tag = conditionTag("boiler-high-temp");
        final var handler = handlerFor(tag);

        handler.onEventReceived(
                mock(OpcUaSubscription.class),
                List.of(itemFor(tag)),
                List.<Variant[]>of(controlEvent(NodeIds.EventQueueOverflowEventType, "overflow-1")));

        verify(metrics).increment(Constants.METRIC_SUBSCRIPTION_EVENT_QUEUE_OVERFLOW_COUNT);
        assertThat(events.readEvents(null, null))
                .as("the operator has to be told which tag lost data")
                .anySatisfy(event -> {
                    assertThat(event.getSeverity()).isEqualTo(Event.SEVERITY.WARN);
                    assertThat(event.getMessage()).contains("boiler-high-temp").contains("lost condition transitions");
                });
    }

    @Test
    void anOverflowOnAnEventSubscriptionTagIsReportedRatherThanDiscarded() {
        // A query tag carries many conditions, so an overflow there loses transitions from all of them.
        final OpcuaTag tag = new OpcuaTag(
                "area-alarms",
                "every alarm in the boiler area",
                new OpcuaTagDefinition(
                        "ns=2;s=BoilerArea", OpcuaTagKind.EVENT_SUBSCRIPTION, OpcuaConditionType.ALARM_CONDITION));
        final var handler = handlerFor(tag);

        handler.onEventReceived(
                mock(OpcUaSubscription.class),
                List.of(itemFor(tag)),
                List.<Variant[]>of(controlEvent(NodeIds.EventQueueOverflowEventType, "overflow-2")));

        verify(metrics).increment(Constants.METRIC_SUBSCRIPTION_EVENT_QUEUE_OVERFLOW_COUNT);
        assertThat(events.readEvents(null, null))
                .anySatisfy(event -> assertThat(event.getMessage()).contains("area-alarms"));
    }

    @Test
    void anOverflowIsStillNotPublishedAsATransitionReport() {
        // Reported, not published. Emitting a control event under a condition tag's declared field list would
        // produce an alarm whose every state field is null -- which reads as "this alarm's state is unknown"
        // and is worse than silence. A tag modelled for overflow is EDG-856.
        final OpcuaTag tag = conditionTag("boiler-high-temp");
        final var handler = handlerFor(tag);

        handler.onEventReceived(
                mock(OpcUaSubscription.class),
                List.of(itemFor(tag)),
                List.<Variant[]>of(controlEvent(NodeIds.EventQueueOverflowEventType, "overflow-3")));

        verify(publisher, never()).addDataPoint(any());
    }

    @Test
    void anOverflowOnARefreshTagIsBothReportedAndPublished() throws Exception {
        // A refresh tag's own item can overflow too, and then it is the affected tag. It publishes because
        // control events are what that tag is for, and it reports because the loss is the same loss.
        final OpcuaTag tag = refreshTag();
        final var handler = handlerFor(tag);
        when(client.getDynamicEncodingContext()).thenReturn(DefaultEncodingContext.INSTANCE);
        when(publisher.addDataPoint(any()))
                .thenReturn(mock(
                        DataPointBuilder.class, withSettings().defaultAnswer(org.mockito.Answers.RETURNS_DEEP_STUBS)));

        handler.onEventReceived(
                mock(OpcUaSubscription.class),
                List.of(itemFor(tag)),
                List.<Variant[]>of(controlEvent(NodeIds.EventQueueOverflowEventType, "overflow-4")));

        verify(metrics).increment(Constants.METRIC_SUBSCRIPTION_EVENT_QUEUE_OVERFLOW_COUNT);
        verify(publisher).addDataPoint(any());
    }

    @Test
    void anOrdinaryTransitionIsNotMistakenForAnOverflow() throws Exception {
        // The guard has to be the event type, not merely "not a transition".
        final OpcuaTag tag = conditionTag("boiler-high-temp");
        final var handler = handlerFor(tag);
        when(client.getDynamicEncodingContext()).thenReturn(DefaultEncodingContext.INSTANCE);
        when(publisher.addDataPoint(any()))
                .thenReturn(mock(
                        DataPointBuilder.class, withSettings().defaultAnswer(org.mockito.Answers.RETURNS_DEEP_STUBS)));

        handler.onEventReceived(
                mock(OpcUaSubscription.class),
                List.of(itemFor(tag)),
                List.<Variant[]>of(controlEvent(NodeIds.AlarmConditionType, "an-ordinary-alarm")));

        verify(metrics, never()).increment(Constants.METRIC_SUBSCRIPTION_EVENT_QUEUE_OVERFLOW_COUNT);
        verify(publisher).addDataPoint(any());
    }

    // ── finding 4: RefreshRequired coalescing ────────────────────────────────────────────────────────

    @Test
    void copiesOfOneRefreshRequiredCauseOneRefresh_evenAcrossCallbacks() {
        // The defect: coalescing was by call duration, not by event identity. One server-side occurrence is
        // copied to every notifier item (OPC 10000-9 §4.5), and nothing bounds those copies to a single
        // publish batch -- so a request that completes between two batches released the in-flight guard and
        // let the same occurrence start a second refresh. A completed future is the sharpest form of it, and
        // exactly what a stubbed client produces.
        final OpcuaTag tag = conditionTag("boiler-high-temp");
        final var handler = handlerFor(tag);
        final OpcUaSubscription subscription = established(handler, 4711);
        stubRefreshCallCompletingImmediately();

        handler.onEventReceived(
                subscription,
                List.of(itemFor(tag)),
                List.<Variant[]>of(controlEvent(NodeIds.RefreshRequiredEventType, "occurrence-A")));
        handler.onEventReceived(
                subscription,
                List.of(itemFor(tag)),
                List.<Variant[]>of(controlEvent(NodeIds.RefreshRequiredEventType, "occurrence-A")));
        handler.onEventReceived(
                subscription,
                List.of(itemFor(tag)),
                List.<Variant[]>of(controlEvent(NodeIds.RefreshRequiredEventType, "occurrence-A")));

        verify(client, times(1)).callAsync(any());
    }

    @Test
    void copiesOfOneRefreshRequiredInOneCallbackCauseOneRefresh() {
        // The same occurrence arriving on several items of one batch, which is the shape §4.5 describes.
        final OpcuaTag first = conditionTag("alarm-one");
        final OpcuaTag second = conditionTag("alarm-two");
        final var handler = handlerFor(first, second);
        stubRefreshCallCompletingImmediately();

        handler.onEventReceived(
                established(handler, 4711),
                List.of(itemFor(first), itemFor(second)),
                List.<Variant[]>of(
                        controlEvent(NodeIds.RefreshRequiredEventType, "occurrence-B"),
                        controlEvent(NodeIds.RefreshRequiredEventType, "occurrence-B")));

        verify(client, times(1)).callAsync(any());
    }

    @Test
    void aFreshRefreshRequiredCausesAnotherRefresh() {
        // Deduplication must not become suppression: a later occurrence is a new reason to resynchronise and
        // must not be swallowed by the memory of the previous one.
        final OpcuaTag tag = conditionTag("boiler-high-temp");
        final var handler = handlerFor(tag);
        final OpcUaSubscription subscription = established(handler, 4711);
        stubRefreshCallCompletingImmediately();

        handler.onEventReceived(
                subscription,
                List.of(itemFor(tag)),
                List.<Variant[]>of(controlEvent(NodeIds.RefreshRequiredEventType, "occurrence-C")));
        handler.onEventReceived(
                subscription,
                List.of(itemFor(tag)),
                List.<Variant[]>of(controlEvent(NodeIds.RefreshRequiredEventType, "occurrence-D")));

        verify(client, times(2)).callAsync(any());
    }

    // ── review-02 finding 2: an occurrence arriving while a call is outstanding ──────────────────────

    @Test
    void aDistinctRefreshRequiredArrivingDuringACallIsNotLost() {
        // The defect. The id was recorded as handled by isFirstSightOf and *then* dropped by the in-flight
        // guard, so nothing was left to retry it: the deduplication memory now says it has been dealt with,
        // and no state anywhere says otherwise. RefreshRequired means the server can no longer guarantee the
        // client is in sync, so losing one leaves the retained alarm picture stale indefinitely.
        //
        // Every earlier test returns an already-completed future, which is why none of them reached this: the
        // call settles before the next notification is delivered, and the overlap never happens.
        final OpcuaTag tag = conditionTag("boiler-high-temp");
        final var handler = handlerFor(tag);
        final OpcUaSubscription subscription = established(handler, 4711);
        final CompletableFuture<CallResponse> outstanding = new CompletableFuture<>();
        when(client.callAsync(any())).thenReturn(outstanding, completedRefreshCall());

        refreshRequired(handler, subscription, tag, "occurrence-E");
        refreshRequired(handler, subscription, tag, "occurrence-E"); // a copy of it, which must add nothing
        refreshRequired(handler, subscription, tag, "occurrence-F"); // a genuinely different one

        verify(client, times(1)).callAsync(any());

        outstanding.complete(goodCallResponse());

        verify(client, times(2)).callAsync(any());
    }

    @Test
    void copiesOfOneOccurrenceArrivingDuringACallStillAddNothing() {
        // The property the fix must not cost. Coalescing by identity has to keep working while a call is
        // outstanding, or the pending flag simply moves the duplicate refresh to just after the call instead
        // of just after the burst -- which is the collision the specification defines Bad_RefreshInProgress
        // for, only later.
        final OpcuaTag tag = conditionTag("boiler-high-temp");
        final var handler = handlerFor(tag);
        final OpcUaSubscription subscription = established(handler, 4711);
        final CompletableFuture<CallResponse> outstanding = new CompletableFuture<>();
        when(client.callAsync(any())).thenReturn(outstanding, completedRefreshCall());

        refreshRequired(handler, subscription, tag, "occurrence-G");
        refreshRequired(handler, subscription, tag, "occurrence-G");
        refreshRequired(handler, subscription, tag, "occurrence-G");

        outstanding.complete(goodCallResponse());

        verify(client, times(1)).callAsync(any());
    }

    @Test
    void severalDistinctOccurrencesDuringOneCallCollapseIntoOneFollowUp() {
        // Why a flag rather than a queue of ids. OPC 10000-9 §4.5 makes a ConditionRefresh subscription-wide,
        // so one call covers every reason outstanding when it starts. Three reasons that arrived during the
        // first call are all answered by the second; a queue would make three calls and the last two would
        // collide with the first.
        final OpcuaTag tag = conditionTag("boiler-high-temp");
        final var handler = handlerFor(tag);
        final OpcUaSubscription subscription = established(handler, 4711);
        final CompletableFuture<CallResponse> outstanding = new CompletableFuture<>();
        when(client.callAsync(any())).thenReturn(outstanding, completedRefreshCall());

        refreshRequired(handler, subscription, tag, "occurrence-H");
        refreshRequired(handler, subscription, tag, "occurrence-I");
        refreshRequired(handler, subscription, tag, "occurrence-J");
        refreshRequired(handler, subscription, tag, "occurrence-K");

        outstanding.complete(goodCallResponse());

        verify(client, times(2)).callAsync(any());
    }

    @Test
    void aFailedCallStillPicksUpTheWorkThatArrivedDuringIt() {
        // The pending occurrence asked for the server to be re-read, not for this particular attempt to
        // succeed. Draining only on success would make a transient failure swallow a reason that outlives it.
        final OpcuaTag tag = conditionTag("boiler-high-temp");
        final var handler = handlerFor(tag);
        final OpcUaSubscription subscription = established(handler, 4711);
        final CompletableFuture<CallResponse> outstanding = new CompletableFuture<>();
        when(client.callAsync(any())).thenReturn(outstanding, completedRefreshCall());

        refreshRequired(handler, subscription, tag, "occurrence-L");
        refreshRequired(handler, subscription, tag, "occurrence-M");

        outstanding.completeExceptionally(new IllegalStateException("the session went away"));

        verify(client, times(2)).callAsync(any());
    }

    @Test
    void anOccurrenceThatArrivesAfterTheCallSettlesIsHandledDirectly() {
        // The drain must not become the only way in. With no call outstanding the flag is raised and claimed
        // by the same thread, and the refresh happens immediately rather than waiting for a completion that
        // has already been and gone.
        final OpcuaTag tag = conditionTag("boiler-high-temp");
        final var handler = handlerFor(tag);
        final OpcUaSubscription subscription = established(handler, 4711);
        stubRefreshCallCompletingImmediately();

        refreshRequired(handler, subscription, tag, "occurrence-N");
        refreshRequired(handler, subscription, tag, "occurrence-O");
        refreshRequired(handler, subscription, tag, "occurrence-P");

        verify(client, times(3)).callAsync(any());
    }

    // ── review-03 finding 3: the coordinator outlives the subscription ──────────────────────────────

    @Test
    void aPendingRefreshIsSentToTheSubscriptionThatIsCurrentWhenTheCallIsMade() {
        // The defect. Both coalescing flags are handler-global while a subscription is not, and every drain
        // carried the subscription that happened to deliver the notification. So an old generation's
        // completion claimed the new generation's pending work and called ConditionRefresh with the id the
        // server had already refused to transfer -- consuming the flag on the way, which left the new
        // subscription with a refresh asked of it, a Bad_SubscriptionIdInvalid against the dead one, and
        // nothing to retry it. Exactly the window in which the retained alarm picture matters most.
        final OpcuaTag tag = conditionTag("boiler-high-temp");
        final var handler = handlerFor(tag);
        final OpcUaSubscription old = established(handler, 4711);
        final CompletableFuture<CallResponse> outstanding = new CompletableFuture<>();
        when(client.callAsync(any())).thenReturn(outstanding, completedRefreshCall());

        refreshRequired(handler, old, tag, "occurrence-old");
        verify(client, times(1)).callAsync(any());

        // The transfer fails and a replacement is established, as onTransferFailed then recreateSubscription
        // would do -- while the first refresh is still outstanding.
        final OpcUaSubscription replacement = established(handler, 4712);
        refreshRequired(handler, replacement, tag, "occurrence-new");

        // Still one call: the in-flight guard is held by the old generation's request.
        verify(client, times(1)).callAsync(any());

        outstanding.complete(goodCallResponse());

        assertThat(refreshedSubscriptionIds())
                .as("the second refresh must name the subscription that is live, not the one that died")
                .containsExactly(uint(4711), uint(4712));
    }

    @Test
    void aPendingRefreshWithNoSubscriptionLeftDoesNotCallAgainstTheDeadOne() {
        // The other half of the same window: the replacement is not established yet. There is nothing to
        // refresh, and calling with the old id would be a request against a subscription the server has
        // already refused to transfer. established() refreshes every subscription it installs, so the reason
        // this occurrence was raised for is answered by the rebuild rather than dropped.
        final OpcuaTag tag = conditionTag("boiler-high-temp");
        final var handler = handlerFor(tag);
        final OpcUaSubscription old = established(handler, 4711);
        final CompletableFuture<CallResponse> outstanding = new CompletableFuture<>();
        when(client.callAsync(any())).thenReturn(outstanding, completedRefreshCall());

        refreshRequired(handler, old, tag, "occurrence-old");
        refreshRequired(handler, old, tag, "occurrence-new");

        handler.onTransferFailed(old, new StatusCode(StatusCodes.Bad_SubscriptionIdInvalid));
        outstanding.complete(goodCallResponse());

        assertThat(refreshedSubscriptionIds())
                .as("no second call, rather than a second call against 4711")
                .containsExactly(uint(4711));
    }

    @Test
    void andTheGuardIsReleasedSoALaterOccurrenceStillRefreshes() {
        // The property the branch above must not cost. Skipping the call while no subscription exists has to
        // hand the in-flight guard back, or the first RefreshRequired after a transfer failure would wedge it
        // for the rest of the connection -- and the skip is a bare return, so nothing would say so.
        final OpcuaTag tag = conditionTag("boiler-high-temp");
        final var handler = handlerFor(tag);
        final OpcUaSubscription old = established(handler, 4711);
        final CompletableFuture<CallResponse> outstanding = new CompletableFuture<>();
        when(client.callAsync(any())).thenReturn(outstanding, completedRefreshCall());

        refreshRequired(handler, old, tag, "occurrence-old");
        refreshRequired(handler, old, tag, "occurrence-new");
        handler.onTransferFailed(old, new StatusCode(StatusCodes.Bad_SubscriptionIdInvalid));
        outstanding.complete(goodCallResponse());

        final OpcUaSubscription replacement = established(handler, 4712);
        refreshRequired(handler, replacement, tag, "occurrence-later");

        assertThat(refreshedSubscriptionIds()).containsExactly(uint(4711), uint(4712));
    }

    @Test
    void aNotificationFromAReplacedSubscriptionIsIgnored() {
        // A superseded subscription has no business publishing: its items were re-established on the
        // replacement, so anything still arriving on it is a transition the new generation reports as well.
        final OpcuaTag tag = conditionTag("boiler-high-temp");
        final var handler = handlerFor(tag);
        final OpcUaSubscription old = subscriptionWithId(4711);
        established(handler, 4712);
        stubRefreshCallCompletingImmediately();

        refreshRequired(handler, old, tag, "occurrence-stale");

        verify(client, never()).callAsync(any());
        verify(publisher, never()).addDataPoint(any());
    }

    @Test
    void butOneArrivingBeforeTheSubscriptionIsRecordedIsStillDelivered() throws Exception {
        // The distinction that makes the guard above safe, and the reason it tests for a *different*
        // subscription rather than for the absence of one. established() records the subscription after
        // monitored-item synchronization, so there is a real interval in which the server publishes and the
        // handler has not stored it yet. Dropping those would lose alarms at connect time, with no symptom.
        final OpcuaTag tag = conditionTag("boiler-high-temp");
        final var handler = handlerFor(tag);
        when(client.getDynamicEncodingContext()).thenReturn(DefaultEncodingContext.INSTANCE);
        when(publisher.addDataPoint(any()))
                .thenReturn(mock(
                        DataPointBuilder.class, withSettings().defaultAnswer(org.mockito.Answers.RETURNS_DEEP_STUBS)));

        handler.onEventReceived(
                subscriptionWithId(4711),
                List.of(itemFor(tag)),
                List.<Variant[]>of(controlEvent(NodeIds.AlarmConditionType, "an-alarm-during-startup")));

        verify(publisher).addDataPoint(any());
    }

    @Test
    void aRefreshRequiredTheOldGenerationHandledIsActedOnAgainOnTheNewOne() {
        // The deduplication memory is a per-generation fact. A new subscription is a new conversation, and a
        // server re-reporting the same RefreshRequired to it is asking this subscription to resynchronise --
        // suppressing that as a duplicate would be answering on behalf of one that no longer exists.
        final OpcuaTag tag = conditionTag("boiler-high-temp");
        final var handler = handlerFor(tag);
        stubRefreshCallCompletingImmediately();

        refreshRequired(handler, established(handler, 4711), tag, "occurrence-repeated");
        refreshRequired(handler, established(handler, 4712), tag, "occurrence-repeated");

        assertThat(refreshedSubscriptionIds()).containsExactly(uint(4711), uint(4712));
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

    private void stubRefreshCallCompletingImmediately() {
        when(client.callAsync(any())).thenReturn(completedRefreshCall());
    }

    private static @NotNull CompletableFuture<CallResponse> completedRefreshCall() {
        return CompletableFuture.completedFuture(goodCallResponse());
    }

    private static @NotNull CallResponse goodCallResponse() {
        return new CallResponse(
                new ResponseHeader(null, uint(0), StatusCode.GOOD, null, null, null),
                new CallMethodResult[] {new CallMethodResult(StatusCode.GOOD, null, null, null)},
                null);
    }

    private static @NotNull OpcUaSubscription subscriptionWithId() {
        return subscriptionWithId(4711);
    }

    private static @NotNull OpcUaSubscription subscriptionWithId(final int id) {
        final OpcUaSubscription subscription = mock(OpcUaSubscription.class);
        when(subscription.getSubscriptionId()).thenReturn(Optional.of(uint(id)));
        return subscription;
    }

    /**
     * A subscription the handler has been told is the current generation.
     * <p>
     * Installing it is not scaffolding: a refresh is sent to whichever subscription is current when the call
     * is made, not to the one that delivered the notification asking for it, so a handler that has never
     * established one has nothing to refresh. That is the review-03 finding 3 fix, and a test driving
     * {@code onEventReceived} against a subscription the handler has never heard of would be modelling a
     * situation that cannot arise — notifications only arrive on a subscription that was established.
     */
    private static @NotNull OpcUaSubscription established(
            final @NotNull OpcUaSubscriptionLifecycleHandler handler, final int id) {

        final OpcUaSubscription subscription = subscriptionWithId(id);
        handler.installSubscriptionForTesting(subscription);
        return subscription;
    }

    /** The subscription id each {@code ConditionRefresh} named, in call order. */
    @SuppressWarnings("unchecked")
    private @NotNull List<UInteger> refreshedSubscriptionIds() {
        final ArgumentCaptor<List<CallMethodRequest>> captor = ArgumentCaptor.forClass(List.class);
        verify(client, org.mockito.Mockito.atLeastOnce()).callAsync(captor.capture());
        return captor.getAllValues().stream()
                .flatMap(List::stream)
                .map(request -> (UInteger) request.getInputArguments()[0].value())
                .toList();
    }

    private static @NotNull OpcuaTag conditionTag(final @NotNull String name) {
        return new OpcuaTag(
                name,
                "a condition tag",
                new OpcuaTagDefinition("ns=2;s=" + name, OpcuaTagKind.CONDITION, OpcuaConditionType.ALARM_CONDITION));
    }

    private static @NotNull OpcuaTag refreshTag() {
        return new OpcuaTag(
                "refresh",
                "the adapter's refresh channel",
                new OpcuaTagDefinition("ns=0;i=2253", OpcuaTagKind.REFRESH));
    }

    private static @NotNull OpcUaMonitoredItem itemFor(final @NotNull OpcuaTag tag) {
        final OpcUaMonitoredItem item = mock(OpcUaMonitoredItem.class);
        when(item.getUserObject()).thenReturn(Optional.of(tag));
        return item;
    }

    /**
     * An event carrying only the two fields the routing reads, at the positions the select clause puts them.
     * <p>
     * {@code EventId} and {@code EventType} are the first two entries of {@code BASE_EVENT_FIELDS}, which
     * every select clause begins with whatever type a tag declares — that is what makes reading them
     * positionally correct. The rest of the array is left short on purpose: a server may return fewer values
     * than were selected, and the converter treats the tail as null.
     */
    private static @NotNull Variant[] controlEvent(final @NotNull NodeId eventType, final @NotNull String eventId) {
        final List<String> base = OpcuaConditionType.BASE_EVENT_FIELDS;
        final Variant[] fields = new Variant[base.size()];
        fields[base.indexOf("EventId")] = Variant.of(new ByteString(eventId.getBytes(StandardCharsets.UTF_8)));
        fields[base.indexOf("EventType")] = Variant.of(eventType);
        return fields;
    }
}
