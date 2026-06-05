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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hivemq.edge.adapters.opcua.southbound.JsonSchemaGenerator.FieldInformation;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.core.typetree.DataType;
import org.eclipse.milo.opcua.sdk.core.typetree.DataTypeTree;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.OpcUaDataType;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.enumerated.StructureType;
import org.eclipse.milo.opcua.stack.core.types.structured.StructureDefinition;
import org.eclipse.milo.opcua.stack.core.types.structured.StructureField;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JsonSchemaGenerator#processExtensionObject} field resolution.
 * <p>
 * These mock the milo {@link OpcUaClient} / {@link DataTypeTree} so they run without an OPC UA server
 * and stay deterministic. They guard the fix for the "Unsupported type definition: null" 500: struct
 * fields are resolved via {@link DataTypeTree#getBuiltinType(NodeId)} rather than by namespace prefix,
 * and field types that are not scalar-mappable (enumerations, abstract / unresolvable types) degrade to
 * {@code any} instead of throwing.
 */
class JsonSchemaGeneratorProcessExtensionObjectTest {

    private static final String TEST_NS = "urn:test";

    private final NamespaceTable namespaceTable = newNamespaceTable();
    private final DataTypeTree tree = mock(DataTypeTree.class);
    private final OpcUaClient client = mock(OpcUaClient.class);

    private static NamespaceTable newNamespaceTable() {
        final NamespaceTable table = new NamespaceTable();
        table.add(TEST_NS); // index 1, for custom-namespace field types
        return table;
    }

    private JsonSchemaGenerator newGenerator() throws Exception {
        when(client.getDataTypeTree()).thenReturn(tree);
        when(client.getNamespaceTable()).thenReturn(namespaceTable);
        return new JsonSchemaGenerator(client);
    }

    private static StructureField scalarField(final String name, final NodeId dataType) {
        return new StructureField(name, LocalizedText.NULL_VALUE, dataType, ValueRanks.Scalar, null, uint(0), false);
    }

    private static StructureField arrayField(final String name, final NodeId elementType, final int... dimensions) {
        final org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger[] arrayDimensions =
                new org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger[dimensions.length];
        for (int i = 0; i < dimensions.length; i++) {
            arrayDimensions[i] = uint(dimensions[i]);
        }
        return new StructureField(
                name, LocalizedText.NULL_VALUE, elementType, dimensions.length, arrayDimensions, uint(0), false);
    }

    private static DataType structDataType(final String browseName, final StructureField... fields) {
        final DataType dataType = mock(DataType.class);
        when(dataType.getBrowseName()).thenReturn(new QualifiedName(0, browseName));
        when(dataType.getDataTypeDefinition())
                .thenReturn(
                        new StructureDefinition(NodeId.NULL_VALUE, NodeIds.Structure, StructureType.Structure, fields));
        return dataType;
    }

    private static FieldInformation fieldNamed(final FieldInformation parent, final String name) {
        return parent.nestedFields().stream()
                .filter(f -> name.equals(f.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no field named '" + name + "' in " + parent.nestedFields()));
    }

    @Test
    void processExtensionObject_scalarBuiltinAndSubtypeFields_mapToScalars() throws Exception {
        // 'alias' is a custom (non-OPC-Foundation) DataType that resolves to the builtin String.
        final NodeId aliasType = new NodeId(1, "StringAlias");
        final StructureField alias = scalarField("alias", aliasType);
        final StructureField bar = scalarField("bar", NodeIds.UInt32);
        final StructureField baz = scalarField("baz", NodeIds.Boolean);

        when(tree.getBuiltinType(aliasType)).thenReturn(OpcUaDataType.String);
        when(tree.getBuiltinType(NodeIds.UInt32)).thenReturn(OpcUaDataType.UInt32);
        when(tree.getBuiltinType(NodeIds.Boolean)).thenReturn(OpcUaDataType.Boolean);

        final FieldInformation info = newGenerator()
                .processExtensionObject(structDataType("Root", alias, bar, baz), true, true, true, "root");

        assertThat(info.nestedFields()).hasSize(3);
        // The custom String subtype is resolved to a string scalar (previously this threw).
        assertThat(fieldNamed(info, "alias").dataType()).isEqualTo(OpcUaDataType.String);
        assertThat(fieldNamed(info, "alias").nestedFields()).isEmpty();
        assertThat(fieldNamed(info, "bar").dataType()).isEqualTo(OpcUaDataType.UInt32);
        assertThat(fieldNamed(info, "baz").dataType()).isEqualTo(OpcUaDataType.Boolean);
    }

    @Test
    void processExtensionObject_enumOrUnresolvableField_rendersAsAnyWithoutThrowing() throws Exception {
        // Enumeration subtypes (and abstract / unresolvable types) resolve to Variant in milo.
        final NodeId enumType = new NodeId(1, "MyEnum");
        final StructureField enumField = scalarField("en", enumType);
        when(tree.getBuiltinType(enumType)).thenReturn(OpcUaDataType.Variant);

        final JsonSchemaGenerator generator = newGenerator();
        final DataType root = structDataType("Root", enumField);

        assertThatCode(() -> generator.processExtensionObject(root, true, true, true, "root"))
                .doesNotThrowAnyException();

        final FieldInformation info = generator.processExtensionObject(root, true, true, true, "root");
        // dataType == null + no nested fields -> rendered as `any` by the schema builder.
        assertThat(fieldNamed(info, "en").dataType()).isNull();
        assertThat(fieldNamed(info, "en").nestedFields()).isEmpty();
    }

    @Test
    void processExtensionObject_genuineNestedStructField_recursesIntoNestedObject() throws Exception {
        final NodeId nestedType = new NodeId(1, "NestedStruct");
        final StructureField nestedField = scalarField("nested", nestedType);
        final StructureField inner = scalarField("inner", NodeIds.Boolean);

        // Build the nested DataType first — Mockito forbids stubbing (inside structDataType) within an
        // unfinished when(...).thenReturn(...).
        final DataType nestedDataType = structDataType("NestedStruct", inner);
        final DataType rootDataType = structDataType("Root", nestedField);

        when(tree.getBuiltinType(nestedType)).thenReturn(OpcUaDataType.ExtensionObject);
        when(tree.getBuiltinType(NodeIds.Boolean)).thenReturn(OpcUaDataType.Boolean);
        when(tree.getDataType(nestedType)).thenReturn(nestedDataType);

        final FieldInformation info = newGenerator().processExtensionObject(rootDataType, true, true, true, "root");

        final FieldInformation nested = fieldNamed(info, "nested");
        assertThat(nested.nestedFields()).hasSize(1);
        assertThat(fieldNamed(nested, "inner").dataType()).isEqualTo(OpcUaDataType.Boolean);
    }

    @Test
    void processExtensionObject_arrayField_isModelledAsArray() throws Exception {
        // A struct field that is itself an array (e.g. Int16[10], ValueRank 1) must keep its array
        // dimensions so it renders as an array of integers, not a scalar.
        final StructureField scalarInt = scalarField("scalarInt", NodeIds.Int32);
        final StructureField intArr = arrayField("intArr", NodeIds.Int16, 10);

        when(tree.getBuiltinType(NodeIds.Int32)).thenReturn(OpcUaDataType.Int32);
        when(tree.getBuiltinType(NodeIds.Int16)).thenReturn(OpcUaDataType.Int16);

        final FieldInformation info = newGenerator()
                .processExtensionObject(structDataType("Root", scalarInt, intArr), true, true, true, "root");

        assertThat(fieldNamed(info, "scalarInt").dataType()).isEqualTo(OpcUaDataType.Int32);
        assertThat(fieldNamed(info, "scalarInt").arrayDimensions()).isNull();

        final FieldInformation arrayInfo = fieldNamed(info, "intArr");
        assertThat(arrayInfo.dataType()).isEqualTo(OpcUaDataType.Int16);
        assertThat(arrayInfo.arrayDimensions()).containsExactly(uint(10));
    }

    @Test
    void processExtensionObject_complexTypeWithNullDefinition_degradesGracefully() throws Exception {
        // A complex type whose DataTypeDefinition is null (neither Structure nor Enum) must not 500.
        final DataType undefined = mock(DataType.class);
        when(undefined.getBrowseName()).thenReturn(new QualifiedName(0, "Undefined"));
        when(undefined.getDataTypeDefinition()).thenReturn(null);

        final JsonSchemaGenerator generator = newGenerator();

        assertThatCode(() -> generator.processExtensionObject(undefined, true, true, true, "root"))
                .doesNotThrowAnyException();

        final FieldInformation info = generator.processExtensionObject(undefined, true, true, true, "root");
        assertThat(info.dataType()).isNull();
        assertThat(info.nestedFields()).isEmpty();
    }
}
