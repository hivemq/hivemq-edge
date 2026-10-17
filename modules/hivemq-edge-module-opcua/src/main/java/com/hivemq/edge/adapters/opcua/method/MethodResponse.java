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
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.eclipse.milo.opcua.stack.core.encoding.EncodingContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExtensionObject;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.XmlElement;
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
/// Everything else -- `methodName`, the caller's `correlationData`, and on failure `errorCode` and `statusInfo`
/// -- goes into one object *inside* `value`, named by
/// [MethodRequestFieldsOpcUa#INTERNAL_RESPONSE_PARAMETERS]. Edge lifts those out onto the published message
/// beside `value` and removes the carrier, so the wire shape is `{"methodName": ..., "value": ...}` and no
/// published message or schema mentions the carrier at all.
///
/// ## Why inside `value` rather than in the datapoint's `context`
///
/// The datapoint's aspect vocabulary -- value, timestamp, metadata, tag name, user properties -- has no slot for
/// any of these, so they need a carrier. An earlier version used the datapoint's `context` child, which looked
/// free because nothing published it. It was not: `context` is the *protocol's* namespace for what describes a
/// tag or a device, so Edge borrowing it meant picking its own names back out of an object an adapter may also
/// fill, and both generated schemas then described that inner shape instead of the messages.
///
/// A field inside `value` is recognisably Edge's instead, and is safe there because nothing validates the
/// payload at the moment it crosses the boundary between Edge and an adapter. The field's name ends in 64
/// random bits, so a device argument or a hand-written payload cannot reach this machinery by chance -- see
/// [MethodRequestFieldsOpcUa#INTERNAL_RESPONSE_PARAMETERS] for the encoding and the residual limitation.
public final class MethodResponse {

    private MethodResponse() {}

    /// Fills a datapoint with a call's outcome.
    ///
    /// @param builder         the datapoint being built for the method tag.
    /// @param methodName      which method this answers, echoed so a consumer reading one topic can tell.
    /// @param correlationData the caller's token, or null when it sent none or the mapping does not read one.
    /// @param responseTopic   the address the request asked for, or null when it named none. Passed on rather
    ///                        than acted on here: whether it is honoured is the northbound mapping's decision,
    ///                        and {@code NorthboundTagConsumer} makes it. It travels in the parameter object
    ///                        with the rest, and is the one entry that consumer takes instead of publishing, so
    ///                        a routing instruction never appears inside the routed message.
    /// @param result          what the call answered.
    /// @param ctx             the client's encoding context, which the shared converter needs to decode a value
    ///                        of a custom structure type. A method *argument* of such a type is refused as
    ///                        unencodable, but a device may still return one.
    /// @return the outputs that could not be decoded, by name, with the reason -- empty when all converted.
    ///         The caller logs and counts them; the answer itself names them in `statusInfo`.
    public static @NotNull Map<String, UndecodedOutput> write(
            final @NotNull DataPointBuilder<?> builder,
            final @NotNull String methodName,
            final @Nullable String correlationData,
            final @Nullable String responseTopic,
            final @NotNull MethodCallResult result,
            final @NotNull Supplier<EncodingContext> ctx) {

        final Map<String, UndecodedOutput> undecodable = new LinkedHashMap<>();

        // `value` is always an object here, because the parameters ride inside it. A failed call writes no
        // output arguments, so what Edge receives holds the parameters alone -- and Edge publishes `value` as
        // null once it has lifted them out. That keeps "the call failed" distinct from "the method succeeded
        // and declares no outputs", which publishes an empty object.
        final var value = builder.startObjectValue();
        if (result.succeeded()) {
            result.outputArguments().forEach((name, variant) -> putVariant(value, name, variant, ctx, undecodable));
        }

        final var parameters = value.startObject(MethodRequestFieldsOpcUa.INTERNAL_RESPONSE_PARAMETERS);
        parameters.put(MethodRequestFieldsOpcUa.RESPONSE_METHOD_NAME, methodName);
        if (correlationData != null) {
            parameters.put(MethodRequestFieldsOpcUa.CORRELATION_DATA, correlationData);
        }
        if (responseTopic != null && !responseTopic.isBlank()) {
            parameters.put(MethodRequestFieldsOpcUa.RESPONSE_TOPIC, responseTopic);
        }
        if (!result.succeeded()) {
            parameters.put(MethodRequestFieldsOpcUa.ERROR_CODE, result.statusName());
        }
        writeStatusInfo(parameters, result, ctx, undecodable);
        parameters.endObject();

        value.endObject();
        return undecodable;
    }

