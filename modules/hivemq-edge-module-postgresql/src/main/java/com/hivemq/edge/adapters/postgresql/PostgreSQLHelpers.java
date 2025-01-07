/*
 * Copyright 2024-present HiveMQ GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.hivemq.edge.adapters.postgresql;

import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

public class PostgreSQLHelpers {
    // Database connection method
    public @NotNull Connection connectDatabase(
            final @NotNull String compiledUri,
            final @NotNull String username,
            final @NotNull String password) throws SQLException {
        return DriverManager.getConnection(compiledUri, username, password);
    }

    // Query cleaning method
    public @NotNull String removeLimitFromQuery(final @NotNull String query, final @NotNull String toRemove) {
        final var words = query.replace(";", "").split(" ");
        final StringBuilder newStr = new StringBuilder();
        var wasPreviousWord = false;
        for (final String word : words) {
            if (!Objects.equals(word, toRemove) && !wasPreviousWord) {
                newStr.append(word).append(" ");
            } else {
                wasPreviousWord = !wasPreviousWord;
            }
        }
        return newStr.toString();
    }
}
