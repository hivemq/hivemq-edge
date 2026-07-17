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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The HikariCP connection pool for one adapter instance, carried over from the v1 adapter: a per-engine
 * {@link HikariConfig} (PostgreSQL and MS SQL through their data-source classes, MySQL through a MariaDB JDBC URL),
 * opened on {@link #connect()} and released on {@link #close()}.
 * <p>
 * Three v1 defects are deliberately corrected here, with review sign-off recorded in the module README: the
 * connection timeout is converted with {@code * 1000} (v1 multiplied the configured seconds by 2000, doubling every
 * timeout), the {@code trustCertificate} setting now drives the MS SQL {@code trustServerCertificate} property (v1
 * ignored it and trusted the server certificate whenever {@code encrypt} was on), and callers close every JDBC
 * resource they obtain (v1 closed only the pooled connection).
 */
public class DatabaseConnection {

    private static final @NotNull Logger log = LoggerFactory.getLogger(DatabaseConnection.class);

    private final @NotNull HikariConfig config;
    private @Nullable HikariDataSource dataSource;

    public DatabaseConnection(final @NotNull DatabasesAdapterConfiguration configuration) {
        config = new HikariConfig();

        switch (configuration.type()) {
            case POSTGRESQL -> {
                config.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource");
                config.addDataSourceProperty("serverName", configuration.server());
                config.addDataSourceProperty("portNumber", configuration.port());
                config.addDataSourceProperty("databaseName", configuration.database());
                config.addDataSourceProperty("user", configuration.username());
                config.addDataSourceProperty("password", configuration.password());
                config.setConnectionTimeout(configuration.connectionTimeoutSeconds() * 1000L);
            }
            case MYSQL -> {
                config.setJdbcUrl(String.format(
                        "jdbc:mariadb://%s:%s/%s?allowPublicKeyRetrieval=true&useSSL=%s",
                        configuration.server(),
                        configuration.port(),
                        configuration.database(),
                        configuration.encrypt()));
                config.addDataSourceProperty("user", configuration.username());
                config.addDataSourceProperty("password", configuration.password());
                config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                config.addDataSourceProperty("cachePrepStmts", "true");
                config.addDataSourceProperty("prepStmtCacheSize", "250");
                config.setConnectionTimeout(configuration.connectionTimeoutSeconds() * 1000L);
            }
            case MSSQL -> {
                config.setDataSourceClassName("com.microsoft.sqlserver.jdbc.SQLServerDataSource");
                config.addDataSourceProperty("serverName", configuration.server());
                config.addDataSourceProperty("portNumber", configuration.port());
                config.addDataSourceProperty("databaseName", configuration.database());
                config.addDataSourceProperty("user", configuration.username());
                config.addDataSourceProperty("password", configuration.password());
                config.setConnectionTimeout(configuration.connectionTimeoutSeconds() * 1000L);
                final Properties properties = new Properties();
                if (configuration.encrypt()) {
                    properties.setProperty("encrypt", "true");
                    properties.setProperty(
                            "trustServerCertificate", Boolean.toString(configuration.trustCertificate()));
                } else {
                    properties.setProperty("encrypt", "false");
                }
                config.setDataSourceProperties(properties);
            }
        }
    }

    /**
     * Open the connection pool. The pool eagerly establishes its first connection, so a wrong address or credential
     * fails here.
     */
    public void connect() {
        log.debug("Connection settings : {}", config);
        this.dataSource = new HikariDataSource(config);
    }

    /**
     * Borrow a connection from the pool. The caller must close it (returning it to the pool) — best inside a
     * try-with-resources together with every statement and result set it creates.
     *
     * @return a pooled connection.
     * @throws SQLException if no connection can be established.
     */
    public @NotNull Connection getConnection() throws SQLException {
        final HikariDataSource currentDataSource = dataSource;
        if (currentDataSource == null) {
            throw new IllegalStateException("Hikari Connection Pool must be started before usage.");
        }
        return currentDataSource.getConnection();
    }

    /**
     * Close the pool and every idle connection it holds. Safe to call when the pool was never opened.
     */
    public void close() {
        final HikariDataSource currentDataSource = dataSource;
        if (currentDataSource != null) {
            currentDataSource.close();
        }
    }

    /**
     * The pool configuration, exposed for tests to assert the per-engine settings without opening a real pool.
     *
     * @return the Hikari configuration this pool opens with.
     */
    @NotNull
    HikariConfig hikariConfiguration() {
        return config;
    }
}
