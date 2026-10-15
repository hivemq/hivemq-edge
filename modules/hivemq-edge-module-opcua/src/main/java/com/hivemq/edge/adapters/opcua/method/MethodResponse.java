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

import com.hivemq.adapter.sdk.api.datapoint.DataPointBuilder;
import com.hivemq.edge.adapters.opcua.northbound.OpcUaToJsonConverter;
import org.eclipse.milo.opcua.stack.core.encoding.EncodingContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// Writes a call's outcome into the datapoint that becomes the response message.
///
/// ## The shape
///
/// `value` carries the method's output arguments by name on success, and is **null** on failure -- present either
/// way, so every response has the same fields and a consumer never has to ask whether one exists. A method that
/// succeeded and returns nothing gets an empty object, which says it ran and had nothing to report; that is
/// deliberately distinct from null, which says it did not run.
///
/// Everything else goes in the datapoint's `context` child: `methodName`, the caller's `correlationData`, and on
/// failure `errorCode` and `statusInfo`. The northbound converter flattens those into the published message
/// beside `value`, so the wire shape is `{"methodName": ..., "value": ...}` rather than a nested object.
///
/// ## Why `context` rather than named aspects
///
/// The datapoint's aspect vocabulary -- value, timestamp, metadata, tag name, user properties -- has no slot for
/// any of these, and `context` is an existing child that nothing publishes today. Using it avoids adding four
/// terms to the SDK for one adapter, at the cost of a shape the generated schemas cannot name. That trade is
/// recorded as a workaround in the plan rather than as the design; named aspects are the eventual form.
public final class MethodResponse {

    private MethodResponse() {}

    /// Fills a datapoint with a call's outcome.
    ///
    /// @param builder         the datapoint being built for the method tag.
    /// @param methodName      which method this answers, echoed so a consumer reading one topic can tell.
    /// @param correlationData the caller's token, or null when it sent none or the mapping does not read one.
    /// @param responseTopic   the address the request asked for, or null when it named none. Passed on rather
    ///                        than acted on here: whether it is honoured is the northbound mapping's decision,
    ///                        and {@code NorthboundTagConsumer} makes it. Carried in `context` because that is
    ///                        where that consumer looks -- it is the one entry it consumes instead of
    ///                        publishing, so a routing instruction never appears inside the routed message.
    /// @param result          what the call answered.
    /// @param ctx             the client's encoding context, which the shared converter needs to decode a value
    ///                        of a custom structure type. A method *argument* of such a type is refused as
    ///                        unencodable, but a device may still return one.
    public static void write(
            final @NotNull DataPointBuilder<?> builder,
            final @NotNull String methodName,
            final @Nullable String correlationData,
            final @Nullable String responseTopic,
            final @NotNull MethodCallResult result,
            final @NotNull EncodingContext ctx) {

        // `value` is always an object here, because the parameters ride inside it. A failed call writes no
        // output arguments, so what Edge receives holds the parameters alone -- and Edge publishes `value` as
        // null once it has lifted them out. That keeps "the call failed" distinct from "the method succeeded
        // and declares no outputs", which publishes an empty object.
        final var value = builder.startObjectValue();
        if (result.succeeded()) {
            result.outputArguments().forEach((name, variant) -> putVariant(value, name, variant, ctx));
        }

        final var parameters = value.startObject(MethodRequestFieldsOpcUa.INTERNAL_RESPONSE_PARAMETERS);
        parameters.put(MethodRequestFieldsOpcUa.METHOD_NAME, methodName);
        if (correlationData != null) {
            parameters.put(MethodRequestFieldsOpcUa.CORRELATION_DATA, correlationData);
        }
        if (responseTopic != null && !responseTopic.isBlank()) {
            parameters.put(MethodRequestFieldsOpcUa.RESPONSE_TOPIC, responseTopic);
        }
        if (!result.succeeded()) {
            parameters.put(MethodRequestFieldsOpcUa.ERROR_CODE, result.statusName());
            writeStatusInfo(parameters, result, ctx);
        }
        parameters.endObject();

        value.endObject();
    }

    /// Writes whatever detail the device gave about a failure, in up to two parts.
    ///
    /// OPC UA reports a failed call's detail in two unrelated forms: a status per input argument, and whatever
    /// the method itself returned. Each part appears only when the device supplied it, so `statusInfo` with one
    /// key is normal and the whole object is absent when the device supplied neither.
    private static void writeStatusInfo(
            final DataPointBuilder.@NotNull ObjectBuilder<?> context,
            final @NotNull MethodCallResult result,
            final @NotNull EncodingContext ctx) {

        final boolean hasArguments = !result.argumentStatuses().isEmpty();
        final boolean hasDeviceOutput = !result.outputArguments().isEmpty();
        if (!hasArguments && !hasDeviceOutput) {
            return;
        }

        final var statusInfo = context.startObject(MethodRequestFieldsOpcUa.STATUS_INFO);
        if (hasArguments) {
            final var arguments = statusInfo.startObject("arguments");
            // Keyed by the name the request used, not the position the server reported. The caller never saw the
            // positions, so reporting "argument 0 was rejected" would be unusable.
            result.argumentStatuses().forEach((name, status) -> arguments.put(name, nameOf(status)));
            arguments.endObject();
        }
        if (hasDeviceOutput) {
            // A failed call may still return values -- how a PLC usually says *why* it refused. They are the
            // device's explanation rather than the method's result, which is why they are here and not in
            // `value`.
            final var deviceOutput = statusInfo.startObject("deviceOutput");
            result.outputArguments().forEach((name, variant) -> putVariant(deviceOutput, name, variant, ctx));
            deviceOutput.endObject();
        }
        statusInfo.endObject();
    }

    /// Writes one OPC UA value under a key, through the same converter the read path uses.
    ///
    /// Delegated rather than dispatched here. An earlier version handled the primitives itself and fell back to
    /// `toString` for everything else, on the stated grounds that `toString` was what the read path published for
    /// those types. It is not: {@link OpcUaToJsonConverter} converts every one of them explicitly -- an unsigned
    /// integer becomes a number, a ByteString base64 text, a DateTime an ISO instant, a NodeId and a
    /// QualifiedName objects. So a method returning a `UInt32` of 7 published `"7"` where the generated schema
    /// promised `7`, and a ByteString published `ByteString[bytes=010203]` where the schema promised base64.
    ///
    /// The same type must look the same whether it arrives as a variable's value or a method's output, and the
    /// way to get that is one converter rather than two that agree by assertion. A multi-dimensional output
    /// needs nothing special here either: the converter unwraps a `Matrix` to its nested array itself, which it
    /// has to do anyway for a variable whose value is one.
    private static void putVariant(
            final DataPointBuilder.@NotNull ObjectBuilder<?> target,
            final @NotNull String key,
            final @Nullable Variant variant,
            final @NotNull EncodingContext ctx) {

        OpcUaToJsonConverter.addValueToObject(target, key, variant == null ? null : variant.value(), ctx);
    }

    /// A status code's specification name, falling back to its hexadecimal value.
    private static @NotNull String nameOf(final @NotNull StatusCode status) {
        return org.eclipse.milo.opcua.stack.core.StatusCodes.lookup(status.value())
                .map(names -> names[0])
                .orElseGet(() -> String.format("0x%08X", status.value()));
    }
}
