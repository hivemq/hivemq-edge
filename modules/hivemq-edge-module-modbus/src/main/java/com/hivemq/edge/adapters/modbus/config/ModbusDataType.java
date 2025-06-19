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
package com.hivemq.edge.adapters.modbus.config;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

public enum ModbusDataType {
    BOOL("BOOL", 1),
    INT_16("INT_16", 1),
    UINT_16("UINT_16", 1),
    INT_32("INT_32", 2),
    UINT_32("UINT_32", 2),
    INT_64("INT_64", 4),
    FLOAT_32("FLOAT_32", 2),
    FLOAT_64("FLOAT_64", 4),
    UTF_8("UTF_8", 4);

    public final @NotNull String label;
    public final int nrOfRegistersToRead;

    ModbusDataType(final @NotNull String label, final int nrOfRegistersToRead) {
        this.label = label;
        this.nrOfRegistersToRead = nrOfRegistersToRead;
    }

    private static @NotNull Object getUnsigned64(@NotNull final ByteBuf buffi) {
        final byte b1 = buffi.readByte();
        final byte b2 = buffi.readByte();
        final byte b3 = buffi.readByte();
        final byte b4 = buffi.readByte();
        final byte b5 = buffi.readByte();
        final byte b6 = buffi.readByte();
        final byte b7 = buffi.readByte();
        final byte b8 = buffi.readByte();
        return Unpooled.wrappedBuffer(new byte[]{b7, b8, b5, b6, b3, b4, b1, b2}).readUnsignedInt();
    }

    @Override
    public @NotNull String toString() {
        return label;
    }

    public @NotNull Object convert(final byte @NotNull [] payload, final boolean flipRegisters) {
        final ByteBuf buff = Unpooled.wrappedBuffer(payload);
        return switch (this) {
            case BOOL -> buff.readBoolean();

            case INT_16 -> buff.readShort();

            case UINT_16 -> Short.toUnsignedInt(buff.readShort());

            case INT_32 -> {
                if (flipRegisters) {
                    final byte b1 = buff.readByte();
                    final byte b2 = buff.readByte();
                    final byte b3 = buff.readByte();
                    final byte b4 = buff.readByte();
                    yield Unpooled.wrappedBuffer(new byte[]{b4, b3, b2, b1}).readInt();
                } else {
                    yield buff.readInt();
                }
            }

            case UINT_32 -> {
                if (flipRegisters) {
                    final byte b1 = buff.readByte();
                    final byte b2 = buff.readByte();
                    final byte b3 = buff.readByte();
                    final byte b4 = buff.readByte();
                    yield Unpooled.wrappedBuffer(new byte[]{b3, b4, b1, b2}).readUnsignedInt();
                } else {
                    yield buff.readUnsignedInt();
                }
            }

            case INT_64 -> flipRegisters ? getUnsigned64(buff) : buff.readLong();

            case FLOAT_32 -> {
                if (flipRegisters) {
                    final byte b1 = buff.readByte();
                    final byte b2 = buff.readByte();
                    final byte b3 = buff.readByte();
                    final byte b4 = buff.readByte();
                    yield Unpooled.wrappedBuffer(new byte[]{b3, b4, b1, b2}).readFloat();
                } else {
                    yield buff.readFloat();
                }
            }
            case FLOAT_64 -> flipRegisters ? getUnsigned64(buff) : buff.readDouble();

            case UTF_8 -> {
                final byte[] bytes = new byte[nrOfRegistersToRead * 2];
                buff.readBytes(bytes);
                yield new String(bytes, StandardCharsets.UTF_8);
            }
        };
    }
}
