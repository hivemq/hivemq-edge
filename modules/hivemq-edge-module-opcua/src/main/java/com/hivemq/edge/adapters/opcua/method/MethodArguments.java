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

import com.fasterxml.jackson.databind.JsonNode;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.stack.core.OpcUaDataType;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.Matrix;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// Turns a request's named arguments into the positional `Variant[]` a `Call` takes.
///
/// A request names its arguments; the wire passes them by position. The order in {@link MethodSignature} is that
/// mapping, so this walks the signature rather than the request -- an argument the method declares and the
/// request omits still occupies its position, and one the request carries but the method does not declare is a
/// mistake worth naming rather than silently dropping.
public final class MethodArguments {

    /// 2^64 - 1, the largest UInt64. Milo states it as `ULong.MAX_VALUE`, but that constant is a `BigInteger` the
    /// schema generator cannot carry -- it writes bounds through a `Number` the SDK renders with `longValue()` --
    /// so a UInt64 argument's schema states no range and this is the only gate the value passes.
    private static final @NotNull BigInteger UINT64_MAX = BigInteger.TWO.pow(64).subtract(BigInteger.ONE);

    /// No list has been seen yet at a dimension, so its length is still open. Negative because 0 is a length a
    /// real list can have: `[[], []]` is a rectangular 2x0 value, and its inner dimension is genuinely 0 rather
    /// than unknown.
    private static final int UNKNOWN_LENGTH = -1;

    /// 2^53, the first magnitude a whole number cannot be trusted at. The largest trustworthy one is 2^53 - 1.
    ///
    /// **Why the limit excludes 2^53 itself rather than including it.** `JSON.parse` rounds to nearest-even, so
    /// 2^53 is the value a rounded number *arrives as*: `9007199254740993` becomes `9007199254740992`, and
    /// `-...993` becomes `-2^53`. Accepting 2^53 therefore accepts exactly the corrupted case this guard exists
    /// for -- the first measured row of the original report. A caller that genuinely means 2^53 is refused as
    /// collateral, which is the right side to err on: 2^53 and "something that was 2^53 + 1" are the same bits
    /// here, and nothing can tell them apart. 2^53 - 1 is JavaScript's own `Number.MAX_SAFE_INTEGER`, defined as
    /// the largest integer for which that ambiguity does not arise.
    ///
    /// Found by Miguel in round 2 of EDG-1087: the first version of this guard compared `> 2^53` and let the
    /// rounded value through, and its test pinned that value as accepted.
    ///
    /// **Why a limit unrelated to any OPC UA type.** A southbound request does not arrive here as the caller
    /// wrote it. It passes through the field-mapping transform, which Data Hub runs as JavaScript, and that
    /// wrapper's first act is `JSON.parse` on the payload. JavaScript has one numeric type, the IEEE double, so
    /// every whole number beyond 2^53 lands on the nearest representable double before any Edge code sees it.
    /// `9007199254740993` arrives as `9007199254740992`; the maximum signed 64-bit integer,
    /// `9223372036854775807`, arrives as `9223372036854776000`.
    ///
    /// **Why refuse rather than detect.** There is nothing left to detect. `...993` and `...992` both arrive as
    /// `...992`, so a corrupted value and a legitimate one are byte-identical here -- no residue, no flag, no
    /// lost-precision bit. The only honest question this code can ask is whether the value *could* have crossed
    /// intact, and the only honest answer to "no" is a refusal. A caller then learns the limit instead of a PLC
    /// acting on a number nobody requested.
    ///
    /// **What this does and does not add.** Values near the top of a type's range already fail: they round
    /// *outward*, past the type's own maximum, and the range check above catches them. This closes the interior
    /// -- 2^53 up to the type maximum -- where a value rounds to a different but perfectly legal number and
    /// passed every gate. Only `Int64` and `UInt64` can reach it; every narrower type is bounded far below.
    ///
    /// **Not fixable here, and not fixable in the transform either.** The generated script receives an already
    /// parsed payload and cannot reach the original text -- the wrapper builds its argument as
    /// `{...publishData, payload}`, which replaces the raw string. A real fix belongs at Data Hub's parse
    /// boundary, which would affect every data policy in every deployment, so it is not this adapter's to make.
    ///
    /// The limit is the same for every adapter whose southbound path carries a field mapping; it is stated here
    /// because this is where a method argument is converted. Raised by Miguel as M3 on EDG-1087.
    private static final @NotNull BigInteger PRECISION_LIMIT = BigInteger.TWO.pow(53);

    private MethodArguments() {}

