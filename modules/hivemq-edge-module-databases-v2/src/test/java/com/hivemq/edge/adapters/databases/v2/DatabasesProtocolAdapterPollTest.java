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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.adapter.sdk.api.v2.node.NodeProperty;
import com.hivemq.protocols.v2.runtime.ManualDispatcher;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.EnumSet;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * Drives {@link DatabasesProtocolAdapter#pollBatch(List)} against a stubbed JDBC layer — no real database — and
 * asserts the three message-shaping modes, the batch size (a cursor page size in {@link SplitMode#ONE_PER_ROW} and the
 * array size in {@link SplitMode#ONE_PER_BATCH}), the zero-row poll, the row-to-JSON type mapping, and the failure
 * path. The adapter owns its own terminator: {@link SplitMode#ALL_IN_ONE} is a single completing {@code dataPoint};
 * {@link SplitMode#ONE_PER_ROW} reports each row as its own value (drained in batch-size pages) and
 * {@link SplitMode#ONE_PER_BATCH} reports each batch of rows as one array value, both via non-terminating
 * {@code dataPoints} calls then an explicit {@code pollComplete}; a failure is a {@code nodeError}, which is itself the
 * terminator.
 */
class DatabasesProtocolAdapterPollTest {

    private final ManualDispatcher dispatcher = new ManualDispatcher();
    private final RecordingProtocolAdapterOutput output = new RecordingProtocolAdapterOutput();

    /**
     * A {@link DatabaseConnection} whose pool is replaced by a stubbed JDBC connection.
     */
    private static final class StubbedDatabaseConnection extends DatabaseConnection {

        private final @NotNull Connection connection;

        private StubbedDatabaseConnection(
                final @NotNull DatabasesAdapterConfiguration configuration, final @NotNull Connection connection) {
            super(configuration);
            this.connection = connection;
        }

        @Override
        public void connect() {
            // No pool to open.
        }

        @Override
        public @NotNull Connection getConnection() {
            return connection;
        }

        @Override
        public void close() {
            // No pool to close.
        }
    }

    private @NotNull DatabasesProtocolAdapter adapterOver(final @NotNull Connection connection) {
        return new DatabasesProtocolAdapter(
                DatabasesAdapterTestFixtures.input(
                        "databases-v2-1",
                        dispatcher,
                        new DatabasesAdapterTestFixtures.TestDataPointFactory(),
                        DatabasesAdapterTestFixtures.configuration("POSTGRESQL", 5432),
                        List.of()),
                output,
                configuration -> new StubbedDatabaseConnection(configuration, connection));
    }

    /**
     * Stub a query returning the given rows, all columns of {@code VARCHAR} type.
     */
    private static @NotNull Connection connectionReturningStringRows(final @NotNull List<List<String>> rows)
            throws SQLException {
        final Connection connection = mock(Connection.class);
        final PreparedStatement statement = mock(PreparedStatement.class);
        final ResultSet resultSet = mock(ResultSet.class);
        final ResultSetMetaData metaData = mock(ResultSetMetaData.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(metaData);
        final int columns = rows.isEmpty() ? 0 : rows.get(0).size();
        when(metaData.getColumnCount()).thenReturn(columns);
        for (int i = 1; i <= columns; i++) {
            when(metaData.getColumnName(i)).thenReturn("column" + i);
            when(metaData.getColumnType(i)).thenReturn(Types.VARCHAR);
        }
        if (rows.isEmpty()) {
            when(resultSet.next()).thenReturn(false);
        } else {
            // next() answers true once per row, then false.
            final Boolean[] followingAnswers = new Boolean[rows.size()];
            for (int i = 0; i < rows.size() - 1; i++) {
                followingAnswers[i] = true;
            }
            followingAnswers[rows.size() - 1] = false;
            when(resultSet.next()).thenReturn(true, followingAnswers);
            for (int column = 1; column <= columns; column++) {
                final int columnIndex = column;
                var stubbing = when(resultSet.getString(columnIndex))
                        .thenReturn(rows.get(0).get(columnIndex - 1));
                for (int row = 1; row < rows.size(); row++) {
                    stubbing = stubbing.thenReturn(rows.get(row).get(columnIndex - 1));
                }
            }
        }
        return connection;
    }

    private static @NotNull ObjectNode rowOf(final @NotNull RecordingProtocolAdapterOutput.DataPointRecord record) {
        return (ObjectNode) record.value().getTagValue();
    }

    @Test
    void allInOneMode_emitsOneDataPointCarryingAllRows_thenCompletes() throws SQLException {
        final DatabasesProtocolAdapter adapter =
                adapterOver(connectionReturningStringRows(List.of(List.of("first"), List.of("second"))));
        final DatabaseNode node = new DatabaseNode("SELECT name FROM products", SplitMode.ALL_IN_ONE, 100);

        adapter.pollBatch(List.of(node));
        dispatcher.drainAll();

        assertThat(output.nodeErrors).isEmpty();
        assertThat(output.dataPoints).hasSize(1);
        final Object value = output.dataPoints.get(0).value().getTagValue();
        assertThat(value).isInstanceOf(ArrayNode.class);
        final ArrayNode rows = (ArrayNode) value;
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).get("column1").asText()).isEqualTo("first");
        assertThat(rows.get(1).get("column1").asText()).isEqualTo("second");
        // A single completing dataPoint carries all rows — no separate pollComplete.
        assertThat(output.events).containsExactly("dataPoint");
    }

    @Test
    void onePerRowMode_emitsEachRowAsItsOwnDataPoint_drainedInOnePageAtTheDefaultBatchSize() throws SQLException {
        final DatabasesProtocolAdapter adapter = adapterOver(
                connectionReturningStringRows(List.of(List.of("first"), List.of("second"), List.of("third"))));
        final DatabaseNode node = new DatabaseNode("SELECT name FROM products", SplitMode.ONE_PER_ROW, 100);

        adapter.pollBatch(List.of(node));
        dispatcher.drainAll();

        assertThat(output.nodeErrors).isEmpty();
        // The default batch size (100) exceeds the row count, so all three bare-row values are drained in one page,
        // then one completion. Each row is still its own data point — never packed into an array.
        assertThat(output.batches).extracting(List::size).containsExactly(3);
        assertThat(output.dataPoints).hasSize(3);
        for (final RecordingProtocolAdapterOutput.DataPointRecord record : output.dataPoints) {
            assertThat(record.node()).isSameAs(node);
            assertThat(record.value().getTagValue()).isInstanceOf(ObjectNode.class);
        }
        assertThat(rowOf(output.dataPoints.get(0)).get("column1").asText()).isEqualTo("first");
        assertThat(rowOf(output.dataPoints.get(2)).get("column1").asText()).isEqualTo("third");
        assertThat(output.events).containsExactly("dataPoints", "pollComplete");
    }

    @Test
    void onePerRowMode_drainsRowsInBatchSizePages_eachRowStillItsOwnDataPoint() throws SQLException {
        final DatabasesProtocolAdapter adapter = adapterOver(connectionReturningStringRows(
                List.of(List.of("a"), List.of("b"), List.of("c"), List.of("d"), List.of("e"))));
        final DatabaseNode node = new DatabaseNode("SELECT name FROM products", SplitMode.ONE_PER_ROW, 2);

        adapter.pollBatch(List.of(node));
        dispatcher.drainAll();

        assertThat(output.nodeErrors).isEmpty();
        // Five rows drained in pages of two → dataPoints calls carrying 2, 2, and 1 bare-row values, then one
        // completion. The batch size only pages the cursor drain; each row is still its own data point (an object),
        // never packed into an array.
        assertThat(output.batches).extracting(List::size).containsExactly(2, 2, 1);
        assertThat(output.dataPoints).hasSize(5);
        for (final RecordingProtocolAdapterOutput.DataPointRecord record : output.dataPoints) {
            assertThat(record.value().getTagValue()).isInstanceOf(ObjectNode.class);
        }
        assertThat(rowOf(output.dataPoints.get(0)).get("column1").asText()).isEqualTo("a");
        assertThat(rowOf(output.dataPoints.get(4)).get("column1").asText()).isEqualTo("e");
        assertThat(output.events).containsExactly("dataPoints", "dataPoints", "dataPoints", "pollComplete");
    }

    @Test
    void onePerBatchMode_packsRowsIntoArraysOfTheBatchSize_thenCompletes() throws SQLException {
        final DatabasesProtocolAdapter adapter = adapterOver(connectionReturningStringRows(
                List.of(List.of("a"), List.of("b"), List.of("c"), List.of("d"), List.of("e"))));
        final DatabaseNode node = new DatabaseNode("SELECT name FROM products", SplitMode.ONE_PER_BATCH, 2);

        adapter.pollBatch(List.of(node));
        dispatcher.drainAll();

        assertThat(output.nodeErrors).isEmpty();
        // Five rows in batches of two → three array data points holding 2, 2, and 1 rows, then one completion.
        assertThat(output.dataPoints).hasSize(3);
        final ArrayNode firstBatch =
                (ArrayNode) output.dataPoints.get(0).value().getTagValue();
        final ArrayNode secondBatch =
                (ArrayNode) output.dataPoints.get(1).value().getTagValue();
        final ArrayNode lastBatch = (ArrayNode) output.dataPoints.get(2).value().getTagValue();
        assertThat(firstBatch).hasSize(2);
        assertThat(secondBatch).hasSize(2);
        assertThat(lastBatch).hasSize(1);
        assertThat(firstBatch.get(0).get("column1").asText()).isEqualTo("a");
        assertThat(firstBatch.get(1).get("column1").asText()).isEqualTo("b");
        assertThat(lastBatch.get(0).get("column1").asText()).isEqualTo("e");
        assertThat(output.events).containsExactly("dataPoints", "dataPoints", "dataPoints", "pollComplete");
    }

    @Test
    void onePerBatchMode_withABatchSizeLargerThanTheResult_emitsASingleArrayDataPoint() throws SQLException {
        final DatabasesProtocolAdapter adapter = adapterOver(
                connectionReturningStringRows(List.of(List.of("first"), List.of("second"), List.of("third"))));
        final DatabaseNode node = new DatabaseNode("SELECT name FROM products", SplitMode.ONE_PER_BATCH, 100);

        adapter.pollBatch(List.of(node));
        dispatcher.drainAll();

        assertThat(output.nodeErrors).isEmpty();
        // The batch size exceeds the row count, so all three rows land in one array data point.
        assertThat(output.dataPoints).hasSize(1);
        final ArrayNode onlyBatch = (ArrayNode) output.dataPoints.get(0).value().getTagValue();
        assertThat(onlyBatch).hasSize(3);
        assertThat(output.events).containsExactly("dataPoints", "pollComplete");
    }

    @Test
    void aZeroRowQueryInOnePerRowMode_emitsNothingButStillCompletes() throws SQLException {
        final DatabasesProtocolAdapter adapter = adapterOver(connectionReturningStringRows(List.of()));
        final DatabaseNode node = new DatabaseNode("SELECT name FROM products WHERE 1 = 0", SplitMode.ONE_PER_ROW, 100);

        adapter.pollBatch(List.of(node));
        dispatcher.drainAll();

        assertThat(output.dataPoints).isEmpty();
        assertThat(output.nodeErrors).isEmpty();
        assertThat(output.events).containsExactly("pollComplete");
    }

    @Test
    void aZeroRowQueryInOnePerBatchMode_emitsNothingButStillCompletes() throws SQLException {
        final DatabasesProtocolAdapter adapter = adapterOver(connectionReturningStringRows(List.of()));
        final DatabaseNode node = new DatabaseNode("SELECT name FROM products WHERE 1 = 0", SplitMode.ONE_PER_BATCH, 2);

        adapter.pollBatch(List.of(node));
        dispatcher.drainAll();

        assertThat(output.dataPoints).isEmpty();
        assertThat(output.nodeErrors).isEmpty();
        assertThat(output.events).containsExactly("pollComplete");
    }

    @Test
    void aZeroRowQueryInAllInOneMode_emitsAnEmptyArray_thenCompletes() throws SQLException {
        final DatabasesProtocolAdapter adapter = adapterOver(connectionReturningStringRows(List.of()));
        final DatabaseNode node = new DatabaseNode("SELECT name FROM products WHERE 1 = 0", SplitMode.ALL_IN_ONE, 100);

        adapter.pollBatch(List.of(node));
        dispatcher.drainAll();

        assertThat(output.dataPoints).hasSize(1);
        assertThat((ArrayNode) output.dataPoints.get(0).value().getTagValue()).isEmpty();
        // An empty result in all-in-one mode is still one completing dataPoint carrying an empty array.
        assertThat(output.events).containsExactly("dataPoint");
    }

    @Test
    void eachSqlTypeBranchMapsToItsJsonNodeType() throws SQLException {
        final Connection connection = mock(Connection.class);
        final PreparedStatement statement = mock(PreparedStatement.class);
        final ResultSet resultSet = mock(ResultSet.class);
        final ResultSetMetaData metaData = mock(ResultSetMetaData.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(resultSet.next()).thenReturn(true, false);
        when(metaData.getColumnCount()).thenReturn(5);
        when(metaData.getColumnName(1)).thenReturn("intColumn");
        when(metaData.getColumnType(1)).thenReturn(Types.INTEGER);
        when(resultSet.getInt(1)).thenReturn(21);
        when(metaData.getColumnName(2)).thenReturn("longColumn");
        when(metaData.getColumnType(2)).thenReturn(Types.BIGINT);
        when(resultSet.getLong(2)).thenReturn(9_000_000_000L);
        when(metaData.getColumnName(3)).thenReturn("decimalColumn");
        when(metaData.getColumnType(3)).thenReturn(Types.DECIMAL);
        when(resultSet.getBigDecimal(3)).thenReturn(new BigDecimal("12.34"));
        when(metaData.getColumnName(4)).thenReturn("doubleColumn");
        when(metaData.getColumnType(4)).thenReturn(Types.DOUBLE);
        when(resultSet.getDouble(4)).thenReturn(0.5);
        when(metaData.getColumnName(5)).thenReturn("textColumn");
        when(metaData.getColumnType(5)).thenReturn(Types.VARCHAR);
        when(resultSet.getString(5)).thenReturn("text");

        // Each one-per-row value is its own bare object, so the type mapping is asserted directly on the row.
        final DatabasesProtocolAdapter adapter = adapterOver(connection);
        adapter.pollBatch(List.of(new DatabaseNode("SELECT * FROM readings", SplitMode.ONE_PER_ROW, 100)));
        dispatcher.drainAll();

        assertThat(output.dataPoints).hasSize(1);
        final ObjectNode row = rowOf(output.dataPoints.get(0));
        assertThat(row.get("intColumn").isInt()).isTrue();
        assertThat(row.get("intColumn").asInt()).isEqualTo(21);
        assertThat(row.get("longColumn").isLong()).isTrue();
        assertThat(row.get("longColumn").asLong()).isEqualTo(9_000_000_000L);
        assertThat(row.get("decimalColumn").isBigDecimal()).isTrue();
        assertThat(row.get("decimalColumn").decimalValue()).isEqualByComparingTo("12.34");
        assertThat(row.get("doubleColumn").isDouble()).isTrue();
        assertThat(row.get("doubleColumn").asDouble()).isEqualTo(0.5);
        assertThat(row.get("textColumn").isTextual()).isTrue();
        assertThat(row.get("textColumn").asText()).isEqualTo("text");
    }

    @Test
    void sqlNullInAPrimitiveNumericColumn_publishesJsonNullNotZero() throws SQLException {
        final Connection connection = mock(Connection.class);
        final PreparedStatement statement = mock(PreparedStatement.class);
        final ResultSet resultSet = mock(ResultSet.class);
        final ResultSetMetaData metaData = mock(ResultSetMetaData.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(resultSet.next()).thenReturn(true, false);
        when(metaData.getColumnCount()).thenReturn(3);
        when(metaData.getColumnLabel(1)).thenReturn("intColumn");
        when(metaData.getColumnType(1)).thenReturn(Types.INTEGER);
        when(resultSet.getInt(1)).thenReturn(0);
        when(metaData.getColumnLabel(2)).thenReturn("longColumn");
        when(metaData.getColumnType(2)).thenReturn(Types.BIGINT);
        when(resultSet.getLong(2)).thenReturn(0L);
        when(metaData.getColumnLabel(3)).thenReturn("doubleColumn");
        when(metaData.getColumnType(3)).thenReturn(Types.DOUBLE);
        when(resultSet.getDouble(3)).thenReturn(0.0);
        // Every primitive getter returned its zero, but each value was actually SQL NULL.
        when(resultSet.wasNull()).thenReturn(true);

        final DatabasesProtocolAdapter adapter = adapterOver(connection);
        adapter.pollBatch(List.of(new DatabaseNode("SELECT * FROM readings", SplitMode.ONE_PER_ROW, 100)));
        dispatcher.drainAll();

        assertThat(output.dataPoints).hasSize(1);
        final ObjectNode row = rowOf(output.dataPoints.get(0));
        // A SQL NULL is JSON null — not a fabricated real 0/0L/0.0.
        assertThat(row.get("intColumn").isNull()).isTrue();
        assertThat(row.get("longColumn").isNull()).isTrue();
        assertThat(row.get("doubleColumn").isNull()).isTrue();
    }

    @Test
    void aNumericColumnIsCarriedAsBigDecimalPreservingPrecisionBeyondADouble() throws SQLException {
        final Connection connection = mock(Connection.class);
        final PreparedStatement statement = mock(PreparedStatement.class);
        final ResultSet resultSet = mock(ResultSet.class);
        final ResultSetMetaData metaData = mock(ResultSetMetaData.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(resultSet.next()).thenReturn(true, false);
        when(metaData.getColumnCount()).thenReturn(1);
        when(metaData.getColumnLabel(1)).thenReturn("amount");
        when(metaData.getColumnType(1)).thenReturn(Types.NUMERIC);
        final BigDecimal precise = new BigDecimal("12345678901234567890.123456789");
        when(resultSet.getBigDecimal(1)).thenReturn(precise);

        final DatabasesProtocolAdapter adapter = adapterOver(connection);
        adapter.pollBatch(List.of(new DatabaseNode("SELECT amount FROM ledger", SplitMode.ONE_PER_ROW, 100)));
        dispatcher.drainAll();

        final ObjectNode row = rowOf(output.dataPoints.get(0));
        // NUMERIC goes through BigDecimal like DECIMAL — mapping it to double (as v1 did) would truncate the value.
        assertThat(row.get("amount").isBigDecimal()).isTrue();
        assertThat(row.get("amount").decimalValue()).isEqualByComparingTo(precise);
    }

    @Test
    void theJsonKeyIsTheColumnLabelSoAnAliasIsPortableAcrossDrivers() throws SQLException {
        final Connection connection = mock(Connection.class);
        final PreparedStatement statement = mock(PreparedStatement.class);
        final ResultSet resultSet = mock(ResultSet.class);
        final ResultSetMetaData metaData = mock(ResultSetMetaData.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(resultSet.next()).thenReturn(true, false);
        when(metaData.getColumnCount()).thenReturn(1);
        // The query aliased product_no AS id; the JSON key must be the label (the alias), not the underlying name.
        when(metaData.getColumnLabel(1)).thenReturn("id");
        when(metaData.getColumnName(1)).thenReturn("product_no");
        when(metaData.getColumnType(1)).thenReturn(Types.INTEGER);
        when(resultSet.getInt(1)).thenReturn(7);

        final DatabasesProtocolAdapter adapter = adapterOver(connection);
        adapter.pollBatch(
                List.of(new DatabaseNode("SELECT product_no AS id FROM products", SplitMode.ONE_PER_ROW, 100)));
        dispatcher.drainAll();

        final ObjectNode row = rowOf(output.dataPoints.get(0));
        assertThat(row.has("id")).isTrue();
        assertThat(row.has("product_no")).isFalse();
        assertThat(row.get("id").asInt()).isEqualTo(7);
    }

    @Test
    void aBlankColumnLabelFallsBackToTheColumnName() throws SQLException {
        final Connection connection = mock(Connection.class);
        final PreparedStatement statement = mock(PreparedStatement.class);
        final ResultSet resultSet = mock(ResultSet.class);
        final ResultSetMetaData metaData = mock(ResultSetMetaData.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(resultSet.next()).thenReturn(true, false);
        when(metaData.getColumnCount()).thenReturn(1);
        // A driver that reports no label: the underlying column name is used instead of an empty JSON key.
        when(metaData.getColumnLabel(1)).thenReturn("");
        when(metaData.getColumnName(1)).thenReturn("product_no");
        when(metaData.getColumnType(1)).thenReturn(Types.INTEGER);
        when(resultSet.getInt(1)).thenReturn(7);

        final DatabasesProtocolAdapter adapter = adapterOver(connection);
        adapter.pollBatch(List.of(new DatabaseNode("SELECT product_no FROM products", SplitMode.ONE_PER_ROW, 100)));
        dispatcher.drainAll();

        final ObjectNode row = rowOf(output.dataPoints.get(0));
        assertThat(row.has("product_no")).isTrue();
        assertThat(row.get("product_no").asInt()).isEqualTo(7);
    }

    @Test
    void allInOneMode_aResourceCloseFailure_reportsASingleNodeErrorAndNoSuccessTerminator() throws SQLException {
        final Connection connection = mock(Connection.class);
        final PreparedStatement statement = mock(PreparedStatement.class);
        final ResultSet resultSet = mock(ResultSet.class);
        final ResultSetMetaData metaData = mock(ResultSetMetaData.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(1);
        when(metaData.getColumnLabel(1)).thenReturn("name");
        when(metaData.getColumnType(1)).thenReturn(Types.VARCHAR);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString(1)).thenReturn("apple");
        // Returning the result set to the driver fails after every row was read.
        doThrow(new SQLException("cursor close failed")).when(resultSet).close();

        final DatabasesProtocolAdapter adapter = adapterOver(connection);
        adapter.pollBatch(List.of(new DatabaseNode("SELECT name FROM products", SplitMode.ALL_IN_ONE, 100)));
        dispatcher.drainAll();

        // The success dataPoint is emitted only after the resources close, so a close failure yields exactly one
        // terminator — the node error — never a success terminator followed by a contradictory failure.
        assertThat(output.dataPoints).isEmpty();
        assertThat(output.nodeErrors).hasSize(1);
        assertThat(output.nodeErrors.get(0).reason()).contains("cursor close failed");
        assertThat(output.events).containsExactly("nodeError");
    }

    @Test
    void onePerRowMode_aResourceCloseFailureAfterRows_deliversTheRowsThenExactlyOneNodeError() throws SQLException {
        final Connection connection = mock(Connection.class);
        final PreparedStatement statement = mock(PreparedStatement.class);
        final ResultSet resultSet = mock(ResultSet.class);
        final ResultSetMetaData metaData = mock(ResultSetMetaData.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(1);
        when(metaData.getColumnLabel(1)).thenReturn("name");
        when(metaData.getColumnType(1)).thenReturn(Types.VARCHAR);
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getString(1)).thenReturn("apple", "banana");
        doThrow(new SQLException("cursor close failed")).when(resultSet).close();

        // Batch size 1 flushes each row as its own page before the cursor is closed.
        final DatabasesProtocolAdapter adapter = adapterOver(connection);
        adapter.pollBatch(List.of(new DatabaseNode("SELECT name FROM products", SplitMode.ONE_PER_ROW, 1)));
        dispatcher.drainAll();

        // The rows were streamed before the close failed; the close failure is the single terminator — pollComplete
        // (the success terminator) is emitted only after the resources close, so it is never reported here.
        assertThat(output.dataPoints).hasSize(2);
        assertThat(output.nodeErrors).hasSize(1);
        assertThat(output.nodeErrors.get(0).reason()).contains("cursor close failed");
        assertThat(output.events).containsExactly("dataPoints", "dataPoints", "nodeError");
        assertThat(output.events).doesNotContain("pollComplete");
    }

    @Test
    void onePerBatchMode_aResourceCloseFailureAfterABatch_deliversTheBatchThenExactlyOneNodeError()
            throws SQLException {
        final Connection connection = mock(Connection.class);
        final PreparedStatement statement = mock(PreparedStatement.class);
        final ResultSet resultSet = mock(ResultSet.class);
        final ResultSetMetaData metaData = mock(ResultSetMetaData.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(1);
        when(metaData.getColumnLabel(1)).thenReturn("name");
        when(metaData.getColumnType(1)).thenReturn(Types.VARCHAR);
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getString(1)).thenReturn("apple", "banana");
        doThrow(new SQLException("cursor close failed")).when(resultSet).close();

        final DatabasesProtocolAdapter adapter = adapterOver(connection);
        adapter.pollBatch(List.of(new DatabaseNode("SELECT name FROM products", SplitMode.ONE_PER_BATCH, 2)));
        dispatcher.drainAll();

        // The two rows filled one batch (of size two) that was streamed before the close failed; the close failure is
        // the single terminator, and no pollComplete follows it.
        assertThat(output.dataPoints).hasSize(1);
        assertThat((ArrayNode) output.dataPoints.get(0).value().getTagValue()).hasSize(2);
        assertThat(output.nodeErrors).hasSize(1);
        assertThat(output.nodeErrors.get(0).reason()).contains("cursor close failed");
        assertThat(output.events).containsExactly("dataPoints", "nodeError");
        assertThat(output.events).doesNotContain("pollComplete");
    }

    @Test
    void aFailedQuery_reportsANodeErrorWhichTerminatesThePoll() throws SQLException {
        final Connection connection = mock(Connection.class);
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("relation does not exist"));
        final DatabasesProtocolAdapter adapter = adapterOver(connection);
        final DatabaseNode node = new DatabaseNode("SELECT name FROM missing", SplitMode.ALL_IN_ONE, 100);

        adapter.pollBatch(List.of(node));
        dispatcher.drainAll();

        assertThat(output.dataPoints).isEmpty();
        assertThat(output.nodeErrors).hasSize(1);
        assertThat(output.nodeErrors.get(0).node()).isSameAs(node);
        assertThat(output.nodeErrors.get(0).reason()).contains("relation does not exist");
        assertThat(output.nodeErrors.get(0).spontaneous()).isFalse();
        // nodeError is itself the terminator — no trailing pollComplete.
        assertThat(output.events).containsExactly("nodeError");
    }

    @Test
    void aNodeOfAnUnexpectedType_reportsANodeError() {
        final DatabasesProtocolAdapter adapter = adapterOver(mock(Connection.class));
        final Node wrongNode = new Node() {
            @Override
            public @NotNull String nodeId() {
                return "wrong";
            }

            @Override
            public @NotNull String nodeString() {
                return "{}";
            }

            @Override
            public @NotNull EnumSet<NodeProperty> properties() {
                return EnumSet.noneOf(NodeProperty.class);
            }
        };

        adapter.pollBatch(List.of(wrongNode));
        dispatcher.drainAll();

        assertThat(output.nodeErrors).hasSize(1);
        assertThat(output.nodeErrors.get(0).reason()).contains("unexpected type");
    }
}
