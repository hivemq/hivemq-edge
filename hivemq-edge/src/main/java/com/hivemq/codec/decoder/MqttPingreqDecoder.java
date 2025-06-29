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
package com.hivemq.codec.decoder;

import com.hivemq.bootstrap.ClientConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.PINGREQ;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.util.ReasonStrings;
import io.netty.buffer.ByteBuf;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author Florian Limpöck
 */
@Singleton
public class MqttPingreqDecoder extends MqttDecoder<PINGREQ> {

    private final @NotNull MqttServerDisconnector serverDisconnector;

    @Inject
    public MqttPingreqDecoder(final @NotNull MqttServerDisconnector serverDisconnector) {
        this.serverDisconnector = serverDisconnector;
    }

    @Override
    public @Nullable PINGREQ decode(
            final @NotNull ClientConnection clientConnection, final @NotNull ByteBuf buf, final byte header) {

        final ProtocolVersion protocolVersion = clientConnection.getProtocolVersion();

        //Pingreq of MQTTv5 is equal to MQTTv3_1_1
        if (protocolVersion == ProtocolVersion.MQTTv5 || protocolVersion == ProtocolVersion.MQTTv3_1_1) {
            if (!validateHeader(header)) {
                serverDisconnector.disconnect(clientConnection.getChannel(),
                        "A client (IP: {}) sent a PINGREQ with an invalid fixed header. Disconnecting client.",
                        "Sent a PINGREQ with invalid fixed header",
                        Mqtt5DisconnectReasonCode.MALFORMED_PACKET,
                        String.format(ReasonStrings.DISCONNECT_MALFORMED_FIXED_HEADER, "PINGREQ"));
                buf.clear();
                return null;
            }
        }
        return PINGREQ.INSTANCE;
    }
}
