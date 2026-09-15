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
package etherip;

import etherip.protocol.Encapsulation;
import etherip.protocol.MessageRouterProtocol;
import etherip.protocol.ProtocolAdapter;
import etherip.protocol.SendRRDataProtocol;
import etherip.types.CNService;
import etherip.types.LogicalAddressPath;

public class EthernetIPWithODVA extends EtherNetIP {

    public EthernetIPWithODVA(final String address, final int slot) {
        super(address, slot);
    }

    public void getAttributeSingle(LogicalAddressPath logicalAddressPath, ProtocolAdapter protocolAdapter)
            throws Exception {
        executeSingle(CNService.Get_Attribute_Single, logicalAddressPath, protocolAdapter);
    }

    public void setAttributeSingle(LogicalAddressPath logicalAddressPath, ProtocolAdapter protocolAdapter)
            throws Exception {
        executeSingle(CNService.Set_Attribute_Single, logicalAddressPath, protocolAdapter);
    }

    protected void executeSingle(
            CNService service, LogicalAddressPath logicalAddressPath, ProtocolAdapter protocolAdapter)
            throws Exception {
        Encapsulation encap = new Encapsulation(
                Encapsulation.Command.SendRRData,
                this.connection.getSession(),
                new SendRRDataProtocol(new MessageRouterProtocol(service, logicalAddressPath, protocolAdapter)));

        this.connection.execute(encap);
    }

    //    MRChipWriteAnyProtocol cip_write = new MRChipWriteAnyProtocol(tagClasPath, value);
    //    Encapsulation encap = new Encapsulation(Encapsulation.Command.SendRRData,
    //            this.connection.getSession(),
    //            new SendRRDataProtocol(new UnconnectedSendProtocol(this.slot, cip_write)));
    //        this.connection.execute(encap);

    //    public void write(String[] tags, CIPData[] values) throws Exception {
    //        if (tags.length != values.length) {
    //            throw new IllegalArgumentException("Got " + tags.length + " tags but " + values.length + " values");
    //        } else {
    //            MRChipWriteAnyProtocol[] writes = new MRChipWriteAnyProtocol[tags.length];
    //
    //            for (int i = 0; i < tags.length; ++i) {
    //                writes[i] = new MRChipWriteAnyProtocol(tags[i], values[i]);
    //            }
    //
    //            Encapsulation encap = new Encapsulation(Command.SendRRData,
    //                    this.connection.getSession(),
    //                    new SendRRDataProtocol(new UnconnectedSendProtocol(this.slot,
    //                            new MessageRouterProtocol(CNService.CIP_MultiRequest,
    //                                    CNPath.MessageRouter(),
    //                                    new CIPMultiRequestProtocol(writes)))));
    //            this.connection.execute(encap);
    //        }
    //    }
}