    /// Why a request could not be turned into a call. Carries the message a caller sees, which names the
    /// offending argument -- an OPC UA server answers the same mistakes with `Bad_InvalidArgument` and no
    /// indication of which argument was wrong.
    public static final class InvalidArgumentException extends RuntimeException {
        private final @NotNull String reason;

        InvalidArgumentException(final @NotNull String message) {
            super(message);
            this.reason = message;
        }

        /// The message, as a non-null value.
        ///
        /// `getMessage()` is declared nullable on `Throwable`, so a caller reporting this to a user has to
        /// defend against a null that cannot occur here. Kept as its own field rather than asserted at each
        /// call site.
        public @NotNull String reason() {
            return reason;
        }
    }

    /// Builds the input arguments for a call, in wire order.
    ///
    /// @param signature the method as the device describes it.
    /// @param value     the request's `value` object, keyed by argument name.
    /// @throws InvalidArgumentException when the request names an argument the method does not declare, omits one
    ///         it does, or carries a value the argument's type cannot hold.
    public static @NotNull Variant[] forCall(final @NotNull MethodSignature signature, final @NotNull JsonNode value) {

        rejectUnknownNames(signature, value);

        final List<Argument> declared = signature.inputArguments();
        final Variant[] arguments = new Variant[declared.size()];
        for (int i = 0; i < declared.size(); i++) {
            final Argument argument = declared.get(i);
            final String name = argument.getName();
            // An argument the device did not name cannot be addressed by a request, so nothing can be supplied
            // for it. Passing a null Variant keeps the positions aligned, which is the only thing that matters
            // for the arguments that *are* named.
            if (name == null || name.isEmpty()) {
                arguments[i] = Variant.NULL_VALUE;
                continue;
            }
            final JsonNode supplied = value.get(name);
            if (supplied == null || supplied.isNull()) {
                throw new InvalidArgumentException("method '"
                        + signature.name()
                        + "' requires the argument '"
                        + name
                        + "', which the request does not carry. It declares: "
                        + String.join(", ", signature.inputArgumentNames()));
            }
            arguments[i] = convert(signature.name(), name, argument, signature.builtinTypeOf(argument), supplied);
        }
        return arguments;
    }

    /// Rejects a request naming arguments the method does not declare.
    ///
    /// Done before any conversion so the message names the mistake rather than the symptom. Without it the extra
    /// field is simply ignored, and a caller who misspells `setpoint` gets a `Bad_ArgumentsMissing` about the
    /// argument they thought they had supplied.
    private static void rejectUnknownNames(final @NotNull MethodSignature signature, final @NotNull JsonNode value) {
        final List<String> unknown = new ArrayList<>();
        value.fieldNames().forEachRemaining(name -> {
            if (signature.inputPosition(name) == null) {
                unknown.add(name);
            }
        });
        if (!unknown.isEmpty()) {
            throw new InvalidArgumentException("method '"
                    + signature.name()
                    + "' does not declare the argument(s) "
                    + String.join(", ", unknown)
                    + ". It declares: "
                    + (signature.declaresNoInputs() ? "none" : String.join(", ", signature.inputArgumentNames())));
        }
    }

