package com.hivemq.edge.adapters.s7;

import com.github.xingshuangs.iot.protocol.s7.enums.EPlcType;
import com.github.xingshuangs.iot.protocol.s7.service.S7PLC;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.edge.adapters.s7.config.S7AdapterConfig;
import com.hivemq.edge.adapters.s7.config.S7DataType;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class S7Client {

    private static final Logger log = LoggerFactory.getLogger(S7Client.class);

    private final S7PLC s7PLC;
    
    private final DataPointFactory dataPointFactory;

    public S7Client(final @NotNull EPlcType elpcType, final @NotNull String hostname, final int port, final int rack, final int slot, final int pduLength, final @NotNull DataPointFactory dataPointFactory) {
        s7PLC = new S7PLC(elpcType, hostname, port, rack, slot, pduLength);
        this.dataPointFactory = dataPointFactory;
    }

    public List<DataPoint> read(final @NotNull S7DataType type, final @NotNull List<String> addresses) {
        if(log.isTraceEnabled()) {
            log.trace("Reading data from addresses {} with type {}", addresses, type);
        }
        switch (type) {
            case BOOL: return createDatapointsFromAddressesAndValues(dataPointFactory, addresses, s7PLC.readBoolean(addresses));
            case BYTE: return addresses.stream().map(address -> dataPointFactory
                            .create(address, s7PLC.readByte(address)))
                            .collect(Collectors.toList());
            case WORD: return createDatapointsFromAddressesAndValues(dataPointFactory, addresses, readBytes(addresses, 2));
            case DWORD: return createDatapointsFromAddressesAndValues(dataPointFactory, addresses, readBytes(addresses, 4));
            case LWORD: return createDatapointsFromAddressesAndValues(dataPointFactory, addresses, readBytes(addresses, 8));
            case USINT: return addresses.stream().map(address -> dataPointFactory
                                .create(address, Byte.toUnsignedInt(s7PLC.readByte(address))))
                                .collect(Collectors.toList());
            case UINT: return createDatapointsFromAddressesAndValues(dataPointFactory, addresses, s7PLC.readUInt16(addresses));
            case UDINT: return createDatapointsFromAddressesAndValues(dataPointFactory, addresses, s7PLC.readUInt32(addresses));
            case ULINT: return addresses.stream()
                                .map(address -> dataPointFactory.create(address, new BigInteger(Long.toUnsignedString(s7PLC.readInt64(address)))))
                                .collect(Collectors.toList());
            case SINT: return addresses.stream().map(address -> dataPointFactory
                                .create(address, ((Byte)s7PLC.readByte(address)).shortValue()))
                                .collect(Collectors.toList());
            case INT: return createDatapointsFromAddressesAndValues(dataPointFactory, addresses, s7PLC.readInt16(addresses));
            case DINT: return createDatapointsFromAddressesAndValues(dataPointFactory, addresses, s7PLC.readInt32(addresses));
            case LINT: return createDatapointsFromAddressesAndValues(dataPointFactory, addresses, s7PLC.readInt64(addresses));
            case REAL: return createDatapointsFromAddressesAndValues(dataPointFactory, addresses, s7PLC.readFloat32(addresses));
            case LREAL: return createDatapointsFromAddressesAndValues(dataPointFactory, addresses, s7PLC.readFloat64(addresses));
            case CHAR: return addresses.stream().map(address -> dataPointFactory
                                .create(address, s7PLC.readByte(address)))
                                .collect(Collectors.toList());
            case WCHAR: return addresses.stream()
                                .map(address -> {
                                        final byte[] bytes = s7PLC.readByte(address, 2);
                                        final char charValue = (char) ((bytes[0] & 0xff) << 8 | (bytes[1] & 0xff));
                                        return dataPointFactory.create(address, charValue);
                                    })
                                .collect(Collectors.toList());
            case STRING:
            case WSTRING: return createDatapointsFromAddressesAndValues(dataPointFactory, addresses, addresses.stream().map(s7PLC::readString).collect(Collectors.toList()));
            case TIME: return createDatapointsFromAddressesAndValues(dataPointFactory, addresses, addresses.stream().map(s7PLC::readTime).collect(Collectors.toList()));
            case LTIME: return createDatapointsFromAddressesAndValues(dataPointFactory, addresses, s7PLC.readInt64(addresses));
            case DATE: return createDatapointsFromAddressesAndValues(dataPointFactory, addresses, addresses.stream().map(s7PLC::readDate).collect(Collectors.toList()));
            case TOD: return createDatapointsFromAddressesAndValues(dataPointFactory, addresses, addresses.stream().map(s7PLC::readTimeOfDay).collect(Collectors.toList()));
            case LTOD: return addresses.stream()
                                .map(address -> dataPointFactory.create(address, new BigInteger(Long.toUnsignedString(s7PLC.readInt64(address)))))
                                .collect(Collectors.toList());
            case DT: return addresses.stream()
                                .map(address -> dataPointFactory.create(address, s7PLC.readDate(address)))
                                .collect(Collectors.toList());
            case LDT:return addresses.stream()
                                .map(address -> dataPointFactory.create(address, new BigInteger(Long.toUnsignedString(s7PLC.readInt64(address)))))
                                .collect(Collectors.toList());
            case DTL: return createDatapointsFromAddressesAndValues(dataPointFactory, addresses, addresses.stream().map(s7PLC::readDTL).collect(Collectors.toList()));
            default: {
                log.error("Unspported tag-type {} at address {}", type, addresses);
                throw new IllegalArgumentException("Unspported tag-type " + type + " at address " + addresses);
            }
        }
    }

    public List<byte[]> readBytes(final List<String> addresses, final int count) {
        return addresses.stream().map(address -> s7PLC.readByte(address, count)).collect(Collectors.toList());
    }

    public static List<DataPoint> createDatapointsFromAddressesAndValues(final @NotNull DataPointFactory dataPointFactory, final @NotNull List<String> addresses, final @NotNull  List<?> values) {
        return IntStream
                .range(0, addresses.size())
                .mapToObj(i -> dataPointFactory.create(addresses.get(i), values.get(i)))
                .collect(Collectors.toList());
    }

    public void connect() {
        s7PLC.connect();
    }

    public void disconnect() {
        s7PLC.close();
    }
    
    public static EPlcType getEplcType(final @NotNull S7AdapterConfig.ControllerType controllerType) {
        switch (controllerType) {
            case S7_200: return EPlcType.S200;
            case S7_200_SMART: return EPlcType.S200_SMART;
            case S7_300: return EPlcType.S300;
            case S7_400: return EPlcType.S400;
            case S7_1200: return EPlcType.S1200;
            case S7_1500: return EPlcType.S1500;
            default: throw new IllegalArgumentException("Unsupported controller type: " + controllerType);
        }
    }
}
