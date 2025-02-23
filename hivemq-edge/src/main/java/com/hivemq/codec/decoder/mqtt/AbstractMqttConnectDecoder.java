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
package com.hivemq.codec.decoder.mqtt;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.codec.decoder.MqttDecoder;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.configuration.service.InternalConfigurations;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.connect.MqttWillPublish;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.util.Bytes;
import com.hivemq.util.ClientIds;
import com.hivemq.util.ReasonStrings;
import com.hivemq.util.Strings;
import io.netty.buffer.ByteBuf;

import static com.hivemq.util.Bytes.isBitSet;

/**
 * An Abstract Class for all Mqtt CONNECT Decoders
 *
 * @author Florian Limpöck
 */
public abstract class AbstractMqttConnectDecoder extends MqttDecoder<CONNECT> {

    protected static final int DISCONNECTED = -1;
    private static final byte VARIABLE_HEADER_LENGTH = 10;

    protected final @NotNull MqttConnacker mqttConnacker;
    protected final @NotNull ClientIds clientIds;
    protected final long maxSessionExpiryInterval;
    protected final long maxUserPropertiesLength;
    protected final boolean allowAssignedClientId;
    protected final boolean validateUTF8;

    protected AbstractMqttConnectDecoder(
            final @NotNull MqttConnacker mqttConnacker,
            final @NotNull ConfigurationService configurationService,
            final @NotNull ClientIds clientIds) {
        this.mqttConnacker = mqttConnacker;
        this.clientIds = clientIds;
        validateUTF8 = configurationService.securityConfiguration().validateUTF8();
        maxUserPropertiesLength = InternalConfigurations.USER_PROPERTIES_MAX_SIZE_BYTES;
        maxSessionExpiryInterval = configurationService.mqttConfiguration().maxSessionExpiryInterval();
        allowAssignedClientId = configurationService.securityConfiguration().allowServerAssignedClientId();
    }

    protected void disconnectByInvalidFixedHeader(final @NotNull ClientConnection clientConnection) {
        mqttConnacker.connackError(clientConnection.getChannel(),
                "A client (IP: {}) connected with an invalid fixed header.",
                "Invalid CONNECT fixed header",
                Mqtt5ConnAckReasonCode.MALFORMED_PACKET,
                ReasonStrings.CONNACK_MALFORMED_PACKET_FIXED_HEADER);
    }

    protected void disconnectByInvalidHeader(final @NotNull ClientConnection clientConnection) {
        mqttConnacker.connackError(clientConnection.getChannel(),
                "A client (ID: {},IP: {}) connected with an invalid CONNECT header.",
                "Invalid CONNECT header",
                Mqtt5ConnAckReasonCode.PROTOCOL_ERROR,
                ReasonStrings.CONNACK_PROTOCOL_ERROR_VARIABLE_HEADER);
    }

    /**
     * Validates Will Flags by MQTT Specification.
     * <p>
     * If the Will Flag is set to 0, then the Will QoS MUST be set to 0. If the Will Flag is set to 0, then Will Retain
     * MUST be set to 0
     * <p>
     * If the Will Flag is set to 1, the value of Will QoS can be 0, 1, or 2.
     * <p>
     * A Will QoS of 3 is a malformed packet.
     * <p>
     * A Will QoS of 0 and a Will Retain of 1 is a malformed packet.
     *
     * @param isWillFlag       will flag set
     * @param isWillRetain     will retain set
     * @param willQoS          quality of service of will
     * @param clientConnection the connection of the mqtt client
     * @return true if valid, else false
     */
    protected boolean validateWill(
            final boolean isWillFlag,
            final boolean isWillRetain,
            final int willQoS,
            final @NotNull ClientConnection clientConnection) {

        final boolean valid = (isWillFlag && willQoS < 3) || (!isWillRetain && willQoS == 0);
        if (!valid) {
            mqttConnacker.connackError(clientConnection.getChannel(),
                    "A client (IP: {}) connected with an invalid willTopic flag combination. Disconnecting client.",
                    "Invalid will-topic/flag combination",
                    Mqtt5ConnAckReasonCode.MALFORMED_PACKET,
                    ReasonStrings.CONNACK_MALFORMED_WILL_FLAG);
        }
        return valid;
    }

    /**
     * Validates the connect flags byte
     * <p>
     * If the first bit of the connect flags byte is set, it is a malformed packet
     *
     * @param connectFlagsByte the connect flags byte
     * @param clientConnection the connection of the mqtt client
     * @return false if the reserved bit zero is set to 1, else true
     */
    protected boolean validateConnectFlagByte(
            final byte connectFlagsByte, final @NotNull ClientConnection clientConnection) {

        if (isBitSet(connectFlagsByte, 0)) {
            mqttConnacker.connackError(clientConnection.getChannel(),
                    "A client (IP: {}) connected with invalid CONNECT flags. Disconnecting client.",
                    "Invalid CONNECT flags",
                    Mqtt5ConnAckReasonCode.MALFORMED_PACKET,
                    ReasonStrings.CONNACK_MALFORMED_CONNECT_FLAGS);
            return false;
        }
        return true;
    }

