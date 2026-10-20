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
package com.hivemq.edge.adapters.opcua.southbound;

import static com.hivemq.edge.adapters.opcua.Constants.ENUM_NAME;
import static com.hivemq.edge.adapters.opcua.Constants.ENUM_VALUE;
import static com.hivemq.edge.adapters.opcua.Constants.LOCALIZED_TEXT_LOCALE;
import static com.hivemq.edge.adapters.opcua.Constants.LOCALIZED_TEXT_TEXT;
import static com.hivemq.edge.adapters.opcua.Constants.NODE_ID_IDENTIFIER;
import static com.hivemq.edge.adapters.opcua.Constants.NODE_ID_NAMESPACE_INDEX;
import static com.hivemq.edge.adapters.opcua.Constants.NODE_ID_TYPE;
import static com.hivemq.edge.adapters.opcua.Constants.QUALIFIED_NAME_NAME;
import static com.hivemq.edge.adapters.opcua.Constants.QUALIFIED_NAME_NAMESPACE_INDEX;
import static com.hivemq.edge.adapters.opcua.Constants.STATUS_CODE_CODE;
import static com.hivemq.edge.adapters.opcua.Constants.STATUS_CODE_SYMBOL;

import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.edge.adapters.opcua.method.Guids;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.core.types.DynamicEnumType;
import org.eclipse.milo.opcua.sdk.core.types.DynamicStructType;
import org.eclipse.milo.opcua.sdk.core.typetree.DataType;
import org.eclipse.milo.opcua.sdk.core.typetree.DataTypeTree;
import org.eclipse.milo.opcua.stack.core.OpcUaDataType;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.encoding.EncodingContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExtensionObject;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.Matrix;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.XmlElement;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned;
import org.eclipse.milo.opcua.stack.core.types.enumerated.StructureType;
import org.eclipse.milo.opcua.stack.core.types.structured.DataTypeDefinition;
import org.eclipse.milo.opcua.stack.core.types.structured.EnumDefinition;
import org.eclipse.milo.opcua.stack.core.types.structured.EnumField;
import org.eclipse.milo.opcua.stack.core.types.structured.StructureDefinition;
import org.eclipse.milo.opcua.stack.core.types.structured.StructureField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// Turns the JSON a caller sends southbound into the value Milo puts on the wire, for any DataType Edge can encode.
///
/// The southbound counterpart of `OpcUaToJsonConverter`, and meant to be the only one: a value write, a method
/// argument and a condition update all carry OPC-UA values of some DataType, and a value of a given DataType must be
/// accepted in the same forms whichever route it takes. One converter is how that holds, rather than several that
/// are asserted to agree.
///
/// ## What it is told, and nothing else
///
/// A DataType node id and a value rank. That is everything a variable, a method argument and a structure field
/// declare about a value, so it is everything this needs. It knows nothing about the route: a condition's event id is
/// a ByteString here like any other, and what makes it required for `Acknowledge` and meaningless for `Suppress` is
/// the method's contract, which stays with the caller.
///
/// ## What it encodes
///
///   - **The 21 builtin types with a JSON form**, and every Simple DataType reducing to one -- `Duration` is a
///     Double. The six structured builtins take the object the read path publishes *and* a shorthand; see
///     [#builtin].
///   - **Enumerations**, as the Int32 they are carried as. A caller may send the number, the name, or the
///     `{"name": ..., "value": ...}` object the read path publishes for an enumeration inside a structure.
///   - **Structures**, including those with optional fields, built from the server's own StructureDefinition and
///     encoded by Milo's dynamic codec. Fields may themselves be any of the above, arrays of them, or structures.
///
/// ## What it refuses
///
///   - `Variant`, `DataValue` and `DiagnosticInfo` -- the "any value at all" types, where the declaration tells a
///     caller nothing to write against.
///   - The abstract `Structure` and `Enumeration` themselves, and any abstract subtype, since JSON cannot say which
///     concrete type is meant.
///   - Unions. Milo can encode one, but the read path does not publish one, so there is no form to accept back.
///
/// ## The server's type descriptions are read only when a value needs them
///
/// A builtin DataType is identified by its node id alone -- OPC 10000-6 fixes the 25 identifiers -- so an `Int32`
/// is converted without the server. Only a Simple DataType, an enumeration or a structure reaches for the
/// hierarchy. Both suppliers are therefore called lazily: reading the hierarchy eagerly made a value nobody asked
/// about decide whether a write could happen at all (Miguel's R6-3 on EDG-1087).
public final class JsonToOpcUaValueConverter {

    /// 2^64 - 1, the largest UInt64. Milo states it as `ULong.MAX_VALUE`, but that constant is a `BigInteger` the
    /// schema generator cannot carry -- it writes bounds through a `Number` the SDK renders with `longValue()` --
    /// so a UInt64 value's schema states no range and this is the only gate the value passes.
    private static final @NotNull BigInteger UINT64_MAX = BigInteger.TWO.pow(64).subtract(BigInteger.ONE);

    /// 2^32 - 1, the largest UInt32. Also the ceiling on a numeric node identifier, which OPC 10000-6 section
    /// 5.2.2.2 defines as a UInt32.
    private static final long UINT32_MAX = 4294967295L;

    /// 2^16 - 1, the largest UInt16. Also the ceiling on a namespace index, which OPC 10000-3 section 8.2.1
    /// defines as a UInt16.
    private static final long UINT16_MAX = 65535L;

