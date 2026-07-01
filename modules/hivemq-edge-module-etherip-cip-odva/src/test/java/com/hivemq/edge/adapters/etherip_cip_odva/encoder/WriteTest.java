package com.hivemq.edge.adapters.etherip_cip_odva.encoder;
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

import etherip.EthernetIPWithODVA;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class WriteTest {

    @Disabled("Work in progress")
    @Test
    void shouldWrite() throws Exception {

        try (EthernetIPWithODVA ethernetIPWithODVA = new EthernetIPWithODVA("127.0.0.1", 0)) {
            ethernetIPWithODVA.connectTcp();

            //            LogicalAddressPath logicalAddressPath = new LogicalAddressPath(22, 1, 1);
            //        ethernetIPWithODVA.setAttributeSingle(logicalAddressPath, new );

        }
    }
}
