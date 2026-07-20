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
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import com.zaxxer.hikari.HikariConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * Asserts the per-engine {@link HikariConfig} the pool opens with — without opening a real pool — including the
 * corrected connection-timeout conversion and the corrected MS SQL certificate-trust wiring.
 */
class DatabaseConnectionTest {

    private static @NotNull DatabasesAdapterConfiguration configuration(
            final @NotNull DatabaseType type, final boolean encrypt, final boolean trustCertificate) {
        return new DatabasesAdapterConfiguration(
                type,
                "database.example.com",
                4444,
                "warehouse",
                "reader",
                "secret",
                encrypt,
                trustCertificate,
                30,
                100);
    }

    @Test
    void postgresqlUsesTheSimpleDataSourceWithTheConnectionProperties() {
        final DatabaseConnection connection =
                new DatabaseConnection(configuration(DatabaseType.POSTGRESQL, false, false));

        final HikariConfig config = connection.hikariConfiguration();
        assertThat(config.getDataSourceClassName()).isEqualTo("org.postgresql.ds.PGSimpleDataSource");
        assertThat(config.getDataSourceProperties())
                .containsEntry("serverName", "database.example.com")
                .containsEntry("portNumber", 4444)
                .containsEntry("databaseName", "warehouse")
                .containsEntry("user", "reader")
                .containsEntry("password", "secret");
    }

    @Test
    void mysqlUsesAMariaDbJdbcUrlCarryingTheEncryptChoice() {
        final DatabaseConnection plain = new DatabaseConnection(configuration(DatabaseType.MYSQL, false, false));
        final DatabaseConnection encrypted = new DatabaseConnection(configuration(DatabaseType.MYSQL, true, false));

        assertThat(plain.hikariConfiguration().getJdbcUrl())
                .isEqualTo("jdbc:mariadb://database.example.com:4444/warehouse"
                        + "?allowPublicKeyRetrieval=true&useSSL=false");
        assertThat(encrypted.hikariConfiguration().getJdbcUrl())
                .isEqualTo(
                        "jdbc:mariadb://database.example.com:4444/warehouse?allowPublicKeyRetrieval=true&useSSL=true");
        assertThat(plain.hikariConfiguration().getDataSourceProperties())
                .containsEntry("user", "reader")
                .containsEntry("password", "secret");
    }

    @Test
    void mssqlUsesTheSqlServerDataSourceWithTheConnectionProperties() {
        final DatabaseConnection connection = new DatabaseConnection(configuration(DatabaseType.MSSQL, false, false));

        final HikariConfig config = connection.hikariConfiguration();
        assertThat(config.getDataSourceClassName()).isEqualTo("com.microsoft.sqlserver.jdbc.SQLServerDataSource");
        assertThat(config.getDataSourceProperties())
                .containsEntry("serverName", "database.example.com")
                .containsEntry("portNumber", 4444)
                .containsEntry("databaseName", "warehouse")
                .containsEntry("user", "reader")
                .containsEntry("password", "secret")
                .containsEntry("encrypt", "false");
    }

    @Test
    void theConnectionTimeoutIsTheConfiguredSecondsInMilliseconds() {
        // v1 multiplied the configured seconds by 2000, doubling every timeout; the v2 port corrects this.
        final DatabaseConnection connection =
                new DatabaseConnection(configuration(DatabaseType.POSTGRESQL, false, false));

        assertThat(connection.hikariConfiguration().getConnectionTimeout()).isEqualTo(30_000L);
    }

    @Test
    void mssqlCertificateTrustIsDrivenByTheTrustCertificateSetting() {
        // v1 ignored trustCertificate and trusted the server certificate whenever encrypt was on; the v2 port wires
        // the setting.
        final DatabaseConnection trusting = new DatabaseConnection(configuration(DatabaseType.MSSQL, true, true));
        final DatabaseConnection checking = new DatabaseConnection(configuration(DatabaseType.MSSQL, true, false));

        assertThat(trusting.hikariConfiguration().getDataSourceProperties())
                .containsEntry("encrypt", "true")
                .containsEntry("trustServerCertificate", "true");
        assertThat(checking.hikariConfiguration().getDataSourceProperties())
                .containsEntry("encrypt", "true")
                .containsEntry("trustServerCertificate", "false");
    }

    @Test
    void borrowingAConnectionBeforeThePoolOpensFailsClearly() {
        final DatabaseConnection connection =
                new DatabaseConnection(configuration(DatabaseType.POSTGRESQL, false, false));

        assertThatIllegalStateException()
                .isThrownBy(connection::getConnection)
                .withMessage("Hikari Connection Pool must be started before usage.");
    }

    @Test
    void closingANeverOpenedPoolIsSafe() {
        final DatabaseConnection connection =
                new DatabaseConnection(configuration(DatabaseType.POSTGRESQL, false, false));

        connection.close();
    }
}
