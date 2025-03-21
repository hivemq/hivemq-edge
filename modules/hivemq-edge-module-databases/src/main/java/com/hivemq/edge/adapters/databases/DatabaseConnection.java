package com.hivemq.edge.adapters.databases;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {
    private final @NotNull HikariConfig config;
    private @Nullable HikariDataSource ds;

    public DatabaseConnection(final @NotNull String jdbcUrl, final @NotNull String username, final @NotNull String password, final int connectionTimeout, final boolean encrypt) {
        config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setConnectionTimeout(connectionTimeout * 1000L);


        String[] dataSource = config.getJdbcUrl().split(":");
        switch (dataSource[1]){
            case "mysql", "postgresql" -> {
                config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                config.addDataSourceProperty("cachePrepStmts", "true");
                config.addDataSourceProperty("prepStmtCacheSize", "250");
            }
            case "sqlserver" -> {
                config.setDataSourceClassName("com.microsoft.sqlserver.jdbc.SQLServerDataSource");
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
