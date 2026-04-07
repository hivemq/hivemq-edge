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

import com.hivemq.adapter.sdk.api.schema.ItemSchemaBuilder;
import com.hivemq.adapter.sdk.api.schema.ObjectSchemaBuilder;
import com.hivemq.adapter.sdk.api.schema.PropertySchemaBuilder;
import com.hivemq.adapter.sdk.api.schema.ScalarType;
import com.hivemq.adapter.sdk.api.schema.Schema;
import com.hivemq.adapter.sdk.api.schema.SchemaBuilder;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationOutput;
import com.hivemq.adapter.sdk.api.schema.impl.SchemaBuilderImpl;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.core.typetree.DataType;
import org.eclipse.milo.opcua.sdk.core.typetree.DataTypeTree;
import org.eclipse.milo.opcua.stack.core.OpcUaDataType;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.ULong;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.structured.EnumDefinition;
import org.eclipse.milo.opcua.stack.core.types.structured.StructureDefinition;
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
        final var parsed = NodeId.parse(tag.getDefinition().getNode());
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
        final var valueSchema = new SchemaBuilderImpl();
        // value — required, carries the OPC UA node value (scalar, array, or nested object)
        applyFieldInfoToSchema(valueSchema, info);

        final var metadataSchema = new SchemaBuilderImpl()
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
            final @NotNull DataType dataType, final boolean required, final @Nullable String name) {
        try {
            final var dataTypeDefinition = dataType.getDataTypeDefinition();
            if (dataTypeDefinition instanceof final StructureDefinition structureDefinition) {
                if (structureDefinition.getFields() != null) {
                    final var properties = Arrays.stream(structureDefinition.getFields())
                            .map(field -> {
                                final String localPart;
                                final DataType extractedDataType;
                                try {
                                    extractedDataType = client.getDataTypeTree().getDataType(field.getDataType());
                                    if (extractedDataType == null) {
                                        throw new RuntimeException(
                                                "Unsupported type definition: " + dataTypeDefinition);
                                    }
                                    localPart =
                                            extractedDataType.getBrowseName().name();
                                } catch (final UaException e) {
                                    throw new RuntimeException(e);
                                }
                                final var fieldName = field.getName();
                                final var isRequired = !field.getIsOptional();
                                final var namespaceUri = field.getDataType()
                                        .expanded(client.getNamespaceTable())
                                        .getNamespaceUri();
                                final boolean isStandard =
                                        namespaceUri != null && namespaceUri.startsWith("http://opcfoundation.org/");
                                final var opcUaType = isStandard ? OpcUaDataType.valueOf(localPart) : null;

                                if (isStandard) {
                                    return new FieldInformation(
                                            fieldName,
                                            namespaceUri,
                                            opcUaType,
                                            null,
                                            false,
                                            null,
                                            isRequired,
                                            List.of());
                                } else {
                                    return processExtensionObject(extractedDataType, isRequired, fieldName);
                                }
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
                            List.of());
                }
            } else if (dataTypeDefinition instanceof EnumDefinition) {
                throw new RuntimeException("Enums not implemented yet");
            } else {
                throw new RuntimeException("Unsupported type definition: " + dataTypeDefinition);
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ── Type info collection (private) ───────────────────────────────────────

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

                    if (builtinType != OpcUaDataType.ExtensionObject) {
                        return new FieldInformation(
                                null,
                                dataType.getNodeId()
                                        .expanded(client.getNamespaceTable())
                                        .getNamespaceUri(),
                                builtinType,
                                null,
                                false,
                                dimensions,
                                true,
                                List.of());
                    } else {
                        if (dataType.getBinaryEncodingId() == null) {
                            throw new RuntimeException(
                                    "No encoding was present for the complex data type: '" + dataType + "'.");
                        }
                        return processExtensionObject(dataType, true, null);
                    }
                })
                .exceptionally(throwable -> {
                    throw new RuntimeException("Problem accessing node", throwable);
                });
    }

    private static void applyScalarType(final @NotNull SchemaBuilder schema, final @NotNull OpcUaDataType opcUaType) {
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

    private static void applyScalarType(
            final @NotNull PropertySchemaBuilder<?> prop, final @NotNull OpcUaDataType opcUaType) {
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
            final var prop = obj.property(fieldName).writable();
            if (field.required()) {
                prop.required();
            }
            applyFieldInfoToProperty(prop, field);
        }
        obj.endObject();
    }

    private static void applyFieldInfoToSchema(
            final @NotNull SchemaBuilder schema, final @NotNull FieldInformation info) {
        if (info.arrayDimensions() != null && info.arrayDimensions().length > 0) {
            applyArrayType(schema.startArray().writable(), info.dataType(), info.arrayDimensions(), 0);
        } else if (!info.nestedFields().isEmpty()) {
            applyObjectProperties(schema.startObject(), info);
        } else if (info.dataType() != null) {
            applyScalarType(schema.writable(), info.dataType());
        } else {
            schema.any().writable();
        }
    }

    private static <P> void applyFieldInfoToProperty(
            final @NotNull PropertySchemaBuilder<P> prop, final @NotNull FieldInformation info) {
        if (info.arrayDimensions() != null && info.arrayDimensions().length > 0) {
            applyArrayType(prop.startArray().writable(), info.dataType(), info.arrayDimensions(), 0);
        } else if (!info.nestedFields().isEmpty()) {
            applyObjectProperties(prop.startObject(), info);
        } else if (info.dataType() != null) {
            applyScalarType(prop, info.dataType());
        } else {
            prop.any();
        }
    }

    private static <P> void applyArrayType(
            final @NotNull ItemSchemaBuilder<P> items,
            final @Nullable OpcUaDataType opcUaType,
            final @NotNull UInteger @NotNull [] dimensions,
            final int depth) {
        if (depth == dimensions.length - 1) {
            if (opcUaType != null) {
                applyScalarType(items, opcUaType);
            } else {
                items.any();
            }
        } else {
            applyArrayType(items.startArray(), opcUaType, dimensions, depth + 1);
        }
        final long maxSize = dimensions[depth].longValue();
        if (maxSize > 0) {
            items.minContains((int) maxSize).maxContains((int) maxSize);
        }
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

    static @NotNull ScalarType mapOpcUaToScalarType(final @NotNull OpcUaDataType opcUaType) {
        return switch (opcUaType) {
            case Boolean -> ScalarType.BOOLEAN;
            case SByte, Byte, Int16, UInt16, Int32, UInt32, StatusCode, Int64 -> ScalarType.LONG;
            case UInt64 -> ScalarType.ULONG;
            case Float, Double -> ScalarType.DOUBLE;
            case String, Guid, ByteString, XmlElement, NodeId, ExpandedNodeId, LocalizedText, DateTime ->
                ScalarType.STRING;
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
            case Int32, StatusCode -> (long) Integer.MIN_VALUE;
            case UInt32 -> UInteger.MIN_VALUE;
            case Int64 -> Long.MIN_VALUE;
            case UInt64 -> ULong.MIN_VALUE;
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
            case Int32, StatusCode -> (long) Integer.MAX_VALUE;
            case UInt32 -> UInteger.MAX_VALUE;
            case Int64 -> Long.MAX_VALUE;
            case UInt64 ->
                BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.TWO).add(BigInteger.ONE);
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
            @NotNull List<FieldInformation> nestedFields) {}
}
