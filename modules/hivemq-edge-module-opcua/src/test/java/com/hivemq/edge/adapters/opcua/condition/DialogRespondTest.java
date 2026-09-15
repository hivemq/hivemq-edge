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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.adapter.sdk.api.schema.SchemaJsonRepresentation;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaConditionType;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
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
 * Answering a dialog, which Edge could report and not respond to.
 * <p>
 * Review-02 finding 12. {@code DialogConditionType} is one of the twenty-two types Edge publishes, and the
 * read side supports it fully — {@code Prompt}, {@code ResponseOptionSet}, {@code DefaultResponse},
 * {@code DialogState} and the rest are all selected. The command enum had fourteen methods and none of them
 * was a dialog response, so a server could ask Edge a question, Edge would forward the question and the list
 * of permitted answers, and there was no way to give one. The type was presented as supported while being
 * operationally read-only.
 * <p>
 * OPC 10000-9 §5.6.3 defines {@code Respond(SelectedResponse)} and §5.6.4 the newer
 * {@code Respond2(SelectedResponse, Comment)} — so this is the one non-alarm method with a {@code "2"}
 * variant, and it reaches the same both-directions resolution the alarm methods use.
 */
class DialogRespondTest {

    private static final @NotNull NodeId DIALOG = NodeId.parse("ns=2;s=Boiler1.StartDialog");

    private final @NotNull ObjectMapper mapper = new ObjectMapper();

    private @NotNull OpcUaClient client;

    @BeforeEach
    void setUp() {
        client = mock(OpcUaClient.class);
        when(client.getNamespaceTable()).thenReturn(new NamespaceTable());
        // A server exposing no instance methods, so every call falls through to the standard type's node.
        when(client.browseAsync(any(BrowseDescription.class)))
                .thenReturn(CompletableFuture.completedFuture(
                        new BrowseResult(StatusCode.GOOD, ByteString.NULL_VALUE, new ReferenceDescription[0])));
        when(client.callAsync(any()))
                .thenReturn(CompletableFuture.completedFuture(new CallResponse(
                        new ResponseHeader(null, uint(0), StatusCode.GOOD, null, null, null),
                        new CallMethodResult[] {new CallMethodResult(StatusCode.GOOD, null, null, null)},
                        null)));
    }

    // ── parsing ─────────────────────────────────────────────────────────────────────────────────────

    @Test
    void aDialogResponseIsAMethodLikeAnyOther() throws Exception {
        final ConditionUpdate update = parse("""
                { "method": "RESPOND", "selectedResponse": 1 }
                """);

        assertThat(update.method()).isEqualTo(ConditionUpdate.Method.RESPOND);
        assertThat(update.selectedResponse()).isEqualTo(1);
    }

    @Test
    void andTheAnswerIsRequiredRatherThanDefaulted() {
        // There is no safe default for "which answer did you mean". Zero is a valid option on every dialog
        // that offers any, so defaulting would answer the question on the operator's behalf.
        assertThatThrownBy(() -> parse("""
                        { "method": "RESPOND" }
                        """))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("selectedResponse")
                .hasMessageContaining("ResponseOptionSet");
    }

