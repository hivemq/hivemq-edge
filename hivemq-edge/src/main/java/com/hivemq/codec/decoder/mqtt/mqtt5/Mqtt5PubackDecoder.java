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

import com.google.common.collect.ImmutableList;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.codec.decoder.mqtt.AbstractMqttDecoder;
import com.hivemq.configuration.service.ConfigurationService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.puback.Mqtt5PUBACK;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.reason.Mqtt5PubAckReasonCode;
import io.netty.buffer.ByteBuf;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import static com.hivemq.mqtt.message.mqtt5.MessageProperties.REASON_STRING;
import static com.hivemq.mqtt.message.mqtt5.MessageProperties.USER_PROPERTY;

/**
 * @author Waldemar Ruck
 * @since 4.0
 */
@Singleton
public class Mqtt5PubackDecoder extends AbstractMqttDecoder<PUBACK> {

    @Inject
    public Mqtt5PubackDecoder(
            final @NotNull MqttServerDisconnector disconnector,
            final @NotNull ConfigurationService configurationService) {
        super(disconnector, configurationService);
    }

    @Override
    public @Nullable PUBACK decode(
            final @NotNull ClientConnection clientConnection, final @NotNull ByteBuf buf, final byte header) {

        if (!validateHeader(header)) {
            disconnectByInvalidFixedHeader(clientConnection, MessageType.PUBACK);
            return null;
        }

        if (buf.readableBytes() < 2) {
            disconnectByRemainingLengthToShort(clientConnection, MessageType.PUBACK);
            return null;
        }

        final int packetIdentifier = decodePacketIdentifier(clientConnection, buf, MessageType.PUBACK);
        if (packetIdentifier == 0) {
            return null;
        }

        //nothing more to read
        if (!buf.isReadable()) {
            return new PUBACK(packetIdentifier, Mqtt5PUBACK.DEFAULT_REASON_CODE, null, Mqtt5UserProperties.NO_USER_PROPERTIES);
        }

        final Mqtt5PubAckReasonCode reasonCode = Mqtt5PubAckReasonCode.fromCode(buf.readUnsignedByte());
        if (reasonCode == null) {
            disconnectByInvalidReasonCode(clientConnection, MessageType.PUBACK);
            return null;
        }

        if (!buf.isReadable()) {
            return new PUBACK(packetIdentifier, reasonCode, null, Mqtt5UserProperties.NO_USER_PROPERTIES);
        }

        final int propertiesLength = decodePropertiesLengthNoPayload(clientConnection, buf, MessageType.PUBACK);
        if (propertiesLength == DISCONNECTED) {
            return null;
        }

        String reasonString = null;
        ImmutableList.Builder<MqttUserProperty> userPropertiesBuilder = null;

        while (buf.isReadable()) {
            final int propertyIdentifier = buf.readByte();

            switch (propertyIdentifier) {
                case REASON_STRING:
                    reasonString = decodeReasonString(clientConnection, buf, reasonString, MessageType.PUBACK);
                    if (reasonString == null) {
                        return null;
                    }
                    break;

                case USER_PROPERTY:
                    userPropertiesBuilder = readUserProperty(clientConnection, buf, userPropertiesBuilder, MessageType.PUBACK);
                    if (userPropertiesBuilder == null) {
                        return null;
                    }
                    break;

                default:
                    disconnectByInvalidPropertyIdentifier(clientConnection, propertyIdentifier, MessageType.PUBACK);
                    return null;
            }
        }

        final Mqtt5UserProperties userProperties = Mqtt5UserProperties.build(userPropertiesBuilder);
        if (invalidUserPropertiesLength(clientConnection, MessageType.PUBACK, userProperties)) {
            return null;
        }

        return new PUBACK(packetIdentifier, reasonCode, reasonString, userProperties);
    }
}
