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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.edge.adapters.opcua.OpcUaProtocolAdapter;
import com.hivemq.protocols.ProtocolAdapterStopOutputImpl;
import org.assertj.core.groups.Tuple;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.LONG;

class OpcUaToJsonConverterTest extends AbstractOpcUaPayloadConverterTest {

    private static @NotNull Stream<Arguments> provideBaseTypes() {
        return Stream.of(Arguments.of("Boolean", NodeIds.Boolean, true, "true"),
                Arguments.of("Byte", NodeIds.Byte, 0, "0"),
                Arguments.of("Byte", NodeIds.Byte, 255, "255"),
                Arguments.of("ByteString",
                        NodeIds.ByteString,
                        new ByteString(new byte[]{1, 2, 3, 4, 5}),
                        "\"AQIDBAU=\""),
                Arguments.of("DateTime",
                        NodeIds.DateTime,
                        new DateTime(Instant.ofEpochMilli(1683724156000L)),
                        "\"2023-05-10T13:09:16Z\""),
                Arguments.of("Double", NodeIds.Double, 123.456, "123.456"),
                Arguments.of("Duration", NodeIds.Duration, 1234.5678d, "1234.5678"),
                Arguments.of("Float", NodeIds.Float, 1234.567f, "1234.567"),
                Arguments.of("Guid",
                        NodeIds.Guid,
                        UUID.fromString("b12776f9-bf9f-460a-9984-89c5ac1ea724"),
                        "\"b12776f9-bf9f-460a-9984-89c5ac1ea724\""),
                Arguments.of("SByte", NodeIds.SByte, -128, "-128"),
                Arguments.of("SByte", NodeIds.SByte, 127, "127"),
                Arguments.of("Int16", NodeIds.Int16, -32768, "-32768"),
                Arguments.of("Int16", NodeIds.Int16, 32767, "32767"),
                Arguments.of("Int32", NodeIds.Int32, -2147483648, "-2147483648"),
                Arguments.of("Int32", NodeIds.Int32, 2147483647, "2147483647"),
                Arguments.of("Int64", NodeIds.Int64, -9223372036854775808L, "-9223372036854775808"),
                Arguments.of("Int64", NodeIds.Int64, 9223372036854775807L, "9223372036854775807"),
                Arguments.of("String", NodeIds.String, "content", "\"content\""),
                Arguments.of("StringControl",
                        NodeIds.String,
                        " \" \\ \b \f \n \r \t ",
                        "\" \\\" \\\\ \\b \\f \\n \\r \\t \""),
                //JSON control characters
                Arguments.of("UInt16", NodeIds.UInt16, 65535, "65535"),
                Arguments.of("UInt32", NodeIds.UInt32, 4294967295L, "4294967295"),
                Arguments.of("XmlElement", NodeIds.XmlElement, "<a><b>c</b></a>", "\"<a><b>c</b></a>\""));
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("provideBaseTypes")
    @Timeout(10)
    public void whenTypeSubscriptionPresent_thenReceiveMsg(
            final @NotNull String name,
            final @NotNull NodeId typeId,
            final @NotNull Object value,
            final @NotNull String jsonValue) throws Exception {
        final String nodeId =
                opcUaServerExtension.getTestNamespace().addNode("Test" + name + "Node", typeId, () -> value, 999);

        final OpcUaProtocolAdapter protocolAdapter = createAndStartAdapter(nodeId, null);
        assertThat(ProtocolAdapterState.ConnectionStatus.CONNECTED).isEqualTo(protocolAdapter.getProtocolAdapterState()
                .getConnectionStatus());

        final var received = expectAdapterPublish();
        protocolAdapter.stop(new ProtocolAdapterStopInput() {
        }, new ProtocolAdapterStopOutputImpl());

        assertThat(received).extractingByKey(nodeId).satisfies(dataPoints -> {
            assertThat(dataPoints).hasSize(1)
                    .extracting(DataPoint::getTagName, DataPoint::getTagValue)
                    .containsExactly(Tuple.tuple(nodeId, "{\"value\":" + jsonValue + "}"));
        });
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("provideBaseTypes")
    @Timeout(10)
    public void whenTypeSubscriptionPresent_thenReceiveMsgAndCollectAllProperties(
            final @NotNull String name,
            final @NotNull NodeId typeId,
            final @NotNull Object value,
            final @NotNull String jsonValue) throws Exception {
        final String nodeId =
                opcUaServerExtension.getTestNamespace().addNode("Test" + name + "Node", typeId, () -> value, 999);

        final OpcUaProtocolAdapter protocolAdapter = createAndStartAdapter(nodeId, true);
        assertThat(ProtocolAdapterState.ConnectionStatus.CONNECTED).isEqualTo(protocolAdapter.getProtocolAdapterState()
                .getConnectionStatus());

        final var received = expectAdapterPublish();
        protocolAdapter.stop(new ProtocolAdapterStopInput() {
        }, new ProtocolAdapterStopOutputImpl());

        final var typeRef
                = new TypeReference<HashMap<String, Object>>() {};
        var mapper = new ObjectMapper();
        var dataPoints = received.get(nodeId);
        var tagNameToValue = dataPoints.stream().collect(Collectors.toMap(DataPoint::getTagName, v -> {
            try {
                return mapper.readValue((String)v.getTagValue(), typeRef);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }));

        assertThat(tagNameToValue)
                .hasSize(1)
                .containsKey(nodeId)
                .extractingByKey(nodeId)
                .satisfies(innerMap -> {
                    assertThat(innerMap.get("serverPicoseconds")).isEqualTo(0);
                    assertThat(innerMap.get("sourcePicoseconds")).isEqualTo(0);
                    assertThat(innerMap.get("sourceTime")).asInstanceOf(LONG).isGreaterThan(1000L);
                    assertThat(innerMap.get("serverTime")).asInstanceOf(LONG).isGreaterThan(1000L);

                    final String sanitized;
                    if(jsonValue.startsWith("\"")) {
                        final var temp = jsonValue.replaceFirst("\"", "");
                        sanitized = temp.substring(0, temp.length() - 1);
                    } else {
                        sanitized = jsonValue;
                    }

                    final Object toCheck;
                    if(value instanceof Float) {
                        //jackson deserializes Float as Double
                        toCheck = ((Double)innerMap.get("value")).floatValue();
                    } else {
                        toCheck = innerMap.get("value");
                    }
                    assertThat(toCheck).isIn(value, sanitized);
                });
    }
}
