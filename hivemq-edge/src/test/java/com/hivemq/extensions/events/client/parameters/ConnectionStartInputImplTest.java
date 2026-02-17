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
package com.hivemq.extensions.events.client.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.mqtt.message.ProtocolVersion;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;
import util.TestMessageUtil;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class ConnectionStartInputImplTest {

    @Test
    public void test_construction_client_null() {

        assertThrows(NullPointerException.class, () -> new ConnectionStartInputImpl(null, new EmbeddedChannel()));
    }

    @Test
    public void test_construction_values() {
        final EmbeddedChannel channel = new EmbeddedChannel();
        ClientConnection clientConnection = new ClientConnection(channel, null);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
        final ConnectionStartInputImpl input =
                new ConnectionStartInputImpl(TestMessageUtil.createFullMqtt5Connect(), channel);
        assertEquals(input, input.get());
        assertNotNull(input.getClientInformation());
        assertNotNull(input.getConnectionInformation());
        assertNotNull(input.getConnectPacket());
    }
}
