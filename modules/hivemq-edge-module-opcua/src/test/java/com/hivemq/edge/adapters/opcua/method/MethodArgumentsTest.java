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
import java.util.List;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
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
        return new MethodSignature("Start", NODE, List.of(inputs), List.of());
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