    /// Converts one JSON value to the Java type the argument's DataType expects.
    ///
    /// Only the builtin types are handled, but the argument's own DataType need not *be* one: a **Simple
    /// DataType** is a subtype of a builtin and, per OPC 10000-3 section 5.8.2, is "handled on the wire like the
    /// Built-in DataType". `Duration` -- a Double in milliseconds -- is the specification's example and the
    /// standard type for a timeout argument. Reducing one needs the server's DataType hierarchy, so the
    /// {@link MethodSignature} carries the answer, resolved during the browse that discovered the method.
    ///
    /// An argument of a custom structure type reduces to no builtin and is refused rather than guessed at: the
    /// generated schema already says `any` for it, so a caller has no contract to write against, and encoding a
    /// structure needs the DataTypeTree machinery the value path uses.
    ///
    /// An argument the device declares with a positive value rank is an array, and its value has to be built as a
    /// Java array of the element type's backing class -- `Integer[]` rather than `Object[]` -- because that is how
    /// the encoder decides what it is writing.
    private static @NotNull Variant convert(
            final @NotNull String methodName,
            final @NotNull String argumentName,
            final @NotNull Argument argument,
            final @Nullable OpcUaDataType type,
            final @NotNull JsonNode supplied) {

        if (type == null) {
            throw new InvalidArgumentException("argument '"
                    + argumentName
                    + "' of method '"
                    + methodName
                    + "' has a type this version cannot encode. Only the builtin OPC UA types are supported");
        }

        final int dimensions = depthFor(argument, supplied, argumentName, methodName);

        try {
            if (dimensions == 0) {
                return Variant.of(toJavaValue(type, supplied));
            }
            // One slot per declared dimension, each `UNKNOWN_LENGTH` until the first list at that level fixes
            // it. A real length may legitimately be 0 -- `[[], []]` is a rectangular 2x0 value -- so the
            // sentinel has to be a value no array can have rather than zero.
            final int[] lengthPerLevel = new int[dimensions];
            Arrays.fill(lengthPerLevel, UNKNOWN_LENGTH);
            final Object array = toJavaArray(type, supplied, dimensions, lengthPerLevel, -1);
            // A one-dimensional array goes into a Variant as itself; anything deeper has to be wrapped in a
            // Matrix, which is Milo's representation of a multi-dimensional value and what `Variant.of` demands
            // ("Multi-dimensional arrays must be wrapped in Matrix"). The wrapper carries the dimension lengths,
            // which it derives from the nested array it is given.
            return Variant.of(dimensions == 1 ? array : new Matrix(array));
            // Every runtime failure of a conversion, not only IllegalArgumentException. A conversion reaches for
            // whatever parser the type needs and those do not agree on an exception type: `Instant.parse` throws
            // DateTimeParseException, which is a RuntimeException and not an IllegalArgumentException, so a
            // timestamp written without a zone offset -- `2026-10-08T12:00:00` -- escaped this method entirely.
            // It then escaped `invoke`, which catches only InvalidArgumentException, into a whenComplete consumer
            // where nothing completes the WritingOutput, so the caller got no response and no refusal until the
            // write timed out 30 seconds later. The generated schema does not catch it either: it states
            // `format: date-time`, and format is an annotation unless a validator is configured to assert it.
        } catch (final RuntimeException e) {
            throw new InvalidArgumentException("argument '"
                    + argumentName
                    + "' of method '"
                    + methodName
                    + "' expects "
                    + describeExpected(type, dimensions)
                    + ", and the value supplied is not one: "
                    + e.getMessage());
        }
    }

    /// How deeply to nest this argument's value: a declared depth where the device fixed one, otherwise the
    /// depth the request itself has.
    ///
    /// OPC 10000-3 table 28 gives a value rank six meanings, and only three of them fix a depth:
    ///
    ///   - `n >= 1` -- an array with exactly n dimensions. The declaration decides, and a request that nests
    ///     differently is refused: a flat list for a two-dimensional argument is a mistake worth naming rather
    ///     than something to reshape.
    ///   - `-1` (Scalar) -- not an array.
    ///   - `0` (OneOrMoreDimensions) -- an array, of a depth the server declined to fix.
    ///   - `-2` (Any) -- a scalar or an array of any depth.
    ///   - `-3` (ScalarOrOneDimension) -- a scalar or a one-dimensional array.
    ///
    /// For the last three the request is the only thing that knows, so the depth is counted from it and then
    /// checked against what the rank permits. This is what makes such an argument callable at all: a rank-0
    /// argument accepts nothing but arrays, so treating it as a scalar -- which this did -- refused every legal
    /// value and the method could not be invoked. The generated schema says `any` for these three, so nothing
    /// before this point has constrained the shape either.
    ///
    /// Whatever passes here may still be wrong about the real signature, and the device is left to say so. That
    /// is the trade: an argument whose shape the declaration leaves open is answered by the server rather than
    /// by Edge guessing.
    private static int depthFor(
            final @NotNull Argument argument,
            final @NotNull JsonNode supplied,
            final @NotNull String argumentName,
            final @NotNull String methodName) {

        final Integer rank = argument.getValueRank();
        if (rank == null || rank == ValueRanks.Scalar) {
            return 0;
        }
        if (rank >= 1) {
            return rank;
        }

        final int suppliedDepth = depthOf(supplied);
        if (rank == ValueRanks.OneOrMoreDimensions && suppliedDepth == 0) {
            throw new InvalidArgumentException("argument '"
                    + argumentName
                    + "' of method '"
                    + methodName
                    + "' is declared as an array of one or more dimensions, so a single value is not one of its "
                    + "values. Send an array");
        }
        if (rank == ValueRanks.ScalarOrOneDimension && suppliedDepth > 1) {
            throw new InvalidArgumentException("argument '"
                    + argumentName
                    + "' of method '"
                    + methodName
                    + "' is declared as a single value or a one-dimensional array, and the value supplied nests "
                    + suppliedDepth
                    + " deep");
        }
        return suppliedDepth;
    }

