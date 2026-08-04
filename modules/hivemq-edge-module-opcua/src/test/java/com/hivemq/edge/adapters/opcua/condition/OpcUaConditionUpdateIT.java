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
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.Base64;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import util.EmbeddedOpcUaServerExtension;
import util.TestNamespace;

/**
 * Writes to a condition tag against an embedded OPC UA server and checks what the server was asked to do.
 * <p>
 * The risk on this path is that acknowledging is a <em>method call</em>, not a write: the arguments, their
 * order and the method node id are all decided locally and only validated by the server. A unit test over the
 * command parser cannot establish that a server accepts the call, so the round trip is exercised here.
 */
public class OpcUaConditionUpdateIT {

    private static final long CONDITION_NODE_ID = 9200L;

    @RegisterExtension
    public final @NotNull EmbeddedOpcUaServerExtension opcUaServerExtension = new EmbeddedOpcUaServerExtension();

    private final @NotNull ObjectMapper mapper = new ObjectMapper();

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
    void whenAConditionIsAcknowledged_thenTheServerIsAskedToAcknowledgeThatTransition() throws Exception {
        final String conditionNodeId = opcUaServerExtension
                .getTestNamespace()
                .addAcknowledgeableConditionNode("AckableAlarm", CONDITION_NODE_ID);

        startAdapterWith(conditionNodeId);

        // The eventId is echoed back exactly as it was published north — base64 of the bytes the server
        // minted. Acknowledging is against a transition, so this token is what makes the request meaningful.
        final String eventId = Base64.getEncoder().encodeToString("transition-42".getBytes());

        final WritingOutput output = writeToCondition(conditionNodeId, """
                {"eventId": "%s", "method": 0, "comment": "Checked - reducing setpoint"}
                """.formatted(eventId));

        verify(output, timeout(10_000)).finish();

        final List<TestNamespace.MethodCall> calls =
                opcUaServerExtension.getTestNamespace().methodCallsExcludingRefresh();
        assertThat(calls).hasSize(1);
        assertThat(calls.get(0).methodName())
                .as("method 0 must dispatch to Acknowledge")
                .isEqualTo("Acknowledge");
        assertThat(new String(calls.get(0).eventId().bytesOrEmpty()))
                .as("the server must receive the transition token it issued, not the base64 text of it")
                .isEqualTo("transition-42");
        assertThat(calls.get(0).comment()).isEqualTo("Checked - reducing setpoint");
    }

    @Test
    @Timeout(120)
    void whenNoCommentIsGiven_thenTheExistingOneIsLeftAlone() throws Exception {
        // OPC 10000-9 §5.7.3: "If the comment field is NULL (both locale and text are empty) it will be
        // ignored and any existing comments will remain unchanged." Since `comment` is optional in the write
        // schema, an acknowledgement without one is the natural thing to send -- and it must not erase what a
        // previous operator recorded. Erasure would also be broadcast: §5.5.2 makes any comment change fire a
        // new event, so every other client would be told the audit trail had gone.
        final String conditionNodeId = opcUaServerExtension
                .getTestNamespace()
                .addAcknowledgeableConditionNode("NoCommentAlarm", CONDITION_NODE_ID + 30);

        startAdapterWith(conditionNodeId);

        final String eventId = Base64.getEncoder().encodeToString("transition-nc".getBytes());
        final WritingOutput output = writeToCondition(conditionNodeId, """
                {"eventId": "%s", "method": "ACKNOWLEDGE"}
                """.formatted(eventId));

        verify(output, timeout(10_000)).finish();

        final List<TestNamespace.MethodCall> calls =
                opcUaServerExtension.getTestNamespace().methodCallsExcludingRefresh();
        assertThat(calls).hasSize(1);
        assertThat(calls.get(0).rawComment())
                .as("a missing comment must reach the server as a NULL LocalizedText")
                .isNotNull();
        assertThat(calls.get(0).rawComment().isNull())
                .as("both locale and text must be null, which is what the spec calls NULL -- an empty text "
                        + "with a locale is the *erase* form, and would wipe the existing comment")
                .isTrue();
    }

