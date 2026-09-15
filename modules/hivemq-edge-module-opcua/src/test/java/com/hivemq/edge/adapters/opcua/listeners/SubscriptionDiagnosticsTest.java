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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.encoding.DefaultEncodingContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExtensionObject;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodResult;
import org.eclipse.milo.opcua.stack.core.types.structured.CallResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ContentFilterElementResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ContentFilterResult;
import org.eclipse.milo.opcua.stack.core.types.structured.EventFilterResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ResponseHeader;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Review-09 finding 1: reading the server's optional commentary must not decide whether we keep the
 * subscription it is commenting on.
 * <p>
 * A monitored item's {@code filterResult} is where the server says which of the fields we selected it would
 * not honour. It is optional twice over, and Java models only one of those: {@link Optional} distinguishes a
 * null <em>reference</em>, while the OPC UA {@code ExtensionObject} it holds has its own idea of empty — no
 * body, or no encoding id to identify one by. The OPC Foundation reference server answers the second way,
 * and {@code decode()} resolves its codec from the encoding id before it ever looks at a body, so it threw
 * {@code no codec registered for encodingId=i=0} on being told there was nothing to report.
 * <p>
 * What made that a release blocker rather than a bad log line is where it was thrown from. {@code
 * established()} runs on the connection-attempt future, so the exception failed the attempt, reported {@code
 * ERROR}, and handed the adapter to exponential backoff — which reconnected, subscribed, arrived here and
 * threw again. QA saw six restarts from one condition tag while a value tag on the same adapter and server
 * was fine (EDG-894 P1/P2). The subscription had been accepted by the server every time.
 * <p>
 * So the tests below are all one assertion in different clothes: whatever the filter result turns out to be,
 * the subscription is still established, still installed as the current generation, and still refreshed.
 * Only the log line changes.
 */
class SubscriptionDiagnosticsTest {

    private static final @NotNull String ADAPTER_ID = "test-adapter";

    /** The reference server's answer: a body that is present but empty, identified by nothing. */
    private static final @NotNull ExtensionObject NOTHING_TO_REPORT =
            ExtensionObject.of(ByteString.of(new byte[0]), NodeId.NULL_VALUE);

    /** The other shape of the same answer: an id that names a type we can decode, and no body under it. */
    private static final @NotNull ExtensionObject NO_BODY = ExtensionObject.of(
            ByteString.NULL_VALUE,
            EventFilterResult.BINARY_ENCODING_ID.toNodeId(new NamespaceTable()).orElseThrow());

    private @NotNull ProtocolAdapterMetricsService metrics;
    private @NotNull ProtocolAdapterTagStreamingService streaming;
    private @NotNull FakeEventService events;
    private @NotNull OpcUaClient client;
    private @NotNull ListAppender<ILoggingEvent> logged;
    private @NotNull Logger handlerLog;

    @BeforeEach
    void setUp() {
        metrics = mock(ProtocolAdapterMetricsService.class);
        streaming = mock(ProtocolAdapterTagStreamingService.class);
        events = new FakeEventService();
        client = mock(OpcUaClient.class);
        when(streaming.dataPointsPublisher()).thenReturn(mock(DataPointListBuilder.class));
        when(client.getStaticEncodingContext()).thenReturn(DefaultEncodingContext.INSTANCE);
        when(client.callAsync(any())).thenReturn(CompletableFuture.completedFuture(goodCallResponse()));

        handlerLog = (Logger) LoggerFactory.getLogger(OpcUaSubscriptionLifecycleHandler.class);
        logged = new ListAppender<>();
        logged.start();
        handlerLog.addAppender(logged);
        handlerLog.setLevel(Level.DEBUG);
    }

    @AfterEach
    void tearDown() {
        handlerLog.detachAppender(logged);
        handlerLog.setLevel(null);
    }

    // ── the two shapes of "nothing to report" ────────────────────────────────────────────────────────

