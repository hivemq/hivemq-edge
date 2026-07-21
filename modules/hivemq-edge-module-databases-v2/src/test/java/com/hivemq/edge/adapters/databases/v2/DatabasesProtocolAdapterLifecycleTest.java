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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hivemq.adapter.sdk.api.v2.model.WriteEntry;
import com.hivemq.protocols.v2.runtime.ManualDispatcher;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * The actor lifecycle: start and stop acknowledge immediately; connect registers the bundled JDBC drivers under the
 * module classloader, opens the pool, and validates a connection; disconnect closes the pool. Failures on the connect
 * path report a {@code CONNECTION}-scope error and close whatever was opened.
 */
class DatabasesProtocolAdapterLifecycleTest {

    private final ManualDispatcher dispatcher = new ManualDispatcher();
    private final RecordingProtocolAdapterOutput output = new RecordingProtocolAdapterOutput();

    /**
     * A {@link DatabaseConnection} test double that scripts the connect/validate path without a real pool.
     */
    private static final class ScriptedDatabaseConnection extends DatabaseConnection {

        private final @NotNull Connection connection;
        private final boolean connectFails;
        private boolean opened;
        private boolean closed;

        private ScriptedDatabaseConnection(
                final @NotNull DatabasesAdapterConfiguration configuration,
                final @NotNull Connection connection,
                final boolean connectFails) {
            super(configuration);
            this.connection = connection;
            this.connectFails = connectFails;
        }

        @Override
        public void connect() {
            if (connectFails) {
                throw new IllegalStateException("the pool could not reach the database");
            }
            opened = true;
        }