    /// Writes whatever detail there is about this answer, in up to three parts.
    ///
    /// **`statusInfo` is detail about the answer, not a sign that the call failed.** `errorCode` alone says
    /// that, and a consumer deciding otherwise -- on a missing `value`, or on the presence of this object --
    /// is reading the implementation rather than the contract. Two of the three parts below do only appear on
    /// a failure, because only a failure produces them; the third appears on a successful call.
    ///
    /// OPC UA reports a failed call's detail in two unrelated forms: a status per input argument, and whatever
    /// the method itself returned. The third part names an output this client could not decode. Each appears
    /// only when there is something to say, so one key is normal and the whole object is absent when there is
    /// nothing.
    private static void writeStatusInfo(
            final DataPointBuilder.@NotNull ObjectBuilder<?> parameters,
            final @NotNull MethodCallResult result,
            final @NotNull Supplier<EncodingContext> ctx,
            final @NotNull Map<String, UndecodedOutput> undecodable) {

        final boolean failed = !result.succeeded();
        final boolean hasArguments = failed && !result.argumentStatuses().isEmpty();
        final boolean hasDeviceOutput = failed && !result.outputArguments().isEmpty();
        if (!hasArguments && !hasDeviceOutput && undecodable.isEmpty()) {
            return;
        }

        final var statusInfo = parameters.startObject(MethodRequestFieldsOpcUa.STATUS_INFO);
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
            result.outputArguments()
                    .forEach((name, variant) -> putVariant(deviceOutput, name, variant, ctx, undecodable));
            deviceOutput.endObject();
        }
        if (!undecodable.isEmpty()) {
            // Written last, because the two branches above convert values too and may add to this map.
            //
            // Named rather than silently stringified: the consumer is holding a string where its schema
            // promised an object, and has no other way to learn why. The condition is a missing type
            // description rather than a property of the output, so it is usually transient -- the same call a
            // moment later may answer properly.
            final var undecodedOutputs = statusInfo.startObject("undecodedOutputs");
            undecodable.forEach((name, output) -> {
                final var entry = undecodedOutputs.startObject(name);
                entry.put("reason", output.reason());
                if (output.typeId() != null) {
                    entry.put("typeId", output.typeId());
                }
                if (output.encoding() != null) {
                    entry.put("encoding", output.encoding());
                }
                entry.put("body", output.body());
                entry.endObject();
            });
            undecodedOutputs.endObject();
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
    /// Written as its string form when the converter cannot do better, and the names of those outputs are
    /// collected so the caller learns which ones and why.
    ///
    /// **A method's answer exists once.** The value path answers an undecodable structure by dropping the
    /// sample, resetting the client's type registry and waiting for the server to resample (EDG-776) -- correct
    /// there, because the next notification carries the value again. A call has no next notification: dropping
    /// its answer leaves the caller with silence for a method that ran, and the device cannot be asked to run
    /// it again to find out what happened.
    ///
    /// So the structure is published as its string representation, which is what this converter already does
    /// for any type it has no explicit handling for. It is a poor rendering of a value that was returned, and
    /// better than no answer about a method that executed.
    ///
    /// Reported by Miguel as R5-3 on EDG-1087.
    private static void putVariant(
            final DataPointBuilder.@NotNull ObjectBuilder<?> target,
            final @NotNull String key,
            final @Nullable Variant variant,
            final @NotNull Supplier<EncodingContext> ctx,
            final @NotNull Map<String, UndecodedOutput> undecodable) {

        final Object value = variant == null ? null : variant.value();
        try {
            OpcUaToJsonConverter.addValueToObject(target, key, value, ctx.get());
        } catch (final RuntimeException e) {
            // **Caught by outcome, not by cause.** The common one is `UaSerializationException`, "no codec
            // registered": Milo rebuilds its dynamic codec registry on every session activation, best-effort,
            // and a browse that failed under load leaves it incomplete for a type that would decode a moment
            // later. But a structure whose enumeration field holds a value outside its definition raises
            // `IllegalArgumentException` instead, and naming one exception type let that one escape to the
            // caller's handler, which logs and publishes nothing -- so a method that ran on the device was
            // never answered and the write still counted as a success. Whatever the cause, the outcome here
            // is the same and so is the right response: say what came back, as well as it can be said.
            //
            // **The output itself becomes null, and what came back moves to `statusInfo`.** That keeps the
            // generated response schema honest: an output argument's own field stays the type its signature
            // declares, merely nullable, rather than being widened to admit a substitute shape it will carry
            // only when something went wrong. `statusInfo` is already unconstrained, so the recovered body
            // belongs there. An earlier draft of this fix published the body in place of the value, which
            // made every structured output's schema describe a failure mode.
            //
            // Reported by Miguel as Q03 on EDG-1087, who found the two cases that reached the caller's
            // handler.
            target.putNull(key);
            undecodable.put(
                    key,
                    undecodedBody(
                            value,
                            Objects.requireNonNullElse(
                                    e.getMessage(), e.getClass().getSimpleName())));
        }
    }

    /// What came back for an output that could not be decoded: why, and the body the device actually sent.
    ///
    /// **An undecoded `ExtensionObject` still carries its body**, because decoding is what failed -- the
    /// container is in hand with its raw encoded body and the type id the server labelled it with. OPC 10000-6
    /// gives three body encodings and each is reported as what it is: binary as base64, XML and JSON as their
    /// own text. So a caller holds the value the device returned and can decode it with a type description
    /// this client lacked, instead of a Java rendering like
    /// `ExtensionObject.Binary[body=ByteString[bytes=0102...]]` -- a debugging aid in which the bytes are
    /// present but not recoverable.
    ///
    /// `encoding` is named rather than implied, for the reason the correlation token carries its own
    /// `base64:` prefix: a bare base64 string cannot be told from a device value that genuinely is base64
    /// text, so nothing would tell a consumer to decode it.
    ///
    /// Anything that is not an `ExtensionObject` reports its string form and no body. That case does not
    /// arise from a missing type description -- there is no encoded body to hand back -- and inventing fields
    /// for it would promise more than is known.
    private static @NotNull UndecodedOutput undecodedBody(final @Nullable Object value, final @NotNull String reason) {
        if (!(value instanceof final ExtensionObject extensionObject)) {
            return new UndecodedOutput(reason, null, null, String.valueOf(value));
        }
        final NodeId typeId = extensionObject.getEncodingOrTypeId();
        final String typeName = typeId == null ? null : typeId.toParseableString();
        final Object body = extensionObject.getBody();
        if (body instanceof final ByteString bytes) {
            return new UndecodedOutput(
                    reason, typeName, "base64", Base64.getEncoder().encodeToString(bytes.bytesOrEmpty()));
        }
        if (body instanceof final XmlElement xml) {
            return new UndecodedOutput(reason, typeName, "xml", Objects.requireNonNullElse(xml.getFragment(), ""));
        }
        // The JSON body encoding, whose body is the text itself. Reached by elimination rather than by a cast:
        // the three subclasses are fixed by the specification, and a fourth would report its string form here
        // rather than cost the answer.
        return new UndecodedOutput(reason, typeName, "json", String.valueOf(body));
    }

    /// One output that could not be decoded, as `statusInfo.undecodedOutputs` reports it.
    ///
    /// @param reason   what the converter said -- a message, or an exception's name when it carried none
    /// @param typeId   the type the server labelled the value with, absent when it is not a structure
    /// @param encoding `base64`, `xml` or `json`, naming how to read `body`; absent with no body
    /// @param body     the value as the device sent it, or its string form when nothing encoded is available
    record UndecodedOutput(
            @NotNull String reason,
            @Nullable String typeId,
            @Nullable String encoding,
            @NotNull String body) {}

    /// A status code's specification name, falling back to its hexadecimal value.
    private static @NotNull String nameOf(final @NotNull StatusCode status) {
        return org.eclipse.milo.opcua.stack.core.StatusCodes.lookup(status.value())
                .map(names -> names[0])
                .orElseGet(() -> String.format("0x%08X", status.value()));
    }
}
