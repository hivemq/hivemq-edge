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
package com.hivemq.edge.adapters.opcua.northbound;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.function.Supplier;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.core.types.DynamicStructType;
import org.eclipse.milo.opcua.sdk.core.types.codec.DynamicStructCodec;
import org.eclipse.milo.opcua.sdk.core.typetree.DataType;
import org.eclipse.milo.opcua.sdk.core.typetree.DataTypeTree;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.ServerTable;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.channel.EncodingLimits;
import org.eclipse.milo.opcua.stack.core.encoding.DataTypeCodec;
import org.eclipse.milo.opcua.stack.core.encoding.DefaultEncodingContext;
import org.eclipse.milo.opcua.stack.core.encoding.EncodingContext;
import org.eclipse.milo.opcua.stack.core.encoding.EncodingManager;
import org.eclipse.milo.opcua.stack.core.types.DataTypeDictionary;
import org.eclipse.milo.opcua.stack.core.types.DataTypeManager;
import org.eclipse.milo.opcua.stack.core.types.UaStructuredType;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExtensionObject;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.enumerated.StructureType;
import org.eclipse.milo.opcua.stack.core.types.structured.DataTypeDefinition;
import org.eclipse.milo.opcua.stack.core.types.structured.StructureDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// What the read converter may need from the server to render a value, each part fetched only when a value calls
/// for it.
///
/// @param context the encoding context, for a structure that arrives still encoded.
/// @param types   the server's DataType hierarchy, for a standard structure Milo has decoded into its generated
///                class -- see [#asDynamic]. Optional in effect: when it cannot be supplied, such a value is
///                published as its string form, as it was before the hierarchy was consulted.
public record Decoding(
        @NotNull Supplier<EncodingContext> context, @NotNull Supplier<DataTypeTree> types) {

    private static final @NotNull Logger log = LoggerFactory.getLogger(Decoding.class);

    /// Decoding for a connected client: its dynamic encoding context and its cached DataType hierarchy. Neither is
    /// read until a value needs it.
    public static @NotNull Decoding of(final @NotNull OpcUaClient client) {
        return new Decoding(
                () -> {
                    try {
                        return client.getDynamicEncodingContext();
                    } catch (final UaException e) {
                        throw new RuntimeException(e);
                    }
                },
                () -> {
                    try {
                        return client.getDataTypeTree();
                    } catch (final UaException e) {
                        throw new IllegalStateException(e.getMessage(), e);
                    }
                });
    }

    /// Decoding with an encoding context and no hierarchy, for a caller that has none at hand.
    public static @NotNull Decoding withoutTypes(final @NotNull Supplier<EncodingContext> context) {
        return new Decoding(context, () -> {
            throw new IllegalStateException("no DataType hierarchy is available");
        });
    }

    /// A standard structure as the dynamic structure its definition describes, or empty when that cannot be done.
    ///
    /// ## Why this exists
    ///
    /// Milo knows OPC UA's own structures -- `Range`, `ServerStatusDataType`, `ThreeDVector` and some forty more
    /// a device may hold as data -- as generated Java classes, and decodes them into those. The read converter
    /// renders a vendor structure field by field, because Milo decodes it into a `DynamicStructType`; a generated
    /// class it has no branch for fell through to `toString`, so an analog item's `EURange` was published as
    /// `Range{low=1.5, high=9.0}` -- text no consumer can parse and no writer accepts back, while the value tag's
    /// own schema described it as an object with `Low` and `High`.
    ///
    /// ## How
    ///
    /// The generated object is encoded again with the codec that produced it, which reproduces the bytes the
    /// server sent, and those bytes are decoded with a `DynamicStructCodec` built from the type's definition: the
    /// one in the server's hierarchy, or Milo's own where the server publishes none -- see [#withDefinition]. That
    /// is how a vendor structure is decoded, so the result is published exactly as
    /// one, with the field names its definition gives -- the same names the southbound converter checks a written
    /// structure against, so a value read can be sent back unchanged.
    ///
    /// A field that is itself a standard structure comes back as its generated class again, and is converted
    /// the same way when the converter reaches it.
    ///
    /// ## When it declines
    ///
    /// The hierarchy cannot be read, the type is not in it, it has no structure definition, or it is a union --
    /// Milo has a union codec, but the read side does not publish unions, so a converted one would only reach the
    /// same `toString`. Every case answers empty, and the caller falls back to `toString`: the output is never
    /// worse than it was.
    @NotNull
    Optional<DynamicStructType> asDynamic(final @NotNull UaStructuredType value) {
        try {
            final EncodingContext encoding = context.get();
            final DataTypeTree tree = types.get();
            final NodeId typeId =
                    value.getTypeId().toNodeId(encoding.getNamespaceTable()).orElse(null);
            final DataType described = typeId == null ? null : tree.getDataType(typeId);
            final DataType dataType =
                    typeId == null ? null : withDefinition(described, typeId, value, encoding.getNamespaceTable());
            if (dataType == null
                    || !(dataType.getDataTypeDefinition() instanceof final StructureDefinition definition)) {
                return Optional.empty();
            }
            if (definition.getStructureType() == StructureType.Union
                    || definition.getStructureType() == StructureType.UnionWithSubtypedValues) {
                return Optional.empty();
            }
            // Encoded with the static codecs, the family that produced the generated object. The client's dynamic
            // context may hold a dynamic codec for the same type, which expects a DynamicStructType and refuses the
            // generated class. Every generated class is one of OPC UA's own namespace-0 types, which the static
            // context knows without a server.
            final ExtensionObject encoded = ExtensionObject.encode(DefaultEncodingContext.INSTANCE, value);
            final UaStructuredType decoded = encoded.decode(
                    new DynamicFirst(encoding, encoded.getEncodingOrTypeId(), new DynamicStructCodec(dataType, tree)));
            return decoded instanceof final DynamicStructType dynamic ? Optional.of(dynamic) : Optional.empty();
        } catch (final RuntimeException e) {
            log.debug(
                    "Could not render {} field by field, publishing its string form: {}",
                    value.getClass().getSimpleName(),
                    e.getMessage());
            return Optional.empty();
        }
    }

    /// The DataType to decode a standard structure with: the server's, when the server describes the structure,
    /// and otherwise one carrying the definition Milo generated the class from (EDG-1138).
    ///
    /// **Why Milo's definition is as good as the server's.** A generated class is one of OPC UA's own namespace-0
    /// structures -- `Range`, `EUInformation`, `ServerStatusDataType` -- whose fields the specification fixes.
    /// Milo's code generator emits each class with a static `definition(NamespaceTable)` built from that
    /// specification, so a server's copy adds nothing. Some servers do not publish it (the S7-1500 and the WAGO
    /// in the lab), and without this fallback such a value was published as its `toString` text.
    ///
    /// Only namespace 0, because only there is the generated class the specification's own; and only when the
    /// class offers the method. Anything else answers the server's DataType, which may still lack a definition --
    /// the caller then falls back to `toString` as before.
    private static @Nullable DataType withDefinition(
            final @Nullable DataType described,
            final @NotNull NodeId typeId,
            final @NotNull UaStructuredType value,
            final @NotNull NamespaceTable namespaces) {
        if (described != null && described.getDataTypeDefinition() instanceof StructureDefinition) {
            return described;
        }
        if (typeId.getNamespaceIndex().intValue() != 0) {
            return described;
        }
        final StructureDefinition generated;
        try {
            final Method definition = value.getClass().getMethod("definition", NamespaceTable.class);
            if (!Modifier.isStatic(definition.getModifiers())
                    || !(definition.invoke(null, namespaces) instanceof final StructureDefinition found)) {
                return described;
            }
            generated = found;
        } catch (final ReflectiveOperationException e) {
            return described;
        }
        final NodeId binaryEncodingId =
                value.getBinaryEncodingId().toNodeId(namespaces).orElse(NodeId.NULL_VALUE);
        return new GeneratedDataType(
                described != null
                        ? described.getBrowseName()
                        : new QualifiedName(0, value.getClass().getSimpleName()),
                typeId,
                binaryEncodingId,
                generated);
    }

    /// A namespace-0 structure's DataType, described by Milo's generated class rather than by the server.
    private record GeneratedDataType(
            @NotNull QualifiedName browseName,
            @NotNull NodeId nodeId,
            @NotNull NodeId binaryEncodingId,
            @NotNull StructureDefinition definition)
            implements DataType {

        @Override
        public @NotNull QualifiedName getBrowseName() {
            return browseName;
        }

        @Override
        public @NotNull NodeId getNodeId() {
            return nodeId;
        }

        @Override
        public @NotNull NodeId getBinaryEncodingId() {
            return binaryEncodingId;
        }

        @Override
        public @Nullable NodeId getXmlEncodingId() {
            return null;
        }

        @Override
        public @Nullable NodeId getJsonEncodingId() {
            return null;
        }

        @Override
        public @NotNull DataTypeDefinition getDataTypeDefinition() {
            return definition;
        }

        @Override
        public @NotNull Boolean isAbstract() {
            return false;
        }
    }

    /// An encoding context whose codec for one encoding is the dynamic one, and which otherwise is the client's.
    ///
    /// Only the outermost structure is decoded dynamically. Its fields are decoded through the client's own codecs,
    /// so a nested standard structure arrives as its generated class and takes the same route when the converter
    /// renders it.
    private record DynamicFirst(
            @NotNull EncodingContext delegate,
            @NotNull NodeId encodingId,
            @NotNull DataTypeCodec codec) implements EncodingContext {

        @Override
        public @NotNull DataTypeManager getDataTypeManager() {
            final DataTypeManager types = delegate.getDataTypeManager();
            return new DataTypeManager() {
                @Override
                public void registerType(
                        final @NotNull NodeId dataTypeId,
                        final @NotNull DataTypeCodec registered,
                        final @Nullable NodeId binaryEncodingId,
                        final @Nullable NodeId xmlEncodingId,
                        final @Nullable NodeId jsonEncodingId) {
                    types.registerType(dataTypeId, registered, binaryEncodingId, xmlEncodingId, jsonEncodingId);
                }

                @Override
                public @Nullable DataTypeCodec getCodec(final @NotNull NodeId id) {
                    return encodingId.equals(id) ? codec : types.getCodec(id);
                }

                @Override
                public @Nullable NodeId getBinaryEncodingId(final @NotNull NodeId dataTypeId) {
                    return types.getBinaryEncodingId(dataTypeId);
                }

                @Override
                public @Nullable NodeId getXmlEncodingId(final @NotNull NodeId dataTypeId) {
                    return types.getXmlEncodingId(dataTypeId);
                }

                @Override
                public @Nullable NodeId getJsonEncodingId(final @NotNull NodeId dataTypeId) {
                    return types.getJsonEncodingId(dataTypeId);
                }

                @Override
                public @Nullable DataTypeDictionary getTypeDictionary(final @NotNull String namespaceUri) {
                    return types.getTypeDictionary(namespaceUri);
                }

                @Override
                public void registerTypeDictionary(final @NotNull DataTypeDictionary dictionary) {
                    types.registerTypeDictionary(dictionary);
                }
            };
        }

        @Override
        public @NotNull EncodingManager getEncodingManager() {
            return delegate.getEncodingManager();
        }

        @Override
        public @NotNull EncodingLimits getEncodingLimits() {
            return delegate.getEncodingLimits();
        }

        @Override
        public @NotNull NamespaceTable getNamespaceTable() {
            return delegate.getNamespaceTable();
        }

        @Override
        public @NotNull ServerTable getServerTable() {
            return delegate.getServerTable();
        }
    }
}
