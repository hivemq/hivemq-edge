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
package com.hivemq.codec.decoder.mqtt.mqtt5;

import org.jetbrains.annotations.NotNull;
import com.hivemq.mqtt.message.Message;
import com.hivemq.mqtt.message.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import org.junit.Before;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

abstract class AbstractMqtt5DecoderTest extends AbstractMqttDecoderTest {

    @Before
    public void customSetUp() {
        protocolVersion = ProtocolVersion.MQTTv5;
        super.setUp();
    }

    void decodeNullExpected(final byte @NotNull [] encoded) {
        final ByteBuf byteBuf = channel.alloc().buffer();
        byteBuf.writeBytes(encoded);
        channel.writeInbound(byteBuf);

        final Message message = channel.readInbound();
        assertNull(message);

        channel.runPendingTasks();
        assertFalse(channel.isOpen());
        assertFalse(channel.isActive());

        createChannel();
    }
}
