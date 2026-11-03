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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/// What the phone home adds to an event: the licence id while the licence is valid, and the inventory of every
/// configured adapter across all adapter frameworks.
class PhoneHomeDataTest {

    private static final @NotNull ZoneId ZONE = ZoneId.of("UTC");

    @AfterEach
    void tearDown() {
        System.clearProperty(PhoneHomeData.LICENSE_ID_PROPERTY);
        System.clearProperty(PhoneHomeData.LICENSE_VALID_UNTIL_PROPERTY);
        PhoneHomeData.removeAdapterSource("legacy");
        PhoneHomeData.removeAdapterSource("v2");
        PhoneHomeData.removeAdapterSource("broken");
        PhoneHomeData.clock(Clock.system(ZoneId.systemDefault()));
    }

    /// Instances of one protocol and version collapse into one counted entry, sorted by key.
    @Test
    void inventory_whenSeveralAdapters_thenOneSortedEntryPerProtocolAndVersion() {
        final List<PhoneHomeData.ConfiguredAdapter> adapters = List.of(
                adapter("opcua", "2026.16"),
                adapter("modbus", "2026.16"),
                adapter("opcua", "2026.16"),
                adapter("opcua", "2026.15"));

        assertThat(PhoneHomeData.inventory(adapters)).isEqualTo("modbus:2026.16:1,opcua:2026.15:1,opcua:2026.16:2");
    }

    /// An adapter whose type is not installed still counts, with the version `unknown`.
    @Test
    void inventory_whenTypeNotInstalled_thenCountedWithUnknownVersion() {
        assertThat(PhoneHomeData.inventory(List.of(adapter("opcua", null), adapter("opcua", null))))
                .isEqualTo("opcua:unknown:2");
    }

    /// The format's separators cannot appear inside a field, and a blank protocol id reads `unknown`.
    @Test
    void inventory_whenFieldsHoldSeparatorsOrAreBlank_thenEscaped() {
        assertThat(PhoneHomeData.inventory(List.of(adapter("my:proto", "1,2"), adapter(" ", "1.0"))))
                .isEqualTo("my_proto:1_2:1,unknown:1.0:1");
    }

    /// Nothing configured, nothing listed.
    @Test
    void inventory_whenNoAdapters_thenEmpty() {
        assertThat(PhoneHomeData.inventory(List.of())).isEmpty();
    }

    /// First-party types report a placeholder resolved to the Edge version; a missing version becomes `unknown`.
    @Test
    void version_whenPlaceholderOrMissing_thenEdgeVersionOrUnknown() {
        assertThat(PhoneHomeData.version(PhoneHomeData.EDGE_VERSION_PLACEHOLDER, "2026.16"))
                .isEqualTo("2026.16");
        assertThat(PhoneHomeData.version("1.0.0", "2026.16")).isEqualTo("1.0.0");
        assertThat(PhoneHomeData.version(null, "2026.16")).isEqualTo(PhoneHomeData.UNKNOWN_VERSION);
        assertThat(PhoneHomeData.version(" ", "2026.16")).isEqualTo(PhoneHomeData.UNKNOWN_VERSION);
    }

    /// A ping counts the adapters of every registered framework together.
    @Test
    void ping_whenTwoFrameworksRegistered_thenInventoryCoversBoth() throws Exception {
        PhoneHomeData.adapterSource("legacy", () -> List.of(adapter("opcua", "2026.16"), adapter("s7", null)));
        PhoneHomeData.adapterSource("v2", () -> List.of(adapter("http-v2", "1.0.0"), adapter("file-v2", "1.0.0")));

        assertThat(adaptersOf(HiveMQEdgeRemoteEvent.EVENT_TYPE.EDGE_PING))
                .isEqualTo("file-v2:1.0.0:1,http-v2:1.0.0:1,opcua:2026.16:1,s7:unknown:1");
    }

