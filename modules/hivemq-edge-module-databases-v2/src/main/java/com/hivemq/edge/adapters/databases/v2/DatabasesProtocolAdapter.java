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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.v2.model.ErrorScope;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterOutput;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.adapter.sdk.api.v2.template.AbstractProtocolAdapter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The v2 Databases adapter runtime — an actor built on {@link AbstractProtocolAdapter}. It reproduces the v1
 * Databases adapter's behavior: a northbound poll-only reader that executes each tag's SQL query against the
 * configured PostgreSQL, MySQL (via the MariaDB driver), or MS SQL database on each scheduled poll and reports the
 * result set as JSON — either one value carrying all rows as an array, or, with the tag's
 * {@code spiltLinesInIndividualMessages} choice, one value per row (each row its own data point), the rows drained
 * in {@code batchSize}-row pages per {@code dataPoints} call. It never writes, browses, or subscribes — its type
 * advertises an empty capability set, so the framework never issues those commands.
 * <p>
 * The adapter owns a real connection lifecycle: {@code doConnect} registers the three bundled JDBC drivers under the
 * module's own classloader (the module loader isolates each module's classloader, so the drivers are invisible to the
 * default context classloader), opens the HikariCP pool, and validates a connection; {@code doDisconnect} closes the
 * pool. A poll failure is reported as a per-node error; the framework returns the tag to its poll interval, counts
 * the failure, and the next scheduled poll is the retry (there is no auto-removal after repeated errors, unlike v1).
 * The per-row {@code java.sql.Types}-to-JSON conversion is carried over verbatim from the v1 adapter.
 */
public final class DatabasesProtocolAdapter extends AbstractProtocolAdapter {

    private static final @NotNull Logger LOG = LoggerFactory.getLogger(DatabasesProtocolAdapter.class);
    private static final @NotNull ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * The timeout, in seconds, for the connection validation performed after the pool opens — carried over verbatim
     * from the v1 adapter.
     */
    private static final int CONNECTION_VALIDATION_TIMEOUT_SECONDS = 30;

    private final @NotNull DatabaseConnection databaseConnection;
    private final int batchSize;

    /**
     * A human-readable description of an explicitly-configured but unrecognized database engine, or {@code null} when
     * the engine is recognized. When set, {@link #doConnect()} reports it as a connection error instead of connecting
     * — the parsed configuration used the placeholder default engine, which must never open a pool with the wrong
     * driver.
     */
    private final @Nullable String unsupportedTypeError;

    /**
     * @param input  everything this adapter instance is constructed from.
     * @param output the framework's state-and-event reporter.
     */
    public DatabasesProtocolAdapter(
            final @NotNull ProtocolAdapterInput input, final @NotNull ProtocolAdapterOutput output) {
        this(input, output, configuration -> new DatabaseConnection(configuration));
    }

    DatabasesProtocolAdapter(
            final @NotNull ProtocolAdapterInput input,
            final @NotNull ProtocolAdapterOutput output,
            final @NotNull DatabaseConnectionFactory databaseConnectionFactory) {
        super(input, output);
        final DatabasesAdapterConfiguration configuration =
                DatabasesAdapterConfiguration.parse(input.adapterConfig(), OBJECT_MAPPER);
        this.databaseConnection = databaseConnectionFactory.create(configuration);
        this.batchSize = configuration.batchSize();
        this.unsupportedTypeError =
                DatabasesAdapterConfiguration.unsupportedTypeError(input.adapterConfig(), OBJECT_MAPPER);
    }

    /**
     * The seam tests use to substitute the connection layer without opening a real pool.
     */
    @FunctionalInterface
    interface DatabaseConnectionFactory {
        @NotNull
        DatabaseConnection create(@NotNull DatabasesAdapterConfiguration configuration);
    }

    @Override
    protected void doStart() {
        // No resources to allocate — the pool is opened by doConnect.
        output.started();
    }

    @Override
    protected void doStop() {
        // Stopping is the final, idempotent cleanup boundary. The wrapper can reach ERROR from CONNECTED (a watchdog
        // timeout, a defensive reset, or an ADAPTER-scope error) with the pool still open, then stop the adapter
        // directly — without a preceding disconnect — so the pool must be released here too, not only in doDisconnect.
        // Hikari's close is idempotent, so a stop after a disconnect is a harmless no-op.
        try {
            databaseConnection.close();
        } finally {
            output.stopped();
        }
    }

