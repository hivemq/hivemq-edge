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
package com.hivemq.codec.decoder.mqtt.mqtt3;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.codec.decoder.mqtt.AbstractMqttDecoder;
import com.hivemq.configuration.service.ConfigurationService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import io.netty.buffer.ByteBuf;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author Dominik Obermaier
 */
@Singleton
public class Mqtt3PubcompDecoder extends AbstractMqttDecoder<PUBCOMP> {

    @Inject
    public Mqtt3PubcompDecoder(
            final @NotNull MqttServerDisconnector disconnector,
            final @NotNull ConfigurationService configurationService) {
        super(disconnector, configurationService);
    }

    @Override
    public @Nullable PUBCOMP decode(
            final @NotNull ClientConnection clientConnection, final @NotNull ByteBuf buf, final byte header) {

        if (clientConnection.getProtocolVersion() == ProtocolVersion.MQTTv3_1_1) {
            if (!validateHeader(header)) {
                disconnectByInvalidFixedHeader(clientConnection, MessageType.PUBCOMP);
                buf.clear();
                return null;
            }
        }

        if (buf.readableBytes() < 2) {
            disconnectByNoMessageId(clientConnection, MessageType.PUBCOMP);
            buf.clear();
            return null;
        }
        final int messageId = buf.readUnsignedShort();

        return new PUBCOMP(messageId);
    }
}
