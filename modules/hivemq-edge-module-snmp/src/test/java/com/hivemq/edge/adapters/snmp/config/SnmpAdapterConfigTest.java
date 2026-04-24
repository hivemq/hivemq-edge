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
package com.hivemq.edge.adapters.snmp.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class SnmpAdapterConfigTest {

    private final @NotNull ObjectMapper mapper = new ObjectMapper();

    private @NotNull SnmpSpecificAdapterConfig load(final @NotNull String resource) throws Exception {
        try (final InputStream is = getClass().getResourceAsStream(resource)) {
            assertThat(is).as("test resource %s must exist", resource).isNotNull();
            return mapper.readValue(is, SnmpSpecificAdapterConfig.class);
        }
    }

    @Test
    void minimalConfig_appliesDefaults() throws Exception {
        final SnmpSpecificAdapterConfig config = load("/snmp-config-defaults.json");

        assertThat(config.getHost()).isEqualTo("192.168.1.1");
        assertThat(config.getPort()).isEqualTo(SnmpSpecificAdapterConfig.DEFAULT_PORT);
        assertThat(config.getSnmpVersion()).isEqualTo(SnmpVersion.V2C);
        assertThat(config.getCommunity()).isEqualTo(SnmpSpecificAdapterConfig.DEFAULT_COMMUNITY);
        assertThat(config.getSecurityName()).isNull();
        assertThat(config.getAuthProtocol()).isEqualTo(SnmpAuthProtocol.NONE);
        assertThat(config.getAuthPassword()).isNull();
        assertThat(config.getPrivProtocol()).isEqualTo(SnmpPrivProtocol.NONE);
        assertThat(config.getPrivPassword()).isNull();
        assertThat(config.getTimeoutMillis()).isEqualTo(SnmpSpecificAdapterConfig.DEFAULT_TIMEOUT_MILLIS);
        assertThat(config.getRetries()).isEqualTo(SnmpSpecificAdapterConfig.DEFAULT_RETRIES);
        assertThat(config.getSnmpToMqttConfig()).isNotNull();
    }

    @Test
    void fullV2cConfig_parsesAllFields() throws Exception {
        final SnmpSpecificAdapterConfig config = load("/snmp-config-full-v2c.json");

        assertThat(config.getHost()).isEqualTo("192.168.1.100");
        assertThat(config.getPort()).isEqualTo(1161);
        assertThat(config.getSnmpVersion()).isEqualTo(SnmpVersion.V2C);
        assertThat(config.getCommunity()).isEqualTo("private");
        assertThat(config.getTimeoutMillis()).isEqualTo(5000);
        assertThat(config.getRetries()).isEqualTo(3);

        final SnmpToMqttConfig mqtt = config.getSnmpToMqttConfig();
        assertThat(mqtt).isNotNull();
        assertThat(mqtt.getPollingIntervalMillis()).isEqualTo(2000);
        assertThat(mqtt.getMaxPollingErrorsBeforeRemoval()).isEqualTo(5);
        assertThat(mqtt.isPublishChangedDataOnly()).isTrue();
    }

    @Test
    void fullV3Config_parsesSecurityFields() throws Exception {
        final SnmpSpecificAdapterConfig config = load("/snmp-config-full-v3.json");

        assertThat(config.getSnmpVersion()).isEqualTo(SnmpVersion.V3);
        assertThat(config.getSecurityName()).isEqualTo("snmpuser");
        assertThat(config.getAuthProtocol()).isEqualTo(SnmpAuthProtocol.SHA256);
        assertThat(config.getAuthPassword()).isEqualTo("authpass123");
        assertThat(config.getPrivProtocol()).isEqualTo(SnmpPrivProtocol.AES256);
        assertThat(config.getPrivPassword()).isEqualTo("privpass456");
    }

    @Test
    void serialiseAndDeserialise_roundTripsCorrectly() throws Exception {
        final SnmpSpecificAdapterConfig original = load("/snmp-config-full-v2c.json");
        final String json = mapper.writeValueAsString(original);
        final SnmpSpecificAdapterConfig roundTripped = mapper.readValue(json, SnmpSpecificAdapterConfig.class);
        assertThat(roundTripped).isEqualTo(original);
    }

    @Test
    void snmpToMqttConfig_defaultsAppliedWhenOmitted() throws Exception {
        // The minimal config has no snmpToMqtt block — defaults must still be populated
        final SnmpSpecificAdapterConfig config = load("/snmp-config-defaults.json");
        final SnmpToMqttConfig mqtt = config.getSnmpToMqttConfig();

        assertThat(mqtt).isNotNull();
        assertThat(mqtt.getPollingIntervalMillis()).isEqualTo(SnmpToMqttConfig.DEFAULT_POLLING_INTERVAL_MILLIS);
        assertThat(mqtt.getMaxPollingErrorsBeforeRemoval())
                .isEqualTo(SnmpToMqttConfig.DEFAULT_MAX_POLLING_ERRORS_BEFORE_REMOVAL);
        assertThat(mqtt.isPublishChangedDataOnly()).isFalse();
    }
}
