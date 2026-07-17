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
 * asserts the two message-shaping modes, the zero-row poll, the row-to-JSON type mapping, and the failure path. The
 * template completes every poll automatically, so each scenario also asserts the single trailing {@code pollComplete}.
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

    @Test
    void arrayMode_emitsOneDataPointCarryingAllRows_thenCompletes() throws SQLException {
        final DatabasesProtocolAdapter adapter =
                adapterOver(connectionReturningStringRows(List.of(List.of("first"), List.of("second"))));
        final DatabaseNode node = new DatabaseNode("SELECT name FROM products", false);

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
        assertThat(output.events).containsExactly("dataPoint", "pollComplete");
    }

    @Test
    void splitMode_emitsOneDataPointPerRow_thenASingleCompletion() throws SQLException {
        final DatabasesProtocolAdapter adapter = adapterOver(
                connectionReturningStringRows(List.of(List.of("first"), List.of("second"), List.of("third"))));
        final DatabaseNode node = new DatabaseNode("SELECT name FROM products", true);

        adapter.pollBatch(List.of(node));
        dispatcher.drainAll();

        assertThat(output.nodeErrors).isEmpty();
        assertThat(output.dataPoints).hasSize(3);
        for (final RecordingProtocolAdapterOutput.DataPointRecord record : output.dataPoints) {
            assertThat(record.node()).isSameAs(node);
            assertThat(record.value().getTagValue()).isInstanceOf(ObjectNode.class);
        }
        assertThat(((ObjectNode) output.dataPoints.get(0).value().getTagValue())
                        .get("column1")
                        .asText())
                .isEqualTo("first");
        assertThat(output.events).containsExactly("dataPoint", "dataPoint", "dataPoint", "pollComplete");
    }

    @Test
    void aZeroRowQueryInSplitMode_emitsNothingButStillCompletes() throws SQLException {
        final DatabasesProtocolAdapter adapter = adapterOver(connectionReturningStringRows(List.of()));
        final DatabaseNode node = new DatabaseNode("SELECT name FROM products WHERE 1 = 0", true);

        adapter.pollBatch(List.of(node));
        dispatcher.drainAll();

        assertThat(output.dataPoints).isEmpty();
        assertThat(output.nodeErrors).isEmpty();
        assertThat(output.events).containsExactly("pollComplete");
    }

    @Test
    void aZeroRowQueryInArrayMode_emitsAnEmptyArray_thenCompletes() throws SQLException {
        final DatabasesProtocolAdapter adapter = adapterOver(connectionReturningStringRows(List.of()));
        final DatabaseNode node = new DatabaseNode("SELECT name FROM products WHERE 1 = 0", false);

        adapter.pollBatch(List.of(node));
        dispatcher.drainAll();

        assertThat(output.dataPoints).hasSize(1);
        assertThat((ArrayNode) output.dataPoints.get(0).value().getTagValue()).isEmpty();
        assertThat(output.events).containsExactly("dataPoint", "pollComplete");
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

        final DatabasesProtocolAdapter adapter = adapterOver(connection);
        adapter.pollBatch(List.of(new DatabaseNode("SELECT * FROM readings", true)));
        dispatcher.drainAll();

        assertThat(output.dataPoints).hasSize(1);
        final ObjectNode row = (ObjectNode) output.dataPoints.get(0).value().getTagValue();
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
    void aFailedQuery_reportsANodeError_andTheTemplateStillCompletesThePoll() throws SQLException {
        final Connection connection = mock(Connection.class);
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("relation does not exist"));
        final DatabasesProtocolAdapter adapter = adapterOver(connection);
        final DatabaseNode node = new DatabaseNode("SELECT name FROM missing", false);

        adapter.pollBatch(List.of(node));
        dispatcher.drainAll();

        assertThat(output.dataPoints).isEmpty();
        assertThat(output.nodeErrors).hasSize(1);
        assertThat(output.nodeErrors.get(0).node()).isSameAs(node);
        assertThat(output.nodeErrors.get(0).reason()).contains("relation does not exist");
        assertThat(output.nodeErrors.get(0).spontaneous()).isFalse();
        assertThat(output.events).containsExactly("nodeError", "pollComplete");
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
