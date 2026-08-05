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
package com.hivemq.edge.adapters.opcua.northbound;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.datapoint.DataPointWithMetadata;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import java.util.UUID;
import org.eclipse.milo.opcua.stack.core.encoding.DefaultEncodingContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.enumerated.IdType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * EDG-835: a published node id writes {@code namespaceIndex} as the integer it is, for every namespace.
 * <p>
 * This pins the fix for a defect that survived from 2023: the field was written as the whole parseable
 * node id ({@code "ns=2;s=Boiler1.Temperature"}) for every index except 1, so a field named for the
 * namespace index held something that was not one, and namespace 0 produced {@code "i=9482"} -- a string
 * naming no namespace at all. Several namespaces are covered here because the old code produced
 * different shapes for them, and only namespace 1 -- the one our test server uses -- was correct.
 */
class OpcUaToJsonConverterNodeIdTest {

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 300})
    void whenNodeIdPublished_thenNamespaceIndexIsTheInteger(final int namespaceIndex) {
        final JsonNode value = convert(new NodeId(namespaceIndex, "Boiler1.Temperature"));

        assertThat(value.get("namespaceIndex").isInt())
                .as("namespaceIndex must be a JSON integer, not a string holding a whole node id")
                .isTrue();
        assertThat(value.get("namespaceIndex").intValue()).isEqualTo(namespaceIndex);
    }

    @Test
    void whenNumericIdentifier_thenIdTypeAndIdAccompanyTheIndex() {
        final JsonNode value = convert(new NodeId(2, 9482));

        assertThat(value.get("namespaceIndex").intValue()).isEqualTo(2);
        assertThat(value.get("idType").intValue()).isEqualTo(IdType.Numeric.getValue());
        assertThat(value.get("id").longValue()).isEqualTo(9482L);
    }

    @Test
    void whenStringIdentifier_thenIdCarriesTheIdentifierAloneNotTheWholeNodeId() {
        final JsonNode value = convert(new NodeId(2, "Boiler1.Temperature"));

        assertThat(value.get("idType").intValue()).isEqualTo(IdType.String.getValue());
        assertThat(value.get("id").textValue()).isEqualTo("Boiler1.Temperature");
    }

    @Test
    void whenGuidIdentifier_thenIdTypeIsGuid() {
        final UUID uuid = UUID.fromString("72962b91-fa75-4ae6-8d28-b404dc7daf63");
        final JsonNode value = convert(new NodeId(2, uuid));

        assertThat(value.get("namespaceIndex").intValue()).isEqualTo(2);
        assertThat(value.get("idType").intValue()).isEqualTo(IdType.Guid.getValue());
        assertThat(value.get("id").textValue()).isEqualTo(uuid.toString());
    }

    @Test
    void whenOpaqueIdentifier_thenIdTypeIsOpaque() {
        final JsonNode value = convert(new NodeId(2, ByteString.of(new byte[] {1, 2, 3})));

        assertThat(value.get("namespaceIndex").intValue()).isEqualTo(2);
        assertThat(value.get("idType").intValue()).isEqualTo(IdType.Opaque.getValue());
        assertThat(value.get("id").textValue()).isEqualTo("AQID");
    }

    /** Runs a single NodeId value through the converter and returns the emitted {@code value} node. */
    private static JsonNode convert(final NodeId nodeId) {
        final DataValue dataValue = new DataValue(new Variant(nodeId), StatusCode.GOOD, null);
        final var builder = (DataPointWithMetadata.DataPointBuilderImpl<Void>) DataPointWithMetadata.<Void>builder(
                new OpcuaTag("test-tag", "", new OpcuaTagDefinition("ns=2;i=1001")), b -> null);

        OpcUaToJsonConverter.convertPayload(DefaultEncodingContext.INSTANCE, dataValue, builder);

        return builder.build("test-adapter").getTagValue();
    }
}