    /// 2^8 - 1, the largest Byte. OPC-UA's `Byte` is unsigned, unlike Java's.
    private static final long UINT8_MAX = 255L;

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
    /// **Every southbound route passes through it.** A southbound mapping always has a field mapping -- the REST
    /// API substitutes `value -> value` when none is given -- and the writing service refuses to start without
    /// Data Hub. So a value write is rounded exactly as a method argument is, and the limit applies to both.
    ///
    /// **Why refuse rather than detect.** There is nothing left to detect. `...993` and `...992` both arrive as
    /// `...992`, so a corrupted value and a legitimate one are byte-identical here -- no residue, no flag, no
    /// lost-precision bit. The only honest question this code can ask is whether the value *could* have crossed
    /// intact, and the only honest answer to "no" is a refusal. A caller then learns the limit instead of a PLC
    /// acting on a number nobody requested.
    ///
    /// **What this does and does not add.** Values near the top of a type's range already fail: they round
    /// *outward*, past the type's own maximum, and the range check catches them. This closes the interior --
    /// 2^53 up to the type maximum -- where a value rounds to a different but perfectly legal number and passed
    /// every gate. Only `Int64` and `UInt64` can reach it; every narrower type is bounded far below.
    ///
    /// **Not fixable here, and not fixable in the transform either.** The generated script receives an already
    /// parsed payload and cannot reach the original text -- the wrapper builds its argument as
    /// `{...publishData, payload}`, which replaces the raw string. A real fix belongs at Data Hub's parse
    /// boundary, which would affect every data policy in every deployment, so it is not this adapter's to make.
    /// Raised by Miguel as M3 on EDG-1087.
    private static final @NotNull BigInteger PRECISION_LIMIT = BigInteger.TWO.pow(53);

    private final @NotNull Supplier<DataTypeTree> tree;
    private final @NotNull Supplier<EncodingContext> context;

    /// @param tree    the server's DataType hierarchy. Called only for a value whose DataType is not a builtin; it
    ///                may throw, and the value is then refused as one whose type cannot be resolved.
    /// @param context the encoding context a structure is encoded in. Called only for a structure.
    public JsonToOpcUaValueConverter(
            final @NotNull Supplier<DataTypeTree> tree, final @NotNull Supplier<EncodingContext> context) {
        this.tree = tree;
        this.context = context;
    }

    /// The converter for a connected client: its DataType hierarchy and its dynamic encoding context.
    ///
    /// The dynamic context is the one the read path decodes structures with, and it creates a codec for any
    /// DataType the hierarchy describes on first use, so nothing has to be registered before a structure is
    /// encoded.
    public static @NotNull JsonToOpcUaValueConverter of(final @NotNull OpcUaClient client) {
        return new JsonToOpcUaValueConverter(
                () -> {
                    try {
                        return client.getDataTypeTree();
                    } catch (final UaException e) {
                        throw new IllegalStateException(e.getMessage(), e);
                    }
                },
                () -> {
                    try {
                        return client.getDynamicEncodingContext();
                    } catch (final UaException e) {
                        throw new IllegalStateException(e.getMessage(), e);
                    }
                });
    }

    /// A converter for values of builtin types only.
    ///
    /// For a caller with no server at hand: anything that needs the hierarchy is refused as unresolvable rather
    /// than guessed at.
    public static @NotNull JsonToOpcUaValueConverter builtinOnly() {
        return new JsonToOpcUaValueConverter(
                () -> {
                    throw new IllegalStateException("the server's DataType hierarchy is not available");
                },
                () -> {
                    throw new IllegalStateException("no encoding context is available");
                });
    }

    // region The public contract

    /// Why a value could not be converted.
    ///
    /// Carries the reason as a fragment the caller completes, because only the caller can name what was being
    /// converted -- "argument 'recipe' of method 'Load'", "tag 'setpoint'". The kind says how to complete it.
    public static final class ConversionException extends RuntimeException {

        /// What went wrong, in the terms a caller's message needs.
        public enum Fault {
            /// The DataType is one Edge cannot encode. `reason()` says why; `expected()` names the type.
            UNSUPPORTED,
            /// The value nests differently from what the value rank allows. `reason()` is a predicate for the
            /// value's owner: "is declared as ...".
            SHAPE,
            /// The value is not one of the declared type. `expected()` names the type with its array depth.
            VALUE
        }

        private final @NotNull Fault fault;
        private final @NotNull String expected;
        private final @NotNull String reason;

        ConversionException(final @NotNull Fault fault, final @NotNull String expected, final @NotNull String reason) {
            super(reason);
            this.fault = fault;
            this.expected = expected;
            this.reason = reason;
        }

        public @NotNull Fault fault() {
            return fault;
        }

        /// The declared type, as a caller would name it: `Int32`, `Int32[][]`, `RecipeType`.
        public @NotNull String expected() {
            return expected;
        }

        /// What was wrong, without the owner's name. For a value inside a structure it starts with the field's
        /// path: `field 'steps[2].temperature': expected a number, got STRING`.
        public @NotNull String reason() {
            return reason;
        }
    }

    /// Converts one value to the `Variant` that carries it.
    ///
    /// @param value     the JSON value, never null -- an absent value is the caller's to handle, since what it
    ///                  means differs by route.
    /// @param dataType  the declared DataType node id.
    /// @param valueRank the declared value rank; null is read as scalar, which is what OPC 10000-3 makes the
    // default.
    /// @throws ConversionException when the DataType cannot be encoded, or the value does not fit it.
    public @NotNull Variant toVariant(
            final @NotNull JsonNode value, final @NotNull NodeId dataType, final @Nullable Integer valueRank) {
        return toVariant(value, dataType, valueRank, null);
    }

