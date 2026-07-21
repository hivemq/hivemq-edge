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

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class DatabasesAdapterConfigurationParseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private @NotNull DataPoint configOf(final @NotNull Map<String, Object> values) {
        return new DatabasesAdapterTestFixtures.TestDataPoint("adapter-configuration", values, false);
    }

    @Test
    void aFullConfigurationParsesEveryField() {
        final DatabasesAdapterConfiguration configuration = DatabasesAdapterConfiguration.parse(
                configOf(Map.of(
                        "type",
                        "MSSQL",
                        "server",
                        "database.example.com",
                        "port",
                        1433,
                        "database",
                        "warehouse",
                        "username",
                        "reader",
                        "password",
                        "secret",
                        "encrypt",
                        true,
                        "trustCertificate",
                        true,
                        "connectionTimeoutSeconds",
                        60)),
                objectMapper);

        assertThat(configuration.type()).isEqualTo(DatabaseType.MSSQL);
        assertThat(configuration.server()).isEqualTo("database.example.com");
        assertThat(configuration.port()).isEqualTo(1433);
        assertThat(configuration.database()).isEqualTo("warehouse");
        assertThat(configuration.username()).isEqualTo("reader");
        assertThat(configuration.password()).isEqualTo("secret");
        assertThat(configuration.encrypt()).isTrue();
        assertThat(configuration.trustCertificate()).isTrue();
        assertThat(configuration.connectionTimeoutSeconds()).isEqualTo(60);
    }

    @Test
    void theV1DefaultsApplyToAPartialConfiguration() {
        final DatabasesAdapterConfiguration configuration = DatabasesAdapterConfiguration.parse(
                configOf(Map.of(
                        "type", "POSTGRESQL",
                        "server", "localhost",
                        "port", 5432,
                        "database", "postgres",
                        "username", "postgres",
                        "password", "postgres")),
                objectMapper);

        assertThat(configuration.encrypt()).isFalse();
        assertThat(configuration.trustCertificate()).isFalse();
        assertThat(configuration.connectionTimeoutSeconds())
                .isEqualTo(DatabasesAdapterConfiguration.DEFAULT_CONNECTION_TIMEOUT_SECONDS);
    }

    @ParameterizedTest
    @EnumSource(DatabaseType.class)
    void everyEngineParses(final @NotNull DatabaseType type) {
        final DatabasesAdapterConfiguration configuration =
                DatabasesAdapterConfiguration.parse(configOf(Map.of("type", type.name())), objectMapper);

        assertThat(configuration.type()).isEqualTo(type);
    }

    @Test
    void unknownKeysAreIgnored() {
        final DatabasesAdapterConfiguration configuration = DatabasesAdapterConfiguration.parse(
                configOf(Map.of("type", "MYSQL", "legacyExtra", "ignored")), objectMapper);

        assertThat(configuration.type()).isEqualTo(DatabaseType.MYSQL);
    }

    @Test
    void theConnectionTimeoutIsClampedToItsRange() {
        final DatabasesAdapterConfiguration tooLarge =
                DatabasesAdapterConfiguration.parse(configOf(Map.of("connectionTimeoutSeconds", 10_000)), objectMapper);
        final DatabasesAdapterConfiguration tooSmall =
                DatabasesAdapterConfiguration.parse(configOf(Map.of("connectionTimeoutSeconds", 0)), objectMapper);

        assertThat(tooLarge.connectionTimeoutSeconds())
                .isEqualTo(DatabasesAdapterConfiguration.MAX_CONNECTION_TIMEOUT_SECONDS);
        assertThat(tooSmall.connectionTimeoutSeconds())
                .isEqualTo(DatabasesAdapterConfiguration.MIN_CONNECTION_TIMEOUT_SECONDS);
    }

    @Test
    void numbersCarriedAsStringsParse() {
        final DatabasesAdapterConfiguration configuration =
                DatabasesAdapterConfiguration.parse(configOf(Map.of("port", "5432")), objectMapper);

        assertThat(configuration.port()).isEqualTo(5432);
    }

    @Test
    void aRecognizedEngineIsNotReportedAsUnsupported() {
        assertThat(DatabasesAdapterConfiguration.unsupportedTypeError(configOf(Map.of("type", "MYSQL")), objectMapper))
                .isNull();
    }

    @Test
    void anAbsentEngineIsNotReportedAsUnsupported() {
        // An absent type is not an unsupported-engine error: parse applies the default engine, and the schema marks
        // type as required.
        assertThat(DatabasesAdapterConfiguration.unsupportedTypeError(
                        configOf(Map.of("server", "localhost")), objectMapper))
                .isNull();
    }

    @Test
    void anUnknownEngineIsReportedAsUnsupportedInsteadOfSilentlyBecomingPostgresql() {
        // The parse stays total (a placeholder engine keeps construction from throwing), but the unsupported name is
        // surfaced so the adapter reports a clear connection error rather than connecting through the wrong driver.
        assertThat(DatabasesAdapterConfiguration.unsupportedTypeError(configOf(Map.of("type", "ORACLE")), objectMapper))
                .isNotNull()
                .contains("ORACLE")
                .contains("POSTGRESQL", "MYSQL", "MSSQL");
    }

    @Test
    void aCleanMysqlConfigurationHasNoUrlIdentifierError() {
        assertThat(mysqlConfiguration("database.example.com", "warehouse").mysqlUrlIdentifierError())
                .isNull();
    }

    @Test
    void aMysqlServerCarryingAUrlBreakingCharacterIsReported() {
        // A server that closes the authority and opens query parameters would inject connection options.
        assertThat(mysqlConfiguration("host/inject?useSSL=true", "warehouse").mysqlUrlIdentifierError())
                .isNotNull()
                .contains("server")
                .contains("connection URL");
    }

    @Test
    void aMysqlDatabaseCarryingAUrlBreakingCharacterIsReported() {
        // A database name with a query separator would append arbitrary MariaDB connection parameters.
        assertThat(mysqlConfiguration("database.example.com", "warehouse?allowLoadLocalInfile=true")
                        .mysqlUrlIdentifierError())
                .isNotNull()
                .contains("database")
                .contains("connection URL");
    }

    @Test
    void aNonMysqlEngineIsNotCheckedForUrlIdentifiers() {
        // PostgreSQL and MS SQL pass the server and database as data-source properties, not URL segments, so the same
        // characters cannot alter the connection and are not rejected.
        assertThat(new DatabasesAdapterConfiguration(
                                DatabaseType.POSTGRESQL,
                                "host",
                                5432,
                                "weird?name",
                                "user",
                                "password",
                                false,
                                false,
                                30)
                        .mysqlUrlIdentifierError())
                .isNull();
    }

    private static @NotNull DatabasesAdapterConfiguration mysqlConfiguration(
            final @NotNull String server, final @NotNull String database) {
        return new DatabasesAdapterConfiguration(
                DatabaseType.MYSQL, server, 3306, database, "user", "password", false, false, 30);
    }
}
