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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Commands against a server that keeps its condition instances out of the AddressSpace.
 * <p>
 * Review-02 finding 1. OPC 10000-9 permits this server model explicitly and accommodates it repeatedly —
 * §5.7.3, §5.5.6 and §5.8.17.2 all note that "some Servers do not expose Condition instances in the
 * AddressSpace" and require every server to accept the ConditionId as the ObjectId instead. Edge used to
 * answer every command but {@code ENABLE} and {@code DISABLE} — {@code ACKNOWLEDGE} among them — with a
 * client-minted {@code Bad_NotSupported} <b>without sending a Call at all</b>, because it read Part 9's silence about which
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
 * <p>
 * <b>Review-03 finding 1 corrected the stub itself.</b> Its first version answered every browse with
 * {@code Good} and an empty reference array, which is a node that exists and has no children — not a node the
 * server keeps out of its AddressSpace. The tests passed, and the case they were written for did not work:
 * a conforming server refuses the browse with {@code Bad_NodeIdUnknown}, and that failure propagated past the
 * fallback. The default here is now the real status, with the childless-but-present case kept as its own test.
 */
class ConditionUpdateWriterHiddenInstanceTest {

    private static final @NotNull NodeId CONDITION = NodeId.parse("ns=2;s=Boiler1.HighTemp");
    /** What {@link #componentReference} gives a component of that name, so the stub agrees with itself. */
    private static final @NotNull NodeId SHELVING_STATE = NodeId.parse("ns=2;s=ShelvingState");

    private static final @NotNull ByteString EVENT_ID = ByteString.of(new byte[] {1, 2, 3, 4});

    /**
     * What each node exposes as components. A node in neither this map nor {@link #exposedWithNoComponents}
     * is <b>not in the AddressSpace at all</b>, and the stub answers a browse of it the way a conforming
     * server does: {@code Bad_NodeIdUnknown}.
     * <p>
     * That status is the point of this fixture and was the flaw in its first version, which answered
     * {@code Good} with an empty reference array. Those are two different servers. {@code Good} with nothing
     * in it describes a node that is present and childless — so it exercised the type-level fallback under
     * the one status that let control reach it, and the genuinely hidden instance the fallback exists for
     * still failed: {@code Browsing.browseAll} turns a bad operation status into a failed future, which
     * propagated straight past the {@code resolved == null} branch.
     */
    private final @NotNull Map<NodeId, List<String>> components = new HashMap<>();

    /** Nodes the server does expose, but which carry no components — the other, weaker case. */
    private final @NotNull Set<NodeId> exposedWithNoComponents = new HashSet<>();

    /** When set, every browse is refused with this status instead — for failures that are not about existence. */
    private @Nullable StatusCode browseRefusal;

    private @NotNull OpcUaClient client;

    @BeforeEach
    void setUp() {
        client = mock(OpcUaClient.class);
        when(client.getNamespaceTable()).thenReturn(new NamespaceTable());
        when(client.browseAsync(any(BrowseDescription.class))).thenAnswer(invocation -> {
            final BrowseDescription browse = invocation.getArgument(0);
            if (browseRefusal != null) {
                return CompletableFuture.completedFuture(new BrowseResult(browseRefusal, ByteString.NULL_VALUE, null));
            }
            final NodeId node = browse.getNodeId();
            if (!components.containsKey(node) && !exposedWithNoComponents.contains(node)) {
                // The server model this whole class is about: the instance is not in the AddressSpace, so
                // there is no node to browse and the server says exactly that.
                return CompletableFuture.completedFuture(
                        new BrowseResult(new StatusCode(StatusCodes.Bad_NodeIdUnknown), ByteString.NULL_VALUE, null));
            }
            final ReferenceDescription[] references = components.getOrDefault(node, List.of()).stream()
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
        exposedWithNoComponents.add(SHELVING_STATE);

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
    void aNodeThatIsExposedButChildlessReachesTheSameFallback() {
        // The weaker case, and the one this fixture used to model exclusively. A server that answers the
        // browse with Good and no references has told us the node is there and has no Acknowledge beneath
        // it; the type-level MethodId is the right answer for that too. Kept as a distinct test rather than
        // as the default so neither case can be lost behind the other again.
        exposedWithNoComponents.add(CONDITION);

        request(ConditionUpdate.Method.ACKNOWLEDGE, EVENT_ID, null, null);

        final CallMethodRequest call = onlyCall();
        assertThat(call.getObjectId()).isEqualTo(CONDITION);
        assertThat(call.getMethodId()).isEqualTo(NodeIds.AcknowledgeableConditionType_Acknowledge);
    }

    @Test
    void aRefusalThatIsNotAboutTheNodesExistenceIsNotTakenForAHiddenInstance() {
        // The boundary of the fallback, and the reason it keys on one status rather than on "the browse
        // failed". Bad_UserAccessDenied says the session may not browse the node -- it says nothing about
        // whether the node is there -- so guessing that the condition is hidden and calling the standard
        // MethodId anyway would send a side-effecting Call after the server declined to answer.
        browseRefusal = new StatusCode(StatusCodes.Bad_UserAccessDenied);

        assertThatThrownBy(() -> request(ConditionUpdate.Method.ACKNOWLEDGE, EVENT_ID, null, null))
                .hasRootCauseInstanceOf(Browsing.BrowseFailedException.class);

        verify(client, never()).callAsync(any());
    }

    @Test
    void neitherIsATransportFailure() {
        // The same rule for a failure that carries no status at all. Nothing about a dropped connection
        // implies the condition is absent from the AddressSpace.
        when(client.browseAsync(any(BrowseDescription.class)))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("not connected")));

        assertThatThrownBy(() -> request(ConditionUpdate.Method.ACKNOWLEDGE, EVENT_ID, null, null))
                .hasRootCauseInstanceOf(IllegalStateException.class);

        verify(client, never()).callAsync(any());
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