        @Override
        public @NotNull Connection getConnection() {
            return connection;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private @NotNull DatabasesProtocolAdapter adapterOver(final @NotNull ScriptedDatabaseConnection connection) {
        return new DatabasesProtocolAdapter(
                DatabasesAdapterTestFixtures.input(
                        "databases-v2-1",
                        dispatcher,
                        new DatabasesAdapterTestFixtures.TestDataPointFactory(),
                        DatabasesAdapterTestFixtures.configuration("POSTGRESQL", 5432),
                        List.of()),
                output,
                configuration -> connection);
    }

    private static @NotNull Connection validConnection() throws SQLException {
        final Connection connection = mock(Connection.class);
        when(connection.isValid(anyInt())).thenReturn(true);
        return connection;
    }

    @Test
    void startAndStop_acknowledgeImmediately() throws SQLException {
        final DatabasesProtocolAdapter adapter =
                adapterOver(new ScriptedDatabaseConnection(configuration(), validConnection(), false));

        adapter.start();
        adapter.stop();
        dispatcher.drainAll();

        assertThat(output.events).containsExactly("started", "stopped");
    }

    @Test
    void connect_opensAndValidatesThePool_thenAcknowledges() throws SQLException {
        final ScriptedDatabaseConnection connection =
                new ScriptedDatabaseConnection(configuration(), validConnection(), false);
        final DatabasesProtocolAdapter adapter = adapterOver(connection);

        adapter.connect();
        dispatcher.drainAll();

        assertThat(output.events).containsExactly("connected");
        assertThat(connection.opened).isTrue();
        assertThat(connection.closed).isFalse();
    }

    @Test
    void stop_closesThePoolEvenWithoutAPrecedingDisconnect() throws SQLException {
        // The wrapper can stop the adapter directly from ERROR — a watchdog timeout, defensive reset, or ADAPTER-scope
        // error — with the pool still open and no intervening disconnect. doStop must release it, not only
        // doDisconnect, or the Hikari housekeeper threads and pooled connections survive the adapter's removal.
        final ScriptedDatabaseConnection connection =
                new ScriptedDatabaseConnection(configuration(), validConnection(), false);
        final DatabasesProtocolAdapter adapter = adapterOver(connection);

        adapter.connect(); // opens the pool
        adapter.stop(); // stops without an intervening disconnect
        dispatcher.drainAll();

        assertThat(output.events).containsExactly("connected", "stopped");
        assertThat(connection.closed).isTrue();
    }

    @Test
    void connectWhereReturningTheValidationConnectionFails_reportsAConnectionErrorWithoutConnected()
            throws SQLException {
        // The validation connection is valid, but returning it to the pool (its close) throws. The result must be the
        // single connection error, never a premature connected() followed by a contradictory error.
        final Connection validButUncloseable = mock(Connection.class);
        when(validButUncloseable.isValid(anyInt())).thenReturn(true);
        doThrow(new SQLException("returning the validation connection to the pool failed"))
                .when(validButUncloseable)
                .close();
        final ScriptedDatabaseConnection connection =
                new ScriptedDatabaseConnection(configuration(), validButUncloseable, false);
        final DatabasesProtocolAdapter adapter = adapterOver(connection);

        adapter.connect();
        dispatcher.drainAll();

        assertThat(output.events).hasSize(1);
        assertThat(output.events.get(0))
                .startsWith("error:CONNECTION")
                .contains("returning the validation connection to the pool failed");
        assertThat(output.events).doesNotContain("connected");
        assertThat(connection.closed).isTrue();
    }

    @Test
    void connectWithAnUnsupportedEngine_reportsAConnectionErrorWithoutOpeningThePool() throws SQLException {
        final ScriptedDatabaseConnection connection =
                new ScriptedDatabaseConnection(configuration(), validConnection(), false);
        final DatabasesProtocolAdapter adapter = new DatabasesProtocolAdapter(
                DatabasesAdapterTestFixtures.input(
                        "databases-v2-1",
                        dispatcher,
                        new DatabasesAdapterTestFixtures.TestDataPointFactory(),
                        DatabasesAdapterTestFixtures.configuration("ORACLE", 1521),
                        List.of()),
                output,
                configuration -> connection);

        adapter.connect();
        dispatcher.drainAll();

        // A clear connection error, and the pool is never opened with the placeholder default engine's driver.
        assertThat(output.events).hasSize(1);
        assertThat(output.events.get(0)).startsWith("error:CONNECTION").contains("unsupported database type 'ORACLE'");
        assertThat(connection.opened).isFalse();
    }

    @Test
    void connectWithAMysqlIdentifierThatWouldBreakTheUrl_reportsAConnectionErrorWithoutOpeningThePool()
            throws SQLException {
        // A MySQL database name carrying a URL query separator would inject MariaDB connection parameters; the adapter
        // must refuse to connect rather than open a pool with the crafted URL.
        final Map<String, Object> injected = new HashMap<>(DatabasesAdapterTestFixtures.configuration("MYSQL", 3306));
        injected.put("database", "warehouse?allowLoadLocalInfile=true");
        final ScriptedDatabaseConnection connection =
                new ScriptedDatabaseConnection(configuration(), validConnection(), false);
        final DatabasesProtocolAdapter adapter = new DatabasesProtocolAdapter(
                DatabasesAdapterTestFixtures.input(
                        "databases-v2-1",
                        dispatcher,
                        new DatabasesAdapterTestFixtures.TestDataPointFactory(),
                        injected,
                        List.of()),
                output,
                configuration -> connection);

        adapter.connect();
        dispatcher.drainAll();

        assertThat(output.events).hasSize(1);
        assertThat(output.events.get(0)).startsWith("error:CONNECTION").contains("not allowed in the connection URL");
        assertThat(connection.opened).isFalse();
    }

    @Test
    void disconnect_closesThePool_thenAcknowledges() throws SQLException {
        final ScriptedDatabaseConnection connection =
                new ScriptedDatabaseConnection(configuration(), validConnection(), false);
        final DatabasesProtocolAdapter adapter = adapterOver(connection);

        adapter.connect();
        adapter.disconnect();
        dispatcher.drainAll();

        assertThat(output.events).containsExactly("connected", "disconnected");
        assertThat(connection.closed).isTrue();
    }

    @Test
    void aPoolOpenFailure_reportsAConnectionScopeError() throws SQLException {
        final ScriptedDatabaseConnection connection =
                new ScriptedDatabaseConnection(configuration(), validConnection(), true);
        final DatabasesProtocolAdapter adapter = adapterOver(connection);

        adapter.connect();
        dispatcher.drainAll();

        assertThat(output.events).hasSize(1);
        assertThat(output.events.get(0))
                .startsWith("error:CONNECTION")
                .contains("the pool could not reach the database");
        assertThat(connection.closed).isTrue();
    }

    @Test
    void anInvalidConnection_reportsAConnectionScopeError_andClosesThePool() throws SQLException {
        final Connection invalid = mock(Connection.class);
        when(invalid.isValid(anyInt())).thenReturn(false);
        final ScriptedDatabaseConnection connection = new ScriptedDatabaseConnection(configuration(), invalid, false);
        final DatabasesProtocolAdapter adapter = adapterOver(connection);

        adapter.connect();
        dispatcher.drainAll();

        assertThat(output.events).hasSize(1);
        assertThat(output.events.get(0)).startsWith("error:CONNECTION");
        assertThat(connection.closed).isTrue();
    }

    @Test
    void theContextClassloaderIsRestoredAfterConnect() throws SQLException {
        final ClassLoader original = Thread.currentThread().getContextClassLoader();
        final DatabasesProtocolAdapter adapter =
                adapterOver(new ScriptedDatabaseConnection(configuration(), validConnection(), false));

        adapter.connect();
        dispatcher.drainAll();

        assertThat(Thread.currentThread().getContextClassLoader()).isSameAs(original);
    }

    @Test
    void subscriptionsAndWrites_areDefensivelyRejected() throws SQLException {
        final DatabasesProtocolAdapter adapter =
                adapterOver(new ScriptedDatabaseConnection(configuration(), validConnection(), false));
        final DatabaseNode node = new DatabaseNode("SELECT 1", SplitMode.ALL_IN_ONE, 100);

        adapter.addSubscriptionBatch(List.of(node));
        adapter.writeBatch(
                List.of(new WriteEntry(node, new DatabasesAdapterTestFixtures.TestDataPoint("setpoint", "1", false))));
        dispatcher.drainAll();

        assertThat(output.nodeErrors).hasSize(1);
        assertThat(output.nodeErrors.get(0).reason()).contains("does not support subscriptions");
        assertThat(output.writeResults).hasSize(1);
        assertThat(output.writeResults.get(0).success()).isFalse();
    }

    private static @NotNull DatabasesAdapterConfiguration configuration() {
        return new DatabasesAdapterConfiguration(
                DatabaseType.POSTGRESQL,
                "localhost",
                5432,
                "testdatabase",
                "testuser",
                "testpassword",
                false,
                false,
                30);
    }
}
