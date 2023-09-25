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
package com.hivemq.edge.adapters.opcua.payload;

import com.hivemq.edge.adapters.opcua.OpcUaAdapterConfig.PayloadMode;
import com.hivemq.edge.adapters.opcua.OpcUaProtocolAdapter;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapter;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.publish.PUBLISH;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("NullabilityAnnotations")
class OpcUaJsonPayloadConverterTest extends AbstractOpcUaPayloadConverterTest {

    private static Stream<Arguments> provideBaseTypes() {
        return Stream.of(Arguments.of("Boolean", Identifiers.Boolean, true, "true"),
                Arguments.of("Byte", Identifiers.Byte, 0, "0"),
                Arguments.of("Byte", Identifiers.Byte, 255, "255"),
                Arguments.of("ByteString",
                        Identifiers.ByteString,
                        new ByteString(new byte[]{1, 2, 3, 4, 5}),
                        "\"AQIDBAU=\""),
                Arguments.of("DateTime",
                        Identifiers.DateTime,
                        new DateTime(Instant.ofEpochMilli(1683724156000L)),
                        "\"2023-05-10T13:09:16Z\""),
                Arguments.of("Double", Identifiers.Double, 123.456, "123.456"),
                Arguments.of("Duration", Identifiers.Duration, 1234.5678d, "1234.5678"),
                Arguments.of("Float", Identifiers.Float, 1234.567f, "1234.567"),
                Arguments.of("Guid",
                        Identifiers.Guid,
                        UUID.fromString("b12776f9-bf9f-460a-9984-89c5ac1ea724"),
                        "\"b12776f9-bf9f-460a-9984-89c5ac1ea724\""),
                Arguments.of("SByte", Identifiers.SByte, -128, "-128"),
                Arguments.of("SByte", Identifiers.SByte, 127, "127"),
                Arguments.of("Int16", Identifiers.Int16, -32768, "-32768"),
                Arguments.of("Int16", Identifiers.Int16, 32767, "32767"),
                Arguments.of("Int32", Identifiers.Int32, -2147483648, "-2147483648"),
                Arguments.of("Int32", Identifiers.Int32, 2147483647, "2147483647"),
                Arguments.of("Int64", Identifiers.Int64, -9223372036854775808L, "-9223372036854775808"),
                Arguments.of("Int64", Identifiers.Int64, 9223372036854775807L, "9223372036854775807"),
                Arguments.of("String", Identifiers.String, "content", "\"content\""),
                Arguments.of("StringControl",
                        Identifiers.String,
                        " \" \\ \b \f \n \r \t ",
                        "\" \\\" \\\\ \\b \\f \\n \\r \\t \""),
                //JSON control characters
                Arguments.of("UInt16", Identifiers.UInt16, 65535, "65535"),
                Arguments.of("UInt32", Identifiers.UInt32, 4294967295L, "4294967295"),
                Arguments.of("XmlElement", Identifiers.XmlElement, "<a><b>c</b></a>", "\"<a><b>c</b></a>\""));
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("provideBaseTypes")
    @Timeout(10)
    public void whenTypeSubscriptionPresent_thenReceiveMsg(
            final @NotNull String name,
            final @NotNull NodeId typeId,
            final @NotNull Object value,
            final @NotNull String jsonValue) throws Exception {

        checkJsonAdapter(name, typeId, value, jsonValue);
    }

    private void checkJsonAdapter(
            @NotNull String name, @NotNull NodeId typeId, @NotNull Object value, @NotNull String jsonValue)
            throws InterruptedException, ExecutionException {
        final String nodeId =
                opcUaServerExtension.getTestNamespace().addNode("Test" + name + "Node", typeId, () -> value, 999);

        final OpcUaProtocolAdapter protocolAdapter = createAndStartAdapter(nodeId, PayloadMode.JSON);
        assertEquals(ProtocolAdapter.ConnectionStatus.CONNECTED, protocolAdapter.getConnectionStatus());

        final PUBLISH publish = expectAdapterPublish();
        protocolAdapter.stop();
        assertThat(new String(publish.getPayload())).contains("\"value\":" + jsonValue);
    }

}
