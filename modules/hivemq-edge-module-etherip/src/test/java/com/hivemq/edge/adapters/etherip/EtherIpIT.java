package com.hivemq.edge.adapters.etherip;

import etherip.EtherNetIP;
import etherip.types.CIPData;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.hivemq.edge.adapters.etherip.Constants.TAG_REQUIRES_VPN;


@Tag(TAG_REQUIRES_VPN)
public class EtherIpIT {

    private static String address = "172.16.10.60";
    private static int slot = 0;
    private static short array = 1;
    private static String tag = "program:MainProgram.test_tag";

    @Test
    public void test() throws Exception {
        try (final EtherNetIP plc = new EtherNetIP(address, slot)) {
            plc.connectTcp();
            CIPData wdata = new CIPData(CIPData.Type.DINT, 1);
            wdata.set(0, 17);
            plc.writeTag(tag, wdata);
            final CIPData data = plc.readTag(tag, array);
            System.out.println(data);
        }
    }
}
