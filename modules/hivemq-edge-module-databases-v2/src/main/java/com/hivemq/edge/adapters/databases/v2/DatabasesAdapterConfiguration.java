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
package com.hivemq.edge.adapters.databases.v2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import org.jetbrains.annotations.NotNull;

/**
 * The v2 Databases adapter's instance configuration — the connection parameters carried over from the v1 adapter.
 * The per-tag polling cadence lives on each tag ({@code poll-interval-millis}), the MQTT envelope is owned by the
 * framework's northbound mappings, and the v1 poll-error removal threshold has no v2 equivalent (the framework counts
 * failures and the next scheduled poll is the retry), so those v1 settings do not appear here.
 *
 * @param type                     the database engine to connect to.
 * @param server                   the server address.
 * @param port                     the server port.
 * @param database                 the database name.
 * @param username                 the username for the connection.
 * @param password                 the password for the connection.
 * @param encrypt                  whether to use TLS to communicate with the remote database.
 * @param trustCertificate         whether to trust the remote server certificate implicitly (MS SQL only).
 * @param connectionTimeoutSeconds the timeout, in seconds, for connection establishment to the database.
 * @param batchSize                the number of result rows drained per split-lines {@code dataPoints} call (1..1000)
 *                                 — a cursor page size. Each row is emitted as its own value (its own data point)
 *                                 regardless; a larger batch size simply carries more rows per call rather than one
 *                                 call per row. Meaningful only when a tag's {@code spiltLinesInIndividualMessages} is
 *                                 set; array mode always ships every row in one message regardless.
 */
public record DatabasesAdapterConfiguration(
        @NotNull DatabaseType type,
        @NotNull String server,
        int port,
        @NotNull String database,
        @NotNull String username,
        @NotNull String password,
        boolean encrypt,
        boolean trustCertificate,
        int connectionTimeoutSeconds,
        int batchSize) {

    static final int DEFAULT_CONNECTION_TIMEOUT_SECONDS = 30;
    static final int MIN_CONNECTION_TIMEOUT_SECONDS = 1;
    static final int MAX_CONNECTION_TIMEOUT_SECONDS = 180;

    static final int DEFAULT_BATCH_SIZE = 100;
    static final int MIN_BATCH_SIZE = 1;
    static final int MAX_BATCH_SIZE = 1000;

    /**
     * Parse the adapter's instance configuration, applying the v1 defaults for any absent setting and clamping the
     * connection timeout to the documented range. Tolerant of unknown keys and unparseable values — the framework
     * validates the configuration against the type's schema before the adapter is constructed, so this parse never
     * throws.
     *
     * @param adapterConfig the reused v1 configuration value handed to the adapter.
     * @param objectMapper  the mapper used to read the configuration map.
     * @return the parsed configuration.
     */
    public static @NotNull DatabasesAdapterConfiguration parse(
            final @NotNull DataPoint adapterConfig, final @NotNull ObjectMapper objectMapper) {
        final JsonNode node = objectMapper.valueToTree(adapterConfig.getTagValue());
        final int connectionTimeout = intField(node, "connectionTimeoutSeconds", DEFAULT_CONNECTION_TIMEOUT_SECONDS);
        final int batchSize = intField(node, "batchSize", DEFAULT_BATCH_SIZE);
        return new DatabasesAdapterConfiguration(
                typeField(node),
                stringField(node, "server"),
                intField(node, "port", 0),
                stringField(node, "database"),
                stringField(node, "username"),
                stringField(node, "password"),
                boolField(node, "encrypt", false),
                boolField(node, "trustCertificate", false),
                Math.max(MIN_CONNECTION_TIMEOUT_SECONDS, Math.min(connectionTimeout, MAX_CONNECTION_TIMEOUT_SECONDS)),
                Math.max(MIN_BATCH_SIZE, Math.min(batchSize, MAX_BATCH_SIZE)));
    }

    private static @NotNull DatabaseType typeField(final @NotNull JsonNode node) {
        final JsonNode value = node.get("type");
        if (value != null && value.isTextual()) {
            try {
                return DatabaseType.valueOf(value.textValue());
            } catch (final IllegalArgumentException ignored) {
                return DatabaseType.POSTGRESQL;
            }
        }
        return DatabaseType.POSTGRESQL;
    }

    private static @NotNull String stringField(final @NotNull JsonNode node, final @NotNull String field) {
        final JsonNode value = node.get(field);
        if (value != null && value.isTextual()) {
            return value.textValue();
        }
        return "";
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
