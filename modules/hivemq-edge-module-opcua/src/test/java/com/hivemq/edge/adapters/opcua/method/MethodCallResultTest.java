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

import java.util.List;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodResult;
import org.eclipse.milo.opcua.stack.core.types.structured.CallResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ResponseHeader;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/// Reading what a Call answered, and translating it back into the caller's vocabulary.
///
/// The caller sent arguments by name and never saw their positions, so everything the server reports
/// positionally has to be translated back. These pin that translation and the success/failure boundary.
class MethodCallResultTest {

    private static final @NotNull NodeId NODE = new NodeId(2, "Boiler1.Start");

    private static @NotNull Argument argument(final @NotNull String name) {
        return new Argument(name, NodeIds.Int32, -1, null, LocalizedText.NULL_VALUE);
    }

    private static @NotNull MethodSignature signature(
            final @NotNull List<Argument> inputs, final @NotNull List<Argument> outputs) {
        return MethodSignature.ofObject("Start", NODE, NODE, inputs, outputs);
    }

    private static @NotNull CallResponse response(final @NotNull CallMethodResult... results) {
        return new CallResponse(new ResponseHeader(null, null, null, null, null, null), results, null);
    }

    @Test
    void outputArguments_areKeyedByTheNamesTheMethodDeclares() {
        final MethodSignature signature =
                signature(List.of(), List.of(argument("started"), argument("actualSetpoint")));
        final CallMethodResult result =
                new CallMethodResult(StatusCode.GOOD, null, null, new Variant[] {Variant.of(true), Variant.of(178)});

        final MethodCallResult extracted = MethodCallResult.of(response(result), signature);

        assertThat(extracted.succeeded()).isTrue();
        assertThat(extracted.outputArguments()).containsOnlyKeys("started", "actualSetpoint");
        assertThat(extracted.outputArguments().get("started").value()).isEqualTo(true);
        assertThat(extracted.outputArguments().get("actualSetpoint").value()).isEqualTo(178);
    }

    @Test
    void perArgumentStatuses_areTranslatedFromPositionsToNames() {
        // The translation the caller cannot do for itself. Reporting "argument 0 was rejected" would be useless
        // to someone who sent a named object.
        final MethodSignature signature = signature(List.of(argument("setpoint"), argument("ramp")), List.of());
        final CallMethodResult result = new CallMethodResult(
                new StatusCode(StatusCodes.Bad_InvalidArgument),
                new StatusCode[] {new StatusCode(StatusCodes.Bad_OutOfRange), StatusCode.GOOD},
                null,
                null);

        final MethodCallResult extracted = MethodCallResult.of(response(result), signature);

        assertThat(extracted.succeeded()).isFalse();
        assertThat(extracted.statusName()).isEqualTo("Bad_InvalidArgument");
        assertThat(extracted.argumentStatuses()).containsOnlyKeys("setpoint", "ramp");
        assertThat(extracted.argumentStatuses().get("setpoint").isBad()).isTrue();
        assertThat(extracted.argumentStatuses().get("ramp").isGood()).isTrue();
    }

    @Test
    void goodArgumentStatuses_areKeptNotFiltered() {
        // "setpoint was rejected, ramp was fine" says the rest were looked at; "setpoint was rejected" alone
        // leaves the operator wondering whether the others were even evaluated.
        final MethodSignature signature = signature(List.of(argument("setpoint"), argument("ramp")), List.of());
        final CallMethodResult result = new CallMethodResult(
                new StatusCode(StatusCodes.Bad_InvalidArgument),
                new StatusCode[] {new StatusCode(StatusCodes.Bad_OutOfRange), StatusCode.GOOD},
                null,
                null);

        assertThat(MethodCallResult.of(response(result), signature).argumentStatuses())
                .hasSize(2);
    }

    @Test
    void statusName_usesTheSpecificationName() {
        final CallMethodResult result =
                new CallMethodResult(new StatusCode(StatusCodes.Bad_NotExecutable), null, null, null);

        // The name is stable across servers and searchable in the specification; the number is not either.
        assertThat(MethodCallResult.of(response(result), signature(List.of(), List.of()))
                        .statusName())
                .isEqualTo("Bad_NotExecutable");
    }

    @Test
    void uncertainStatus_countsAsFailure() {
        // The opposite of how a read treats Uncertain. An uncertain read still carries a value worth publishing
        // with a caveat; an uncertain call has no result to publish, so the caller is told it failed and given
        // whatever the device said instead of a value it cannot trust.
        final CallMethodResult result =
                new CallMethodResult(new StatusCode(StatusCodes.Uncertain_InitialValue), null, null, null);

        assertThat(MethodCallResult.of(response(result), signature(List.of(), List.of()))
                        .succeeded())
                .isFalse();
    }

    @Test
    void emptyResults_reportABadCallRatherThanSuccess() {
        // The service answered about no method. Nothing says which half failed, so this is not reported as a
        // method that ran and returned nothing.
        final MethodCallResult extracted = MethodCallResult.of(response(), signature(List.of(), List.of()));

        assertThat(extracted.succeeded()).isFalse();
        assertThat(extracted.outputArguments()).isEmpty();
    }

    @Test
    void moreOutputsThanDeclared_dropsTheUnnameable() {
        // The server is trusted about the values and not about the count: an extra value has no name a consumer
        // could address it by, so inventing one would put a key in the response that no schema mentions.
        final MethodSignature signature = signature(List.of(), List.of(argument("started")));
        final CallMethodResult result =
                new CallMethodResult(StatusCode.GOOD, null, null, new Variant[] {Variant.of(true), Variant.of(99)});

        assertThat(MethodCallResult.of(response(result), signature).outputArguments())
                .containsOnlyKeys("started");
    }

    @Test
    void collectionsAreImmutable() {
        final MethodSignature signature = signature(List.of(), List.of(argument("started")));
        final CallMethodResult result =
                new CallMethodResult(StatusCode.GOOD, null, null, new Variant[] {Variant.of(true)});

        final MethodCallResult extracted = MethodCallResult.of(response(result), signature);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> extracted.outputArguments().put("sneaked", Variant.NULL_VALUE))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