    /**
     * Validates the protocol name of the MQTT Version
     * <p>
     * If the validation fails it is an unsupported protocol version error.
     *
     * @param variableHeader   the variable header of a mqtt connect message
     * @param clientConnection the connection of the mqtt client
     * @return true if valid, else false
     */
    protected boolean validateProtocolName(
            final @NotNull ByteBuf variableHeader,
            final @NotNull ClientConnection clientConnection,
            final @NotNull String protocolName) {

        if (!protocolName.equals(Strings.getPrefixedString(variableHeader))) {
            mqttConnacker.connackError(clientConnection.getChannel(),
                    "A client (IP: {}) connected with an invalid protocol name. Disconnecting client.",
                    "Invalid CONNECT protocol name",
                    Mqtt5ConnAckReasonCode.UNSUPPORTED_PROTOCOL_VERSION,
                    ReasonStrings.CONNACK_UNSUPPORTED_PROTOCOL_VERSION);
            variableHeader.clear();
            return false;
        }
        return true;
    }

    /**
     * THIS IS FOR MQTT 3 ONLY !
     * <p>
     * checks if the username flag is set when the password flag is set
     */
    protected boolean validateUsernamePassword(final boolean isUsernameFlag, final boolean isPasswordFlag) {
        //Validates that the username flag is set if the password flag is set
        return !isPasswordFlag || isUsernameFlag;
    }

    /**
     * decodes the fixed part of the connect variable header.
     * <p>
     * the buf reader index will be increased by the length
     * <p>
     * if readable bytes less than length a connack with protocol error reason code will be sent.
     *
     * @param clientConnection the connection of the mqtt client
     * @param buf              the encoded ByteBuf of the message
     * @return a new ByteBuf of the fixed variable header part or {@code null} in an error case
     */
    protected @Nullable ByteBuf decodeFixedVariableHeaderConnect(
            final @NotNull ClientConnection clientConnection, final @NotNull ByteBuf buf) {

        if (buf.readableBytes() >= VARIABLE_HEADER_LENGTH) {
            return buf.readSlice(VARIABLE_HEADER_LENGTH);
        } else {
            disconnectByInvalidHeader(clientConnection);
            return null;
        }
    }

    /**
     * Decodes and validates a Mqtt3 LWT (Will)
     * <p>
     * Client will be disconnected by:
     * <p>
     * - Bad will topic length
     * <p>
     * - topic with bad UTF-8 Character
     * <p>
     * - topic is empty
     * <p>
     * - topic contains null character
     *
     * @param clientConnection the connection of the mqtt client
     * @param buf              the ByteBuf of the encoded will message
     * @param willQoS          the quality of service of the will message
     * @param isWillRetain     the retain flag of the will message
     * @param hiveMQId         the HiveMQ identifier
     * @return a {@link MqttWillPublish} if valid, else {@code null}.
     */
    @Nullable
    protected MqttWillPublish readMqtt3WillPublish(
            final @NotNull ClientConnection clientConnection,
            final @NotNull ByteBuf buf,
            final int willQoS,
            final boolean isWillRetain,
            final @NotNull HivemqId hiveMQId) {

        final MqttWillPublish.Mqtt3Builder willBuilder = new MqttWillPublish.Mqtt3Builder();
        willBuilder.withQos(QoS.valueOf(willQoS));
        willBuilder.withRetain(isWillRetain);

        final String willTopic;
        final int utf8StringLengthWill;

        if (buf.readableBytes() < 2 || buf.readableBytes() < (utf8StringLengthWill = buf.readUnsignedShort())) {
            mqttConnacker.connackError(clientConnection.getChannel(),
                    "A client (IP: {}) sent a CONNECT with an incorrect will-topic length. Disconnecting client.",
                    "Incorrect CONNECT will-topic length",
                    Mqtt5ConnAckReasonCode.MALFORMED_PACKET,
                    ReasonStrings.CONNACK_MALFORMED_PACKET_INCORRECT_WILL_TOPIC_LENGTH);
            return null;
        }

        if (validateUTF8) {
            willTopic = Strings.getValidatedPrefixedString(buf, utf8StringLengthWill, true);
            if (willTopic == null) {
                mqttConnacker.connackError(clientConnection.getChannel(),
                        "The will-topic of the client (IP: {}) is not well formed. This is not allowed. Disconnecting client.",
                        "Sent CONNECT with bad UTF-8 character",
                        Mqtt5ConnAckReasonCode.MALFORMED_PACKET,
                        ReasonStrings.CONNACK_MALFORMED_PACKET_BAD_UTF8);
                return null;
            }
        } else {
            willTopic = Strings.getPrefixedString(buf, utf8StringLengthWill);
        }

        if (isInvalidTopic(clientConnection, willTopic)) {
            mqttConnacker.connackError(clientConnection.getChannel(),
                    null, //already logged
                    "Sent CONNECT with invalid will-topic",
                    Mqtt5ConnAckReasonCode.MALFORMED_PACKET,
                    ReasonStrings.CONNACK_MALFORMED_PACKET_INVALID_WILL_TOPIC);
            return null;
        }

        final byte[] prefixedBytes = Bytes.getPrefixedBytes(buf);
        final byte[] willMessage = prefixedBytes != null ? prefixedBytes : new byte[0];

        return willBuilder.withPayload(willMessage).withTopic(willTopic).withHivemqId(hiveMQId.get()).build();
    }
}
