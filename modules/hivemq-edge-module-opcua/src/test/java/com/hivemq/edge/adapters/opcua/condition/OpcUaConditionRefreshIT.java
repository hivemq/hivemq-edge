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
package com.hivemq.edge.adapters.opcua.condition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.ProtocolAdapterConnectionDirection;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.factories.AdapterFactories;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.adapter.sdk.api.writing.WritingContext;
import com.hivemq.adapter.sdk.api.writing.WritingInput;
import com.hivemq.adapter.sdk.api.writing.WritingOutput;
import com.hivemq.edge.adapters.opcua.FakeEventService;
import com.hivemq.edge.adapters.opcua.OpcUaProtocolAdapter;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagKind;
import com.hivemq.edge.adapters.opcua.southbound.OpcUaPayload;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import util.EmbeddedOpcUaServerExtension;

/**
 * Checks that Edge asks the server to re-report its retained conditions once a subscription is established.
 * <p>
 * Transitions are events, so a condition that went active while Edge was disconnected has already fired and
 * cannot be re-sent. Without this call the current alarm picture stays unknown until each alarm next changes
 * on its own — which for a stable plant could be a very long time.
 */
public class OpcUaConditionRefreshIT {

    private static final long CONDITION_NODE_ID = 9400L;

    @RegisterExtension
    public final @NotNull EmbeddedOpcUaServerExtension opcUaServerExtension = new EmbeddedOpcUaServerExtension();

    private @Nullable OpcUaProtocolAdapter adapter;
    private @NotNull ProtocolAdapterState protocolAdapterState;
    private @NotNull FakeEventService eventService;

    @BeforeEach
    void setUp() {
        protocolAdapterState = new ProtocolAdapterStateImpl(mock(), "test-adapter-id", "opcua");
        eventService = new FakeEventService();
    }

    @AfterEach
    void tearDown() {
        if (adapter != null) {
            adapter.destroy();
        }
    }

    @Test
    @Timeout(120)
    void whenAConditionTagIsSubscribed_thenTheServerIsAskedToRefresh() throws Exception {
        opcUaServerExtension.getTestNamespace().observeRefreshEvents();
        final String conditionNodeId = opcUaServerExtension
                .getTestNamespace()
                .addAcknowledgeableConditionNode("RefreshableAlarm", CONDITION_NODE_ID);

        startAdapterWith(
                new OpcuaTag("refresh-alarm", "", new OpcuaTagDefinition(conditionNodeId, OpcuaTagKind.CONDITION)));

        // Observed through the server's response, not through our namespace: OPC 10000-9 §5.5.7 fixes the
        // call's ObjectId as the well-known ConditionType (ns=0;i=2782), so it never reaches the test
        // namespace's own method handlers. A successful call makes the server emit RefreshStart/RefreshEnd,
        // which is the visible consequence.
        await().untilAsserted(
                        () -> assertThat(opcUaServerExtension.getTestNamespace().refreshBracketCount())
                                .as("the server must be asked to re-report its retained conditions")
                                .isPositive());
    }

    @Test
    @Timeout(120)
    void whenTheServerSaysARefreshIsRequired_thenOneIsRequested() throws Exception {
        // OPC 10000-9 §4.5: "A Client receiving this special Event should initiate a ConditionRefresh". The
        // server sends it when it can no longer guarantee the client is in sync -- a reset of the system
        // beneath it, or an event queue that overflowed and drained. Nothing else recovers from that: our
        // session stays healthy throughout, so no reconnect path fires, and the alarm picture would stay
        // stale until every affected condition happened to change state again.
        opcUaServerExtension.getTestNamespace().observeRefreshEvents();
        // Exists so the area has a condition to re-report; the tag below subscribes to the area, not to it.
        opcUaServerExtension.getTestNamespace().addAcknowledgeableConditionNode("StaleAlarm", CONDITION_NODE_ID + 5);

        // An EVENT_SUBSCRIPTION tag on the area, unnarrowed. A CONDITION tag would be the more natural
        // subject, but its where clause pins ConditionId to one alarm and Milo evaluates that clause against
        // every event -- it does not implement the §4.5 rule that the refresh types bypass filtering, so a
        // RefreshRequired fired here would never reach the adapter through a condition tag's item. The
        // adapter's own handling is what is under test, and it is indifferent to which tag the event arrives
        // on: any notifier item carries it, and the refresh it triggers covers the whole subscription.
        startAdapterWith(new OpcuaTag(
                "area-events",
                "",
                new OpcuaTagDefinition(
                        opcUaServerExtension.getTestNamespace().areaNotifier().toParseableString(),
                        OpcuaTagKind.EVENT_SUBSCRIPTION)));

        // A refresh already fires on connect, so what is asserted is a further one -- measured from whatever
        // the connect-time refresh left behind rather than from zero.
        await().untilAsserted(
                        () -> assertThat(opcUaServerExtension.getTestNamespace().refreshBracketCount())
                                .isPositive());
        final int afterConnect = opcUaServerExtension.getTestNamespace().refreshBracketCount();

        opcUaServerExtension.getTestNamespace().fireRefreshRequired();

        await().untilAsserted(
                        () -> assertThat(opcUaServerExtension.getTestNamespace().refreshBracketCount())
                                .as("the server asked for a refresh, so the adapter must request one")
                                .isGreaterThan(afterConnect));
    }

