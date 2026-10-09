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

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Objects;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
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
                .containsExactlyInAnyOrder("Square", "Stop", "SumArray");
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
