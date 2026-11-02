/*
 * Copyright 2019-present HiveMQ GmbH
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
package com.hivemq.edge.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PhoneHomeDataTest {

    @AfterEach
    void tearDown() {
        System.clearProperty(PhoneHomeData.LICENSE_ID_PROPERTY);
        PhoneHomeData.adapterInventory(null);
    }

    @Test
    void inventory_whenSeveralAdapters_thenOneSortedEntryPerProtocolAndVersion() {
        final List<ProtocolAdapterInformation> adapters = List.of(
                adapter("opcua", "2026.16"),
                adapter("modbus", "2026.16"),
                adapter("opcua", "2026.16"),
                adapter("opcua", "2026.15"));

        assertThat(PhoneHomeData.inventory(adapters, "2026.16"))
                .isEqualTo("modbus:2026.16:1,opcua:2026.15:1,opcua:2026.16:2");
    }

    @Test
    void inventory_whenFirstPartyVersionPlaceholder_thenResolvedToEdgeVersion() {
        final List<ProtocolAdapterInformation> adapters =
                List.of(adapter("modbus", PhoneHomeData.EDGE_VERSION_PLACEHOLDER), adapter("bacnetip", "1.2.0"));

        assertThat(PhoneHomeData.inventory(adapters, "2026.16")).isEqualTo("bacnetip:1.2.0:1,modbus:2026.16:1");
    }

    @Test
    void inventory_whenNoAdapters_thenEmpty() {
        assertThat(PhoneHomeData.inventory(List.of(), "2026.16")).isEmpty();
    }

    @Test
    void event_whenLicenseLoaded_thenEveryEventCarriesItsId() throws Exception {
        System.setProperty(PhoneHomeData.LICENSE_ID_PROPERTY, "0f5c1b9e-licence");

        final JsonNode json = toJson(new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.ADAPTER_STARTED));

        assertThat(json.at("/userData/licenseId").asText()).isEqualTo("0f5c1b9e-licence");
    }

    @Test
    void event_whenNoLicense_thenNoLicenseIdKey() throws Exception {
        final JsonNode json = toJson(new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.EDGE_STARTED));

        assertThat(json.at("/userData").has(PhoneHomeData.LICENSE_ID_KEY)).isFalse();
    }

    @Test
    void ping_whenAdaptersConfigured_thenCarriesInventory_otherEventsDoNot() throws Exception {
        PhoneHomeData.adapterInventory(() -> "opcua:2026.16:2");

        assertThat(toJson(new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.EDGE_PING))
                        .at("/userData/adapters")
                        .asText())
                .isEqualTo("opcua:2026.16:2");
        assertThat(toJson(new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.ADAPTER_STARTED))
                        .at("/userData")
                        .has(PhoneHomeData.ADAPTERS_KEY))
                .isFalse();
    }

    @Test
    void ping_whenNoAdapters_thenNoAdaptersKey() throws Exception {
        PhoneHomeData.adapterInventory(() -> "");

        assertThat(toJson(new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.EDGE_PING))
                        .at("/userData")
                        .has(PhoneHomeData.ADAPTERS_KEY))
                .isFalse();
    }

    private static @NotNull JsonNode toJson(final @NotNull HiveMQEdgeRemoteEvent event) throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(mapper.writeValueAsString(event));
    }

    private static @NotNull ProtocolAdapterInformation adapter(
            final @NotNull String protocolId, final @NotNull String version) {
        final ProtocolAdapterInformation info = mock(ProtocolAdapterInformation.class);
        when(info.getProtocolId()).thenReturn(protocolId);
        when(info.getVersion()).thenReturn(version);
        return info;
    }
}
