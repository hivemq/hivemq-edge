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
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodResult;
import org.eclipse.milo.opcua.stack.core.types.structured.CallResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.ResponseHeader;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Commands against a server that keeps its condition instances out of the AddressSpace.
 * <p>
 * Review-02 finding 1. OPC 10000-9 permits this server model explicitly and accommodates it repeatedly —
 * §5.7.3, §5.5.6 and §5.8.17.2 all note that "some Servers do not expose Condition instances in the
 * AddressSpace" and require every server to accept the ConditionId as the ObjectId instead. Edge used to
 * answer twelve of its fourteen commands, {@code ACKNOWLEDGE} among them, with a client-minted
 * {@code Bad_NotSupported} <b>without sending a Call at all</b>, because it read Part 9's silence about which
 * MethodId accompanies that ObjectId as a prohibition. The rule is stated once and generally, in OPC 10000-4
 * §5.12.2.2 Table 59: the methodId may be "the NodeId of the Method in the ObjectType that defines the
 * Method".
 * <p>
 * So the thing to assert throughout is that <b>a Call is sent</b>, with the ConditionId as ObjectId and the
 * standard type's method node as MethodId — and that a refusal, when one comes, is the server's own status
 * rather than Edge's guess about what the server would have said.
 * <p>
 * Stubbed rather than run against the embedded server for the same reason the sibling resolution test is:
 * Milo's namespace always exposes the instance, so the case under test is one it cannot model.
 */
class ConditionUpdateWriterHiddenInstanceTest {

    private static final @NotNull NodeId CONDITION = NodeId.parse("ns=2;s=Boiler1.HighTemp");
    /** What {@link #componentReference} gives a component of that name, so the stub agrees with itself. */
    private static final @NotNull NodeId SHELVING_STATE = NodeId.parse("ns=2;s=ShelvingState");

    private static final @NotNull ByteString EVENT_ID = ByteString.of(new byte[] {1, 2, 3, 4});

    /** What each node exposes as components. Absent means the node cannot be browsed at all. */
    private final @NotNull Map<NodeId, List<String>> components = new HashMap<>();

    private @NotNull OpcUaClient client;

    @BeforeEach
    void setUp() {
        client = mock(OpcUaClient.class);
        when(client.getNamespaceTable()).thenReturn(new NamespaceTable());
        when(client.browseAsync(any(BrowseDescription.class))).thenAnswer(invocation -> {
            final BrowseDescription browse = invocation.getArgument(0);
            final List<String> exposed = components.getOrDefault(browse.getNodeId(), List.of());
            final ReferenceDescription[] references = exposed.stream()
                    .map(ConditionUpdateWriterHiddenInstanceTest::componentReference)
                    .toArray(ReferenceDescription[]::new);
            return CompletableFuture.completedFuture(
                    new BrowseResult(StatusCode.GOOD, ByteString.NULL_VALUE, references));
        });
        answersCallsWith(StatusCode.GOOD);
    }

    // ── the operations the headline workflow needs ──────────────────────────────────────────────────

    @Test
    void acknowledgeReachesTheServerThroughTheAcknowledgeableConditionTypesMethod() {
        // The release blocker. This is the method EDG-835 exists for, and it used to fail here without the
        // server ever being asked.
        final StatusCode status = request(ConditionUpdate.Method.ACKNOWLEDGE, EVENT_ID, "checked", null);

        assertThat(status.isGood()).isTrue();
        final CallMethodRequest call = onlyCall();
        assertThat(call.getObjectId())
                .as("the ConditionId is the ObjectId -- §5.7.3 forbids the type node here")
                .isEqualTo(CONDITION);
        assertThat(call.getMethodId()).isEqualTo(NodeIds.AcknowledgeableConditionType_Acknowledge);
        assertThat(call.getInputArguments())
                .containsExactly(Variant.of(EVENT_ID), Variant.of(LocalizedText.english("checked")));
    }

    @Test
    void confirmDoesTheSame() {
        request(ConditionUpdate.Method.CONFIRM, EVENT_ID, null, null);

        final CallMethodRequest call = onlyCall();
        assertThat(call.getObjectId()).isEqualTo(CONDITION);
        assertThat(call.getMethodId()).isEqualTo(NodeIds.AcknowledgeableConditionType_Confirm);
        assertThat(call.getInputArguments())
                .as("an absent comment is the specification's null LocalizedText, not an erase")
                .containsExactly(Variant.of(EVENT_ID), Variant.of(LocalizedText.NULL_VALUE));
    }

    @Test
    void addCommentUsesConditionTypesOwnMethod() {
        // AddComment is declared by ConditionType rather than AcknowledgeableConditionType, so it is the one
        // method of the three whose defining type differs.
        request(ConditionUpdate.Method.ADD_COMMENT, EVENT_ID, "note", null);

        assertThat(onlyCall().getMethodId()).isEqualTo(NodeIds.ConditionType_AddComment);
    }

