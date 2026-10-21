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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.adapter.sdk.api.schema.SchemaJsonRepresentation;
import com.hivemq.edge.adapters.opcua.southbound.JsonToOpcUaValueConverter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.OpcUaDataType;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import util.EmbeddedOpcUaServerExtension;
import util.TestNamespace;

/// Reading an object's methods from a server, against the embedded one.
///
/// **Why this did not exist.** `MethodVerifier` had no test, and the obstacle was a fixture gap rather than a
/// missing capability: the embedded server already built method nodes with working invocation handlers, but
/// none of them **declared** its arguments. Those methods were written for the alarm tests, where OPC 10000-9
/// fixes the signatures and the handler finds its arguments by type -- so the metadata was never needed.
/// Reading a signature is exactly reading the `InputArguments` and `OutputArguments` properties, which left
/// the verifier untestable against a server.
///
/// {@code TestNamespace.addMethodObject} closes that: an object carrying three methods that declare their
/// arguments. Everything below goes over a real connection -- browse, read, resolve -- rather than standing in
/// for the address space.
class MethodVerifierIT {

    @RegisterExtension
    public final @NotNull EmbeddedOpcUaServerExtension opcUaServerExtension = new EmbeddedOpcUaServerExtension();

    private @NotNull OpcUaClient client;
    private @NotNull NodeId objectId;

    @BeforeEach
    void setUp() throws Exception {
        objectId = namespace().addMethodObject("Boiler");
        client = OpcUaClient.create(opcUaServerExtension.getServerUri());
        client.connect();
    }

    @AfterEach
    void tearDown() throws Exception {
        client.disconnect();
    }

    private @NotNull TestNamespace namespace() {
        return Objects.requireNonNull(opcUaServerExtension.getTestNamespace());
    }

    private @NotNull List<MethodSignature> verified() {
        final MethodVerifier.Result result =
                MethodVerifier.verify(client, objectId, "boiler-methods").join();
        assertThat(result).isInstanceOf(MethodVerifier.Result.Verified.class);
        return ((MethodVerifier.Result.Verified) result).methods();
    }

