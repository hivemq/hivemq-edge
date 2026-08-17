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
package com.hivemq.edge.adapters.opcua;

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.edge.adapters.opcua.client.ParsedConfig;
import com.hivemq.edge.adapters.opcua.client.Success;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.SecPolicy;
import com.hivemq.edge.adapters.opcua.config.Security;
import com.hivemq.edge.adapters.opcua.config.Tls;
import com.hivemq.edge.adapters.opcua.config.TlsChecks;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * When the "any server certificate is accepted" warnings may fire. The warning must track what is
 * actually running, not what the configuration says: a validator that is not installed, or a
 * connection whose security policy never presents a certificate, accepts nothing — and a false
 * security alarm trains operators to ignore the true ones.
 */
class AnyCertificateWarningGateTest {

    @Test
    void anActiveAnyCertValidatorOnACertificateBearingPolicyWarns() {
        final OpcUaSpecificAdapterConfig config =
                config(new Tls(true, TlsChecks.NO_VERIFICATION, null, null, null, null), SecPolicy.BASIC256SHA256);

        assertThat(OpcUaClientConnection.warnsAnyCertificateAccepted(parse(config), config))
                .isTrue();
    }

    @Test
    void aDisabledTlsBlockDoesNotWarn() {
        // Sam's inert configuration: <enabled>false</enabled> around NO_VERIFICATION. No validator
        // is installed, so no certificate is ever accepted.
        final OpcUaSpecificAdapterConfig config =
                config(new Tls(false, TlsChecks.NO_VERIFICATION, null, null, null, null), SecPolicy.BASIC256SHA256);

        assertThat(OpcUaClientConnection.warnsAnyCertificateAccepted(parse(config), config))
                .isFalse();
    }

    @Test
    void aSecurityPolicyNoneEndpointDoesNotWarn() {
        // Under SecurityPolicy None the secure-channel handshake never consults the validator, so
        // "a certificate was accepted" would describe something that did not happen.
        final OpcUaSpecificAdapterConfig config =
                config(new Tls(true, TlsChecks.NO_VERIFICATION, null, null, null, null), SecPolicy.NONE);

        assertThat(OpcUaClientConnection.warnsAnyCertificateAccepted(parse(config), config))
                .isFalse();
    }

    @Test
    void aTrustingConfigurationDoesNotWarn() {
        final OpcUaSpecificAdapterConfig config =
                config(new Tls(true, TlsChecks.NONE, null, null, null, null), SecPolicy.BASIC256SHA256);

        assertThat(OpcUaClientConnection.warnsAnyCertificateAccepted(parse(config), config))
                .isFalse();
    }

    private static @NotNull ParsedConfig parse(final @NotNull OpcUaSpecificAdapterConfig config) {
        final var result = ParsedConfig.fromConfig(config);
        assertThat(result).isInstanceOf(Success.class);
        return ((Success<ParsedConfig, String>) result).result();
    }

    private static @NotNull OpcUaSpecificAdapterConfig config(
            final @NotNull Tls tls, final @Nullable SecPolicy policy) {
        return new OpcUaSpecificAdapterConfig(
                "opc.tcp://localhost:4840",
                false,
                null,
                null,
                tls,
                new OpcUaToMqttConfig(1, 1000),
                new Security(policy),
                null);
    }
}
