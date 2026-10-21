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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.datapoint.DataPointWithMetadata;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import com.hivemq.edge.adapters.opcua.southbound.JsonToOpcUaValueConverter;
import java.util.Objects;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.core.typetree.DataType;
import org.eclipse.milo.opcua.sdk.core.typetree.DataTypeTree;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.encoding.DefaultEncodingContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExtensionObject;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.EUInformation;
import org.eclipse.milo.opcua.stack.core.types.structured.Range;
import org.eclipse.milo.opcua.stack.core.types.structured.TimeZoneDataType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import util.EmbeddedOpcUaServerExtension;

/// A standard structure -- one of OPC UA's own, which Milo decodes into a generated class -- is published field by
/// field, against the embedded server's own types.
///
/// Before, every one of them except `TimeZoneDataType` and `EUInformation` fell through to `toString`, so an
/// analog item's `EURange` arrived as `Range{low=1.5, high=9.0}`.
class StandardStructurePublishIT {

    @RegisterExtension
    public final @NotNull EmbeddedOpcUaServerExtension opcUaServerExtension = new EmbeddedOpcUaServerExtension();

    private @NotNull OpcUaClient client;

    @BeforeEach
    void setUp() throws Exception {
        client = OpcUaClient.create(opcUaServerExtension.getServerUri());
        client.connect();
    }

    @AfterEach
    void tearDown() throws Exception {
        client.disconnect();
    }

    @Test
    void aRange_isPublishedWithTheFieldsOfItsDefinition() {
        final JsonNode published = publish(Decoding.of(client), new DataValue(new Variant(new Range(1.5, 9.0))));

        assertThat(published.get("Low").asDouble()).isEqualTo(1.5);
        assertThat(published.get("High").asDouble()).isEqualTo(9.0);
        assertThat(published.size()).isEqualTo(2);
    }

    @Test
    void theServersStatus_isPublishedWithItsNestedStructureAndItsEnumeration() throws Exception {
        // ServerStatusDataType, read from the server's own Server/ServerStatus node: a nested standard structure
        // (BuildInfo), timestamps, and the ServerState enumeration -- each rendered by the converter that already
        // renders vendor structures.
        final DataValue status = client.readValue(0.0, TimestampsToReturn.Neither, NodeIds.Server_ServerStatus);

        final JsonNode published = publish(Decoding.of(client), status);

        assertThat(published.get("State")).isEqualTo(enumValue("Running", 0));
        assertThat(published.get("StartTime").isTextual()).isTrue();
        assertThat(published.at("/BuildInfo/ProductUri").isTextual()).isTrue();
        assertThat(published.at("/BuildInfo/BuildDate").isTextual()).isTrue();
    }

    @Test
    void withoutTheTypeHierarchy_aStandardStructureIsPublishedAsBefore() throws Exception {
        // The hierarchy is optional to the read path: a server refusing the type browse costs these values their
        // structure and nothing else.
        final OpcUaClient treeless = spy(client);
        doThrow(new UaException(StatusCodes.Bad_UserAccessDenied))
                .when(treeless)
                .getDataTypeTree();

        final JsonNode published = publish(Decoding.of(treeless), new DataValue(new Variant(new Range(1.5, 9.0))));

        assertThat(published.isTextual()).isTrue();
        assertThat(published.asText()).contains("1.5").contains("9.0");
    }

    @Test
    void aServerThatPublishesNoDefinition_stillGetsTheRangeAsAnObject() throws Exception {
        // EDG-1138, EDG-1119 D38: the S7-1500 and the WAGO publish no definitions for OPC UA's own structures.
        // Milo's generated class carries the definition the specification fixes, so Edge uses that instead.
        final DataTypeTree real = client.getDataTypeTree();
        final DataTypeTree withoutDefinition = spy(real);
        final DataType undescribed = mock(DataType.class);
        when(undescribed.getNodeId()).thenReturn(NodeIds.Range);
        when(undescribed.getDataTypeDefinition()).thenReturn(null);
        doReturn(undescribed).when(withoutDefinition).getDataType(NodeIds.Range);

        final JsonNode published =
                publish(decodingWith(withoutDefinition), new DataValue(new Variant(new Range(1.5, 9.0))));

        assertThat(published.get("Low").asDouble()).isEqualTo(1.5);
        assertThat(published.get("High").asDouble()).isEqualTo(9.0);
    }

    @Test
    void aServerWhoseHierarchyLacksTheType_stillGetsTheRangeAsAnObject() throws Exception {
        final DataTypeTree real = client.getDataTypeTree();
        final DataTypeTree withoutType = spy(real);
        doReturn(null).when(withoutType).getDataType(NodeIds.Range);

        final JsonNode published = publish(decodingWith(withoutType), new DataValue(new Variant(new Range(1.5, 9.0))));

        assertThat(published.get("Low").asDouble()).isEqualTo(1.5);
        assertThat(published.get("High").asDouble()).isEqualTo(9.0);
    }

    private @NotNull Decoding decodingWith(final @NotNull DataTypeTree tree) {
        return new Decoding(
                () -> {
                    try {
                        return client.getDynamicEncodingContext();
                    } catch (final UaException e) {
                        throw new IllegalStateException(e);
                    }
                },
                () -> tree);
    }

    @Test
    void theTwoStructuresRenderedByHand_areSentBackUnchanged() {
        // EUInformation and TimeZoneDataType are rendered by their own code, because every alarm event carries
        // LocalTime and the hand-written shape needs no type hierarchy. Their field names are the definition's
        // own, so the southbound converter -- which checks a structure against that definition -- accepts what
        // was published.
        final EUInformation celsius = new EUInformation(
                "http://www.opcfoundation.org/UA/units/un/cefact",
                4408652,
                LocalizedText.english("°C"),
                LocalizedText.english("degree Celsius"));
        final TimeZoneDataType cologne = new TimeZoneDataType((short) 120, true);

        assertThat(sentBack(celsius, NodeIds.EUInformation)).isEqualTo(celsius);
        assertThat(sentBack(cologne, NodeIds.TimeZoneDataType)).isEqualTo(cologne);
    }

    /// Publishes the value, converts what was published back as the given DataType, and decodes the result.
    private @NotNull Object sentBack(final @NotNull Object value, final @NotNull NodeId dataType) {
        final JsonNode published = publish(Decoding.of(client), new DataValue(new Variant(value)));
        final Variant converted =
                JsonToOpcUaValueConverter.of(client).toVariant(published, dataType, ValueRanks.Scalar);
        return ((ExtensionObject) Objects.requireNonNull(converted.getValue())).decode(DefaultEncodingContext.INSTANCE);
    }

    private static @NotNull JsonNode enumValue(final @NotNull String name, final int value) {
        return com.fasterxml.jackson.databind.node.JsonNodeFactory.instance
                .objectNode()
                .put("name", name)
                .put("value", value);
    }

    private static @NotNull JsonNode publish(final @NotNull Decoding decoding, final @NotNull DataValue value) {
        final DataValue withStatus = new DataValue(value.getValue(), StatusCode.GOOD, null);
        final var builder = (DataPointWithMetadata.DataPointBuilderImpl<Void>) DataPointWithMetadata.<Void>builder(
                new OpcuaTag("test-tag", "", new OpcuaTagDefinition("ns=2;i=1001")), b -> null);
        OpcUaToJsonConverter.convertPayload(decoding, withStatus, builder);
        return builder.build("test-adapter").getTagValue();
    }
}
