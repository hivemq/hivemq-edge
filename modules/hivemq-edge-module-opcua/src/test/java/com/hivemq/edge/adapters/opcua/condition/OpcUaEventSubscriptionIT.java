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
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaConditionType;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagKind;
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
 * Subscribes an event subscription tag — a query against a notifier — and checks what passes its filter.
 * <p>
 * This has to run against a real server. The WhereClause is not validated locally: it is sent to the server,
 * which resolves each operand and rejects a malformed filter by refusing the monitored item. That matters more
 * here than for a condition tag, because the query filter is the first place the adapter builds a
 * <em>composite</em> filter — {@code And} elements referring to other elements by index in a flat array — and
 * a unit test over the builder could only assert the shape we intended, never that the server accepts it.
 */
public class OpcUaEventSubscriptionIT {

    private static final long CONDITION_NODE_ID = 9500L;

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
    void whenNoNarrowingIsGiven_thenEveryConditionOnTheNotifierPublishes() throws Exception {
        // The firehose: all three predicates omitted. This is the case a condition tag can never produce, and
        // it is what makes the tag type worth having — one subscription covering an area.
        final String first =
                opcUaServerExtension.getTestNamespace().addConditionNode("QueryAlarmOne", CONDITION_NODE_ID);
        final String second =
                opcUaServerExtension.getTestNamespace().addConditionNode("QueryAlarmTwo", CONDITION_NODE_ID + 1);

        startAdapterWith(queryTag("area-events", null, null));

        awaitConnected();

        await().untilAsserted(() -> {
            opcUaServerExtension.getTestNamespace().fireAlarm(NodeId.parse(first), "one fired", 700, true);
            opcUaServerExtension.getTestNamespace().fireAlarm(NodeId.parse(second), "two fired", 800, true);

            assertThat(publishedSourceNames())
                    .as("a query with no narrowing must deliver every condition the notifier carries")
                    .contains("source-of-" + first, "source-of-" + second);
        });
    }

    @Test
    @Timeout(120)
    void whenNarrowedToOneCondition_thenOnlyThatConditionPublishes() throws Exception {
        final String wanted =
                opcUaServerExtension.getTestNamespace().addConditionNode("QueryWanted", CONDITION_NODE_ID + 10);
        final String other =
                opcUaServerExtension.getTestNamespace().addConditionNode("QueryOther", CONDITION_NODE_ID + 11);

        startAdapterWith(queryTag("one-condition", null, wanted));

        awaitConnected();

        // Warm up on the wanted condition, so the assertion below distinguishes "filtered out" from
        // "subscription not established yet".
        await().untilAsserted(() -> {
            opcUaServerExtension.getTestNamespace().fireAlarm(NodeId.parse(wanted), "mine", 700, true);
            assertThat(publishedSourceNames()).contains("source-of-" + wanted);
        });

        // Its own publishing cycle: fired back to back with one of ours, a shallow queue rather than the
        // filter could be what dropped it, and the test would pass for the wrong reason.
        Thread.sleep(1500);
        opcUaServerExtension.getTestNamespace().fireAlarm(NodeId.parse(other), "not mine", 900, true);
        Thread.sleep(1500);

        assertThat(publishedSourceNames())
                .as("a query narrowed to one condition must not deliver another condition's events")
                .doesNotContain("source-of-" + other);
    }

    @Test
    @Timeout(120)
    void whenNarrowedToOneSource_thenOnlyThatSourcesEventsPublish() throws Exception {
        // SourceNode is the ConditionSource -- what the alarm is about -- and the harness derives a distinct
        // source per condition, so narrowing by source picks out one condition's events by a different route
        // than narrowing by condition id.
        final String wanted =
                opcUaServerExtension.getTestNamespace().addConditionNode("SourceWanted", CONDITION_NODE_ID + 20);
        final String other =
                opcUaServerExtension.getTestNamespace().addConditionNode("SourceOther", CONDITION_NODE_ID + 21);

        startAdapterWith(
                queryTag("one-source", opcUaServerExtension.getTestNamespace().sourceNodeIdOf(wanted), null));

        awaitConnected();

        await().untilAsserted(() -> {
            opcUaServerExtension.getTestNamespace().fireAlarm(NodeId.parse(wanted), "mine", 700, true);
            assertThat(publishedSourceNames()).contains("source-of-" + wanted);
        });

        Thread.sleep(1500);
        opcUaServerExtension.getTestNamespace().fireAlarm(NodeId.parse(other), "not mine", 900, true);
        Thread.sleep(1500);

        assertThat(publishedSourceNames())
                .as("a query narrowed to one source must not deliver another source's events")
                .doesNotContain("source-of-" + other);
    }

