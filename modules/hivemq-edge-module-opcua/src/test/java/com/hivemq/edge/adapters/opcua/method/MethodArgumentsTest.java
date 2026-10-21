/*
 * Copyright 2019-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.opcua.method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.XmlElement;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.ULong;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/// Turning a request's named arguments into the positional array a Call takes.
///
/// The translation is where a mistake does not fail but succeeds wrongly: pass the values in the wrong positions
/// and the server accepts them, and the device acts on them. So the order, the refusals and the range checks are
/// all pinned.
class MethodArgumentsTest {

    private static final @NotNull ObjectMapper MAPPER = new ObjectMapper();
    private static final @NotNull NodeId NODE = new NodeId(2, "Boiler1.Start");

    private static @NotNull Argument argument(final @NotNull String name, final @NotNull NodeId dataType) {
        return new Argument(name, dataType, -1, null, LocalizedText.NULL_VALUE);
    }

    private static @NotNull MethodSignature signature(final @NotNull Argument... inputs) {
        return MethodSignature.ofObject("Start", NODE, NODE, List.of(inputs), List.of());
    }

    private static @NotNull JsonNode json(final @NotNull String raw) throws Exception {
        return MAPPER.readTree(raw);
    }

    @Test
    void argumentsAreOrderedByTheMethod_notByTheRequest() throws Exception {
        // The decisive test. The request lists ramp before setpoint; the method declares the reverse. Getting
        // this wrong does not fail -- it starts the boiler with the ramp as its setpoint.
        final MethodSignature signature =
                signature(argument("setpoint", NodeIds.Int32), argument("ramp", NodeIds.Int32));

        final Variant[] arguments = MethodArguments.forCall(signature, json("{\"ramp\": 7, \"setpoint\": 180}"));

        assertThat(arguments).hasSize(2);
        assertThat(arguments[0].value()).isEqualTo(180);
        assertThat(arguments[1].value()).isEqualTo(7);
    }

    @Test
    void methodTakingNoArguments_yieldsAnEmptyArray() throws Exception {
        assertThat(MethodArguments.forCall(signature(), json("{}"))).isEmpty();
    }

    @Test
    void unknownArgumentName_isRefusedNamingIt() throws Exception {
        // Without this the misspelling is ignored and the caller gets a complaint about the argument they
        // believed they had supplied.
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("setpoint", NodeIds.Int32)), json("{\"setPoint\": 1}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("setPoint")
                .hasMessageContaining("It declares: setpoint");
    }

    @Test
    void missingArgument_isRefusedNamingWhatIsDeclared() throws Exception {
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("setpoint", NodeIds.Int32), argument("ramp", NodeIds.Int32)),
                        json("{\"setpoint\": 180}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("requires the argument 'ramp'");
    }

    @Test
    void aMissingArgument_isAnsweredBadArgumentsMissing_andEveryOtherFaultBadInvalidArgument() throws Exception {
        // OPC-UA names this fault specifically, so a caller branching on status codes can act on "you left
        // something out" without parsing the message. The second half matters as much as the first: it is
        // what stops the default being changed by accident, since every other argument fault shares it.
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("setpoint", NodeIds.Int32), argument("ramp", NodeIds.Int32)),
                        json("{\"setpoint\": 180}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .extracting(e -> ((MethodArguments.InvalidArgumentException) e).statusCode())
                .isEqualTo(StatusCodes.Bad_ArgumentsMissing);

        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("setpoint", NodeIds.Int32)), json("{\"setpiont\": 180}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .extracting(e -> ((MethodArguments.InvalidArgumentException) e).statusCode())
                .isEqualTo(StatusCodes.Bad_InvalidArgument);

        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("setpoint", NodeIds.Int32)), json("{\"setpoint\": \"hot\"}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .extracting(e -> ((MethodArguments.InvalidArgumentException) e).statusCode())
                .isEqualTo(StatusCodes.Bad_InvalidArgument);
    }

    @Test
    void explicitNull_countsAsMissing() throws Exception {
        // A caller sending null for an argument has not supplied it. Passing a null Variant instead would let the
        // device decide, and a device's answer to a missing argument is not something a caller can predict.
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("setpoint", NodeIds.Int32)), json("{\"setpoint\": null}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("requires the argument 'setpoint'");
    }

    @Test
    void outOfRangeValue_isRefusedRatherThanWrapped() throws Exception {
        // The reason this is checked here and not left to the server: the narrowing cast wraps silently, so 300
        // would reach a Byte argument as 44 and the device would act on it.
        assertThatThrownBy(() ->
                        MethodArguments.forCall(signature(argument("level", NodeIds.Byte)), json("{\"level\": 300}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("[0, 255]");
    }

    @Test
    void aLocalizedTextArgument_acceptsABareStringAsEnglish() throws Exception {
        // The case that motivated all six: AddComment(EventId, Comment) declares its comment a LocalizedText,
        // and a caller writes "acknowledged" rather than assembling an object. English because that is what
        // the condition path already does for the same argument of the same method, reached the other way.
        final var arguments = MethodArguments.forCall(
                signature(argument("Comment", NodeIds.LocalizedText)), json("{\"Comment\": \"acknowledged\"}"));

        final LocalizedText text = (LocalizedText) arguments[0].value();
        assertThat(text.getText()).isEqualTo("acknowledged");
        assertThat(text.getLocale()).isEqualTo("en");
    }

    @Test
    void aLocalizedTextArgument_acceptsTheObjectTheReadPathPublishes() throws Exception {
        // The other half of the same decision: a value read from a device comes back as this object, so it
        // has to be sendable without being rewritten by hand.
        final var arguments = MethodArguments.forCall(
                signature(argument("Comment", NodeIds.LocalizedText)),
                json("{\"Comment\": {\"locale\": \"de-DE\", \"text\": \"quittiert\"}}"));

        final LocalizedText text = (LocalizedText) arguments[0].value();
        assertThat(text.getText()).isEqualTo("quittiert");
        assertThat(text.getLocale()).isEqualTo("de-DE");
    }

    @Test
    void aLocalizedTextArgumentInObjectForm_keepsAnAbsentLocaleAbsent() throws Exception {
        // The read path omits locale when it is null, so {"text": "x"} is how a value with no locale comes
        // back from a device. Defaulting it to English here rewrote that value on its way back.
        final var arguments = MethodArguments.forCall(
                signature(argument("Comment", NodeIds.LocalizedText)),
                json("{\"Comment\": {\"text\": \"acknowledged\"}}"));

        final LocalizedText text = (LocalizedText) arguments[0].value();
        assertThat(text.getText()).isEqualTo("acknowledged");
        assertThat(text.getLocale()).isNull();
    }

    @Test
    void aLocalizedTextArgumentInObjectForm_keepsTheEmptyCasesTheProtocolDistinguishes() throws Exception {
        // OPC 10000-9 section 5.7.3, on a condition's Comment: both fields absent means "leave any existing
        // comment alone", while an empty text WITH a locale resets it. Conflating them turned the first into
        // the second -- the object form of "do not touch" became a command to clear.
        final var bothAbsent = MethodArguments.forCall(
                signature(argument("Comment", NodeIds.LocalizedText)), json("{\"Comment\": {}}"));
        final var emptyTextWithLocale = MethodArguments.forCall(
                signature(argument("Comment", NodeIds.LocalizedText)),
                json("{\"Comment\": {\"locale\": \"en\", \"text\": \"\"}}"));

        assertThat((LocalizedText) bothAbsent[0].value()).isEqualTo(LocalizedText.NULL_VALUE);
        assertThat((LocalizedText) emptyTextWithLocale[0].value()).isEqualTo(new LocalizedText("en", ""));
        assertThat(bothAbsent[0].value()).isNotEqualTo(emptyTextWithLocale[0].value());
    }

    @Test
    void aLocalizedTextArgumentInObjectForm_refusesAFieldOfTheWrongType() {
        // Absent is a statement the protocol has; a number is a caller error. Treating the second as the first
        // would discard what they said.
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("Comment", NodeIds.LocalizedText)),
                        json("{\"Comment\": {\"locale\": 42, \"text\": \"x\"}}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("locale");
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("Comment", NodeIds.LocalizedText)),
                        json("{\"Comment\": {\"text\": [\"x\"]}}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("text");
    }

    @Test
    void anObjectFormArgument_refusesAFieldTheFormDoesNotHave() {
        // The case that makes every optional field dangerous without this check: a misspelled name is
        // indistinguishable from an omitted one, so `Text` yields the null localised text -- which OPC 10000-9
        // defines as "leave any existing comment alone". The call would succeed and the note would be gone.
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("Comment", NodeIds.LocalizedText)),
                        json("{\"Comment\": {\"Text\": \"operator note\"}}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("Text");
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("Comment", NodeIds.LocalizedText)),
                        json("{\"Comment\": {\"locale\": \"en\", \"value\": \"operator note\"}}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("value");

        // A node id, where the silence sends the request to namespace 0 -- the Objects folder rather than the
        // caller's own namespace.
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("node", NodeIds.NodeId)),
                        json("{\"node\": {\"namespace\": 2, \"idType\": 0, \"id\": 85}}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("namespace");
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("name", NodeIds.QualifiedName)),
                        json("{\"name\": {\"ns\": 2, \"name\": \"Boiler\"}}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("ns");
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("status", NodeIds.StatusCode)),
                        json("{\"status\": {\"code\": 0, \"name\": \"Good\"}}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void aShortGuid_isRefusedWhereverItCanEnter() {
        // Every route a caller's Guid takes. Padding it would name a different node or a different method,
        // and nothing downstream could tell that from a value they meant.
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("g", NodeIds.Guid)),
                        json("{\"g\": \"72962b91-fa75-4ae6-8d28-b404dc7daf6\"}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class);
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("node", NodeIds.NodeId)),
                        json("{\"node\": {\"namespaceIndex\": 2, \"idType\": 2, "
                                + "\"id\": \"72962b91-fa75-4ae6-8d28-b404dc7daf6\"}}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class);
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("node", NodeIds.NodeId)),
                        json("{\"node\": \"ns=2;g=72962b91-fa75-4ae6-8d28-b404dc7daf6\"}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class);
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("node", NodeIds.ExpandedNodeId)), json("{\"node\": \"ns=2;g=1-1-1-1-1\"}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class);
    }

    @Test
    void aGuidTheReadPathPublished_isStillSentBack() throws Exception {
        // The round trip: populateNodeId writes UUID.toString(), so that spelling must keep working on
        // every one of those routes.
        final String guid = "72962b91-fa75-4ae6-8d28-0b404dc7daf6";
        final var bare =
                MethodArguments.forCall(signature(argument("g", NodeIds.Guid)), json("{\"g\": \"" + guid + "\"}"));
        final var objectForm = MethodArguments.forCall(
                signature(argument("node", NodeIds.NodeId)),
                json("{\"node\": {\"namespaceIndex\": 2, \"idType\": 2, \"id\": \"" + guid + "\"}}"));
        final var textForm = MethodArguments.forCall(
                signature(argument("node", NodeIds.NodeId)), json("{\"node\": \"ns=2;g=" + guid + "\"}"));

        assertThat((java.util.UUID) bare[0].value()).isEqualTo(java.util.UUID.fromString(guid));
        assertThat((NodeId) objectForm[0].value()).isEqualTo(new NodeId(2, java.util.UUID.fromString(guid)));
        assertThat((NodeId) textForm[0].value()).isEqualTo(new NodeId(2, java.util.UUID.fromString(guid)));
    }

    @Test
    void anObjectFormArgument_stillAcceptsEveryFieldTheReadPathPublishes() throws Exception {
        // The permitted set is what a read produces, because a value read from a device must be sendable back.
        // `symbol` is unread and still a field of the form, so it passes rather than being refused.
        final var withSymbol = MethodArguments.forCall(
                signature(argument("status", NodeIds.StatusCode)),
                json("{\"status\": {\"code\": 2155085824, \"symbol\": \"Bad_NotExecutable\"}}"));
        final var localeAndText = MethodArguments.forCall(
                signature(argument("Comment", NodeIds.LocalizedText)),
                json("{\"Comment\": {\"locale\": \"de\", \"text\": \"Notiz\"}}"));
        final var nodeId = MethodArguments.forCall(
                signature(argument("node", NodeIds.NodeId)),
                json("{\"node\": {\"namespaceIndex\": 2, \"idType\": 1, \"id\": \"Boiler\"}}"));
        final var qualifiedName = MethodArguments.forCall(
                signature(argument("name", NodeIds.QualifiedName)),
                json("{\"name\": {\"namespaceIndex\": 2, \"name\": \"Boiler\"}}"));

        assertThat(((StatusCode) withSymbol[0].value()).getValue()).isEqualTo(2155085824L);
        assertThat((LocalizedText) localeAndText[0].value()).isEqualTo(new LocalizedText("de", "Notiz"));
        assertThat((NodeId) nodeId[0].value()).isEqualTo(new NodeId(2, "Boiler"));
        assertThat((QualifiedName) qualifiedName[0].value()).isEqualTo(new QualifiedName(2, "Boiler"));
    }

    @Test
    void aNodeIdArgument_acceptsBothTheTextAndTheObjectForm() throws Exception {
        final var fromText = MethodArguments.forCall(
                signature(argument("node", NodeIds.NodeId)), json("{\"node\": \"ns=2;s=Boiler\"}"));
        final var fromObject = MethodArguments.forCall(
                signature(argument("node", NodeIds.NodeId)),
                json("{\"node\": {\"namespaceIndex\": 2, \"id\": \"Boiler\"}}"));

        assertThat(fromText[0].value()).isEqualTo(fromObject[0].value());
    }

    @Test
    void aNodeIdArgument_byNamespaceUri_isSentWithThisSessionsIndex() throws Exception {
        final NamespaceTable table = new NamespaceTable();
        table.add("urn:test:boiler"); // index 1

        final var arguments = MethodArguments.forCall(
                signature(argument("node", NodeIds.NodeId)),
                json("{\"node\": \"nsu=urn:test:boiler;s=Boiler\"}"),
                table);

        assertThat(arguments[0].value()).isEqualTo(new NodeId(1, "Boiler"));
    }

    @Test
    void aNodeIdArgument_byAnUnpublishedNamespaceUri_isRefusedNamingTheUri() {
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("node", NodeIds.NodeId)),
                        json("{\"node\": \"nsu=urn:not:published;s=Boiler\"}"),
                        new NamespaceTable()))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("'urn:not:published', which the server does not publish");
    }

    @Test
    void aNumericNodeIdArgument_takesTheWholeUInt32Range() throws Exception {
        final var arguments = MethodArguments.forCall(
                signature(argument("node", NodeIds.NodeId)),
                json("{\"node\": {\"namespaceIndex\": 2, \"id\": 4294967295}}"));

        assertThat(arguments[0].value()).isEqualTo(new NodeId(2, Unsigned.uint(4294967295L)));
    }

    @Test
    void aNumericNodeIdArgument_isRefusedOutsideTheUInt32Range() {
        // The two bounds a boundary test reaches for. Both were already refused before the range check was
        // added, because they survive the narrowing -- which is what made the gap below look covered.
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("node", NodeIds.NodeId)), json("{\"node\": {\"id\": 4294967296}}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class);
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("node", NodeIds.NodeId)), json("{\"node\": {\"id\": -1}}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class);
    }

    @Test
    void aNumericNodeIdArgument_isRefusedRatherThanWrappedWhenItIsWiderThan64Bits() {
        // The identifier is a UInt32 and the check has to happen on the value as written: longValue() answers
        // a wide JSON integer modulo 2^64, so 2^64 itself becomes 0 and satisfies every bound -- naming node
        // i=0 instead of refusing. The second value is 2^64 as a JavaScript transform re-renders it, which is
        // the realistic route to a number this size, and it wrapped to i=384.
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("node", NodeIds.NodeId)),
                        json("{\"node\": {\"namespaceIndex\": 2, \"id\": 18446744073709551616}}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class);
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("node", NodeIds.NodeId)),
                        json("{\"node\": {\"id\": 18446744073709552000}}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class);
    }

    @Test
    void aGuidNodeIdArgument_takesEveryUnambiguousSpelling() throws Exception {
        // This test used to assert the brace and URN spellings were REFUSED, on the reasoning that the only
        // identifiers to accept are the ones our read path emits. That narrowness was dropped with Guids: the
        // forms below are told apart by their shape, so accepting them cannot produce a wrong value, and it
        // saves transcribing what a vendor tool shows. What must still be refused is a spelling that would be
        // padded into a DIFFERENT Guid -- covered by aShortGuid_isRefusedWhereverItCanEnter.
        final String guid = "72962b91-fa75-4ae6-8d28-b404dc7daf63";
        final NodeId expected = new NodeId(2, UUID.fromString(guid));

        for (final String spelling :
                new String[] {guid, "{" + guid + "}", "(" + guid + ")", "urn:uuid:" + guid, guid.replace("-", "")}) {
            final var arguments = MethodArguments.forCall(
                    signature(argument("node", NodeIds.NodeId)),
                    json("{\"node\": {\"namespaceIndex\": 2, \"idType\": 2, \"id\": \"" + spelling + "\"}}"));
            assertThat(arguments[0].value()).describedAs(spelling).isEqualTo(expected);
        }
    }

    @Test
    void aNodeIdArgument_refusesAnIdentifierDisagreeingWithItsIdType() {
        // A kind that does not match the value is a caller error, not something to coerce: the whole reason
        // idType is honoured is that the JSON type of `id` cannot distinguish Guid and Opaque from String.
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("node", NodeIds.NodeId)),
                        json("{\"node\": {\"idType\": 0, \"id\": \"not-a-number\"}}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class);
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("node", NodeIds.NodeId)), json("{\"node\": {\"idType\": 1, \"id\": 9482}}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class);
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("node", NodeIds.NodeId)),
                        json("{\"node\": {\"idType\": 3, \"id\": \"not base64!\"}}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class);
        // An idType the specification does not define, refused by name rather than by a range check. The
        // switch is the only thing that knows which four exist, so the message says which they are -- an
        // earlier version bounded the value at 0..3 and let `default` carry Opaque, which would have read a
        // fifth value as Opaque rather than refusing it.
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("node", NodeIds.NodeId)), json("{\"node\": {\"idType\": 4, \"id\": 1}}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("0 (numeric), 1 (string), 2 (guid) or 3 (opaque)")
                .hasMessageContaining("got 4");
    }

    @Test
    void aNodeIdArgumentWithoutAnIdType_keepsTheNumberOrStringShorthand() throws Exception {
        // What a hand-written configuration carries, and all it can express.
        final var numeric = MethodArguments.forCall(
                signature(argument("node", NodeIds.NodeId)), json("{\"node\": {\"id\": 9482}}"));
        final var textual = MethodArguments.forCall(
                signature(argument("node", NodeIds.NodeId)), json("{\"node\": {\"id\": \"Boiler\"}}"));

        assertThat(numeric[0].value()).isEqualTo(new NodeId(0, Unsigned.uint(9482)));
        assertThat(textual[0].value()).isEqualTo(new NodeId(0, "Boiler"));
    }

    @Test
    void aStatusCodeArgument_acceptsTheNumericCode() throws Exception {
        // The symbol a response carries is looked up on the way out, not carried, so an input needs the
        // number alone.
        final var arguments =
                MethodArguments.forCall(signature(argument("status", NodeIds.StatusCode)), json("{\"status\": 0}"));

        assertThat(((StatusCode) arguments[0].value()).getValue()).isEqualTo(0L);
    }

    @Test
    void aQualifiedNameArgument_takesABareStringAsNamespaceZero() throws Exception {
        // Matching the read path, which omits namespaceIndex when it is zero.
        final var arguments = MethodArguments.forCall(
                signature(argument("name", NodeIds.QualifiedName)), json("{\"name\": \"Start\"}"));

        final QualifiedName qualified = (QualifiedName) arguments[0].value();
        assertThat(qualified.getName()).isEqualTo("Start");
        assertThat(qualified.getNamespaceIndex().intValue()).isEqualTo(0);
    }

    @Test
    void anXmlElementArgument_takesTheFragmentAsText() throws Exception {
        final var arguments = MethodArguments.forCall(
                signature(argument("doc", NodeIds.XmlElement)), json("{\"doc\": \"<a>1</a>\"}"));

        assertThat(((XmlElement) arguments[0].value()).getFragment()).isEqualTo("<a>1</a>");
    }

    @Test
    void aFloatOutsideFloatRange_isRefusedRatherThanSentAsInfinity() throws Exception {
        // The same hazard as the integral cases above, with a different symptom: a double beyond the float
        // range does not wrap, it saturates, so 1e39 would reach the device as +Infinity -- a value the caller
        // never asked for and cannot tell apart from a real one.
        assertThatThrownBy(
                        () -> MethodArguments.forCall(signature(argument("f", NodeIds.Float)), json("{\"f\": 1e39}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("infinity");

        assertThatThrownBy(
                        () -> MethodArguments.forCall(signature(argument("f", NodeIds.Float)), json("{\"f\": -1e39}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("infinity");
    }

    @Test
    void aFloatAtTheEdgeOfItsRange_isAccepted() throws Exception {
        // The boundary the refusal above must not overshoot: the largest value a Float can hold is a legal
        // argument, and 1e38 is an ordinary one.
        final var arguments = MethodArguments.forCall(signature(argument("f", NodeIds.Float)), json("{\"f\": 1e38}"));

        assertThat((float) arguments[0].value()).isEqualTo(1e38f);
    }

    @Test
    void valueWiderThanSixtyFourBits_isRefusedRatherThanWrappedIntoRange() throws Exception {
        // The decisive one, and the reason the check reads the value as a BigInteger. Jackson's longValue()
        // answers a JSON integer wider than 64 bits modulo 2^64 instead of refusing, so a range check performed
        // on its result validates a different number than the caller sent: 18446744073709551617 narrows to 1,
        // which satisfies every Int32 bound. Before this was fixed the device was started with a setpoint of 1.
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("setpoint", NodeIds.Int32)), json("{\"setpoint\": 18446744073709551617}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("18446744073709551617");

        // 2^64 itself narrows to 0, which is in range for every unsigned type.
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("count", NodeIds.UInt64)), json("{\"count\": 18446744073709551616}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("18446744073709551616");
    }

    @Test
    void aUInt64IsNotBoundedByTheSignedCeiling() throws Exception {
        // The counterpart refusal: bounding UInt64 by Long.MAX_VALUE refused the upper half of the type, and
        // narrowing made it total -- 2^64-1 arrived as -1 and failed the lower bound of zero. Nothing else checks
        // this type's range, because its bounds are BigIntegers the schema generator cannot carry, so a legitimate
        // value refused here is refused outright.
        //
        // The type's whole range is no longer reachable, but for a reason that has nothing to do with the type:
        // the magnitude limit below. What this still asserts is that the *type* bound is the unsigned one -- a
        // value is converted as an unsigned 64-bit number rather than measured against a signed ceiling.
        final Variant[] arguments = MethodArguments.forCall(
                signature(argument("count", NodeIds.UInt64)), json("{\"count\": 9007199254740991}"));

        assertThat(arguments[0].value()).hasToString("9007199254740991");
        assertThat(arguments[0].value()).isInstanceOf(ULong.class);
    }

    @Test
    void theValueARoundedNumberArrivesAs_isRefused() throws Exception {
        // The decisive case, and the one the first version of this guard let through. The southbound
        // field-mapping transform runs as JavaScript, whose wrapper parses the payload with JSON.parse before any
        // Edge code sees it, and JSON.parse rounds to nearest-even. So 9007199254740993 does not arrive here at
        // all -- it arrives as 9007199254740992, which is exactly 2^53.
        //
        // A guard that refuses only above 2^53 therefore never sees a corrupted value: it sees the rounding
        // *target* and accepts it. These two assertions feed the values that genuinely arrive.
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("ticks", NodeIds.Int64)), json("{\"ticks\": 9007199254740992}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("9007199254740992")
                .hasMessageContaining("2^53");

        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("ticks", NodeIds.Int64)), json("{\"ticks\": -9007199254740992}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("-9007199254740992");

        // The largest value no rounding can have produced, which is JavaScript's own MAX_SAFE_INTEGER. One below
        // the limit, and the boundary that has to keep working.
        final Variant[] belowTheLimit = MethodArguments.forCall(
                signature(argument("ticks", NodeIds.Int64)), json("{\"ticks\": 9007199254740991}"));

        assertThat(belowTheLimit[0].value()).isEqualTo(9007199254740991L);
    }

    @Test
    void aWholeNumberFarAboveTheLimitIsRefused() throws Exception {
        // Both 64-bit types, since only they have a range reaching past the limit.
        //
        // These two are exactly representable as doubles, so they cross the transform unchanged. They are still
        // refused, and that is the intended reading of the guard: above the limit a value cannot be *trusted*,
        // not proven corrupted. Which values survive by luck is a property of their bit pattern, and asking a
        // caller to know that is worse than one flat limit.
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("count", NodeIds.UInt64)), json("{\"count\": 18000000000000000}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("2^53");

        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("ticks", NodeIds.Int64)), json("{\"ticks\": -18000000000000000}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("2^53");
    }

    @Test
    void aNarrowIntegerTypeIsUnaffectedByTheMagnitudeLimit() throws Exception {
        // The limit can only bite on Int64 and UInt64; every narrower type's own range stops far below it, so
        // this records that the guard changed nothing for them.
        final Variant[] arguments = MethodArguments.forCall(
                signature(argument("setpoint", NodeIds.Int32)), json("{\"setpoint\": 2147483647}"));

        assertThat(arguments[0].value()).isEqualTo(2147483647);
    }

    @Test
    void aTimestampJavaCannotParse_isRefusedRatherThanEscaping() throws Exception {
        // The decisive one. A DateTime argument is converted with Instant.parse, which throws
        // DateTimeParseException -- a RuntimeException, but not an IllegalArgumentException, which is what this
        // conversion used to catch. So it escaped here, escaped `invoke` (which catches only
        // InvalidArgumentException), and surfaced inside a whenComplete consumer where nothing completes the
        // WritingOutput: the caller got no response and no refusal until the write timed out 30 seconds later.
        //
        // A timestamp without a zone offset is the case that matters. It does not look malformed -- it is how a
        // timestamp is usually written by hand -- and the generated schema passes it, because the schema states
        // `format: date-time` and format is an annotation unless a validator is configured to assert it.
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("at", NodeIds.DateTime)), json("{\"at\": \"2026-10-08T12:00:00\"}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("at")
                .hasMessageContaining("DateTime");

        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("at", NodeIds.DateTime)), json("{\"at\": \"not-a-timestamp\"}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("at");
    }

    @Test
    void aWellFormedTimestamp_isStillAccepted() throws Exception {
        // The counterpart: widening the catch must not turn a good value into a refusal.
        final Variant[] arguments = MethodArguments.forCall(
                signature(argument("at", NodeIds.DateTime)), json("{\"at\": \"2026-10-08T12:00:00Z\"}"));

        assertThat(arguments[0].value()).isInstanceOf(DateTime.class);
        assertThat(((DateTime) arguments[0].value()).getJavaInstant()).isEqualTo(Instant.parse("2026-10-08T12:00:00Z"));
    }

    @Test
    void wrongJsonShape_isRefusedNamingTheExpectedType() throws Exception {
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("setpoint", NodeIds.Int32)), json("{\"setpoint\": \"180\"}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("expects Int32");

        assertThatThrownBy(
                        () -> MethodArguments.forCall(signature(argument("on", NodeIds.Boolean)), json("{\"on\": 1}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("expects Boolean");
    }

    @Test
    void fractionalValueForAnIntegerArgument_isRefused() throws Exception {
        // 180.5 truncating to 180 would be a silently different setpoint.
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("setpoint", NodeIds.Int32)), json("{\"setpoint\": 180.5}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("whole number");
    }

    @Test
    void builtinScalarTypes_convertToTheirOpcUaForms() throws Exception {
        final MethodSignature signature = signature(
                argument("flag", NodeIds.Boolean),
                argument("count", NodeIds.Int32),
                argument("ratio", NodeIds.Double),
                argument("label", NodeIds.String));

        final Variant[] arguments = MethodArguments.forCall(
                signature, json("{\"flag\": true, \"count\": 42, \"ratio\": 1.5, \"label\": \"x\"}"));

        assertThat(arguments[0].value()).isEqualTo(true);
        assertThat(arguments[1].value()).isEqualTo(42);
        assertThat(arguments[2].value()).isEqualTo(1.5);
        assertThat(arguments[3].value()).isEqualTo("x");
    }

    @Test
    void customStructureArgument_isRefusedRatherThanGuessedAt() throws Exception {
        // The generated schema already says `any` for such an argument, so a caller has no contract to write
        // against. Encoding one needs the DataTypeTree machinery the value-write path uses.
        assertThatThrownBy(() -> MethodArguments.forCall(
                        signature(argument("recipe", new NodeId(3, 4001))), json("{\"recipe\": {\"steps\": 3}}")))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("cannot encode");
    }
}
