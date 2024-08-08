/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.modbus.writing;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public enum ConvertibleDataType {

    BYTE(ConvertibleDataType::convertByte),
    INTEGER(ConvertibleDataType::convertInteger),
    SHORT(ConvertibleDataType::covertShort),
    LONG(ConvertibleDataType::convertLong),
    UTF8_STRING(ConvertibleDataType::convertUtf8String),
    ASCII_STRING(ConvertibleDataType::convertAsciiString);

    private final @NotNull ConversionFunction conversionFunction;

    ConvertibleDataType(final @NotNull ConversionFunction conversionFunction) {
        this.conversionFunction = conversionFunction;
    }

    public static byte[] convertByte(final @NotNull Object value) {
        if (value instanceof String) {
            final byte[] decoded = Base64.getDecoder().decode(((String) value).getBytes());
            if (decoded.length > 1) {
                throw new IllegalArgumentException(
                        "The data type ''Byte' was specified, but more than the base64 encoded String contained more than one byte.");
            }
            return decoded;
        } else if (value instanceof Integer) {
            return new byte[]{
                    0, (byte) value};
        }
        throw new IllegalArgumentException(value.getClass().getSimpleName() + " can not be converted to Byte.");
    }

    public static byte[] convertInteger(final @NotNull Object value) {
        byte[] serialized = new byte[4];
        if (value instanceof Integer) {
            int cast = (Integer) value;
            // Big Endian
            serialized[0] = (byte) (cast >> 24);
            serialized[1] = (byte) (cast >> 16);
            serialized[2] = (byte) (cast >> 8);
            serialized[3] = (byte) cast;
            return serialized;
        }
        throw new IllegalArgumentException(value.getClass().getSimpleName() + " can not be converted to Byte.");
    }

    public static byte[] covertShort(final @NotNull Object value) {
        byte[] serialized = new byte[2];
        //TODO do we need more checks? Json should only give us Integers.
        if (value instanceof Integer) {
            int cast = (Integer) value;
            // Big Endian
            serialized[0] = (byte) (cast >> 8);
            serialized[1] = (byte) cast;
            return serialized;
        }
        throw new IllegalArgumentException(value.getClass().getSimpleName() + " can not be converted to Byte.");
    }

    public static byte[] convertLong(final @NotNull Object value) {
        byte[] serialized = new byte[8];
        if (value instanceof Integer) {
            int cast = (Integer) value;
            // Big Endian
            serialized[5] = (byte) (cast >> 24);
            serialized[6] = (byte) (cast >> 16);
            serialized[7] = (byte) (cast >> 8);
            serialized[8] = (byte) cast;
            return serialized;
        } else if (value instanceof Long) {
            final long cast = (Long) value;
            // Big Endian
            serialized[0] = (byte) (cast >> 56);
            serialized[1] = (byte) (cast >> 48);
            serialized[2] = (byte) (cast >> 40);
            serialized[3] = (byte) (cast >> 32);

            serialized[4] = (byte) (cast >> 24);
            serialized[5] = (byte) (cast >> 16);
            serialized[6] = (byte) (cast >> 8);
            serialized[7] = (byte) cast;
            return serialized;
        }
        throw new IllegalArgumentException(value.getClass().getSimpleName() + " can not be converted to Byte.");
    }

    public static byte[] convertUtf8String(final @NotNull Object value) {
        if (value instanceof String) {
            final String cast = (String) value;
            return cast.getBytes(StandardCharsets.UTF_8);
        }
        throw new IllegalArgumentException(value.getClass().getSimpleName() +
                " can not be converted to an utf-8 String.");
    }

    public static byte[] convertAsciiString(final @NotNull Object value) {
        if (value instanceof String) {
            final String cast = (String) value;
            return cast.getBytes(StandardCharsets.US_ASCII);
        }
        throw new IllegalArgumentException(value.getClass().getSimpleName() +
                " can not be converted to an utf-8 String.");
    }

    public byte @NotNull [] convert(final @NotNull Object value) {
        return conversionFunction.convert(value);
    }
}
