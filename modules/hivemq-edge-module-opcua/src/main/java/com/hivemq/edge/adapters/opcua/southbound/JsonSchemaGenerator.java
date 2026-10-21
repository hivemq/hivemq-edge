/*
 * Copyright 2023-present HiveMQ GmbH
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

import static com.hivemq.edge.adapters.opcua.northbound.OpcUaToJsonConverter.METADATA_SERVER_PICOSECONDS;
import static com.hivemq.edge.adapters.opcua.northbound.OpcUaToJsonConverter.METADATA_SERVER_TIMESTAMP;
import static com.hivemq.edge.adapters.opcua.northbound.OpcUaToJsonConverter.METADATA_SOURCE_PICOSECONDS;
import static com.hivemq.edge.adapters.opcua.northbound.OpcUaToJsonConverter.METADATA_SOURCE_TIMESTAMP;
import static com.hivemq.edge.adapters.opcua.northbound.OpcUaToJsonConverter.METADATA_STATUS_CODE;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import com.hivemq.adapter.sdk.api.schema.ItemSchemaBuilder;
import com.hivemq.adapter.sdk.api.schema.ObjectSchemaBuilder;
import com.hivemq.adapter.sdk.api.schema.PropertySchemaBuilder;
import com.hivemq.adapter.sdk.api.schema.ScalarType;
import com.hivemq.adapter.sdk.api.schema.Schema;
import com.hivemq.adapter.sdk.api.schema.SchemaBuilder;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationOutput;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.util.NodeIdText;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.typetree.DataType;
import org.eclipse.milo.opcua.sdk.core.typetree.DataTypeTree;
import org.eclipse.milo.opcua.stack.core.OpcUaDataType;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.structured.EnumDefinition;
import org.eclipse.milo.opcua.stack.core.types.structured.StructureDefinition;
import org.eclipse.milo.opcua.stack.core.types.structured.StructureField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Collects OPC UA type information from the server's type tree and converts it into {@link Schema}
 * objects using the {@link SchemaBuilder} fluent API.
 */
public class JsonSchemaGenerator {

    private final @NotNull OpcUaClient client;
    private final @NotNull DataTypeTree tree;