    @Test
    void aNonIntegerAnswerIsRejected() {
        // An index into an array. A fractional or textual one is not a mistake to interpret generously.
        assertThatThrownBy(() -> parse("""
                        { "method": "RESPOND", "selectedResponse": 1.5 }
                        """)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> parse("""
                        { "method": "RESPOND", "selectedResponse": "yes" }
                        """)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aNegativeIndexIsRejected() {
        assertThatThrownBy(() -> parse("""
                        { "method": "RESPOND", "selectedResponse": -1 }
                        """))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be negative");
    }

    // ── review-03 finding 2: the range the type does not carry ─────────────────────────────────────

    @Test
    void anAnswerTooLargeForAnInt32IsRefusedRatherThanWrapped() {
        // The finding, and the reason it is High rather than cosmetic. asInt() is a narrowing cast: it took
        // 4294967296 to 0, which is a valid index on every dialog that offers any options. The request did
        // not fail -- it answered a different question, and the server received a well-formed Int32 with no
        // way to know it was not what the caller sent. Responding to a dialog actuates plant.
        assertThatThrownBy(() -> parse("""
                        { "method": "RESPOND", "selectedResponse": 4294967296 }
                        """))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Int32")
                .hasMessageContaining("4294967296");
    }

    @Test
    void andSoIsOneThatWouldWrapOntoADifferentValidAnswer() {
        // The same cast one step along: 4294967297 became 1. Two adjacent out-of-range values selecting two
        // adjacent real options is the shape of the defect, so both are pinned.
        assertThatThrownBy(() -> parse("""
                        { "method": "RESPOND", "selectedResponse": 4294967297 }
                        """)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void andSoIsALargeNegativeOneThatWouldWrapPastTheNegativeCheck() {
        // -4294967296 narrowed to 0 -- non-negative by the time the sign was checked, so the guard below it
        // never saw the value the caller actually sent.
        assertThatThrownBy(() -> parse("""
                        { "method": "RESPOND", "selectedResponse": -4294967296 }
                        """))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Int32");
    }

    @Test
    void andAnIntegerFarBeyondALongToo() {
        // Jackson parses this as a BigInteger rather than a LongNode, which is a different node type taking a
        // different path to the same cast.
        assertThatThrownBy(() -> parse("""
                        { "method": "RESPOND", "selectedResponse": 99999999999999999999999999 }
                        """))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Int32");
    }

    @Test
    void theBoundsThemselvesAreAccepted() {
        // The other half of a range check: it must not refuse the values that are in range. Int32's largest
        // is a legitimate, if improbable, index, and zero is the first option of every dialog.
        assertThat(parseQuietly("""
                        { "method": "RESPOND", "selectedResponse": 0 }
                        """).selectedResponse()).isZero();
        assertThat(parseQuietly("""
                        { "method": "RESPOND", "selectedResponse": 2147483647 }
                        """).selectedResponse()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void aRefusedAnswerSendsNoCallAtAll() {
        // The consequence that matters. A rejection has to happen before the server is asked to do anything,
        // or the range check is only a nicer error message on an action already taken.
        assertThatThrownBy(() -> request("""
                        { "method": "RESPOND", "selectedResponse": 4294967296 }
                        """)).isInstanceOf(IllegalArgumentException.class);

        verify(client, never()).callAsync(any());
    }

    @Test
    void andTheSchemaBoundsTheFieldSoAGeneratedClientCannotBuildOne() {
        // The runtime check stays mandatory -- nothing guarantees a caller validated against this schema --
        // but a client generated from it should not be able to construct the bad request in the first place.
        // The SDK has no scalar narrower than LONG, so the bounds are how Int32 is expressed at all.
        final ObjectNode field = (ObjectNode) SchemaJsonRepresentation.INSTANCE
                .toJsonSchemaDocument(ConditionSchemas.writeSchema())
                .get("properties")
                .get(ConditionUpdate.FIELD_SELECTED_RESPONSE);

        assertThat(field.get("minimum").longValue()).isZero();
        assertThat(field.get("maximum").longValue())
                .as("JSON Schema's 'maximum' is inclusive, so the largest Int32 is itself allowed")
                .isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void anAnswerSuppliedToAMethodThatTakesNoneIsIgnored() {
        // The same rule the other optional fields follow: meaningless rather than invalid, so it is dropped
        // rather than rejected. A payload template carrying every field must not fail on the methods that
        // read only some of it.
        assertThat(parseQuietly("""
                        { "method": "ACKNOWLEDGE", "eventId": "AQIDBA==", "selectedResponse": 3 }
                        """).selectedResponse()).isNull();
    }

    // ── the call ────────────────────────────────────────────────────────────────────────────────────

    @Test
    void respondSendsTheIndexAsAnInt32() throws Exception {
        // OPC 10000-9 §5.6.3 declares SelectedResponse an Int32, and a Variant carrying anything else is a
        // type mismatch the server rejects rather than coerces.
        request("""
                { "method": "RESPOND", "selectedResponse": 2 }
                """);

        final CallMethodRequest call = onlyCall();
        assertThat(call.getObjectId()).isEqualTo(DIALOG);
        assertThat(call.getMethodId()).isEqualTo(NodeIds.DialogConditionType_Respond);
        assertThat(call.getInputArguments()).containsExactly(Variant.of(2));
    }

    @Test
    void andRespond2CarriesTheCommentAfterIt() throws Exception {
        // §5.6.4: Respond2(SelectedResponse, Comment), in that order. The only other two-argument method is
        // TimedShelve2, and getting the order wrong on either is a silent mis-call rather than a failure.
        request("""
                { "method": "RESPOND", "selectedResponse": 0, "comment": "operator confirmed" }
                """);

        final CallMethodRequest call = onlyCall();
        assertThat(call.getMethodId()).isEqualTo(NodeIds.DialogConditionType_Respond2);
        assertThat(call.getInputArguments())
                .containsExactly(Variant.of(0), Variant.of(LocalizedText.english("operator confirmed")));
    }

    // ── the contract it is advertised under ─────────────────────────────────────────────────────────

    @Test
    void theWriteSchemaDescribesTheField() {
        final ObjectNode properties = (ObjectNode) SchemaJsonRepresentation.INSTANCE
                .toJsonSchemaDocument(ConditionSchemas.writeSchema())
                .get("properties");

        assertThat(properties.has(ConditionUpdate.FIELD_SELECTED_RESPONSE))
                .as("a command nobody can discover from the schema is not offered")
                .isTrue();
        assertThat(properties.get(ConditionUpdate.FIELD_SELECTED_RESPONSE).toString())
                .contains("ResponseOptionSet");
    }

    @Test
    void andTheMethodItselfIsListedThere() {
        // The method list in the schema description is generated from the enum, so this is really a check
        // that the enum entry exists -- but it is what an operator reads.
        assertThat(SchemaJsonRepresentation.INSTANCE
                        .toJsonSchemaDocument(ConditionSchemas.writeSchema())
                        .toString())
                .contains("RESPOND");
    }

    @Test
    void theTypeThatOffersDialogsStillPublishesItsOptions() {
        // The read side, unchanged and asserted here so the pair reads together: the options a caller picks
        // from are the ones the event carries.
        assertThat(OpcuaConditionType.DIALOG_CONDITION.allFields())
                .contains("ResponseOptionSet", "Prompt", "DefaultResponse", "DialogState");
    }

    // ── helpers ─────────────────────────────────────────────────────────────────────────────────────

    private @NotNull ConditionUpdate parse(final @NotNull String json) throws Exception {
        return ConditionUpdate.fromJson(mapper.readTree(json));
    }

    private @NotNull ConditionUpdate parseQuietly(final @NotNull String json) {
        try {
            return parse(json);
        } catch (final Exception e) {
            throw new AssertionError("the payload should have parsed", e);
        }
    }

    private void request(final @NotNull String json) throws Exception {
        ConditionUpdateWriter.requestTransition(client, DIALOG, parse(json)).join();
    }

    @SuppressWarnings("unchecked")
    private @NotNull CallMethodRequest onlyCall() {
        final ArgumentCaptor<List<CallMethodRequest>> captor = ArgumentCaptor.forClass(List.class);
        verify(client).callAsync(captor.capture());
        return captor.getValue().get(0);
    }
}
