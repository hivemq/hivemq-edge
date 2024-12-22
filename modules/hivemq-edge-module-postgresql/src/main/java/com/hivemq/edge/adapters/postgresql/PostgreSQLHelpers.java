package com.hivemq.edge.adapters.postgresql;

import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

public class PostgreSQLHelpers {
    // Database connection method
    public Connection connectDatabase(final @NotNull String compiledUri, final @NotNull String username, final @NotNull String password ) throws SQLException {
        return DriverManager.getConnection(compiledUri, username, password);
    }

    // Query cleaning method
    public String removeLimitFromQuery(final @NotNull String query, final @NotNull String toRemove) {
        var words = query.replace(";","").split(" ");
        StringBuilder newStr = new StringBuilder();
        var wasPreviousWord = false;
        for (String word : words) {
            if (!Objects.equals(word, toRemove) && !wasPreviousWord) {
                newStr.append(word).append(" ");
            } else {
                wasPreviousWord = !wasPreviousWord;
            }
        }
        return newStr.toString();
    }
}