    @Test
    @Timeout(120)
    void whenAnEmptyCommentIsGiven_thenTheExistingOneIsErased() throws Exception {
        // The other half of §5.7.3: "To reset the comment, an empty text with a locale shall be provided."
        // This is the only way to clear a stale comment, so it is deliberately reachable -- and deliberately
        // distinct from omitting the field.
        final String conditionNodeId = opcUaServerExtension
                .getTestNamespace()
                .addAcknowledgeableConditionNode("EraseCommentAlarm", CONDITION_NODE_ID + 31);

        startAdapterWith(conditionNodeId);

        final String eventId = Base64.getEncoder().encodeToString("transition-erase".getBytes());
        final WritingOutput output = writeToCondition(conditionNodeId, """
                {"eventId": "%s", "method": "ACKNOWLEDGE", "comment": ""}
                """.formatted(eventId));

        verify(output, timeout(10_000)).finish();

        final List<TestNamespace.MethodCall> calls =
                opcUaServerExtension.getTestNamespace().methodCallsExcludingRefresh();
        assertThat(calls).hasSize(1);
        assertThat(calls.get(0).rawComment()).isNotNull();
        assertThat(calls.get(0).rawComment().isNull())
                .as("an explicit empty comment is the erase form, not the leave-alone form")
                .isFalse();
        // The locale is what actually carries the distinction on the wire. Milo's binary encoder writes an
        // empty text as *absent* -- mask bit cleared, nothing serialized -- so it decodes back as null and an
        // erase is indistinguishable from leave-alone by the text alone. A present locale with no text is
        // exactly the specification's "empty text with a locale".
        assertThat(calls.get(0).rawComment().getLocale())
                .as("the spec's erase form is an empty text WITH a locale, and the locale is the only part "
                        + "that survives the wire encoding")
                .isNotNull();
    }

    @Test
    @Timeout(120)
    void whenAMethodHasACommentedVariant_thenItIsUsedToCarryTheComment() throws Exception {
        // Suppress() takes no arguments, so a comment sent with it had nowhere to go and was silently
        // dropped. Suppress2(Comment) is the same operation with somewhere to put it.
        final String conditionNodeId = opcUaServerExtension
                .getTestNamespace()
                .addAcknowledgeableConditionNode("CommentedVariantAlarm", CONDITION_NODE_ID + 40);

        startAdapterWith(conditionNodeId);

        final WritingOutput output = writeToCondition(conditionNodeId, """
                {"method": "SUPPRESS", "comment": "suppressed during maintenance"}
                """);

        verify(output, timeout(10_000)).finish();

        final List<TestNamespace.MethodCall> calls =
                opcUaServerExtension.getTestNamespace().methodCallsExcludingRefresh();
        assertThat(calls).hasSize(1);
        assertThat(calls.get(0).methodName())
                .as("the commented variant must be preferred when the server has it")
                .isEqualTo("Suppress2");
        assertThat(calls.get(0).comment()).isEqualTo("suppressed during maintenance");
    }

    @Test
    @Timeout(120)
    void whenNoCommentIsGiven_thenTheBaseVariantIsUsed() throws Exception {
        // Nothing to carry, so there is no reason to prefer the newer method. Calling the base form keeps
        // the request identical to what it was before the comment support existed.
        final String conditionNodeId = opcUaServerExtension
                .getTestNamespace()
                .addAcknowledgeableConditionNode("BaseVariantAlarm", CONDITION_NODE_ID + 41);

        startAdapterWith(conditionNodeId);

        final WritingOutput output = writeToCondition(conditionNodeId, """
                {"method": "SUPPRESS"}
                """);

        verify(output, timeout(10_000)).finish();

        final List<TestNamespace.MethodCall> calls =
                opcUaServerExtension.getTestNamespace().methodCallsExcludingRefresh();
        assertThat(calls).hasSize(1);
        assertThat(calls.get(0).methodName()).isEqualTo("Suppress");
    }

    @Test
    @Timeout(120)
    void whenTheServerHasNoCommentedVariant_thenTheActionStillHappens() throws Exception {
        // The decisive case. A user writing {"method": "SUPPRESS", "comment": "..."} wants the alarm
        // suppressed first and foremost; the comment is best effort. So a server with only the base method
        // still gets the suppression -- the comment is dropped with a warning rather than failing the write
        // and leaving an alarm unsuppressed over a note.
        final String conditionNodeId = opcUaServerExtension
                .getTestNamespace()
                .addConditionNodeWithoutCommentedMethods("OldServerAlarm", CONDITION_NODE_ID + 42);

        startAdapterWith(conditionNodeId);

        final WritingOutput output = writeToCondition(conditionNodeId, """
                {"method": "SUPPRESS", "comment": "this server cannot record me"}
                """);

        verify(output, timeout(10_000)).finish();

        final List<TestNamespace.MethodCall> calls =
                opcUaServerExtension.getTestNamespace().methodCallsExcludingRefresh();
        assertThat(calls).hasSize(1);
        assertThat(calls.get(0).methodName())
                .as("the suppression must happen even though the comment cannot be recorded")
                .isEqualTo("Suppress");
    }

