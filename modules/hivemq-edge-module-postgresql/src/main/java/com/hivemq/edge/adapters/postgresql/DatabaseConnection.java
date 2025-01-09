package com.hivemq.edge.adapters.postgresql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnection {

    private @Nullable HikariDataSource ds;

    public DatabaseConnection() {
    }

    public void connect(final @NotNull String jdbcUrl, final @NotNull String username, final @NotNull String password) {
        final HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
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
