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
import com.hivemq.codec.encoder.MqttEncoder;
import org.jetbrains.annotations.NotNull;
import com.hivemq.mqtt.message.puback.PUBACK;
import io.netty.buffer.ByteBuf;

/**
 * @author Dominik Obermaier
 */
public class Mqtt3PubackEncoder implements MqttEncoder<PUBACK> {

    public static final int ENCODED_PUBACK_SIZE = 4;
    private static final byte PUBACK_FIXED_HEADER = 0b0100_0000;
    private static final byte PUBACK_REMAINING_LENGTH = 0b0000_0010;

    @Override
    public void encode(
            final @NotNull ClientConnection clientConnection,
            final @NotNull PUBACK msg,
            final @NotNull ByteBuf out) {

        if (msg.getPacketIdentifier() == 0) {
            throw new IllegalArgumentException("Message ID must not be null");
        }

        out.writeByte(PUBACK_FIXED_HEADER);
        //The remaining length is always static for PUBACKs
        out.writeByte(PUBACK_REMAINING_LENGTH);

        out.writeShort(msg.getPacketIdentifier());
    }

    @Override
    public int bufferSize(final @NotNull ClientConnection clientConnection, final @NotNull PUBACK msg) {
        return ENCODED_PUBACK_SIZE;
    }
}