    @Test
    @Timeout(120)
    void whenAConditionIsConfirmed_thenTheServerIsAskedToConfirm() throws Exception {
        final String conditionNodeId = opcUaServerExtension
                .getTestNamespace()
                .addAcknowledgeableConditionNode("ConfirmableAlarm", CONDITION_NODE_ID + 1);

        startAdapterWith(conditionNodeId);

        final String eventId = Base64.getEncoder().encodeToString("transition-7".getBytes());
        final WritingOutput output = writeToCondition(conditionNodeId, """
                {"eventId": "%s", "method": 1, "comment": ""}
                """.formatted(eventId));

        verify(output, timeout(10_000)).finish();

        // Same action, different parameter: the point of the unified command is that only `method` changes.
        final List<TestNamespace.MethodCall> calls =
                opcUaServerExtension.getTestNamespace().methodCallsExcludingRefresh();
        assertThat(calls).hasSize(1);
        assertThat(calls.get(0).methodName()).isEqualTo("Confirm");
    }

    @Test
    @Timeout(120)
    void whenTheMethodIsNamed_thenItIsAcceptedToo() throws Exception {
        final String conditionNodeId = opcUaServerExtension
                .getTestNamespace()
                .addAcknowledgeableConditionNode("NamedMethodAlarm", CONDITION_NODE_ID + 2);

        startAdapterWith(conditionNodeId);

        final String eventId = Base64.getEncoder().encodeToString("transition-9".getBytes());
        final WritingOutput output = writeToCondition(conditionNodeId, """
                {"eventId": "%s", "method": "ACKNOWLEDGE", "comment": "by name"}
                """.formatted(eventId));

        verify(output, timeout(10_000)).finish();
        assertThat(opcUaServerExtension.getTestNamespace().methodCallsExcludingRefresh())
                .singleElement()
                .satisfies(call -> assertThat(call.methodName()).isEqualTo("Acknowledge"));
    }

    @Test
    @Timeout(120)
    void whenTheCommandIsMalformed_thenTheWriteFailsAndNothingIsCalled() throws Exception {
        final String conditionNodeId = opcUaServerExtension
                .getTestNamespace()
                .addAcknowledgeableConditionNode("StrictAlarm", CONDITION_NODE_ID + 3);

        startAdapterWith(conditionNodeId);

        // No eventId: there is no way to know which transition this refers to, and guessing at one risks
        // acknowledging something the operator did not intend.
        final WritingOutput output = writeToCondition(conditionNodeId, """
                {"method": 0, "comment": "no event id"}
                """);

        verify(output, timeout(10_000)).fail(org.mockito.ArgumentMatchers.anyString());

        // Checked after the failure is observed: the point is not merely that the write failed, but that the
        // command was rejected locally and never became a call on the server.
        assertThat(opcUaServerExtension.getTestNamespace().methodCallsExcludingRefresh())
                .as("a command that cannot be understood must not reach the server")
                .isEmpty();
    }

    @Test
    @Timeout(120)
    void whenTheEventIdIsNotBase64_thenTheWriteFailsAndNothingIsCalled() throws Exception {
        final String conditionNodeId = opcUaServerExtension
                .getTestNamespace()
                .addAcknowledgeableConditionNode("PickyAlarm", CONDITION_NODE_ID + 8);

        startAdapterWith(conditionNodeId);

        // An EventId is base64 of the bytes the server issued, and the schema says so. There is deliberately
        // no fallback that reads an undecodable value as literal text: base64 and arbitrary text cannot be
        // told apart by inspection, so a guess would silently acknowledge a transition nobody named.
        final WritingOutput output = writeToCondition(conditionNodeId, """
                {"method": 0, "eventId": "not base64!", "comment": "typed by hand"}
                """);

        verify(output, timeout(10_000)).fail(org.mockito.ArgumentMatchers.anyString());

        assertThat(opcUaServerExtension.getTestNamespace().methodCallsExcludingRefresh())
                .as("an eventId that cannot be decoded must not reach the server")
                .isEmpty();
    }

    @Test
    @Timeout(120)
    void whenAMethodTakesNoArguments_thenNoEventIdIsNeeded() throws Exception {
        final String conditionNodeId = opcUaServerExtension
                .getTestNamespace()
                .addAcknowledgeableConditionNode("SuppressibleAlarm", CONDITION_NODE_ID + 4);

        startAdapterWith(conditionNodeId);

        // Ten of the fourteen methods act on the condition as a whole rather than on one transition, so
        // requiring an eventId here would make them impossible to call.
        final WritingOutput output = writeToCondition(conditionNodeId, """
                {"method": "SUPPRESS"}
                """);

        verify(output, timeout(10_000)).finish();
        assertThat(opcUaServerExtension.getTestNamespace().methodCallsExcludingRefresh())
                .singleElement()
                .satisfies(call -> assertThat(call.methodName()).isEqualTo("Suppress"));
    }

