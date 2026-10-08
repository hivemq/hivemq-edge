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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.OpcUaDataType;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

/// What the union of several methods' arguments states, and what it deliberately does not.
///
/// The schema a method tag publishes has to describe requests for every method of its object at once. These pin
/// the rule that makes that honest: every type stated is true, and where two methods disagree the union says
/// nothing rather than something false.
class MethodSchemasUnionTest {

    private static final @NotNull NodeId NODE = new NodeId(2, "Boiler1.M");

    private static @NotNull Argument argument(final @NotNull String name, final @NotNull NodeId dataType) {
        return new Argument(name, dataType, -1, null, LocalizedText.NULL_VALUE);
    }

    private static @NotNull MethodSignature method(final @NotNull String name, final @NotNull Argument... inputs) {
        return new MethodSignature(name, NODE, NODE, List.of(inputs), List.of());
    }

    /// The union projected back to name -> element type, which is what these tests are about.
    ///
    /// The union carries each argument's shape -- element type *and* array dimensions -- because an array
    /// argument and a scalar of the same element type are different things and the schema has to say which. These
    /// tests predate that and all build scalars, so projecting away the dimensions keeps every assertion below
    /// meaning exactly what it did. Array shapes are covered in {@link MethodSchemasArrayTest}.
    private static @NotNull Map<String, @Nullable OpcUaDataType> typesOf(
            final @NotNull Map<String, MethodSchemas.ArgumentShape> union) {

        final Map<String, OpcUaDataType> types = new LinkedHashMap<>();
        union.forEach((name, shape) -> types.put(name, shape.type()));
        return types;
    }

    @Test
    void oneMethod_statesEachArgumentsType() {
        final Map<String, OpcUaDataType> union = typesOf(MethodSchemas.unionOfInputs(
                List.of(method("Start", argument("setpoint", NodeIds.Int32), argument("ramp", NodeIds.String)))));

        assertThat(union)
                .containsExactly(Map.entry("setpoint", OpcUaDataType.Int32), Map.entry("ramp", OpcUaDataType.String));
    }

    @Test
    void severalMethodsAgreeing_keepTheSharedType() {
        // The common case: two methods both take a `setpoint` Int32. Stating Int32 is true of both.
        final Map<String, OpcUaDataType> union = typesOf(MethodSchemas.unionOfInputs(List.of(
                method("Start", argument("setpoint", NodeIds.Int32)),
                method("Adjust", argument("setpoint", NodeIds.Int32)))));

        assertThat(union).containsExactly(Map.entry("setpoint", OpcUaDataType.Int32));
    }

    @Test
    void severalMethodsDisagreeing_collapseToAny() {
        // Narrowing to either type would make the schema state something false about the other method, so the
        // union states nothing. A null value is how `any` is carried.
        final Map<String, OpcUaDataType> union = typesOf(MethodSchemas.unionOfInputs(List.of(
                method("Start", argument("mode", NodeIds.Int32)), method("Adjust", argument("mode", NodeIds.String)))));

        assertThat(union).containsOnlyKeys("mode");
        assertThat(union.get("mode")).isNull();
    }

    @Test
    void aThirdMethodAgreeingWithTheFirst_doesNotUndoTheConflict() {
        // Order matters here: once two methods disagree the answer is `any` permanently, and a later method
        // agreeing with whichever was seen first must not restore its type. Without the conflict set this
        // reverted to Int32 and the schema went back to lying about Adjust.
        final Map<String, OpcUaDataType> union = typesOf(MethodSchemas.unionOfInputs(List.of(
                method("Start", argument("mode", NodeIds.Int32)),
                method("Adjust", argument("mode", NodeIds.String)),
                method("Reset", argument("mode", NodeIds.Int32)))));

        assertThat(union.get("mode")).isNull();
    }

    @Test
    void argumentsAreInTheOrderTheDeviceReportedThem() {
        // Stable for a given server, which is what makes a schema diff readable when the device changes.
        final Map<String, OpcUaDataType> union = typesOf(MethodSchemas.unionOfInputs(List.of(
                method("Start", argument("b", NodeIds.Int32), argument("a", NodeIds.Int32)),
                method("Stop", argument("c", NodeIds.Int32)))));

        assertThat(union.keySet()).containsExactly("b", "a", "c");
    }

    @Test
    void unnamedArgument_isOmittedFromTheSchema() {
        // A request addresses arguments by name, so an unnamed one cannot appear in the schema. It stays
        // callable positionally -- the signature keeps it, only the schema drops it.
        final Map<String, OpcUaDataType> union = typesOf(MethodSchemas.unionOfInputs(List.of(method(
                "Start",
                argument("setpoint", NodeIds.Int32),
                new Argument(null, NodeIds.Int32, -1, null, LocalizedText.NULL_VALUE),
                new Argument("", NodeIds.Int32, -1, null, LocalizedText.NULL_VALUE)))));

        assertThat(union).containsOnlyKeys("setpoint");
    }

    @Test
    void customStructureType_becomesAny() {
        // A DataType naming a custom structure resolves to no builtin, and `any` is the only true answer --
        // the same answer a collision gets, for the same reason.
        final Map<String, OpcUaDataType> union =
                typesOf(MethodSchemas.unionOfInputs(List.of(method("Start", argument("recipe", new NodeId(3, 4001))))));

        assertThat(union).containsOnlyKeys("recipe");
        assertThat(union.get("recipe")).isNull();
    }

    @Test
    void noMethods_yieldsAnEmptyUnion() {
        // An object exposing no methods is a usable tag that can never serve a request. Its schema says the
        // request has no arguments, which is true.
        assertThat(MethodSchemas.unionOfInputs(List.of())).isEmpty();
    }

    @Test
    void outputsUnionIndependentlyOfInputs() {
        final MethodSignature start = new MethodSignature(
                "Start",
                NODE,
                NODE,
                List.of(argument("setpoint", NodeIds.Int32)),
                List.of(argument("started", NodeIds.Boolean)));

        assertThat(MethodSchemas.unionOfInputs(List.of(start))).containsOnlyKeys("setpoint");
        assertThat(MethodSchemas.unionOfOutputs(List.of(start))).containsOnlyKeys("started");
    }
}