    private @NotNull MethodSignature verified(final @NotNull String name) {
        return verified().stream()
                .filter(method -> method.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no method named '" + name + "'"));
    }

    @Test
    void everyMethodTheObjectExposes_isFound() {
        assertThat(verified())
                .extracting(MethodSignature::name)
                .containsExactlyInAnyOrder("Square", "Stop", "SumArray", "Wait", "SetState");
    }

    @Test
    void anEnumerationArgument_isEncodedAsTheInt32ItIsCarriedAs() {
        // An enumeration's DataType reduces to Variant rather than to the Int32 it is carried as, which is why
        // the reduction alone cannot encode it. The converter recognises the enumeration from the server's type
        // hierarchy instead, and accepts the number, the name, and the object a structure publishes.
        // `ServerState` is the standard one: Running = 0, Failed = 1.
        final MethodSignature setState = verified("SetState");
        final JsonToOpcUaValueConverter converter = JsonToOpcUaValueConverter.of(client);

        assertThat(setState.builtinTypeOf(setState.inputArguments().get(0))).isEqualTo(OpcUaDataType.Variant);
        assertThat(
                        MethodArguments.forCall(setState, state(JsonNodeFactory.instance.numberNode(1)), converter)[0]
                                .value())
                .isEqualTo(1);
        assertThat(
                        MethodArguments.forCall(
                                setState, state(JsonNodeFactory.instance.textNode("Failed")), converter)[0]
                                .value())
                .isEqualTo(1);
        assertThat(MethodArguments.forCall(
                        setState,
                        state(JsonNodeFactory.instance
                                .objectNode()
                                .put("name", "Failed")
                                .put("value", 1)),
                        converter)[0]
                        .value())
                .isEqualTo(1);
    }

    @Test
    void anEnumerationArgument_refusesAValueTheEnumerationDoesNotDefine() {
        final MethodSignature setState = verified("SetState");
        final JsonToOpcUaValueConverter converter = JsonToOpcUaValueConverter.of(client);

        assertThatThrownBy(() ->
                        MethodArguments.forCall(setState, state(JsonNodeFactory.instance.numberNode(99)), converter))
                .isInstanceOf(MethodArguments.InvalidArgumentException.class)
                .hasMessageContaining("expects ServerState")
                .hasMessageContaining("defines no value 99");
        assertThatThrownBy(() -> MethodArguments.forCall(
                        setState, state(JsonNodeFactory.instance.textNode("Exploded")), converter))
                .hasMessageContaining("has no value named 'Exploded'");
        // Both fields, disagreeing: refused rather than resolved in favour of either.
        assertThatThrownBy(() -> MethodArguments.forCall(
                        setState,
                        state(JsonNodeFactory.instance
                                .objectNode()
                                .put("name", "Running")
                                .put("value", 1)),
                        converter))
                .hasMessageContaining("gives to different values");
    }

    @Test
    void anEnumerationArgument_withoutTheServersTypeHierarchy_isRefused() {
        // The builtin-only converter has no hierarchy to recognise an enumeration in, so it refuses rather than
        // guessing that a Variant-reducing type is carried as an Int32.
        final MethodSignature setState = verified("SetState");

        assertThatThrownBy(() -> MethodArguments.forCall(setState, state(JsonNodeFactory.instance.numberNode(1))))
                .hasMessageContaining("cannot encode");
    }

    private static @NotNull ObjectNode state(final @NotNull JsonNode value) {
        final ObjectNode request = JsonNodeFactory.instance.objectNode();
        request.set("state", value);
        return request;
    }

    @Test
    void anArgumentTypedAsASimpleDataType_reducesToItsBuiltinSupertype() {
        // `Duration` is a subtype of Double, and OPC 10000-3 section 5.8.2 says such a Simple DataType is
        // "handled on the wire like the Built-in DataType, i.e. they cannot be distinguished on the wire from
        // their Built-in supertypes" -- Duration is the specification's own example. Only the server's DataType
        // hierarchy says so: the id is not one of the 25 builtins, so the table that discovery used to consult
        // answered null and the argument was refused outright (EDG-1087 M6).
        final MethodSignature wait = verified("Wait");
        final Argument timeout = wait.inputArguments().get(0);

        assertThat(timeout.getDataType()).isEqualTo(NodeIds.Duration);
        assertThat(wait.builtinTypeOf(timeout)).isEqualTo(OpcUaDataType.Double);
        assertThat(wait.builtinTypeOf(wait.outputArguments().get(0))).isEqualTo(OpcUaDataType.Double);
    }

    @Test
    void anUnreadableTypeHierarchy_costsOnlyTheTypesThatNeededIt() throws Exception {
        // The defect this pins: the DataType hierarchy was read before anything was resolved, and a failure
        // returned an empty map -- so one unreadable hierarchy discarded the types of every argument,
        // including the ones answerable from the local table of the 25 builtins with no server at all. An
        // `Int32` input became unencodable, the signature was nevertheless reported fully verified, and the
        // cache kept it, leaving a method taking a plain integer uncallable until the adapter restarted.
        //
        // A spy on the real connected client, so the browse and the argument reads are genuine and only the
        // hierarchy read fails -- which is the shape of the real fault: a session that answers browses while
        // the type tree cannot be built.
        final OpcUaClient treeless = spy(client);
        doThrow(new UaException(StatusCodes.Bad_SessionClosed)).when(treeless).getDataTypeTree();

        final MethodVerifier.Result result =
                MethodVerifier.verify(treeless, objectId, "boiler-methods").join();

        assertThat(result).isInstanceOf(MethodVerifier.Result.Verified.class);
        final List<MethodSignature> methods = ((MethodVerifier.Result.Verified) result).methods();

        // `Square` takes an Int32, which is a builtin: its type is known without the server and survives.
        final MethodSignature square = methods.stream()
                .filter(method -> method.name().equals("Square"))
                .findFirst()
                .orElseThrow();
        assertThat(square.builtinTypeOf(square.inputArguments().get(0)))
                .withFailMessage("a builtin argument must not lose its type to an unreadable hierarchy")
                .isEqualTo(OpcUaDataType.Int32);

        // `Wait` takes a Duration, a subtype only the hierarchy can reduce: unresolved is the honest answer.
        final MethodSignature wait = methods.stream()
                .filter(method -> method.name().equals("Wait"))
                .findFirst()
                .orElseThrow();
        assertThat(wait.builtinTypeOf(wait.inputArguments().get(0)))
                .withFailMessage("a Simple DataType cannot be reduced without the hierarchy")
                .isNull();

        // And the result must say the lookup did not answer, so nothing remembers it.
        //
        // Four states, and the one here is the fourth: (1) the type is a builtin, answered from the local
        // table; (2) the hierarchy reduces it to a builtin; (3) the hierarchy says it reduces to none, which
        // means a structure this release does not encode and is durable; (4) the hierarchy could not be read,
        // which says nothing about the method. Until it is read we cannot even tell an alias for a Double from
        // a structure, so refusing the argument permanently is a conclusion state 4 does not support.
        //
        // States 3 and 4 both leave the type absent and used to be indistinguishable, so the signature was
        // reported fully verified either way and the cache kept it -- one failed read made a callable method
        // uncallable until the adapter restarted.
        assertThat(wait.typesResolved())
                .withFailMessage("an unanswered DataType lookup must not be recorded as a resolved one")
                .isFalse();
        assertThat(((MethodVerifier.Result.Verified) result).complete())
                .withFailMessage("a result with an unanswered DataType lookup must be incomplete, so it is not cached")
                .isFalse();
    }

    @Test
    void aHierarchyThatAnswers_leavesTheResultCacheable() {
        // The counterpart, and the reason the fix is not "treat a missing type as incomplete". With the
        // hierarchy readable every lookup answers -- including an enumeration's, which answers `Variant` and
        // is then refused at conversion, and a structure's, which answers nothing at all. Both are durable:
        // asking again would learn the same thing, so the result stays complete and the cache keeps it.
        //
        // Only state 4 is incomplete: the hierarchy could not be read, so we do not know what the type is.
        final MethodVerifier.Result result =
                MethodVerifier.verify(client, objectId, "boiler-methods").join();

        assertThat(result).isInstanceOf(MethodVerifier.Result.Verified.class);
        final MethodVerifier.Result.Verified verified = (MethodVerifier.Result.Verified) result;

        assertThat(verified.methods()).allSatisfy(method -> assertThat(method.typesResolved())
                .withFailMessage("the hierarchy answered for '%s', so its lookups are resolved", method.name())
                .isTrue());
        assertThat(verified.complete())
                .withFailMessage("a result whose lookups all answered must stay cacheable")
                .isTrue();
    }

    @Test
    void anArgumentTypedAsASimpleDataType_isEncodedRatherThanRefused() {
        // What the reviewer actually hit: a method taking a timeout could not be called at all.
        final MethodSignature wait = verified("Wait");

        final ObjectNode request = JsonNodeFactory.instance.objectNode().put("timeout", 2500);
        final Variant[] arguments = MethodArguments.forCall(wait, request);

        assertThat(arguments).hasSize(1);
        assertThat(arguments[0].getValue()).isEqualTo(2500.0);
    }

    @Test
    void aSimpleDataTypeArgumentIsStatedInTheSchema_ratherThanRenderedAsAny() {
        // The second half of the same defect: an unresolved type renders as `any` -- an empty subschema -- so
        // even a caller who could somehow get the call through had no contract to write against.
        final JsonNode timeout = SchemaJsonRepresentation.INSTANCE
                .toJsonSchemaDocument(MethodSchemas.requestSchema(verified()))
                .get("properties")
                .get("timeout");

        assertThat(timeout).isNotNull();
        assertThat(timeout.get("type").asText()).isEqualTo("number");
    }

    @Test
    void aMethodsArgumentsAreReadWithTheirNamesAndOrder() {
        // The order is the mapping a request needs: a caller names its arguments, the wire passes them by
        // position, and the signature is what translates between the two.
        final MethodSignature square = verified("Square");

        assertThat(square.inputArgumentNames()).containsExactly("input1");
        assertThat(square.outputArgumentNames()).containsExactly("output1");
        assertThat(square.inputArguments().get(0).getDataType()).isEqualTo(NodeIds.Int32);
        assertThat(square.inputPosition("input1")).isZero();
        assertThat(square.inputPosition("nosuch")).isNull();
    }

    @Test
    void aMethodDeclaringNoArguments_isVerifiedRatherThanRejected() {
        // The decisive case, and the one only a server can settle. OPC 10000-3 section 5.7 makes both argument
        // properties optional and says an absent property means the method has none -- so `Stop` carries no
        // property at all, which is how an argument-less method is normally modelled. The server answers
        // Bad_NotFound for the read, and that must count as an answer about the method rather than a failure:
        // treated as a failure it would reject the whole tag, taking `Square` and `SumArray` with it.
        final MethodSignature stop = verified("Stop");

        assertThat(stop.inputArguments()).isEmpty();
        assertThat(stop.outputArguments()).isEmpty();
        assertThat(stop.declaresNoInputs()).isTrue();
    }

    @Test
    void anArrayArgumentKeepsItsRankAndDimensions() {
        // What a generated schema's nesting depth is built from, read off the wire rather than assumed.
        final MethodSignature sum = verified("SumArray");

        assertThat(sum.inputArguments().get(0).getValueRank()).isEqualTo(1);
        assertThat(sum.inputArguments().get(0).getArrayDimensions()).isNotNull();
    }

    @Test
    void aMethodsNodeIdIsTheOneTheServerAssigned() {
        // A user method's node id exists only on the server, which is why browsing is the only way to learn it
        // and why an unverified method tag is unusable rather than merely undescribed.
        final MethodSignature square = verified("Square");

        assertThat(square.nodeId().getNamespaceIndex()).isEqualTo(namespace().getNamespaceIndex());
        assertThat(square.nodeId()).isNotEqualTo(objectId);
    }

    @Test
    void theObjectToCallOnIsTheTagsOwnNode_forAMethodTheObjectExposes() {
        // The object id travels with the signature because a method found only on the object's type is invoked
        // by naming that type. For everything the object itself exposes it must stay the tag's own node.
        assertThat(verified())
                .allSatisfy(method -> assertThat(method.objectId()).isEqualTo(objectId));
    }

    @Test
    void aTypeReportingNoMethodsWithBadNodeIdUnknown_isReadAsAnEmptyLevelAndStaysCacheable() throws Exception {
        // The Siemens S7-1500's answer, which cost every call a fresh browse of the whole hierarchy: about
        // 275 verifications for 270 calls. A level with no methods is the ordinary case for most of a
        // hierarchy and servers report it with an empty browse result; the S7 reports it with
        // Bad_NodeIdUnknown, which read as a failure to reach the device -- so the climb was incomplete,
        // MethodCache refused to remember an incomplete result, and the next call did it all again.
        //
        // What makes the lenient reading safe is what the status means: Bad_NodeIdUnknown is the server's
        // verdict about its own address space, which it does not give transiently, and the methods to take
        // from such a level are none whether it declares none or is genuinely absent.
        //
        // The spy fails only the method browse of a type node, leaving the object's own browse and every
        // supertype browse genuine -- the shape of the real fault.
        final NodeId object = namespace().addObjectWithTypeDeclaredMethods("Boiler9");
        final OpcUaClient quirky = spy(client);
        doAnswer(invocation -> {
                    final BrowseDescription browse = invocation.getArgument(0);
                    final boolean methodBrowseOfAType = browse.getBrowseDirection() == BrowseDirection.Forward
                            && !browse.getNodeId().equals(object);
                    if (methodBrowseOfAType) {
                        // A BrowseResult carrying a bad status, which is how a server actually answers -- not a
                        // failed service call. Browsing.browseAll attaches the status only from the result, so a
                        // failed call would lose it and the lenient reading would never see Bad_NodeIdUnknown.
                        return CompletableFuture.completedFuture(new BrowseResult(
                                new StatusCode(StatusCodes.Bad_NodeIdUnknown), null, new ReferenceDescription[0]));
                    }
                    return invocation.callRealMethod();
                })
                .when(quirky)
                .browseAsync(any(BrowseDescription.class));

        final MethodVerifier.Result result =
                MethodVerifier.verify(quirky, object, "boiler9-methods").join();

        assertThat(result).isInstanceOf(MethodVerifier.Result.Verified.class);
        final MethodVerifier.Result.Verified verified = (MethodVerifier.Result.Verified) result;

        // The decisive assertion: cacheable. Without it every call re-browses.
        assertThat(verified.complete())
                .withFailMessage(
                        "a level reporting no methods is not a failure to reach the device, so the climb "
                                + "must stay cacheable: %s",
                        verified)
                .isTrue();
    }

    @Test
    void aSupertypeRefusedAsUnknown_isAVerdictAndStaysCacheable() throws Exception {
        // What the S7-1500 actually does, and what the first fix missed: it refuses the *supertype* browse of
        // a type node too, not only the method browse. The first fix read a refused supertype browse as a
        // failure to reach the device, so the climb was incomplete, nothing was cached, and the S7 still paid
        // for a full hierarchy browse on every call -- 180 calls, 360 verifications.
        //
        // Bad_NodeIdUnknown is the server answering about its own address space, whichever browse carries it,
        // and a server does not say it transiently. So: no supertype to climb to, this level's methods are
        // taken as found, and the result is remembered.
        //
        // Reported by Miguel as Q06 in round 6 on EDG-1087.
        final NodeId object = namespace().addObjectWithTypeDeclaredMethods("Boiler12");
        final OpcUaClient refusing = spy(client);
        doAnswer(invocation -> {
                    final BrowseDescription browse = invocation.getArgument(0);
                    if (browse.getBrowseDirection() == BrowseDirection.Inverse) {
                        // A BrowseResult with a bad status, as a server answers -- not a failed service call,
                        // which would lose the status before anything could read it.
                        return CompletableFuture.completedFuture(new BrowseResult(
                                new StatusCode(StatusCodes.Bad_NodeIdUnknown), null, new ReferenceDescription[0]));
                    }
                    return invocation.callRealMethod();
                })
                .when(refusing)
                .browseAsync(any(BrowseDescription.class));

        final MethodVerifier.Result result =
                MethodVerifier.verify(refusing, object, "boiler12-methods").join();

        assertThat(result).isInstanceOf(MethodVerifier.Result.Verified.class);
        final MethodVerifier.Result.Verified verified = (MethodVerifier.Result.Verified) result;

        assertThat(verified.complete())
                .withFailMessage(
                        "a supertype refused with Bad_NodeIdUnknown is the server's verdict, not an outage, so "
                                + "the result must be cacheable: %s",
                        verified)
                .isTrue();

        // The methods of the level whose supertype was refused are still used: the browse was attempted
        // rather than skipped, which is the second half of the fix.
        assertThat(verified.methods())
                .withFailMessage(
                        "the methods of a level are read whatever its supertype browse said: %s",
                        verified.methods().stream().map(MethodSignature::name).toList())
                .isNotEmpty();
    }

    @Test
    void aFailureAboveAReadableType_keepsTheMethodsThatTypeDeclared() throws Exception {
        // A static method lives only on the type, so losing a readable type level loses the method entirely:
        // it disappears from resolution and from both generated schemas, and naming its full node id cannot
        // recover it because the node-id lookup searches the same list.
        //
        // The recovery used to sit outside the recursion, where only the instance's own result was in scope,
        // so a browse failing *above* a type that had answered returned the object's methods alone. The
        // javadoc on addInheritedMethods promised the methods already found would be kept.
        //
        // The spy lets the object's type be read and fails the browse one level further up, which is the
        // shape of the real fault -- a server that restricts part of its type hierarchy.
        final NodeId object = namespace().addObjectWithTypeDeclaredMethods("Boiler11");
        // The type this object derives from, read from a healthy run: the node that declares StaticOp.
        final NodeId objectType = ((MethodVerifier.Result.Verified)
                        MethodVerifier.verify(client, object, "boiler11-probe").join())
                .methods().stream()
                        .filter(method -> method.name().equals("StaticOp"))
                        .findFirst()
                        .orElseThrow()
                        .objectId();

        final OpcUaClient partial = spy(client);
        doAnswer(invocation -> {
                    final BrowseDescription browse = invocation.getArgument(0);
                    // Everything about the object and its own type is real, so StaticOp is genuinely discovered.
                    // Only the climb *above* that type fails -- selected by node rather than by call order, because
                    // several of these browses are forward ones and counting them picks the wrong level.
                    if (!browse.getNodeId().equals(object)
                            && !browse.getNodeId().equals(objectType)) {
                        return CompletableFuture.failedFuture(new UaException(StatusCodes.Bad_Timeout));
                    }
                    return invocation.callRealMethod();
                })
                .when(partial)
                .browseAsync(any(BrowseDescription.class));

        final MethodVerifier.Result result =
                MethodVerifier.verify(partial, object, "boiler11-methods").join();

        assertThat(result).isInstanceOf(MethodVerifier.Result.Verified.class);
        final MethodVerifier.Result.Verified verified = (MethodVerifier.Result.Verified) result;

        assertThat(verified.methods())
                .withFailMessage(
                        "a method read from a readable type level must survive a failure above it: %s",
                        verified.methods().stream().map(MethodSignature::name).toList())
                .anySatisfy(method -> assertThat(method.name()).isEqualTo("StaticOp"));

        // And the result is still not cacheable, because the hierarchy was not read in full.
        assertThat(verified.complete())
                .withFailMessage("an unread level above means the list may be short, so nothing may be cached")
                .isFalse();
    }

    @Test
    void aTypeBrowseThatTrulyFails_stillLeavesTheClimbIncomplete() throws Exception {
        // The other side, and the reason the rule is about the *status* rather than about which browse failed.
        // A timeout carries no verdict: we do not know what is above, so methods may be hidden and the result
        // must not be remembered. Contrast aSupertypeRefusedAsUnknown_isAVerdictAndStaysCacheable.
        final NodeId object = namespace().addObjectWithTypeDeclaredMethods("Boiler10");
        final OpcUaClient broken = spy(client);
        doAnswer(invocation -> {
                    final BrowseDescription browse = invocation.getArgument(0);
                    if (browse.getBrowseDirection() == BrowseDirection.Inverse) {
                        return CompletableFuture.failedFuture(new UaException(StatusCodes.Bad_Timeout));
                    }
                    return invocation.callRealMethod();
                })
                .when(broken)
                .browseAsync(any(BrowseDescription.class));

        final MethodVerifier.Result result =
                MethodVerifier.verify(broken, object, "boiler10-methods").join();

        assertThat(result).isInstanceOf(MethodVerifier.Result.Verified.class);
        assertThat(((MethodVerifier.Result.Verified) result).complete())
                .withFailMessage("an unreachable supertype leaves the hierarchy unknown, so nothing may be cached")
                .isFalse();
    }

    @Test
    void aTypeWhoseSupertypeBrowseFails_isStillBrowsedForItsOwnMethods() throws Exception {
        // A level's own methods do not depend on its parent. The browse reorder that fixed the accumulated-list
        // recovery introduced a level-skip: a type whose *supertype* browse failed returned before its method
        // browse ran, so a static method declared on that very type disappeared -- from resolution and from both
        // generated schemas, and naming its node id could not recover it because the node-id lookup searches the
        // same list.
        //
        // The shape here is the one that exposes it: the object's own browses are genuine, and only the inverse
        // (supertype) browse of its type fails, with a timeout rather than Bad_NodeIdUnknown so it carries no
        // verdict. StaticOp is declared on that type and must still be found.
        //
        // Distinct from the three neighbouring tests, which is why it is its own:
        //   - aSupertypeRefusedAsUnknown_isAVerdictAndStaysCacheable  -- same browse, but a verdict status, and
        //     its subject is cacheability;
        //   - aFailureAboveAReadableType_keepsTheMethodsThatTypeDeclared -- failure one level *higher*, after
        //     the type had answered;
        //   - aTypeBrowseThatTrulyFails_stillLeavesTheClimbIncomplete -- same fault, but it asserts only that
        //     nothing is cached and never looks at what was found.
        //
        // Reported by Sam as V08 in review 05, against a head where the early return was still present.
        final NodeId object = namespace().addObjectWithTypeDeclaredMethods("Boiler13");
        final OpcUaClient failingAbove = spy(client);
        doAnswer(invocation -> {
                    final BrowseDescription browse = invocation.getArgument(0);
                    final boolean supertypeBrowseOfTheType = browse.getBrowseDirection() == BrowseDirection.Inverse
                            && !browse.getNodeId().equals(object);
                    if (supertypeBrowseOfTheType) {
                        return CompletableFuture.failedFuture(new UaException(StatusCodes.Bad_Timeout));
                    }
                    return invocation.callRealMethod();
                })
                .when(failingAbove)
                .browseAsync(any(BrowseDescription.class));

        final MethodVerifier.Result result =
                MethodVerifier.verify(failingAbove, object, "boiler13-methods").join();

        assertThat(result).isInstanceOf(MethodVerifier.Result.Verified.class);
        final MethodVerifier.Result.Verified verified = (MethodVerifier.Result.Verified) result;

        // The decisive assertion: the type was browsed for methods even though its parent could not be read.
        assertThat(verified.methods())
                .withFailMessage(
                        "a parent relationship is not a prerequisite for reading a node's own methods, so the "
                                + "type's StaticOp must be found: %s",
                        verified.methods().stream().map(MethodSignature::name).toList())
                .anySatisfy(method -> assertThat(method.name()).isEqualTo("StaticOp"));

        // And still not cacheable: a timeout says nothing about what sits above, so methods may be missing.
        assertThat(verified.complete())
                .withFailMessage("a supertype that timed out leaves the hierarchy unknown, so nothing is cached")
                .isFalse();
    }

    @Test
    void aStaticTypeMethodIsCalledOnTheType_aDeclarationOnTheObject() {
        // EDG-1087 M7, against a live address space. The type carries two method nodes that a browse cannot
        // tell apart without reading their modelling rule: StaticOp has none, so it exists only on the type and
        // OPC 10000-4's Call names the type as its object; OptionalOp is declared Optional, which per OPC
        // 10000-3 section 6.4.4.4.4 means this instance may simply not implement it -- so it describes a method
        // of the object, and naming the type would run something against the class instead. That is what
        // happened: a caller asking about one boiler had the method executed on BoilerType.
        final NodeId object = namespace().addObjectWithTypeDeclaredMethods("Kettle");
        final MethodVerifier.Result result =
                MethodVerifier.verify(client, object, "kettle-methods").join();

        assertThat(result).isInstanceOf(MethodVerifier.Result.Verified.class);
        final List<MethodSignature> methods = ((MethodVerifier.Result.Verified) result).methods();

        final MethodSignature staticOp = methods.stream()
                .filter(method -> method.name().equals("StaticOp"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the static type method must still be callable: " + methods));
        assertThat(staticOp.origin()).isEqualTo(MethodSignature.Origin.TYPE_STATIC);
        assertThat(staticOp.objectId())
                .as("a method that exists only on the type is invoked by naming the type")
                .isNotEqualTo(object);

        final MethodSignature optionalOp = methods.stream()
                .filter(method -> method.name().equals("OptionalOp"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("an Optional declaration is still offered: " + methods));
        assertThat(optionalOp.origin()).isEqualTo(MethodSignature.Origin.TYPE_DECLARATION);
        assertThat(optionalOp.objectId())
                .as("a declaration describes a method of the instance, so the call names the object")
                .isEqualTo(object);
    }

    @Test
    void theObjectsOwnMethodIsUnaffectedByItsTypesDeclarations() {
        final NodeId object = namespace().addObjectWithTypeDeclaredMethods("Kettle");
        final MethodVerifier.Result result =
                MethodVerifier.verify(client, object, "kettle-methods").join();

        final MethodSignature square = ((MethodVerifier.Result.Verified) result)
                .methods().stream()
                        .filter(method -> method.name().equals("Square"))
                        .findFirst()
                        .orElseThrow();

        assertThat(square.origin()).isEqualTo(MethodSignature.Origin.OBJECT);
        assertThat(square.objectId()).isEqualTo(object);
    }

    @Test
    void anObjectExposingNoMethods_isVerifiedWithNoneRatherThanRejected() {
        // EDG-1100 P12's second half. An object may legitimately expose no methods, and more may be added
        // later without the tag changing -- so this is Verified-with-nothing rather than a rejection. What was
        // missing is that Edge said so only at DEBUG: the adapter stayed connected, the generated schema
        // admitted no arguments, and every call was quarantined by the validation gate, so an operator saw a
        // configured tag, a connected device and calls that vanished. MethodVerifier now logs a WARN naming
        // the node; the outcome asserted here is what that warning accompanies.
        final NodeId bare = namespace().addBareObject("EmptyBoiler");

        final MethodVerifier.Result result =
                MethodVerifier.verify(client, bare, "empty-methods").join();

        assertThat(result).isInstanceOf(MethodVerifier.Result.Verified.class);
        assertThat(((MethodVerifier.Result.Verified) result).methods()).isEmpty();
    }

    @Test
    void aTagPointingAtAVariableIsRejected_namingWhatItFound() {
        // Not "an object with no methods", which is what browsing alone would have reported: a usable tag that
        // can never serve a request, and a mistake the operator never hears about.
        final NodeId variable = NodeId.parse(namespace().addNode("aVariable", NodeIds.Int32, () -> 1, 9_001L));

        final MethodVerifier.Result result =
                MethodVerifier.verify(client, variable, "boiler-methods").join();

        assertThat(result).isInstanceOf(MethodVerifier.Result.Rejected.class);
        assertThat(((MethodVerifier.Result.Rejected) result).reason())
                .contains("not an Object")
                .contains("boiler-methods");
    }

    @Test
    void aTagPointingAtANodeTheServerDoesNotHave_isRejected() {
        final NodeId missing = new NodeId(namespace().getNamespaceIndex(), "NoSuchObject");

        final MethodVerifier.Result result =
                MethodVerifier.verify(client, missing, "boiler-methods").join();

        assertThat(result).isInstanceOf(MethodVerifier.Result.Rejected.class);
    }

    @Test
    void anUnadornedNameResolvesAgainstWhatWasRead() {
        // Joins the read to the call: the resolution a request goes through, over the signatures this browse
        // produced rather than over hand-built ones.
        final MethodReference.Resolution resolution =
                MethodReference.resolve(verified(), "Square", client.getNamespaceTable());

        assertThat(resolution).isInstanceOf(MethodReference.Resolution.Matched.class);
        assertThat(((MethodReference.Resolution.Matched) resolution).method().name())
                .isEqualTo("Square");
    }

    @Test
    void aQualifiedNodeIdResolvesToTheSameMethod() {
        // The qualified spelling, with its URI resolved against this server's live namespace table -- the
        // table being the thing a unit test has to invent.
        final List<MethodSignature> methods = verified();
        final MethodSignature square = methods.stream()
                .filter(method -> method.name().equals("Square"))
                .findFirst()
                .orElseThrow();
        final String qualified = "nsu=" + EmbeddedOpcUaServerExtension.NS_URI + ";s="
                + square.nodeId().getIdentifier();

        final MethodReference.Resolution resolution =
                MethodReference.resolve(methods, qualified, client.getNamespaceTable());

        assertThat(resolution).isInstanceOf(MethodReference.Resolution.Matched.class);
        assertThat(((MethodReference.Resolution.Matched) resolution).method().nodeId())
                .isEqualTo(square.nodeId());
    }

    @Test
    void aNameTheObjectDoesNotHave_isRefusedWithWhatItDoesHave() {
        final MethodReference.Resolution resolution =
                MethodReference.resolve(verified(), "Start", client.getNamespaceTable());

        assertThat(resolution).isInstanceOf(MethodReference.Resolution.NoSuchMethod.class);
        assertThat(((MethodReference.Resolution.NoSuchMethod) resolution).reason())
                .contains("Square")
                .contains("Stop")
                .contains("SumArray");
    }

    @Test
    void aRequestsArgumentsAreOrderedByTheSignatureThatWasRead() {
        // The last link, and the one that fails quietly when it is wrong: a method called with the right values
        // in the wrong positions returns a plausible answer. Checked here against a signature read from the
        // server rather than one written by hand.
        final MethodSignature square = verified("Square");
        final ObjectNode value = JsonNodeFactory.instance.objectNode().put("input1", 7);

        final Variant[] arguments = MethodArguments.forCall(square, value);

        assertThat(arguments).hasSize(1);
        assertThat(arguments[0].getValue()).isEqualTo(7);
    }

    @Test
    void anArrayArgumentIsBuiltToTheDeclaredType() {
        final MethodSignature sum = verified("SumArray");
        final ObjectNode value = JsonNodeFactory.instance.objectNode();
        value.putArray("values").add(1).add(2).add(3);

        final Variant[] arguments = MethodArguments.forCall(sum, value);

        assertThat(arguments).hasSize(1);
        assertThat(arguments[0].getValue()).isEqualTo(new Integer[] {1, 2, 3});
    }
}
