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
package com.hivemq.edge.adapters.etherip_cip_odva;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EthernetIPCipOdvaProtocolAdapterInformationTest {

    @Test
    void legacyProtocolIds_containNoBlankEntries() {
        // A blank legacy id would be registered by ProtocolAdapterFactoryManager, letting a missing/blank adapter
        // type resolve to this adapter. This adapter is new and has no legacy alias, so the list must be empty.
        assertThat(EthernetIPCipOdvaProtocolAdapterInformation.INSTANCE.getLegacyProtocolIds())
                .isEmpty();
    }

    @Test
    void protocolId_isNotBlank() {
        assertThat(EthernetIPCipOdvaProtocolAdapterInformation.INSTANCE.getProtocolId())
                .isNotBlank();
    }
}