    @Test
    @Timeout(120)
    void whenTheConditionIsTimedShelved_thenTheDurationReachesTheShelvingState() throws Exception {
        final String conditionNodeId = opcUaServerExtension
                .getTestNamespace()
                .addAcknowledgeableConditionNode("ShelvableAlarm", CONDITION_NODE_ID + 5);

        startAdapterWith(conditionNodeId);

        // TimedShelve is the one method taking a duration, and it lives on the condition's ShelvingState
        // object rather than on the condition -- so this exercises the descent as well as the argument.
        final WritingOutput output = writeToCondition(conditionNodeId, """
                {"method": "TIMED_SHELVE", "duration": 5000}
                """);

        verify(output, timeout(10_000)).finish();
        assertThat(opcUaServerExtension.getTestNamespace().methodCallsExcludingRefresh())
                .singleElement()
                .satisfies(call -> {
                    assertThat(call.methodName()).isEqualTo("TimedShelve");
                    assertThat(call.duration()).isEqualTo(5000.0);
                });
    }

    @Test
    @Timeout(120)
    void whenTimedShelveHasNoDuration_thenTheWriteFailsAndNothingIsCalled() throws Exception {
        final String conditionNodeId = opcUaServerExtension
                .getTestNamespace()
                .addAcknowledgeableConditionNode("StrictShelveAlarm", CONDITION_NODE_ID + 6);

        startAdapterWith(conditionNodeId);

        // Which fields are required follows from the method, so this is the same class of rejection as a
        // missing eventId on an acknowledge -- checked per method, not per field.
        final WritingOutput output = writeToCondition(conditionNodeId, """
                {"method": "TIMED_SHELVE"}
                """);

        verify(output, timeout(10_000)).fail(org.mockito.ArgumentMatchers.anyString());
        assertThat(opcUaServerExtension.getTestNamespace().methodCallsExcludingRefresh())
                .isEmpty();
    }

    private @NotNull WritingOutput writeToCondition(final @NotNull String conditionNodeId, final @NotNull String json)
            throws Exception {
        final JsonNode value = mapper.readTree(json);

        final WritingContext writingContext = mock(WritingContext.class);
        when(writingContext.getTagName()).thenReturn("alarm-tag");

        final WritingInput writingInput = mock(WritingInput.class);
        when(writingInput.getWritingContext()).thenReturn(writingContext);
        when(writingInput.getWritingPayload()).thenReturn(new OpcUaPayload(value));

        final WritingOutput output = mock(WritingOutput.class);
        requireAdapter().write(writingInput, output);
        return output;
    }

    private @NotNull OpcUaProtocolAdapter requireAdapter() {
        final OpcUaProtocolAdapter started = adapter;
        if (started == null) {
            throw new IllegalStateException("the adapter has not been started");
        }
        return started;
    }

    private void startAdapterWith(final @NotNull String conditionNodeId) {
        final OpcuaTag tag =
                new OpcuaTag("alarm-tag", "", new OpcuaTagDefinition(conditionNodeId, OpcuaTagKind.CONDITION));

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
        final List<Tag> genericTags = new ArrayList<>(List.of(tag));
        when(input.getTags()).thenReturn(genericTags);
        when(input.adapterFactories()).thenReturn(mock(AdapterFactories.class));
        when(input.getProtocolAdapterMetricsHelper()).thenReturn(mock(ProtocolAdapterMetricsService.class));

        final ModuleServices moduleServices = mock(ModuleServices.class);
        when(moduleServices.eventService()).thenReturn(eventService);
        when(input.moduleServices()).thenReturn(moduleServices);

        adapter = new OpcUaProtocolAdapter(adapterInformation, input);

        final ProtocolAdapterStartInput startInput = mock(ProtocolAdapterStartInput.class);
        when(startInput.moduleServices()).thenReturn(moduleServices);

        // OPC UA shares one client between directions: the Northbound start is what actually connects, and
        // the Southbound start is a no-op that assumes it. Writing therefore needs the Northbound start.
        adapter.start(
                ProtocolAdapterConnectionDirection.Northbound, startInput, mock(ProtocolAdapterStartOutput.class));

        await().untilAsserted(() -> assertThat(protocolAdapterState.getConnectionStatus())
                .isEqualTo(ProtocolAdapterState.ConnectionStatus.CONNECTED));
    }
}
