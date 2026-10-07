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
import java.util.Map;
import org.eclipse.milo.opcua.stack.core.types.builtin.Matrix;
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
    public static void write(
            final @NotNull DataPointBuilder<?> builder,
            final @NotNull String methodName,
            final @Nullable String correlationData,
            final @Nullable String responseTopic,
            final @NotNull MethodCallResult result) {

        if (result.succeeded()) {
            writeOutputArguments(builder, result.outputArguments());
        } else {
            // Present and null rather than absent. Absence would mean a consumer reading `value` has to ask
            // whether the field exists before asking what it holds, and the two questions have different
            // answers for the same failure.
            builder.valueNull();
        }

        final var context = builder.startObjectContext();
        context.put("methodName", methodName);
        if (correlationData != null) {
            context.put("correlationData", correlationData);
        }
        if (responseTopic != null && !responseTopic.isBlank()) {
            context.put("responseTopic", responseTopic);
        }
        if (!result.succeeded()) {
            context.put("errorCode", result.statusName());
            writeStatusInfo(context, result);
        }
        context.endObject();
    }

    /// Writes the output arguments as an object keyed by the names the method declares.
    ///
    /// An empty object for a method that returns nothing: it ran and had nothing to report, which a consumer
    /// should be able to distinguish from the null that says it did not run.
    private static void writeOutputArguments(
            final @NotNull DataPointBuilder<?> builder, final @NotNull Map<String, Variant> outputs) {

        final var value = builder.startObjectValue();
        outputs.forEach((name, variant) -> putVariant(value, name, variant));
        value.endObject();
    }

    /// Writes whatever detail the device gave about a failure, in up to two parts.
    ///
    /// OPC UA reports a failed call's detail in two unrelated forms: a status per input argument, and whatever
    /// the method itself returned. Each part appears only when the device supplied it, so `statusInfo` with one
    /// key is normal and the whole object is absent when the device supplied neither.
    private static void writeStatusInfo(
            final DataPointBuilder.@NotNull ObjectBuilder<?> context, final @NotNull MethodCallResult result) {

        final boolean hasArguments = !result.argumentStatuses().isEmpty();
        final boolean hasDeviceOutput = !result.outputArguments().isEmpty();
        if (!hasArguments && !hasDeviceOutput) {
            return;
        }

        final var statusInfo = context.startObject("statusInfo");
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
            result.outputArguments().forEach((name, variant) -> putVariant(deviceOutput, name, variant));
            deviceOutput.endObject();
        }
        statusInfo.endObject();
    }

    /// Writes one OPC UA value under a key, as the JSON type that matches it.
    ///
    /// Each branch is explicit rather than deferring to `toString`, which would publish a number as a quoted
    /// string and a boolean as `"true"` -- a consumer validating against the generated schema would then be
    /// rejected for a value Edge itself produced.
    private static void putVariant(
            final DataPointBuilder.@NotNull ObjectBuilder<?> target,
            final @NotNull String key,
            final @Nullable Variant variant) {

        // A multi-dimensional output arrives as a Matrix rather than a nested Java array, which is how Milo
        // represents one inside a Variant. Its nested form is what the published shape should mirror.
        final Object raw = variant == null ? null : variant.value();
        final Object value = raw instanceof final Matrix matrix ? matrix.nestedArrayValue() : raw;

        if (value == null) {
            target.putNull(key);
        } else if (value instanceof final Object[] elements) {
            // An array-valued output argument. Without this it fell into the toString branch below and published
            // a Java array identity -- `[Ljava.lang.Integer;@1a2b3c` -- for a method whose schema promised a JSON
            // array. Nested arrays recurse, so a multi-dimensional output keeps its shape.
            final var array = target.startArray(key);
            addElements(array, elements);
            array.endArray();
        } else if (value instanceof final Boolean b) {
            target.put(key, b.booleanValue());
        } else if (value instanceof final Integer i) {
            target.put(key, i.intValue());
        } else if (value instanceof final Long l) {
            target.put(key, l.longValue());
        } else if (value instanceof final Short s) {
            target.put(key, s.shortValue());
        } else if (value instanceof final Byte b) {
            target.put(key, b.byteValue());
        } else if (value instanceof final Double d) {
            target.put(key, d.doubleValue());
        } else if (value instanceof final Float f) {
            target.put(key, f.floatValue());
        } else if (value instanceof final String s) {
            target.put(key, s);
        } else {
            // Unsigned integers, DateTime, ByteString, NodeId and the rest. Their own toString is the wire form
            // the read path publishes for them, so matching it keeps a method's output and a variable's value
            // looking the same for the same OPC UA type.
            target.put(key, value.toString());
        }
    }

    /// Appends each element of an array output, as the JSON type that matches it.
    ///
    /// The same dispatch as {@link #putVariant}, by element rather than by key: one list of branches would need a
    /// target that is both an object and an array, and the two builders have no common supertype for that.
    private static void addElements(
            final DataPointBuilder.@NotNull ArrayBuilder<?> target, final @NotNull Object @NotNull [] elements) {

        for (final Object element : elements) {
            if (element == null) {
                target.addNull();
            } else if (element instanceof final Object[] inner) {
                final var nested = target.startArray();
                addElements(nested, inner);
                nested.endArray();
            } else if (element instanceof final Boolean b) {
                target.add(b.booleanValue());
            } else if (element instanceof final Integer i) {
                target.add(i.intValue());
            } else if (element instanceof final Long l) {
                target.add(l.longValue());
            } else if (element instanceof final Short s) {
                target.add(s.shortValue());
            } else if (element instanceof final Byte b) {
                target.add(b.byteValue());
            } else if (element instanceof final Double d) {
                target.add(d.doubleValue());
            } else if (element instanceof final Float f) {
                target.add(f.floatValue());
            } else if (element instanceof final String s) {
                target.add(s);
            } else {
                target.add(element.toString());
            }
        }
    }

    /// A status code's specification name, falling back to its hexadecimal value.
    private static @NotNull String nameOf(final @NotNull StatusCode status) {
        return org.eclipse.milo.opcua.stack.core.StatusCodes.lookup(status.value())
                .map(names -> names[0])
                .orElseGet(() -> String.format("0x%08X", status.value()));
    }
}
