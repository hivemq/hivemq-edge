package com.hivemq.edge.adapters.databases;

import com.hivemq.edge.adapters.databases.config.DatabaseType;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {
    private static final @NotNull Logger log = LoggerFactory.getLogger(DatabaseConnection.class);
    private final @NotNull HikariConfig config;
    private @Nullable HikariDataSource ds;

    public DatabaseConnection(final @NotNull DatabaseType dbType,
                              final @NotNull String server,
                              final @NotNull Integer port,
                              final @NotNull String database,
                              final @NotNull String username,
                              final @NotNull String password,
                              final int connectionTimeout,
                              final boolean encrypt) {

        config = new HikariConfig();

        switch (dbType){
            case POSTGRESQL -> {
                config.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource");
                config.addDataSourceProperty("serverName", server);
                config.addDataSourceProperty("portNumber", port);
                config.addDataSourceProperty("databaseName", database);
                config.addDataSourceProperty("user", username);
                config.addDataSourceProperty("password", password);
                config.setConnectionTimeout(connectionTimeout * 2000L);
            }
            case MYSQL -> {
                config.setJdbcUrl(String.format("jdbc:mysql://%s:%s/%s?allowPublicKeyRetrieval=true&useSSL=%s",
                        server,
                        port,
                        database,
                        encrypt));
                config.addDataSourceProperty("user", username);
                config.addDataSourceProperty("password", password);
                config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                config.addDataSourceProperty("cachePrepStmts", "true");
                config.addDataSourceProperty("prepStmtCacheSize", "250");
                config.setConnectionTimeout(connectionTimeout * 2000L);
            }
            case MSSQL -> {
                config.setDataSourceClassName("com.microsoft.sqlserver.jdbc.SQLServerDataSource");
                config.addDataSourceProperty("serverName", server);
                config.addDataSourceProperty("portNumber", port);
                config.addDataSourceProperty("databaseName", database);
                config.addDataSourceProperty("user", username);
                config.addDataSourceProperty("password", password);
                config.setConnectionTimeout(connectionTimeout * 2000L);
                Properties properties = new Properties();
                if (encrypt) {
                    properties.setProperty("encrypt", "true");
                    properties.setProperty("trustServerCertificate", "true"); // Trust the server certificate implicitly
                } else properties.setProperty("encrypt", "false");
                config.setDataSourceProperties(properties);
            }
        }
    }

    public void connect() {
        log.debug("Connection settings : {}", config.toString());
        this.ds = new HikariDataSource(config);
    }

    public @NotNull Connection getConnection() throws SQLException {
        if (ds == null) {
            throw new IllegalStateException("Hikari Connection Pool must be started before usage.");
        }
        return ds.getConnection();
    }

    public void close() {
        if (ds != null) {
            ds.close();
        }
    }
}
