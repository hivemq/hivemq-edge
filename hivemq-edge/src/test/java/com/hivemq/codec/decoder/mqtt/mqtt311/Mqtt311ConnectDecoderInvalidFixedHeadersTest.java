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
package com.hivemq.codec.decoder.mqtt.mqtt311;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.codec.decoder.mqtt.mqtt3.Mqtt311ConnectDecoder;
import com.hivemq.configuration.HivemqId;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.util.ClientIds;
import com.hivemq.util.ReasonStrings;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import util.TestChannelAttribute;
import util.TestConfigurationBootstrap;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class Mqtt311ConnectDecoderInvalidFixedHeadersTest {

    private MqttConnacker connacker;
    private ClientConnection clientConnection;
    private Mqtt311ConnectDecoder decoder;

    @BeforeEach
    public void setUp() throws Exception {
        final Channel channel = mock(Channel.class);
        connacker = mock(MqttConnacker.class);
        clientConnection = new ClientConnection(channel, null);
        when(channel.attr(any(AttributeKey.class))).thenReturn(new TestChannelAttribute(clientConnection));

        decoder = new Mqtt311ConnectDecoder(connacker,
                new ClientIds(new HivemqId()),
                new TestConfigurationBootstrap().getConfigurationService(),
                new HivemqId());
    }

    @ParameterizedTest
    @ValueSource(bytes = {
            (byte) 0b0001_0001, (byte) 0b0001_0011, (byte) 0b0001_0111, (byte) 0b0001_1111,
            (byte) 0b0001_0010, (byte) 0b0001_0110, (byte) 0b0001_1110,
            (byte) 0b0001_0100, (byte) 0b0001_1100,
            (byte) 0b0001_1000
    })
    public void test_fixed_header_reserved_bit_set(byte invalidBitHeader) {
        assertNull(decoder.decode(clientConnection, null, invalidBitHeader));
        verify(connacker).connackError(clientConnection.getChannel(),
                "A client (IP: {}) connected with an invalid fixed header.",
                "Invalid CONNECT fixed header",
                Mqtt5ConnAckReasonCode.MALFORMED_PACKET,
                ReasonStrings.CONNACK_MALFORMED_PACKET_FIXED_HEADER);
    }
}