    @Test
    void theReferenceServersEmptyFilterResultDoesNotCostTheSubscription() {
        // Verbatim the EDG-894 P1 case: one ordinary condition tag, and a server with no rejected fields to
        // name. Before the fix this line threw UaSerializationException and the adapter never started.
        final OpcuaTag tag = conditionTag("plainConditionTag");
        final var handler = handlerFor(tag);

        assertThat(handler.establishedForTesting(subscriptionReporting(tag, NOTHING_TO_REPORT)))
                .as("the server accepted the subscription; reading its commentary cannot un-accept it")
                .isTrue();

        assertThat(handler.currentSubscriptionForTesting())
                .as("installed as the current generation, so the refresh has something to address")
                .isNotNull();
        verify(client).callAsync(any());
    }

    @Test
    void norDoesTheOtherShapeOfTheSameAnswer() {
        // An encoding id that names a type we do have a codec for, and no body under it. isNull() catches
        // this one where the encoding-id check would not, which is why the guard tests both.
        final OpcuaTag tag = conditionTag("plainConditionTag");
        final var handler = handlerFor(tag);

        assertThat(handler.establishedForTesting(subscriptionReporting(tag, NO_BODY)))
                .isTrue();
        assertThat(handler.currentSubscriptionForTesting()).isNotNull();
        verify(client).callAsync(any());
    }

    @Test
    void andNeitherIsWorthATellingAnOperatorAbout() {
        // The point of separating "empty" from "undecodable". An empty result is the ordinary answer from a
        // server with nothing to complain about; logging it would put a line in front of the operator on
        // every connect, for every event tag, saying nothing was wrong.
        final OpcuaTag tag = conditionTag("plainConditionTag");
        handlerFor(tag).establishedForTesting(subscriptionReporting(tag, NOTHING_TO_REPORT));

        assertThat(logged.list)
                .as("an empty diagnostic is not a diagnostic")
                .noneMatch(event -> event.getFormattedMessage().contains("plainConditionTag"));
    }

    // ── what the guard cannot anticipate ─────────────────────────────────────────────────────────────

    @Test
    void anUndecodableFilterResultCostsALogLineRatherThanTheAdapter() {
        // A body encoded against a type this client has no codec for. The emptiness check cannot help here --
        // the object claims to hold something -- so this is the catch, and the catch is the reason the fix
        // is not simply a null test.
        final OpcuaTag tag = conditionTag("plainConditionTag");
        final var handler = handlerFor(tag);
        final ExtensionObject undecodable =
                ExtensionObject.of(ByteString.of(new byte[] {1, 2, 3}), NodeId.parse("ns=2;i=5001"));

        assertThat(handler.establishedForTesting(subscriptionReporting(tag, undecodable)))
                .isTrue();
        assertThat(handler.currentSubscriptionForTesting()).isNotNull();
        verify(client).callAsync(any());

        assertThat(logged.list)
                .as("named, so someone chasing a missing field can tell this apart from a clean result")
                .anyMatch(event -> event.getLevel() == Level.DEBUG
                        && event.getFormattedMessage().contains("plainConditionTag")
                        && event.getFormattedMessage().contains("could not be decoded"));
    }

    @Test
    void andAnythingElseThatThrowsInTheDiagnosticsIsContainedToo() {
        // The fence in established(), tested through the other reporter. Neither of these two methods is
        // about whether the subscription exists, so neither may be able to answer that question -- including
        // by a route nobody has thought of yet.
        final OpcuaTag tag = conditionTag("plainConditionTag");
        final var handler = handlerFor(tag);
        final OpcUaMonitoredItem item = itemFor(tag);
        when(item.getRevisedQueueSize()).thenThrow(new IllegalStateException("the queue size is unreadable"));

        assertThat(handler.establishedForTesting(subscriptionOf(item))).isTrue();
        assertThat(handler.currentSubscriptionForTesting()).isNotNull();
        verify(client).callAsync(any());

        assertThat(logged.list)
                .as("contained, but not silently -- the fence still says it caught something")
                .anyMatch(event -> event.getLevel() == Level.WARN
                        && event.getFormattedMessage().contains("failure to describe it, not a failure of it"));
    }