    @Test
    @Timeout(120)
    void whenNoTagIsACondition_thenNoRefreshIsRequested() throws Exception {
        // A refresh only makes sense when something is subscribed to receive the burst. Asking anyway would
        // be a pointless round trip against every server Edge talks to.
        final String valueNodeId = opcUaServerExtension
                .getTestNamespace()
                .addNode("Counter", org.eclipse.milo.opcua.stack.core.NodeIds.Int32, () -> 42, CONDITION_NODE_ID + 1);

        startAdapterWith(new OpcuaTag("plain-value", "", new OpcuaTagDefinition(valueNodeId, OpcuaTagKind.VALUE)));

        await().untilAsserted(() -> assertThat(protocolAdapterState.getConnectionStatus())
                .isEqualTo(ProtocolAdapterState.ConnectionStatus.CONNECTED));

        Thread.sleep(2000);

        assertThat(opcUaServerExtension.getTestNamespace().methodCalls())
                .as("a value-only adapter must not call ConditionRefresh")
                .noneSatisfy(call -> assertThat(call.methodName()).isEqualTo("ConditionRefresh"));
    }

    @Test
    @Timeout(120)
    void whenOneTagFailsToSubscribe_thenTheHealthyOnesStillGetTheirRefresh() throws Exception {
        // EDG-835: a partial monitored-item sync used to report success without recording the subscription,
        // which left the handler's own state denying a subscription the server was serving. Nothing here is
        // about the failed tag: the subscription id comes from creating the subscription, well before any
        // monitored item exists, so losing the reference cost the refresh for every *healthy* tag -- on
        // connect, on every reconnect, and for any southbound refresh request, which answered "no
        // subscription is established yet" about one that plainly was.
        opcUaServerExtension.getTestNamespace().observeRefreshEvents();
        final String conditionNodeId = opcUaServerExtension
                .getTestNamespace()
                .addAcknowledgeableConditionNode("PartialSyncAlarm", CONDITION_NODE_ID);

        // A node the server does not have, so its monitored item is rejected while the condition's succeeds.
        final OpcuaTag missing = new OpcuaTag(
                "absent-node", "", new OpcuaTagDefinition("ns=2;s=NoSuchNodeAnywhere", OpcuaTagKind.VALUE));

        startAdapterWith(
                new OpcuaTag("partial-alarm", "", new OpcuaTagDefinition(conditionNodeId, OpcuaTagKind.CONDITION)),
                missing);

        await().untilAsserted(
                        () -> assertThat(opcUaServerExtension.getTestNamespace().refreshBracketCount())
                                .as("one rejected tag must not cost the healthy tags their refresh")
                                .isPositive());
    }

