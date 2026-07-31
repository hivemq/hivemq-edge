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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
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
import com.hivemq.edge.adapters.opcua.FakeEventService;
import com.hivemq.edge.adapters.opcua.OpcUaProtocolAdapter;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagType;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import util.EmbeddedOpcUaServerExtension;

/**
 * Subscribes a condition tag against an embedded OPC UA server, fires a real alarm, and checks what the
 * adapter publishes.
 * <p>
 * This is the test that carries the risk of the northbound condition path. The select clause is not validated
 * locally: it is sent to the server, which resolves each browse path and rejects the monitored item if the
 * filter is malformed. Only a real server exercises that, so a unit test over the filter builder cannot
 * establish that events will actually arrive.
 */
public class OpcUaConditionSubscriptionIT {

    private static final long CONDITION_NODE_ID = 9100L;

    @RegisterExtension
    public final @NotNull EmbeddedOpcUaServerExtension opcUaServerExtension = new EmbeddedOpcUaServerExtension();

    private @Nullable OpcUaProtocolAdapter adapter;
    private @NotNull ProtocolAdapterState protocolAdapterState;
    private @NotNull FakeEventService eventService;
    private @NotNull CapturingTagStreamingService tagStreamingService;

    @BeforeEach
    void setUp() {
        protocolAdapterState = new ProtocolAdapterStateImpl(mock(), "test-adapter-id", "opcua");
        eventService = new FakeEventService();
        tagStreamingService = new CapturingTagStreamingService();
    }

    @AfterEach
    void tearDown() {
        if (adapter != null) {
            adapter.destroy();
        }
    }

    @Test
    @Timeout(120)
    void whenAnAlarmFires_thenTheConditionTagPublishesTheTransition() throws Exception {
        final String conditionNodeId =
                opcUaServerExtension.getTestNamespace().addConditionNode("HighTempAlarm", CONDITION_NODE_ID);

        startAdapterWith(new OpcuaTag(
                "boiler-alarm",
                "The boiler's high temperature alarm",
                new OpcuaTagDefinition(conditionNodeId, OpcuaTagType.CONDITION)));

        await().untilAsserted(() -> assertThat(protocolAdapterState.getConnectionStatus())
                .isEqualTo(ProtocolAdapterState.ConnectionStatus.CONNECTED));

        // The subscription is established asynchronously; an alarm fired before the monitored item exists
        // would be missed, so keep firing until one is observed.
        await().untilAsserted(() -> {
            opcUaServerExtension
                    .getTestNamespace()
                    .fireAlarm(NodeId.parse(conditionNodeId), "Temperature exceeded 90C", 700, true);
            assertThat(tagStreamingService.published()).isNotEmpty();
        });

        final JsonNode value = tagStreamingService.published().get(0);

        // EventId identifies the transition, and is what a downstream system echoes back to acknowledge.
        assertThat(value.get(ConditionFields.EVENT_ID).isNull())
                .as("EventId must be present: without it an alarm cannot be acknowledged")
                .isFalse();
        assertThat(value.get("Message").toString()).contains("Temperature exceeded 90C");
        assertThat(value.get("Severity").asInt()).isEqualTo(700);
        assertThat(value.get("ActiveState").toString()).contains("Active");
        // A NodeId is emitted as a structure, not as a parseable string, so the identifier is compared
        // field-wise rather than against "ns=1;i=9100".
        assertThat(value.get("SourceNode").get("id").asLong()).isEqualTo(CONDITION_NODE_ID);
        assertThat(value.get("SourceName").asText()).isEqualTo(conditionNodeId);
    }

    @Test
    @Timeout(120)
    void whenTheConditionIsSubscribed_thenEverySelectedFieldIsPresent() throws Exception {
        final String conditionNodeId =
                opcUaServerExtension.getTestNamespace().addConditionNode("FieldShapeAlarm", CONDITION_NODE_ID + 1);

        startAdapterWith(
                new OpcuaTag("shape-alarm", "", new OpcuaTagDefinition(conditionNodeId, OpcuaTagType.CONDITION)));

        await().untilAsserted(() -> assertThat(protocolAdapterState.getConnectionStatus())
                .isEqualTo(ProtocolAdapterState.ConnectionStatus.CONNECTED));

        await().untilAsserted(() -> {
            opcUaServerExtension.getTestNamespace().fireAlarm(NodeId.parse(conditionNodeId), "shape check", 500, false);
            assertThat(tagStreamingService.published()).isNotEmpty();
        });

        final JsonNode value = tagStreamingService.published().get(0);

        // The published shape is fixed: every selected field appears, so a consumer can rely on the keys
        // being there even when the server has nothing to say for one of them.
        assertThat(ConditionFields.SELECTED).allSatisfy(field -> assertThat(value.has(field))
                .as("selected field '%s' must be present in the published payload", field)
                .isTrue());
    }

