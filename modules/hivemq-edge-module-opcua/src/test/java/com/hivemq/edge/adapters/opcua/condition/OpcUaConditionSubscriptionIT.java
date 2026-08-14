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
import com.hivemq.adapter.sdk.api.data.DataPoint;
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
import org.eclipse.milo.opcua.stack.core.NodeIds;
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
                new OpcuaTagDefinition(conditionNodeId, OpcuaTagKind.CONDITION)));

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
        assertThat(value.get("EventId").isNull())
                .as("EventId must be present: without it an alarm cannot be acknowledged")
                .isFalse();
        assertThat(value.get("Message").toString()).contains("Temperature exceeded 90C");
        assertThat(value.get("Severity").asInt()).isEqualTo(700);
        assertThat(value.get("ActiveState").toString()).contains("Active");
        // The Boolean half of the state, beside the display text. Without it a consumer has to string-match
        // "Active" -- text the server writes in the session's locale and spells to its own taste.
        assertThat(value.get("ActiveState").get("id").asBoolean())
                .as("a two-state field must publish its machine-readable Id")
                .isTrue();
        assertThat(value.get("AckedState").get("id").asBoolean())
                .as("the alarm has not been acknowledged, so its Id must be false -- and false is exactly "
                        + "the value a missing-Id bug would be indistinguishable from if it were absent")
                .isFalse();
        // A NodeId is emitted as a structure, not as a parseable string, so the identifier is compared
        // field-wise rather than against "ns=1;i=9100".
        // SourceNode is the ConditionSource — the process variable the alarm is about — so it is deliberately
        // NOT the condition node. Which condition this came from is ConditionId, asserted in its own test.
        assertThat(value.get("SourceNode").get("id").asText()).contains("source-of");
        assertThat(value.get("SourceName").asText()).isEqualTo("source-of-" + conditionNodeId);
    }

    @Test
    @Timeout(120)
    void whenTheConditionIsSubscribed_thenEverySelectedFieldIsPresent() throws Exception {
        final String conditionNodeId =
                opcUaServerExtension.getTestNamespace().addConditionNode("FieldShapeAlarm", CONDITION_NODE_ID + 1);

        startAdapterWith(
                new OpcuaTag("shape-alarm", "", new OpcuaTagDefinition(conditionNodeId, OpcuaTagKind.CONDITION)));

        await().untilAsserted(() -> assertThat(protocolAdapterState.getConnectionStatus())
                .isEqualTo(ProtocolAdapterState.ConnectionStatus.CONNECTED));

        await().untilAsserted(() -> {
            opcUaServerExtension.getTestNamespace().fireAlarm(NodeId.parse(conditionNodeId), "shape check", 500, false);
            assertThat(tagStreamingService.published()).isNotEmpty();
        });

        final JsonNode value = tagStreamingService.published().get(0);

        // The published shape is fixed: every selected field appears, so a consumer can rely on the keys
        // being there even when the server has nothing to say for one of them.
        assertThat(OpcuaConditionType.ALARM_CONDITION.allFields()).allSatisfy(field -> assertThat(value.has(field))
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
                new OpcuaTag("subscribed-alarm", "", new OpcuaTagDefinition(subscribed, OpcuaTagKind.CONDITION)));

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

        // Both conditions hang off the same notifier, so the filter is the only thing separating them: every
        // published event must trace back to the subscribed condition, not merely to some condition.
        assertThat(tagStreamingService.published())
                .as("only transitions of the subscribed condition may be published")
                .allSatisfy(published ->
                        assertThat(published.get("SourceName").asText()).isEqualTo("source-of-" + subscribed));
    }

    @Test
    @Timeout(120)
    void whenTheDeclaredTypeIsRicher_thenItsExtraFieldsArePublished() throws Exception {
        final String conditionNodeId = opcUaServerExtension
                .getTestNamespace()
                .addConditionNode("LevelAlarm", CONDITION_NODE_ID + 10, NodeIds.ExclusiveLevelAlarmType);

        startAdapterWith(new OpcuaTag(
                "level-alarm",
                "",
                new OpcuaTagDefinition(
                        conditionNodeId, OpcuaTagKind.CONDITION, OpcuaConditionType.EXCLUSIVE_LEVEL_ALARM)));

        await().untilAsserted(() -> assertThat(protocolAdapterState.getConnectionStatus())
                .isEqualTo(ProtocolAdapterState.ConnectionStatus.CONNECTED));

        await().untilAsserted(() -> {
            opcUaServerExtension.getTestNamespace().fireAlarm(NodeId.parse(conditionNodeId), "high", 800, true);
            assertThat(tagStreamingService.published()).isNotEmpty();
        });

        // The point of declaring the type: a level alarm's limits are selected and published. A fixed field
        // list would drop them silently, which for a level alarm loses the interesting part.
        final JsonNode value = tagStreamingService.published().get(0);
        assertThat(value.has("HighLimit"))
                .as("a level alarm must publish its limits")
                .isTrue();
        assertThat(value.has("LowLowLimit")).isTrue();
        assertThat(value.has("EventId")).isTrue();
    }

    @Test
    @Timeout(120)
    void whenAnAlarmFires_thenConditionIdNamesTheConditionItCameFrom() throws Exception {
        // Patrick D'Addona, 2026-08-07: the documentation said ConditionId and SourceNode "both appear in
        // every event", and only SourceNode did. ConditionId existed solely as a filter operand.
        //
        // It is not a property beneath the event -- it is the event's own node id, selected with an empty
        // browse path against the NodeId attribute. That is why it fell out of a field table keyed by name,
        // and why SelectedField had to learn to carry an attribute before it could be added.
        //
        // The value is what makes this worth asserting rather than merely checking presence: it must be the
        // *condition* node, not the ConditionSource that SourceNode names. Those are different nodes, and a
        // consumer acknowledging an alarm needs this one.
        final String conditionNodeId =
                opcUaServerExtension.getTestNamespace().addConditionNode("IdentifiedAlarm", CONDITION_NODE_ID + 80);

        startAdapterWith(
                new OpcuaTag("identified-alarm", "", new OpcuaTagDefinition(conditionNodeId, OpcuaTagKind.CONDITION)));

        await().untilAsserted(() -> assertThat(protocolAdapterState.getConnectionStatus())
                .isEqualTo(ProtocolAdapterState.ConnectionStatus.CONNECTED));

        await().untilAsserted(() -> {
            opcUaServerExtension
                    .getTestNamespace()
                    .fireAlarm(NodeId.parse(conditionNodeId), "which condition am I", 700, true);
            assertThat(tagStreamingService.published()).isNotEmpty();
        });

        final JsonNode value = tagStreamingService.published().get(0);
        final NodeId expected = NodeId.parse(conditionNodeId);

        assertThat(value.get("ConditionId").isNull())
                .as("ConditionId must be published, not merely used to build the where clause")
                .isFalse();
        assertThat(value.get("ConditionId").get("id").asText())
                .as("ConditionId must name the condition node itself")
                .isEqualTo(expected.getIdentifier().toString());
        assertThat(value.get("ConditionId").get("namespaceIndex").asInt())
                .isEqualTo(expected.getNamespaceIndex().intValue());

        // The distinction that makes the field worth having: SourceNode is the ConditionSource -- the sensor
        // the alarm is about -- and is a different node. A payload carrying only SourceNode cannot say which
        // alarm fired when several alarms watch one sensor.
        assertThat(value.get("SourceNode").get("id").asText())
                .as("SourceNode is the ConditionSource, so ConditionId is not redundant with it")
                .isNotEqualTo(value.get("ConditionId").get("id").asText());
    }

    @Test
    @Timeout(120)
    void whenTheDeviceReportsAVendorSubtype_thenItIsResolvedToItsStandardAncestor() throws Exception {
        // OPC 10000-9 §5.5: "It is expected that vendors or other standardisation groups will define
        // additional ConditionTypes deriving from the common base types defined in this part." So a server
        // reporting a type Edge has never heard of is doing the normal thing, not misbehaving.
        //
        // Edge used to reject the tag outright, and tell the operator to "declare the nearest standard type
        // it derives from" -- an instruction that cannot be followed, since the rejection depends on what the
        // *device* reports, not on the declaration. Whatever they set, the same browse returns the same
        // vendor name and the same rejection fires.
        //
        // The condition here carries a single HasTypeDefinition naming a vendor type, with nothing standard
        // beside it, so the only way to place it is to ask the server what it derives from.
        final String vendorAlarm = opcUaServerExtension
                .getTestNamespace()
                .addConditionNodeOfVendorType(
                        "VendorAlarm", CONDITION_NODE_ID + 70, "AcmeBoilerAlarmType", NodeIds.AlarmConditionType);

        startAdapterWith(new OpcuaTag(
                "vendor-alarm",
                "",
                new OpcuaTagDefinition(vendorAlarm, OpcuaTagKind.CONDITION, OpcuaConditionType.ALARM_CONDITION)));

        await().untilAsserted(() -> assertThat(protocolAdapterState.getConnectionStatus())
                .isEqualTo(ProtocolAdapterState.ConnectionStatus.CONNECTED));

        // Subscribed, so its transitions arrive: the walk found AlarmConditionType above the vendor type and
        // verified the declaration against that.
        await().untilAsserted(() -> {
            opcUaServerExtension.getTestNamespace().fireAlarm(NodeId.parse(vendorAlarm), "vendor alarm", 700, true);
            assertThat(tagStreamingService.published())
                    .as("a vendor subtype of a standard condition type must subscribe")
                    .isNotEmpty();
        });
    }

    @Test
    @Timeout(120)
    void whenTheDeviceExposesNoTypeDefinition_thenTheTagIsRejectedWithBothReadings() throws Exception {
        // A server may legitimately keep its condition instances out of the address space (OPC 10000-9 §4.3),
        // so an empty answer is not proof the tag is wrong. It is not proof the tag is right either, and a
        // verifier that treats "I could not check" as "it is fine" has stopped verifying -- so the tag is
        // rejected. What the message must not do is assert the node is not a condition: it names both
        // readings, so an operator meeting this on a real device can tell us which one it was.
        final String unexposed = opcUaServerExtension
                .getTestNamespace()
                .addConditionNodeWithoutTypeDefinition("UnexposedAlarm", CONDITION_NODE_ID + 60);
        final String healthy =
                opcUaServerExtension.getTestNamespace().addConditionNode("HealthyNeighbour", CONDITION_NODE_ID + 61);

        startAdapterWith(
                new OpcuaTag("unexposed-alarm", "", new OpcuaTagDefinition(unexposed, OpcuaTagKind.CONDITION)),
                new OpcuaTag("healthy-neighbour", "", new OpcuaTagDefinition(healthy, OpcuaTagKind.CONDITION)));

        // One unverifiable tag must not stop the adapter, nor the tags beside it.
        await().untilAsserted(() -> assertThat(protocolAdapterState.getConnectionStatus())
                .isEqualTo(ProtocolAdapterState.ConnectionStatus.CONNECTED));

        await().untilAsserted(() -> {
            opcUaServerExtension.getTestNamespace().fireAlarm(NodeId.parse(healthy), "still working", 600, true);
            assertThat(tagStreamingService.published()).isNotEmpty();
        });

        opcUaServerExtension.getTestNamespace().fireAlarm(NodeId.parse(unexposed), "should not arrive", 900, true);
        Thread.sleep(2000);

        assertThat(tagStreamingService.published())
                .as("the unverifiable tag must not be subscribed")
                .allSatisfy(published ->
                        assertThat(published.get("SourceName").asText()).isEqualTo("source-of-" + healthy));

        final List<String> messages = eventService.readEvents(null, null).stream()
                .map(event -> String.valueOf(event.getMessage()))
                .filter(message -> message.contains("unexposed-alarm"))
                .toList();
        assertThat(messages)
                .as("a dropped tag must be reported as an adapter event")
                .isNotEmpty();
        assertThat(messages).anySatisfy(message -> assertThat(message)
                .as("the reason must say the declaration could not be verified, not assert the node is wrong")
                .contains("could not be verified"));
    }

    @Test
    @Timeout(120)
    void whenTheDeclaredTypeDoesNotMatchTheDevice_thenOnlyThatTagIsDropped() throws Exception {
        // Declared as a level alarm, but the device offers only a plain alarm -- the tag promises limits the
        // device has not got.
        final String mismatched = opcUaServerExtension
                .getTestNamespace()
                .addConditionNode("PlainAlarm", CONDITION_NODE_ID + 11, NodeIds.AlarmConditionType);
        final String sound =
                opcUaServerExtension.getTestNamespace().addConditionNode("SoundAlarm", CONDITION_NODE_ID + 12);

        startAdapterWith(
                new OpcuaTag(
                        "mismatched-alarm",
                        "",
                        new OpcuaTagDefinition(
                                mismatched, OpcuaTagKind.CONDITION, OpcuaConditionType.EXCLUSIVE_LEVEL_ALARM)),
                new OpcuaTag("sound-alarm", "", new OpcuaTagDefinition(sound, OpcuaTagKind.CONDITION)));

        // The adapter still connects: one mistyped tag must not stop it, nor the tags beside it.
        await().untilAsserted(() -> assertThat(protocolAdapterState.getConnectionStatus())
                .isEqualTo(ProtocolAdapterState.ConnectionStatus.CONNECTED));

        await().untilAsserted(() -> {
            opcUaServerExtension.getTestNamespace().fireAlarm(NodeId.parse(sound), "still working", 600, true);
            assertThat(tagStreamingService.published()).isNotEmpty();
        });

        // Fire the mismatched one too, and give it every chance to arrive.
        opcUaServerExtension.getTestNamespace().fireAlarm(NodeId.parse(mismatched), "should not arrive", 900, true);
        Thread.sleep(2000);

        assertThat(tagStreamingService.published())
                .as("the mismatched tag must not be subscribed")
                .allSatisfy(published ->
                        assertThat(published.get("SourceName").asText()).isEqualTo("source-of-" + sound));

        // And the operator is told which tag was dropped and why.
        assertThat(eventService.readEvents(null, null).stream()
                        .map(event -> String.valueOf(event.getMessage()))
                        .filter(message -> message.contains("mismatched-alarm"))
                        .toList())
                .as("a dropped tag must be reported as an adapter event")
                .isNotEmpty();
    }

    @Test
    @Timeout(120)
    void whenTheNotifierIsDeclared_thenItIsUsedInsteadOfWalking() throws Exception {
        final String conditionNodeId =
                opcUaServerExtension.getTestNamespace().addConditionNode("DeclaredAlarm", CONDITION_NODE_ID + 20);
        final String notifier =
                opcUaServerExtension.getTestNamespace().areaNotifier().toParseableString();

        // Naming the notifier is the escape hatch for servers whose references cannot be walked. Here the
        // walk would also succeed, so this checks the declaration is honoured rather than merely tolerated.
        startAdapterWith(new OpcuaTag(
                "declared-notifier-alarm",
                "",
                new OpcuaTagDefinition(
                        conditionNodeId, OpcuaTagKind.CONDITION, OpcuaConditionType.ALARM_CONDITION, notifier)));

        await().untilAsserted(() -> assertThat(protocolAdapterState.getConnectionStatus())
                .isEqualTo(ProtocolAdapterState.ConnectionStatus.CONNECTED));

        await().untilAsserted(() -> {
            opcUaServerExtension.getTestNamespace().fireAlarm(NodeId.parse(conditionNodeId), "declared", 700, true);
            assertThat(tagStreamingService.published()).isNotEmpty();
        });

        assertThat(tagStreamingService.published().get(0).get("SourceName").asText())
                .isEqualTo("source-of-" + conditionNodeId);
    }

    @Test
    @Timeout(120)
    void whenTheConditionHangsOffTheNotifierDirectly_thenTheWalkStillFindsIt() throws Exception {
        // The layout OPC 10000-9 §6.2/§6.3 does not describe: HasEventSource from the area straight to the
        // condition, no ConditionSource in between. Servers do it, so the resolver falls back to browsing
        // HasEventSource from the condition when it has no ConditionSource -- without which the conformant
        // fix would strand exactly the devices that used to work.
        final String conditionNodeId = opcUaServerExtension
                .getTestNamespace()
                .addDirectlyAttachedConditionNode("LegacyAlarm", CONDITION_NODE_ID + 50);

        startAdapterWith(new OpcuaTag(
                "legacy-layout-alarm", "", new OpcuaTagDefinition(conditionNodeId, OpcuaTagKind.CONDITION)));

        await().untilAsserted(() -> assertThat(protocolAdapterState.getConnectionStatus())
                .isEqualTo(ProtocolAdapterState.ConnectionStatus.CONNECTED));

        await().untilAsserted(() -> {
            opcUaServerExtension.getTestNamespace().fireAlarm(NodeId.parse(conditionNodeId), "legacy", 700, true);
            assertThat(tagStreamingService.published()).isNotEmpty();
        });

        assertThat(tagStreamingService.published().get(0).get("Message").toString())
                .contains("legacy");
    }

    @Test
    @Timeout(120)
    void whenNoNotifierCanBeFound_thenTheTagIsInactiveAndTheAdapterStillRuns() throws Exception {
        // A condition with no path to any notifier: nothing can be subscribed for it. The tag must go quiet
        // on its own rather than taking the adapter -- or its neighbours -- down with it.
        final String orphan =
                opcUaServerExtension.getTestNamespace().addOrphanConditionNode("OrphanAlarm", CONDITION_NODE_ID + 21);
        final String healthy =
                opcUaServerExtension.getTestNamespace().addConditionNode("HealthyAlarm", CONDITION_NODE_ID + 22);

        startAdapterWith(
                new OpcuaTag("orphan-alarm", "", new OpcuaTagDefinition(orphan, OpcuaTagKind.CONDITION)),
                new OpcuaTag("healthy-alarm", "", new OpcuaTagDefinition(healthy, OpcuaTagKind.CONDITION)));

        await().untilAsserted(() -> assertThat(protocolAdapterState.getConnectionStatus())
                .isEqualTo(ProtocolAdapterState.ConnectionStatus.CONNECTED));

        await().untilAsserted(() -> {
            opcUaServerExtension.getTestNamespace().fireAlarm(NodeId.parse(healthy), "still working", 600, true);
            assertThat(tagStreamingService.published()).isNotEmpty();
        });

        assertThat(eventService.readEvents(null, null).stream()
                        .map(event -> String.valueOf(event.getMessage()))
                        .filter(message -> message.contains("orphan-alarm"))
                        .toList())
                .as("a tag with no notifier must be reported, not silently idle")
                .isNotEmpty();
    }

    @Test
    @Timeout(120)
    void whenTwoConditionsShareANotifier_thenBothPublish() throws Exception {
        // The ordinary case in a plant: several alarms in one area, so their tags resolve to the same
        // notifier. Every other test here has one condition, or two where one is rejected before an item is
        // created, so nothing yet covers two tags that are both accepted and both subscribed.
        final String first =
                opcUaServerExtension.getTestNamespace().addConditionNode("FirstAreaAlarm", CONDITION_NODE_ID + 30);
        final String second =
                opcUaServerExtension.getTestNamespace().addConditionNode("SecondAreaAlarm", CONDITION_NODE_ID + 31);

        startAdapterWith(
                new OpcuaTag("first-alarm", "", new OpcuaTagDefinition(first, OpcuaTagKind.CONDITION)),
                new OpcuaTag("second-alarm", "", new OpcuaTagDefinition(second, OpcuaTagKind.CONDITION)));

        // The adapter must come up at all: the two tags share a notifier node id, and anything keyed by node
        // id sees that as a collision.
        await().untilAsserted(() -> assertThat(protocolAdapterState.getConnectionStatus())
                .isEqualTo(ProtocolAdapterState.ConnectionStatus.CONNECTED));

        // Both conditions must be subscribed, not just whichever one was processed first.
        await().untilAsserted(() -> {
            opcUaServerExtension.getTestNamespace().fireAlarm(NodeId.parse(first), "first fired", 700, true);
            opcUaServerExtension.getTestNamespace().fireAlarm(NodeId.parse(second), "second fired", 800, true);

            final var sources = tagStreamingService.published().stream()
                    .map(published -> published.get("SourceName").asText())
                    .toList();
            assertThat(sources)
                    .as("both conditions sharing a notifier must publish, not only one of them")
                    .contains("source-of-" + first, "source-of-" + second);
        });
    }

    @Test
    @Timeout(120)
    void whenAlarmsFireInOneCycle_thenNoTransitionIsDropped() throws Exception {
        // A value item's queue may be one deep: a lost sample is replaced by the next one. An event is a
        // transition report -- if the queue drops it, the server never re-sends it and the northbound picture
        // silently disagrees with the device until that alarm next changes. Conditions are exactly where
        // bursts are normal: one notifier carries every condition beneath it, and ConditionRefresh asks the
        // server to re-report every retained condition at once.
        //
        // This is the test the earlier isolation test worked around rather than wrote: it fires its foreign
        // alarm in a publishing cycle of its own precisely because a depth-1 queue would swallow one of two.
        final String alarm =
                opcUaServerExtension.getTestNamespace().addConditionNode("BurstAlarm", CONDITION_NODE_ID + 40);

        startAdapterWith(new OpcuaTag("burst-alarm", "", new OpcuaTagDefinition(alarm, OpcuaTagKind.CONDITION)));

        await().untilAsserted(() -> assertThat(protocolAdapterState.getConnectionStatus())
                .isEqualTo(ProtocolAdapterState.ConnectionStatus.CONNECTED));

        // Warm up: prove the subscription is live before the burst, so a failure below means "dropped", not
        // "never subscribed".
        await().untilAsserted(() -> {
            opcUaServerExtension.getTestNamespace().fireAlarm(NodeId.parse(alarm), "warmup", 500, true);
            assertThat(tagStreamingService.published()).isNotEmpty();
        });

        // Fired back to back, so they land in one publishing cycle and must all fit in the queue. Counted by
        // their distinct messages rather than by clearing what came before, which the capture does not allow.
        final int burst = 10;
        for (int i = 0; i < burst; i++) {
            opcUaServerExtension.getTestNamespace().fireAlarm(NodeId.parse(alarm), "burst-" + i, 600 + i, true);
        }

        await().untilAsserted(() -> {
            // Message is a LocalizedText object, not a bare string, so it is read whole rather than by asText.
            final var burstMessages = tagStreamingService.published().stream()
                    .map(published -> published.get("Message").toString())
                    .filter(message -> message.contains("burst-"))
                    .distinct()
                    .toList();
            assertThat(burstMessages)
                    .as("every transition in a burst must be published: an event dropped from the server "
                            + "queue is never re-sent")
                    .hasSize(burst);
            assertThat(tagStreamingService.publishedBatches())
                    .as("the back-to-back transitions must arrive in one server publication batch; the "
                            + "flattened payload view alone cannot prove that boundary")
                    .anySatisfy(batch -> assertThat(batch.stream()
                                    .map(DataPoint::getTagValue)
                                    .filter(JsonNode.class::isInstance)
                                    .map(JsonNode.class::cast)
                                    .map(published -> published.get("Message").toString())
                                    .filter(message -> message.contains("burst-"))
                                    .distinct())
                            .hasSize(burst));
        });
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
