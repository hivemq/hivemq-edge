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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import org.jetbrains.annotations.NotNull;

/**
 * The v2 HTTP adapter's instance configuration — the adapter-level response-handling policy carried over from the v1
 * adapter. The per-tag polling cadence lives on each tag ({@code poll-interval-millis}) and the MQTT envelope is owned
 * by the framework's northbound mappings, so those v1 settings do not appear here.
 *
 * @param httpConnectTimeoutSeconds       the timeout, in seconds, to establish the underlying HTTP connection.
 * @param allowUntrustedCertificates      whether to trust any TLS certificate (for example expired certificates).
 * @param assertResponseIsJson            whether to always parse a response body as JSON regardless of its
 *                                        {@code Content-Type}.
 * @param httpPublishSuccessStatusCodeOnly whether to only publish data for a successful (200–299) HTTP status.
 */
public record HttpAdapterConfiguration(
        int httpConnectTimeoutSeconds,
        boolean allowUntrustedCertificates,
        boolean assertResponseIsJson,
        boolean httpPublishSuccessStatusCodeOnly) {

    static final int DEFAULT_TIMEOUT_SECONDS = 5;
    static final int MIN_TIMEOUT_SECONDS = 1;
    static final int MAX_TIMEOUT_SECONDS = 60;

    /**
     * Parse the adapter's instance configuration, applying the v1 defaults for any absent setting and clamping the
     * connect timeout to a sane ceiling. Tolerant of an absent, empty, or additionally-populated configuration.
     *
     * @param adapterConfig the reused v1 configuration value handed to the adapter.
     * @param objectMapper  the mapper used to read the configuration map.
     * @return the parsed configuration.
     */
    public static @NotNull HttpAdapterConfiguration parse(
            final @NotNull DataPoint adapterConfig, final @NotNull ObjectMapper objectMapper) {
        final JsonNode node = objectMapper.valueToTree(adapterConfig.getTagValue());
        final int connectTimeout = intField(node, "httpConnectTimeoutSeconds", DEFAULT_TIMEOUT_SECONDS);
        return new HttpAdapterConfiguration(
                Math.max(MIN_TIMEOUT_SECONDS, Math.min(connectTimeout, MAX_TIMEOUT_SECONDS)),
                boolField(node, "allowUntrustedCertificates", false),
                boolField(node, "assertResponseIsJson", false),
                boolField(node, "httpPublishSuccessStatusCodeOnly", true));
    }

    private static int intField(final @NotNull JsonNode node, final @NotNull String field, final int defaultValue) {
        final JsonNode value = node.get(field);
        if (value == null) {
            return defaultValue;
        }
        if (value.isIntegralNumber() && value.canConvertToInt()) {
            return value.intValue();
        }
        if (value.isTextual()) {
            try {
                return Integer.parseInt(value.textValue());
            } catch (final NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private static boolean boolField(
            final @NotNull JsonNode node, final @NotNull String field, final boolean defaultValue) {
        final JsonNode value = node.get(field);
        if (value == null) {
            return defaultValue;
        }
        if (value.isBoolean()) {
            return value.booleanValue();
        }
        if (value.isTextual()) {
            final String text = value.textValue();
            if ("true".equalsIgnoreCase(text)) {
                return true;
            }
            if ("false".equalsIgnoreCase(text)) {
                return false;
            }
        }
        return defaultValue;
    }
}
