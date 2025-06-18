package com.hivemq.edge.adapters.modbus;

import com.hivemq.edge.adapters.modbus.config.ModbusDataType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

class ModbusDataTypeConverter {
    static @NotNull Object convert(
            final @NotNull ByteBuf buffi,
            final @NotNull ModbusDataType dataType,
            final int count,
            final boolean flipRegisters) {
        switch (dataType) {
            case BOOL:
                return buffi.readBoolean();
            case INT_16:
                return buffi.readShort();
            case UINT_16:
                return Short.toUnsignedInt(buffi.readShort());
            case INT_32:
                if (flipRegisters) {
                    final byte b1 = buffi.readByte();
                    final byte b2 = buffi.readByte();
                    final byte b3 = buffi.readByte();
                    final byte b4 = buffi.readByte();
                    return Unpooled.wrappedBuffer(new byte[]{b4, b3, b2, b1}).readInt();
                } else {
                    return buffi.readInt();
                }
            case UINT_32:
                if (flipRegisters) {
                    final byte b1 = buffi.readByte();
                    final byte b2 = buffi.readByte();
                    final byte b3 = buffi.readByte();
                    final byte b4 = buffi.readByte();
                    return Unpooled.wrappedBuffer(new byte[]{b3, b4, b1, b2}).readUnsignedInt();
                } else {
                    return buffi.readUnsignedInt();
                }
            case INT_64:
                if (flipRegisters) {
                    return getUnsigned64(buffi);
                } else {
                    return buffi.readLong();
                }
            case FLOAT_32:
                if (flipRegisters) {
                    final byte b1 = buffi.readByte();
                    final byte b2 = buffi.readByte();
                    final byte b3 = buffi.readByte();
                    final byte b4 = buffi.readByte();
                    return Unpooled.wrappedBuffer(new byte[]{b3, b4, b1, b2}).readFloat();
                } else {
                    return buffi.readFloat();
                }
            case FLOAT_64:
                if (flipRegisters) {
                    return getUnsigned64(buffi);
                } else {
                    return buffi.readDouble();
                }
            case UTF_8:
                final byte[] bytes = new byte[count * 2];
                buffi.readBytes(bytes);
                return new String(bytes, StandardCharsets.UTF_8);
        }
        throw new RuntimeException("Unknown dataType '" + dataType.name() + "'.");
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
}