    /// As above, with the builtin the DataType reduces to when the caller already knows it.
    ///
    /// A method signature resolves each argument's builtin during the browse that discovered the method, so a
    /// `Duration` argument is converted at call time without asking the hierarchy again. A hint of `Variant` or
    /// `ExtensionObject` says nothing usable -- an enumeration reduces to the first, a structure to the second --
    /// so those, like a null hint, are resolved from the DataType itself.
    public @NotNull Variant toVariant(
            final @NotNull JsonNode value,
            final @NotNull NodeId dataType,
            final @Nullable Integer valueRank,
            final @Nullable OpcUaDataType knownBuiltin) {

        final Kind kind = knownBuiltin != null && isConvertibleBuiltin(knownBuiltin)
                ? new Kind.Builtin(knownBuiltin)
                : resolve(dataType);
        if (kind instanceof final Kind.Unsupported unsupported) {
            throw new ConversionException(
                    ConversionException.Fault.UNSUPPORTED, unsupported.name(), unsupported.reason());
        }

        final int dimensions = depthFor(valueRank, value);
        try {
            if (dimensions == 0) {
                return Variant.of(scalar(kind, value, false));
            }
            // One slot per declared dimension, each `UNKNOWN_LENGTH` until the first list at that level fixes
            // it. A real length may legitimately be 0 -- `[[], []]` is a rectangular 2x0 value -- so the
            // sentinel has to be a value no array can have rather than zero.
            final int[] lengthPerLevel = new int[dimensions];
            Arrays.fill(lengthPerLevel, UNKNOWN_LENGTH);
            final Object array = toJavaArray(kind, value, dimensions, lengthPerLevel, -1, false);
            // A one-dimensional array goes into a Variant as itself; anything deeper has to be wrapped in a
            // Matrix, which is Milo's representation of a multi-dimensional value and what `Variant.of` demands
            // ("Multi-dimensional arrays must be wrapped in Matrix"). The wrapper carries the dimension lengths,
            // which it derives from the nested array it is given.
            return Variant.of(dimensions == 1 ? array : new Matrix(array));
            // Every runtime failure of a conversion, not only IllegalArgumentException. A conversion reaches for
            // whatever parser the type needs and those do not agree on an exception type: `Instant.parse` throws
            // DateTimeParseException, which is a RuntimeException and not an IllegalArgumentException, so a
            // timestamp written without a zone offset -- `2026-10-08T12:00:00` -- escaped the method-argument
            // converter entirely and the caller got no answer until the write timed out.
        } catch (final AtPath e) {
            throw new ConversionException(
                    ConversionException.Fault.VALUE,
                    describeExpected(kind, dimensions),
                    "field '" + e.path + "': " + e.reason);
        } catch (final RuntimeException e) {
            throw new ConversionException(
                    ConversionException.Fault.VALUE,
                    describeExpected(kind, dimensions),
                    String.valueOf(e.getMessage()));
        }
    }

    // endregion

    // region What a DataType is

    /// What a DataType is, as far as encoding a value of it is concerned.
    private sealed interface Kind {

        /// One of the 21 builtins with a JSON form.
        record Builtin(@NotNull OpcUaDataType type) implements Kind {}

        /// A concrete subtype of `Enumeration`, carried as an Int32. The definition names its values; a server
        /// that predates OPC UA 1.04 may describe them only through `EnumStrings`, and then this is null and
        // only
        /// the number is accepted.
        record Enumeration(
                @NotNull DataType dataType, @Nullable EnumDefinition definition) implements Kind {}

        /// A concrete structure, with or without optional fields.
        record Structure(
                @NotNull DataType dataType, @NotNull StructureDefinition definition) implements Kind {}

        /// Anything else, with the reason.
        record Unsupported(@NotNull String name, @NotNull String reason) implements Kind {}
    }

    private @NotNull Kind resolve(final @NotNull NodeId dataTypeId) {
        // The static table first, and for a builtin that is the whole of it. See the class comment.
        final OpcUaDataType tabulated = OpcUaDataType.fromNodeId(dataTypeId);
        if (tabulated != null) {
            return kindOfBuiltin(tabulated, tabulated.name());
        }

        final DataTypeTree hierarchy;
        try {
            hierarchy = tree.get();
        } catch (final RuntimeException e) {
            return new Kind.Unsupported(
                    dataTypeId.toParseableString(),
                    "its DataType could not be resolved, because the server's type hierarchy could not be read: "
                            + e.getMessage());
        }

        final DataType dataType = hierarchy.getDataType(dataTypeId);
        if (dataType == null) {
            return new Kind.Unsupported(
                    dataTypeId.toParseableString(), "its DataType is not in the server's type hierarchy");
        }
        final String name = nameOf(dataType);
        if (Boolean.TRUE.equals(dataType.isAbstract())) {
            return new Kind.Unsupported(
                    name, "its DataType is abstract, and a value cannot say which concrete subtype it is");
        }

        // The same order Milo's own structure codec decides a field's encoding in (`FieldUtil.createFieldHints`):
        // enumeration, then structure, then whatever builtin the hierarchy reduces the type to. A value converted
        // here and one converted as a structure's field are therefore the same value.
        final DataTypeDefinition definition = dataType.getDataTypeDefinition();
        if (hierarchy.isEnumType(dataTypeId)) {
            return new Kind.Enumeration(dataType, definition instanceof final EnumDefinition e ? e : null);
        }
        if (hierarchy.isStructType(dataTypeId)) {
            if (!(definition instanceof final StructureDefinition structure)) {
                return new Kind.Unsupported(
                        name, "it is a structure the server does not describe, so its fields are unknown");
            }
            final StructureType structureType = structure.getStructureType();
            if (structureType == StructureType.Union || structureType == StructureType.UnionWithSubtypedValues) {
                return new Kind.Unsupported(name, "it is a union, which this version does not encode");
            }
            return new Kind.Structure(dataType, structure);
        }
        return kindOfBuiltin(hierarchy.getBuiltinType(dataTypeId), name);
    }

    private static @NotNull Kind kindOfBuiltin(final @NotNull OpcUaDataType builtin, final @NotNull String name) {
        if (isConvertibleBuiltin(builtin)) {
            return new Kind.Builtin(builtin);
        }
        return new Kind.Unsupported(
                name,
                switch (builtin) {
                    case ExtensionObject ->
                        "it is the abstract Structure, and a value cannot say which concrete structure it is";
                    case Variant, DataValue, DiagnosticInfo ->
                        "it reduces to " + builtin.name() + ", which accepts a value of any type and so states none";
                    default -> "it reduces to " + builtin.name() + ", which this version does not encode";
                });
    }

