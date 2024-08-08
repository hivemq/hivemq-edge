package com.hivemq.edge.adapters.etherip.model;

import com.hivemq.edge.adapters.etherip.model.dataypes.EtherIpBool;
import com.hivemq.edge.adapters.etherip.model.dataypes.EtherIpDouble;
import com.hivemq.edge.adapters.etherip.model.dataypes.EtherIpInt;
import com.hivemq.edge.adapters.etherip.model.dataypes.EtherIpLong;
import com.hivemq.edge.adapters.etherip.model.dataypes.EtherIpString;
import etherip.types.CIPData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class EtherIpDataTypeFactory {
    private static final Logger log = LoggerFactory.getLogger(EtherIpDataType.class);

    public static Optional<EtherIpDataType> fromTagAddressAndCipData(@NotNull String tagAddress, @NotNull CIPData cipData) {
        CIPData.Type dataType = cipData.getType();

        try {
            if(cipData.isNumeric()) {
                if (cipData.getElementCount() > 1) {
                    log.warn("More than one element returned, only the first one will be used");
                }
                Number number = cipData.getNumber(0);
                log.debug("Got value {} for type {} for tag address {}", number, dataType, tagAddress);
                switch (dataType) {
                    case BOOL:
                        return Optional.of(new EtherIpBool(tagAddress, number));
                    case SINT:
                    case INT:
                        return Optional.of(new EtherIpInt(tagAddress, number));
                    case DINT:
                        return Optional.of(new EtherIpLong(tagAddress, number));
                    case REAL:
                        return Optional.of(new EtherIpDouble(tagAddress, number));
                    case BITS:
                    case STRUCT:
                    case STRUCT_STRING:
                        return Optional.empty();
                }
                return Optional.empty();
            } else {
                log.debug("Got value {} for type {} for tag address {}", cipData.getString(), dataType, tagAddress);
                return Optional.of(new EtherIpString(tagAddress, cipData.getString()));
            }
        } catch (Exception e) {
            log.error("Unable to parse data", e);
        }
        return Optional.empty();
    }
}
