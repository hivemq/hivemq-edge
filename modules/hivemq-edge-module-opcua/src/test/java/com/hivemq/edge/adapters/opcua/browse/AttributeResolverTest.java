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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.core.typetree.DataType;
import org.eclipse.milo.opcua.sdk.core.typetree.DataTypeTree;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** {@link AttributeResolver}: the three attribute values turned into the strings a BrowsedNode carries. */
class AttributeResolverTest {

    private static @NotNull DataValue value(final @NotNull Object v) {
        return new DataValue(new Variant(v));
    }

    // --- data type ---

    @Test
    void dataTypeName_fromTheTree_isTheBrowseName() {
        final DataTypeTree tree = mock(DataTypeTree.class);
        final DataType int32 = mock(DataType.class);
        when(int32.getBrowseName()).thenReturn(new QualifiedName(0, "Int32"));
        when(tree.getDataType(NodeIds.Int32)).thenReturn(int32);

        assertThat(new AttributeResolver(tree).dataTypeName(value(NodeIds.Int32)))
                .isEqualTo("Int32");
    }

    @Test
    void dataTypeName_unknownToTheTree_isTheNodeId() {
        final DataTypeTree tree = mock(DataTypeTree.class);
        final NodeId custom = NodeId.parse("ns=3;i=3001");

        assertThat(new AttributeResolver(tree).dataTypeName(value(custom))).isEqualTo("ns=3;i=3001");
    }

    @Test
    void dataTypeName_treeKnowsTheTypeButNotItsName_isTheNodeId() {
        final DataTypeTree tree = mock(DataTypeTree.class);
        final DataType nameless = mock(DataType.class);
        when(nameless.getBrowseName()).thenReturn(QualifiedName.NULL_VALUE);
        when(tree.getDataType(NodeIds.Int32)).thenReturn(nameless);

        assertThat(new AttributeResolver(tree).dataTypeName(value(NodeIds.Int32)))
                .isEqualTo("i=6");
    }

    @Test
    void dataTypeName_treeKnowsTheTypeWithoutABrowseName_isTheNodeId() {
        final DataTypeTree tree = mock(DataTypeTree.class);
        when(tree.getDataType(NodeIds.Int32)).thenReturn(mock(DataType.class)); // getBrowseName() -> null

        assertThat(new AttributeResolver(tree).dataTypeName(value(NodeIds.Int32)))
                .isEqualTo("i=6");
    }

    @Test
    void dataTypeName_withoutATree_isTheNodeId() {
        assertThat(new AttributeResolver(null).dataTypeName(value(NodeIds.Double)))
                .isEqualTo("i=11");
    }

    @Test
    void dataTypeName_valueThatIsNotANodeId_isUnknown() {
        assertThat(new AttributeResolver(null).dataTypeName(value("Int32"))).isEqualTo("Unknown");
        assertThat(new AttributeResolver(null).dataTypeName(new DataValue(Variant.NULL_VALUE)))
                .isEqualTo("Unknown");
    }

    // --- access level ---

    @ParameterizedTest(name = "AccessLevel byte {0} -> {1}")
    @CsvSource({
        "3, READ_WRITE", // CurrentRead | CurrentWrite
        "1, READ",
        "2, WRITE",
        "0, NONE",
        "7, READ_WRITE", // HistoryRead set too: only the Current bits count
        "4, NONE", // HistoryRead only
    })
    void accessLevel_fromTheCurrentReadAndWriteBits(final int bits, final @NotNull String expected) {
        assertThat(new AttributeResolver(null).accessLevel(value(Unsigned.ubyte(bits))))
                .isEqualTo(expected);
    }

    @Test
    void accessLevel_valueThatIsNotAByte_defaultsToRead() {
        assertThat(new AttributeResolver(null).accessLevel(value(3))).isEqualTo("READ");
        assertThat(new AttributeResolver(null).accessLevel(new DataValue(Variant.NULL_VALUE)))
                .isEqualTo("READ");
    }

    // --- description ---

    @Test
    void description_localizedText_isItsText() {
        assertThat(new AttributeResolver(null).description(value(LocalizedText.english("motor speed"))))
                .isEqualTo("motor speed");
        assertThat(new AttributeResolver(null).description(value(new LocalizedText("de", null))))
                .isNull();
    }

    @Test
    void description_anythingElse_isNull() {
        assertThat(new AttributeResolver(null).description(value("plain"))).isNull();
        assertThat(new AttributeResolver(null).description(new DataValue(Variant.NULL_VALUE)))
                .isNull();
    }

    // --- construction from a client ---

    @Test
    void forClient_usesTheClientsTree() throws Exception {
        final OpcUaClient client = mock(OpcUaClient.class);
        final DataTypeTree tree = mock(DataTypeTree.class);
        final DataType bool = mock(DataType.class);
        when(bool.getBrowseName()).thenReturn(new QualifiedName(0, "Boolean"));
        when(tree.getDataType(NodeIds.Boolean)).thenReturn(bool);
        when(client.getDataTypeTree()).thenReturn(tree);

        assertThat(AttributeResolver.forClient(client).dataTypeName(value(NodeIds.Boolean)))
                .isEqualTo("Boolean");
    }

    @Test
    void forClient_treeUnavailable_fallsBackToNodeIds() throws Exception {
        final OpcUaClient client = mock(OpcUaClient.class);
        when(client.getDataTypeTree()).thenThrow(new UaException(StatusCodes.Bad_SessionClosed));

        assertThat(AttributeResolver.forClient(client).dataTypeName(value(NodeIds.Boolean)))
                .isEqualTo("i=1");
    }
}
