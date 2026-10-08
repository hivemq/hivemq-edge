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

import java.util.List;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/// What the name-to-position translation guarantees.
///
/// A request names its arguments; a `Call` passes them positionally. Everything here defends that translation,
/// because getting it wrong does not fail -- it calls the method with the arguments in the wrong order, which
/// the server accepts and the device acts on.
class MethodSignatureTest {

    private static final @NotNull NodeId METHOD_NODE = new NodeId(2, "Boiler1.Start");

    /// The object a `Call` names. Distinct from the method node here, which is the ordinary case: a method is a
    /// component of its object rather than the object itself.
    private static final @NotNull NodeId OBJECT_NODE = new NodeId(2, "Boiler1");

    private static @NotNull Argument argument(final @NotNull String name) {
        return new Argument(name, NodeIds.Int32, -1, null, LocalizedText.NULL_VALUE);
    }

    private static @NotNull MethodSignature signature(final @NotNull String... inputNames) {
        return new MethodSignature(
                "Start",
                METHOD_NODE,
                OBJECT_NODE,
                List.of(inputNames).stream().map(MethodSignatureTest::argument).toList(),
                List.of());
    }

    @Test
    void inputPosition_namesInDeclaredOrder_returnTheirIndex() {
        final MethodSignature signature = signature("setpoint", "ramp", "mode");

        assertThat(signature.inputPosition("setpoint")).isZero();
        assertThat(signature.inputPosition("ramp")).isEqualTo(1);
        assertThat(signature.inputPosition("mode")).isEqualTo(2);
    }

    @Test
    void inputPosition_unknownName_returnsNull() {
        // The check that turns a server-side Bad_InvalidArgument into a message naming the offending field.
        assertThat(signature("setpoint").inputPosition("setPoint")).isNull();
        assertThat(signature("setpoint").inputPosition("")).isNull();
    }

    @Test
    void inputPosition_isCaseSensitive() {
        // OPC UA argument names are case-sensitive, so matching loosely would silently bind a caller's
        // "Setpoint" to an argument named "setpoint" and pass it in a position the device reads differently.
        final MethodSignature signature = signature("setpoint");

        assertThat(signature.inputPosition("setpoint")).isZero();
        assertThat(signature.inputPosition("Setpoint")).isNull();
        assertThat(signature.inputPosition("SETPOINT")).isNull();
    }

    @Test
    void inputPosition_duplicateNames_resolvesToTheFirst() {
        // A badly-described server may declare two arguments with one name. Keeping a list rather than a map
        // means neither is discarded; this pins which one a request binds to, so the behaviour is stated
        // rather than incidental.
        final MethodSignature signature = signature("value", "value");

        assertThat(signature.inputPosition("value")).isZero();
        assertThat(signature.inputArgumentNames()).containsExactly("value", "value");
    }

    @Test
    void argumentNames_areInWireOrder() {
        final MethodSignature signature = new MethodSignature(
                "Start",
                METHOD_NODE,
                OBJECT_NODE,
                List.of(argument("setpoint"), argument("ramp")),
                List.of(argument("started"), argument("actualSetpoint")));

        assertThat(signature.inputArgumentNames()).containsExactly("setpoint", "ramp");
        assertThat(signature.outputArgumentNames()).containsExactly("started", "actualSetpoint");
    }

    @Test
    void declaresNoInputs_cannotDistinguishAbsentFromEmpty() {
        // A method genuinely taking no arguments and one whose InputArguments property the server omits both
        // arrive here as empty, because that is what Milo reports for each. Both are callable; the difference
        // shows only in how much a generated schema can say. Pinned so nobody later reads an empty list as
        // "the server failed to describe this".
        assertThat(signature().declaresNoInputs()).isTrue();
        assertThat(signature("setpoint").declaresNoInputs()).isFalse();
    }

    @Test
    void argumentLists_areDefensivelyCopied() {
        final List<Argument> inputs = new java.util.ArrayList<>(List.of(argument("setpoint")));
        final MethodSignature signature = new MethodSignature("Start", METHOD_NODE, OBJECT_NODE, inputs, List.of());

        inputs.add(argument("sneaked"));

        // The signature is cached for the adapter's lifetime and read on every call, so a caller mutating the
        // list it passed would change which position an argument binds to long after verification.
        assertThat(signature.inputArgumentNames()).containsExactly("setpoint");
        assertThatThrownBy(() -> signature.inputArguments().add(argument("also")))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
