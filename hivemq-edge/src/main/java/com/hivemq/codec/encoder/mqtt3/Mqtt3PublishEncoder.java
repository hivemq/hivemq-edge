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
package com.hivemq.codec.encoder.mqtt3;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.mqtt.message.publish.Mqtt3PUBLISH;
import com.hivemq.util.Strings;
import com.hivemq.util.Utf8Utils;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * Encoder for MQTT 3 PUBLISH messages.
 *
 * @author Dominik Obermaier
 */
public class Mqtt3PublishEncoder extends AbstractVariableHeaderLengthEncoder<Mqtt3PUBLISH> {

    private static final byte PUBLISH_FIXED_HEADER = 0b0011_0000;

    @Override
    public void encode(
            final @NotNull ClientConnection clientConnection,
            final @NotNull Mqtt3PUBLISH msg,
            final @NotNull ByteBuf out) {

        byte header = PUBLISH_FIXED_HEADER;
        final int qos = msg.getQoS().getQosNumber();
        if (msg.isDuplicateDelivery()) {
            header = (byte) (header | 0b0000_1000);
        }
        if (msg.isRetain()) {
            header = (byte) (header | 0b0000_0001);
        }
        header = (byte) (header | qos << 1);

        out.writeByte(header);
        createRemainingLength(msg.getRemainingLength(), out);

        Strings.createPrefixedBytesFromString(msg.getTopic(), out);

        if (qos > 0) {
            out.writeShort(msg.getPacketIdentifier());
        }
        out.writeBytes(msg.getPayload());
    }

    @Override
    protected int remainingLength(final @NotNull Mqtt3PUBLISH msg) {
        int length = 0;
        length += Utf8Utils.encodedLength(msg.getTopic());
        length += msg.getPayload().length;
        length += 2; // Topic length
        if (msg.getQoS().getQosNumber() > 0) {
            length += 2; // message ID
        }
        return length;
    }
}
