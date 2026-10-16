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
import java.util.UUID;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.stack.core.OpcUaDataType;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.Matrix;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.XmlElement;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
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
        private final long statusCode;

        InvalidArgumentException(final @NotNull String message) {
            this(message, StatusCodes.Bad_InvalidArgument);
        }

        InvalidArgumentException(final @NotNull String message, final long statusCode) {
            super(message);
            this.reason = message;
            this.statusCode = statusCode;
        }

        /// The message, as a non-null value.
        ///
        /// `getMessage()` is declared nullable on `Throwable`, so a caller reporting this to a user has to
        /// defend against a null that cannot occur here. Kept as its own field rather than asserted at each
        /// call site.
        public @NotNull String reason() {
            return reason;
        }

        /// The status code the caller is answered with.
        ///
        /// Carried on the exception because the fault is known here and not at the catch: every argument
        /// problem reaches the adapter through one handler, which cannot tell a missing argument from a
        /// misspelled one without inspecting the message text. `Bad_InvalidArgument` is the default, so a
        /// throw site that does not say otherwise behaves as before.
        public long statusCode() {
            return statusCode;
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
                // The one argument fault OPC-UA names specifically. Every other one -- a name the method does
                // not declare, a value its type cannot hold -- is Bad_InvalidArgument, which is also what a
                // server answers. Reported as its own code because a caller branching on status codes can act
                // on "you left something out" without parsing the message.
                throw new InvalidArgumentException(
                        "method '"
                                + signature.name()
                                + "' requires the argument '"
                                + name
                                + "', which the request does not carry. It declares: "
                                + String.join(", ", signature.inputArgumentNames()),
                        StatusCodes.Bad_ArgumentsMissing);
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
            case Float -> requireFloat(node);
            case Double -> requireNumber(node);
            case String -> requireText(node);
            // The six structured builtins. Each takes the form the northbound converter publishes *and* a
            // simple form, so a value read from a device can be sent straight back and a caller writing one by
            // hand does not have to assemble an object. Their schema says `any` -- see MethodSchemas.
            case LocalizedText -> requireLocalizedText(node);
            case NodeId -> requireNodeId(node);
            case ExpandedNodeId -> ExpandedNodeId.parse(requireText(node));
            case QualifiedName -> requireQualifiedName(node);
            case StatusCode -> requireStatusCode(node);
            case XmlElement -> new XmlElement(requireText(node));
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

    /// A status code, from either the object the read path publishes or the bare number.
    ///
    /// `code` carries the value and `symbol` is a label the read path looks up from it -- so the object form
    /// takes `code` and ignores `symbol`, rather than trying to reconcile the two. A caller who edits the
    /// symbol and not the code has changed nothing, which is the right reading: the code is the status.
    ///
    /// Both forms, for the same reason as the other structured types: a status code read from a device is
    /// published as this object, so requiring the bare number made a published value unsendable.
    private static @NotNull StatusCode requireStatusCode(final @NotNull JsonNode node) {
        if (node.isObject()) {
            final JsonNode code = node.get("code");
            if (code == null) {
                throw new IllegalArgumentException("expected an object with a 'code'");
            }
            return new StatusCode(requireIntegral(code, 0, 4294967295L));
        }
        return new StatusCode(requireIntegral(node, 0, 4294967295L));
    }

    /// A localised text, from either the object the read path publishes or a bare string.
    ///
    /// ## The two forms say different amounts, so they are read differently
    ///
    /// **A bare string becomes English.** It is a form that cannot express a locale at all, so filling one in
    /// is the only way to make it work -- and `ConditionUpdateWriter` already does exactly this for an alarm
    /// comment, the same argument of the same standard method reached by the other route. Two routes to one
    /// method answering differently would be worse than either answer.
    ///
    /// **The object form says exactly what the caller said, absences included.** A missing `locale` stays
    /// missing rather than becoming English, and a missing `text` stays missing rather than being refused,
    /// because both are states the protocol defines and the read path publishes: `populateLocalizedText` omits
    /// each field when it is null, so `{"text": "x"}` *is* how a value with no locale comes back from a device.
    /// Defaulting here rewrote every such value on its way back, which is the round trip the object form exists
    /// for.
    ///
    /// The distinction is not decorative. OPC 10000-9 section 5.7.3 gives a null localised text -- both fields
    /// absent -- its own meaning for a comment, "leave any existing comment alone", distinct from an empty text
    /// *with* a locale, which resets it. Substituting a locale turned the first into the second, so the object
    /// form of "do not touch the comment" became a command to clear it. The wire format has the absence built
    /// in: OPC 10000-6 encodes the pair as a bitmask plus whichever strings are present, and Milo spells the
    /// both-absent case {@link LocalizedText#NULL_VALUE}.
    ///
    /// A `locale` or `text` of the wrong JSON type is refused rather than ignored, since a caller who sent a
    /// number for a locale meant something, and it was not "no locale".
    private static @NotNull LocalizedText requireLocalizedText(final @NotNull JsonNode node) {
        if (node.isTextual()) {
            return LocalizedText.english(node.textValue());
        }
        if (!node.isObject()) {
            throw new IllegalArgumentException("expected a string or an object with 'text', got " + node.getNodeType());
        }
        return new LocalizedText(optionalText(node, "locale"), optionalText(node, "text"));
    }

    /// A string field that may legitimately be absent, distinguished from one that is the wrong type.
    ///
    /// Absent and JSON `null` both answer `null` -- the same state, spelled two ways. Anything else present is
    /// refused: silently treating it as absent would discard what the caller said.
    private static @Nullable String optionalText(final @NotNull JsonNode node, final @NotNull String field) {
        final JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isTextual()) {
            throw new IllegalArgumentException("expected '" + field + "' to be a string, got " + value.getNodeType());
        }
        return value.textValue();
    }

    /// A node id, from either the text form every tag configuration uses or the object the read path publishes.
    ///
    /// The text form is `ns=2;s=Boiler`, which `NodeId.parse` already reads for a tag's own node and for a
    /// method named by id. The object form carries `namespaceIndex`, `id` and `idType`, and is what a caller
    /// gets back from a read -- so a value can make the round trip without being rewritten by hand.
    ///
    /// ## Why `idType` decides, and is not inferred
    ///
    /// A node id is its namespace *and* its identifier *and* the identifier's kind: OPC 10000-3 section 8.2.3
    /// gives Numeric, String, Guid and Opaque, and two ids agreeing on namespace and spelling but differing in
    /// kind name different nodes. Two of the four kinds reach JSON as text -- a Guid as its hyphenated spelling
    /// and an Opaque as base64 -- so the JSON type of `id` cannot tell them apart from a String identifier.
    /// Inferring the kind from it, which is what this method used to do, silently rewrote every Guid and Opaque
    /// identifier read from the device into a String one, breaking the round trip the object form exists for.
    ///
    /// So `idType` is honoured when present and a value disagreeing with it is refused rather than coerced.
    /// Absent, the shorthand stands: a number is Numeric and text is String, which is what a hand-written
    /// configuration carries and all it can express.
    ///
    /// **A Guid is read by `UUID.fromString`, and that is the whole accepted syntax** -- the hyphenated form,
    /// which is what `populateNodeId` emits for one. The brace-wrapped and URN spellings a vendor tool may show
    /// are refused, naming the expected form: the identifiers this has to accept are the ones our own read path
    /// produces, and widening the syntax would add parsing to admit spellings nothing here writes.
    private static @NotNull NodeId requireNodeId(final @NotNull JsonNode node) {
        if (node.isTextual()) {
            return NodeId.parse(node.textValue());
        }
        if (!node.isObject()) {
            throw new IllegalArgumentException("expected a node id as text or an object, got " + node.getNodeType());
        }
        final JsonNode id = node.get("id");
        if (id == null) {
            throw new IllegalArgumentException("expected an object with an 'id'");
        }
        final UShort namespace = Unsigned.ushort(
                node.has("namespaceIndex") ? (int) requireIntegral(node.get("namespaceIndex"), 0, 65535) : 0);
        final JsonNode idType = node.get("idType");
        if (idType != null && !idType.isNull()) {
            return switch ((int) requireIntegral(idType, 0, 3)) {
                case 0 -> new NodeId(namespace, Unsigned.uint(requireUInt32Identifier(id)));
                case 1 -> new NodeId(namespace, requireText(id));
                case 2 -> new NodeId(namespace, requireUuidIdentifier(id));
                // Opaque. Base64 because that is how the read path renders one, and how the generated schema
                // describes any ByteString.
                default -> new NodeId(namespace, ByteString.of(requireBase64(id)));
            };
        }
        if (id.isIntegralNumber()) {
            return new NodeId(namespace, Unsigned.uint(requireUInt32Identifier(id)));
        }
        if (!id.isTextual()) {
            throw new IllegalArgumentException("expected 'id' to be a number or a string, got " + id.getNodeType());
        }
        return new NodeId(namespace, id.textValue());
    }

    /// A numeric identifier, range-checked on the value as written.
    ///
    /// Through `requireIntegral`, not `Unsigned.uint(id.longValue())`: the narrowing would happen before Milo's
    /// own range check, so an identifier wider than 64 bits wraps into the range instead of being refused --
    /// 2^64 arriving as node `i=0`, a different node than the caller named.
    private static long requireUInt32Identifier(final @NotNull JsonNode id) {
        return requireIntegral(id, 0, 4294967295L);
    }

    private static @NotNull UUID requireUuidIdentifier(final @NotNull JsonNode id) {
        final String text = requireText(id);
        try {
            return UUID.fromString(text);
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "expected 'id' to be a Guid as 8-4-4-4-12 hexadecimal digits, got '" + text + "'");
        }
    }

    /// A qualified name -- a namespace index and a name -- from either the object or a bare string.
    ///
    /// A bare string means namespace 0, which is how the read path renders one: it omits `namespaceIndex`
    /// when it is zero.
    private static @NotNull QualifiedName requireQualifiedName(final @NotNull JsonNode node) {
        if (node.isTextual()) {
            return new QualifiedName(0, node.textValue());
        }
        if (!node.isObject()) {
            throw new IllegalArgumentException("expected a string or an object with 'name', got " + node.getNodeType());
        }
        final JsonNode name = node.get("name");
        if (name == null || !name.isTextual()) {
            throw new IllegalArgumentException("expected an object with a string 'name'");
        }
        final int namespace =
                node.has("namespaceIndex") ? (int) requireIntegral(node.get("namespaceIndex"), 0, 65535) : 0;
        return new QualifiedName(namespace, name.textValue());
    }

    private static double requireNumber(final @NotNull JsonNode node) {
        if (!node.isNumber()) {
            throw new IllegalArgumentException("expected a number, got " + node.getNodeType());
        }
        return node.doubleValue();
    }

    /// The range check a Float argument passes through, for the same reason the integral ones have theirs:
    /// the narrowing cast at the call site does not refuse a value it cannot hold.
    ///
    /// A `double` beyond the float range casts to an infinity rather than throwing, so `1e39` would reach the
    /// device as `+Infinity` -- a value the caller never asked for and cannot distinguish from a real one.
    /// The generated schema states this range too, so a request usually fails the gate first; this is the
    /// check for when it does not, which the reference promises for every number.
    ///
    /// An infinity the caller sent deliberately is refused by the same comparison. That is a limitation of
    /// the write path rather than of this method -- the southbound transform runs as JavaScript and JSON has
    /// no infinity to carry one in.
    private static float requireFloat(final @NotNull JsonNode node) {
        final double value = requireNumber(node);
        final float narrowed = (float) value;
        if (Float.isInfinite(narrowed) && !Double.isInfinite(value)) {
            throw new IllegalArgumentException("expected a value in ["
                    + -Float.MAX_VALUE
                    + ", "
                    + Float.MAX_VALUE
                    + "], got "
                    + value
                    + ". A Float argument cannot hold it: the narrowing would send the device an infinity"
                    + " instead of the value requested");
        }
        return narrowed;
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