    /// A framework whose source throws leaves only its own adapters out of the ping.
    @Test
    void ping_whenOneSourceFails_thenTheOthersStillCount() throws Exception {
        PhoneHomeData.adapterSource("legacy", () -> List.of(adapter("opcua", "2026.16")));
        PhoneHomeData.adapterSource("broken", () -> {
            throw new IllegalStateException("not started");
        });

        assertThat(adaptersOf(HiveMQEdgeRemoteEvent.EVENT_TYPE.EDGE_PING)).isEqualTo("opcua:2026.16:1");
    }

    /// Only pings carry the inventory; other events do not.
    @Test
    void event_whenNotAPing_thenNoAdaptersKey() throws Exception {
        PhoneHomeData.adapterSource("legacy", () -> List.of(adapter("opcua", "2026.16")));

        assertThat(toJson(new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.ADAPTER_STARTED))
                        .at("/userData")
                        .has(PhoneHomeData.ADAPTERS_KEY))
                .isFalse();
    }

    /// A ping with nothing configured carries no `adapters` key, not an empty one.
    @Test
    void ping_whenNoAdapters_thenNoAdaptersKey() throws Exception {
        PhoneHomeData.adapterSource("legacy", List::of);

        assertThat(toJson(new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.EDGE_PING))
                        .at("/userData")
                        .has(PhoneHomeData.ADAPTERS_KEY))
                .isFalse();
    }

    /// Every event carries the id of a licence valid today, including on its last valid day.
    @Test
    void event_whenLicenseValidToday_thenEveryEventCarriesItsId() throws Exception {
        PhoneHomeData.clock(
                Clock.fixed(LocalDate.of(2026, 11, 3).atStartOfDay(ZONE).toInstant(), ZONE));
        System.setProperty(PhoneHomeData.LICENSE_ID_PROPERTY, "0f5c1b9e-licence");
        System.setProperty(PhoneHomeData.LICENSE_VALID_UNTIL_PROPERTY, "2026-11-03");

        final JsonNode json = toJson(new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.ADAPTER_STARTED));

        assertThat(json.at("/userData/licenseId").asText()).isEqualTo("0f5c1b9e-licence");
    }

    /// The day after the licence's last valid day the id is gone, though the licence folder was not checked again.
    @Test
    void event_whenLicenseExpiredSinceLastFolderCheck_thenNoLicenseIdKey() throws Exception {
        PhoneHomeData.clock(
                Clock.fixed(LocalDate.of(2026, 11, 4).atStartOfDay(ZONE).toInstant(), ZONE));
        System.setProperty(PhoneHomeData.LICENSE_ID_PROPERTY, "0f5c1b9e-licence");
        System.setProperty(PhoneHomeData.LICENSE_VALID_UNTIL_PROPERTY, "2026-11-03");

        final JsonNode json = toJson(new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.EDGE_PING));

        assertThat(json.at("/userData").has(PhoneHomeData.LICENSE_ID_KEY)).isFalse();
    }

    /// An unreadable validity date sends no id rather than one that may have expired.
    @Test
    void event_whenValidityUnreadable_thenNoLicenseIdKey() {
        System.setProperty(PhoneHomeData.LICENSE_ID_PROPERTY, "0f5c1b9e-licence");
        System.setProperty(PhoneHomeData.LICENSE_VALID_UNTIL_PROPERTY, "not-a-date");

        assertThat(PhoneHomeData.currentLicenseId()).isNull();
    }

    /// Without a licence no event carries a `licenseId`.
    @Test
    void event_whenNoLicense_thenNoLicenseIdKey() throws Exception {
        final JsonNode json = toJson(new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.EDGE_STARTED));

        assertThat(json.at("/userData").has(PhoneHomeData.LICENSE_ID_KEY)).isFalse();
    }

    private static @NotNull String adaptersOf(final @NotNull HiveMQEdgeRemoteEvent.EVENT_TYPE type) throws Exception {
        return toJson(new HiveMQEdgeRemoteEvent(type)).at("/userData/adapters").asText();
    }

    private static @NotNull JsonNode toJson(final @NotNull HiveMQEdgeRemoteEvent event) throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(mapper.writeValueAsString(event));
    }

    private static @NotNull PhoneHomeData.ConfiguredAdapter adapter(
            final @NotNull String protocolId, final String version) {
        return new PhoneHomeData.ConfiguredAdapter(protocolId, version);
    }
}