    /// How many array levels a JSON value has. Zero for anything that is not an array, and an empty array counts
    /// as one level -- it is an array with no elements, not a scalar.
    private static int depthOf(final @NotNull JsonNode node) {
        int depth = 0;
        JsonNode current = node;
        while (current.isArray()) {
            depth++;
            if (current.isEmpty()) {
                break;
            }
            current = current.get(0);
        }
        return depth;
    }

    /// What the argument wanted, for an error message. A rank is worth naming: "expects Int32" against a value
    /// that is a perfectly good Int32 but should have been wrapped in an array reads as a contradiction.
    private static @NotNull String describeExpected(final @NotNull OpcUaDataType type, final int dimensions) {
        if (dimensions == 0) {
            return type.name();
        }
        return type.name() + "[]".repeat(dimensions);
    }

    /// Builds a nested Java array of the element type's backing class, one level per declared dimension.
    ///
    /// The depth comes from the declared value rank rather than from how deeply the JSON happens to nest, so a
    /// flat list supplied for a two-dimensional argument is reported as the wrong shape instead of being encoded
    /// as something the device did not ask for. Dimension *lengths* are not checked against the declaration --
    /// the generated schema states them, and the device enforces them; duplicating that would mean two places to
    /// keep in agreement.
    ///
    /// **Rectangularity is a different question from length, and is checked here.** A value whose rows disagree
    /// does not reach the device as sent: `Matrix` takes each dimension's length from the first row it finds and
    /// flattens against those, so a short row makes it read beyond that row's end and a long row makes it stop
    /// early -- dropping elements. Its own consistency check is a Java `assert`, disabled at runtime, so it
    /// reports nothing and the device acts on the truncated value.
    ///
    /// `lengthPerLevel` carries one length per dimension, each `UNKNOWN_LENGTH` until the first list at that
    /// depth fixes it. **Every later list at the same depth is measured against that one, whichever branch it
    /// sits in.** An earlier version compared a row only against its own first sibling, which misses a
    /// disagreement between branches: in `[[[1]], [[2,3]]]` the rows `[1]` and `[2,3]` have different parents, so
    /// nothing ever compared them, `Matrix` took the inner length as 1, and the `3` was silently dropped.
    /// `rowIndex` is this node's position among its siblings, or `-1` for the outermost value, which has none. It
    /// carries no logic and is only there so a refusal can name the row at fault.
    private static @NotNull Object toJavaArray(
            final @NotNull OpcUaDataType type,
            final @NotNull JsonNode node,
            final int remainingDimensions,
            final int @NotNull [] lengthPerLevel,
            final int rowIndex) {

        if (!node.isArray()) {
            throw new IllegalArgumentException(
                    "expected an array of " + remainingDimensions + " more dimension(s), got " + node.getNodeType());
        }

        // This node's depth, counted from the outside, so the two inner rows of `[[[1]], [[2,3]]]` share a slot.
        final int level = lengthPerLevel.length - remainingDimensions;
        if (lengthPerLevel[level] == UNKNOWN_LENGTH) {
            lengthPerLevel[level] = node.size();
        } else if (lengthPerLevel[level] != node.size()) {
            throw new IllegalArgumentException("every list at the same depth must have the same length, and row "
                    + rowIndex
                    + " at depth "
                    + (level + 1)
                    + " has "
                    + node.size()
                    + " element(s) where an earlier list at that depth has "
                    + lengthPerLevel[level]);
        }

        if (remainingDimensions == 1) {
            final Object[] elements = (Object[]) Array.newInstance(type.getBackingClass(), node.size());
            for (int i = 0; i < node.size(); i++) {
                elements[i] = toJavaValue(type, node.get(i));
            }
            return elements;
        }

        // A dimension above the innermost: each element is itself an array, so the component type has one array
        // level per remaining inner dimension -- `Integer[]` at depth 2, `Integer[][]` at depth 3. Building it
        // with an int[] of zero lengths asks the JDK for exactly that class without allocating anything.
        final Class<?> componentType = Array.newInstance(type.getBackingClass(), new int[remainingDimensions - 1])
                .getClass();
        final Object[] rows = (Object[]) Array.newInstance(componentType, node.size());
        for (int i = 0; i < node.size(); i++) {
            rows[i] = toJavaArray(type, node.get(i), remainingDimensions - 1, lengthPerLevel, i);
        }
        return rows;
    }

