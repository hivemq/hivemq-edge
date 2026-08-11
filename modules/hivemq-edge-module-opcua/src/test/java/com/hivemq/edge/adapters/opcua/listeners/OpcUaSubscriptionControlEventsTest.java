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
import org.eclipse.milo.opcua.stack.core.encoding.DefaultEncodingContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodResult;
import org.eclipse.milo.opcua.stack.core.types.structured.CallResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ResponseHeader;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        final OpcUaSubscription subscription = subscriptionWithId();
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
                subscriptionWithId(),
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
        final OpcUaSubscription subscription = subscriptionWithId();
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

    // ── helpers ─────────────────────────────────────────────────────────────────────────────────────

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
        when(client.callAsync(any()))
                .thenReturn(CompletableFuture.completedFuture(new CallResponse(
                        new ResponseHeader(null, uint(0), StatusCode.GOOD, null, null, null),
                        new CallMethodResult[] {new CallMethodResult(StatusCode.GOOD, null, null, null)},
                        null)));
    }

    private static @NotNull OpcUaSubscription subscriptionWithId() {
        final OpcUaSubscription subscription = mock(OpcUaSubscription.class);
        when(subscription.getSubscriptionId()).thenReturn(Optional.of(uint(4711)));
        return subscription;
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