    @Test
    @Timeout(120)
    void whenBothSourceAndConditionAreGiven_thenTheServerAcceptsTheCompositeFilter() throws Exception {
        // The case that exercises the And element: two predicates in one flat ContentFilter, the And
        // referring to both by index. If that encoding were wrong the server would reject the monitored item
        // and nothing would ever arrive, so a single published event is the proof.
        final String wanted =
                opcUaServerExtension.getTestNamespace().addConditionNode("BothNarrowed", CONDITION_NODE_ID + 30);

        startAdapterWith(queryTag(
                "both-narrowed", opcUaServerExtension.getTestNamespace().sourceNodeIdOf(wanted), wanted));

        awaitConnected();

        await().untilAsserted(() -> {
            opcUaServerExtension.getTestNamespace().fireAlarm(NodeId.parse(wanted), "composite", 700, true);
            assertThat(publishedSourceNames())
                    .as("the server must accept an And of two predicates, and deliver what matches both")
                    .contains("source-of-" + wanted);
        });
    }

    @Test
    @Timeout(120)
    void whenThePublishedTypeIsNarrowerThanTheFilter_thenOnlyItsFieldsAreSelected() throws Exception {
        // conditionType decides the published shape and filterType decides what passes — independently. Here
        // the filter is broad (any AlarmConditionType event) while the published shape is narrow (only the
        // ConditionType fields), which is the combination that proves the two are not the same knob.
        final String alarm =
                opcUaServerExtension.getTestNamespace().addConditionNode("ProjectedAlarm", CONDITION_NODE_ID + 40);

        final OpcuaTag tag = new OpcuaTag(
                "projected",
                "",
                new OpcuaTagDefinition(
                        opcUaServerExtension.getTestNamespace().areaNotifier().toParseableString(),
                        OpcuaTagKind.EVENT_SUBSCRIPTION,
                        // conditionType: what is published.
                        OpcuaConditionType.CONDITION,
                        null,
                        null,
                        alarm,
                        // filterType: what passes.
                        OpcuaConditionType.ALARM_CONDITION));

        startAdapterWith(tag);

        awaitConnected();

        await().untilAsserted(() -> {
            opcUaServerExtension.getTestNamespace().fireAlarm(NodeId.parse(alarm), "projected", 700, true);
            assertThat(tagStreamingService.published()).isNotEmpty();
        });

        final var value = tagStreamingService.published().get(0);
        assertThat(value.has("ConditionName"))
                .as("a field of the published type must be present")
                .isTrue();
        // ActiveState belongs to AlarmConditionType. The event that passed the filter carries it, but the
        // published shape does not ask for it -- so its absence is what shows the two types are separate.
        assertThat(value.has("ActiveState"))
                .as("a field the filter's type has, but the published type does not, must not be selected")
                .isFalse();
    }

    private @NotNull OpcuaTag queryTag(
            final @NotNull String name, final @Nullable String sourceNode, final @Nullable String conditionNode) {
        return new OpcuaTag(
                name,
                "",
                new OpcuaTagDefinition(
                        opcUaServerExtension.getTestNamespace().areaNotifier().toParseableString(),
                        OpcuaTagKind.EVENT_SUBSCRIPTION,
                        OpcuaConditionType.ALARM_CONDITION,
                        null,
                        sourceNode,
                        conditionNode,
                        null));
    }

    private void awaitConnected() {
        await().untilAsserted(() -> assertThat(protocolAdapterState.getConnectionStatus())
                .isEqualTo(ProtocolAdapterState.ConnectionStatus.CONNECTED));
    }

    private @NotNull List<String> publishedSourceNames() {
        return tagStreamingService.published().stream()
                .map(published -> published.get("SourceName").asText())
                .toList();
    }

    private void startAdapterWith(final @NotNull OpcuaTag... tags) {
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
        final List<Tag> genericTags = new ArrayList<>(List.of(tags));
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
