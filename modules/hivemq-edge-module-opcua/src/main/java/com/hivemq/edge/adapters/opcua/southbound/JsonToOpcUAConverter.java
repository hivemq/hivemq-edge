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

import static com.hivemq.edge.adapters.opcua.southbound.JsonToOpcUAConverterUtil.collectCustomDatatypes;
import static com.hivemq.edge.adapters.opcua.southbound.JsonToOpcUAConverterUtil.generateArrayFromArrayNode;
import static com.hivemq.edge.adapters.opcua.southbound.JsonToOpcUAConverterUtil.parsetoOpcUAObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.core.types.DynamicStructType;
import org.eclipse.milo.opcua.sdk.core.types.codec.DynamicStructCodec;
import org.eclipse.milo.opcua.sdk.core.typetree.DataType;
import org.eclipse.milo.opcua.sdk.core.typetree.DataTypeTree;
import org.eclipse.milo.opcua.stack.core.OpcUaDataType;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExtensionObject;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonToOpcUAConverter {

    private static final @NotNull Logger log = LoggerFactory.getLogger(JsonToOpcUAConverter.class);

    private final @NotNull OpcUaClient client;

    public JsonToOpcUAConverter(final @NotNull OpcUaClient client) {
        this.client = client;
    }

    /// The server's DataType hierarchy, read on first use rather than in the constructor.
    ///
    /// **Reading it eagerly made a value nobody asked about decide whether a write could happen at all.** The
    /// hierarchy is a recursive browse of the whole DataType tree, and a server may stay connected while
    /// refusing or timing out that one browse -- an authorization rule on the type nodes is enough. The
    /// constructor turned that into a `RuntimeException` before any value had been looked at, so a write of a
    /// plain `Int32` failed for a hierarchy it never needed, and a method request on the same adapter was
    /// dropped without an answer because this object is built before the branch on tag kind.
    ///
    /// **Nothing below the ExtensionObject branch needs it.** A builtin DataType is identified by its node id
    /// alone -- OPC 10000-6 fixes the 25 identifiers, and `OpcUaDataType.fromNodeId` is that table -- so an
    /// `Int32`, a `Double`, a `String` and every array of one are encoded without a server. The hierarchy
    /// answers two questions beyond the table: what a **Simple DataType** reduces to, and how to encode a
    /// **structure**. Only those reach for it.
    ///
    /// Costs nothing when it is needed: Milo caches the tree on the client after the first read, and the
    /// adapter warms that cache during startup (EDG-488).
    ///
    /// Reported by Miguel as R6-3 on EDG-1087, for the method path. The same eagerness was on the value path,
    /// and splitting the rule between them would have been a difference with no reason behind it.
    private @NotNull DataTypeTree tree() {
        try {
            return client.getDataTypeTree();
        } catch (final UaException e) {
            throw new RuntimeException(e);
        }
    }

    public @NotNull Object convertToOpcUAValue(
            final @NotNull JsonNode rootNode, final @NotNull NodeId destinationNodeId) {
        if (log.isDebugEnabled()) {
            log.debug(
                    "Convert json '{}' to opcua compatible object for destination nodeId '{}'.",
                    rootNode,
                    destinationNodeId);
        }
        try {
            final NodeId dataTypeNodeId =
                    client.getAddressSpace().getVariableNode(destinationNodeId).getDataType();
            if (dataTypeNodeId == null) {
                log.warn("No dataType-nodeId was found for the destination nodeId '{}'.", destinationNodeId);
                throw new RuntimeException(
                        "No dataType-nodeId was found for the destination nodeId " + destinationNodeId + "'");
            }
            if (log.isDebugEnabled()) {
                log.debug("Destination NodeId '{}' has DataType NodeId '{}'.", destinationNodeId, dataTypeNodeId);
            }

            // The static table first, and for a builtin destination that is the whole of it. OPC 10000-6 fixes
            // the 25 builtin DataType node ids, so `i=6` *is* Int32 with nothing to look up on the server --
            // and a value of a builtin type is encoded from that answer alone. Asking the hierarchy here
            // instead made every write depend on a recursive browse that the write did not need.
            final OpcUaDataType tabulated = OpcUaDataType.fromNodeId(dataTypeNodeId);
            if (tabulated != null && tabulated != OpcUaDataType.ExtensionObject) {
                if (log.isDebugEnabled()) {
                    log.debug(
                            "Destination Node '{}' has builtin DataType NodeId '{}' representing '{}', read from the "
                                    + "builtin table without the DataType hierarchy. The Json '{}' is parsed to this.",
                            destinationNodeId,
                            dataTypeNodeId,
                            tabulated,
                            rootNode);
                }
                return rootNode.isArray()
                        ? generateArrayFromArrayNode((ArrayNode) rootNode, tabulated, client.getNamespaceTable())
                        : parsetoOpcUAObject(tabulated, rootNode, client.getNamespaceTable());
            }

            // Past the table: a Simple DataType to reduce, or a structure to encode. Both are what the
            // hierarchy is for, so this is where it is read.
            final DataTypeTree tree = tree();
            final var dataType = tree.getDataType(dataTypeNodeId);
            if (dataType == null) {
                log.warn("No data type was found in the DataTypeTree for dataType with nodeId '{}'.", dataTypeNodeId);
                throw new RuntimeException(
                        "No data type was found in the DataTypeTree for node id '" + dataTypeNodeId + "'");
            }
            if (log.isDebugEnabled()) {
                log.debug(
                        "DataType NodeId '{}' represents data type '{}'.",
                        dataTypeNodeId,
                        dataType.getBrowseName().getName());
            }

            final var builtinType = tree.getBuiltinType(dataType.getNodeId());

            if (log.isDebugEnabled()) {
                log.debug(
                        "Destination Node '{}' has DataType NodeId '{}' representing DataType '{}' with builtin type '{}'. The Json '{}' is parsed to this.",
                        destinationNodeId,
                        dataTypeNodeId,
                        dataType,
                        builtinType,
                        rootNode);
            }

            if (builtinType != OpcUaDataType.ExtensionObject) {
                if (rootNode.isArray()) {
                    return generateArrayFromArrayNode((ArrayNode) rootNode, builtinType, client.getNamespaceTable());
                } else {
                    return parsetoOpcUAObject(builtinType, rootNode, client.getNamespaceTable());
                }
            }

            // readable/writable flags only influence schema generation; this is the write path,
            // so defaults are fine.
            // Built here rather than in the constructor: it reads the hierarchy too, and only a structure
            // needs it. See tree().
            final var field = new JsonSchemaGenerator(client).processExtensionObject(dataType, true, true, true, null);

            final var dataTypesToRegister = new ArrayList<DataType>();
            collectCustomDatatypes(field, dataTypesToRegister);
            dataTypesToRegister.forEach(dataTypeToRegister -> {
                try {
                    client.getStaticDataTypeManager()
                            .registerType(
                                    dataTypeToRegister.getNodeId(),
                                    new DynamicStructCodec(dataTypeToRegister, client.getDataTypeTree()),
                                    dataTypeToRegister.getBinaryEncodingId(),
                                    null,
                                    null);
                } catch (final UaException e) {
                    throw new RuntimeException(e);
                }
            });

            final LinkedHashMap<String, Object> dataTypeMap = new LinkedHashMap<>();
            field.nestedFields().forEach(nestedField -> {
                final var key = nestedField.name();
                final var jsonNode = rootNode.get(key);
                dataTypeMap.put(key, parseToOpcUACompatibleObject(jsonNode, nestedField));
            });

            return ExtensionObject.encode(
                    client.getDynamicEncodingContext(), new DynamicStructType(dataType, dataTypeMap));
        } catch (final UaException e) {
            throw new RuntimeException(e);
        }
    }

    private @NotNull Object parseToOpcUACompatibleObject(
            final @NotNull JsonNode jsonNode, final @NotNull JsonSchemaGenerator.FieldInformation fieldType) {
        final OpcUaDataType builtinDataType = fieldType.dataType();

        if (builtinDataType == OpcUaDataType.ExtensionObject || fieldType.customDataType() != null) {
            final String namespaceURI = fieldType.namespaceUri();
            final DataType customDataType = Objects.requireNonNull(fieldType.customDataType());
            final ExpandedNodeId expandedNodeId = ExpandedNodeId.of(
                    Objects.requireNonNull(namespaceURI),
                    customDataType.getBrowseName().getName());

            final Optional<NodeId> optionalDataTypeId = expandedNodeId.toNodeId(client.getNamespaceTable());
            if (optionalDataTypeId.isEmpty()) {
                log.warn("Expanded node id '{}}' could not be parsed to node id.", expandedNodeId);
                throw new RuntimeException("Expanded node id '" + expandedNodeId + "' could not be parsed to node id.");
            }

            final NodeId dataTypeId = optionalDataTypeId.get();
            // A nested structure field, so the hierarchy is genuinely needed here -- see tree().
            final DataType dataType = tree().getDataType(dataTypeId);

            if (dataType == null) {
                log.warn("No data type was found in the DataTypeTree for dataType with nodeId '{}'.", dataTypeId);
                throw new RuntimeException(
                        "No data type was found in the DataTypeTree for node id '" + dataTypeId + "'");
            }

            final NodeId binaryEncodingId = dataType.getBinaryEncodingId();
            log.debug(
                    "FieldType '{}' with ExpandedNodeId '{}' resolved to DataType NodeId '{}' representing DataType '{}', which has BinaryEncodingId '{}' and represents the builtin type '{}'. The Json '{}' is parsed to this.",
                    fieldType,
                    expandedNodeId,
                    dataTypeId,
                    dataType,
                    binaryEncodingId,
                    builtinDataType,
                    jsonNode);

            if (binaryEncodingId == null) {
                throw new IllegalStateException("Binary encoding id was null for nested struct.");
            }
            return extractExtensionObject(jsonNode, fieldType);
        }

        return parsetoOpcUAObject(Objects.requireNonNull(builtinDataType), jsonNode, client.getNamespaceTable());
    }

    private @NotNull DynamicStructType extractExtensionObject(
            final @NotNull JsonNode jsonNode, final @NotNull JsonSchemaGenerator.FieldInformation fieldInformation) {

        final var fields = new LinkedHashMap<String, Object>();
        fieldInformation.nestedFields().forEach(field -> {
            final String key = field.name();
            final JsonNode nestedObjectNode = jsonNode.get(key);
            if (nestedObjectNode == null) {
                throw new RuntimeException("No nested json was found for key '" + key + "'.");
            }
            final Object parsed = parseToOpcUACompatibleObject(nestedObjectNode, field);
            fields.put(key, parsed);
        });

        return new DynamicStructType(Objects.requireNonNull(fieldInformation.customDataType()), fields);
    }
}