    public JsonSchemaGenerator(final @NotNull OpcUaClient client) {
        this.client = client;
        try {
            this.tree = client.getDataTypeTree();
        } catch (final UaException e) {
            throw new RuntimeException(e);
        }
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Collects OPC UA type information for the given tag asynchronously.
     */
    public @NotNull CompletableFuture<FieldInformation> collectTypeInfo(final @NotNull OpcuaTag tag) {
        final var parsed = NodeIdText.resolve(tag.getDefinition().getNode(), client.getNamespaceTable());
        return collectTypeInfo(parsed);
    }

    /**
     * Builds a {@link Schema} from the given {@link FieldInformation} using the provided
     * {@link SchemaBuilder}. The result is wrapped in an object with a required {@code value}
     * property and an optional read-only {@code metadata} property, matching the MQTT payload
     * structure produced by OPC UA adapters.
     * Calls {@link SchemaBuilder#build()} at the end.
     */
    public static @NotNull TagSchemaCreationOutput.DataPointSchema buildSchema(final @NotNull FieldInformation info) {
        final var valueSchema = new SchemaBuilder();
        // value — required, carries the OPC UA node value (scalar, array, or nested object)
        applyFieldInfoToSchema(valueSchema, info);

        final var metadataSchema = new SchemaBuilder()
                .startObject()
                .property(METADATA_STATUS_CODE)
                .startObject()
                .property("code")
                .scalar(ScalarType.LONG)
                .property("symbol")
                .scalar(ScalarType.STRING)
                .endObject()
                .readable()
                .writable(false)
                .property(METADATA_SOURCE_TIMESTAMP)
                .scalar(ScalarType.LONG)
                .readable()
                .writable(false)
                .property(METADATA_SOURCE_PICOSECONDS)
                .scalar(ScalarType.LONG)
                .readable()
                .writable(false)
                .property(METADATA_SERVER_TIMESTAMP)
                .scalar(ScalarType.LONG)
                .readable()
                .writable(false)
                .property(METADATA_SERVER_PICOSECONDS)
                .scalar(ScalarType.LONG)
                .readable()
                .writable(false)
                .endObject()
                .readable()
                .writable(false);

        return new TagSchemaCreationOutput.DataPointSchema(valueSchema.build(), metadataSchema.build(), null);
    }

    /**
     * Walks a complex OPC UA data type (ExtensionObject) recursively, producing a
     * {@link FieldInformation} tree.
     */
    public @NotNull FieldInformation processExtensionObject(
            final @NotNull DataType dataType,
            final boolean required,
            final boolean readable,
            final boolean writable,
            final @Nullable String name) {
        return processExtensionObject(dataType, required, readable, writable, name, Set.of());
    }

    /// The walk, with the structure DataTypes enclosing the one being described.
    ///
    /// **A field whose structure type encloses it is described as `any`.** A structure type may contain itself,
    /// directly or through other types -- a list, or a recipe step with sub-steps -- and the walk would then
    /// never end. Its values are still finite trees, of any depth, which the converter checks field by field;
    /// only the schema stops describing them at the field where the type repeats. A type that does not
    /// recurse never meets the check, so its description is unchanged.
    private @NotNull FieldInformation processExtensionObject(
            final @NotNull DataType dataType,
            final boolean required,
            final boolean readable,
            final boolean writable,
            final @Nullable String name,
            final @NotNull Set<NodeId> enclosing) {
        final Set<NodeId> path = new HashSet<>(enclosing);
        path.add(dataType.getNodeId());
        try {
            final var dataTypeDefinition = dataType.getDataTypeDefinition();
            if (dataTypeDefinition instanceof final StructureDefinition structureDefinition) {
                if (structureDefinition.getFields() != null) {
                    final var properties = Arrays.stream(structureDefinition.getFields())
                            .map(field -> {
                                final var fieldName = field.getName();
                                final var isRequired = !field.getIsOptional();
                                final var namespaceUri = field.getDataType()
                                        .expanded(client.getNamespaceTable())
                                        .getNamespaceUri();

                                // Resolve the underlying builtin type by walking the HasSubtype chain.
                                // Never null and declares no checked exceptions. This correctly handles
                                // simple subtypes of builtins (e.g. a custom String alias -> String) and
                                // custom-namespace types, instead of guessing from the namespace prefix.
                                final OpcUaDataType builtinType = tree.getBuiltinType(field.getDataType());

                                if (builtinType == OpcUaDataType.ExtensionObject) {
                                    // Genuine nested structure -> recurse.
                                    final DataType extractedDataType = tree.getDataType(field.getDataType());
                                    if (extractedDataType == null || path.contains(field.getDataType())) {
                                        // Unresolvable nested struct type, or one that encloses this field:
                                        // degrade to "any" rather than fail or recurse without end.
                                        return new FieldInformation(
                                                fieldName,
                                                namespaceUri,
                                                null,
                                                null,
                                                false,
                                                null,
                                                isRequired,
                                                readable,
                                                writable,
                                                List.of());
                                    }
                                    // The field's own value rank, carried onto the nested description: a field
                                    // declared as an array of a structure is an array, and describing it as a
                                    // single object refused every value of it.
                                    final Set<NodeId> nestedPath = new HashSet<>(path);
                                    nestedPath.add(field.getDataType());
                                    final FieldInformation nested = processExtensionObject(
                                            extractedDataType, isRequired, readable, writable, fieldName, nestedPath);
                                    return new FieldInformation(
                                            nested.name(),
                                            nested.namespaceUri(),
                                            nested.dataType(),
                                            nested.customDataType(),
                                            nested.isEnum(),
                                            arrayDimensionsOf(field),
                                            nested.required(),
                                            nested.readable(),
                                            nested.writable(),
                                            nested.nestedFields());
                                }

                                // Scalar-mappable builtin -> carry the OpcUaDataType. Enumerations,
                                // abstract and unresolvable types resolve to Variant/DataValue/
                                // DiagnosticInfo, which are not scalar-mappable; a null dataType makes
                                // the schema render as "any". A field that is itself an array
                                // (ValueRank >= 1) keeps its dimensions so it renders as an array.
                                final OpcUaDataType scalarType = isScalarMappable(builtinType) ? builtinType : null;
                                return new FieldInformation(
                                        fieldName,
                                        namespaceUri,
                                        scalarType,
                                        null,
                                        false,
                                        arrayDimensionsOf(field),
                                        isRequired,
                                        readable,
                                        writable,
                                        List.of());
                            })
                            .toList();

                    return new FieldInformation(
                            name,
                            client.getNamespaceTable()
                                    .get(dataType.getBrowseName().getNamespaceIndex()),
                            null,
                            dataType,
                            false,
                            null,
                            required,
                            readable,
                            writable,
                            properties);
                } else {
                    return new FieldInformation(
                            name,
                            client.getNamespaceTable()
                                    .get(dataType.getBrowseName().getNamespaceIndex()),
                            null,
                            dataType,
                            false,
                            null,
                            required,
                            readable,
                            writable,
                            List.of());
                }
            } else if (dataTypeDefinition instanceof EnumDefinition) {
                throw new RuntimeException("Enums not implemented yet");
            } else {
                // Complex type with an unknown/null DataTypeDefinition: degrade gracefully to "any"
                // (dataType == null, no nested fields) instead of failing the whole schema with a 500.
                return new FieldInformation(
                        name,
                        client.getNamespaceTable().get(dataType.getBrowseName().getNamespaceIndex()),
                        null,
                        dataType,
                        false,
                        null,
                        required,
                        readable,
                        writable,
                        List.of());
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ── Type info collection (private) ───────────────────────────────────────

    /**
     * Decodes the AccessLevel attribute of the given variable node into read/write flags.
     * Defaults to read+write when the attribute is unavailable or exposes neither
     * {@link AccessLevel#CurrentRead} nor {@link AccessLevel#CurrentWrite}, so schema generation
     * stays usable on servers that do not publish a meaningful access level.
     */
    private static @NotNull AccessFlags decodeAccessLevel(final @NotNull UaVariableNode uaVariableNode) {
        try {
            final Set<AccessLevel> levels = AccessLevel.fromValue(uaVariableNode.getAccessLevel());
            final boolean hasRead = levels.contains(AccessLevel.CurrentRead);
            final boolean hasWrite = levels.contains(AccessLevel.CurrentWrite);
            if (!hasRead && !hasWrite) {
                return new AccessFlags(true, true);
            }
            return new AccessFlags(hasRead, hasWrite);
        } catch (final Exception e) {
            return new AccessFlags(true, true);
        }
    }

    private record AccessFlags(boolean readable, boolean writable) {}

    private @NotNull CompletableFuture<FieldInformation> collectTypeInfo(final @NotNull NodeId destinationNodeId) {
        final CompletableFuture<UaVariableNode> variableNodeFuture =
                client.getAddressSpace().getVariableNodeAsync(destinationNodeId);

        return variableNodeFuture
                .thenApply(uaVariableNode -> {
                    final NodeId dataTypeNodeId = uaVariableNode.getDataType();
                    final DataType dataType = tree.getDataType(dataTypeNodeId);
                    final UInteger[] dimensions = uaVariableNode.getArrayDimensions();

                    if (dataType == null) {
                        throw new RuntimeException(
                                "Unable to find the data type for the given node id '" + destinationNodeId + "'.");
                    }
                    final OpcUaDataType builtinType = tree.getBuiltinType(dataType.getNodeId());

                    final AccessFlags access = decodeAccessLevel(uaVariableNode);
                    final boolean readable = access.readable();
                    final boolean writable = access.writable();

                    if (builtinType != OpcUaDataType.ExtensionObject) {
                        // Enumerations, abstract and unresolvable types resolve to Variant/DataValue/
                        // DiagnosticInfo, which are not scalar-mappable; a null dataType renders as "any"
                        // instead of throwing later in mapOpcUaToScalarType.
                        final OpcUaDataType scalarType = isScalarMappable(builtinType) ? builtinType : null;
                        return new FieldInformation(
                                null,
                                dataType.getNodeId()
                                        .expanded(client.getNamespaceTable())
                                        .getNamespaceUri(),
                                scalarType,
                                null,
                                false,
                                dimensions,
                                true,
                                readable,
                                writable,
                                List.of());
                    } else {
                        if (dataType.getBinaryEncodingId() == null) {
                            throw new RuntimeException(
                                    "No encoding was present for the complex data type: '" + dataType + "'.");
                        }
                        return processExtensionObject(dataType, true, readable, writable, null);
                    }
                })
                .exceptionally(throwable -> {
                    throw new RuntimeException("Problem accessing node", throwable);
                });
    }

    /// Every value is nullable exactly where its OPC UA type has a null value (EDG-1138), so a `null` passes the
    /// schema where the converter sends that null value, and fails it where the type has none. The rule is the
    /// converter's: [JsonToOpcUaValueConverter#hasNullValue].
    ///
    /// **An array is nullable**, whatever its element type: the null array is a value of every array, and the
    /// converter sends it for a `null`. Only the outermost array is -- the rows of a multi-dimensional array are
    /// not values of their own, and the converter refuses a `null` row. Array elements follow their element type
    /// like any scalar. The array's own flag is set by [#startNullableArray].
    private static void applyScalarType(final @NotNull SchemaBuilder schema, final @NotNull OpcUaDataType opcUaType) {
        if (JsonToOpcUaValueConverter.hasNullValue(opcUaType)) {
            schema.nullable();
        }
        if (opcUaType == OpcUaDataType.QualifiedName) {
            schema.startObject()
                    .property("namespaceIndex")
                    .required()
                    .scalar(ScalarType.LONG)
                    .property("name")
                    .required()
                    .scalar(ScalarType.STRING)
                    .endObject();
            return;
        }
        schema.scalar(mapOpcUaToScalarType(opcUaType));
        applyMinMax(schema, opcUaType);
    }

    /// Describes a property as the given OPC UA type, whatever that type is.
    ///
    /// The one entry point a caller outside this class should use, because it owns the whole decision: a type
    /// with no JSON scalar form becomes `any`, `QualifiedName` becomes its two-field object, and everything else
    /// becomes a scalar with its width's range constraints. Calling {@link #applyScalarType} directly requires
    /// knowing that it throws for the four non-scalar types, and that is exactly the pairing a second caller
    /// would get wrong.
    public static void describeAs(
            final @NotNull PropertySchemaBuilder<?> prop, final @NotNull OpcUaDataType opcUaType) {
        if (!isScalarMappable(opcUaType)) {
            prop.any();
            return;
        }
        applyScalarType(prop, opcUaType);
    }

    /// Describes a property as an array of the given OPC UA type, with one nesting level per dimension.
    ///
    /// The array counterpart of {@link #describeAs}, for a caller that has an OPC UA value rank rather than a
    /// node's `FieldInformation` -- a method's {@code Argument} is the case this exists for. Delegates to the
    /// same {@link #applyArrayType} the value-tag path uses, so a method's array argument and a value tag's
    /// array node are described identically, nesting and element constraints included.
    ///
    /// **No length is stated.** `dimensions` supplies the nesting depth and nothing else -- see
    /// {@link #applyArrayType} for why a declared length is neither emitted nor emittable here.
    ///
    /// @param prop       the property to describe.
    /// @param opcUaType  the element type, or null when it is not one Edge can state -- then the elements are
    ///                   `any`, which is the same answer {@link #describeAs} gives for an unmappable scalar.
    /// @param dimensions one entry per dimension; its length is the nesting depth, and its entries are unused.
    public static void describeAsArray(
            final @NotNull PropertySchemaBuilder<?> prop,
            final @Nullable OpcUaDataType opcUaType,
            final @NotNull UInteger @NotNull [] dimensions) {

        describeAsArray(prop, opcUaType, dimensions, true);
    }

    /// As above, with the elements' access flags stated.
    ///
    /// An array is written by supplying its elements, so elements claiming to be read-only describe a shape nothing
    /// can be written to -- which is why the writable case has to say so at every depth. A response's outputs are
    /// the opposite: the device produced them and nobody writes them.
    public static void describeAsArray(
            final @NotNull PropertySchemaBuilder<?> prop,
            final @Nullable OpcUaDataType opcUaType,
            final @NotNull UInteger @NotNull [] dimensions,
            final boolean writable) {

        final OpcUaDataType elementType = opcUaType != null && isScalarMappable(opcUaType) ? opcUaType : null;
        applyArrayType(startNullableArray(prop), elementType, null, dimensions, 0, true, writable);
    }

    /// Starts the outermost array of a value, declared nullable: see [#applyScalarType]. The SDK puts an array's
    /// own flags on the builder that starts it; the item builder's apply to the elements.
    private static <P> @NotNull ItemSchemaBuilder<PropertySchemaBuilder<P>> startNullableArray(
            final @NotNull PropertySchemaBuilder<P> prop) {
        return prop.nullable().startArray();
    }

    private static @NotNull ItemSchemaBuilder<SchemaBuilder> startNullableArray(final @NotNull SchemaBuilder schema) {
        return schema.nullable().startArray();
    }

    private static void applyScalarType(
            final @NotNull PropertySchemaBuilder<?> prop, final @NotNull OpcUaDataType opcUaType) {
        if (JsonToOpcUaValueConverter.hasNullValue(opcUaType)) {
            prop.nullable();
        }
        if (opcUaType == OpcUaDataType.QualifiedName) {
            prop.startObject()
                    .property("namespaceIndex")
                    .required()
                    .scalar(ScalarType.LONG)
                    .property("name")
                    .required()
                    .scalar(ScalarType.STRING)
                    .endObject();
            return;
        }
        prop.scalar(mapOpcUaToScalarType(opcUaType));
        applyMinMax(prop, opcUaType);
    }

    private static void applyScalarType(
            final @NotNull ItemSchemaBuilder<?> items, final @NotNull OpcUaDataType opcUaType) {
        if (JsonToOpcUaValueConverter.hasNullValue(opcUaType)) {
            items.nullable();
        }
        if (opcUaType == OpcUaDataType.QualifiedName) {
            items.startObject()
                    .property("namespaceIndex")
                    .required()
                    .scalar(ScalarType.LONG)
                    .property("name")
                    .required()
                    .scalar(ScalarType.STRING)
                    .endObject();
            return;
        }
        items.scalar(mapOpcUaToScalarType(opcUaType));
        applyMinMax(items, opcUaType);
    }

    private static <P> void applyObjectProperties(
            final @NotNull ObjectSchemaBuilder<P> obj, final @NotNull FieldInformation info) {
        for (final FieldInformation field : info.nestedFields()) {
            final String fieldName = field.name();
            if (fieldName == null) {
                continue;
            }
            final var prop = obj.property(fieldName).readable(field.readable()).writable(field.writable());
            if (field.required()) {
                prop.required();
            }
            applyFieldInfoToProperty(prop, field);
        }
        obj.endObject();
    }

    private static void applyFieldInfoToSchema(
            final @NotNull SchemaBuilder schema, final @NotNull FieldInformation info) {
        schema.readable(info.readable()).writable(info.writable());
        if (info.arrayDimensions() != null && info.arrayDimensions().length > 0) {
            applyArrayType(
                    startNullableArray(schema),
                    info.dataType(),
                    info.nestedFields().isEmpty() ? null : info,
                    info.arrayDimensions(),
                    0,
                    info.readable(),
                    info.writable());
        } else if (!info.nestedFields().isEmpty()) {
            applyObjectProperties(schema.startObject(), info);
        } else if (info.dataType() != null) {
            applyScalarType(schema, info.dataType());
        } else {
            schema.any();
        }
    }

    /// A field of a structure.
    ///
    /// **A builtin accepted in two shapes is `any` here**, for the reason {@link #acceptsTwoShapes} gives: the
    /// read path publishes it as an object, a caller may write the shorthand, and stating either refuses the
    /// other. As a field of a structure that refusal is the worse one, because it rejects a structure read from
    /// the device and sent back unchanged.
    private static <P> void applyFieldInfoToProperty(
            final @NotNull PropertySchemaBuilder<P> prop, final @NotNull FieldInformation info) {
        final OpcUaDataType stated =
                info.dataType() != null && acceptsTwoShapes(info.dataType()) ? null : info.dataType();
        if (info.arrayDimensions() != null && info.arrayDimensions().length > 0) {
            applyArrayType(
                    startNullableArray(prop).readable(info.readable()).writable(info.writable()),
                    stated,
                    info.nestedFields().isEmpty() ? null : info,
                    info.arrayDimensions(),
                    0,
                    info.readable(),
                    info.writable());
        } else if (!info.nestedFields().isEmpty()) {
            applyObjectProperties(prop.startObject(), info);
        } else if (stated != null) {
            applyScalarType(prop, stated);
        } else {
            prop.any();
        }
    }

    /// Whether the converter accepts a value of this builtin in two JSON shapes: the object the read path
    /// publishes, and a shorthand a caller writes by hand. The schema model has no alternation, so such a value
    /// can only be described as `any`.
    public static boolean acceptsTwoShapes(final @NotNull OpcUaDataType type) {
        return switch (type) {
            case LocalizedText, NodeId, QualifiedName, StatusCode -> true;
            default -> false;
        };
    }

    /// Describes a property as a structure -- or an array of one, when `dimensions` is not empty -- from the
    /// structure's description.
    ///
    /// For a method argument, whose structure was described during the browse and whose access is the
    /// argument's rather than the description's: a request's arguments are written, a response's are not.
    public static void describeStructure(
            final @NotNull PropertySchemaBuilder<?> prop,
            final @NotNull FieldInformation structure,
            final @NotNull UInteger @NotNull [] dimensions,
            final boolean writable) {
        final FieldInformation access = withAccess(structure, true, writable);
        if (dimensions.length > 0) {
            applyArrayType(startNullableArray(prop), null, access, dimensions, 0, true, writable);
        } else {
            applyObjectProperties(prop.startObject(), access);
        }
    }

    /// The description with every field's access flags replaced, all the way down.
    private static @NotNull FieldInformation withAccess(
            final @NotNull FieldInformation info, final boolean readable, final boolean writable) {
        return new FieldInformation(
                info.name(),
                info.namespaceUri(),
                info.dataType(),
                info.customDataType(),
                info.isEnum(),
                info.arrayDimensions(),
                info.required(),
                readable,
                writable,
                info.nestedFields().stream()
                        .map(field -> withAccess(field, readable, writable))
                        .toList());
    }

    /**
     * Builds the item schema for an array-valued node.
     * <p>
     * The elements carry the node's own access flags. A writable node is written by supplying its elements, so
     * elements that claim to be read-only describe a shape nothing can be written to — the degenerate
     * "array of read-only items admits only {@code []}" case. Without this the elements fell back to
     * {@link com.hivemq.adapter.sdk.api.schema.SchemaBuilder}'s {@code writable = false} default and every
     * writable array node advertised read-only elements. Applies at every nesting depth, so a
     * multi-dimensional array is writable all the way down.
     * <p>
     * {@code structure}, when given, describes the elements as that structure's object, and {@code opcUaType}
     * is then unused. Without either the elements are {@code any}.
     */
    private static <P> void applyArrayType(
            final @NotNull ItemSchemaBuilder<P> items,
            final @Nullable OpcUaDataType opcUaType,
            final @Nullable FieldInformation structure,
            final @NotNull UInteger @NotNull [] dimensions,
            final int depth,
            final boolean readable,
            final boolean writable) {
        if (depth == dimensions.length - 1) {
            if (structure != null) {
                applyObjectProperties(items.startObject(), structure);
            } else if (opcUaType != null) {
                applyScalarType(items, opcUaType);
            } else {
                items.any();
            }
        } else {
            applyArrayType(items.startArray(), opcUaType, structure, dimensions, depth + 1, readable, writable);
        }
        items.readable(readable).writable(writable);
        // No length constraint is stated, and `dimensions` is consulted only for the nesting depth.
        //
        // This used to emit minContains and maxContains, both set to the declared length. Three things were
        // wrong with that, and they compound:
        //
        //   - The keywords do nothing. They count how many elements match a `contains` keyword, and no
        //     `contains` is ever emitted -- so a schema saying minContains 3 accepted [], [1] and [1,2,3,4].
        //     Measured against the validator Edge uses.
        //   - Equal minimum and maximum misstates the declaration. OPC 10000-3 section 5.6.2: ArrayDimensions
        //     "specifies the maximum supported length of each dimension. If the maximum is unknown the value
        //     shall be 0". A maximum of 5 does not mean exactly five elements.
        //   - A union over several methods had no length to agree on, so whichever method the browse reported
        //     first supplied the number; reversing the browse order changed the schema.
        //
        // The honest statement would be maxItems, which this builder cannot emit: the shared SDK's array
        // builder offers only minContains and maxContains, and the SDK is unchanged in this release. It could
        // be added to the rendered JSON afterwards -- `SchemaJsonRepresentation.toJsonSchemaDocument` returns a
        // mutable ObjectNode to code in this repository -- so the SDK is not what stops it.
        //
        // What stops it is that there is nothing to gain. The length rests with the device, which refuses an
        // array longer than it declared and whose refusal reaches the caller as a response. A *shorter* array
        // is a legal request -- OPC 10000-3 Table 28 defines an argument's ArrayDimensions as "the maximum
        // supported length", and gives a device no way to say "exactly five" -- so maxItems would duplicate a
        // refusal the device already makes, and minItems would refuse requests the specification permits.
        //
        // Measured on an S7-1500 (EDG-1087 M1): seven elements for a declared five were refused
        // Bad_InvalidArgument; three were accepted and the method computed on whatever remained in the unsent
        // slots. Nothing is wrong in that, and it is the caller's to get right -- the reference document says
        // so, because no gate anywhere will catch it.
        items.endArray();
    }

    // ── Min/Max helpers ──────────────────────────────────────────────────────

    private static void applyMinMax(final @NotNull SchemaBuilder schema, final @NotNull OpcUaDataType type) {
        final Number min = minimumForOpcUaType(type);
        final Number max = maximumForOpcUaType(type);
        if (min instanceof final Long l) {
            schema.minimum(l);
        } else if (min instanceof final Double d) {
            schema.minimum(d);
        }
        if (max instanceof final Long l) {
            schema.maximum(l);
        } else if (max instanceof final Double d) {
            schema.maximum(d);
        }
    }

    private static void applyMinMax(final @NotNull PropertySchemaBuilder<?> prop, final @NotNull OpcUaDataType type) {
        final Number min = minimumForOpcUaType(type);
        final Number max = maximumForOpcUaType(type);
        if (min instanceof final Long l) {
            prop.minimum(l);
        } else if (min instanceof final Double d) {
            prop.minimum(d);
        }
        if (max instanceof final Long l) {
            prop.maximum(l);
        } else if (max instanceof final Double d) {
            prop.maximum(d);
        }
    }

    private static void applyMinMax(final @NotNull ItemSchemaBuilder<?> items, final @NotNull OpcUaDataType type) {
        final Number min = minimumForOpcUaType(type);
        final Number max = maximumForOpcUaType(type);
        if (min instanceof final Long l) {
            items.minimum(l);
        } else if (min instanceof final Double d) {
            items.minimum(d);
        }
        if (max instanceof final Long l) {
            items.maximum(l);
        } else if (max instanceof final Double d) {
            items.maximum(d);
        }
    }

    // ── Type mapping ─────────────────────────────────────────────────────────

    /**
     * Returns the array dimensions of a struct field that is itself an array ({@code ValueRank >= 1}),
     * or {@code null} for a scalar field. When the server declares an array but leaves the dimensions
     * unspecified, one unbounded dimension per rank is synthesized so the field is still rendered as an
     * array rather than flattened to a scalar.
     */
    private static UInteger @Nullable [] arrayDimensionsOf(final @NotNull StructureField field) {
        final Integer valueRank = field.getValueRank();
        if (valueRank == null || valueRank < 1) {
            return null;
        }
        final UInteger[] dimensions = field.getArrayDimensions();
        if (dimensions != null && dimensions.length > 0) {
            return dimensions;
        }
        final UInteger[] synthesized = new UInteger[valueRank];
        Arrays.fill(synthesized, uint(0));
        return synthesized;
    }

    /**
     * Returns {@code true} when the given builtin type can be represented as a JSON scalar by
     * {@link #mapOpcUaToScalarType(OpcUaDataType)}. The four non-scalar types
     * ({@code ExtensionObject}, {@code Variant}, {@code DataValue}, {@code DiagnosticInfo}) — which
     * also cover enumerations, abstract and unresolvable types once resolved to {@code Variant} — must
     * be rendered as {@code any} instead of being forced through the scalar path.
     */
    private static boolean isScalarMappable(final @NotNull OpcUaDataType type) {
        return type != OpcUaDataType.ExtensionObject
                && type != OpcUaDataType.Variant
                && type != OpcUaDataType.DataValue
                && type != OpcUaDataType.DiagnosticInfo;
    }

    static @NotNull ScalarType mapOpcUaToScalarType(final @NotNull OpcUaDataType opcUaType) {
        return switch (opcUaType) {
            case Boolean -> ScalarType.BOOLEAN;
            case SByte, Byte, Int16, UInt16, Int32, UInt32, StatusCode, Int64 -> ScalarType.LONG;
            case UInt64 -> ScalarType.ULONG;
            case Float, Double -> ScalarType.DOUBLE;
            case String, Guid, XmlElement, NodeId, ExpandedNodeId, LocalizedText -> ScalarType.STRING;
            // Bytes, not text. The wire form is the same base64 string it always was; BINARY is what says so,
            // adding contentEncoding: base64 to the schema instead of leaving a consumer to infer it.
            case ByteString -> ScalarType.BINARY;
            case DateTime -> ScalarType.INSTANT;
            case QualifiedName -> throw new IllegalArgumentException("QualifiedName is an object type, not a scalar");
            case ExtensionObject, DataValue, Variant, DiagnosticInfo ->
                throw new IllegalArgumentException("Unsupported OPC UA data type: " + opcUaType.name());
        };
    }

    // ── Range constraints per OPC UA type width ──────────────────────────────

    private static @Nullable Number minimumForOpcUaType(final @NotNull OpcUaDataType opcUaType) {
        return switch (opcUaType) {
            case SByte -> (long) java.lang.Byte.MIN_VALUE;
            case Byte -> (long) UByte.MIN_VALUE;
            case Int16 -> (long) Short.MIN_VALUE;
            case UInt16 -> (long) UShort.MIN.intValue();
            case Int32 -> (long) Integer.MIN_VALUE;
            // A StatusCode is unsigned: every Bad code has the top bit set, and the read side publishes Bad as
            // 2147483648, which Int32 bounds would refuse.
            case UInt32, StatusCode -> UInteger.MIN_VALUE;
            case Int64 -> Long.MIN_VALUE;
            // As a long, not `ULong.MIN_VALUE`: that constant is a BigInteger, which the builder takes for
            // neither bound, so the lower limit of a UInt64 was silently dropped along with its maximum.
            case UInt64 -> 0L;
            case Float -> (double) -java.lang.Float.MAX_VALUE;
            case Double -> -java.lang.Double.MAX_VALUE;
            default -> null;
        };
    }

    private static @Nullable Number maximumForOpcUaType(final @NotNull OpcUaDataType opcUaType) {
        return switch (opcUaType) {
            case SByte -> (long) java.lang.Byte.MAX_VALUE;
            case Byte -> (long) UByte.MAX_VALUE;
            case Int16 -> (long) Short.MAX_VALUE;
            case UInt16 -> (long) UShort.MAX.intValue();
            case Int32 -> (long) Integer.MAX_VALUE;
            case UInt32, StatusCode -> UInteger.MAX_VALUE;
            case Int64 -> Long.MAX_VALUE;
            // No maximum for UInt64, deliberately. You would want 2^64-1 here, but it does not fit a signed
            // long and the builder takes only Long or Double -- so it would be dropped, or truncated to -1 and
            // refuse every value. The converter bounds it before the device sees it.
            case UInt64 -> null;
            case Float -> (double) java.lang.Float.MAX_VALUE;
            case Double -> java.lang.Double.MAX_VALUE;
            default -> null;
        };
    }

    // ── Field information record ─────────────────────────────────────────────

    @SuppressWarnings("ArrayRecordComponent")
    public record FieldInformation(
            @Nullable String name,
            @Nullable String namespaceUri,
            @Nullable OpcUaDataType dataType,
            @Nullable DataType customDataType,
            boolean isEnum,
            UInteger @Nullable [] arrayDimensions,
            boolean required,
            boolean readable,
            boolean writable,
            @NotNull List<FieldInformation> nestedFields) {}
}