    @Test
    void anAlarmMethodUsesAlarmConditionTypes() {
        request(ConditionUpdate.Method.SILENCE, null, null, null);

        final CallMethodRequest call = onlyCall();
        assertThat(call.getObjectId()).isEqualTo(CONDITION);
        assertThat(call.getMethodId()).isEqualTo(NodeIds.AlarmConditionType_Silence);
        assertThat(call.getInputArguments()).as("Silence takes no arguments").isEmpty();
    }

    @Test
    void enableStillUsesTheIdPartNineNamesOutright() {
        // One of the two the old table held. §5.5.4 and §5.5.5 name these explicitly, so they are the one
        // case where Part 9 and Part 4 agree in so many words, and the fix must not have moved them.
        request(ConditionUpdate.Method.ENABLE, null, null, null);

        assertThat(onlyCall().getMethodId()).isEqualTo(NodeIds.ConditionType_Enable);
    }

    @Test
    void andSoDoesDisable() {
        request(ConditionUpdate.Method.DISABLE, null, null, null);

        assertThat(onlyCall().getMethodId()).isEqualTo(NodeIds.ConditionType_Disable);
    }

    // ── shelving: the defining type follows the ObjectId ────────────────────────────────────────────

    @Test
    void shelvingOnAnExposedStateMachineUsesTheStateMachinesMethod() {
        // The server exposes ShelvingState but not the method beneath it. The Call names the state machine
        // object, so the type that defines the method is ShelvedStateMachineType.
        components.put(CONDITION, List.of("ShelvingState"));

        request(ConditionUpdate.Method.UNSHELVE, null, null, null);

        final CallMethodRequest call = onlyCall();
        assertThat(call.getObjectId()).isEqualTo(SHELVING_STATE);
        assertThat(call.getMethodId()).isEqualTo(NodeIds.ShelvedStateMachineType_Unshelve);
    }

    @Test
    void shelvingWithNoStateMachineAtAllGoesThroughTheCondition() {
        // Nothing is exposed, so the ObjectId falls back to the condition -- which §5.8.17.2 requires every
        // server to accept -- and the defining type becomes AlarmConditionType, which declares the same
        // operation one level down at ShelvingState.
        request(ConditionUpdate.Method.ONE_SHOT_SHELVE, null, null, null);

        final CallMethodRequest call = onlyCall();
        assertThat(call.getObjectId()).isEqualTo(CONDITION);
        assertThat(call.getMethodId()).isEqualTo(NodeIds.AlarmConditionType_ShelvingState_OneShotShelve);
    }

    @Test
    void timedShelveCarriesItsDurationEitherWay() {
        request(ConditionUpdate.Method.TIMED_SHELVE, null, null, 5_000.0);

        final CallMethodRequest call = onlyCall();
        assertThat(call.getObjectId()).isEqualTo(CONDITION);
        assertThat(call.getMethodId()).isEqualTo(NodeIds.AlarmConditionType_ShelvingState_TimedShelve);
        assertThat(call.getInputArguments()).containsExactly(Variant.of(5_000.0));
    }

    // ── the server decides ─────────────────────────────────────────────────────────────────────────

    @Test
    void aRefusalIsTheServersStatusAndNotEdgesGuess() {
        // The distinction the finding turns on. Edge may not decide that a method is unsupported; only the
        // server can, and it has to be asked before it can answer. ACKNOWLEDGE has no commented variant, so
        // there is nothing to retry and the server's status is the answer.
        answersCallsWith(new StatusCode(StatusCodes.Bad_NotSupported));

        final StatusCode status = request(ConditionUpdate.Method.ACKNOWLEDGE, EVENT_ID, null, null);

        assertThat(status).isEqualTo(new StatusCode(StatusCodes.Bad_NotSupported));
        verify(client, times(1)).callAsync(any());
    }

    @Test
    void aCommentIsDroppedWhenTheServerSaysTheCommentedFormIsNotThere() {
        // Browsing cannot narrow the choice with no instance to browse, so the form the user's intent asks
        // for goes first and the server's status stands in for the browse result. The documented trade
        // survives: the alarm is still suppressed, and the comment is what gives way.
        answersCallsWith(new StatusCode(StatusCodes.Bad_MethodInvalid), StatusCode.GOOD);

        final StatusCode status = request(ConditionUpdate.Method.SUPPRESS, null, "burner checked", null);

        assertThat(status.isGood()).isTrue();
        final List<CallMethodRequest> calls = allCalls();
        assertThat(calls).hasSize(2);
        assertThat(calls.get(0).getMethodId()).isEqualTo(NodeIds.AlarmConditionType_Suppress2);
        assertThat(calls.get(0).getInputArguments())
                .containsExactly(Variant.of(LocalizedText.english("burner checked")));
        assertThat(calls.get(1).getMethodId()).isEqualTo(NodeIds.AlarmConditionType_Suppress);
        assertThat(calls.get(1).getInputArguments()).isEmpty();
    }

