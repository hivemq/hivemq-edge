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
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
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
    void theWholeUInt64RangeIsAccepted() throws Exception {
        // The counterpart refusal: bounding UInt64 by Long.MAX_VALUE refused the upper half of the type, and
        // narrowing made it total -- 2^64-1 arrived as -1 and failed the lower bound of zero. Nothing else checks
        // this type's range, because its bounds are BigIntegers the schema generator cannot carry, so a legitimate
        // value refused here is refused outright.
        final Variant[] arguments = MethodArguments.forCall(
                signature(argument("count", NodeIds.UInt64)), json("{\"count\": 18446744073709551615}"));

        assertThat(arguments[0].value()).hasToString("18446744073709551615");

        // And one above the signed ceiling, the first value the old bound rejected.
        final Variant[] justAboveSigned = MethodArguments.forCall(
                signature(argument("count", NodeIds.UInt64)), json("{\"count\": 9223372036854775808}"));

        assertThat(justAboveSigned[0].value()).hasToString("9223372036854775808");
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