    @Test
    @Timeout(120)
    void whenARefreshTagsNodeIsAPlaceholder_thenItsRefreshCommandStillWorks() throws Exception {
        // Review-02 finding 7. A refresh tag's node "plays no part" -- the call names the well-known
        // ConditionType and carries the subscription id -- and subscription verification says so by not
        // parsing it at all. The write path parsed it anyway, before dispatching on kind, so a tag that
        // starts, subscribes and publishes control events perfectly well threw on the one command it exists
        // to accept. The throw landed inside an ifPresentOrElse consumer with nothing to map it to a
        // failure, so the write did not fail either: it never answered.
        //
        // The node below is deliberately not a node id. "ns=2;s=" would not do -- Milo parses that happily
        // as a string identifier that happens to be empty -- so this is a plain word, which cannot be one.
        opcUaServerExtension.getTestNamespace().observeRefreshEvents();
        opcUaServerExtension.getTestNamespace().addAcknowledgeableConditionNode("RefreshableAlarm", CONDITION_NODE_ID);

        startAdapterWith(new OpcuaTag("refresh", "", new OpcuaTagDefinition("not-a-node-id", OpcuaTagKind.REFRESH)));

        await().untilAsserted(() -> assertThat(protocolAdapterState.getConnectionStatus())
                .as("a placeholder node must not stop a refresh tag starting -- verification never reads it")
                .isEqualTo(ProtocolAdapterState.ConnectionStatus.CONNECTED));
        await().untilAsserted(
                        () -> assertThat(opcUaServerExtension.getTestNamespace().refreshBracketCount())
                                .isPositive());
        final int afterConnect = opcUaServerExtension.getTestNamespace().refreshBracketCount();

        final WritingOutput output = writeToRefreshTag("""
                { "method": "REFRESH" }
                """);

        verify(output, timeout(10_000)).finish();
        verify(output, never()).fail(anyString());
        await().untilAsserted(
                        () -> assertThat(opcUaServerExtension.getTestNamespace().refreshBracketCount())
                                .as("the command has to reach the server, not merely avoid throwing")
                                .isGreaterThan(afterConnect));
    }

    private @NotNull WritingOutput writeToRefreshTag(final @NotNull String json) throws Exception {
        final WritingContext writingContext = mock(WritingContext.class);
        when(writingContext.getTagName()).thenReturn("refresh");

        final WritingInput writingInput = mock(WritingInput.class);
        when(writingInput.getWritingContext()).thenReturn(writingContext);
        when(writingInput.getWritingPayload()).thenReturn(new OpcUaPayload(new ObjectMapper().readTree(json)));

        final WritingOutput output = mock(WritingOutput.class);
        final OpcUaProtocolAdapter started = adapter;
        if (started == null) {
            throw new IllegalStateException("the adapter has not been started");
        }
        started.write(writingInput, output);
        return output;
    }

    private void startAdapterWith(final @NotNull OpcuaTag @NotNull ... tags) {
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(
                opcUaServerExtension.getServerUri(),
                false,
                null,
                null,
                null,
                new OpcUaToMqttConfig(1, 1000),
                null,
                null);

        final ProtocolAdapterInformation adapterInformation = mock(ProtocolAdapterInformation.class);
        when(adapterInformation.getProtocolId()).thenReturn("opcua");

        @SuppressWarnings("unchecked")
        final ProtocolAdapterInput<OpcUaSpecificAdapterConfig> input = mock(ProtocolAdapterInput.class);
        when(input.getAdapterId()).thenReturn("test-adapter-id");
        when(input.getProtocolAdapterState()).thenReturn(protocolAdapterState);
        when(input.getConfig()).thenReturn(config);
        final List<Tag> genericTags = new ArrayList<>(List.of(tags));
        when(input.getTags()).thenReturn(genericTags);
        when(input.adapterFactories()).thenReturn(mock(AdapterFactories.class));
        when(input.getProtocolAdapterMetricsHelper()).thenReturn(mock(ProtocolAdapterMetricsService.class));

        final ModuleServices moduleServices = mock(ModuleServices.class);
        when(moduleServices.eventService()).thenReturn(eventService);
        when(moduleServices.protocolAdapterTagStreamingService())
                .thenReturn(mock(com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService.class));
        when(input.moduleServices()).thenReturn(moduleServices);

        adapter = new OpcUaProtocolAdapter(adapterInformation, input);

        final ProtocolAdapterStartInput startInput = mock(ProtocolAdapterStartInput.class);
        when(startInput.moduleServices()).thenReturn(moduleServices);

        adapter.start(
                ProtocolAdapterConnectionDirection.Northbound, startInput, mock(ProtocolAdapterStartOutput.class));
    }
}
