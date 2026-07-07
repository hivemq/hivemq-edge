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
package com.hivemq.edge.adapters.http.v2;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class HttpAdapterConfigurationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void emptyConfigurationYieldsTheV1Defaults() {
        final HttpAdapterConfiguration configuration = HttpAdapterConfiguration.parse(configOf(Map.of()), objectMapper);

        assertThat(configuration.httpConnectTimeoutSeconds()).isEqualTo(5);
        assertThat(configuration.allowUntrustedCertificates()).isFalse();
        assertThat(configuration.assertResponseIsJson()).isFalse();
        assertThat(configuration.httpPublishSuccessStatusCodeOnly()).isTrue();
    }

    @Test
    void partialConfigurationAppliesGivenValuesAndDefaultsTheRest() {
        final HttpAdapterConfiguration configuration = HttpAdapterConfiguration.parse(
                configOf(Map.of("assertResponseIsJson", true, "httpPublishSuccessStatusCodeOnly", false)),
                objectMapper);

        assertThat(configuration.assertResponseIsJson()).isTrue();
        assertThat(configuration.httpPublishSuccessStatusCodeOnly()).isFalse();
        assertThat(configuration.httpConnectTimeoutSeconds()).isEqualTo(5);
        assertThat(configuration.allowUntrustedCertificates()).isFalse();
    }

    @Test
    void clampsAnOversizedConnectTimeout() {
        final HttpAdapterConfiguration configuration =
                HttpAdapterConfiguration.parse(configOf(Map.of("httpConnectTimeoutSeconds", 9999)), objectMapper);

        assertThat(configuration.httpConnectTimeoutSeconds()).isEqualTo(60);
    }

    @Test
    void ignoresUnknownKeys() {
        final HttpAdapterConfiguration configuration = HttpAdapterConfiguration.parse(
                configOf(Map.of("allowUntrustedCertificates", true, "legacyPollingIntervalMillis", 1000)),
                objectMapper);

        assertThat(configuration.allowUntrustedCertificates()).isTrue();
    }

    private static @NotNull DataPoint configOf(final @NotNull Map<String, Object> map) {
        return new HttpAdapterTestFixtures.TestDataPoint("http-1", map, true);
    }
}
