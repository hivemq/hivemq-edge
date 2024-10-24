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


    public DataPoint readByte(final String address) {
        if(log.isTraceEnabled()) {
            log.trace("Reading bytes from address {} with count {}", address, 1);
        }
        return dataPointFactory.create(address, s7PLC.readByte(address, 1)[0]);
    }


    public DataPoint readBytes(final String address, final int count) {
        if(log.isTraceEnabled()) {
            log.trace("Reading bytes from address {} with count {}", address, count);
        }
        return dataPointFactory.create(address, s7PLC.readByte(address, count));
    }

    public List<DataPoint> read(final @NotNull S7DataType type, final @NotNull List<String> addresses) {
        if(log.isTraceEnabled()) {
            log.trace("Reading data from addresses {} with type {}", addresses, type);
        }
        switch (type) {
            case BYTE: throw new IllegalArgumentException("Byte data type not supported by this method, use readBytes");
            case BOOL: return combine(dataPointFactory, addresses, s7PLC.readBoolean(addresses));
            case INT16: return combine(dataPointFactory, addresses, s7PLC.readInt16(addresses));
            case UINT16: return combine(dataPointFactory, addresses, s7PLC.readUInt16(addresses));
            case INT32: return combine(dataPointFactory, addresses, s7PLC.readInt32(addresses));
            case UINT32: return combine(dataPointFactory, addresses, s7PLC.readUInt32(addresses));
            case INT64: return combine(dataPointFactory, addresses, s7PLC.readInt64(addresses));
            case REAL: return combine(dataPointFactory, addresses, s7PLC.readFloat32(addresses));
            case LREAL: return combine(dataPointFactory, addresses, s7PLC.readFloat64(addresses));
            case STRING: return combine(dataPointFactory, addresses, addresses.stream().map(s7PLC::readString).collect(Collectors.toList()));
            case DATE: return combine(dataPointFactory, addresses, addresses.stream().map(s7PLC::readDate).collect(Collectors.toList()));
            case TIME_OF_DAY: return combine(dataPointFactory, addresses, addresses.stream().map(s7PLC::readTimeOfDay).collect(Collectors.toList()));
            case TIME: return combine(dataPointFactory, addresses, addresses.stream().map(s7PLC::readTime).collect(Collectors.toList()));
            case DATE_AND_TIME: return combine(dataPointFactory, addresses, addresses.stream().map(s7PLC::readDTL).collect(Collectors.toList()));
            default: {
                log.error("Unspported tag-type {} at address {}", type, addresses);
                throw new IllegalArgumentException("Unspported tag-type " + type + " at address " + addresses);
            }
        }
    }
    
    public static List<DataPoint> combine(final @NotNull DataPointFactory dataPointFactory, final @NotNull List<String> addresses, final @NotNull  List<?> values) {
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
