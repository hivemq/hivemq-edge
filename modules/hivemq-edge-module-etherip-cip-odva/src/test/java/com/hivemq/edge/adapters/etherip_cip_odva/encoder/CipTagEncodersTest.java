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
package com.hivemq.edge.adapters.etherip_cip_odva.encoder;

import com.hivemq.edge.adapters.etherip_cip_odva.config.CipDataType;
import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTag;
import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTagDefinition;
import com.hivemq.edge.adapters.etherip_cip_odva.handler.CipTagEncodingAttributeProtocol;
import com.hivemq.edge.adapters.etherip_cip_odva.tag.TagGroup;
import etherip.EthernetIPWithODVA;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class CipTagEncodersTest {

    @Test
    @Disabled("Work in progress")
    void shouldWriteToPLC_experimental() throws Exception {

        EthernetIPWithODVA ethernetIPWithODVA = new EthernetIPWithODVA("localhost", 0);
        ethernetIPWithODVA.connectTcp();

        {
            String address = "@22/1/5"; // SINT[10]
            TagGroup tagGroup = new TagGroup(address);

            tagGroup.add(
                    new CipTag("scada", "scada", new CipTagDefinition(address, 1, CipDataType.BOOL, 0d, null, 9, 7)));
            tagGroup.add(new CipTag(
                    "int[]", "scada", new CipTagDefinition(address, 1, CipDataType.BOOL, 0d, null, 0, null)));

            CipTagEncodingAttributeProtocol cipTagEncodingAttributeProtocol = new CipTagEncodingAttributeProtocol(
                    new CipTagEncoders(), tagGroup.getTags(), ByteOrder.LITTLE_ENDIAN, (cipTag) -> {
                        return true;
                        //                        return false;
                    });

            ethernetIPWithODVA.setAttributeSingle(tagGroup.getLogicalAddressPath(), cipTagEncodingAttributeProtocol);
        }

        //        {
        //            String address = "@22/1/1"; // INT
        //            TagGroup tagGroup = new TagGroup(address);
        //
        //            tagGroup.add(new CipTag("scada", "scada", new CipTagDefinition(address, 1, CipDataType.BOOL, 0d,
        // 1, 7)));
        //
        //            CipTagEncodingAttributeProtocol cipTagEncodingAttributeProtocol = new
        // CipTagEncodingAttributeProtocol(new CipTagEncoders(),
        //                    tagGroup.getTags(),
        //                    ByteOrder.LITTLE_ENDIAN,
        //                    (cipTag) -> {
        //                        return true;
        ////                        return false;
        //                    });
        //
        //            ethernetIPWithODVA.setAttributeSingle(tagGroup.getLogicalAddressPath(),
        // cipTagEncodingAttributeProtocol);
        //        }

        //        {
        //            String address = "@22/1/2";
        //            TagGroup tagGroup = new TagGroup(address);
        //
        //            tagGroup.add(new CipTag("byte", "byte", new CipTagDefinition(address, 1, CipDataType.SSTRING, 0d,
        // 0, null)));
        ////            tagGroup.add(new CipTag("byte", "byte", new CipTagDefinition(address, 1, CipDataType.SSTRING,
        // 0d, 0, null)));
        //
        //            CipTagEncodingAttributeProtocol cipTagEncodingAttributeProtocol = new
        // CipTagEncodingAttributeProtocol(new CipTagEncoders(),
        //                    tagGroup.getTags(),
        //                    ByteOrder.LITTLE_ENDIAN,
        //                    (cipTag) -> {
        //                        return
        // "1234567890123456789012345678901234567890123456789012345678901234567890123456789";
        ////                        return List.of("1234", "5678");
        //                    });
        //
        //            ethernetIPWithODVA.setAttributeSingle(tagGroup.getLogicalAddressPath(),
        // cipTagEncodingAttributeProtocol);
        //        }

        // Write to INT
        //        {
        //            String address = "@22/1/1";
        //            TagGroup tagGroup = new TagGroup(address);
        //
        ////        tagGroup.add(new CipTag("scada", "scada", new CipTagDefinition(address, 1, CipDataType.INT, 0d, 0,
        // null)));
        //
        //            tagGroup.add(new CipTag("scada", "scada", new CipTagDefinition(address, 1, CipDataType.SINT, 0d,
        // 1, null)));
        //
        //            CipTagEncodingAttributeProtocol cipTagEncodingAttributeProtocol = new
        // CipTagEncodingAttributeProtocol(new CipTagEncoders(),
        //                    tagGroup.getTags(),
        //                    ByteOrder.LITTLE_ENDIAN,
        //                    (cipTag) -> {
        //                        return 127;
        ////                        return -1;
        //                    });
        //
        //            ethernetIPWithODVA.setAttributeSingle(tagGroup.getLogicalAddressPath(),
        // cipTagEncodingAttributeProtocol);
        //        }

        //        {
        //            String address = "@22/1/3";
        //            TagGroup tagGroup = new TagGroup(address);
        //
        ////        tagGroup.add(new CipTag("scada", "scada", new CipTagDefinition(address, 1, CipDataType.INT, 0d, 0,
        // null)));
        //
        //            tagGroup.add(new CipTag("float", "float", new CipTagDefinition(address, 1, CipDataType.REAL, 0d,
        // 0, null)));
        //
        //            CipTagEncodingAttributeProtocol cipTagEncodingAttributeProtocol = new
        // CipTagEncodingAttributeProtocol(new CipTagEncoders(),
        //                    tagGroup.getTags(),
        //                    ByteOrder.LITTLE_ENDIAN,
        //                    (cipTag) -> {
        //                        return 123.45; //
        //                    });
        //
        //            ethernetIPWithODVA.setAttributeSingle(tagGroup.getLogicalAddressPath(),
        // cipTagEncodingAttributeProtocol);
        //        }

        //        {
        //            String address = "@22/1/6";
        //            TagGroup tagGroup = new TagGroup(address);
        //
        ////        tagGroup.add(new CipTag("scada", "scada", new CipTagDefinition(address, 1, CipDataType.INT, 0d, 0,
        // null)));
        //
        //            tagGroup.add(new CipTag("usint[]", "usint[]", new CipTagDefinition(address, 1, CipDataType.USINT,
        // 0d, 29, null)));
        //
        //            CipTagEncodingAttributeProtocol cipTagEncodingAttributeProtocol = new
        // CipTagEncodingAttributeProtocol(new CipTagEncoders(),
        //                    tagGroup.getTags(),
        //                    ByteOrder.LITTLE_ENDIAN,
        //                    (cipTag) -> {
        //                        return -1; //
        //                    });
        //
        //            ethernetIPWithODVA.setAttributeSingle(tagGroup.getLogicalAddressPath(),
        // cipTagEncodingAttributeProtocol);
        //        }

        // Write Array
        //        {
        //            String address = "@22/1/6";
        //            TagGroup tagGroup = new TagGroup(address);
        //
        ////        tagGroup.add(new CipTag("scada", "scada", new CipTagDefinition(address, 1, CipDataType.INT, 0d, 0,
        // null)));
        //
        //            tagGroup.add(new CipTag("usint[]", "usint[]", new CipTagDefinition(address, 30, CipDataType.USINT,
        // 0d, 0, null)));
        //
        //            List<Byte> values = new ArrayList<>();
        //            IntStream.rangeClosed(1, 30).forEach(v -> values.add((byte)v));
        //
        //            CipTagEncodingAttributeProtocol cipTagEncodingAttributeProtocol = new
        // CipTagEncodingAttributeProtocol(new CipTagEncoders(),
        //                    tagGroup.getTags(),
        //                    ByteOrder.LITTLE_ENDIAN,
        //                    (cipTag) -> {
        //                        return values;
        //                    });
        //
        //            ethernetIPWithODVA.setAttributeSingle(tagGroup.getLogicalAddressPath(),
        // cipTagEncodingAttributeProtocol);
        //        }

        ethernetIPWithODVA.close();
    }
}