    private static @NotNull Object toJavaValue(final @NotNull OpcUaDataType type, final @NotNull JsonNode node) {
        return switch (type) {
            case Boolean -> requireBoolean(node);
            case SByte -> (byte) requireIntegral(node, Byte.MIN_VALUE, Byte.MAX_VALUE);
            case Byte -> Unsigned.ubyte(requireIntegral(node, 0, 255));
            case Int16 -> (short) requireIntegral(node, Short.MIN_VALUE, Short.MAX_VALUE);
            case UInt16 -> Unsigned.ushort((int) requireIntegral(node, 0, 65535));
            case Int32 -> (int) requireIntegral(node, Integer.MIN_VALUE, Integer.MAX_VALUE);
            case UInt32 -> Unsigned.uint(requireIntegral(node, 0, 4294967295L));
            case Int64 -> requireIntegral(node, Long.MIN_VALUE, Long.MAX_VALUE);
            // UInt64 is the one type whose range does not fit a signed long, so it is bounded and built as a
            // BigInteger throughout. Bounding it by Long.MAX_VALUE would refuse the upper half of the type.
            case UInt64 -> Unsigned.ulong(requireInRange(node, BigInteger.ZERO, UINT64_MAX));
            case Float -> (float) requireNumber(node);
            case Double -> requireNumber(node);
            case String -> requireText(node);
            case DateTime -> new DateTime(java.time.Instant.parse(requireText(node)));
            case ByteString -> ByteString.of(requireBase64(node));
            case Guid -> java.util.UUID.fromString(requireText(node));
            default -> throw new IllegalArgumentException(type.name() + " is not supported as a method argument");
        };
    }

    private static boolean requireBoolean(final @NotNull JsonNode node) {
        if (!node.isBoolean()) {
            throw new IllegalArgumentException("expected true or false, got " + node.getNodeType());
        }
        return node.booleanValue();
    }

    private static long requireIntegral(final @NotNull JsonNode node, final long min, final long max) {
        return requireInRange(node, BigInteger.valueOf(min), BigInteger.valueOf(max))
                .longValue();
    }

    /// The range check every integral argument passes through, comparing the value the caller actually sent.
    ///
    /// `JsonNode.longValue()` is not a safe first step: for a JSON integer wider than 64 bits it answers the value
    /// modulo 2^64 rather than refusing, so a check performed on its result validates a different number.
    /// `18446744073709551617` narrows to `1`, which satisfies every Int32 bound and reaches the device as `1`.
    /// `bigIntegerValue()` answers the value as written, so the comparison happens before anything narrows.
    ///
    /// Checked here rather than left to the server, because the narrowing cast at each call site wraps silently --
    /// 300 arriving at a Byte argument as 44, which the device would act on.
    ///
    /// **The magnitude limit is separate, and it is about the journey rather than the type.** See
    /// {@link #PRECISION_LIMIT}.
    private static @NotNull BigInteger requireInRange(
            final @NotNull JsonNode node, final @NotNull BigInteger min, final @NotNull BigInteger max) {
        if (!node.isIntegralNumber()) {
            throw new IllegalArgumentException("expected a whole number, got " + node.getNodeType());
        }
        final BigInteger value = node.bigIntegerValue();
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw new IllegalArgumentException("expected a value in [" + min + ", " + max + "], got " + value);
        }
        if (value.abs().compareTo(PRECISION_LIMIT) >= 0) {
            throw new IllegalArgumentException("expected a magnitude below "
                    + PRECISION_LIMIT
                    + " (2^53), got "
                    + value
                    + ". A larger whole number cannot reach a device unchanged: the southbound transform carries"
                    + " it through JavaScript, which has only the IEEE double and rounds it. Refused rather than"
                    + " sent, because the value the device would receive is not the value requested");
        }
        return value;
    }

    private static double requireNumber(final @NotNull JsonNode node) {
        if (!node.isNumber()) {
            throw new IllegalArgumentException("expected a number, got " + node.getNodeType());
        }
        return node.doubleValue();
    }

    /// A ByteString arrives as base64 text, which is how the generated schema describes it -- `BINARY` renders
    /// `contentEncoding: base64`. Decoded here rather than through Jackson's `binaryValue()`, which answers an
    /// empty array for a non-textual node instead of saying the value was the wrong shape.
    private static byte @NotNull [] requireBase64(final @NotNull JsonNode node) {
        final String text = requireText(node);
        try {
            return java.util.Base64.getDecoder().decode(text);
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException("expected base64-encoded bytes: " + e.getMessage());
        }
    }

    private static @NotNull String requireText(final @NotNull JsonNode node) {
        if (!node.isTextual()) {
            throw new IllegalArgumentException("expected a string, got " + node.getNodeType());
        }
        return node.textValue();
    }
}