    @Test
    @Timeout(120)
    void whenAnotherConditionFires_thenTheTagDoesNotPublishIt() throws Exception {
        final String subscribed =
                opcUaServerExtension.getTestNamespace().addConditionNode("SubscribedAlarm", CONDITION_NODE_ID + 2);
        final String other =
                opcUaServerExtension.getTestNamespace().addConditionNode("OtherAlarm", CONDITION_NODE_ID + 3);

        startAdapterWith(
                new OpcuaTag("subscribed-alarm", "", new OpcuaTagDefinition(subscribed, OpcuaTagType.CONDITION)));

        await().untilAsserted(() -> assertThat(protocolAdapterState.getConnectionStatus())
                .isEqualTo(ProtocolAdapterState.ConnectionStatus.CONNECTED));

        // Establish that the subscription is live: until one of our own transitions has arrived, the absence
        // of a foreign one proves nothing.
        await().untilAsserted(() -> {
            opcUaServerExtension.getTestNamespace().fireAlarm(NodeId.parse(subscribed), "warmup", 700, true);
            assertThat(tagStreamingService.published()).isNotEmpty();
        });

        // The foreign alarm must sit in a publishing cycle of its own. Fired back-to-back with one of ours it
        // would be dropped by a server queue of depth 1 regardless of the filter, and the test would pass for
        // the wrong reason.
        final int before = tagStreamingService.published().size();
        opcUaServerExtension.getTestNamespace().fireAlarm(NodeId.parse(other), "not mine", 900, true);
        Thread.sleep(1500);

        // Then one of ours, whose arrival bounds the wait: by the time it lands the foreign event has had at
        // least as long to be delivered.
        opcUaServerExtension.getTestNamespace().fireAlarm(NodeId.parse(subscribed), "mine", 700, true);
        await().untilAsserted(
                        () -> assertThat(tagStreamingService.published().size()).isGreaterThan(before));

        assertThat(tagStreamingService.published())
                .as("only transitions of the subscribed condition may be published")
                .allSatisfy(published -> assertThat(
                                published.get("SourceNode").get("id").asLong())
                        .isEqualTo(CONDITION_NODE_ID + 2));
    }

    private void startAdapterWith(final @NotNull OpcuaTag tag) {
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

        final ProtocolAdapterInput<OpcUaSpecificAdapterConfig> input = mock(ProtocolAdapterInput.class);
        when(input.getAdapterId()).thenReturn("test-adapter-id");
        when(input.getProtocolAdapterState()).thenReturn(protocolAdapterState);
        when(input.getConfig()).thenReturn(config);
        final List<Tag> genericTags = new ArrayList<>(List.of(tag));
        when(input.getTags()).thenReturn(genericTags);
        when(input.adapterFactories()).thenReturn(mock(AdapterFactories.class));
        // Subscription callbacks count metrics; without this every delivered event dies in an NPE.
        when(input.getProtocolAdapterMetricsHelper()).thenReturn(mock(ProtocolAdapterMetricsService.class));

        // The constructor already reaches for the event service, so this has to be wired before construction,
        // not just on the start input.
        final ModuleServices moduleServices = mock(ModuleServices.class);
        when(moduleServices.eventService()).thenReturn(eventService);
        when(moduleServices.protocolAdapterTagStreamingService()).thenReturn(tagStreamingService);
        when(input.moduleServices()).thenReturn(moduleServices);

        adapter = new OpcUaProtocolAdapter(adapterInformation, input);

        final ProtocolAdapterStartInput startInput = mock(ProtocolAdapterStartInput.class);
        when(startInput.moduleServices()).thenReturn(moduleServices);

        adapter.start(
                ProtocolAdapterConnectionDirection.Northbound, startInput, mock(ProtocolAdapterStartOutput.class));
    }
}
