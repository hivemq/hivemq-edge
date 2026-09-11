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
package com.hivemq.codec.encoder.mqtt5;

import com.google.common.base.Charsets;
import com.hivemq.mqtt.message.QoS;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Util for decoders of MQTT 5 messages.
 *
 * @author Silvio Giebl
 */
final class Mqtt5MessageEncoderUtil {

    private Mqtt5MessageEncoderUtil() {}

    static int propertyEncodedLength(final @NotNull String value) {
        return 1 + MqttBinaryData.encodedLength(value);
    }

    static int nullablePropertyEncodedLength(final @Nullable String value) {
        return (value == null) ? 0 : propertyEncodedLength(value);
    }

    static int nullablePropertyEncodedLength(final byte @Nullable [] binary) {
        return (binary == null) ? 0 : 1 + MqttBinaryData.encodedLength(binary);
    }

    static int nullablePropertyEncodedLength(final @Nullable Mqtt5PayloadFormatIndicator payloadFormatIndicator) {
        return (payloadFormatIndicator == null) ? 0 : 2;
    }

    static int nullablePropertyEncodedLength(final @Nullable QoS qoS) {
        return (qoS != null && qoS != QoS.EXACTLY_ONCE) ? 2 : 0;
    }

    static int booleanPropertyEncodedLength(final boolean value, final boolean defaultValue) {
        return (value == defaultValue) ? 0 : 2;
    }

    static int shortPropertyEncodedLength(final int value, final int defaultValue) {
        return (value == defaultValue) ? 0 : 3;
    }

    /**
     * EDG-811: the single decision on whether a four-byte integer property is emitted, shared by
     * {@link #intPropertyEncodedLength} and {@link #encodeIntProperty}. One declares how many property bytes
     * the packet claims, the other writes them; if they ever disagree the packet claims more property bytes
     * than it carries, the receiver reads payload as if it were a property, and the connection dies with a
     * protocol error. That is not hypothetical — it happened on this branch when only the encoder learned to
     * skip out-of-range values. Hence one predicate rather than two conditions that have to be kept in sync.
     * <p>
     * {@code defaultValue} carries two different meanings across the call sites. For MAXIMUM_PACKET_SIZE it is
     * a genuine default — a legal value the receiver reconstructs from the spec, so omitting it merely saves
     * bytes. For MESSAGE_EXPIRY_INTERVAL and SESSION_EXPIRY_INTERVAL it is an out-of-range marker meaning
     * "absent", where omission is the only correct encoding.
     * <p>
     * The range check is needed on top of the equality: the markers are not the only values outside the
     * four-byte range, and anything else out there used to reach {@code writeInt} and be silently truncated to
     * its low 32 bits — 2^40 encoding as 0, "expire immediately". A value that does not fit in an MQTT
     * four-byte integer has no correct encoding, so the property is dropped rather than corrupted.
     */
    private static boolean shouldEncodeIntProperty(final long value, final long defaultValue) {
        return value != defaultValue && UnsignedDataTypes.isUnsignedInt(value);
    }

    static int intPropertyEncodedLength(final long value, final long defaultValue) {
        return shouldEncodeIntProperty(value, defaultValue) ? 5 : 0;
    }

    static int variableByteIntegerPropertyEncodedLength(final int value) {
        return 1 + MqttVariableByteInteger.encodedLength(value);
    }

    static void encodeProperty(final int propertyIdentifier, final @NotNull String value, final @NotNull ByteBuf out) {

        out.writeByte(propertyIdentifier);
        MqttBinaryData.encode(value.getBytes(Charsets.UTF_8), out);
    }

    static void encodeNullableProperty(
            final int propertyIdentifier, final @Nullable String value, final @NotNull ByteBuf out) {

        if (value != null) {
            encodeProperty(propertyIdentifier, value, out);
        }
    }

    static void encodeNullableProperty(
            final int propertyIdentifier, final byte @Nullable [] binary, final @NotNull ByteBuf out) {

        if (binary != null) {
            out.writeByte(propertyIdentifier);
            MqttBinaryData.encode(binary, out);
        }
    }

    static void encodeNullableProperty(
            final int propertyIdentifier, final @Nullable QoS qoS, final @NotNull ByteBuf out) {

        if ((qoS != null) && (qoS.getQosNumber() < 2)) {
            out.writeByte(propertyIdentifier);
            out.writeByte(qoS.getQosNumber());
        }
    }

    static void encodeNullableProperty(
            final int propertyIdentifier,
            final @Nullable Mqtt5PayloadFormatIndicator payloadFormatIndicator,
            final @NotNull ByteBuf out) {

        if (payloadFormatIndicator != null) {
            out.writeByte(propertyIdentifier);
            out.writeByte(payloadFormatIndicator.getCode());
        }
    }

    static void encodeBooleanProperty(
            final int propertyIdentifier, final boolean value, final boolean defaultValue, final @NotNull ByteBuf out) {

        if (value != defaultValue) {
            out.writeByte(propertyIdentifier);
            out.writeByte(value ? 1 : 0);
        }
    }

    static void encodeShortProperty(
            final int propertyIdentifier, final int value, final int defaultValue, final @NotNull ByteBuf out) {

        if (value != defaultValue) {
            out.writeByte(propertyIdentifier);
            out.writeShort(value);
        }
    }

    static void encodeIntProperty(
            final int propertyIdentifier, final long value, final long defaultValue, final @NotNull ByteBuf out) {

        if (shouldEncodeIntProperty(value, defaultValue)) {
            out.writeByte(propertyIdentifier);
            out.writeInt((int) value);
        }
    }

    static void encodeVariableByteIntegerProperty(
            final int propertyIdentifier, final int value, final @NotNull ByteBuf out) {

        out.writeByte(propertyIdentifier);
        MqttVariableByteInteger.encode(value, out);
    }
}
