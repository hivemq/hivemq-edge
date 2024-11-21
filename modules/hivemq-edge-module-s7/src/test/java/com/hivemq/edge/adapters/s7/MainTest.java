package com.hivemq.edge.adapters.s7;

import com.github.xingshuangs.iot.protocol.s7.enums.EPlcType;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.edge.adapters.s7.config.S7AdapterConfig;
import com.hivemq.edge.adapters.s7.config.S7DataType;
import com.hivemq.edge.modules.adapters.data.DataPointImpl;

import java.util.List;

public class MainTest {

    public static void main(String[] args) {
        s7_300();
        s7_1200();
        s7_1500();
    }

    public static void s7_300() {
        final EPlcType eplcType =
                S7Client.getEplcType(S7AdapterConfig.ControllerType.S7_300);
        S7Client s7Client = new S7Client(eplcType, "172.16.10.53", 102,
                eplcType.getRack(), eplcType.getSlot(), eplcType.getPduLength(), DataPointImpl::new);
        s7Client.connect();
        final DataPoint dataPoint = s7Client.readBytes("IB1", 1);
        System.out.println(((byte[])dataPoint.getTagValue())[0]); // => 122
        s7Client.disconnect();
    }

    public static void s7_1200() {
        final EPlcType eplcType =
                S7Client.getEplcType(S7AdapterConfig.ControllerType.S7_1200);
        S7Client s7Client = new S7Client(eplcType, "172.16.10.52", 102,
                eplcType.getRack(), eplcType.getSlot(), eplcType.getPduLength(), DataPointImpl::new);
        s7Client.connect();
        System.out.println(s7Client.read(S7DataType.BOOL, List.of("I5.0")).get(0).getTagValue()); // => false
        s7Client.disconnect();
    }

    public static void s7_1500() {
        final EPlcType eplcType =
                S7Client.getEplcType(S7AdapterConfig.ControllerType.S7_1500);
        S7Client s7Client = new S7Client(eplcType, "172.16.10.51", 102,
                eplcType.getRack(), eplcType.getSlot(), eplcType.getPduLength(), DataPointImpl::new);
        s7Client.connect();
        System.out.println(s7Client.read(S7DataType.BOOL, List.of("I5.0")).get(0).getTagValue()); // => false
        s7Client.disconnect();
    }
}