    // ── and the diagnostic still works when there is one ─────────────────────────────────────────────

    @Test
    void aFilterResultThatDoesDecodeStillNamesTheRejectedFields() {
        // The guard must not have been bought by dropping the feature. A server that genuinely rejects a
        // select clause still gets its WARN, which is the whole reason this code exists.
        final OpcuaTag tag = conditionTag("plainConditionTag");
        final var handler = handlerFor(tag);
        final ExtensionObject rejecting = ExtensionObject.encode(
                DefaultEncodingContext.INSTANCE,
                new EventFilterResult(
                        new StatusCode[] {StatusCode.GOOD, new StatusCode(StatusCodes.Bad_NodeIdUnknown)},
                        null,
                        new ContentFilterResult(new ContentFilterElementResult[0], null)));

        assertThat(handler.establishedForTesting(subscriptionReporting(tag, rejecting)))
                .isTrue();

        assertThat(logged.list)
                .as("the rejection is reported, and reported as a rejection")
                .anyMatch(event -> event.getLevel() == Level.WARN
                        && event.getFormattedMessage().contains("plainConditionTag")
                        && event.getFormattedMessage().contains("the server rejected"));
    }

    @Test
    void aValueTagsFilterResultIsNotReadAtAll() {
        // Unchanged by the fix, and worth holding: a value tag has no select clause, so anything in this
        // field belongs to a conversation we are not having.
        final OpcuaTag tag = valueTag("plainValueTag");
        final var handler = handlerFor(tag);

        assertThat(handler.establishedForTesting(subscriptionReporting(tag, NOTHING_TO_REPORT)))
                .isTrue();
        verify(client, never()).callAsync(any());
    }

    // ── fixtures ─────────────────────────────────────────────────────────────────────────────────────

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

    /** A subscription carrying one item for {@code tag}, whose filter result is {@code filterResult}. */
    private static @NotNull OpcUaSubscription subscriptionReporting(
            final @NotNull OpcuaTag tag, final @NotNull ExtensionObject filterResult) {

        final OpcUaMonitoredItem item = itemFor(tag);
        when(item.getFilterResult()).thenReturn(Optional.of(filterResult));
        return subscriptionOf(item);
    }

    private static @NotNull OpcUaSubscription subscriptionOf(final @NotNull OpcUaMonitoredItem item) {
        final OpcUaSubscription subscription = mock(OpcUaSubscription.class);
        when(subscription.getSubscriptionId()).thenReturn(Optional.of(uint(4711)));
        when(subscription.getMonitoredItems()).thenReturn(List.of(item));
        return subscription;
    }

    private static @NotNull OpcUaMonitoredItem itemFor(final @NotNull OpcuaTag tag) {
        final OpcUaMonitoredItem item = mock(OpcUaMonitoredItem.class);
        when(item.getUserObject()).thenReturn(Optional.of(tag));
        when(item.getRevisedQueueSize()).thenReturn(Optional.empty());
        return item;
    }

    private static @NotNull OpcuaTag conditionTag(final @NotNull String name) {
        return new OpcuaTag(
                name,
                "a condition tag",
                new OpcuaTagDefinition("ns=2;s=" + name, OpcuaTagKind.CONDITION, OpcuaConditionType.ALARM_CONDITION));
    }

    private static @NotNull OpcuaTag valueTag(final @NotNull String name) {
        return new OpcuaTag(name, "a value tag", new OpcuaTagDefinition("ns=2;s=" + name));
    }

    private static @NotNull CallResponse goodCallResponse() {
        return new CallResponse(
                new ResponseHeader(null, uint(0), StatusCode.GOOD, null, null, null),
                new CallMethodResult[] {new CallMethodResult(StatusCode.GOOD, null, null, null)},
                null);
    }
}
