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

import com.hivemq.adapter.sdk.api.schema.ScalarType;
import org.eclipse.milo.opcua.stack.core.OpcUaDataType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Unit tests for the OPC UA → SDK type mapping done in
 * {@link JsonSchemaGenerator#mapOpcUaToScalarType(OpcUaDataType)}.
 */
class JsonSchemaGeneratorMappingTest {

    @Test
    void test_dateTime_mapsToInstant() {
        assertThat(JsonSchemaGenerator.mapOpcUaToScalarType(OpcUaDataType.DateTime))
                .isEqualTo(ScalarType.INSTANT);
    }

    /**
     * Every OPC UA scalar type that has a mapping must produce a non-null {@link ScalarType}.
     * Types that are intentionally unsupported ({@code QualifiedName}, {@code ExtensionObject},
     * {@code DataValue}, {@code Variant}, {@code DiagnosticInfo}) throw — we assert that
     * separately and exclude them here.
     */
    @ParameterizedTest
    @EnumSource(
            value = OpcUaDataType.class,
            mode = EnumSource.Mode.EXCLUDE,
            names = {"QualifiedName", "ExtensionObject", "DataValue", "Variant", "DiagnosticInfo"})
    void test_allScalarTypesMapToAScalarType(final OpcUaDataType opcUaType) {
        assertThat(JsonSchemaGenerator.mapOpcUaToScalarType(opcUaType)).isNotNull();
    }
}
