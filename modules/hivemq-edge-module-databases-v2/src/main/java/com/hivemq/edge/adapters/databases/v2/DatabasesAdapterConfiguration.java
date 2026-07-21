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
import org.jetbrains.annotations.Nullable;

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
        int connectionTimeoutSeconds) {

    static final int DEFAULT_CONNECTION_TIMEOUT_SECONDS = 30;
    static final int MIN_CONNECTION_TIMEOUT_SECONDS = 1;
    static final int MAX_CONNECTION_TIMEOUT_SECONDS = 180;

    /**
     * Parse the adapter's instance configuration, applying the documented defaults for any absent setting and clamping
     * the connection timeout to its documented range. The parse is total — it never throws — so a malformed value
     * cannot abort adapter construction: an absent or non-textual field takes its default, and an
     * explicitly-configured but unrecognized engine is reported separately by {@link #unsupportedTypeError} (the
     * adapter surfaces it as a connection error) rather than silently masquerading as the default engine. The message
     * shaping ({@code splitMode}) and its per-mode {@code batchSize} live on each tag's {@link DatabaseNode}, not here.
     *
     * @param adapterConfig the reused v1 configuration value handed to the adapter.
     * @param objectMapper  the mapper used to read the configuration map.
     * @return the parsed configuration.
     */
    public static @NotNull DatabasesAdapterConfiguration parse(
            final @NotNull DataPoint adapterConfig, final @NotNull ObjectMapper objectMapper) {
        final JsonNode node = objectMapper.valueToTree(adapterConfig.getTagValue());
        final int connectionTimeout = intField(node, "connectionTimeoutSeconds", DEFAULT_CONNECTION_TIMEOUT_SECONDS);
        return new DatabasesAdapterConfiguration(
                typeField(node),
                stringField(node, "server"),
                intField(node, "port", 0),
                stringField(node, "database"),
                stringField(node, "username"),
                stringField(node, "password"),
                boolField(node, "encrypt", false),
                boolField(node, "trustCertificate", false),
                Math.max(MIN_CONNECTION_TIMEOUT_SECONDS, Math.min(connectionTimeout, MAX_CONNECTION_TIMEOUT_SECONDS)));
    }

    /**
     * Report an explicitly-configured but unrecognized database engine, so the adapter can surface it as a clear
     * connection error at connect time instead of silently falling back to the default engine and attempting a
     * connection through the wrong JDBC driver. An absent or non-textual {@code type} is not reported here — the
     * framework projects {@code type} as a required field, and {@link #parse} applies the default engine for an
     * absent value.
     *
     * @param adapterConfig the reused v1 configuration value handed to the adapter.
     * @param objectMapper  the mapper used to read the configuration map.
     * @return a human-readable description of the unsupported engine, or {@code null} when the engine is recognized
     *         (or absent/non-textual).
     */
    static @Nullable String unsupportedTypeError(
            final @NotNull DataPoint adapterConfig, final @NotNull ObjectMapper objectMapper) {
        final JsonNode value =
                objectMapper.valueToTree(adapterConfig.getTagValue()).get("type");
        if (value == null || !value.isTextual()) {
            return null;
        }
        final String raw = value.textValue();
        for (final DatabaseType type : DatabaseType.values()) {
            if (type.name().equals(raw)) {
                return null;
            }
        }
        return "unsupported database type '" + raw + "' (expected one of POSTGRESQL, MYSQL, MSSQL)";
    }

    /**
     * The characters that would break out of the MySQL/MariaDB JDBC connection URL
     * ({@code jdbc:mariadb://server:port/database?params}) and let a crafted server or database name inject or
     * override connection parameters: the query/fragment/path separators and any whitespace or control character. A
     * legitimate host name (including a bracketed IPv6 literal) or database name never contains any of these, so the
     * check restores the allowlist the v1 adapter enforced through its {@code database} field pattern — which the v2
     * scalar schema cannot express.
     */
    private static final @NotNull String FORBIDDEN_URL_IDENTIFIER_CHARACTERS = "/\\?#&";

    /**
     * Report a MySQL {@code server} or {@code database} that would break out of the JDBC connection URL, so the adapter
     * can surface it as a clear connection error at connect time instead of opening a pool with an injected URL. Only
     * MySQL interpolates these values into a URL; PostgreSQL and MS SQL pass them as data-source properties, where the
     * same characters cannot alter the connection, so they are not checked here.
     *
     * @return a human-readable description of the offending identifier, or {@code null} when both are safe (or the
     *         engine is not MySQL).
     */
    @Nullable
    String mysqlUrlIdentifierError() {
        if (type != DatabaseType.MYSQL) {
            return null;
        }
        final String serverError = urlIdentifierError("server", server);
        return serverError != null ? serverError : urlIdentifierError("database", database);
    }

    private static @Nullable String urlIdentifierError(final @NotNull String field, final @NotNull String value) {
        for (int i = 0; i < value.length(); i++) {
            final char character = value.charAt(i);
            if (character <= ' ' || FORBIDDEN_URL_IDENTIFIER_CHARACTERS.indexOf(character) >= 0) {
                return "the MySQL "
                        + field
                        + " '"
                        + value
                        + "' contains a character that is not allowed in the connection URL";
            }
        }
        return null;
    }

    private static @NotNull DatabaseType typeField(final @NotNull JsonNode node) {
        final JsonNode value = node.get("type");
        if (value != null && value.isTextual()) {
            try {
                return DatabaseType.valueOf(value.textValue());
            } catch (final IllegalArgumentException ignored) {
                // Keep the parse total. This is a placeholder only: the adapter never opens a pool with it because
                // unsupportedTypeError() flags the same value and the adapter reports a connection error first.
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
