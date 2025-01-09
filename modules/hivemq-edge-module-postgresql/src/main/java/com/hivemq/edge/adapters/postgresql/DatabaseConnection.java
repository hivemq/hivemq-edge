package com.hivemq.edge.adapters.postgresql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnection {

    private final @NotNull HikariConfig config;
    private @Nullable HikariDataSource ds;

    public DatabaseConnection(final @NotNull String jdbcUrl, final @NotNull String username, final @NotNull String password, final int connectionTimeout) {
        config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setConnectionTimeout(connectionTimeout * 1000L);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
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