    @Override
    protected void doConnect() {
        if (unsupportedTypeError != null) {
            // An explicitly-configured but unrecognized engine: fail with a clear connection error rather than
            // connecting through the placeholder default engine's driver.
            output.error(ErrorScope.CONNECTION, unsupportedTypeError);
            return;
        }
        // The module loader isolates this module's classloader, so the bundled JDBC drivers must be registered under
        // it explicitly, and the context classloader must point at it while HikariCP creates the data source.
        final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

            LOG.debug("Loading PostgreSQL Driver");
            Class.forName("org.postgresql.Driver", true, getClass().getClassLoader());
            LOG.debug("Loading MariaDB Driver (for MySQL)");
            Class.forName("org.mariadb.jdbc.Driver", true, getClass().getClassLoader());
            LOG.debug("Loading MS SQL Driver");
            Class.forName(
                    "com.microsoft.sqlserver.jdbc.SQLServerDriver",
                    true,
                    getClass().getClassLoader());

            LOG.debug("Creating database connection");
            databaseConnection.connect();

            LOG.debug("Validating the connection to the database instance");
            // Evaluate validity inside the try-with-resources so the borrowed connection is returned to the pool
            // before any lifecycle result is emitted. A close() that throws while returning the connection is then
            // caught below and reported as the single connection error — never after a premature connected().
            final boolean valid;
            try (final Connection connection = databaseConnection.getConnection()) {
                valid = connection.isValid(CONNECTION_VALIDATION_TIMEOUT_SECONDS);
            }
            if (valid) {
                output.connected();
            } else {
                databaseConnection.close();
                output.error(ErrorScope.CONNECTION, "Error connecting database, please check the configuration");
            }
        } catch (final Exception e) {
            databaseConnection.close();
            output.error(ErrorScope.CONNECTION, "Error connecting database: " + e.getMessage());
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @Override
    protected void doDisconnect() {
        databaseConnection.close();
        output.disconnected();
    }

    @Override
    protected void doPoll(final @NotNull Node node) {
        if (!(node instanceof final DatabaseNode databaseNode)) {
            output.nodeError(node, "the databases adapter received a node of an unexpected type", false);
            return;
        }
        LOG.debug("Executing query : {}", databaseNode.query());
        final boolean splitLines = databaseNode.spiltLinesInIndividualMessages();
        // The array-mode value, built inside the try body but emitted only after the JDBC resources have closed.
        DataPoint arrayValue = null;
        try (final Connection connection = databaseConnection.getConnection();
                final PreparedStatement preparedStatement = connection.prepareStatement(databaseNode.query());
                final ResultSet result = preparedStatement.executeQuery()) {
            final ResultSetMetaData resultSetMetaData = result.getMetaData();
            if (splitLines) {
                // Split-lines mode: each row is its own non-terminating value. The rows are drained in batches of at
                // most batchSize per dataPoints call (a cursor page — one call carries a whole page rather than paying
                // a call per row), none ending the poll. The poll terminator (pollComplete) is emitted below, only
                // after every JDBC resource has closed; a mid-stream or close-time SQLException is caught as the single
                // node-error terminator instead, so the tag never hangs and never sees two terminators.
                List<DataPoint> batch = new ArrayList<>(batchSize);
                while (result.next()) {
                    batch.add(toDataPoint(databaseNode, readRow(result, resultSetMetaData)));
                    if (batch.size() >= batchSize) {
                        output.dataPoints(node, batch);
                        batch = new ArrayList<>(batchSize);
                    }
                }
                if (!batch.isEmpty()) {
                    output.dataPoints(node, batch);
                }
            } else {
                // Array mode: buffer every row into one value (an empty result set is an empty-array value). The value
                // is emitted below, only after the JDBC resources have closed.
                final ArrayNode allRows = OBJECT_MAPPER.createArrayNode();
                while (result.next()) {
                    allRows.add(readRow(result, resultSetMetaData));
                }
                arrayValue = toDataPoint(databaseNode, allRows);
            }
        } catch (final SQLException e) {
            LOG.debug("An exception occurred while executing the query '{}'.", databaseNode.query(), e);
            output.nodeError(
                    node,
                    "An exception occurred while executing the query '" + databaseNode.query() + "': " + e.getMessage(),
                    false);
            return;
        }
        // Every JDBC resource closed successfully: only now emit the single success terminator. Emitting it inside the
        // try body would let a resource close() failure report a second terminator (the node error above) for the same
        // poll — violating the exactly-one-terminator contract.
        if (splitLines) {
            output.pollComplete(node);
        } else {
            LOG.debug("Publishing all rows in a single message");
            output.dataPoint(node, Objects.requireNonNull(arrayValue, "the array-mode value must be built on success"));
        }
    }

    private static @NotNull ObjectNode readRow(
            final @NotNull ResultSet result, final @NotNull ResultSetMetaData resultSetMetaData) throws SQLException {
        final int numberOfColumns = resultSetMetaData.getColumnCount();
        final ObjectNode row = OBJECT_MAPPER.createObjectNode();
        for (int i = 1; i <= numberOfColumns; i++) {
            parseAndAddValue(i, result, resultSetMetaData, row);
        }
        return row;
    }

    // according to https://www.ibm.com/docs/en/db2/11.1?topic=djr-sql-data-type-representation
    private static void parseAndAddValue(
            final int index,
            final @NotNull ResultSet result,
            final @NotNull ResultSetMetaData resultSetMetaData,
            final @NotNull ObjectNode node)
            throws SQLException {
        // Prefer the column label — the JDBC name for an {@code AS} alias — so an aliased column publishes the same
        // JSON key on every engine; fall back to the underlying column name only when a driver reports no label.
        String columnName = resultSetMetaData.getColumnLabel(index);
        if (columnName == null || columnName.isBlank()) {
            columnName = resultSetMetaData.getColumnName(index);
        }
        final int columnType = resultSetMetaData.getColumnType(index);
        switch (columnType) {
            case Types.BIT, Types.TINYINT, Types.SMALLINT, Types.INTEGER -> {
                final int value = result.getInt(index);
                // A JDBC primitive getter returns 0 for a SQL NULL; wasNull() distinguishes a real 0 from a missing
                // value, which must publish as JSON null rather than be corrupted into a real zero.
                if (result.wasNull()) {
                    node.putNull(columnName);
                } else {
                    node.put(columnName, value);
                }
            }
            case Types.BIGINT -> {
                final long value = result.getLong(index);
                if (result.wasNull()) {
                    node.putNull(columnName);
                } else {
                    node.put(columnName, value);
                }
            }
            // DECIMAL and NUMERIC both carry arbitrary precision, so both go through BigDecimal — mapping NUMERIC to
            // double (as v1 did) truncates values beyond IEEE-754 precision. getBigDecimal returns null for SQL NULL,
            // which put() records as JSON null.
            case Types.DECIMAL, Types.NUMERIC -> node.put(columnName, result.getBigDecimal(index));
            case Types.REAL, Types.FLOAT, Types.DOUBLE -> {
                final double value = result.getDouble(index);
                if (result.wasNull()) {
                    node.putNull(columnName);
                } else {
                    node.put(columnName, value);
                }
            }
            // Boolean, temporal, and binary types are carried in their JDBC string form (retained v1 parity);
            // getString returns null for a SQL NULL, which put() records as JSON null.
            default -> node.put(columnName, result.getString(index));
        }
    }

    private @NotNull DataPoint toDataPoint(final @NotNull DatabaseNode databaseNode, final @NotNull Object value) {
        // The framework stamps the owning tag's name onto the value, so the node id is a stable placeholder here.
        return dataPointFactory.create(databaseNode.nodeId(), value);
    }

    @Override
    protected void doAddSubscription(final @NotNull Node node) {
        // The Databases adapter does not advertise the SUBSCRIPTIONS capability, so the framework never calls this;
        // report a per-node error defensively should it ever be invoked.
        output.nodeError(node, "the databases adapter does not support subscriptions", false);
    }

    @Override
    protected void doWrite(final @NotNull Node node, final @NotNull DataPoint value) {
        // The Databases adapter does not advertise the WRITE capability, so the framework never calls this; report a
        // failed write defensively should it ever be invoked.
        output.writeResult(node, false, "the databases adapter does not support writing");
    }
}