    private static boolean isConvertibleBuiltin(final @NotNull OpcUaDataType builtin) {
        return switch (builtin) {
            case ExtensionObject, Variant, DataValue, DiagnosticInfo -> false;
            default -> true;
        };
    }

    private static @NotNull String nameOf(final @NotNull DataType dataType) {
        final String name = dataType.getBrowseName().getName();
        return name != null ? name : dataType.getNodeId().toParseableString();
    }

    private static @NotNull String nameOf(final @NotNull Kind kind) {
        return switch (kind) {
            case Kind.Builtin builtin -> builtin.type().name();
            case Kind.Enumeration enumeration -> nameOf(enumeration.dataType());
            case Kind.Structure structure -> nameOf(structure.dataType());
            case Kind.Unsupported unsupported -> unsupported.name();
        };
    }

    /// What the value wanted, for an error message. A rank is worth naming: "expects Int32" against a value that is
    /// a perfectly good Int32 but should have been wrapped in an array reads as a contradiction.
    private static @NotNull String describeExpected(final @NotNull Kind kind, final int dimensions) {
        return nameOf(kind) + "[]".repeat(dimensions);
    }

    // endregion

    // region Shape

    /// How deeply to nest the value: a declared depth where the declaration fixed one, otherwise the depth the
    /// value itself has.
    ///
    /// OPC 10000-3 table 28 gives a value rank six meanings, and only three of them fix a depth:
    ///
    ///   - `n >= 1` -- an array with exactly n dimensions. The declaration decides, and a value that nests
    ///     differently is refused: a flat list for a two-dimensional value is a mistake worth naming rather
    ///     than something to reshape.
    ///   - `-1` (Scalar) -- not an array.
    ///   - `0` (OneOrMoreDimensions) -- an array, of a depth the server declined to fix.
    ///   - `-2` (Any) -- a scalar or an array of any depth.
    ///   - `-3` (ScalarOrOneDimension) -- a scalar or a one-dimensional array.
    ///
    /// For the last three the value is the only thing that knows, so the depth is counted from it and then checked
    /// against what the rank permits. A rank-0 value accepts nothing but arrays, so treating it as a scalar refused
    /// every legal value. A caller that wants the value's own shape to decide, whatever was declared, passes `-2`.
    ///
    /// Whatever passes here may still be wrong about the real declaration, and the device is left to say so.
    private static int depthFor(final @Nullable Integer rank, final @NotNull JsonNode value) {
        if (rank == null || rank == ValueRanks.Scalar) {
            return 0;
        }
        if (rank >= 1) {
            return rank;
        }

        final int suppliedDepth = depthOf(value);
        if (rank == ValueRanks.OneOrMoreDimensions && suppliedDepth == 0) {
            throw new ConversionException(
                    ConversionException.Fault.SHAPE,
                    "an array",
                    "is declared as an array of one or more dimensions, so a single value is not one of its "
                            + "values. Send an array");
        }
        if (rank == ValueRanks.ScalarOrOneDimension && suppliedDepth > 1) {
            throw new ConversionException(
                    ConversionException.Fault.SHAPE,
                    "a single value or a one-dimensional array",
                    "is declared as a single value or a one-dimensional array, and the value supplied nests "
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

    /// Builds a nested Java array of the element class, one level per dimension.
    ///
    /// The depth comes from the declaration rather than from how deeply the JSON happens to nest, so a flat list
    /// supplied for a two-dimensional value is reported as the wrong shape instead of being encoded as something
    /// the device did not ask for. Dimension *lengths* are not checked against the declaration -- the generated
    /// schema states them, and the device enforces them; duplicating that would mean two places to keep in
    /// agreement.
    ///
    /// **Rectangularity is a different question from length, and is checked here.** A value whose rows disagree
    /// does not reach the device as sent: `Matrix` takes each dimension's length from the first row it finds and
    /// flattens against those, so a short row makes it read beyond that row's end and a long row makes it stop
    /// early -- dropping elements. Its own consistency check is a Java `assert`, disabled at runtime, so it
    /// reports nothing and the device acts on the truncated value.
    ///
    /// `lengthPerLevel` carries one length per dimension, each `UNKNOWN_LENGTH` until the first list at that
    /// depth fixes it. **Every later list at the same depth is measured against that one, whichever branch it
    /// sits in.** Comparing a row only against its own first sibling misses a disagreement between branches: in
    /// `[[[1]], [[2,3]]]` the rows `[1]` and `[2,3]` have different parents, so nothing compared them, `Matrix`
    /// took the inner length as 1, and the `3` was silently dropped. `rowIndex` is this node's position among its
    /// siblings, or `-1` for the outermost value, which has none. It carries no logic and is only there so a
    /// refusal can name the row at fault.
    ///
    /// `member` says whether the elements go into a structure, where Milo's codec wants the decoded forms, or onto
    /// the wire directly, where it wants the encoded ones -- see [#scalar].
    private @NotNull Object toJavaArray(
            final @NotNull Kind kind,
            final @NotNull JsonNode node,
            final int remainingDimensions,
            final int @NotNull [] lengthPerLevel,
            final int rowIndex,
            final boolean member) {

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

        final Class<?> elementClass = elementClassOf(kind, member);
        if (remainingDimensions == 1) {
            final Object[] elements = (Object[]) Array.newInstance(elementClass, node.size());
            for (int i = 0; i < node.size(); i++) {
                elements[i] = element(kind, node.get(i), i, member);
            }
            return elements;
        }

        // A dimension above the innermost: each element is itself an array, so the component type has one array
        // level per remaining inner dimension -- `Integer[]` at depth 2, `Integer[][]` at depth 3. Building it
        // with an int[] of zero lengths asks the JDK for exactly that class without allocating anything.
        final Class<?> componentType = Array.newInstance(elementClass, new int[remainingDimensions - 1])
                .getClass();
        final Object[] rows = (Object[]) Array.newInstance(componentType, node.size());
        for (int i = 0; i < node.size(); i++) {
            rows[i] = toJavaArray(kind, node.get(i), remainingDimensions - 1, lengthPerLevel, i, member);
        }
        return rows;
    }

    /// One array element. A builtin element's failure is reported as before, without a position, because the
    /// message already names what was wrong; an element of an enumeration or a structure is named by its index,
    /// since a structure's failure is about one of its fields and needs the way there.
    private @NotNull Object element(
            final @NotNull Kind kind, final @NotNull JsonNode node, final int index, final boolean member) {
        if (kind instanceof Kind.Builtin) {
            return scalar(kind, node, member);
        }
        try {
            return scalar(kind, node, member);
        } catch (final AtPath e) {
            throw new AtPath("[" + index + "]" + (e.path.startsWith("[") ? "" : ".") + e.path, e.reason);
        } catch (final RuntimeException e) {
            throw new AtPath("[" + index + "]", String.valueOf(e.getMessage()));
        }
    }

    /// The Java class one element has: the builtin's backing class, or for an enumeration and a structure the
    /// decoded form inside a structure and the encoded form on the wire.
    private static @NotNull Class<?> elementClassOf(final @NotNull Kind kind, final boolean member) {
        return switch (kind) {
            case Kind.Builtin builtin -> builtin.type().getBackingClass();
            case Kind.Enumeration ignored -> member ? DynamicEnumType.class : Integer.class;
            case Kind.Structure ignored -> member ? DynamicStructType.class : ExtensionObject.class;
            case Kind.Unsupported ignored -> throw new IllegalStateException("unsupported kinds are refused first");
        };
    }

    // endregion

    // region One value

    /// One value of the given kind.
    ///
    /// **An enumeration and a structure have two Java forms, and which one depends on where the value goes.** On
    /// the wire -- as a variable's value or a method argument -- an enumeration is an `Integer` and a structure an
    /// `ExtensionObject`. Inside a structure, Milo's codec wants the decoded forms, `DynamicEnumType` and
    /// `DynamicStructType`, and encodes them itself as part of the enclosing structure. `member` chooses.
    private @NotNull Object scalar(final @NotNull Kind kind, final @NotNull JsonNode node, final boolean member) {
        return switch (kind) {
            case Kind.Builtin builtin -> builtin(builtin.type(), node);
            case Kind.Enumeration enumeration -> {
                final int value = enumValue(enumeration, node);
                yield member ? new DynamicEnumType(enumeration.dataType(), value) : (Object) value;
            }
            case Kind.Structure structure -> {
                final DynamicStructType value = structure(structure, node);
                yield member ? value : ExtensionObject.encode(context.get(), value);
            }
            case Kind.Unsupported ignored -> throw new IllegalStateException("unsupported kinds are refused first");
        };
    }

    /// A failure inside a structure, with the path to the field that failed.
    ///
    /// Thrown and rethrown as the failure travels outwards, each level prefixing its own field name, so a refusal
    /// of a deeply nested value names the whole way to it: `steps[2].temperature`.
    private static final class AtPath extends RuntimeException {
        private final @NotNull String path;
        private final @NotNull String reason;

        AtPath(final @NotNull String path, final @NotNull String reason) {
            super(path + ": " + reason, null, false, false);
            this.path = path;
            this.reason = reason;
        }
    }

    // endregion

    // region Structures

    /// A structure, from a JSON object keyed by field name -- the form the read path publishes.
    ///
    /// ## Which fields must be present
    ///
    /// Every field the definition does not mark optional. An optional field -- only a `StructureWithOptionalFields`
    /// has them -- may be absent or `null`, and is then left out, which is how Milo's codec knows to clear its bit
    /// in the encoding mask. A required field that is absent is refused rather than defaulted: there is no value
    /// Edge could pick that the device would not act on.
    ///
    /// **Optional fields may be left out only at the end.** The encoding mask has one bit per optional field, in
    /// declaration order. Milo's `DynamicStructCodec` (1.1.x) numbers those bits by the optional fields *present*,
    /// so an absent optional field before a present one shifts the later field onto the absent one's bit:
    /// `{x, a?, b?}` sent as `{"x": 1, "b": 42}` arrives at the device as `{x=1, a=0}`. Leaving out only a tail
    // keeps every present field on its own bit, so a value that leaves out a
    /// field and gives a later one is refused. Milo fixed the numbering after 1.1.8 (commit 00f996ded, "Preserve
    /// mask positions of absent optional fields"); once Edge uses a release with the fix, any optional field may
    /// be left out.
    ///
    /// ## Which fields may be present
    ///
    /// Only the definition's. A field nobody reads is a field the caller believes they sent: `{"temprature": 80}`
    /// would otherwise leave `temperature` to the required-field check at best, and for an optional field would
    /// silently send the structure without it.
    ///
    /// ## How a field is converted
    ///
    /// By the same rules as a top-level value, with the field's own DataType and value rank. A field's value rank
    /// is always a definite one -- OPC 10000-3 section 8.51 allows only -1 and n >= 1 in a structure -- so no
    /// shape is inferred.
    private @NotNull DynamicStructType structure(final @NotNull Kind.Structure kind, final @NotNull JsonNode node) {
        if (!node.isObject()) {
            throw new IllegalArgumentException(
                    "expected an object with the fields of " + nameOf(kind.dataType()) + ", got " + node.getNodeType());
        }
        final StructureField[] fields =
                kind.definition().getFields() != null ? kind.definition().getFields() : new StructureField[0];
        final boolean optionalFieldsAllowed =
                kind.definition().getStructureType() == StructureType.StructureWithOptionalFields;

        final List<String> declared = new ArrayList<>();
        for (final StructureField field : fields) {
            declared.add(field.getName());
        }
        refuseUnknownFields(node, "a " + nameOf(kind.dataType()), declared.toArray(String[]::new));

        final LinkedHashMap<String, Object> members = new LinkedHashMap<>();
        @Nullable String leftOut = null;
        for (final StructureField field : fields) {
            final String name = field.getName();
            final JsonNode supplied = node.get(name);
            if (supplied == null || supplied.isNull()) {
                if (optionalFieldsAllowed && Boolean.TRUE.equals(field.getIsOptional())) {
                    if (leftOut == null) {
                        leftOut = name;
                    }
                    continue;
                }
                throw new AtPath(name, "is required, and the value supplied does not carry it");
            }
            if (leftOut != null) {
                throw new AtPath(
                        leftOut,
                        "is left out, but the later field '" + name
                                + "' is given; a structure may leave out optional fields only at its end");
            }
            try {
                members.put(name, fieldValue(field, supplied));
            } catch (final AtPath e) {
                throw new AtPath(name + (e.path.startsWith("[") ? "" : ".") + e.path, e.reason);
            } catch (final RuntimeException e) {
                throw new AtPath(name, String.valueOf(e.getMessage()));
            }
        }
        return new DynamicStructType(kind.dataType(), members);
    }

    private @NotNull Object fieldValue(final @NotNull StructureField field, final @NotNull JsonNode supplied) {
        final Kind kind = resolve(field.getDataType());
        if (kind instanceof final Kind.Unsupported unsupported) {
            throw new IllegalArgumentException(
                    "has the type " + unsupported.name() + ", which cannot be encoded: " + unsupported.reason());
        }
        final Integer rank = field.getValueRank();
        if (rank == null || rank == ValueRanks.Scalar) {
            return scalar(kind, supplied, true);
        }
        if (rank < 1) {
            throw new IllegalArgumentException("has the value rank " + rank + ", which a structure field cannot have");
        }
        final int[] lengthPerLevel = new int[rank];
        Arrays.fill(lengthPerLevel, UNKNOWN_LENGTH);
        final Object array = toJavaArray(kind, supplied, rank, lengthPerLevel, -1, true);
        return rank == 1 ? array : new Matrix(array);
    }

    // endregion

    // region Enumerations

    /// An enumeration's value, from the number, the name, or the object the read path publishes.
    ///
    /// ## Three forms, because each has a reason to exist
    ///
    ///   - **The number** is what the wire carries, and the only form available when the server describes the
    ///     enumeration only through `EnumStrings`.
    ///   - **The name** is what a person writing a configuration means: `"Running"` rather than looking up `2`.
    ///   - **The object** `{"name": "Running", "value": 2}` is what the read path publishes for an enumeration
    ///     inside a structure, so a structure read from a device can be sent back unchanged.
    ///
    /// ## The value is checked against the definition when there is one
    ///
    /// An Int32 the enumeration does not define is not one of its values, and a device given one may act on it
    /// rather than refuse it. When the object carries both fields and they disagree, it is refused rather than
    /// resolved in favour of either: a caller who edited one and not the other meant something, and which one is
    /// not knowable here.
    private static int enumValue(final @NotNull Kind.Enumeration kind, final @NotNull JsonNode node) {
        if (node.isIntegralNumber()) {
            return requireDefined(kind, (int) requireIntegral(node, Integer.MIN_VALUE, Integer.MAX_VALUE));
        }
        if (node.isTextual()) {
            return valueNamed(kind, node.textValue());
        }
        if (!node.isObject()) {
            throw new IllegalArgumentException("expected the value as a number, its name as a string, or an object "
                    + "with '"
                    + ENUM_VALUE
                    + "' or '"
                    + ENUM_NAME
                    + "', got "
                    + node.getNodeType());
        }
        refuseUnknownFields(node, "an enumeration value", ENUM_NAME, ENUM_VALUE);
        final JsonNode value = node.get(ENUM_VALUE);
        final String name = optionalText(node, ENUM_NAME);
        if (value == null || value.isNull()) {
            if (name == null) {
                throw new IllegalArgumentException(
                        "expected an object with '" + ENUM_VALUE + "' or '" + ENUM_NAME + "'");
            }
            return valueNamed(kind, name);
        }
        final int number = requireDefined(kind, (int) requireIntegral(value, Integer.MIN_VALUE, Integer.MAX_VALUE));
        if (name != null && kind.definition() != null && valueNamed(kind, name) != number) {
            throw new IllegalArgumentException("'" + ENUM_NAME + "' is " + name + " and '" + ENUM_VALUE + "' is "
                    + number + ", which " + nameOf(kind.dataType()) + " gives to different values. Send one of them,"
                    + " or both agreeing");
        }
        return number;
    }

    private static int requireDefined(final @NotNull Kind.Enumeration kind, final int value) {
        final EnumField[] fields = fieldsOf(kind);
        if (fields == null) {
            return value;
        }
        for (final EnumField field : fields) {
            if (field.getValue() != null && field.getValue() == value) {
                return value;
            }
        }
        throw new IllegalArgumentException(
                nameOf(kind.dataType()) + " defines no value " + value + ". It defines " + describeValues(fields));
    }

    private static int valueNamed(final @NotNull Kind.Enumeration kind, final @NotNull String name) {
        final EnumField[] fields = fieldsOf(kind);
        if (fields == null) {
            throw new IllegalArgumentException("the server does not describe the names of "
                    + nameOf(kind.dataType())
                    + ", so only its number is accepted, not '"
                    + name
                    + "'");
        }
        for (final EnumField field : fields) {
            if (name.equals(field.getName()) && field.getValue() != null) {
                return field.getValue().intValue();
            }
        }
        throw new IllegalArgumentException(
                nameOf(kind.dataType()) + " has no value named '" + name + "'. It defines " + describeValues(fields));
    }

    private static EnumField @Nullable [] fieldsOf(final @NotNull Kind.Enumeration kind) {
        final EnumDefinition definition = kind.definition();
        return definition != null ? definition.getFields() : null;
    }

    private static @NotNull String describeValues(final EnumField @NotNull [] fields) {
        final StringBuilder described = new StringBuilder();
        for (final EnumField field : fields) {
            if (field == null) {
                continue;
            }
            if (!described.isEmpty()) {
                described.append(", ");
            }
            described.append(field.getName()).append('=').append(field.getValue());
        }
        return described.toString();
    }

    // endregion

    // region Builtins

    /// One value of a builtin type.
    ///
    /// The six structured builtins each take the form the northbound converter publishes *and* a simple form, so a
    /// value read from a device can be sent straight back and a caller writing one by hand does not have to
    /// assemble an object.
    private static @NotNull Object builtin(final @NotNull OpcUaDataType type, final @NotNull JsonNode node) {
        return switch (type) {
            case Boolean -> requireBoolean(node);
            case SByte -> (byte) requireIntegral(node, Byte.MIN_VALUE, Byte.MAX_VALUE);
            case Byte -> Unsigned.ubyte(requireIntegral(node, 0, UINT8_MAX));
            case Int16 -> (short) requireIntegral(node, Short.MIN_VALUE, Short.MAX_VALUE);
            case UInt16 -> Unsigned.ushort((int) requireIntegral(node, 0, UINT16_MAX));
            case Int32 -> (int) requireIntegral(node, Integer.MIN_VALUE, Integer.MAX_VALUE);
            case UInt32 -> Unsigned.uint(requireIntegral(node, 0, UINT32_MAX));
            case Int64 -> requireIntegral(node, Long.MIN_VALUE, Long.MAX_VALUE);
            // UInt64 is the one type whose range does not fit a signed long, so it is bounded and built as a
            // BigInteger throughout. Bounding it by Long.MAX_VALUE would refuse the upper half of the type.
            case UInt64 -> Unsigned.ulong(requireInRange(node, BigInteger.ZERO, UINT64_MAX));
            case Float -> requireFloat(node);
            case Double -> requireNumber(node);
            case String -> requireText(node);
            case LocalizedText -> requireLocalizedText(node);
            case NodeId -> requireNodeId(node);
            case ExpandedNodeId -> ExpandedNodeId.parse(requireCanonicalGuidIn(requireText(node)));
            case QualifiedName -> requireQualifiedName(node);
            case StatusCode -> requireStatusCode(node);
            case XmlElement -> new XmlElement(requireText(node));
            case DateTime -> new DateTime(java.time.Instant.parse(requireText(node)));
            case ByteString -> ByteString.of(requireBase64(node));
            case Guid -> Guids.parse(requireText(node));
            default -> throw new IllegalArgumentException(type.name() + " cannot be encoded");
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

    /// The range check every integral value passes through, comparing the value the caller actually sent.
    ///
    /// `JsonNode.longValue()` is not a safe first step: for a JSON integer wider than 64 bits it answers the value
    /// modulo 2^64 rather than refusing, so a check performed on its result validates a different number.
    /// `18446744073709551617` narrows to `1`, which satisfies every Int32 bound and reaches the device as `1`.
    /// `bigIntegerValue()` answers the value as written, so the comparison happens before anything narrows.
    ///
    /// Checked here rather than left to the server, because the narrowing cast at each call site wraps silently --
    /// 300 arriving at a Byte as 44, which the device would act on.
    ///
    /// **The magnitude limit is separate, and it is about the journey rather than the type.** See
    /// [#PRECISION_LIMIT].
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
    /// `symbol` is still a field of the form, so it is accepted and unread; a name that is neither is refused.
    ///
    /// Both forms, for the same reason as the other structured types: a status code read from a device is
    /// published as this object, so requiring the bare number made a published value unsendable.
    private static @NotNull StatusCode requireStatusCode(final @NotNull JsonNode node) {
        if (node.isObject()) {
            refuseUnknownFields(node, "a status code", STATUS_CODE_CODE, STATUS_CODE_SYMBOL);
            final JsonNode code = node.get(STATUS_CODE_CODE);
            if (code == null) {
                throw new IllegalArgumentException("expected an object with a 'code'");
            }
            return new StatusCode(requireIntegral(code, 0, UINT32_MAX));
        }
        return new StatusCode(requireIntegral(node, 0, UINT32_MAX));
    }

    /// A localised text, from either the object the read path publishes or a bare string.
    ///
    /// ## The two forms say different amounts, so they are read differently
    ///
    /// **A bare string becomes English.** It is a form that cannot express a locale at all, so filling one in
    /// is the only way to make it work -- and `ConditionUpdateWriter` does exactly this for an alarm comment, the
    /// same argument of the same standard method reached by the other route. Two routes to one method answering
    /// differently would be worse than either answer.
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
        refuseUnknownFields(node, "a localised text", LOCALIZED_TEXT_LOCALE, LOCALIZED_TEXT_TEXT);
        return new LocalizedText(optionalText(node, LOCALIZED_TEXT_LOCALE), optionalText(node, LOCALIZED_TEXT_TEXT));
    }

    /// Refuses an object form carrying a field the form does not have.
    ///
    /// **A field nobody reads is a field the caller believes they sent.** Every object form here has fields that
    /// may legitimately be absent, so a misspelled one is indistinguishable from an omitted one unless the name
    /// itself is checked: `{"Text": "operator note"}` has no `text`, and without this check it converts to the
    /// null localised text -- which OPC 10000-9 defines as "leave any existing comment alone". The device
    /// accepts it, the call succeeds, and the note is gone. The same silence sends `{"namespace": 2, ...}` to
    /// namespace 0, since `namespace` is not `namespaceIndex`.
    ///
    /// Which fields are optional, and what each means, is a separate question this does not touch. This asks
    /// only whether the caller named a field of this form at all.
    ///
    /// The permitted set is exactly what the read path publishes for that type, which is why these object forms
    /// exist: a value read from a device must be sendable back unchanged.
    ///
    /// Reported by Miguel as R6-2 on EDG-1087, who found the localised-text case on the lab hardware.
    private static void refuseUnknownFields(
            final @NotNull JsonNode node, final @NotNull String form, final @NotNull String... permitted) {

        final Iterator<String> names = node.fieldNames();
        while (names.hasNext()) {
            final String name = names.next();
            if (!Arrays.asList(permitted).contains(name)) {
                throw new IllegalArgumentException(
                        "'" + name + "' is not a field of " + form + ", which takes " + String.join(", ", permitted));
            }
        }
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
    /// Inferring the kind from it silently rewrote every Guid and Opaque identifier read from the device into a
    /// String one, breaking the round trip the object form exists for.
    ///
    /// So `idType` is honoured when present and a value disagreeing with it is refused rather than coerced.
    /// Absent, the shorthand stands: a number is Numeric and text is String, which is what a hand-written
    /// configuration carries and all it can express.
    ///
    /// **A Guid is read by {@link Guids}, which refuses a spelling `UUID.fromString` would pad into a
    /// different Guid** and accepts the wrapped, URN, struct and OID spellings a vendor tool may show
    /// alongside the hyphenated form `populateNodeId` emits. The refusal is what matters here; the extra
    /// spellings only save a transcription step.
    private static @NotNull NodeId requireNodeId(final @NotNull JsonNode node) {
        if (node.isTextual()) {
            return NodeId.parse(requireCanonicalGuidIn(node.textValue()));
        }
        if (!node.isObject()) {
            throw new IllegalArgumentException("expected a node id as text or an object, got " + node.getNodeType());
        }
        refuseUnknownFields(node, "a node id", NODE_ID_NAMESPACE_INDEX, NODE_ID_TYPE, NODE_ID_IDENTIFIER);
        final JsonNode id = node.get(NODE_ID_IDENTIFIER);
        if (id == null) {
            throw new IllegalArgumentException("expected an object with an '" + NODE_ID_IDENTIFIER + "'");
        }
        final UShort namespace = Unsigned.ushort(
                node.has(NODE_ID_NAMESPACE_INDEX)
                        ? (int) requireIntegral(node.get(NODE_ID_NAMESPACE_INDEX), 0, UINT16_MAX)
                        : 0);
        final JsonNode idType = node.get(NODE_ID_TYPE);
        if (idType != null && !idType.isNull()) {
            // The four `idType` values OPC 10000-6 section 5.2.2.2 defines, named rather than bounded, so a fifth
            // value appearing in a later revision of the specification is refused with the value in the message
            // rather than silently read as one of these.
            final long declaredType = requireIntegral(idType, 0, UINT32_MAX);
            return switch ((int) declaredType) {
                case 0 -> new NodeId(namespace, Unsigned.uint(requireUInt32Identifier(id)));
                case 1 -> new NodeId(namespace, requireText(id));
                case 2 -> new NodeId(namespace, requireUuidIdentifier(id));
                // Opaque. Base64 because that is how the read path renders one, and how the generated schema
                // describes any ByteString.
                case 3 -> new NodeId(namespace, ByteString.of(requireBase64(id)));
                default ->
                    throw new IllegalArgumentException("expected 'idType' to be 0 (numeric), 1 (string), "
                            + "2 (guid) or 3 (opaque), got "
                            + declaredType);
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
        return requireIntegral(id, 0, UINT32_MAX);
    }

    private static @NotNull UUID requireUuidIdentifier(final @NotNull JsonNode id) {
        return Guids.parse(requireText(id));
    }

    /// The text form of a node id, with any Guid identifier in it checked before Milo parses it.
    ///
    /// `NodeId.parse` and `ExpandedNodeId.parse` read a `g=` identifier with `UUID.fromString`, which pads a
    /// short group into a different valid Guid -- see {@link Guids}. The identifier is everything after the
    /// first `;`, since only that one separates the namespace from the identifier.
    private static @NotNull String requireCanonicalGuidIn(final @NotNull String text) {
        final int separator = text.indexOf(';');
        final int marker = text.indexOf("g=", separator + 1);
        if (separator < 0 || marker != separator + 1) {
            return text;
        }
        Guids.requireCanonical(text.substring(marker + 2));
        return text;
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
        refuseUnknownFields(node, "a qualified name", QUALIFIED_NAME_NAMESPACE_INDEX, QUALIFIED_NAME_NAME);
        final JsonNode name = node.get(QUALIFIED_NAME_NAME);
        if (name == null || !name.isTextual()) {
            throw new IllegalArgumentException("expected an object with a string '" + QUALIFIED_NAME_NAME + "'");
        }
        final int namespace = node.has(QUALIFIED_NAME_NAMESPACE_INDEX)
                ? (int) requireIntegral(node.get(QUALIFIED_NAME_NAMESPACE_INDEX), 0, UINT16_MAX)
                : 0;
        return new QualifiedName(namespace, name.textValue());
    }

    private static double requireNumber(final @NotNull JsonNode node) {
        if (!node.isNumber()) {
            throw new IllegalArgumentException("expected a number, got " + node.getNodeType());
        }
        return node.doubleValue();
    }

    /// The range check a Float passes through, for the same reason the integral ones have theirs: the narrowing
    /// cast does not refuse a value it cannot hold.
    ///
    /// A `double` beyond the float range casts to an infinity rather than throwing, so `1e39` would reach the
    /// device as `+Infinity` -- a value the caller never asked for and cannot distinguish from a real one.
    ///
    /// An infinity the caller sent deliberately **passes**, which the second half of the condition is there to
    /// allow: what this refuses is a finite value the narrowing would turn into an infinity, not an infinity
    /// that was already one. Reaching it is another matter: the southbound transform runs as JavaScript and JSON
    /// has no infinity to carry one in, so a deliberate infinity does not arrive through the documented path.
    ///
    ///
    /// **The range is the only check.** Any finite value inside it is rounded to the nearest Float, whole or
    /// not: writing to a Float means accepting a Float's precision, and 16777217 becoming 16777216 is the same
    /// loss as 0.1 becoming 0.100000001. Whether a number reaches here as a whole number or as a decimal is
    /// decided by the JavaScript transform's printing, not by the caller, so it cannot carry a caller's intent.
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
                    + ". A Float cannot hold it: the narrowing would send the device an infinity"
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

    // endregion
}