    @Test
    void andTheOlderFormGivesWayToTheNewerOneJustAsReadily() {
        // The same rule in the other direction, which is the one the review-01 fix established for instances:
        // a server may implement either form, so neither may be assumed present.
        answersCallsWith(new StatusCode(StatusCodes.Bad_NodeIdUnknown), StatusCode.GOOD);

        final StatusCode status = request(ConditionUpdate.Method.RESET, null, null, null);

        assertThat(status.isGood()).isTrue();
        final List<CallMethodRequest> calls = allCalls();
        assertThat(calls).hasSize(2);
        assertThat(calls.get(0).getMethodId()).isEqualTo(NodeIds.AlarmConditionType_Reset);
        assertThat(calls.get(1).getMethodId()).isEqualTo(NodeIds.AlarmConditionType_Reset2);
        assertThat(calls.get(1).getInputArguments())
                .as("the newer form is called with the null comment, which §5.7.3 defines as leaving it alone")
                .containsExactly(Variant.of(LocalizedText.NULL_VALUE));
    }

    @Test
    void aRefusalThatIsNotAboutTheMethodNodeIsNotRetried() {
        // Bad_UserAccessDenied means the server found the method and declined the operation. Trying the other
        // form would fail identically at best, and at worst -- on a server that reports a partial failure this
        // way -- would perform the operation twice.
        answersCallsWith(new StatusCode(StatusCodes.Bad_UserAccessDenied), StatusCode.GOOD);

        final StatusCode status = request(ConditionUpdate.Method.SUPPRESS, null, "burner checked", null);

        assertThat(status).isEqualTo(new StatusCode(StatusCodes.Bad_UserAccessDenied));
        verify(client, times(1)).callAsync(any());
    }

    @Test
    void anInstanceMethodIsStillPreferredWhenTheServerHasOne() {
        // The tier order, pinned from this side too: a vendor's own instance method node must keep winning,
        // because it is the form every server accepts and the only one a non-standard subtype can offer.
        components.put(CONDITION, List.of("Acknowledge"));

        request(ConditionUpdate.Method.ACKNOWLEDGE, EVENT_ID, null, null);

        assertThat(onlyCall().getMethodId())
                .as("the browsed instance node, not the type's")
                .isEqualTo(NodeId.parse("ns=2;s=Acknowledge"));
    }

    // ── helpers ─────────────────────────────────────────────────────────────────────────────────────

    private @NotNull StatusCode request(
            final ConditionUpdate.Method method,
            final ByteString eventId,
            final String comment,
            final Double duration) {
        return ConditionUpdateWriter.requestTransition(
                        client, CONDITION, new ConditionUpdate(method, eventId, comment, duration, null))
                .join();
    }

    /** Answers successive calls with these statuses, repeating the last one thereafter. */
    private void answersCallsWith(final @NotNull StatusCode... statuses) {
        final int[] next = {0};
        when(client.callAsync(any())).thenAnswer(invocation -> {
            final StatusCode status = statuses[Math.min(next[0]++, statuses.length - 1)];
            return CompletableFuture.completedFuture(new CallResponse(
                    new ResponseHeader(null, uint(0), StatusCode.GOOD, null, null, null),
                    new CallMethodResult[] {new CallMethodResult(status, null, null, null)},
                    null));
        });
    }

    private @NotNull CallMethodRequest onlyCall() {
        final List<CallMethodRequest> calls = allCalls();
        assertThat(calls).as("exactly one Call was expected").hasSize(1);
        return calls.get(0);
    }

    @SuppressWarnings("unchecked")
    private @NotNull List<CallMethodRequest> allCalls() {
        final ArgumentCaptor<List<CallMethodRequest>> captor = ArgumentCaptor.forClass(List.class);
        verify(client, org.mockito.Mockito.atLeastOnce()).callAsync(captor.capture());
        final List<CallMethodRequest> flattened = new ArrayList<>();
        captor.getAllValues().forEach(flattened::addAll);
        return flattened;
    }

    /** The node id this stub gives a component of that name, so a call can be checked against it. */
    private static @NotNull ReferenceDescription componentReference(final @NotNull String browseName) {
        return new ReferenceDescription(
                NodeIds.HasComponent,
                true,
                ExpandedNodeId.parse("ns=2;s=" + browseName),
                // Namespace 0: these are specification-defined names, and Browsing.isStandardName requires it.
                new QualifiedName(0, browseName),
                LocalizedText.english(browseName),
                "ShelvingState".equals(browseName) ? NodeClass.Object : NodeClass.Method,
                ExpandedNodeId.NULL_VALUE);
    }
}
