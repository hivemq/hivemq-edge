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
package com.hivemq.edge.adapters.opcua.browse;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.core.typetree.DataType;
import org.eclipse.milo.opcua.sdk.core.typetree.DataTypeTree;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.structured.AccessLevelType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Turns the three attribute values Phase 2 reads per variable into the strings a {@code BrowsedNode} carries.
 * Data type names come from Milo's {@link DataTypeTree}, which handles built-in and server-defined types
 * alike; without a tree the type's node id is reported instead.
 */
final class AttributeResolver {

    private final @Nullable DataTypeTree dataTypeTree;

    AttributeResolver(final @Nullable DataTypeTree dataTypeTree) {
        this.dataTypeTree = dataTypeTree;
    }

    /**
     * With the client's data type tree. Milo builds it on first use by browsing the DataType hierarchy, then
     * caches it per session; when that fails the resolver falls back to node ids.
     */
    static @NotNull AttributeResolver forClient(final @NotNull OpcUaClient client) {
        try {
            return new AttributeResolver(client.getDataTypeTree());
        } catch (final UaException e) {
            return new AttributeResolver(null);
        }
    }

    /** The data type's browse name, its node id when the tree does not know it, "Unknown" when not a node id. */
    @NotNull
    String dataTypeName(final @NotNull DataValue dataTypeValue) {
        if (!(dataTypeValue.getValue().getValue() instanceof final NodeId dataTypeNodeId)) {
            return "Unknown";
        }
        if (dataTypeTree != null) {
            final DataType dataType = dataTypeTree.getDataType(dataTypeNodeId);
            if (dataType != null
                    && dataType.getBrowseName() != null
                    && dataType.getBrowseName().getName() != null) {
                return dataType.getBrowseName().getName();
            }
        }
        return dataTypeNodeId.toParseableString();
    }

    /** READ_WRITE, READ, WRITE or NONE from the CurrentRead/CurrentWrite bits; READ when the value is not a byte. */
    @NotNull
    String accessLevel(final @NotNull DataValue accessLevelValue) {
        if (accessLevelValue.getValue().getValue() instanceof final UByte accessByte) {
            final AccessLevelType accessLevel = new AccessLevelType(accessByte);
            final boolean readable = accessLevel.getCurrentRead();
            final boolean writable = accessLevel.getCurrentWrite();
            if (readable && writable) {
                return "READ_WRITE";
            }
            if (readable) {
                return "READ";
            }
            if (writable) {
                return "WRITE";
            }
            return "NONE";
        }
        return "READ";
    }

    @Nullable
    String description(final @NotNull DataValue descriptionValue) {
        if (descriptionValue.getValue().getValue() instanceof final LocalizedText text) {
            return text.getText();
        }
        return null;
    }
}
